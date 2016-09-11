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
    "local/versastack/topology/Subnet",
    "local/versastack/topology/Volume",
    "local/versastack/topology/Element",
    "local/versastack/topology/Policy",
    "local/versastack/topology/Edge"

], function (utils, Node, Port, Service, values, Subnet, Volume, Element, Policy, Edge) {

    function Model(oldModel) {
        var map_ = utils.map_;
        var rootNodes = [];
        var versionID;
//        var others = [];

        //Associates a name with the corresponding backing
        var map = {};
        var that = this;
        /**
         * Initialize the model. This asyncronasly loads and parsed the model from the backend.
         * @returns {undefined}
         */
        this.init = function (mode, callback, model) {            
            var request = new XMLHttpRequest();  
            // If ready, load the live model. Otherwise, load the static model. 
            request.open("GET", "/VersaStack-web/restapi/model/"); 
            //request.open("GET", "/VersaStack-web/data/json/spa-rvtk-versastack-qa1-1vm.json");
            //request.open("GET", "/VersaStack-web/data/json/aws-blank.json");
            requestModel();
            
            function requestModel() {
                request.setRequestHeader("Accept", "application/json");
                request.setRequestHeader("Content-type", "application/json");
                request.onload = function () {
                    if (model === null) {
                        var data = request.responseText;
                    } else
                        var data = model;
                    
                    if (JSON.parse(data).hasOwnProperty('exception')) return;

                    if (data.charAt(0) === '<') {
                        alert("Empty Topology.");
                        return;
                    } else if ((data.charAt(0) === 'u' || data.charAt(0) === '{')) {
                        //data = JSON.parse(data);
                        //versionID = data.version;
                        map = JSON.parse(data);
                        if (map.ttlModel) {
                           map = JSON.parse(map.ttlModel);
                        }
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
                    that.modelString = JSON.stringify(map, null, '\t');
                    console.log("\n\nmap from request \n\n");
                    console.log(that.modelString);

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
                    that.volumeMap = {};
                    that.elementMap = {};
                    that.policyMap = {};
                    that.policyEdges = [];
                    for (var key in map) {
                        var val = map[key];
                        val.name = key;
                        //console.log("JSON.stringify(element, null, 2): " + JSON.stringify(val, null, 2));
                        var types = val[values.type];
                        if (!types) {
                           //console.log("Types empty!\n\nVal: " + val + "\nName: " + val.name);
                        } else {
                            map_(types, function (type) {
                                type = type.value;

                                // Adding every element to the elementMap for the 
                                // displayPanel.  Ifnoring elemnets with the type "NamedIndividual"
                                if (type.split("#")[1] === "NamedIndividual") return "";//                                                

                                var toAdd;
                                if (oldModel && oldModel.elementMap[key]) {
                                    toAdd = oldModel.elementMap[key];
                                    toAdd.reload(val, map);
                                } else {
                                    console.log("i was used");
                                    toAdd = new Element(val, map, that.elementMap);
                                    toAdd.topLevel = true;
                                }
                                that.elementMap[key] = toAdd;
                                switch (type) {
                                    // Fallthrough group 
                                    case values.topology:
                                    case values.node:
                                        console.log("type: " + type);
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

                                        // Fallthrough group     
                                    case values.switchingService:
                                    case values.topopolgySwitchingService:
                                    case values.hypervisorService:
                                    case values.routingService:
                                    case values.virtualCloudService:
                                    case values.blockStorageService:
                                        var toAdd;
                                        if (oldModel && oldModel.serviceMap[key]) {
                                            toAdd = oldModel.serviceMap[key];
                                            toAdd.reload(val, map);
                                        } else {
                                            toAdd = new Service(val, map);
                                        }
                                        that.serviceMap[key] = toAdd;
                                        break;

                                        // Fallthrough group 
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
                                        break;

                                    //fallthrough group x 
                                    case values.labelGroup:
                                    case values.label:
                                    case values.networkAdress:
                                    case values.bucket:
                                    case values.tag:
                                    case values.route:
                                        break;
                                    case values.volume:
                                        var toAdd;
                                        if (oldModel && oldModel.volumeMap[key]) {
                                            toAdd = oldModel.volumeMap[key];
                                            toAdd.reload(val, map);
                                        } else {
                                            toAdd = new Volume(val, map);
                                        }

                                        toAdd.isTopology = type === values.topology;
                                        that.volumeMap[key] = toAdd;
                                        break;

                                    // fallthrough group x 
                                    case values.routingTable:
                                    case values.ontology:
                                    case values.POSIX_IOBenchmark:
                                    case values.address:
                                        break;
                                    case values.spaPolicyData:
                                    case values.spaPolicyAction:
                                        var toAdd;
                                        if (oldModel && oldModel.policyMap[key]) {
                                            toAdd = oldModel.policyMap[key];
                                            toAdd.reload(val, map);
                                        } else {
                                            toAdd = new Policy(val, map);
                                        }
                                        that.policyMap[key] = toAdd;
                                        break;
                                    default:
                                        console.log("Unknown type: " + type);
                                        break;
                                }
                            });                                                
                        }
                    }
                    
                    for (var key in that.policyMap) {
                        var policy = that.policyMap[key];
                        var policy_ = policy._backing;
                        for (var key in policy_) {
                            switch(key) {
                                case values.spaType:
                                    if (policy.getTypeDetailed() === "PolicyAction")
                                    policy.data = policy_[key][0].value;
                                    break;
                                case values.spaImportFrom:
                                    var ifs = policy_[key];
                                    map_(ifs, function (if_key) {
                                        if_key = if_key.value;

                                        var i_f = that.policyMap[if_key];                                       
                                        var toAdd = new Edge(i_f, policy); 
                                        toAdd.edgeType = "importFrom";
                                        that.policyEdges.push(toAdd);                                                                     
                                    });
                                    break;
                                case values.spaExportTo:
                                    var ets = policy_[key];
                                    map_(ets, function (et_key) {
                                        et_key = et_key.value;
                                        var e_t = that.policyMap[et_key];
                                        var toAdd = new Edge(policy, e_t);
                                        toAdd.edgeType = "exportTo";
                                        that.policyEdges.push(toAdd);     
                                    });                                    
                                    break;
                                case values.spaDependOn:
                                    var dos = policy_[key];
                                    map_(dos, function (do_key) {
                                        do_key = do_key.value;
                                        var d_o = that.policyMap[do_key];
                                        var toAdd = new Edge(policy, d_o);
                                        toAdd.edgeType = "dependOn";
                                        that.policyEdges.push(toAdd);                                                                    
                                     });
                                    break;
                                case values.spaValue:
                                    if (policy.getTypeDetailed() === "PolicyData")
                                    policy.data = policy_[key][0].value;
                                    break;
                                case values.spaFormat:
                                    if (policy.getTypeDetailed() === "PolicyData")
                                    policy.data = policy_[key][0].value;                                    
                                    break;
                                default:
                                    console.log("Unknown policy attribute: " + key);
                            }
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
                                        subnet.origin = service;
                                    });
                                    break;
                                case values.spaDependOn:
                                    var dos = service_[key];
                                    map_(dos, function (do_key) {
                                        do_key = do_key.value;
                                        var d_o = that.policyMap[do_key];
                                        var toAdd = new Edge(service, d_o);
                                        toAdd.edgeType = "dependOn";
                                        that.policyEdges.push(toAdd);                                    
                                    });                                    
                                    
                                    break;                                    
                                default:
                                    console.log("Unknown service attribute: " + key);

                            }
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
                                // Asymmetrical aliases
                                port.alias = aliasPort;
                                // The alias' alias key 
                                var otherAliasKey = that.portMap[aliasKey[0].value]._backing[values.isAlias];
                                // If the alias has the current port as an alias in the model, represent that
                                // in the object. Otherwise set its alias to null. 
                                if (otherAliasKey && (otherAliasKey[0].value === key)) {
                                    aliasPort.alias = port;
                                } else {
                                    aliasPort.alias = null;
                                }
                            }
                        } else {
                            port.alias = null;
                        }
                        port.childrenPorts = [];
                        var childrenKeys = port_[values.hasBidirectionalPort];
                        if (childrenKeys) {
                            map_(childrenKeys, function (childKey) {
                                var child = that.portMap[childKey.value];
                                try {
                                    port.childrenPorts.push(child);
                                    child.parentPort = port;
                                } catch (err) {
                                    console.log("Port Children Error!");
                                }
                            });
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
                                case values.spaDependOn:
                                    var dos = subnet_[key];
                                    map_(dos, function (do_key) {
                                        do_key = do_key.value;
                                        var d_o = that.policyMap[do_key];
                                        var toAdd = new Edge(subnet, d_o);
                                        toAdd.edgeType = "dependOn";
                                        that.policyEdges.push(toAdd);                                    });                                    
                                    
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
                        var node = that.nodeMap[key];
                        var node_ = node._backing;
                        for (var key in node_) {
                            console.log("key: " + key);
                            switch (key) {
                                case values.hasBidirectionalPort:
                                    var ports = node_[key];
                                    map_(ports, function (portKey) {
                                        portKey = portKey.value;
                                        var errorVal = portKey;
                                        var port = that.portMap[portKey];
                                        if (!port || node.ports.indexOf(port) !== -1) {
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
                                            console.log("No subnode: " + errorVal);
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
                                    break;
                                case values.type:
                                case values.hasNetworkAddress:
                                case values.provideByService:
                                case values.hasBucket:
                                case values.belongsTo:
                                case values.name:                           
                                    break;                        
                                case values.volume:
                                    break;
                                case values.hasVolume:
                                    var volumes = node_[key];
                                    map_(volumes, function (volume) {
                                        var errorVal = volume.value;
                                        volume = that.volumeMap[volume.value];
                                        // bandaid fix
                                        if (!volume || node.volumes.indexOf(volume) !== -1) {
                                            //service is undefined
                                            console.log("No volume: " + errorVal);
                                        } else {
                                            node.volumes.push(volume);
                                            volume.parentNode = node;
                                        }
                                    });
                                    break;
                                case values.num_core:
                                case values.memory_mb:
                                case values.mount_point:
                                case values.measurement:
                                case values.topoType:
                                case values.hasTag:                            
                                    break;
                                case values.spaDependOn:
                                    var dos = node_[key];
                                    map_(dos, function (do_key) {
                                        do_key = do_key.value;
                                        var d_o = that.policyMap[do_key];
                                        var toAdd = new Edge(node, d_o);
                                        toAdd.edgeType = "dependOn";
                                        that.policyEdges.push(toAdd);
                                    });                                    
                                    
                                    break;                                      
                                    
                                default:                         
                                    console.log("Unknown key: " + key);
                            }
                        }
                    }

                    // Goes through all of the nodes and if they have a switching service 
                    // that has ports that the node doesn't, adds the ports to the node
                    for (var key in that.nodeMap) {
                        var node = that.nodeMap[key];
                        map_(node.services, function (service) {
                            var service_ = service._backing;
                            var types = service_[values.type];
                            map_(types, function (type) {
                                type = type.value;
                                if (type === values.switchingService) {
                                    var switchingServicePorts = service._backing[values.hasBidirectionalPort];
                                    if (switchingServicePorts !== undefined) {
                                        map_(switchingServicePorts, function (port) {
                                            var portObj = that.portMap[port.value];
                                            if (portObj !== undefined && node.ports.indexOf(portObj) === -1) {
                                                node.ports.push(portObj);
                                                portObj.setNode(node);
                                            }
                                        });
                                    }
                                }
                            });
                        });
                    }


                    // Storing the relationships between all of the elemnts
                    // Relationships stored in an <Element, Type> map, storing the
                    // element it has the relationship to and what the relationship
                    // is. 
                    for (var key in that.elementMap) {               
                        var src_element = that.elementMap[key];
                        if ((src_element.getType() === "Node" ||
                                src_element.getType() === "Topology")
                                && that.nodeMap[src_element.getName()].isLeaf()) 
                        {
                            src_element.topLevel = false;
                        }
                        if (src_element !== undefined) {
                            var src_element_ = src_element._backing;
                            for (var key in src_element_) {

                                // Elements with key 'name' are always undefined. 
                                if (key === "name") continue;

                                var elements = src_element_[key];
                                map_(elements, function (element){
                                    var errorVal = element.value;
                                    element = that.elementMap[element.value];
                                    if (element) {
                                        var relationship =  key.split("#")[1];
                                        var src_type = src_element.getType();
                                        var type = element.getType();
                                        if ((type !== "Topology" && type !== "Node") ||
                                            ((src_type === "Topology" || src_type === "Node")  && 
                                            (relationship === "hasTopology")))
                                        {
                                            element.topLevel = false;
                                        }
                                        element.relationship_to[src_element.getName()] = relationship;
                                        src_element.misc_elements.push(element);                                  
                                    } else {
                                        //  console.log("name: " + key.split("#")[1] + " value: " + errorVal);
                                    }
                                }); 
                            }
                        }
                    }

                    // New unidirectional alias code 
                    for (var key in that.elementMap) {               
                        var src_element = that.elementMap[key];
                        for (var i in src_element.misc_elements) {
                            var elem = src_element.misc_elements[i];
                            if (elem.relationship_to[src_element.getName()] === "isAlias"){
                                    var port1 = that.portMap[src_element.getName()];
                                    var port2 = that.portMap[elem.getName()];
                                    if (port1 && port2) {
                                        port1.alias = port2;
                                        port2.alias = port1;
                                    }
                            }
                        }
                    }

                    //console.log ("ELEMENTS: \n\n\n" + Object.keys(that.elementMap).toString() + "\n\n\n\n");
                    for (var key in that.nodeMap) {
                        var node = that.nodeMap[key];
                        if (node.isRoot) {
                            rootNodes.push(node);
                        }
                    }
                    callback();
                };
                request.send();
            }
        };
        
        this.getHostNodeURN = function(urn){
            var e = that.elementMap[urn];
            
            if (!e) return null;
            for (var rel in e.relationship_to){
                if (that.elementMap[rel].getType() === "Node" ||
                    that.elementMap[rel].getType() === "Topology")
                    return rel;
            }
            return null;
        };
        
        this.getOrigin = function(urn) {
            var e = that.elementMap[urn];
            
            if (!e) return null;
            
            switch(e.getType()){
                case "Topology":
                case "Node":
                    return that.nodeMap[urn];
                case "SwitchingService":
                case "HypervisorService":
                case "RoutingService":
                case "VirtualCloudService":
                case "BlockStorageService":
                case "ObjectStorageService":
                case "VirtualSwitchService":
                case "HypervisorBypassInterfaceService":
                case "StorageService":
                case "IOPerformanceMeasurementService":
                case "DataTransferService":
                case "DataTransferClusterService":
                case "NetworkObject":
                case "Service":
                    return that.serviceMap[urn];
                case "Port":
                case "BidirectionalPort":
                    return that.portMap[urn];
                case "Volume":
                    return that.volumeMap[urn];
                case "Subnet":
                case "SwitchingSubnet":
                    return that.subnetMap[urn];
                default: return null;
            }
        };

         this.getBaseOrigin = function(urn) {
            var e = that.elementMap[urn];
            
            if (!e) return null;
            
            switch(e.getType()){
                case "Topology":
                case "Node":
                    return new Node(that.nodeMap[urn]._backing, 
                                  that.nodeMap[urn]._map);
                case "SwitchingService":
                case "HypervisorService":
                case "RoutingService":
                case "VirtualCloudService":
                case "BlockStorageService":
                case "ObjectStorageService":
                case "VirtualSwitchService":
                case "HypervisorBypassInterfaceService":
                case "StorageService":
                case "IOPerformanceMeasurementService":
                case "DataTransferService":
                case "DataTransferClusterService":
                case "NetworkObject":
                case "Service":
                    return new Service(that.serviceMap[urn]._backing, 
                                       that.serviceMap[urn]._map);
                case "Port":
                case "BidirectionalPort":
                    return new Port(that.portMap[urn]._backing, 
                                    that.portMap[urn]._map);
                case "Volume":
                    return new Volume(that.volumeMap[urn]._backing, 
                                      that.volumeMap[urn]._map);
                case "Subnet":
                case "SwitchingSubnet":
                    return new Subnet(that.subnetMap[urn]._backing, 
                                      that.subnetMap[urn]._map);
                default: return null;
            }
        };
                
        this.makeSubModel = function(mapList) { 

            var nodeMap = {};
            var portMap = {};
            var serviceMap = {};
            var subnetMap = {};
            var volumeMap = {};
            for (var i in mapList) {
                if (mapList[i] === undefined || mapList[i] === {}) continue;

                for (var key in mapList[i]) {
                    //var val = mapList[i][key];
                    //val.name = key;
                    //console.log("JSON.stringify(element, null, 2): " + JSON.stringify(val, null, 2));
                    var map = mapList[i];
                    //alert(key);
                    var val = map[key];
                    val.name = key;
                    //console.log("JSON.stringify(element, null, 2): " + JSON.stringify(val, null, 2));                    
                    var types = val[values.type];
                    if (!types) {
                        var hostURN = that.getHostNodeURN(key);
                        var obj = that.getOrigin(key);
                        if (hostURN) nodeMap[hostURN] = that.nodeMap[hostURN];

                        if (obj) {
                            switch(obj.getType()){
                             case "Topology":
                             case "Node":
                                  nodeMap[key] = obj;
                                  break;
                             case "SwitchingService":
                             case "HypervisorService":
                             case "RoutingService":
                             case "VirtualCloudService":
                             case "BlockStorageService":
                             case "ObjectStorageService":
                             case "VirtualSwitchService":
                             case "HypervisorBypassInterfaceService":
                             case "StorageService":
                             case "IOPerformanceMeasurementService":
                             case "DataTransferService":
                             case "DataTransferClusterService":
                             case "NetworkObject":
                             case "Service":
                                 serviceMap[key] = obj;
                                 break;
                             case "Port":
                             case "BidirectionalPort":
                                 portMap[key] = obj;
                                 break;
                             case "Volume":
                                 volumeMap[key] = obj;
                                 break;
                            }
                        }
                       //console.log("Types empty!\n\nVal: " + val + "\nName: " + val.name);
                    } else {
                        map_(types, function (type) {
                            type = type.value;

                            switch (type) {
                                // Fallthrough group 
                                case values.topology:
                                case values.node:
                                    console.log("type: " + type);
                                    var toAdd;
                                    if (that.nodeMap[key]) {
                                        toAdd = that.nodeMap[key];
                                        nodeMap[key] = toAdd;
                                    }
                                    break;

                                case values.bidirectionalPort:
                                    var toAdd;
                                    if (that.portMap[key]) {
                                        toAdd = that.portMap[key];
                                        portMap[key] = toAdd;
                                    } 
                                    break;

                                    // Fallthrough group     
                                case values.switchingService:
                                case values.topopolgySwitchingService:
                                case values.hypervisorService:
                                case values.routingService:
                                case values.virtualCloudService:
                                case values.blockStorageService:
                                    var toAdd;
                                    if (that.serviceMap[key]) {
                                        toAdd = that.serviceMap[key];
                                        serviceMap[key] = toAdd;
                                    } 
                                    break;

                                    // Fallthrough group 
                                case values.objectStorageService:
                                case values.virtualSwitchingService:
                                case values.hypervisorBypassInterfaceService:
                                case values.storageService:
                                case values.IOPerformanceMeasurementService:
                                case values.DataTransferService:
                                case values.DataTransferClusterService:
                                case values.NetworkObject:
                                    var toAdd;
                                    if (that.serviceMap[key]) {
                                        toAdd = that.serviceMap[key];
                                        serviceMap[key] = toAdd;
                                    } 
                                    break;

                                case values.switchingSubnet:
                                    var toAdd;
                                    if (that.subnetMap[key]) {
                                        toAdd = that.subnetMap[key];
                                        subnetMap[key] = toAdd;
                                    }
                                    break;
                                case values.namedIndividual://All elements have this
                                    break;

                                //fallthrough group x 
                                case values.labelGroup:
                                case values.label:
                                case values.networkAdress:
                                case values.bucket:
                                case values.tag:
                                case values.route:
                                    break;
                                case values.volume:
                                    var toAdd;
                                    if (that.volumeMap[key]) {
                                        toAdd = that.volumeMap[key];
                                        volumeMap[key] = toAdd;
                                    }
                                    break;

                                // fallthrough group x 
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
            }
            return {
                nodeMap: nodeMap,
                portMap: portMap,
                serviceMap: serviceMap,
                subnetMap: subnetMap,
                volumeMap: volumeMap
            };
        };    
 
        this.makeServiceDtlModel = function (map, baseModel) {
            that.nodeMap = {};
            that.elementMap = {};
            that.policyMap = {};
            that.policyEdges = [];
            
            for (var key in map) {
                var val = map[key];
                val.name = key;

                var types = val[values.type];
                var detailsReference = false;
                if (!types) {
                    if(val[values.topoType]) {
                        types = val[values.topoType];
                    } else if (baseModel.getBaseOrigin(key)) {
                        types = baseModel.getBaseOrigin(key)._map[key][values.type];
                        val[values.type] = types;
                        detailsReference = true;
                    } else {
                        continue;
                    }
                }

                that.elementMap[key] = new Element(val, map, that.elementMap);

                map_(types, function (type) {
                  type = type.value;

                  // Adding every element to the elementMap for the 
                  // displayPanel.  Ifnoring elemnets with the type "NamedIndividual"
                  if (type.split("#")[1] === "NamedIndividual") return "";//                                                

                  switch (type) {
                      // Fallthrough group 
                      case values.topology:
                      case values.node:
                      case values.bidirectionalPort:
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
                      case values.switchingSubnet:
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
                          var toAdd;
                          toAdd = new Node(val, map);
                          toAdd.isTopology = type === values.topology;
                          that.nodeMap[key] = toAdd;
                          break;                             
                      case values.spaPolicyData:
                      case values.spaPolicyAction:
                          var toAdd;
                          toAdd = new Policy(val, map);

                          that.policyMap[key] = toAdd;
                          break;                                        
                      default:
                          console.log("Unknown type: " + type);
                          break;
                    }
                });     
            }
            
            for (var key in that.policyMap) {
                var policy = that.policyMap[key];
                var policy_ = policy._backing;
                for (var key in policy_) {
                    switch(key) {
                        case values.spaType:
                            if (policy.getTypeDetailed() === "PolicyAction")
                            policy.data = policy_[key][0].value;
                            break;
                        case values.spaImportFrom:
                            var ifs = policy_[key];
                            map_(ifs, function (if_key) {
                                if_key = if_key.value;

                                var i_f = that.policyMap[if_key];                                       
                                var toAdd = new Edge(i_f, policy); 
                                toAdd.edgeType = "importFrom";
                                that.policyEdges.push(toAdd);                                                                     
                            });
                            break;
                        case values.spaExportTo:
                            var ets = policy_[key];
                            map_(ets, function (et_key) {
                                et_key = et_key.value;
                                var e_t = that.policyMap[et_key];
                                var toAdd = new Edge(policy, e_t);
                                toAdd.edgeType = "exportTo";
                                that.policyEdges.push(toAdd);     
                            });                                    
                            break;
                        case values.spaDependOn:
                            var dos = policy_[key];
                            map_(dos, function (do_key) {
                                do_key = do_key.value;
                                var d_o = that.policyMap[do_key];
                                var toAdd = new Edge(policy, d_o);
                                toAdd.edgeType = "dependOn";
                                that.policyEdges.push(toAdd);                                                                    
                             });
                            break;
                        case values.spaValue:
                            if (policy.getTypeDetailed() === "PolicyData")
                            policy.data = policy_[key][0].value;
                            break;
                        case values.spaFormat:
                            if (policy.getTypeDetailed() === "PolicyData")
                            policy.data = policy_[key][0].value;                                    
                            break;
                        default:
                            console.log("Unknown policy attribute: " + key);
                    }
                }
            }
            //Associate ports and subnodes with their parent node
           //Create services
           for (var key in that.nodeMap) {
               /**@type Node**/
               var node = that.nodeMap[key];
               var node_ = node._backing;
               for (var key in node_) {
                   console.log("key: " + key);
                   switch (key) {
                       case values.spaDependOn:
                           var dos = node_[key];
                           map_(dos, function (do_key) {
                               do_key = do_key.value;
                               var d_o = that.policyMap[do_key];
                               var toAdd = new Edge(node, d_o);
                               toAdd.edgeType = "dependOn";
                               that.policyEdges.push(toAdd);
                           });                                    
                           break;                                                                          
                       default:                         
                           console.log("Unknown key: " + key);
                   }
               }
           }
           
        // Storing the relationships between all of the elemnts
        // Relationships stored in an <Element, Type> map, storing the
        // element it has the relationship to and what the relationship
        // is. 
        for (var key in that.elementMap) {               
            var src_element = that.elementMap[key];
            if ((src_element.getType() === "Node" ||
                    src_element.getType() === "Topology")
                    && that.nodeMap[src_element.getName()].isLeaf()) 
            {
                src_element.topLevel = false;
            }
            if (src_element !== undefined) {
                var src_element_ = src_element._backing;
                for (var key in src_element_) {

                    // Elements with key 'name' are always undefined. 
                    if (key === "name") continue;

                    var elements = src_element_[key];
                    map_(elements, function (element){
                        var errorVal = element.value;
                        element = that.elementMap[element.value];
                        if (element) {
                            var relationship =  key.split("#")[1];
                            var src_type = src_element.getType();
                            var type = element.getType();
                            if ((type !== "Topology" && type !== "Node") ||
                                ((src_type === "Topology" || src_type === "Node")  && 
                                (relationship === "hasTopology")))
                            {
                                element.topLevel = false;
                            }
                            element.relationship_to[src_element.getName()] = relationship;
                            src_element.misc_elements.push(element);                                  
                        } else {
                            //  console.log("name: " + key.split("#")[1] + " value: " + errorVal);
                        }
                    }); 
                }
            }
        }
           
            for (var key in that.nodeMap) {
                var node = that.nodeMap[key];
                if (node) {
                    rootNodes.push(node);
                }
            }                   
        };
        
        this.initWithMap = function(map, baseModel) {
                    versionID = map.version;
                    
                    that.nodeMap = {};
                    that.portMap = {};
                    that.serviceMap = {};
                    that.subnetMap = {};
                    that.volumeMap = {};
                    that.elementMap = {};
                    that.policyMap = {};
                    that.policyEdges = [];
               
                    for (var key in map) {
                        var val = map[key];
                        val.name = key;
                        //console.log("JSON.stringify(element, null, 2): " + JSON.stringify(val, null, 2));
                        var hostURN = baseModel.getHostNodeURN(key);
                        if (hostURN && !val[values.type]) {
                            that.nodeMap[hostURN] = new Node(baseModel.nodeMap[hostURN]._backing, 
                                                                          baseModel.nodeMap[hostURN].map);
                            that.nodeMap[hostURN].detailsReference = true;
                            
                            that.elementMap[hostURN] = new Element(baseModel.nodeMap[hostURN]._backing, 
                                                                   baseModel.nodeMap[hostURN].map, 
                                                                   that.elementMap);
                            that.elementMap[hostURN].topLevel = true;        
                                                                          
                        }
                        
                        var types = val[values.type];
                        var detailsReference = false;
                        if (!types) {
                            if(val[values.topoType]) {
                                types = val[values.topoType];
                            } else if (baseModel.getBaseOrigin(key)) {
                                types = baseModel.getBaseOrigin(key)._map[key][values.type];
                                val[values.type] = types;
                                detailsReference = true;
                            } else {
                                continue;
                            }
                        }
                        
                        that.elementMap[key] = new Element(val, map, that.elementMap);
                        that.elementMap[key].topLevel = true;        

                        map_(types, function (type) {
                            type = type.value;

                            // Adding every element to the elementMap for the 
                            // displayPanel.  Ifnoring elemnets with the type "NamedIndividual"
                            if (type.split("#")[1] === "NamedIndividual") return "";//                                                

                            switch (type) {
                                // Fallthrough group 
                                case values.topology:
                                case values.node:
                                    console.log("type: " + type);
                                    var toAdd;
                                    toAdd = new Node(val, map);
                                    toAdd.isTopology = type === values.topology;
                                    if (detailsReference) toAdd.detailsReference = true;
                                    that.nodeMap[key] = toAdd;
                                    break;

                                case values.bidirectionalPort:
                                    var toAdd;
                                    toAdd = new Port(val, map);
                                    if (detailsReference) toAdd.detailsReference = true;                                        
                                    that.portMap[key] = toAdd;
                                    break;

                                    // Fallthrough group     
                                case values.switchingService:
                                case values.topopolgySwitchingService:
                                case values.hypervisorService:
                                case values.routingService:
                                case values.virtualCloudService:
                                case values.blockStorageService:
                                    var toAdd;
                                    toAdd = new Service(val, map);
                                    if (detailsReference) toAdd.detailsReference = true;                                        
                                    that.serviceMap[key] = toAdd;
                                    break;

                                    // Fallthrough group 
                                case values.objectStorageService:
                                case values.virtualSwitchingService:
                                case values.hypervisorBypassInterfaceService:
                                case values.storageService:
                                case values.IOPerformanceMeasurementService:
                                case values.DataTransferService:
                                case values.DataTransferClusterService:
                                case values.NetworkObject:
                                    var toAdd;
                                    toAdd = new Service(val, map);
                                    if (detailsReference) toAdd.detailsReference = true;                                        
                                    that.serviceMap[key] = toAdd;
                                    break;

                                case values.switchingSubnet:
                                    var toAdd;
                                    toAdd = new Subnet(val, map);
                                    if (detailsReference) toAdd.detailsReference = true;                                        
                                    that.subnetMap[key] = toAdd;
                                    break;
                                case values.namedIndividual://All elements have this
                                    break;

                                //fallthrough group x 
                                case values.labelGroup:
                                case values.label:
                                case values.networkAdress:
                                case values.bucket:
                                case values.tag:
                                case values.route:
                                    break;
                                case values.volume:
                                    var toAdd;
                                    toAdd = new Volume(val, map);

                                    toAdd.isTopology = type === values.topology;
                                     if (detailsReference) toAdd.detailsReference = true;                                       
                                    that.volumeMap[key] = toAdd;
                                    break;

                                // fallthrough group x 
                                case values.routingTable:
                                case values.ontology:
                                case values.POSIX_IOBenchmark:
                                case values.address:
                                    break;
                                case values.spaPolicyData:
                                case values.spaPolicyAction:
                                    var toAdd;
                                    toAdd = new Policy(val, map);
                                    if (detailsReference) toAdd.detailsReference = true;                                       

                                    that.policyMap[key] = toAdd;
                                    break;                                        
                                default:
                                    console.log("Unknown type: " + type);
                                    break;
                            }
                        });                                                

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
                                        if (subnet) {
                                            service.subnets.push(subnet);
                                            subnet.origin = service;
                                        }
                                    });
                                    break;
                                case values.spaDependOn:
                                    var dos = service_[key];
                                    map_(dos, function (do_key) {
                                        do_key = do_key.value;
                                        var d_o = that.policyMap[do_key];
                                        var toAdd = new Edge(service, d_o);
                                        toAdd.edgeType = "dependOn";
                                        that.policyEdges.push(toAdd);                                    
                                    });                                                                      
                                    break;                                    
                                default:
                                    console.log("Unknown service attribute: " + key);

                            }
                        }

                    }

                    for (var key in that.policyMap) {
                        var policy = that.policyMap[key];
                        var policy_ = policy._backing;
                        for (var key in policy_) {
                            switch(key) {
                                case values.spaType:
                                    if (policy.getTypeDetailed() === "PolicyAction")
                                    policy.data = policy_[key][0].value;
                                    break;
                                case values.spaImportFrom:
                                    var ifs = policy_[key];
                                    map_(ifs, function (if_key) {
                                        if_key = if_key.value;

                                        var i_f = that.policyMap[if_key];                                       
                                        var toAdd = new Edge(i_f, policy); 
                                        toAdd.edgeType = "importFrom";
                                        that.policyEdges.push(toAdd);                                                                     
                                    });
                                    break;
                                case values.spaExportTo:
                                    var ets = policy_[key];
                                    map_(ets, function (et_key) {
                                        et_key = et_key.value;
                                        var e_t = that.policyMap[et_key];
                                        var toAdd = new Edge(policy, e_t);
                                        toAdd.edgeType = "exportTo";
                                        that.policyEdges.push(toAdd);     
                                    });                                    
                                    break;
                                case values.spaDependOn:
                                    var dos = policy_[key];
                                    map_(dos, function (do_key) {
                                        do_key = do_key.value;
                                        var d_o = that.policyMap[do_key];
                                        var toAdd = new Edge(policy, d_o);
                                        toAdd.edgeType = "dependOn";
                                        that.policyEdges.push(toAdd);                                                                    
                                     });
                                    break;
                                case values.spaValue:
                                    if (policy.getTypeDetailed() === "PolicyData")
                                    policy.data = policy_[key][0].value;
                                    break;
                                case values.spaFormat:
                                    if (policy.getTypeDetailed() === "PolicyData")
                                    policy.data = policy_[key][0].value;                                    
                                    break;
                                default:
                                    console.log("Unknown policy attribute: " + key);
                            }
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
                                // Asymmetrical aliases
                                port.alias = aliasPort;
                                // The alias' alias key 
                                var otherAliasKey = that.portMap[aliasKey[0].value]._backing[values.isAlias];
                                // If the alias has the current port as an alias in the model, represent that
                                // in the object. Otherwise set its alias to null. 
                                if (otherAliasKey && (otherAliasKey[0].value === key)) {
                                    aliasPort.alias = port;
                                } else {
                                    aliasPort.alias = null;
                                }
                            }
                        } else {
                            port.alias = null;
                        }
                        port.childrenPorts = [];
                        var childrenKeys = port_[values.hasBidirectionalPort];
                        if (childrenKeys) {
                            map_(childrenKeys, function (childKey) {
                                var child = that.portMap[childKey.value];
                                if (child) {
                                    try {
                                        port.childrenPorts.push(child);
                                        child.parentPort = port;
                                    } catch (err) {
                                        console.log("Port Children Error!");
                                    }
                                }
                            });
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
                                case values.spaDependOn:
                                    var dos = subnet_[key];
                                    map_(dos, function (do_key) {
                                        do_key = do_key.value;
                                        var d_o = that.policyMap[do_key];
                                        var toAdd = new Edge(subnet, d_o);
                                        toAdd.edgeType = "dependOn";
                                        that.policyEdges.push(toAdd);                                    });                                    
                                    
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
                        var node = that.nodeMap[key];
                        var node_ = node._backing;
                        for (var key in node_) {
                            console.log("key: " + key);
                            switch (key) {
                                case values.hasBidirectionalPort:
                                    var ports = node_[key];
                                    map_(ports, function (portKey) {
                                        portKey = portKey.value;
                                        var errorVal = portKey;
                                        var port = that.portMap[portKey];
                                        if (!port || node.ports.indexOf(port) !== -1) {
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
                                            console.log("No subnode: " + errorVal);
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
                                    break;
                                case values.type:
                                case values.hasNetworkAddress:
                                case values.provideByService:
                                case values.hasBucket:
                                case values.belongsTo:
                                case values.name:                           
                                    break;                        
                                case values.volume:
                                    break;
                                case values.hasVolume:
                                    var volumes = node_[key];
                                    map_(volumes, function (volume) {
                                        var errorVal = volume.value;
                                        volume = that.volumeMap[volume.value];
                                        // bandaid fix
                                        if (!volume || node.volumes.indexOf(volume) !== -1) {
                                            //service is undefined
                                            console.log("No volume: " + errorVal);
                                        } else {
                                            node.volumes.push(volume);
                                            volume.parentNode = node;
                                        }
                                    });
                                    break;
                                case values.num_core:
                                case values.memory_mb:
                                case values.mount_point:
                                case values.measurement:
                                case values.topoType:
                                case values.hasTag:                            
                                    break;
                                case values.spaDependOn:
                                    var dos = node_[key];
                                    map_(dos, function (do_key) {
                                        do_key = do_key.value;
                                        var d_o = that.policyMap[do_key];
                                        var toAdd = new Edge(node, d_o);
                                        toAdd.edgeType = "dependOn";
                                        that.policyEdges.push(toAdd);
                                    });                                    
                                    
                                    break;                                                                          
                                default:                         
                                    console.log("Unknown key: " + key);
                            }
                        }
                    }

                    // Goes through all of the nodes and if they have a switching service 
                    // that has ports that the node doesn't, adds the ports to the node
                    for (var key in that.nodeMap) {

                        var node = that.nodeMap[key];
                        map_(node.services, function (service) {
                            var service_ = service._backing;
                            var types = service_[values.type];
                            map_(types, function (type) {
                                type = type.value;
                                if (type === values.switchingService) {
                                    var switchingServicePorts = service._backing[values.hasBidirectionalPort];
                                    if (switchingServicePorts !== undefined) {
                                        map_(switchingServicePorts, function (port) {
                                            var portObj = that.portMap[port.value];
                                            if (portObj !== undefined && node.ports.indexOf(portObj) === -1) {
                                                node.ports.push(portObj);
                                                portObj.setNode(node);
                                            }
                                        });
                                    }
                                }
                            });
                        });
                    }


                    // Storing the relationships between all of the elemnts
                    // Relationships stored in an <Element, Type> map, storing the
                    // element it has the relationship to and what the relationship
                    // is. 
                    for (var key in that.elementMap) {        

                        var src_element = that.elementMap[key];
                        if ((src_element.getType() === "Node" ||
                                src_element.getType() === "Topology")
                                && that.nodeMap[src_element.getName()].isLeaf()) 
                        {
                            src_element.topLevel = false;
                        }
                        if (src_element !== undefined) {
                            var src_element_ = src_element._backing;
                            for (var key in src_element_) {

                                // Elements with key 'name' are always undefined. 
                                if (key === "name") continue;

                                var elements = src_element_[key];
                                map_(elements, function (element){
                                    var errorVal = element.value;
                                    element = that.elementMap[element.value];
                                    if (element) {
                                        var relationship =  key.split("#")[1];
                                        var src_type = src_element.getType();
                                        var type = element.getType();
                                        if ((type !== "Topology" && type !== "Node") ||
                                            ((src_type === "Topology" || src_type === "Node")  && 
                                            (relationship === "hasTopology")))
                                        {
                                            element.topLevel = false;
                                        }
                                        element.relationship_to[src_element.getName()] = relationship;
                                        src_element.misc_elements.push(element);                                  
                                    } else {
                                        //  console.log("name: " + key.split("#")[1] + " value: " + errorVal);
                                    }
                                }); 
                            }
                        }
                    }

                    // New unidirectional alias code 
                    for (var key in that.elementMap) {   

                        var src_element = that.elementMap[key];
                        for (var i in src_element.misc_elements) {
                            var elem = src_element.misc_elements[i];
                            if (elem.relationship_to[src_element.getName()] === "isAlias"){
                                    var port1 = that.portMap[src_element.getName()];
                                    var port2 = that.portMap[elem.getName()];
                                    if (port1 && port2) {
                                        port1.alias = port2;
                                        port2.alias = port1;
                                    }
                            }
                        }
                    }
                    
                    for (var key in that.nodeMap) {
                        var node = that.nodeMap[key];
                        if (node.isRoot) {
                            rootNodes.push(node);
                        }
                    }
        };
        
        this.getVersion = function () {
            return versionID;
        };


        this.getModelMapValues = function(m) {
            var ans = [];
            for (var i in m.nodeMap){
                ans.push(m.nodeMap[i]);
            }
            for (var i in m.serviceMap) {
                ans.push(m.serviceMap[i]);
            }
            for (var i in m.portMap) {
                ans.push(m.portMap[i]);
            }
            for (var i in m.volumeMap) {
                ans.push(m.volumeMap[i]);
            }               
            for (var i in m.subnetMap) {
                ans.push(m.subnetMap[i]);
            }              
            return ans;
        };
        
        
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

        this.listPolicies = function() {
            var policies = [];
            for (var key in that.policyMap) policies.push(that.policyMap[key]);
            return policies;
        };
        
        
        this.listPorts = function() {
            var ports = [];
            for (var key in that.portMap) ports.push(that.portMap[key]);
            return ports;
        };
        
        this.listServices = function() {
            var services = [];
            for (var key in that.serviceMap) services.push(that.serviceMap[key]);  
            return services;            
        };
        
        this.listSubnets = function() {
            var subnets = [];
            for (var key in that.subnetMap) subnets.push(that.subnetMap[key]);   
            return subnets;
        };
        
        this.listVolumes = function() {
            var volumes = [];
            for (var key in that.volumeMap) volumes.push(that.volumeMap[key]);  
            return volumes;
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
