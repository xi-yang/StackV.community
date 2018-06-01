import _ from 'lodash';

import * as Utils from './utils';
import ServerData from './server-data/server-data';


/**
 * Front-end Data Model engine, provide API for further D3 rendering
 */
class DataModel {
  /**
   * Local data field
   * @type {object}
   */
  data = {};
  /**
   * Current nodes in view hierarchy
   * @type {Array<object>}
   */
  nodes = [];
  /**
   * Current available nodes
   * @type {Array<object>}
   */
  links = [];
  /**
   * Current background Convex Hulls Data
   * @type {Array<object>}
   */
  hulls = [];
  /**
   * Record the current expansion status
   * @type {object}
   */
  expandInfo = {};
  /**
   * Start a new DataModel instance
   *
   * @param {object} localData - A object contains all Node Info
   * @param {object} options - Extra parsing options (Not-Currently implemented)
   */
  constructor(localData, options = {}) {
    if (options['deepCopy']) {
      console.log('deepCopy');
      localData = _.cloneDeep(localData);
    }
    const parseResult = ServerData.parse(localData);

    this.data = parseResult.serverData;

    // Ensure context
    this.nodeFetcher = this.nodeFetcher.bind(this);

    // Calculate and assign Top-Level nodes
    this.nodes = Utils.prepareData(this.data, parseResult.topLevel, this.nodeFetcher);

    // Calculate relations for first time
    this.calculateGraphicData();
  }

  /**
   * Calculate Links and Hulls based on current nodes
   */
  calculateGraphicData() {
    // Reset Links and Hulls to prevent weird relations
    this.links = [];
    this.hulls = [];

    Utils.generateCentroidLinks(this.nodes, this.links, this.nodeFetcher);
    Utils.calculateVisibleLinks(this.nodes, this.links, this.hulls, this.expandInfo, this.nodeFetcher);
    this.updateHullCoordinate();
  }

  /**
   * Update hull coordinate for path generation
   * @returns {Array<object>} new hull info
   */
  updateHullCoordinate() {
    Utils.updateHullCoordinate(this.hulls, this.nodeFetcher);
    return this.hulls;
  }

  /**
   * Return a node reference with nodeId
   *
   * @param {string} nodeId - The REAL node id
   * @param {boolean} pretty - If pretty is true, RETURN node wrapper instead of node
   * @return {object|undefined} The node info
   */
  nodeFetcher(nodeId, pretty = false) {
    const nodeInfo = this.data[nodeId];
    return nodeInfo == null ? null : ( pretty ? nodeInfo : nodeInfo.metadata );
  }

  /**
   * See if the node is already expanded
   *
   * @param {string|object} any - nodeId or node reference
   * @returns {boolean} If the node is already expanded
   */
  isNodeExpanded(any) {
    let nodeId;
    if (typeof(any) === 'object') {
      nodeId = any.metadata.id;
    }
    else {
      nodeId = any;
    }
    return this.expandInfo.hasOwnProperty(nodeId);
  }

  /**
   * Expand node with id
   *
   * @param {object} nodeWrapper - node wrapper reference
   * @returns {boolean} if the expansion success
   */
  expandNode(nodeWrapper) {
    const nodeId = nodeWrapper.metadata.id;
    // ensure the node is NOT expanded
    if (!this.isNodeExpanded(nodeId)) {
      const targetNode = this.nodeFetcher(nodeId);
      if (targetNode) {
        Utils.expandNode(targetNode, this.nodes, this.expandInfo, this.nodeFetcher);
        this.calculateGraphicData();
      }
      return true;
    }
    return false;
  }

  /**
   * Shrink node with id
   *
   * @param {object} nodeWrapper - node wrapper reference
   * @returns {boolean} if the shrink success
   */
  shrinkNode(nodeWrapper) {
    const nodeId = nodeWrapper.metadata.id;
    // ensure the node is expanded
    if (this.isNodeExpanded(nodeId)) {
      const targetNode = this.nodeFetcher(nodeId);
      if (targetNode) {
        Utils.shrinkNode(targetNode, this.nodes, this.expandInfo, this.nodeFetcher);
        this.calculateGraphicData();
      }
      return true;
    }
    return false;
  }

  /**
   * Prepare space for node to expand
   *
   * @param {object} nodeWrapper - node wrapper reference
   */
  prepareSpace(nodeWrapper) {
    if (nodeWrapper) {
      Utils.startSpacePrepare(nodeWrapper, this.nodes);
    }
  }

  /**
   * Remove the large collisionRadius after preparation
   */
  prepareSpaceDone() {
    Utils.endSpacePrepare(this.nodes);
  }

  /**
   * Generate d3 purpose data
   * @returns {{nodes: Array<object>, links: Array<object>, hulls: Array<object>}} D3 render data
   */
  parse() {
    const data = {
      nodes: [],
      links: [ ...this.links ],
      hulls: [ ...this.hulls ],
    };

    this.nodes.forEach(d => data.nodes.push(this.nodeFetcher(d.id, true)));

    return data;
  }
}

export default DataModel;