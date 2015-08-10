"use strict";
define([
    "local/versastack/utils",
    "local/versastack/topology/Edge",
    "local/versastack/topology/modelConstants"
], function (utils, Edge, values) {
    var map_ = utils.map_;
    function Port(backing, map) {
        var that = this;
        this._backing = backing;
        /**@type Array.Port**/
        this.childrenPorts = [];
        /**@type Port**/
        this.parentPort = null;
        /**@type Node**/
        this.ancestorNode = null;
        this.alias = null;

        this.isVisible = false;
        this.x = 0;
        this.y = 0;
        this.enlarged = false;
        this.svgNode = null;
        this.svgNodeSubnetHighlight = null; // For subnet tab
        this.folded = false;

        //We are reloading this port from a new model
        //Model.js will handle most of the reparsing, but we need to
        //clear out some old data
        this.reload=function(backing,map){
            this._backing=backing;
            this._map=map;
            this.childrenPorts=[];
            this.parentPort=null;
            this.ancestorNode=null;
            this.alias=null;
        };

        this.getCenterOfMass = function () {
            if(this.getVisible()){
                return {x: this.x, y: this.y};
            }
            if(this.parentPort){
                return this.parentPort.getCenterOfMass();
            }
            return this.ancestorNode.getCenterOfMass();
        };
        
        this.getFirstVisibleParent=function(){
            if(this.getVisible()){
                return this;
            }
            if(this.parentPort){
                return this.parentPort.getFirstVisibleParent();
            }
            return this.ancestorNode.getFirstVisibleParent();
        }

        var children = backing[values.hasBidirectionalPort];
        if (children) {
            map_(children, function (child) {
                child = map[child.value];
                child = new Port(child, map);
                that.childrenPorts.push(child);
            });
        }

        this.setFolded = function (b) {
            if (this.childrenPorts.length > 0) {
                this.folded = b;
            }
        };
        this.getVisible = function () {
            var ans = this.isVisible;
            var cursor = this.parentPort;
            while (cursor) {
                ans &= !cursor.folded;
                cursor = cursor.parentPort;
            }
            return ans;
        };
        this.setVisible = function (vis) {
            this.isVisible = vis;
            map_(this.childrenPorts, function (child) {
                child.setVisible(vis && !that.folded);
            });
        };

        this.getFolded = function () {
            return this.folded;
        };

        this.getVisibleHeight = function () {
            var ans = 0;
            if (!this.folded) {
                map_(this.childrenPorts, function (child) {
                    ans = Math.max(ans, child.getVisibleHeight());
                });
            }
            ans += 1;
            return ans;
        };
        this.hasChildren = function () {
            return this.childrenPorts.length > 0 && !this.folded;
        };
        this.countVisibleLeaves = function () {
            if (!this.hasChildren() || this.folded) {
                return 1;
            }
            var ans = 0;
            map_(this.childrenPorts, function (child) {
                ans += child.countVisibleLeaves();
            });
            return ans;
        };

        //return all the edges involving this port, or its decendents
        this.getEdges = function () {
            var ans = [];
            if (this.alias) {
                ans.push(new Edge(this, this.alias));
            }
            map_(this.childrenPorts, function (child) {
                ans = ans.concat(child.getEdges());
            });
            return ans;
        };


        this.setNode = function (node) {
            if (this.ancestorNode) {
                //In some cases, the backend may incorrectly mark a port as belonging to both
                //a topology, and one of its decendents (specifically, the upper topology
                //appears to be top level aws). In this case, we only want to store is
                //as belonging to the lower node
                var oldNode = this.ancestorNode;
                var arr = [];
                map_(oldNode.ports, function (port) {
                    if (port !== that) {
                        arr.push(port);
                    }
                });
                oldNode.ports = arr;

                arr = [];
                map_(node.ports, function (port) {
                    if (port !== that) {
                        arr.push(port);
                    }
                });
                node.ports = arr;

                this.ancestorNode = oldNode.getHeight() > node.getHeight ? oldNode : node;
                this.ancestorNode.ports.push(this);
            }
            this.ancestorNode = node;
            map_(this.childrenPorts, function (child) {
                child.setNode(node);
            });
        };

        this.getName = function () {
            return backing.name;
        };
        this.populateTreeMenu = function (tree) {
            var root = tree.addChild(this.getName());
            map_(this.childrenPorts, function (child) {
                child.populateTreeMenu(root);
            });
        };

        this.hasAlias = function () {
            return this.alias !== null;
        };

        this.getIconPath = function () {
            if (this.folded) {
                return "/VersaStack-web/resources/bidirectional_port_expandable.png"
            }
            return "/VersaStack-web/resources/bidirectional_port.png";
        };
        this.getType=function(){
            return "Port"
        }
    }

    return Port;
});