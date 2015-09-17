"use strict";
define(["local/versastack/topology/modelConstants"],
        function (values) {
            /**
             * There are two graphs we want to consider. The first is the tree representing the node/subnode relationships
             * The second is the model graph, representing the connections as understood by the model
             * We want to be able to generate a model graph when some, all, or none of the non-leaf nodes are folded
             * When a node is folded, it assumes all the connections of its children in the model graph
             * 
             * We say that a node is external if it is not the current node, nor a descendent of the current node
             * 
             * In the below data structure each Node corresponds directly to a model node (or topology), given by _backing
             * The children field represents the tree structure of subnodes
             * The primary neighboors refer to Nodes that connect directly to the current one
             * the secondary neighboors refers to external nodes that connect to a descendent of the current node
             * 
             **/
            var i = 0;
            function Node(backing, map) {
                this._backing = backing; //the node/topology from the model
                this._map = map;
                /**@type Array.Node**/
                this.children = [];
                this.isTopology=false;
                this.isRoot = true;
                this.uid = i++;
                this.isFolded = false;
                this.isVisible = true;
                /**@type Array.Node**/
                this._parent = null;
                this.svgNode = null;
                this.svgNodeAnchor = null;//For topologies
                this.svgNodeServices = null;
                this.svgNodeSubnetHighlight = null; // for subnet tab
                /**@type Array.Service**/
                this.services = [];
                /**@type Array.Port**/
                this.ports = [];
                /**@type PortDisplayPopup**/
                this.portPopup = null;
                this.x = 0;
                this.y = 0;
                this.dx = 0;
                this.dy = 0;
                this.size = 0;
                /**@type Node**/
                var that = this;
                ////We are reloading this port from a new model
                //Model.js will handle most of the reparsing, but we need to
                //clear out some old data
                this.reload = function (backing, map) {
                    this._backing = backing;
                    this._map = map;
                    this.children = [];
                    this._parent = null;
                    this.services = [];
                    this.ports = [];
                }
                this.fold = function () {
                    this.isFolded = true;
                    this._updateVisible(this.isVisible); //this will update our children appropriatly
                    if (this.portPopup) {
                        this.portPopup.setVisible(false);
                    }
                };
                this.unfold = function () {
                    this.isFolded = false;
                    this._updateVisible(this.isVisible);
                };
                this.setFolded = function (b) {
                    if (b) {
                        this.fold();
                    } else {
                        this.unfold();
                    }
                };
                this.getFolded = function () {
                    return this.isFolded;
                };
                this.toggleFold = function () {
                    this.setFolded(!this.getFolded());
                };
                this._updateVisible = function (vis) {
                    this.isVisible = vis;
                    var showChildren = vis && !this.isFolded;
                    map_(this.children, function (child) {
                        child._updateVisible(showChildren);
                    });
                    if (!vis) {
                        map_(this.ports, function (port) {
                            port.setVisible(false);
                        });
                    }
                };
                this._getEdges = function () {
                    var ans = [];
                    map_(this.ports, /**@param {Port} port**/ function (port) {
                        ans = ans.concat(port.getEdges());
                    });
                    map_(this.children, function (child) {
                        ans = ans.concat(child._getEdges());
                    });
                    return ans;
                };
                this._getNodes = function () {
                    var ans = [that];
                    if (!this.isFolded) {
                        map_(this.children, /**@param {Node} child**/function (child) {
                            ans = ans.concat(child._getNodes());
                        });
                    }
                    return ans;
                };
                this.getTopologies = function(){
                    var ans=[];
                    map_(this.children,function(child){
                        if(child.isTopology){
                            ans.push(child);
                        }
                    });
                    return ans;
                }
                //Return the number of visible nodes in the subtree rooted at this;
                this.visibleSize = function () {
                    var ans = 0;
                    if (this.isVisible) {
                        ans = 1;
                        map_(this.children, /**@param {Node} child**/function (child) {
                            ans += child.visibleSize();
                        });
                    }
                    return ans;
                };
                this.isLeaf = function () {
                    return this.isFolded || this.children.length === 0;
                };
                this.getLeaves = function () {
                    if (this.isLeaf()) {
                        return [this];
                    } else {
                        var ans = [];
                        map_(this.children, function (n) {
                            ans = ans.concat(n.getLeaves());
                        });
                        return ans;
                    }
                };
                this.getName = function () {
                    return this._backing.name;
                };
                this.getIconPath = function () {
                    var prefix = "/VersaStack-web/resources/";
                    var types = this._backing[values.type];
                    var ans = iconMap.default;
                    map_(types, function (type) {
                        type = type.value;
                        if (type in iconMap) {
                            ans = iconMap[type];
                        } else if (type !== values.namedIndividual) {
                            console.log("No icon registered for type: " + type);
                        }
                    });
                    return prefix + ans;
                };
                this.getCenterOfMass = function () {
                    if (!this.isVisible) {
                        return this._parent.getCenterOfMass();
                    }
                    var ans = {x: 0, y: 0};
                    var leaves = this.getLeaves();
                    var num = leaves.length;
                    map_(leaves, function (leaf) {
                        ans.x += leaf.x / num;
                        ans.y += leaf.y / num;
                    });
                    if (num === 0) {
                        ans.x = this.x;
                        ans.y = this.y;
                    }
                    return ans;
                };
                this.getVisible = function () {
                    if (this.isRoot && this.isVisible) {
                        return true;
                    }
                    return this.isVisible && this._parent.getVisible();
                };
                this.getFirstVisibleParent = function () {
                    if (this.isVisible) {
                        return this;
                    }
                    return this._parent.getFirstVisibleParent();
                };
                //Return the depth of this node in the topology tree
                //This is used to determine what color to use when drawing topologies
                this.getDepth = function () {
                    var ans = 0;
                    var cursor = this._parent;
                    while (cursor) {
                        ans++;
                        cursor = cursor._parent;
                    }
                    return ans;
                };
                //the depth of the deepest child
                this.getHeight = function () {
                    var ans = -1;
                    map_(this.children, /**@param {Node} child**/function (child) {
                        ans = Math.max(ans, child.getHeight());
                    });
                    ans += 1;
                    return ans;
                };
                this.getType = function () {
                    return this.isLeaf() ? "Node" : "Topology";
                };

                this.populateTreeMenu = function (tree) {
                    if (this.services.length > 0) {
                        var serviceNode = tree.addChild("Services");
                        map_(this.services, function (service) {
                            service.populateTreeMenu(serviceNode);
                        })
                    }
                    if (this.ports.length > 0) {
                        var portsNode = tree.addChild("Ports");
                        map_(this.ports, function (port) {
                            port.populateTreeMenu(portsNode);
                        });
                    }
                    if (this.children.length > 0) {
                        var childrenNode = tree.addChild("SubNodes");
                        map_(this.children, function (child) {
                            var childNode = childrenNode.addChild(child.getName());
                            child.populateTreeMenu(childNode);
                        });
                    }
                };
            }

            var iconMap = {};
            {
                //The curly brackets are for cold folding purposes
                iconMap["default"] = "default.png";
                iconMap[values.node] = "node.png";
                iconMap[values.topology] = "topology.png";
                iconMap[values.FileSystem] = "filesystem.png";
                //iconMap[values.NetworkObject] = iconMap[values.node];
            }

            return Node;
        });
