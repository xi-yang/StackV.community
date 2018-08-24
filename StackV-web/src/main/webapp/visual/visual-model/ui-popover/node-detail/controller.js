import NodeSwitch from './node-switch';
import NodeServices from './node-services';


/**
 * Manage OPEN & CLOSE of NodeDetail
 */
class NodeDetailController {
  /**
   * Record open and close list for PORTS
   * @type {Object.<string, NodeSwitch>}
   */
  switchList = {};
  /**
   * record open and close list for SERVICES
   * @type {Object.<string, NodeServices>}
   */
  serviceList = {};
  /**
   * SVG elements
   * @type {object}
   */
  svg;
  /**
   * Node Fetcher Function
   * @type {Function}
   */
  nodeFetcher;
  /**
   * parent visual Model
   * @type {VisualModel}
   */
  visualModel;
  /**
   * D3 Element for Network Switch Container
   * @type {object}
   */
  switchContainer;

  /**
   * @param {VisualModel} visualModel - parent visual Model
   */
  constructor(visualModel) {
    this.visualModel = visualModel;
    this.svg = visualModel.svg;
    this.nodeFetcher = visualModel.dataModel.nodeFetcher;
    // create node
    const container = this.svg.outerWrapper.append('div').attr('class', 'popover-container');
    this.switchContainer = container.append('div').attr('class', 'popover-switches');
  }

  /**
   * Show network switch popover for nodeId
   * @param {string} nodeId - nodeId string
   */
  showNetworkSwitch(nodeId) {
    if (!this.switchList.hasOwnProperty(nodeId)) {
      const networkSwitch = new NodeSwitch(nodeId, this);
      networkSwitch.show();
      this.switchList[nodeId] = networkSwitch;
    }
  }

  /**
   * Hide network switch popover for nodeId
   * @param {string} nodeId - nodeId string
   */
  hideNetworkSwitch(nodeId) {
    if (!this.switchList.hasOwnProperty(nodeId)) {
      const networkSwitch = new NodeSwitch(nodeId, this);
      networkSwitch.hide();
    }
  }

  /**
   * Mark network switch popover HIDDEN for nodeId
   * @param {string} nodeId - nodeId string
   */
  markSwitchHidden(nodeId) {
    if (this.switchList.hasOwnProperty(nodeId)) {
      delete this.switchList[nodeId];
    }
  }

  /**
   * Batch hide all current network switch
   */
  hideAllNetworkSwitch() {
    for (let nodeId in this.switchList) {
      if (this.switchList.hasOwnProperty(nodeId)) {
        this.switchList[nodeId].hide();
      }
    }
  }

  /**
   * Show services dialog popover for nodeId
   * @param {string} nodeId - nodeId string
   */
  showServicesDialog(nodeId) {
    if (!this.serviceList.hasOwnProperty(nodeId)) {
      const servicesDialog = new NodeServices(nodeId, this);
      servicesDialog.show();
      this.serviceList[nodeId] = servicesDialog;
    }
  }

  /**
   * Hide services dialog popover for nodeId
   * @param {string} nodeId - nodeId string
   */
  hideServicesDialog(nodeId) {
    if (!this.serviceList.hasOwnProperty(nodeId)) {
      const servicesDialog = new NodeServices(nodeId, this);
      servicesDialog.hide();
    }
  }

  /**
   * Mark services dialog popover HIDDEN for nodeId
   * @param {string} nodeId - nodeId string
   */
  markServicesDialogHidden(nodeId) {
    if (this.serviceList.hasOwnProperty(nodeId)) {
      delete this.serviceList[nodeId];
    }
  }

  /**
   * Batch hide all current Services Dialog
   */
  hideAllServicesDialog() {
    for (let nodeId in this.serviceList) {
      if (this.serviceList.hasOwnProperty(nodeId)) {
        this.serviceList[nodeId].hide();
      }
    }
  }

  /**
   * Batch hide Services Dialog & Network Switch
   */
  hideAllPopover() {
    this.hideAllNetworkSwitch();
    this.hideAllServicesDialog();
  }

  /**
   * Show all available dialogs for node
   * @param {string} nodeId - nodeId string
   */
  show(nodeId) {
    this.showNetworkSwitch(nodeId);
    this.showServicesDialog(nodeId);
  }

  /**
   * Hide all available dialogs for node
   * @param {string} nodeId - nodeId string
   */
  hide(nodeId) {
    this.hideNetworkSwitch(nodeId);
    this.hideServicesDialog(nodeId);
  }
}

export default NodeDetailController;