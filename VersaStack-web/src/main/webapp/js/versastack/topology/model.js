/**
 * The goal of this class is to download the model from the back-end and convert
 * it into a format more convinient for displaying graphically.
 * 
 * Specifically, we output a list nodes, and a separate list of the edges between them.
 * The model itself also has the concept of topologies, subtopologies, and subnodes. 
 *   The idea being that a topology or node can be thought of as its own node, but
 *   can also be expanded to show an arbitrarily complicated subnetwork of nodes,
 *   potential containing aditionaly topologies. Within this program, we use "topology"
 *   to refer to both topologies, and nodes which have subnodes. 
 *   Internally, we store the topologie information in a tree structure, defined
 *   by the children field of the Node class.
 *   The exported Nodes have a fold/unfold method. When a Node is folded, it should
 *   be considered to be a leaf node, regardless of if it has children. Decendents
 *   of this node will not be returned through the listNodes method, and any edges
 *   that involved a desecendent of said Node will be redirected to said Node when
 *   in the output of listEdges.
 */

"use strict";
define([
    "local/versastack/utils",
    "local/versastack/topology/Node",
    "local/versastack/topology/Edge",
    "local/versastack/topology/modelConstants"
], function (utils,Node,Edge,values) {
    var map_ = utils.map_;
    var rootNodes = [];

    //Associates a name with the corresponding backing
    var map={};
    /**
     * Initialize the model. This asyncronasly loads and parsed the model from the backend.
     * @returns {undefined}
     */
    function init(callback) {
        var request = new XMLHttpRequest();
        // request.open("GET","/VersaStack-web/restapi/model/");
//        request.open("GET", "/VersaStack-web/data/graph1.json");
        request.open("GET", "/VersaStack-web/data/graph-full.json");

        request.setRequestHeader("Accept", "application/json");
        request.onload = function () {
            var data = request.responseText;
            data = JSON.parse(data);
//            map = JSON.parse(data.ttlModel);
            map=data;

            /*
             * We begin by extracting all nodes/topologies
             *   nodeMap is used to associate a name with its corresponding Node
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
                            var toAdd = new Node(val,map);
                            nodeMap[key] = toAdd;
                            nodeList.push(toAdd);
                            break;
                        case values.bidirectionalPort:
                            if (values.hasBidirectionalPort in val) {
                                var subPortKeys = val[values.hasBidirectionalPort];
                                for (key in subPortKeys) {
                                    var subPortKey = subPortKeys[key].value;
                                    var subPort = map[subPortKey];
                                    if(subPort){
                                        subPort.parentPort = val;
                                    }else{
                                        console.log("Subport does not exist: "+subPortKey);
                                    }
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
                                    if(aliasPort){
                                        var newEdge = {portA: port, portB: aliasPort};
                                        edgeList.push(newEdge);
                                        aliasPort.processed = true;
                                    }else{
                                        console.log("aliasPort does not exist: "+aliasPortKey);
                                    }
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


            //Use the edge information to construct the sibling relationship in the nodes themselves
            //Because folding changes the edges, we dynamicly re-construct them ondemend in the Node class
            map_(edgeList, /**@param {Edge} edge**/ function (edge) {
                var nodeA = getNodeOfPort(edge.portA);
                var nodeB = getNodeOfPort(edge.portB);

                nodeA.primaryNeighboors.push(nodeB);
                nodeB.primaryNeighboors.push(nodeA);
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