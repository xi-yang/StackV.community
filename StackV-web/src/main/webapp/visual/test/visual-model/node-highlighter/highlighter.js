import * as d3 from 'd3';


/**
 * Highlight node by their Node ID
 */
class NodeHighlighter {
  /**
   * Reference to parent visual model
   * @type {VisualModel}
   */
  visualModel;
  /**
   * Record travel history of each search, prevent stack overflow
   * @type {Array<string>}
   */
  visitHistory;
  /**
   * The current Global Highlight Node ID
   * @type {string}
   */
  currentHighlightId;
  /**
   * Create new instance for the highlighter
   * @param {VisualModel} visualModel - parent visualModel
   */
  constructor(visualModel) {
    this.visualModel = visualModel;
  }

  /**
   * Toggle a global highlight for a node, ONLY ONE CAN EXIST AT A TIME
   * @param {string} nodeId - node REAL id
   */
  toggleGlobal(nodeId) {
    if (nodeId !== this.currentHighlightId) {
      this.currentHighlightId = nodeId;
    }
    this.toggle(this.currentHighlightId);
  }

  /**
   * Toggle a global highlight for a node
   * @param {string} nodeId - node REAL id
   */
  toggle(nodeId) {
    const targetNode = this.searchNode(nodeId);
    if (targetNode.classed('user-highlight')) {
      this.hide(nodeId);
    }
    else {
      this.show(nodeId);
    }
  }

  /**
   * Highlight a node with nodeId
   * @param {string} nodeId - node REAL id
   */
  show(nodeId) {
    const targetNode = this.searchNode(nodeId);
    if (targetNode) {
      targetNode.classed('user-highlight', true);
    }
  }

  /**
   * Dismiss a highlight with nodeId
   * @param {string} nodeId - node REAL id
   */
  hide(nodeId) {
    const targetNode = this.searchNode(nodeId);
    if (targetNode) {
      targetNode.classed('user-highlight', false);
    }
  }

  /**
   * Find HTML Node for highlight purpose
   * @param {string} nodeId - node REAL id
   */
  searchNode(nodeId) {
    this.visitHistory = [];
    return d3.select(this.searchNode_helper(nodeId));
  }

  /**
   * RECURSION HELPER Find HTML Node for highlight purpose
   * @param {string} nodeId - node REAL id
   */
  searchNode_helper(nodeId) {
    if (this.visitHistory.indexOf(nodeId) === -1) {
      this.visitHistory.push(nodeId);
      // search in NODE_LIST

      let returnNode;
      this.visualModel.svg.nodes.each(function(d) {
        if (returnNode) return true;
        if (d.metadata && d.metadata.id === nodeId) {
          // FOUND
          returnNode = d3.select(this).node();
        }
      });

      if (returnNode) return returnNode;

      // search in NETWORK SWITCH
      const nodeData = this.visualModel.dataModel.nodeFetcher(nodeId, true);
      if (nodeData) {
        const { id: aliasId, metadata: data } = nodeData;

        if (aliasId) {
          this.visitHistory.push(aliasId);

          let nodeElement = document.getElementById(`switch-${aliasId}`);

          if (nodeElement) {
            return nodeElement;
          }
          else {
            nodeElement = document.getElementById(`switch-port-${aliasId}`);
            if (nodeElement) {
              return nodeElement;
            }
            else {
              if (data.hasOwnProperty('directParentList')) {
                // Reverse travel
                for (let i = data['directParentList'].length - 1; i >= 0; i--) {
                  const realNodeId = data['directParentList'][i];
                  const nodeElement = this.searchNode_helper(realNodeId);
                  if (nodeElement) return nodeElement;
                }

                return this.searchNode_helper(data['isAlias']);
              }
            }
          }
        }
      }
    }
    return false;
  }
}

export default NodeHighlighter;