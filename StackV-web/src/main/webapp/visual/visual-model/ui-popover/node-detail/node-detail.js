import NodeDetailController from "./controller";


/**
 * ABSTRACT CLASS
 *
 * Manage Node Detail popover(s)
 */
class NodeDetail {
  /**
   * Linked nodeId
   * @type {string}
   */
  nodeId;
  /**
   * Correspondent Hull Data
   * @type {object}
   */
  hullData;
  /**
   * Correspondent Node data
   * @type {object}
   */
  nodeData;
  /**
   * Linked Controller
   * @type {NodeDetailController}
   */
  controller;
  /**
   * D3 Element for the Dialog Container
   */
  container;

  /**
   * Construct data
   *
   * @param {string} nodeId - linked REAL node id
   * @param {NodeDetailController} controller - parent controller instance
   */
  constructor(nodeId, controller) {
    this.nodeId = nodeId;
    this.controller = controller;
    this.container = null;

    this.controller.svg.hulls.each((data) => {
      if (data.id === this.nodeId) {
        this.hullData = data;
        this.nodeData = this.controller.nodeFetcher(data.id, true);
      }
    });

    if (new.target === NodeDetail) {
      throw new TypeError('NodeDetail is a abstract class, DO NOT CONSTRUCT');
    }
  }

  /**
   * Show port popover for nodeId
   * Construct HTML dialog
   *
   * @public
   */
  show() {
    if (!this.container) {
      if (this.dataValid()) {
        // Ask for dialog coordinate
        const absPoint = this.popoverCoordinate();

        this.container = this.controller.switchContainer.append('div')
            .attr('id', this.containerId())
            .attr('class', this.containerClass())
            .style('left', `${absPoint.x}px`)
            .style('top', `${absPoint.y}px`)
            .style('min-width', this.containerWidth());

        const buttonContainer = this.container.append('div').attr('class', 'ui-buttons');
        buttonContainer.append('div').attr('class', 'close-x').on('click', () => {
          this.hide(this.nodeId);
        });

        const content = this.container.append('div').attr('class', this.contentClass());

        this.prepareContent(content);

        // Done constructing, show it
        this.container.transition()
            .duration(250)
            .style('opacity', 1);
      }
    }
  }

  /**
   * Show port popover for nodeId
   * WARN: NEED TO REPORT AS HIDDEN to controller
   *
   * @abstract
   */
  hide() {
    if (this.container) {
      this.container.transition()
          .duration(250)
          .style('opacity', 0)
          .on('end', () => this.container.remove());
    }
  }

  /**
   * Expecting a absolute (abs to div.stackv-graphic) coordinate (TOP-LEFT POINT) for the dialog
   *
   * @abstract
   * @protected
   * @returns {{ x: Number, y: Number }} The xy coordinate
   */
  popoverCoordinate() {
    throw new Error('Abstract function NOT OVERRIDDEN!!');
  };

  /**
   * Expecting a HTML unique ID for the dialog container
   * Should contain aliasId
   *
   * @example
   * return `switch-${aliasId}`;
   * @example
   * return `services-${aliasId}`;
   *
   * @abstract
   * @returns {string} dialog container ID
   */
  containerId() {
    throw new Error('Abstract function NOT OVERRIDDEN!!');
  };

  /**
   * Expecting a HTML className for the dialog container
   *
   * @example
   * return 'switch-container';
   *
   * @abstract
   * @protected
   * @returns {string} dialog className
   */
  containerClass() {
    throw new Error('Abstract function NOT OVERRIDDEN!!');
  };

  /**
   * Expecting a Minimum Width for the CONTENT wrapper inside dialog container
   *
   * @example
   * return 'switch-ports';
   *
   * @abstract
   * @protected
   * @returns {string} className for CONTENT wrapper
   */
  contentClass() {
    throw new Error('Abstract function NOT OVERRIDDEN!!');
  }

  /**
   * Expecting a Minimum Width STRING for the dialog container
   *
   * @example
   * return '150px';
   *
   * @abstract
   * @protected
   * @returns {string} min-width property value
   */
  containerWidth() {
    throw new Error('Abstract function NOT OVERRIDDEN!!');
  }

  /**
   * Verify if the hullData and nodeData is valid for creating the dialog
   *
   * @abstract
   * @protected
   * @returns {boolean} this.hullData this.nodeData VALID or NOT
   */
  dataValid() {
    throw new Error('Abstract function NOT OVERRIDDEN!!');
  }

  /**
   * APPEND new children into the content container
   *
   * @param {object} content - D3 selection result
   * @abstract
   * @protected
   */
  prepareContent(content) {
    throw new Error('Abstract function NOT OVERRIDDEN!!');
  }
}

export default NodeDetail;