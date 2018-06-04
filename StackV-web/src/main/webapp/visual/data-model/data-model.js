import _ from 'lodash';

import * as Utils from './utils';
import ServerData from './server-data/server-data';
import VersionControl from './version-control/version-control';
import * as Constants from './consts';


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
   * Version controller
   * @type {VersionControl}
   */
  versionControl = new VersionControl();

  /**
   * Start a new DataModel instance
   *
   * @param {object} initialServerData - An object contains all Node Info
   */
  constructor(initialServerData) {
    // Ensure context
    this.nodeFetcher = this.nodeFetcher.bind(this);
    // The procedure of initialization is actually a PATCH_UPDATE to Empty Data
    this.update(initialServerData, true);
  }

  /**
   * Provide new data
   * @param {object} latestServerData - latest server raw data
   * @param {boolean} initial - Initial data
   * @returns {Array<string>} List of removed node id
   */
  update(latestServerData, initial = false) {
    const parsedLatestResult = ServerData.parse(latestServerData);

    let changed = true;
    let removedIdList = [];

    // speed up initialization process
    if (!initial) {
      const diffData = this.versionControl.diff(parsedLatestResult);
      changed = diffData.changed;
      removedIdList = diffData.remove;
    }

    let newNodeIdList = [];

    if (changed) {
      // save version
      this.versionControl.updateVersion(parsedLatestResult);
      // get NEW expand IDs
      const latestExpandedKeys = Object.keys(this.expandInfo).filter(d => removedIdList.indexOf(d) === -1);
      // update node data
      for (let nodeId in parsedLatestResult.serverData) {
        const nodeData = parsedLatestResult.serverData[nodeId];
        if (this.data.hasOwnProperty(nodeId)) {
          // updating a EXIST node
          _.assign(this.data[nodeId].metadata, nodeData.metadata);
        }
        else {
          this.data[nodeId] = nodeData;
          newNodeIdList.push(nodeId)
        }
      }
      // remove deleted node
      if (removedIdList.length > 0) {
        removedIdList.forEach(nodeId => {
          delete this.data[nodeId];
          delete parsedLatestResult.topLevel[nodeId];
        });
      }

      // Calculate and assign Top-Level nodes
      this.nodes = Utils.prepareData(this.data, parsedLatestResult.topLevel, this.nodeFetcher);

      // calculate influenced EXPANDED NODES
      for (let nodeId in this.expandInfo) {
        if (this.expandInfo.hasOwnProperty(nodeId)) {
          /**
           * Check if the children node of current expanded node
           * INCLUDE NEW NODEs
           * or SOME NODEs INSIDE it was being REMOVED
           *
           * If this happens, REMOVE ALL layout data of that PARENT NODE
           * In case the force layout is being MESSED UP after RE-RENDERING
           */
          const nodeData = this.nodeFetcher(nodeId);
          if (nodeData) {
            const currentChildrenNodeIdList = this.expandInfo[nodeId].nodes.map(d => d.id);
            let latestChildrenNodeIdList = [];

            if (this.canExpand(nodeData)) {
              latestChildrenNodeIdList = this.childrenElementIdList(nodeData);
            }
            // Check if NEW NODE joined
            const newNodeIdList = _.difference(latestChildrenNodeIdList, currentChildrenNodeIdList);
            // Check if OLD NODE removed
            const removedNodeIdList = _.difference(currentChildrenNodeIdList, latestChildrenNodeIdList);

            if (newNodeIdList.length > 0 || removedNodeIdList.length > 0) {
              // if the children list CHANGED
              latestChildrenNodeIdList.forEach(nodeId => {
                Utils.cleanForceLayoutData(this.nodeFetcher(nodeId, true));
              });
            }
          }
        }
      }
      // REMOVE ALL EXPAND INFO
      this.expandInfo = {};
      // Calculate relations for latest data
      this.calculateGraphicData();
      // Restore expand state
      latestExpandedKeys.forEach(nodeId => this.expandNode(this.nodeFetcher(nodeId, true)));
    }
    return removedIdList;
  }

  /**
   * Return children elements of node
   * @param {object} nodeData - REAL node data
   * @returns {Array<string>} List of REAL NODE id
   */
  childrenElementIdList(nodeData) {
    let nodeList = [];
    Constants.EXPAND_KEYS.forEach(expandKeyName => {
      if (nodeData.hasOwnProperty(expandKeyName)) {
        if (Array.isArray(nodeData[expandKeyName])) {
          nodeData[expandKeyName].forEach(d => nodeList.push(d));
        }
      }
    });
    return nodeList;
  }

  /**
   * Tell if a node can expand by interacting with the node
   *
   * @param {object} node - need to pass in the REAL node info
   * @returns {boolean} if a node can be expanded
   */
  canExpand(node) {
    let canExpand = false;
    Constants.EXPAND_KEYS.forEach(keyName => {
      if (node.hasOwnProperty(keyName)) {
        canExpand = true;
      }
    });
    return canExpand;
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
    Utils.updateHullCoordinate(this.hulls, this.nodeFetcher);
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
    return nodeInfo == null ? null : (pretty ? nodeInfo : nodeInfo.metadata);
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
    if (nodeWrapper) {
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
      links: [...this.links],
      hulls: [...this.hulls],
    };

    this.nodes.forEach(d => data.nodes.push(this.nodeFetcher(d.id, true)));

    return data;
  }
}

export default DataModel;