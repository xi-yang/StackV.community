import _ from 'lodash';

import * as Utils from './utils';

/**
 * The function is trying to clean up the server side data for local DataModel
 *
 * The function would do these following steps
 *
 *
 * 1. Assign NODE-ID for better reference in Array
 * 2. Remove useless URL in KEY NAME
 * 3. Remove useless ONE-ELEMENT-ARRAY
 *
 * Few examples
 *
 * [1. ASSIGN-NODE-ID] NOTE: This function CHANGE the pass-in object, and RETURN NOTHING
 *
 * For example:
 * {
 *   "aws-asia-s3": {
 *     "isAlias": "aws-na-s3",
 *     "hasNode": [ ... ]
 *   }
 * }
 *
 * We would want to assign the ID to the node information body
 *
 * {
 *   "aws-asia-s3": {
 *     "id": "aws-asia-s3",  // I have an ID now, YAY
 *     "isAlias": "aws-na-s3",
 *     "hasNode": [ ... ]
 *   }
 * }
 */
class ServerData {
  /**
   * Actually parse the server data for local Data Model, WE MIGHT MODIFY THE PASSED IN OBJECT
   *
   * @param {object} serverData - Javascript Object directly parsed from HTTP API request from StackV server
   * @param {object} options - Extra parsing options (Not-Currently implemented)
   * @returns {{ topLevel: Object.<string, object>, serverData: object }}
   */
  static parse(serverData) {
    serverData = _.cloneDeep(serverData);
    // preserve TOP_LEVEL Node data
    let topLevelNodeIdList = Object.keys(serverData);
    let serverDataKeyStructure = {};

    let realServerData = {};

    topLevelNodeIdList.forEach(topLevelNodeId => {
      if (serverData[topLevelNodeId]) {
        const innerData = JSON.parse(serverData[topLevelNodeId].json);
        realServerData = {
          ...realServerData,
          ...innerData,
        };
        serverDataKeyStructure[topLevelNodeId] = Object.keys(innerData);
        serverDataKeyStructure[topLevelNodeId] = Object.keys(innerData);
      }
      else {
        serverDataKeyStructure[topLevelNodeId] = [];
      }
    });

    let versionRecord = {};

    topLevelNodeIdList.forEach(topLevelId => {
      if (serverData[topLevelId] != null) {
        versionRecord[topLevelId] = {
          time: serverData[topLevelId].time,
          uuid: serverData[topLevelId].uuid,
        };
      }
      else {
        versionRecord[topLevelId] = {
          time: -1,
          uuid: '30624700-30624770-534202-13942-43140624',  // dummy uuid
        };
      }
    });

    ServerData._parseHelper(realServerData);
    return {
      topLevel: serverDataKeyStructure,
      serverData: ServerData._simplifyFormat(realServerData),
      versionRecord: versionRecord,
    };
  }

  /**
   * Parse the server data recursively
   *
   * @param {object} serverData - Javascript Object directly parsed from HTTP API request from StackV server
   * @private
   */
  static _parseHelper(serverData) {
    /**
     * Server data must in this FORMAT
     * {
     *   "NODE_ID_1": { ... },
     *   "NODE_ID_2": { ... },
     * }
     */
    if (serverData !== null && typeof(serverData) === 'object') {
      for (let keyName in serverData) {
        if (serverData.hasOwnProperty(keyName)) {
          // recursively parse data
          ServerData._parseHelper(serverData[keyName]);

          // parse current object
          if (Utils.shouldAssignNodeId(keyName)) {
            // Assign NODE-ID (KEY NAME) to node information body
            serverData[keyName].id = keyName;
          }

          // remove weird keyName like http://schema.org/hasNode
          if (Utils.containSchemaURL(keyName)) {
            const newKeyName = Utils.removeSchemeURL(keyName);
            serverData[newKeyName] = serverData[keyName];
            delete serverData[keyName];
            keyName = newKeyName;
          }

          if (Utils.containSchemaURL(serverData[keyName])) {
            serverData[keyName] = Utils.removeSchemeURL(serverData[keyName]);
          }

          // remove ONE-ELEMENT array, assign array[0] to top-level object
          if (Utils.shouldRemoveOneElementArray(keyName)) {
            // double check if the content is Array
            if (Array.isArray(serverData[keyName])) {
              if (serverData[keyName].length === 0) {
                serverData[keyName] = null;
              }
              else {
                serverData[keyName] = serverData[keyName][0];
              }
            }
          }

          if (typeof(serverData[keyName]) === 'object' && !Array.isArray(serverData[keyName])) {
            if (serverData[keyName].hasOwnProperty('value') && serverData[keyName].hasOwnProperty('type')) {
              if (Object.keys(serverData[keyName]).length === 2) {
                if (serverData[keyName].type === 'uri' || serverData[keyName].type === 'bnode' || serverData[keyName].type === 'literal') {
                  serverData[keyName] = serverData[keyName].value;
                }
                if (serverData[keyName].type === 'bnode') {
                  // @todo implement the node reference for TAG NODES
                }
              }
            }
          }
        }
      }
    }
  }

  /**
   * Generate new format nodes
   *
   * @param {object} serverData - parsed server data
   * @returns {object} KEY -> VALUE pair of new format nodes
   * @private
   */
  static _simplifyFormat(serverData) {
    let data = {};
    for (let keyName in serverData) {
      if (serverData.hasOwnProperty(keyName)) {
        data[keyName] = {
          id: _.uniqueId('node_'),
          metadata: serverData[keyName],
        };
      }
    }
    return data;
  }
}

export default ServerData;