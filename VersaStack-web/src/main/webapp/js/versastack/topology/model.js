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
    "local/versastack/topology/Port",
    "local/versastack/topology/Service",
    "local/versastack/topology/modelConstants",
    "local/versastack/topology/Subnet"
], function (utils, Node, Port, Service, values, Subnet) {
    var map_ = utils.map_;
    var rootNodes = [];

    //Associates a name with the corresponding backing
    var map = {};
    /**
     * Initialize the model. This asyncronasly loads and parsed the model from the backend.
     * @returns {undefined}
     */
    function init(callback) {
        var request = new XMLHttpRequest();
//         request.open("GET","/VersaStack-web/restapi/model/");
        request.open("GET", "/VersaStack-web/data/json/max-aws.json");
//        request.open("GET", "/VersaStack-web/data/graph-full.json");

        request.setRequestHeader("Accept", "application/json");
        request.onload = function () {
            var data = request.responseText;
            data = JSON.parse(data);
            map = JSON.parse(data.ttlModel);
//            map=data;

          
            /*
             * We begin by extracting all nodes/topologies
             *   nodeMap is used to associate a name with its corresponding Node
             * We also begin to handle the case of nested bidirectional ports,
             *  we do this by creating backlinks, so that a nested port will have
             *  a link to its parent.
             */
            var nodeMap = {};
            var portMap = {};
            var serviceMap = {};
            var subnetMap = {};
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
                            var toAdd = new Node(val, map);
                            nodeMap[key] = toAdd;
                            nodeList.push(toAdd);
                            break;
                        case values.bidirectionalPort:
                            var toAdd = new Port(val, map);
                            portMap[key] = toAdd;
                            break;
                        case values.switchingService:
                        case values.topopolgySwitchingService:
                        case values.hypervisorService:
                        case values.routingService:
                        case values.virtualCloudService:
                        case values.blockStorageService:
                        case values.objectStorageService:
                        case values.virtualSwitchingService:
                        case values.hypervisorBypassInterfaceService:
                        case values.storageService:
                        case values.IOPerformanceMeasurementService:
                            var toAdd = new Service(val, map);
                            serviceMap[key] = toAdd;
                            break;

                        case values.switchingSubnet:
                            var toAdd = new Subnet(val, map);
                            subnetMap[key] = toAdd;
                            break;
                        case values.namedIndividual://All elements have this
                        case values.labelGroup:
                        case values.label:
                        case values.networkAdress:
                        case values.bucket:
                        case values.tag:
                        case values.route:
                        case values.volume:
                        case values.routingTable:
                        case values.ontology:
                        case values.POSIX_IOBenchmark:
                        case values.address:
                            break;
                        default:
                            console.log("Unknown type: " + type);
                            break;
                    }
                });
            }

            //Complete the ports
            //  Create aliases between our Port objects
            //  Associate a port with its children
            for (var key in portMap) {
                /**@type Port**/
                var port = portMap[key];
                var port_ = port._backing;
                var aliasKey = port_[values.isAlias];
                if (aliasKey) {
                    var aliasPort = portMap[aliasKey[0].value];
                    port.alias = aliasPort;
                    aliasPort.alias = port;
                }
                var childrenKeys = port_[values.hasBidirectionalPort];
                if (childrenKeys) {
                    map_(childrenKeys, function (childKey) {
                        var child = portMap[childKey.value];
                        port.childrenPorts.push(child);
                        child.parentPort = port;
                    });
                }
            }

            for (var key in serviceMap) {
                var service = serviceMap[key];
                var service_ = service._backing;

                for (var key in service_) {
                    switch (key) {
                        case "name":
                        case values.type:
                        case values.providesBucket:

                        case values.providesRoutingTable:
                        case values.providesRoute:
                        case values.VirtualCloudService:

                        case values.encoding:
                        case values.labelSwapping:
                        case values.providesVolume:
                        case values.providesRoutingTable:
                        case values.providesRoute:
                        case values.providesVM:
                        case values.providesVPC:
                        case values.providesBucket:
                        case values.hasBidirectionalPort:
                            break;
                        case values.providesSubnet:
                            var subnet = service_[key];
                            map_(subnet, function (subnetKey) {
                                subnetKey = subnetKey.value;

                                var subnet = subnetMap[subnetKey];
                                service.subnets.push(subnet);
                            });
                            break;
                        default:
                            console.log("Unknown service attribute: " + key);

                    }
                }

            }

            for (var key in subnetMap) {
                var subnet = subnetMap[key];
                var subnet_ = subnet._backing;

                for (var key in subnet_) {
                    switch (key) {
                        case "name":
                        case values.type:
                        case values.hasNetworkAddress:
                        case values.encoding:
                        case values.labelSwapping:
                            break;
                            //Associate ports and subnet with their parent node
                        case values.hasBidirectionalPort:
                            var ports = subnet_[key];
                            map_(ports, function (portKey) {
                                portKey = portKey.value;
                                var port = portMap[portKey];
                                subnet.ports.push(port);
                            });
                            break;
                        default:
                            console.log("Unknown subnet attribute: " + key);

                    }
                }

            }
            //Associate ports and subnodes with their parent node
            //Create services
            map_(nodeList, /**@param {Node} node**/function (node) {
                var node_ = node._backing;
                for (var key in node_) {
                    switch (key) {
                        case values.hasBidirectionalPort:
                            var ports = node_[key];
                            map_(ports, function (portKey) {
                                portKey = portKey.value;
                                var port = portMap[portKey];
                                node.ports.push(port);
                                port.setNode(node);
                            });
                            break;
                        case values.hasNode:
                        case values.hasTopology:
                            var subNodes = node_[key];
                            map_(subNodes, function (subNodeKey) {
                                var subNode = nodeMap[subNodeKey.value];
                                subNode.isRoot = false;
                                node.children.push(subNode);
                                subNode._parent = node;
                            });
                            break;
                        case values.hasService:
                            var services = node_[values.hasService];
                            map_(services, function (service) {
                                service = serviceMap[service.value];
                                if (!service) {
                                    //service is undefined
                                    console.log("No service: " + service.value);

                                } else {
                                    node.services.push(service);
                                }
                            });
                            break;
                        case "name":
                        case "isRoot": //This is a key that we added to determine which elements are root in the node/topology tree
                        case "processed":  //This is key that we added to assist in detecting when we fail to handle a case
                        case values.type:
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




            map_(nodeList, /**@param {Node} node**/ function (node) {
                if (node.isRoot) {
                    rootNodes.push(node);
                }
            });
            callback();
        };
        request.send();
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
        getRootNodes: function () {
            return rootNodes;
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