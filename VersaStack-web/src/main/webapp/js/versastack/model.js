"use strict";
define([
    "local/versastack/utils"
], function (utils) {
    var map_ = utils.map_;
    var rootNodes = [];

    /**
     * Initialize the model. This asyncronasly loads and parsed the model from the backend.
     * @returns {undefined}
     */
    function init(callback) {
        var request = new XMLHttpRequest();
        // request.open("GET","/VersaStack-web/restapi/model/");
        request.open("GET", "/VersaStack-web/data/graph1.json");

        request.setRequestHeader("Accept", "application/json");
        request.onload = function () {
            var data = request.responseText;
            data = JSON.parse(data);
            var map = JSON.parse(data.ttlModel);

            /*
             * We begin by extracting all nodes/topologies
             * We also begin to handle the case of nested bidirectional ports,
             *  we do this by creating backlinks, so that a nested port will have
             *  a link to its parent.
             */
            var nodeMap = {};
            var nodeList = [];
            for (var key in map) {
                var val = map[key];
                val.name = key;
                var types = val[values.type];
                map_(types, function (type) {
                    type = type.value;
                    switch (type) {
                        case values.topology:
                        case values.node:
                            var toAdd = new Node(val);
                            nodeMap[key] = toAdd;
                            nodeList.push(toAdd);
                            break;
                        case values.bidirectionalPort:
                            if (values.hasBidirectionalPort in val) {
                                subPortKeys = val[values.hasBidirectionalPort];
                                for (key in subPortKeys) {
                                    subPortKey = subPortKeys[key].value;
                                    subPort = map[subPortKey];
                                    subPort.parentPort = val;
                                }
                            }
                        case values.namedIndividual://All elements have this
                        case values.switchingService:
                        case values.topopolgySwitchingService:
                        case values.hypervisorService:
                        case values.labelGroup:
                        case values.label:
                        case values.networkAdress:
                        case values.bucket:
                        case values.routingService:
                        case values.switchingSubnet:
                        case values.tag:
                        case values.route:
                        case values.volume:
                        case values.virtualCloudService:
                        case values.blockStorageService:
                        case values.routingTable:
                        case values.switchingService:
                        case values.objectStorageService:
                            break;
                        default:
                            console.log("Unknown type: " + type);
                            break;
                    }
                });
            }

            //We will construct a list of edges
            //To do this, we first iterate through the ports of each node.
            //We use the alias of the port to create an edge between ports
            //We also create a backlink in the port back to the node so that we can later convert the edge into an edge between nodes
            //To avoid duplicate edges, we mark the alias port as visited, and if we see a visted port, we do not add an edge
            //This requires that no port has multiple aliases (otherwise we risk missing edges)
            var edgeList = [];
            map_(nodeList, /**@param {Node} node**/function (node) {
                var node_ = node._backing;
                for (var key in node_) {
                    switch (key) {
                        case values.hasBidirectionalPort:
                            var ports = node_[key];
                            map_(ports, function (portKey) {
                                portKey = portKey.value;
                                var port = map[portKey];
                                port.node = node;
                                if (!port.processed && values.isAlias in port) {
                                    var aliasPortKey = port[values.isAlias][0].value;
                                    var aliasPort = map[aliasPortKey];
                                    var newEdge = {portA: port, portB: aliasPort};
                                    edgeList.push(newEdge);
                                    aliasPort.processed = true;
                                }
                                port.processed = true;
                            });
                            break;
                        case values.hasNode:
                        case values.hasTopology:
                            var subNodes = node_[key];
                            map_(subNodes, function (subNodeKey) {
                                var subNode = nodeMap[subNodeKey.value];
                                subNode.isRoot = false;
                                node.children.push(subNode);
                            });
                            break;
                        case "name":
                        case "isRoot": //This is a key that we added to determine which elements are root in the node/topology tree
                        case "processed":  //This is key that we added to assist in detecting when we fail to handle a case
                        case values.type:
                        case values.hasService:
                        case values.hasNetworkAddress:
                        case values.provideByService:
                        case values.hasBucket:
                        case values.belongsTo:
                        case values.name:
                        case values.hasVolume:
                            break;
                        default:
                            console.log("Unknown key: " + key);
                    }
                }
            });


            //clean up the edgelist so it associates nodes instead of ports
            map_(edgeList, /**@param {Edge} edge**/ function (edge) {
                edge.nodeA = getNodeOfPort(edge.portA);
                edge.nodeB = getNodeOfPort(edge.portB);
                delete edge.portA;
                delete edge.portB;

                edge.nodeA.primaryNeighboors.push(edge.nodeB);
                edge.nodeB.primaryNeighboors.push(edge.nodeA);
            });

            map_(nodeList, /**@param {Node} node**/ function (node) {
                if (node.isRoot) {
                    rootNodes.push(node);
                    node._complete();
                }
            });
            callback();
        };
        request.send();
    }

    /**
     * The model allows for nested ports
     * As part of our first pass of parsing the model, we created backlinks in
     *  in children ports to link back to the parent. Here we traverse that chain
     *  to find the original Node
     * 
     * @param {Object} port
     * @returns {Node}
     */
    function getNodeOfPort(port) {
        while ("parentPort" in port) {
            port = port.parentPort;
        }
        return port.node;
    }

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
    function Node(backing) {
        this._backing = backing;//the node/topology from the model
        this.children = [];
        this.primaryNeighboors = [];
        this.secondaryNeighboors = [];
        this.isRoot = true;
        this.uid = -1;
        this.isFolded = false;
        this.isVisible = true;
        this._parent = null;

        var that = this;

        this.fold = function () {
            this.isFolded = true;
            this._updateVisible(this.isVisible);//this will update our children appropriatly
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
        }
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
        }
        this.getName = function () {
            return this._backing.name;
        };

        this.getIconPath = function () {
            var types = this._backing[values.type];
            var ans = iconMap.defuault;
            map_(types, function (type) {
                type=type.value;
                if (type in iconMap) {
                    ans = iconMap[type];
                } else if (type !== values.namedIndividual) {
                    console.log("No icon registered for type: " + type);
                }
            });
            return ans;
        };
    }

    function Edge(left, right) {
        this.left = left;
        this.right = right;

        this.source = left;
        this.target = right;

        this._isProper = function () {
            var ans = true;
            while (!this.left.isVisible) {
                this.left = this.left.parent;
            }
            while (!this.right.isVisible) {
                this.right = this.right.parent;
            }
            ans &= this.left.uid < this.right.uid;
            this.source = left;
            this.target = right;
            return ans;
        };
    }

    function listNodes() {
        var ans = [];
        map_(rootNodes, /**@param {Node} node**/function (node) {
            ans = ans.concat(node._getNodes());
        });
        return ans;
    }

    function listEdges() {
        var ans = [];
        map_(rootNodes, /**@param {Node} node**/function (node) {
            map_(node._getEdges(), /**@param {Edge} edge**/function (edge) {
                if (edge._isProper()) {
                    ans.push(edge);
                }
            });
        });
        return ans;
    }

    //No node in nodes should be a decedent of another
    function computeEdges(nodes) {
        map_(nodes, /**@param {Node} node**/function (node) {
            node.__wasFolded = node.getFolded();
            node.fold();
        });
        var edges = listEdges();
        map_(nodes, /**@param {Node} node**/function (node) {
            node.setFolded(node.__wasFolded);
            delete node.__wasFolded;
        });
        return edges;
    }

    /**These are the strings used in the model**/
    var values = {
        type: "http://www.w3.org/1999/02/22-rdf-syntax-ns#type",
        hasBidirectionalPort: "http://schemas.ogf.org/nml/2013/03/base#hasBidirectionalPort",
        isAlias: "http://schemas.ogf.org/nml/2013/03/base#isAlias",
        namedIndividual: "http://www.w3.org/2002/07/owl#NamedIndividual",
        topology: "http://schemas.ogf.org/nml/2013/03/base#Topology",
        node: "http://schemas.ogf.org/nml/2013/03/base#Node",
        bidirectionalPort: "http://schemas.ogf.org/nml/2013/03/base#BidirectionalPort",
        hypervisorService: "http://schemas.ogf.org/mrs/2013/12/topology#HypervisorService",
        labelGroup: "http://schemas.ogf.org/nml/2013/03/base#LabelGroup",
        label: "http://schemas.ogf.org/nml/2013/03/base#Label",
        hasNode: "http://schemas.ogf.org/nml/2013/03/base#hasNode",
        hasService: "http://schemas.ogf.org/nml/2013/03/base#hasService",
        hasTopology: "http://schemas.ogf.org/nml/2013/03/base#hasTopology",
        networkAdress: "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress",
        bucket: "http://schemas.ogf.org/mrs/2013/12/topology#Bucket",
        routingService: "http://schemas.ogf.org/mrs/2013/12/topology#RoutingService",
        switchingSubnet: "http://schemas.ogf.org/mrs/2013/12/topology#SwitchingSubnet",
        hasNetworkAddress: "http://schemas.ogf.org/mrs/2013/12/topology#hasNetworkAddress",
        provideByService: "http://schemas.ogf.org/mrs/2013/12/topology#providedByService",
        hasBucket: "http://schemas.ogf.org/mrs/2013/12/topology#hasBucket",
        belongsTo: "http://schemas.ogf.org/nml/2013/03/base#belongsTo",
        name: "http://schemas.ogf.org/nml/2013/03/base#name",
        tag: "http://schemas.ogf.org/mrs/2013/12/topology#Tag",
        route: "http://schemas.ogf.org/mrs/2013/12/topology#Route",
        volume: "http://schemas.ogf.org/mrs/2013/12/topology#Volume",
        virtualCloudService: "http://schemas.ogf.org/mrs/2013/12/topology#VirtualCloudService",
        blockStorageService: "http://schemas.ogf.org/mrs/2013/12/topology#BlockStorageService",
        routingTable: "http://schemas.ogf.org/mrs/2013/12/topology#RoutingTable",
        switchingService: "http://schemas.ogf.org/nml/2013/03/base#SwitchingService",
        topopolgySwitchingService: "http://schemas.ogf.org/mrs/2013/12/topology#SwitchingService",
        hasVolume: "http://schemas.ogf.org/mrs/2013/12/topology#hasVolume",
        objectStorageService: "http://schemas.ogf.org/mrs/2013/12/topology#ObjectStorageService"
    };

    var iconMap = {};{
        //The curly brackets are for cold folding purposes
        iconMap["default"] = "/VersaStack-web/resources/node.png";
        iconMap[values.node] = "/VersaStack-web/resources/node.png";
        iconMap[values.topology] = "/VersaStack-web/resources/topology.png";
    }

    /**Begin debug functions**/
    function listNodesPretty() {
        var nodes = listNodes();
        var ans = "";
        map_(nodes, /**@param {Node} n**/function (n) {
            ans += n.uid + ",";
        });
        return ans;
    }
    function listEdgesPretty() {
        var edges = listEdges();
        var ans = "";
        map_(edges, /**@param {Edge} e**/function (e) {
            ans += "(" + e.left.uid + "," + e.right.uid + "), ";
        });
        return ans;
    }

    /*Note that these fold and unfold functions are inteded for testing only
     * If a node is not currently visible, they will have no effect
     */

    function fold(i) {
        map_(listNodes(), /**@param {Node} n**/function (n) {
            if (n.uid === i) {
                n.fold();
            }
        });
    }
    function unfold(i) {
        map_(listNodes(), /**@param {Node} n**/function (n) {
            if (n.uid === i) {
                n.unfold();
            }
        });
    }

    function printTree() {
        var ans = "\n";
        map_(rootNodes, /**@param {Node} n**/function (n) {
            map_(printTree_(n), /**@param {String} line**/function (line) {
                ans += line;
                ans += "\n";
            });
        });
        return ans;
    }

    /**
     * 
     * @param {Node} n
     */
    function printTree_(n) {
        var ans = [];
        ans.push(String(n.uid));
        map_(n.children, /**@param {Node} child**/function (child) {
            map_(printTree_(child), /**@param {String} line**/function (line) {
                ans.push(" " + line);
            });
        });
        return ans;
    }

    /** PUBLIC INTERFACE **/
    return {
        init: init,
        listNodes: listNodes,
        listEdges: listEdges,
        computeEdges: computeEdges,
        getRootNodes: function () {
            return rootNodes
        },
        /** begin debug functions **/
        listNodesPretty: listNodesPretty,
        listEdgesPretty: listEdgesPretty,
        fold: fold,
        unfold: unfold,
        printTree: printTree
    };
    /** END PUBLIC INTERFACE **/

});