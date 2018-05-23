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
    for (let key in this.persistentData.nodeInfo) {
      if (this.persistentData.nodeInfo.hasOwnProperty(key)) {
        if (dataModel.data.hasOwnProperty(key)) {
          _.assign(dataModel.data[key], this.persistentData.nodeInfo[key]);
        }
      }
    }

    this.persistentData.expandInfo.forEach(nodeId => {
      dataModel.expandNode(dataModel.nodeFetcher(nodeId, true));
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

  read(backupData, compress = true) {
    if (compress) {
      backupData = LZString.decompress(backupData);
    }
    this.persistentData = JSON.parse(backupData);
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