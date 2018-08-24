import _ from "lodash";


/**
 * Version Control Manager for dynamic updating
 */
class VersionControl {
  /**
   * Storage space for all current versions
   * @type {object.<string, object>}
   */
  versionRecord = {};
  /**
   * Current top-level node structure
   * @type {Object.<string, Array<string>>}
   */
  currentNodeStructure = {};
  /**
   * Compare new data and old data, return CHANGED keys
   *
   * @param {object} newParsedResult - parsed server data
   * @returns {{changed: boolean, remove: Array<string>}} List of CHANGED node REAL id
   */
  diff(newParsedResult) {
      let changed = false;
      let removedIdList = [];

      Object.keys(newParsedResult.topLevel).forEach(topLevelId => {
          const latestVersionRecord = newParsedResult.versionRecord[topLevelId];
          const currentVersionRecord = this.versionRecord[topLevelId];

          if (!currentVersionRecord || (latestVersionRecord.time > currentVersionRecord.time &&
          latestVersionRecord.uuid !== currentVersionRecord.uuid)) {
              // mark changed
              changed = true;
              // latest node ID list under `topLevelId`
              const latestIdList = newParsedResult.topLevel[topLevelId];
              // current node ID list under `topLevelId`
              const currentIdList = this.currentNodeStructure[topLevelId];
              // diff 2 array get REMOVED NODE LIST
              const diffArray = _.difference(currentIdList, latestIdList);
              diffArray.forEach(d => removedIdList.push(d));
          }

          if (latestVersionRecord.time === -1 && this.currentNodeStructure[topLevelId]) {
              // If the node is being completely removed
              this.currentNodeStructure[topLevelId].forEach(d => removedIdList.push(d));
          }
      });

      return {
          changed,
          remove: removedIdList,
      };
  }

  /**
   * Save new node structure
   *
   * @param {object} newParsedResult - parsed server data
   * @returns {{change: Array<string>, remove: Array<string>}} List of CHANGED node REAL id
   */
  updateVersion(newParsedResult) {
      _.assign(this.currentNodeStructure, newParsedResult.topLevel);
      _.assign(this.versionRecord, newParsedResult.versionRecord);
  }
}

export default VersionControl;
