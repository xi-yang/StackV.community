// Get existing versastack namespace, or define new one
var versastack = versastack || {};

versastack.model = function() {
    var nodeDictionary = [];
    var linksList = [];
    var nodesList = [];
    var topologyList = [];

    var owns = Object.prototype.hasOwnProperty;

    var settings = {
        /** Resource paths **/
        baseIconPath: 'resources/',
        defaultIcon: 'default.png',
        defaultGraphPath: 'data/graph.json'
    };

    /** Namespace prefix constants **/
    var prefix = {
        rdfs: 'http://www.w3.org/2000/01/rdf-schema#',
        rdf: 'http://www.w3.org/1999/02/22-rdf-syntax-ns#',
        xsd: 'http://www.w3.org/2001/XMLSchema#',
        owl: 'http://www.w3.org/2002/07/owl#',
        nml: 'http://schemas.ogf.org/nml/2013/03/base#',
        mrs: 'http://schemas.ogf.org/mrs/2013/12/topology#',
        base: 'urn:ogf:network:'
    };

    var types = {
        topology: {
            name: prefix.nml + 'Topology',
            iconName: 'topology.jpg'
        },
        bidirectionalPort: {
            name: prefix.nml + 'BidirectionalPort',
            iconName: 'bidirectional_port.jpg'
        },
        storageService: {
            name: prefix.mrs + 'StorageService',
            iconName: 'storage_service.jpg'
        },
        node: {
            name: prefix.nml + 'Node',
            iconName: 'node.jpg'
        },
        blockStorageService: {
            name: prefix.mrs + 'BlockStorageService',
            iconName: 'block_storage_service.jpg'
        },
        route: {
            name: prefix.mrs + 'Route',
            iconName: 'route.jpg'
        },
        hypervisorBypassInterfaceService: {
            name: prefix.mrs + 'HypervisorBypassInterfaceService',
            iconName: 'hypervisor_bypass_interface_service.jpg'
        },
        switchingService: {
            name: prefix.nml + 'SwitchingService',
            iconName: 'switching_service.jpg'
        },
        virtualSwitchService: {
            name: prefix.mrs + 'VirtualSwitchService',
            iconName: 'virtual_switch_service.jpg'
        },
        routingService: {
            name: prefix.mrs + 'RoutingService',
            iconName: 'routing_service.jpg'
        },
        ioPerformanceMeasurementService: {
            name: prefix.mrs + 'IOPerformanceMeasurementService',
            iconName: 'io_perf_service.jpg'
        },
        posixIOBenchmark: {
            name: prefix.mrs + 'POSIX_IOBenchmark',
            iconName: 'io_benchmark.jpg'
        },
        hypervisorService: {
            name: prefix.mrs + 'HypervisorService',
            iconName: 'hypervisor_service.jpg'
        },
        address: {
            name: prefix.mrs + 'Address'
        }
    };

    var properties = {
        hasNode: prefix.nml + 'hasNode',
        hasService: prefix.nml + 'hasService',
        isAlias: prefix.nml + 'isAlias',
        topology: prefix.nml + 'Topology',
        type: prefix.rdf + 'type',
        labelType: prefix.nml + 'labeltype'
    };

    // Module execution starts here
    function main(jsonPath, loadingItem) {
        // Activate loading indicator if specified
        if (loadingItem) {
            settings.loadingItem = loadingItem;
            versastack.loading.start(loadingItem);
        }

        // Load graph JSON at specified path (if exists) and process it
        var graph = jsonPath || settings.defaultGraphPath;
        d3.json(graph, processJSON);
    }

    function processJSON(error, json) {
        console.log("Processing json...");
        console.log(json);

        // Create a lookup dictionary of all nodes
        populateNodesDictionary(json);

        // Parse graph JSON into data structures and objects
        console.log('Generating nodes and links from json...');
        for (var key in nodeDictionary) {
            var node = nodeDictionary[key];
            console.log('Processing node: ', node);
            if (isTopology(node.jsonObj)) { // store topology objects and relations
                var children = mergeProperties(node.jsonObj[properties.hasNode], node.jsonObj[properties.hasService]);
                var childrenNodes = [];

                for (var key in children) {
                    if (!owns.call(children, key)) {
                        continue;
                    }

                    var target = findByName(nodeDictionary, removePrefix(children[key].value, prefix.base));
                    if (target) {
                        childrenNodes.push(target);
                    }
                }

                node.nodes = childrenNodes;
                node.visible = true;

                console.log('Adding topology');
                console.log(node);

                topologyList.push(node);
                nodesList.push(node);

                // Add any topology links
                for (var key in childrenNodes) {
                    linksList.push({source: node, target: childrenNodes[key], visible: false});
                }
            } else if (shouldBeDisplayed(node.jsonObj)) { // store all other nodes that are displayed
                node.visible = true;
                nodesList.push(node);
            } else { // ignored nodes
                console.log('Ignored element ' + node.name);
            }


            // Add any alias links that exist
            if (hasProperty(node.jsonObj, properties.isAlias)) {
                for (var i = 0, l = node.jsonObj[properties.isAlias].length; i < l; ++i) {
                    var target = findByName(nodeDictionary, node.jsonObj[properties.isAlias][i].value);
                    if (target) {
//                                    linksList.push({source: node, target: target, visible: true});
                    }
                }
            }
        }

        // Hide topology children nodes
        for (var topology in topologyList) {
            for (var key in topologyList[topology].nodes) {
                var node = findByName(nodesList, topologyList[topology].nodes[key].name);
                if (node) {
                    nodesList.splice(indexOfName(nodesList, node.name), 1);
                }
            }
        }

        if (settings.loadingItem) {
            versastack.loading.end(settings.loadingItem); // finished loading
        }
    }

    function populateNodesDictionary(json) {
        console.log("Creating lookup dictionary...");
        for (var key in json) {
            var obj = json[key];
            var name = removePrefix(key, prefix.base);
            var iconPath = getIconPath(obj);

            var node = {name: name, jsonObj: obj, icon: iconPath};
            nodeDictionary.push(node);
        }
    }


    function indexOfName(array, search) {
        for (var i = 0; i < array.length; ++i) {
            if (array[i].name === search) {
                return i;
            }
        }

        return -1;
    }

    function findByName(array, name) {
        var index = indexOfName(array, removePrefix(name, prefix.base));
        if (index >= 0) {
            return array[index];
        } else {
            return null;
        }
    }

    function  mergeProperties(p1, p2) {
        var p3 = p1 || {};

        for (var property in p2) {
            if (owns.call(p2, property)) {
                p3[property] = p2[property];
            }
        }

        return p3;
    }


    function hasProperty(obj, property) {
        for (var objProperty in obj) {
            if (owns.call(obj, objProperty) && objProperty === property) {
                return true;
            }
        }

        return false;
    }

    function hasType(obj, type) {
        for (var objType in obj) {
            var typeValue = obj[objType].value;
            if (owns.call(obj, objType) && typeValue === type) {
                return true;
            }
        }

        return false;
    }

    function removePrefix(string, prefix) {
        var regexp = new RegExp(escapeRegExp(prefix), 'ig');
        return string.replace(regexp, '');
    }

    /**
     * bobince
     * stackoverflow.com/questions/3561493/is-there-a-regexp-escape-function-in-javascript
     */
    function escapeRegExp(s) {
        return s.replace(/[-\/\\^$*+?.()|[\]{}]/g, '\\$&');
    }

    /************************************************************
     * TypeEnum is created on first call if not already defined.
     * TypeEnum is a constant static hash such that:
     * The keys of TypeEnum are fully expanded type names
     * The values of TypeEnum are corresponding icon paths
     * 
     * If TypeEnum holds an icon path for a type it is returned,
     * otherwise a default icon path is returned.
     ************************************************************/
    function getIconPath(obj) {
        if (getIconPath.TypeEnum === undefined) {
            getIconPath.TypeEnum = {};

            for (var key in types) {
                if (owns.call(types, key)) {
                    getIconPath.TypeEnum[types[key].name] = settings.baseIconPath + types[key].iconName;
                }
            }

            Object.freeze(getIconPath.TypeEnum);
        }

        if (hasProperty(obj, properties.type)) {
            for (var key in obj[properties.type]) {
                var type = obj[properties.type][key].value;
                var path = getIconPath.TypeEnum[type];

                if (path) {
                    return path;
                }
            }
        }


        return settings.baseIconPath + settings.defaultIcon;
    }

    /** 
     * Display elements from NML or MRS namesapce
     * Except, do not display labeltypes or addresses
     */
    function shouldBeDisplayed(obj) {
        if (hasProperty(obj, properties.labelType)) {
            return false;
        } else if (hasProperty(obj, properties.type)) {
            for (var type in obj[properties.type]) {
                var typeValue = obj[properties.type][type].value;

                if (typeValue === types.address.name) {
                    return false;
                } else if (typeValue.indexOf('http://schemas.ogf.org/') >= 0) {
                    return true;
                }
            }
        }

        console.log('Could not decide whether to display an object, defaulted to false');
        return false;
    }

    function getNonTopologyNodes(nodes) {
        var ret = [];
        for (var node in nodes) {
            if (owns.call(nodes, node) && !isTopologyNode(nodes[node])) {
                ret.push(nodes[node]);
            }
        }

        return ret;
    }

    function isTopologyNode(node) {
        var topology, key, nodeName, topologyNode;

        for (key in topologyList) {
            if (!owns.call(topologyList, key)) {
                continue;
            }

            topology = topologyList[key];
            for (topologyNode in topology.nodes) {
                if (!owns.call(topology.nodes, topologyNode)) {
                    continue;
                }

                nodeName = topology.nodes[topologyNode].name;
                if (node.name === nodeName) {
                    return true;
                }
            }
        }

        return false;
    }

    function isTopology(obj) {
        if (hasProperty(obj, properties.type)) {
            if (hasType(obj[properties.type], types.topology.name)) {
                return true;
            }
        }

        return false;
    }

    /** PUBLIC INTERFACE **/
    return {
        createModel: main,
        dictionary: nodeDictionary,
        links: linksList,
        nodes: nodesList,
        topologies: topologyList,
        settings: settings,
        prefix: prefix,
        type: types,
        property: properties

    };
}(); // end versastack.mode module