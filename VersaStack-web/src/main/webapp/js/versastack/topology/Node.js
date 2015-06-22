"use strict";
define(["local/versastack/topology/modelConstants",
    "local/versastack/topology/Service",
    "local/versastack/topology/Edge"], 
    function (values,Service,Edge) {
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
    function Node(backing,map) {
        this._backing = backing; //the node/topology from the model
        this.children = [];
        this.primaryNeighboors = [];
        this.secondaryNeighboors = [];
        this.isRoot = true;
        this.uid = -1;
        this.isFolded = false;
        this.isVisible = true;
        this._parent = null;
        this.services = [];
        var that = this;
        this.fold = function () {
            this.isFolded = true;
            this._updateVisible(this.isVisible); //this will update our children appropriatly
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
        };
        this._complete = function () {
            this.uid = i++;
            map_(this.children, /**@param {Node} child**/function (child) {
                child.parent = that;
                child._complete();
                that.secondaryNeighboors = that.secondaryNeighboors.concat(child.secondaryNeighboors, child.primaryNeighboors);
            });
            //extract the services that this node has
            var services = this._backing[values.hasService];
            if (services) {
                map_(services, function (service) {
                    service = map[service.value];
                    if (!service) {
                        //service is undefined
                        console.log("No service: " + service.value);
                        return;
                    }
                    var toAdd = new Service(service,this);
                    that.services.push(toAdd);
                });
            }
        };
        this._getEdges = function () {

            var ans = [];
            map_(this.primaryNeighboors, /**@param {Node} neighboor**/function (neighboor) {
                if (neighboor.uid > that.uid) {
                    var toAdd = new Edge(that, neighboor);
                    ans.push(toAdd);
                }
            });
            if (this.isFolded) {
                map_(this.secondaryNeighboors, /**@param {Node} neighboor**/function (neighboor) {
                    if (neighboor.uid > that.uid) {
                        var toAdd = new Edge(that, neighboor);
                        ans.push(toAdd);
                    }
                });
            } else {
                map_(this.children, /**@param {Node} child**/function (child) {
                    ans = ans.concat(child._getEdges());
                });
            }
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
            var prefix="/VersaStack-web/resources/";
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
            return prefix+ans;
        };
        this.getCenterOfMass = function(){
            var ans={x:0,y:0};
            var leaves=this.getLeaves();
            var num=leaves.length;
            map_(leaves,function(leaf){
                ans.x+=leaf.x/num;
                ans.y+=leaf.y/num;
            });
            if(num===0){
                ans.x=this.x;
                ans.y=this.y;
            }
            return ans;
        };
    }

    var iconMap = {};{
//The curly brackets are for cold folding purposes
        iconMap["default"] = "default.png";
        iconMap[values.node] = "node.png";
        iconMap[values.topology] = "topology.png";
    }

    return Node;
});