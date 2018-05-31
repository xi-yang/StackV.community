/* eslint-disable */
import * as d3 from 'd3';

import DataModel from '../data-model/data-model';
import getIcon from '../icon';
import SVDragEvent from './SVDragEvent';
import LayoutPersistent from './layout-persistent/layout-persistent';
import {LP_LOCALSTORAGE_KEY} from './layout-persistent/consts';
import NodeDetailController from './ui-popover/node-detail/controller';
import NodeHighlighter from './node-highlighter/highlighter';

const $detailsModal = $("#details-modal");
const detailsConfig = {
    title: 'Details',
    headerColor: '#3e4d5f',
    width: '50vh',
    transitionIn: 'fadeInDown',
    transitionOut: 'fadeOutUp',
    top: '104px',
    overlay: false
};

const NODE_COLOR = d3.scaleOrdinal(['#63a4ff']);
const HULL_COLOR = d3.scaleOrdinal(['rgba(26, 35, 126, 0.85)']);

const DEFAULT_RENDER_SPEED = 5;

/**
 * Front-end Visual Model engine, directly operate D3 SVG
 */
class VisualModel {
  /**
   * related data model
   * @type {DataModel}
   */
  dataModel = null;
  /**x
   * HTML SVG Element
   */
  svg = {
    outerWrapper: null,
    root: null,
    container: null,
    nodes: null,
    services: null,
    links: null,
    hulls: null,
  };
  /**
   * SVG view size
   */
  size = {width: 0, height: 0};
  /**
   * D3 force layout controller
   */
  force = null;
  /**
   * D3 force layout config
   */
  forceConfig = {
    centerForce: null,
    centerRadialForce: null,
  };
  /**
   * DOM Event handlers
   */
  eventHandlers = {
    node: {
      drag: null,
    },
    hull: {
      drag: null,
    }
  };
  /**
   * D3 SVG zoom controller
   */
  zoom = d3.zoom().on('zoom', () => this.zoomed());
  /**
   * Render speed
   * @type {number}
   */
  renderSpeed = 5;
  /**
   * Since some task need to be done AFTER TICK, so we use queue
   * @type {Array<object>}
   */
  renderQueue = [];
  /**
   * Store render record
   * @type {object}
   */
  renderState = {
    initialTickEnd: false,
    globalRendering: false,
    nodeDragStarted: false,
    hullDragStarted: false,
    hullDragStartedPosition: {x: 0, y: 0},
  };
  /**
   * Node Popover Detail dialogs controller
   * @type {NodeDetailController}
   */
  nodeDetailController;
  /**
   * Mark if apply layout persistence automatically
   * @type {boolean}
   */
  persistence = false;
  /**
   * Utility for highlighting any node in view hierarchy
   * @type {NodeHighlighter}
   */
  highlighter = null;

  /**
   * constructor receive a HTML element, create SVG
   * @param {DataModel} dataModel - A data model instance
   * @param {object} domElement - The ROOT element
   * @param {number} width - initial width
   * @param {number} height - initial height
   * @param {boolean} persistence - Enable or Disable the layout persistence
   */
  constructor(dataModel, domElement, width = 1024, height = 768, persistence = true) {
    this.dataModel = dataModel;

    let layoutPersistent;
    if (persistence) {
      const localLayoutData = localStorage.getItem(LP_LOCALSTORAGE_KEY);
      if (localLayoutData) {
        layoutPersistent = new LayoutPersistent();
        layoutPersistent.read(localLayoutData);
        layoutPersistent.apply(this);
      }
    }

    this.size.width = width;
    this.size.height = height;

    this.svg.outerWrapper = d3.select(domElement)
        .insert('div', ':first-child')
        .attr('class', 'stackv-graphic')
        .style('width', `${width}px`)
        .style('height', `${height}px`)
        .style('margin', 'auto');

    this.svg.root = this.svg.outerWrapper
        .insert('svg', ':first-child')
        .attr('width', this.size.width)
        .attr('height', this.size.height)
        .call(this.zoom);

    this.svg.container = this.svg.root
        .append('g')
        .attr('class', 'container');

    if (layoutPersistent) {
      layoutPersistent.applyZoomLevel(this);
    }

    this.svg.hulls = this.svg.container.append('g')
        .attr('class', 'hulls')
        .selectAll('.convex-hull');
    this.svg.nodes = this.svg.container.append('g')
        .attr('class', 'nodes')
        .selectAll('.node-wrapper');
    this.svg.services = this.svg.container.append('g')
        .attr('class', 'services')
        .selectAll('.node-services');
    this.svg.links = this.svg.container.append('g')
        .attr('class', 'links')
        .selectAll('.node-links');

    this.forceConfig.centerForce = d3.forceCenter(width / 2, height / 2);
    this.forceConfig.centerRadialForce = d3.forceRadial(100, width / 2, height / 2);

    const nodeDragEventHandler = new SVDragEvent({
      dragStart: this.nodeDragStart,
      dragMove: this.nodeDragMove,
      dragEnd: this.nodeDragEnd,
    });
    this.eventHandlers.node.drag = d3.drag()
        .on('start', nodeDragEventHandler.dragStartEvent())
        .on('drag', nodeDragEventHandler.dragMoveEvent())
        .on('end', nodeDragEventHandler.dragEndEvent());

    const self = this;
    const hullDragEventHandler = new SVDragEvent({
      dragStart: function (d) {
        self.hullDragStart(d, d3.mouse(this))
      },
      dragMove: this.hullDragMove,
      dragEnd: this.hullDragEnd,
    });
    this.eventHandlers.hull.drag = d3.drag()
        .on('start', hullDragEventHandler.dragStartEvent())
        .on('drag', hullDragEventHandler.dragMoveEvent())
        .on('end', hullDragEventHandler.dragEndEvent());

    this.force = d3.forceSimulation()
        .force('link', d3.forceLink()
            .distance(d => d.metadata.length)
            .strength(d => d.metadata.strength))
        .force('charge', d3.forceManyBody())
        .force('collision', d3.forceCollide().radius(d => d.metadata.renderConfig['collisionRadius']))
        .force('center', this.forceConfig.centerForce)
        .on('end', this.tickEnd)
        .on('tick', this.ticked);

    // set up hooks
    const forceRestart = this.force.restart;
    this.force.restart = () => {
      forceRestart();
      this.startRendering();
    };

    const forceStop = this.force.stop;
    this.force.stop = () => {
      forceStop();
      this.stopRendering();
    };

    this.persistence = persistence;

    this.nodeDetailController = new NodeDetailController(this);

    this.highlighter = new NodeHighlighter(this);

    $detailsModal.iziModal(detailsConfig);
  }

  restart = () => {
    let {nodes, links, hulls} = this.dataModel.parse();

    this.svg.hulls = this.svg.hulls.data(hulls);
    this.svg.hulls.exit().remove();
    this.svg.hulls = this.svg.hulls.enter()
        .append('path')
        .attr('class', 'convex-hull')
        .style('fill', () => HULL_COLOR(Math.random()))
        .on('click', (d) => {
            d3.event.preventDefault();
            d3.event.stopPropagation();

            console.log(d.hullNodes);
            this.loadModalData(d);
            if ($detailsModal.iziModal("getState") === "closed") {
                $detailsModal.iziModal("open");
            }
        })
        .on('dblclick', (d) => {
          d3.event.preventDefault();
          d3.event.stopPropagation();
          console.log(`(DBLCLICK) Selected Node ${d.id}`, d);

          if (this.canExpandByHull(d)) {
            this.toggleNodeByHull(d);
          }
        })
        .on('contextmenu', (d) => {
            d3.event.preventDefault();
            d3.event.stopPropagation();
            this.showPopover(d.id);
            $("#input-clipbook-text").val(d.id);
            openClipbookAdd();
        })
        .call(this.eventHandlers.hull.drag)
        .merge(this.svg.hulls);

    this.svg.links = this.svg.links.data(links);
    this.svg.links.exit().remove();
    this.svg.links = this.svg.links.enter()
        .append('line')
        .attr('class', 'node-link')
        .merge(this.svg.links);
    this.svg.links.style('display', d => {
      return (d.metadata && d.metadata.required) ? '' : 'none';
    });

    this.svg.nodes = this.svg.nodes.data(nodes);
    this.svg.nodes.exit().remove();

    let forceNodeEnter = this.svg.nodes.enter()
        .append('g')
        .attr('class', 'node-wrapper')
        .on('click', (d) => {
            d3.event.preventDefault();
            d3.event.stopPropagation();

            this.loadModalData(d);
            if ($detailsModal.iziModal("getState") === "closed") {
                $detailsModal.iziModal("open");
            }
        })
        .on('dblclick', (d) => {
          d3.event.preventDefault();
          d3.event.stopPropagation();
          console.log(`(DBLCLICK) Selected Node ${d.id}`, d);

          if (this.canExpand(d)) {
            this.toggleNode(d);
          }
        })
        .on('contextmenu', (d) => {
            d3.event.preventDefault();
            d3.event.stopPropagation();

            $("#input-clipbook-text").val(d.metadata.id);
            openClipbookAdd();
        })
        .call(this.eventHandlers.node.drag);
    forceNodeEnter.append('path')
        .style('fill', d => {
          d.fill = NODE_COLOR(Math.random());
          return d.fill;
        })
        .on('click', ({ metadata: { id } }) => {
          console.log(id);
          this.highlighter.toggleGlobal(id);
        });
    this.svg.nodes = forceNodeEnter.merge(this.svg.nodes);

    this.svg.nodes.select('path')
        .attr('d', d => {
          return this.canExpand(d) ? getIcon('cloud').d : getIcon('server').d;
        })
        .style('transform', d => {
          return this.canExpand(d) ? getIcon('cloud').transform : getIcon('server').transform;
        })
        .style('fill', d => d.fill)
        .style('display', d => {
          if (this.dataModel.isNodeExpanded(d)) {
            return 'none';
          }
          else {
            return '';
          }
        });

    this.force.nodes(nodes);
    this.force.force('link').links(links);
    this.force.alpha(0.3).restart();
  };

  /**
   * Callback method for D3 SVG Zoom
   */
  zoomed = () => {
    this.svg.container.attr('transform', d3.event.transform);
    this.applyLayoutPersistence();
    // remove all switch
    this.nodeDetailController && this.nodeDetailController.hideAllPopover();
  };

  /**
   * force layout tick
   */
  ticked = () => {
    // speed-up rendering
    for (let i = 0; i < this.renderSpeed; i++) {
      this.force.tick();
    }

    this.svg.hulls.data(this.dataModel.updateHullCoordinate())
        .attr('d', d => d.path);

    this.svg.links.attr('x1', (d) => d.source.x)
        .attr('y1', (d) => d.source.y)
        .attr('x2', (d) => d.target.x)
        .attr('y2', (d) => d.target.y);

    this.svg.nodes.attr('transform', (d) => `translate(${d.x}, ${d.y})`);
  };

  /**
   * force layout tickEnd
   */
  tickEnd = () => {
    this.renderSpeed = DEFAULT_RENDER_SPEED;

    // "FREEZE" node
    this.svg.nodes.attr('transform', d => {
      d['fx'] = d.x;
      d['fy'] = d.y;
      return `translate(${d.x}, ${d.y})`;
    });

    // apply fist-time only operations
    if (!this.renderState.initialTickEnd) {
      this.renderState.initialTickEnd = true;
      // Changer to Radial Force to prevent collision of Hulls
      this.force = this.force.force('center', this.forceConfig.centerRadialForce);
    }

    this.stopRendering();

    // execute UI tasks in queue
    const newTask = this.renderQueue.pop();
    newTask && newTask.run && newTask.run();
  };

  /**
   * Preserved hook when D3 force layout starts
   */
  startRendering = () => {
    console.log('RENDER START');
  };

  /**
   * Preserved hook when D3 force layout stops
   */
  stopRendering = () => {
    console.log('RENDER END');
    this.applyLayoutPersistence();
  };

  /**
   * Start the layout persistence
   *
   * @param {boolean} force - Force save layout to localStorage
   */
  applyLayoutPersistence = (force = false) => {
    if (this.persistence || force) {
      const layoutPersistent = new LayoutPersistent();
      layoutPersistent.backup(this);
      const backupData = layoutPersistent.parse();

      localStorage.setItem(LP_LOCALSTORAGE_KEY, backupData);
    }
  };

  /**
   * Ensure that NO ACTION is performed when D3 force layout is still simulating
   *
   * @returns {boolean}
   */
  canRequestRender = () => {
    return this.renderQueue.length === 0 &&
        !this.renderState.nodeDragStarted &&
        !this.renderState.hullDragStarted &&
        !this.renderState.globalRendering &&
        this.renderState.initialTickEnd;
  };

  /**
   * Tell if a node can expand by interacting with the hull
   *
   * @param {object} hullElement - hull object
   * @returns {boolean} if a node can be expanded
   */
  canExpandByHull = (hullElement) => {
    return this.canExpand(this.dataModel.nodeFetcher(hullElement.id, true));
  };

  /**
   * Tell if a node can expand by interacting with the node
   *
   * @param {object} nodeWrapper - need to pass in the nodeWrapper
   * @returns {boolean} if a node can be expanded
   */
  canExpand = ({metadata: node}) => {
    return node && (node.hasOwnProperty('hasNode') || node.hasOwnProperty('hasTopology'));
  };

  /**
   * Expand a node by interacting with its hull
   *
   * @param {object} hullElement - hull object
   */
  toggleNodeByHull = (hullElement) => {
    this.toggleNode(this.dataModel.nodeFetcher(hullElement.id, true));
  };

  /**
   * Expand a node by interacting with node
   *
   * @param {object} nodeWrapper - node wrapper object
   */
  toggleNode = (nodeWrapper) => {
    if (this.canRequestRender()) {
      if (this.dataModel.isNodeExpanded(nodeWrapper)) {
        // IF ALREADY EXPAND, WE SHRINK
        this.nodeDetailController.markSwitchHidden(nodeWrapper.metadata.id);
        this.dataModel.shrinkNode(nodeWrapper);
        this.restart();
      }
      else {
        // WE EXPAND!!
        if (!nodeWrapper.dragged && !nodeWrapper.draggedNotGroup) {
          this.renderSpeed = 15;
          this.dataModel.prepareSpace(nodeWrapper);
          this.restart();

          this.renderQueue.push({
            type: 'DELAYED_EXPAND_RENDERING',
            run: () => {
              // restore expand space
              this.dataModel.prepareSpaceDone(nodeWrapper);

              this.dataModel.expandNode(nodeWrapper);
              this.restart();
            },
          });
        }
        else {
          this.dataModel.expandNode(nodeWrapper);
          this.restart();
        }
      }
    }
  };

  /**
   * The drag_start callback when a Node is being dragged
   */
  nodeDragStart = () => {
    if (this.canRequestRender()) {
      this.force.alphaTarget(0.3).restart();
      this.renderState.nodeDragStarted = true;
    }
  };

  /**
   * The drag_move callback when a Node is being dragged
   */
  nodeDragMove = (d) => {
    if (this.renderState.nodeDragStarted) {
      d.fx = d3.event.x;
      d.fy = d3.event.y;
    }
  };

  /**
   * The drag_end callback when a Node is being dragged
   */
  nodeDragEnd = (d) => {
    if (this.renderState.nodeDragStarted) {
      this.force.alphaTarget(0);
      this.force.stop();

      // assign permanent x, y
      d.x = d.fx;
      d.y = d.fy;

      // Clean force velocity, prevent bounce
      d.vx = 0;
      d.vy = 0;

      // mark it is being dragged
      d.dragged = true;
      d.draggedNotGroup = true;
    }
    this.renderState.nodeDragStarted = false;
  };

  /**
   * The drag_start callback when a Hull is being dragged
   */
  hullDragStart = (d, mousePosition) => {
    if (this.canRequestRender()) {
      this.force.alphaTarget(0.3).restart();
      this.renderState.hullDragStarted = true;
      // record initial coordinate
      this.renderState.hullDragStartedPosition.x = mousePosition[0];
      this.renderState.hullDragStartedPosition.y = mousePosition[1];

      d.physicalNodes.forEach(node => {
        node = this.dataModel.nodeFetcher(node.id, true);

        // record original position
        node.__orig_x__ = node.fx || node.x;
        node.__orig_y__ = node.fy || node.y;
      });

      // hide all popover for node
      this.hidePopover(d.id);
    }
  };

  /**
   * The drag_move callback when a Hull is being dragged
   */
  hullDragMove = (d) => {
    if (this.renderState.hullDragStarted) {
      const deltaX = this.renderState.hullDragStartedPosition.x - d3.event.x;
      const deltaY = this.renderState.hullDragStartedPosition.y - d3.event.y;

      d.physicalNodes.forEach(node => {
        node = this.dataModel.nodeFetcher(node.id, true);

        const x = node.__orig_x__ - deltaX;
        const y = node.__orig_y__ - deltaY;

        node.fx = x;
        node.fy = y;
      });
    }
  };

  /**
   * The drag_end callback when a Hull is being dragged
   */
  hullDragEnd = (d) => {
    if (this.renderState.hullDragStarted) {
      this.force.alphaTarget(0);
      this.force.stop();

      d.physicalNodes.forEach(node => {
        node = this.dataModel.nodeFetcher(node.id, true);

        delete node.__orig_x__;
        delete node.__orig_y__;

        // assign permanent x, y
        node.x = node.fx;
        node.y = node.fy;

        // Clean force velocity, prevent bounce
        node.vx = 0;
        node.vy = 0;
        // mark it is being dragged
        node.dragged = true;
      });

      this.renderState.hullDragStarted = false;
    }
  };

  showPopover = (nodeId) => {
    this.nodeDetailController.show(nodeId);
  };

  hidePopover = (nodeId) => {
    this.nodeDetailController.hide(nodeId);
  };

    /**
    * Store panel construction recipes
    */
    buildBook = (data, tag) => {
        switch(tag) {
            default:
            let $panel = $('<div class="panel panel-default">');
            $panel.append('<div class="panel-heading"><h3 class="panel-title">' + tag + '</h3></div>');
            let $list = $('<div class="list-group">');
            for (var port of data[tag]) {
                $list.append('<a class="list-group-item">' + port + '</a>')
            }

            $panel.append($list);
            return $panel;
        }
    };

    /*
    * Load in modal data according to node type and contents.
    */
    loadModalData = (d) => {
        // Processing.
        const $data = $("#details-modal-data");
        $data.html(null);
        if ("hullNodes" in d) {
            // Hull node
            $detailsModal.find(".iziModal-header-title").text(d.id);
            $detailsModal.attr("current", d.id)
        }
        else {
            // Standard node
            let data = d.metadata;
            $detailsModal.find(".iziModal-header-title").text(data.id);
            $detailsModal.attr("current", data.id)

            switch (data.type) {
                default:
                // hasBidirectionalPort
                if ("hasBidirectionalPort" in data) {
                    $data.append(this.buildBook(data, "hasBidirectionalPort"));
                }
                // hasService
                if ("hasService" in data) {
                    $data.append(this.buildBook(data, "hasService"));
                }
                break;
            }
        }
    };
}

export default VisualModel;
