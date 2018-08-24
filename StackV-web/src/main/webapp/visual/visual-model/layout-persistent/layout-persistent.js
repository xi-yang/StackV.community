import * as d3 from 'd3';

import _ from 'lodash';
import { LAYOUT_PERSISTENT_KEYS } from './consts';
import * as LZString from 'lz-string';


/**
 * This class would take control of all the Layout Persistent stuff
 */
class LayoutPersistent {
  persistentData = {
    expandInfo: [],
    nodeInfo: {},
    zoom: {},
  };
  /**
   * The nodes listed here will be expanded AFTER RENDERING
   * @type {Array<object>}
   */
  postRenderExpandNodeWrapperList;
  /**
   * receive a visual model instance and create backup
   * @param {VisualModel} visualModel - visual model instance
   */
  backup(visualModel) {
    const dataModel = visualModel.dataModel;
    this.persistentData.expandInfo = Object.keys(dataModel.expandInfo);

    for (let key in dataModel.data) {
      if (dataModel.data.hasOwnProperty(key)) {
        const nodePersistentData = getPersistentData(dataModel.data[key]);
        if (nodePersistentData) {
          this.persistentData.nodeInfo[key] = nodePersistentData;
        }
      }
    }

    // backup zoom level
    this.persistentData.zoom = d3.zoomTransform(visualModel.svg.root.node());
  }

  /**
   * Apply backup data into current visual model and data model
   * @param {VisualModel} visualModel - visual model instance
   */
  apply(visualModel) {
    const dataModel = visualModel.dataModel;

    const staticExpandNodeWrapperList = [];
    this.postRenderExpandNodeWrapperList = [];
    /**
     * If New Nodes being added in Expanded Hulls
     * We have no Coordinate backup for New Nodes
     * And these New Nodes will float everywhere
     *
     * In order to prevent this, check if all nodes inside a Expanded Node have Coordinate Backup
     * If not, then remove all backup Coordinate of children nodes under that Expanded Node
     *
     * Node being treated will EXPAND in view layer, since it need to prepare space
     *
     * REVERSE TRAVEL prevent UI being messed up
     */
    for (let idx = this.persistentData.expandInfo.length - 1; idx >= 0; idx -= 1) {
      const nodeId = this.persistentData.expandInfo[idx];
      const nodeWrapper = dataModel.nodeFetcher(nodeId, true);

      if (nodeWrapper) {
        if (nodeWrapper.metadata) {
          const childrenIdList = dataModel.childrenElementIdList(nodeWrapper.metadata);

          let haveAllBackup = true;
          for (let i = 0; i < childrenIdList.length; i++) {
            const innerNodeId = childrenIdList[i];

            if (!this.persistentData.nodeInfo.hasOwnProperty(innerNodeId)) {
              // if one of all has NO backup
              haveAllBackup = false;
              break;
            }
          }

          if (!haveAllBackup) {
            childrenIdList.forEach(innerNodeId => delete this.persistentData.nodeInfo[innerNodeId]);
            delete this.persistentData.nodeInfo[nodeId];
            this.postRenderExpandNodeWrapperList.push(nodeWrapper);
          }
          else {
            staticExpandNodeWrapperList.push(nodeWrapper);
          }
        }
      }
    }

    for (let key in this.persistentData.nodeInfo) {
      if (this.persistentData.nodeInfo.hasOwnProperty(key)) {
        if (dataModel.data.hasOwnProperty(key)) {
          _.assign(dataModel.data[key], this.persistentData.nodeInfo[key]);
        }
      }
    }

    staticExpandNodeWrapperList.forEach(nodeWrapper => dataModel.expandNode(nodeWrapper));
  }

  /**
   * Expand nodes after first rendering
   *
   * @param {VisualModel} visualModel - visual model instance
   */
  applyPostRenderExpansion(visualModel) {
    visualModel.renderQueue.push({
      type: 'POST_RENDER_EXPANSION',
      run: () => {
        this.postRenderExpandNodeWrapperList.forEach(nodeWrapper => visualModel.toggleNode(nodeWrapper, true));
        this.postRenderExpandNodeWrapperList = [];
      },
    });
  }

  /**
   * Restore zoom level into current visualModel
   * @param {VisualModel} visualModel - visual model instance
   */
  applyZoomLevel(visualModel) {
    console.log('restore');
    visualModel.svg.root = visualModel.svg.root
        .call(visualModel.zoom.transform, d3.zoomIdentity
            .translate(this.persistentData.zoom.x, this.persistentData.zoom.y)
            .scale(this.persistentData.zoom.k));
    visualModel.svg.container.attr('transform',
        `translate(${this.persistentData.zoom.x}, ${this.persistentData.zoom.y}) scale(${this.persistentData.zoom.k})`);
  }

  /**
   * Export current backup into COMPRESSED STRING or JSON string
   * @param {boolean} compress - Need to compress using LZW or not
   * @returns {string}
   */
  parse(compress = true) {
    let backupData = JSON.stringify(this.persistentData);
    if (compress) {
      backupData = LZString.compress(backupData);
    }
    return backupData;
  }

  /**
   * Read the local persistent data
   * @param {string} backupData
   * @param {boolean} compress
   */
  read(backupData, compress = true) {
    if (compress) {
      backupData = LZString.decompress(backupData);
    }
    const parseResult = JSON.parse(backupData);
    if (this.validate(parseResult)) {
      this.persistentData = parseResult;
    }
  }

  /**
   * Validate if parse result is legal
   * @param {object} parseResult - parse result
   */
  validate() {
    return true;
  }
}

/**
 * parse persistent data
 *
 * @param {object} node - node reference
 * @returns {object}
 */
function getPersistentData(node) {
  let persistentData = {};

  for (let key in node) {
    if (node.hasOwnProperty(key)) {
      if (LAYOUT_PERSISTENT_KEYS.indexOf(key) !== -1) {
        persistentData[key] = node[key];
      }
    }
  }

  return Object.keys(persistentData).length > 0 ? persistentData : null;
}

export default LayoutPersistent;