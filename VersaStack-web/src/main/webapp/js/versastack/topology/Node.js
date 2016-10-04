/*
 * Copyright (c) 2013-2016 University of Maryland
 * Modified by: Antonio Heard 2016

 * Permission is hereby granted, free of charge, to any person obtaining a copy 
 * of this software and/or hardware specification (the “Work”) to deal in the 
 * Work without restriction, including without limitation the rights to use, 
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of 
 * the Work, and to permit persons to whom the Work is furnished to do so, 
 * subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Work.

 * THE WORK IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL 
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
 * FROM, OUT OF OR IN CONNECTION WITH THE WORK OR THE USE OR OTHER DEALINGS  
 * IN THE WORK.
 */

"use strict";
define(["local/versastack/topology/modelConstants","local/versastack/utils"],
        function (values, utils) {
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
            var map_ = utils.map_;
            function Node(backing, map) {
                this._backing = backing; //the node/topology from the model
                this._map = map;
                /**@type Array.Node**/
                this.children = [];
                this.isTopology = false;
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
                /**@type Array.Volume**/
                this.volumes = [];
                /**@type VolumeDisplayPopup**/
                this.volumePopup = null;                
                this.x = 0;
                this.y = 0;
                this.dx = 0;
                this.dy = 0;
                this.size = 0;
                this.detailsReference = false;
                
                this.misc_elements = [];
                
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
                this.getTopologies = function () {
                    var ans = [];
                    map_(this.children, function (child) {
                        if (child.isTopology) {
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
                    leaves.forEach(function(leave) {
                        if (isNaN(leave.x) || isNaN(leave.y)) {
                            console.log("THE LEAVES HAVE NAN X OR Y")
                            console.log("x: " + leave.x + " y: " + leave.y);
                        }
                        if (leave === undefined) {
                            console.log("undefined leaf");
                        }
                    });
                    var num = leaves.length;
                    
//                    console.log("Node -> getCenterOfMass -> leaves.length: " + leaves.length);  @
                    map_(leaves, function (leaf) {
                        if(leaf !== undefined) {
                            // do a before and after x and y check here 
                            ans.x += leaf.x / num;
                            ans.y += leaf.y / num;
//                            console.log("Node->getCenterOfMass -> ans.x in map:  " + ans.x); @
//                            console.log("Node->getCenterOfMass -> ans.y in map: " + ans.y);   @
                        }
                        if (leaf === undefined) console.log("they're (leaf) null \n");                                           
                        //if (isNaN(leaf.x) || isNaN(leaf.y)) console.log("they're (leaf coords) null \n");                   

                    });
                        if (isNaN(ans.x) || isNaN(ans.y)) console.log("they're (ans) null \n");                   
                    if (num === 0 || (isNaN(ans.x) || isNaN(ans.y))) {
                        ans.x = this.x;
                        ans.y = this.y;
                        if (isNaN(this.x) || isNaN(this.y)) {
                            console.log("they're (this) null \n");
                            console.log("my name is: " + this.getName());
                        }
                    }
                    if (isNaN(this.x) || isNaN(this.y)) {
//                        console.log("In Node:getCenterOfMass ->isNaN(tis returned; his.x) || isNaN(this.y is true ans 0,0 is returned; "); @
                        return {x: 0, y: 0};
                    }
//                    console.log("In Node:getCenterOfMass ->ans is returned: ans is ans.x: " + ans.x + ", ans.y: " + ans.y);       @              
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
