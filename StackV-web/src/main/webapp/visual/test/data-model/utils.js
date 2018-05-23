import * as Constants from './consts';
import * as d3 from 'd3';

/**
 * Put all new render attributes into sub-key
 *
 * @param {object} node - node reference
 * @param {string} key - node attr key
 * @param {*} value - node attr value
 */
function setNodeAttribute(node, key, value) {
  if (!node.hasOwnProperty('renderConfig')) {
    node['renderConfig'] = {};
  }
  node['renderConfig'][key] = value;
}

/**
 * Calculate TOP-LEVEL nodes
 *
 * @param {object|Array} nodeList - Clean-up-ed node data, [ NODE ] or { NODE_ID: NODE }
 * @param {Function} nodeFetcher - Function(nodeId: string): Object
 * @returns {Array<object>} The top-level node list
 */
export function calculateTopLevelNodes(nodeList, nodeFetcher) {
  /**
   * Each time children request a node, will record it
   * If any node is not requested, it is top-level node
   * @type {object}
   */
  let referenceRecorder = {};

  /**
   * How this wrapper works:
   *
   * For a topology, if any node is not refer by any other node, it is considered as TopNode
   *
   * NOTE: Backward refer is not calculated
   *
   * @param nodeId - node id
   * @returns {object} node reference
   */
  let wrappedNodeFetcher = (nodeId) => {
    referenceRecorder[nodeId] = true;
    return nodeFetcher(nodeId);
  };
  calculateTopLevelNodes_helper(nodeList, wrappedNodeFetcher);

  let viewHierarchyNodeList = [];
  for (let keyName in nodeList) {
    if (nodeList.hasOwnProperty(keyName)) {
      const node = nodeList[keyName].metadata;

      /**
       * Assign directParentList
       */
      for (let keyName in node) {
        if (node.hasOwnProperty(keyName)) {
          if (Array.isArray(node[keyName])) {
            if (Constants.RECURSION_BLACK_LIST_KEY_NAME_LIST.indexOf(keyName) === -1) {
              const nodeList = node[keyName];
              for (let i = 0; i < nodeList.length; i++) {
                // console.log('query', nodeList[i]);
                const childNode = nodeFetcher(nodeList[i]);

                if (childNode) {
                  if (!childNode.hasOwnProperty('directParentList') || !Array.isArray(childNode['directParentList'])) {
                    childNode['directParentList'] = [];
                  }
                  if (childNode['directParentList'].indexOf(node.id) === -1) {
                    childNode['directParentList'].push(node.id);
                  }

                  if (keyName === 'hasBidirectionalPort') {
                    if (!childNode.hasOwnProperty('isPortOf') || !Array.isArray(childNode['isPortOf'])) {
                      childNode['isPortOf'] = [];
                    }
                    if (childNode['isPortOf'].indexOf(node.id) === -1) {
                      childNode['isPortOf'].push(node.id);
                    }
                  }
                }
              }
            }
          }
        }
      }

      /**
       * Remove hasBidirectionalPort nodes from aggregateChildren
       */
      if (node.hasOwnProperty('hasBidirectionalPort') && node.hasOwnProperty('aggregateChildren')) {
        node['aggregateChildren'] = node['aggregateChildren'].filter(d => {
          return node['hasBidirectionalPort'].indexOf(d) === -1;
        });
      }

      /**
       * Assign default safe radius for all
       */
      setNodeAttribute(node, 'collisionRadius', Constants.DEFAULT_COLLISION_RADIUS);

      if (!referenceRecorder.hasOwnProperty(node.id)) {
        const topLevelNode = node;
        /**
         * MARK TOP-LEVEL-NODE
         */
        setNodeAttribute(topLevelNode, 'top', true);

        viewHierarchyNodeList.push(topLevelNode);
      }
      else {
        /**
         * No Links inside Top-Level nodes
         */
        delete node['aggregateChildren'];
      }
    }
  }

  for (let keyName in nodeList) {
    if (nodeList.hasOwnProperty(keyName)) {
      const node = nodeList[keyName].metadata;
      /**
       * BACK-UP INITIAL DATA
       */
      node['__orig__'] = _.cloneDeep(node);
    }
  }

  return viewHierarchyNodeList;
}

/**
 * Calculate TOP-LEVEL nodes recursion helper
 *
 * Basically, when we access the Top-Level node, we cannot see the children (children's children) node
 * However, we need to build Top-Level node, AND show the relationship
 * So, assign all children nodes to parent node
 *
 * @param {object} mysteryNode - JSON / Object server data
 * @param {Function} nodeFetcher - Function(nodeId: string): Object
 * @returns {Array<object>} Aggregated data for recursion purpose
 * @private
 */
function calculateTopLevelNodes_helper(mysteryNode, nodeFetcher) {
  // NOTE: Would accept both Object AND Array
  if (mysteryNode !== null && typeof(mysteryNode) === 'object') {
    if (mysteryNode.hasOwnProperty('metadata')) {
      mysteryNode = mysteryNode.metadata;
    }
    if (!mysteryNode.hasOwnProperty('id')) {
      // Non Node object, might be an Array or outer Map
      let aggregateChildren = [];
      for (let keyName in mysteryNode) {
        if (mysteryNode.hasOwnProperty(keyName)) {
          /**
           * MYSTERY NODE DATA
           *
           * Since we say this is a mystery data, because it can be
           *
           * 1. Node Id - String (When serverData is an OUTER MAP)
           * 2. Node Reference - Object (When serverData is an Array)
           *
           */
          let mysteryNodeData = mysteryNode[keyName];

          if (typeof(mysteryNodeData) === 'string') {
            // Turn Node Id -> Node Reference
            mysteryNodeData = nodeFetcher(mysteryNodeData);
          }

          mergeAggregateChildren(aggregateChildren, calculateTopLevelNodes_helper(mysteryNodeData, nodeFetcher));
        }
      }
      return aggregateChildren;
    }
    else {
      // Inside a Node object
      let aggregateChildren = [];

      for (let keyName in mysteryNode) {
        if (mysteryNode.hasOwnProperty(keyName)) {
          if (Array.isArray(mysteryNode[keyName])) {
            if (Constants.RECURSION_BLACK_LIST_KEY_NAME_LIST.indexOf(keyName) === -1) {
              mergeAggregateChildren(aggregateChildren, mysteryNode[keyName]);
              mergeAggregateChildren(aggregateChildren, calculateTopLevelNodes_helper(mysteryNode[keyName], nodeFetcher));
            }
          }
        }
      }

      /**
       * We also want to assign parent node, to generate visualization nodes
       */
      for (let i = 0; i < aggregateChildren.length; i++) {
        const node = nodeFetcher(aggregateChildren[i]);
        if (node) {
          if (!node.hasOwnProperty('parentList') || !Array.isArray(node['parentList'])) {
            node['parentList'] = [];
          }
          // ensure no duplicate
          if (node['parentList'].indexOf(mysteryNode.id) === -1) {
            // console.log(mysteryNode);
            node['parentList'].push(mysteryNode.id);
          }
        }
      }

      mysteryNode['aggregateChildren'] = aggregateChildren;
      return aggregateChildren;
    }
  }
}

/**
 * Merge extraNodeList into aggregateChildren, without DUPLICATION
 *
 * @param {Array<object>} aggregateChildren - will merge extra nodes into this Array
 * @param {Array<object>} extraNodeIdList - extra nodes
 * @private
 */
function mergeAggregateChildren(aggregateChildren, extraNodeIdList) {
  if (extraNodeIdList) {
    for (let i = 0; i < extraNodeIdList.length; i++) {
      const extraNodeId = extraNodeIdList[i];
      if (typeof(extraNodeId) === 'string' && aggregateChildren.indexOf(extraNodeId) === -1) {
        aggregateChildren.push(extraNodeId);
      }
    }
  }
}

/**
 * If there is no relations b/t nodes, it would float randomly, so we create dummy
 * Links to add force to the center of EXPANDED CLUSTERS
 *
 * @param {Array<object>} nodes - The list of current nodes
 * @param {Array<object>} links - Would modify and push new links to this array
 * @param {Function} nodeFetcher - Function(nodeId: string): Object
 */
export function generateCentroidLinks(nodes, links, nodeFetcher) {
  nodes.forEach(node => {
    if (node.hasOwnProperty('directParentList')) {
      // loop over every direct parent
      node['directParentList'].forEach(directParentNodeId => {
        const parentNode = nodeFetcher(directParentNodeId);
        if (parentNode) {
          // ensure parent node is in view hierarchy
          if (nodes.indexOf(parentNode) !== -1) {
            links.push({
              source: nodeFetcher(node.id, true),
              target: nodeFetcher(parentNode.id, true),
              metadata: {
                sourceNode: node,
                targetNode: parentNode,
                centroid: true,
                required: false,
                length: 15,
                strength: 1,
              }
            });
          }
        }
      });
    }
  });
}

/**
 * Generate convex hull coordinate, MUST CALL EVERY TICK
 *
 * @param {Array<object>} hulls - Would modify and generate hull curve to this array
 * @param {Function} nodeFetcher - Function(nodeId: string): Object
 */
export function updateHullCoordinate(hulls, nodeFetcher) {
  function validate(arr) {
    // arr.forEach(d => isNaN(d) ? console.log('NON NUM') : null );
    return arr;
  }
  hulls.forEach(hullInfo => {
    const hullPathInfo = [];

    for (let i = 0; i < hullInfo.hullNodes.length; i++) {
      const d = nodeFetcher(hullInfo.hullNodes[i].id, true);
      hullPathInfo.push(validate([ d.x - Constants.HULL_OFFSET, d.y - Constants.HULL_OFFSET ]));
      hullPathInfo.push(validate([ d.x - Constants.HULL_OFFSET, d.y + Constants.HULL_OFFSET ]));
      hullPathInfo.push(validate([ d.x + Constants.HULL_OFFSET, d.y - Constants.HULL_OFFSET ]));
      hullPathInfo.push(validate([ d.x + Constants.HULL_OFFSET, d.y + Constants.HULL_OFFSET ]));
    }

    if (hullInfo.hasOwnProperty('children')) {
      for (let i = 0; i < hullInfo.children.length; i++) {
        const d = nodeFetcher(hullInfo.children[i].id, true);
        hullPathInfo.push(validate([ d.x - 2 * Constants.HULL_OFFSET, d.y - 2 * Constants.HULL_OFFSET ]));
        hullPathInfo.push(validate([ d.x - 2 * Constants.HULL_OFFSET, d.y + 2 * Constants.HULL_OFFSET ]));
        hullPathInfo.push(validate([ d.x + 2 * Constants.HULL_OFFSET, d.y - 2 * Constants.HULL_OFFSET ]));
        hullPathInfo.push(validate([ d.x + 2 * Constants.HULL_OFFSET, d.y + 2 * Constants.HULL_OFFSET ]));
      }
    }

    const path = d3.polygonHull(hullPathInfo);
    hullInfo['__path_raw__'] = path;
    hullInfo['path'] = Constants.HULL_CURVE(path);
  });
}

/**
 * Calculate visible links AND generate convex hulls
 *
 * @param {Array<object>} nodes - The list of current nodes
 * @param {Array<object>} links - Would modify and push new links to this array
 * @param {Array<object>} hulls - Would modify and push new hulls to this array
 * @param {object} expandInfo - The expanded nodes info
 * @param {Function} nodeFetcher - Function(nodeId: string): Object
 */
export function calculateVisibleLinks(nodes, links, hulls, expandInfo, nodeFetcher) {
  nodes.forEach(node => {
    /**
     * This code block will build the link for CHILDREN ONLY, NOT IT-SELF
     */
    if (node.hasOwnProperty('aggregateChildren')) {
      for (let j = 0; j < node['aggregateChildren'].length; j++) {
        const childNode = nodeFetcher(node['aggregateChildren'][j]);
        generateLinkForNode(node, childNode, nodes, links, nodeFetcher, expandInfo);
      }
    }

    /**
     * This code block will build the link for Ports Only
     */
    if (node.hasOwnProperty('hasBidirectionalPort')) {
      for (let j = 0; j < node['hasBidirectionalPort'].length; j++) {
        const childNode = nodeFetcher(node['hasBidirectionalPort'][j]);
        generateLinkForNode(node, childNode, nodes, links, nodeFetcher, expandInfo, childNode);
      }
    }

    /**
     * BUILD LINK FOR IT SELF
     */
    generateLinkForNode(node, node, nodes, links, nodeFetcher, expandInfo);
  });

  /**
   * build initial hulls
   */
  for (let nodeId in expandInfo) {
    if (expandInfo.hasOwnProperty(nodeId)) {
      const expandData = expandInfo[nodeId];
      let hullInfo = {
        id: nodeId,
        hullNodes: [ ...expandData.nodes ],
        physicalNodes: allChildrenNode(nodeId, nodeFetcher, expandInfo),
      };
      if (expandData.hasOwnProperty('children')) {
        hullInfo['children'] = [];
        for (let i = 0; i < expandData['children'].length; i++) {
          const childId = expandData['children'][i];
          hullInfo['children'] = [
            ...hullInfo['children'],
            ...expandInfo[childId]['nodes'],
            nodeFetcher(childId),
          ];
        }
      }
      hulls.push(hullInfo);
    }
  }
}

/**
 * List all children node for a OPEN node
 *
 * @param {string} nodeId - node id
 * @param {Function} nodeFetcher - Function(nodeId: string): Object
 * @param {object} expandInfo - expand info in Data Model
 * @returns {Array<object>} A list of all sub nodes
 * @private
 */
function allChildrenNode(nodeId, nodeFetcher, expandInfo) {
  const nodeList = [ nodeFetcher(nodeId) ];
  allChildrenNode_helper(nodeId, nodeFetcher, expandInfo, nodeList);
  return nodeList;
}

/**
 * Helper function to list all children node for a OPEN node
 *
 * @param {string} nodeId - node id
 * @param {Function} nodeFetcher - Function(nodeId: string): Object
 * @param {object} expandInfo - expand info in Data Model
 * @param {object} nodeList - all children node list
 * @private
 */
function allChildrenNode_helper(nodeId, nodeFetcher, expandInfo, nodeList) {
  const expandData = expandInfo[nodeId];
  if (expandData && expandData.nodes) {
    expandData.nodes.forEach(d => {
      nodeList.push(d);
      allChildrenNode_helper(d.id, nodeFetcher, expandInfo, nodeList);
    });
  }
}

/**
 * This function will try to find all links from childNode -> ANY NODE IN VIEW HIERARCHY
 * And will modify the links in the argument
 *
 * @param {object} parentNode - node reference
 * @param {object} realSourceNode - node reference REAL SOURCE
 * @param {Array<object>} nodes - current top-level nodes in view hierarchy
 * @param {Array<object>} links - link pool for d3
 * @param {Function} nodeFetcher - Function(nodeId: string): Object
 * @param {object} expandInfo - expand info in Data Model
 * @param {object=} overrideRealSourceNode - Provide and override source node for TOP-LEVEL nodes, but PREPARE as non-proxy link
 * @private
 */
function generateLinkForNode(parentNode, realSourceNode, nodes, links, nodeFetcher, expandInfo, overrideRealSourceNode = null) {
  // If there is possible links
  if (realSourceNode && realSourceNode.hasOwnProperty('isAlias')) {
    // check if target node is in current view hierarchy
    const realTargetNode = nodeFetcher(realSourceNode['isAlias']);
    if (realTargetNode) {
      const [ topLevelNodeId, isProxyLink ] = isInViewHierarchy(realTargetNode, nodes);
      if (topLevelNodeId) {
        /**
         * Hurray! We found a Link
         *
         * We render the link ONLY IF they are not from the SAME PARENT NODE
         */
        const linkSourceNodeId = parentNode.id;
        const linkTargetNodeId = topLevelNodeId;

        if (linkSourceNodeId !== linkTargetNodeId) {
          console.log(`Found Link ${linkSourceNodeId} -> ${linkTargetNodeId}`);

          const sourceNode = nodeFetcher(linkSourceNodeId);
          const targetNode = nodeFetcher(linkTargetNodeId);

          if (!hasLink(links, sourceNode, targetNode)) {
            let sourceExpanded = expandInfo.hasOwnProperty(sourceNode.id);
            let targetExpanded = expandInfo.hasOwnProperty(targetNode.id);

            let sourceProxy = overrideRealSourceNode ? false : parentNode !== realSourceNode;
            let targetProxy = isProxyLink;

            if (!overrideRealSourceNode) {
              realSourceNode = nodeFetcher(linkSourceNodeId);
            }

            let sourceRequired = !sourceExpanded || !sourceProxy;
            let targetRequired = !targetExpanded || !targetProxy;
            let linkRequired = sourceRequired && targetRequired;

            // const sourceSize = childrenCount(sourceNode, expandInfo);
            // const targetSize = childrenCount(targetNode, expandInfo);

            let linkSize = 100;

            // let newLinkSize = linkLength(sourceSize) + linkLength(targetSize);
            // if (newLinkSize > linkSize) {
            //   linkSize = newLinkSize;
            // }

            const linkInfo = {
              source: nodeFetcher(sourceNode.id, true),
              target: nodeFetcher(targetNode.id, true),
              metadata: {
                sourceNode,
                targetNode,
                sourceProxy,
                targetProxy,
                required: linkRequired,
                length: linkSize,
                strength: 0.1
              },
            };

            if (sourceProxy) {
              linkInfo['metadata']['realSourceNode'] = realSourceNode;
            }

            if (overrideRealSourceNode) {
              linkInfo['metadata']['realSourceNode'] = overrideRealSourceNode;
            }

            if (targetProxy) {
              linkInfo['metadata']['realTargetNode'] = realTargetNode;
            }

            links.push(linkInfo);
          }
        }
      }
    }
  }
}


/**
 * Detect if same link already exists in link pool, prevent adding duplicated line (performance purpose)
 *
 * @param links {Array<object>} links - Link pool
 * @param {object} sourceNode - source node reference
 * @param {object} targetNode - target node reference
 * @returns {boolean} Return TRUE if link already exists in link pool
 * @private
 */
function hasLink(links, sourceNode, targetNode) {
  for (let i = 0; i < links.length; i++) {
    const linkInfo = links[i];
    if (linkInfo.source === sourceNode && linkInfo.target === targetNode) {
      return true;
    }
    else if (linkInfo.source === targetNode && linkInfo.target === sourceNode) {
      return true;
    }
  }
  return false;
}

/**
 * This function will try to compare the node['parentList'] Array with current Top-Level Nodes,
 * If they have the same element, so the node and its link should be visible
 *
 * ** WHAT IS PROXY LINK
 *
 * 1. It means that sourceNode -> targetNode is not declared, but it is indicated by children
 *
 * @param {object} targetNode - node reference
 * @param {Array<object>} nodes - current top-level nodes
 * @returns {Array<string|null|boolean>} Will return the target top-level id AND if it is proxy link
 * @private
 */
function isInViewHierarchy(targetNode, nodes) {
  const nodeIdList = nodes.map(d => d.id);

  let targetTopLevelId = null;
  let isProxyLink = true;

  if (nodeIdList.indexOf(targetNode.id) !== -1) {
    // TARGET NODE IS IN VIEW HIERARCHY
    targetTopLevelId = targetNode.id;
    isProxyLink = false;
  }

  if (isProxyLink) {
    for (let i = 0; i < nodeIdList.length; i++) {
      const nodeId = nodeIdList[i];
      if (targetNode.hasOwnProperty('parentList') && targetNode['parentList'].indexOf(nodeId) !== -1) {
        /**
         * WE PICK THE FINAL ONE
         * SINCE IT CAN BE THE EXPANDED NODE
         */
        targetTopLevelId = nodeId;
        isProxyLink = true;
      }
    }
  }

  /**
   * See if target node is a hardware node of targetTopLevelNode
   */
  if (targetNode.hasOwnProperty('isPortOf') && Array.isArray(targetNode['isPortOf'])) {
    if (targetNode['isPortOf'].indexOf(targetTopLevelId) !== -1) {
      isProxyLink = false;
    }
  }

  return [ targetTopLevelId, isProxyLink ];
}

/**
 * Expand a node to view hierarchy
 *
 * @param {object} targetNode - node reference
 * @param {Array<object>} nodes - node list, would modify
 * @param {object} expandInfo - expand info
 * @param {Function} nodeFetcher - nodeFetcher(nodeId: string): Object
 */
export function expandNode(targetNode, nodes, expandInfo, nodeFetcher) {
  setNodeAttribute(targetNode, 'expand', false);

  console.log(targetNode);

  // back-up original position
  if (!targetNode.hasOwnProperty('__original_point_backup__')) {
    targetNode['__original_point_backup__'] = {
      x: targetNode.fx,
      y: targetNode.fy,
    };
  }

  expandNodeToViewHierarchy(targetNode, nodes, expandInfo, nodeFetcher);
}

/**
 * This method will expand (join) all possible nodes to the current view hierarchy
 *
 * @param {object} targetNode - node reference
 * @param {Array<object>} nodes - current view hierarchy
 * @param {object} expandInfo - expand info
 * @param {Function} nodeFetcher - Function(nodeId: string): Object
 */
function expandNodeToViewHierarchy(targetNode, nodes, expandInfo, nodeFetcher) {
  let newNodeList = [];
  Constants.EXPAND_KEYS.forEach(expandKeyName => {
    if (targetNode.hasOwnProperty(expandKeyName)) {
      const nodeList = targetNode[expandKeyName];

      for (let i = 0; i < nodeList.length; i++) {
        const extraNode = nodeFetcher(nodeList[i]);
        if (extraNode && nodes.indexOf(extraNode) === -1) {
          nodes.push(extraNode);
          newNodeList.push(extraNode);
        }
      }
    }
  });

  expandInfo[targetNode.id] = {
    id: targetNode.id,
    nodes: newNodeList,
  };
  /**
   * We want to include children group to parent hull, we check now
   */
  if (targetNode.hasOwnProperty('parentList')) {
    for (let i = 0; i < targetNode['parentList'].length; i++) {
      const parentId = targetNode['parentList'][i];
      if (expandInfo.hasOwnProperty(parentId)) {
        if (!expandInfo[parentId].hasOwnProperty('children')) {
          expandInfo[parentId]['children'] = [];
        }
        if (expandInfo[parentId]['children'].indexOf(targetNode.id) === -1) {
          expandInfo[parentId]['children'].push(targetNode.id);
        }
      }
    }
  }
}

/**
 * Shrink a node from view hierarchy
 *
 * @param {object} targetNode - node reference
 * @param {Array<object>} nodes - node list, would modify
 * @param {object} expandInfo - expand info
 * @param {Function} nodeFetcher - nodeFetcher(nodeId: string): Object
 */
export function shrinkNode(targetNode, nodes, expandInfo, nodeFetcher) {
  // remove HOLD state
  // back-up original space
  const uiNode = nodeFetcher(targetNode.id, true);
  if (uiNode.hasOwnProperty('__original_point_backup__')) {
    uiNode.fx = uiNode['__original_point_backup__'].x;
    uiNode.fy = uiNode['__original_point_backup__'].y;

    uiNode.x = uiNode['__original_point_backup__'].x;
    uiNode.y = uiNode['__original_point_backup__'].y;

    uiNode.vx = 0;
    uiNode.vy = 0;

    delete uiNode['__original_point_backup__'];
  }

  setNodeAttribute(targetNode, 'expand', false);

  shrinkNodeFromViewHierarchy(targetNode, nodes, nodeFetcher, expandInfo);
}

/**
 * This method will remove all possible nodes from the current view hierarchy
 *
 * @param {object} targetNode - node reference
 * @param {Array<object>} nodes - current node list
 * @param {Function} nodeFetcher - Function(nodeId: string): Object
 * @param {object} expandInfo - expand info
 * @returns {Array<object>} new node list array
 * @private
 */
function shrinkNodeFromViewHierarchy(targetNode, nodes, nodeFetcher, expandInfo) {
  let needToRemoveNodeList = [];

  // remove children
  recursiveRemoveExpandedNode(targetNode.id, expandInfo, needToRemoveNodeList);

  // check if any node mark it as children
  for (let keyName in expandInfo) {
    if (expandInfo.hasOwnProperty(keyName)) {
      if (expandInfo[keyName].hasOwnProperty('children')) {
        expandInfo[keyName]['children'] = expandInfo[keyName]['children'].filter(d => {
          return d !== targetNode.id;
        });
      }
    }
  }

  delete expandInfo[targetNode.id];

  /**
   * IMPORTANT: Remove force layout parameters, it will stop the next expansion
   */
  needToRemoveNodeList.forEach(d => cleanForceLayoutData(d, nodeFetcher));

  for (let i = nodes.length - 1; i >= 0; i -= 1) {
    const d = nodes[i];
    if (needToRemoveNodeList.indexOf(d) !== -1) {
      nodes.splice(i, 1);
    }
  }
}

/**
 * Remove all sub-children of current node
 *
 * @param {string} currentNodeId - node id
 * @param {object} expandInfo - expand info from Data Model
 * @param {Array<object>} needToRemoveNodeList - A list of unused nodes
 * @private
 */
function recursiveRemoveExpandedNode(currentNodeId, expandInfo, needToRemoveNodeList) {
  console.log(currentNodeId);
  const currentNodeExpandInfo = expandInfo[currentNodeId];

  if (currentNodeExpandInfo.hasOwnProperty('children')) {
    currentNodeExpandInfo.children.forEach(childId => {
      recursiveRemoveExpandedNode(childId, expandInfo, needToRemoveNodeList);
    });
  }

  expandInfo[currentNodeId].nodes.forEach(d => {
    needToRemoveNodeList.push(d);
  });
  delete expandInfo[currentNodeId];
}

/**
 * Restore the node back to its original keys, DELETE all d3.forceLayout keys
 *
 * @param {object} nodeWrapper - node reference
 * @param {Function} nodeFetcher - node fetcher
 * @private
 */
function cleanForceLayoutData(nodeWrapper, nodeFetcher) {
  nodeWrapper = nodeFetcher(nodeWrapper.id, true);

  for (let keyName in nodeWrapper) {
    if (nodeWrapper.hasOwnProperty(keyName)) {
      if (Constants.D3_GRAPHIC_KEYS.indexOf(keyName) !== -1) {
        delete nodeWrapper[keyName];
      }
    }
  }
}

/**
 * Restore state after space expansion
 *
 * @param {object} visibleNodes - All visible nodes on SVG
 */
export function endSpacePrepare(visibleNodes) {
  for (let keyName in visibleNodes) {
    if (visibleNodes.hasOwnProperty(keyName)) {
      const d = visibleNodes[keyName];
      setNodeAttribute(d, 'collisionRadius', Constants.DEFAULT_COLLISION_RADIUS);
    }
  }
}

/**
 * Start prepare space for expansion
 *
 * @param {object} nodeWrapper - node reference
 * @param {object} visibleNodes - All visible nodes on SVG
 */
export function startSpacePrepare(nodeWrapper, visibleNodes) {
  // back-up original position
  if (!nodeWrapper.hasOwnProperty('__original_point_backup__')) {
    nodeWrapper['__original_point_backup__'] = {
      x: nodeWrapper.x,
      y: nodeWrapper.y,
    };
  }

  // clean fixed position
  nodeWrapper.fx = null;
  nodeWrapper.fy = null;

  for (let keyName in visibleNodes) {
    if (visibleNodes.hasOwnProperty(keyName)) {
      const d = visibleNodes[keyName];
      setNodeAttribute(d, 'collisionRadius', linkLength(childrenCount(d)));
    }
  }
}

/**
 * Linear regression function to ESTIMATE required Link Length
 *
 * linear regression 75.1663 + 5.00259x
 *
 * General Safety Margin = 10
 *
 * @param {number} nodeCount
 * @param {boolean} forLink if for link, DO NOT * 1.5
 * @private
 */
function linkLength(nodeCount, forLink = true) {
  if (nodeCount <= 1) {
    return 50;
  }
  else {
    return length = (5.00259 * nodeCount + 75.1663 + 10) * 0.7;
  }
}

/**
 * Count the size of Children
 * If
 *
 * 1. expandInfo is provided
 *      - count active children
 * 2. expandInfo is missing
 *      - count potential children
 *
 * @param {object} node - node reference
 * @param {object=} expandInfo - expand info from Data Model
 * @returns {number} # of children
 * @private
 */
function childrenCount(node, expandInfo = null) {
  let count = 1;

  if (expandInfo) {
    if (expandInfo[node.id]) {
      // if it is expanded
      count += expandInfo[node.id].nodes.length;

      // if it have children
      if (expandInfo[node.id].hasOwnProperty('children')) {
        for (let i = 0; i < expandInfo[node.id].children.length; i++) {
          count += expandInfo[expandInfo[node.id].children[i]].nodes.length;
        }
      }
    }
  }
  else {
    Constants.EXPAND_KEYS.forEach(expandKeyName => {
      if (node.hasOwnProperty(expandKeyName)) {
        count += node[expandKeyName].length;
      }
    });
  }

  return count;
}