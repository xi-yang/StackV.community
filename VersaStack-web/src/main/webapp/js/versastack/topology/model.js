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

//debug code to modify the model before parsing=
var INJECT = false;
define([
    "local/versastack/utils",
    "local/versastack/topology/Node",
    "local/versastack/topology/Port",
    "local/versastack/topology/Service",
    "local/versastack/topology/modelConstants",
    "local/versastack/topology/Subnet"
], function (utils, Node, Port, Service, values, Subnet) {

    function Model(oldModel) {
        var map_ = utils.map_;
        var rootNodes = [];
        var versionID;

        //Associates a name with the corresponding backing
        var map = {};
        var that = this;
        /**
         * Initialize the model. This asyncronasly loads and parsed the model from the backend.
         * @returns {undefined}
         */
        this.init = function (mode, callback, model) {
            var request = new XMLHttpRequest();
            if (mode === 1) {
                //request.open("GET", "/VersaStack-web/data/json/max-aws.json");
                //request.open("GET", "/VersaStack-web/data/json/model-all-hybrid.json");
                request.open("GET", "/VersaStack-web/data/json/umd-anl-all-2.json");
            }
            else if (mode === 2) {
                request.open("GET", "/VersaStack-web/restapi/model/");
            }

            //console.log("Mode: " + mode);
            request.setRequestHeader("Accept", "application/json");
            request.onload = function () {
                if (model === null) {
                    var data = request.responseText;
                } else
                    var data = model;

                //console.log("Data: " + data);

                if (data.charAt(0) === '<') {
                    window.alert("Empty Topology.");
                    return;
                } else if (data.charAt(0) === 'u') {
                    data = JSON.parse(data);
                    versionID = data.version;
                    map = JSON.parse(data);
                } else {
                    data = JSON.parse(data);
                    versionID = data.version;
                    map = JSON.parse(data.ttlModel);
                }

                if (INJECT) {
                    var newNode = {type: 'uri', value: 'FOO:1'};
                    map["urn:ogf:network:sdn.maxgigapop.net:network"][values.hasNode].push(newNode);
                    map[newNode.value] = {};
                    map[newNode.value][values.type] = [{type: 'uri', value: values.namedIndividual}, {type: 'uri', value: values.node}];
                }
//            map=data;


                /*
                 * We begin by extracting all nodes/topologies
                 *   nodeMap is used to associate a name with its corresponding Node
                 * We also begin to handle the case of nested bidirectional ports,
                 *  we do this by creating backlinks, so that a nested port will have
                 *  a link to its parent.
                 */
                that.nodeMap = {};
                that.portMap = {};
                that.serviceMap = {};
                that.subnetMap = {};
                for (var key in map) {
                    var val = map[key];
                    val.name = key;
                    var types = val[values.type];
                    if (!types) {
                        console.log("Types empty!\n\nVal: " + val + "\nName: " + val.name);
                    } else {
                        map_(types, function (type) {
                            type = type.value;
                            switch (type) {
                                case values.topology:
                                case values.node:
                                case values.FileSystem:

                                    var toAdd;
                                    if (oldModel && oldModel.nodeMap[key]) {
                                        toAdd = oldModel.nodeMap[key];
                                        toAdd.reload(val, map);
                                    } else {
                                        toAdd = new Node(val, map);
                                    }
                                    toAdd.isTopology = type === values.topology;
                                    that.nodeMap[key] = toAdd;
                                    break;
                                case values.bidirectionalPort:
                                    var toAdd;
                                    if (oldModel && oldModel.portMap[key]) {
                                        toAdd = oldModel.portMap[key];
                                        toAdd.reload(val, map);
                                    } else {
                                        toAdd = new Port(val, map);
                                    }
                                    that.portMap[key] = toAdd;
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
                                case values.DataTransferService:
                                case values.DataTransferClusterService:
                                case values.NetworkObject:
                                    var toAdd;
                                    if (oldModel && oldModel.serviceMap[key]) {
                                        toAdd = oldModel.serviceMap[key];
                                        toAdd.reload(val, map);
                                    } else {
                                        toAdd = new Service(val, map);
                                    }
                                    that.serviceMap[key] = toAdd;
                                    break;

                                case values.switchingSubnet:
                                    var toAdd;
                                    if (oldModel && oldModel.subnetMap[key]) {
                                        toAdd = oldModel.subnetMap[key];
                                        toAdd.reload(val, map);
                                    } else {
                                        toAdd = new Subnet(val, map);
                                    }
                                    that.subnetMap[key] = toAdd;
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
                }

                //Complete the ports
                //  Create aliases between our Port objects
                //  Associate a port with its children
                for (var key in that.portMap) {
                    /**@type Port**/
                    var port = that.portMap[key];
                    var port_ = port._backing;
                    var aliasKey = port_[values.isAlias];
                    if (aliasKey) {
                        var aliasPort = that.portMap[aliasKey[0].value];
                        if (aliasPort) {
                            port.alias = aliasPort;
                            aliasPort.alias = port;
                        } else {
                            console.log("Alias Port Non-Existent!");
                            break;
                        }
                    } else {
                        port.alias = null;
                    }
                    port.childrenPorts = [];
                    var childrenKeys = port_[values.hasBidirectionalPort];
                    if (childrenKeys) {
                        map_(childrenKeys, function (childKey) {
                            var child = that.portMap[childKey.value];
                            port.childrenPorts.push(child);
                            child.parentPort = port;
                        });
                    }
                }

                for (var key in that.serviceMap) {
                    var service = that.serviceMap[key];
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
                            case values.hasService:
                            case values.active_transfers:
                            case values.topoType:
                            case values.value:
                            case values.hasLabel:
                            case values.hasLabelGroup:
                                break;
                            case values.providesSubnet:
                                var subnet = service_[key];
                                map_(subnet, function (subnetKey) {
                                    subnetKey = subnetKey.value;

                                    var subnet = that.subnetMap[subnetKey];
                                    service.subnets.push(subnet);
                                });
                                break;
                            default:
                                console.log("Unknown service attribute: " + key);

                        }
                    }

                }

                for (var key in that.subnetMap) {
                    var subnet = that.subnetMap[key];
                    var subnet_ = subnet._backing;
                    for (var key in subnet_) {
                        switch (key) {
                            case "name":
                            case values.type:
                            case values.hasNetworkAddress:
                            case values.encoding:
                            case values.labelSwapping:
                            case values.topoType:
                            case values.hasTag:
                            case values.value:
                                break;
                                //Associate ports and subnet with their parent node
                            case values.hasBidirectionalPort:
                                var ports = subnet_[key];
                                map_(ports, function (portKey) {
                                    portKey = portKey.value;
                                    var port = that.portMap[portKey];
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
                for (var key in that.nodeMap) {
                    /**@type Node**/
                    var node = that.nodeMap[key]
                    var node_ = node._backing;
                    for (var key in node_) {
                        switch (key) {
                            case values.hasBidirectionalPort:
                                var ports = node_[key];
                                map_(ports, function (portKey) {
                                    portKey = portKey.value;
                                    var errorVal = portKey;
                                    var port = that.portMap[portKey];
                                    if (!port) {
                                        //port is undefined
                                        console.log("No port: " + errorVal);
                                    } else {
                                        node.ports.push(port);
                                        port.setNode(node);
                                    }
                                });
                                break;
                            case values.hasNode:
                            case values.hasTopology:
                            case values.hasFileSystem:
                                var subNodes = node_[key];
                                map_(subNodes, function (subNodeKey) {
                                    var errorVal = subNodeKey.value;
                                    var subNode = that.nodeMap[subNodeKey.value];
                                    if (!subNode) {
                                        //subnode is undefined
                                        console.log("No subnode: " + errorVal)
                                    } else {
                                        subNode.isRoot = false;
                                        node.children.push(subNode);
                                        subNode._parent = node;
                                    }
                                });
                                break;
                            case values.hasService:
                                var services = node_[values.hasService];
                                map_(services, function (service) {
                                    var errorVal = service.value;
                                    service = that.serviceMap[service.value];
                                    if (!service) {
                                        //service is undefined
                                        console.log("No service: " + errorVal);
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
                            case values.num_core:
                            case values.memory_mb:
                            case values.mount_point:
                            case values.measurement:
                            case values.topoType:
                            case values.hasTag:
                                break;
                            default:
                                console.log("Unknown key: " + key);
                        }
                    }
                }

                for (var key in that.nodeMap) {
                    var node = that.nodeMap[key];
                    if (node.isRoot) {
                        rootNodes.push(node);
                    }
                }
                callback();
            };
            request.send();
        };

        this.getVersion = function () {
            return versionID;
        }

        this.listNodes = function () {
            var ans = [];
            map_(rootNodes, /**@param {Node} node**/function (node) {
                ans = ans.concat(node._getNodes());
            });
            return ans;
        };
        this.listEdges = function () {
            var ans = [];
            map_(rootNodes, /**@param {Node} node**/function (node) {
                map_(node._getEdges(), /**@param {Edge} edge**/function (edge) {
                    if (edge._isProper()) {
                        ans.push(edge);
                    }
                });
            });
            return ans;
        };



        /**Begin debug functions**/
        this.listNodesPretty = function () {
            var nodes = listNodes();
            var ans = "";
            map_(nodes, /**@param {Node} n**/function (n) {
                ans += n.uid + ",";
            });
            return ans;
        };
        this.listEdgesPretty = function () {
            var edges = listEdges();
            var ans = "";
            map_(edges, /**@param {Edge} e**/function (e) {
                ans += "(" + e.left.uid + "," + e.right.uid + "), ";
            });
            return ans;
        };
        this.getRootNodes = function () {
            return rootNodes;
        };
        /*Note that these fold and unfold functions are inteded for testing only
         * If a node is not currently visible, they will have no effect
         */

        this.fold = function (i) {
            map_(listNodes(), /**@param {Node} n**/function (n) {
                if (n.uid === i) {
                    n.fold();
                }
            });
        };
        this.unfold = function (i) {
            map_(listNodes(), /**@param {Node} n**/function (n) {
                if (n.uid === i) {
                    n.unfold();
                }
            });
        };

        this.printTree = function () {
            var ans = "\n";
            map_(rootNodes, /**@param {Node} n**/function (n) {
                map_(printTree_(n), /**@param {String} line**/function (line) {
                    ans += line;
                    ans += "\n";
                });
            });
            return ans;
        };

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
    }
    /** PUBLIC INTERFACE **/
    return Model;
    /** END PUBLIC INTERFACE **/

});
