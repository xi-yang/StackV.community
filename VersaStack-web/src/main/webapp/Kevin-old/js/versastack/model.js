define([
    "d3", "local/versastack/utils", "local/versastack/loading"
], function(d3, utils, loading) {
    var nodeDictionary = [];
    var linksList = [];
    var nodesList = [];

    var owns = Object.prototype.hasOwnProperty;

    // Node class definition
    function Node(name, id, jsonObj, icon) {
        this.name = name;
        this.id = id;
        this.json = jsonObj;
        this.icon = icon;
        this.isVisible = shouldBeDisplayed(this.json);
        this.isTopology = hasType(this.json, types.topology);
        this.hasAlias = hasProperty(this.json, properties.isAlias);
        this.hasBenchmark = hasProperty(this.json, properties.hasBenchmark);
        this.hasNode = hasProperty(this.json, properties.hasNode);
        this.hasPort = hasProperty(this.json, properties.hasBidirectionalPort);
        this.hasService = hasProperty(this.json, properties.hasService);
        this.hasParent = function() {
            return this.parentID != null;
        };
    }

    var settings = {
        /** Resource paths **/
        baseIconPath: 'resources/',
        defaultIcon: 'default.png',
        defaultGraphPath: 'data/graph-full.json'
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
        address: {
            name: prefix.mrs + 'Address'
        },
        bidirectionalPort: {
            name: prefix.nml + 'BidirectionalPort',
            iconName: 'bidirectional_port.png'
        },
        blockStorageService: {
            name: prefix.mrs + 'BlockStorageService',
            iconName: 'block_storage_service.png'
        },
        hypervisorBypassInterfaceService: {
            name: prefix.mrs + 'HypervisorBypassInterfaceService',
            iconName: 'hypervisor_bypass_interface_service.png'
        },
        hypervisorService: {
            name: prefix.mrs + 'HypervisorService',
            iconName: 'hypervisor_service.png'
        },
        ioPerformanceMeasurementService: {
            name: prefix.mrs + 'IOPerformanceMeasurementService',
            iconName: 'io_perf_service.png'
        },
        labelGroup: {
            name: prefix.nml + 'LabelGroup'
        },
        namedIndividual: {
            name: prefix.owl + 'NamedIndividual'
        },
        node: {
            name: prefix.nml + 'Node',
            iconName: 'node.png'
        },
        posixIOBenchmark: {
            name: prefix.mrs + 'POSIX_IOBenchmark',
            iconName: 'io_benchmark.png'
        },
        route: {
            name: prefix.mrs + 'Route',
            iconName: 'route.png'
        },
        routingService: {
            name: prefix.mrs + 'RoutingService',
            iconName: 'routing_service.png'
        },
        storageService: {
            name: prefix.mrs + 'StorageService',
            iconName: 'storage_service.jpg'
        },
        switchingService: {
            name: prefix.nml + 'SwitchingService',
            iconName: 'switching_service.png'
        },
        topology: {
            name: prefix.nml + 'Topology',
            iconName: 'topology.png'
        },
        virtualSwitchService: {
            name: prefix.mrs + 'VirtualSwitchService',
            iconName: 'virtual_switch_service.png'
        }
    };

    var properties = {
        average4KBRead: prefix.mrs + 'average_iops_4kb_read',
        average4KBWrite: prefix.mrs + 'average_iops_4kb_write',
        diskGB: prefix.mrs + 'disk_gb',
        encoding: prefix.nml + 'encoding',
        hasBenchmark: prefix.mrs + 'hasBenchmark',
        hasBidirectionalPort: prefix.nml + 'hasBidirectionalPort',
        hasLabelGroup: prefix.nml + 'hasLabelGroup',
        hasNode: prefix.nml + 'hasNode',
        hasService: prefix.nml + 'hasService',
        isAlias: prefix.nml + 'isAlias',
        labelSwapping: prefix.nml + 'labelSwapping',
        labelType: prefix.nml + 'labeltype',
        localAddress: prefix.mrs + 'localAddress',
        maximum4KBRead: prefix.mrs + 'maximum_iops_4kb_read',
        maximum4KBWrite: prefix.mrs + 'maximum_iops_4kb_write',
        nextHop: prefix.mrs + 'nextHop',
        prefix: prefix.mrs + 'prefix',
        providesBenchmark: prefix.mrs + 'providesBenchmark',
        providesPort: prefix.nml + 'providesPort',
        providesRoute: prefix.mrs + 'providesRoute',
        providesVNic: prefix.mrs + 'providesVNic',
        values: prefix.nml + 'values',
        type: prefix.rdf + 'type'
    };

    // Module execution starts here
    function main(jsonPath, loadingItem) {
        // Activate loading indicator if specified
        if (loadingItem) {
            settings.loadingItem = loadingItem;
            loading.start(loadingItem);
        }

        // Load JSON at specified path (if exists) and process it into graph
        var graph = jsonPath || settings.defaultGraphPath;
        nodesList.length = 0; // temporary workaround fix later
        linksList.length = 0;
        nodeDictionary.length = 0;
        d3.json(graph, processJSON);
    }

    /** DATA PROCESSING **/
    function processJSON(error, json) {
        console.info('Processing json...', json);

        // Create a lookup dictionary of nodes
        populateNodeDictionary(json);
        console.info('Generated node dictionary from json: ', nodeDictionary);

        // Parse JSON into graph data structures and objects
        console.info('Generating nodes and links from json...');
        for (var key in nodeDictionary) {
            var node = nodeDictionary[key];

            processChildren(node);
            processServices(node);
            processPorts(node);

            if (node.isVisible) {
                nodesList.push(node);
            } else { // ignored nodes
                console.log('Ignored element ' + node.name);
            }

            // Add any alias links that exist
            if (node.hasAlias) {
                var aliasList = node.json[properties.isAlias];
                processLinks(node, aliasList, true); // add visible link from node to each element of aliasList
            }
        }

        if (settings.loadingItem) {
            loading.end(settings.loadingItem, false); // finished loading
        }

        console.info('Finished processing nodes', nodesList, 'links', linksList);
    }

    function populateNodeDictionary(json) {
        console.info('Creating lookup dictionary...');
        var i = 0; // used to assign unique node ids
        for (var key in json) {
            var name = key;
            var jsonObj = json[key];
            var iconPath = getIconPath(jsonObj);

            var node = new Node(name, i++, jsonObj, iconPath);
            nodeDictionary.push(node);
        }
    }

    function processChildren(node) {
        var children = node.json[properties.hasNode];
        var childrenList = [];

        for (var key in children) {
            if (!owns.call(children, key)) {
                continue;
            }

            // Lookup id of child node in node dictionary
            var childIndex = utils.indexOfName(nodeDictionary, children[key].value);
            if (childIndex >= 0) {
                childrenList.push(childIndex); // add child relationship to parent node
                nodeDictionary[childIndex].parentID = node.id; // add parent relationship to child node
            } else {
                console.error('Trying to add nonexistent child', children[key].value, 'to node', node.name);
            }
        }

        node.children = childrenList;
        processLinks(node, children, true); // add invisible link from parent to each children
    }

    function processServices(node) {
        var services = node.json[properties.hasService];
        var servicesList = [];

        for (var key in services) {
            if (!owns.call(services, key)) {
                continue;
            }

            // Lookup id of service in node dictionary
            var serviceIndex = utils.indexOfName(nodeDictionary, services[key].value);
            if (serviceIndex >= 0) {
                servicesList.push(serviceIndex); // add service to node
                nodeDictionary[serviceIndex].parentID = node.id; // add parent node to service
            } else {
                console.error('Trying to add nonexistent service', services[key].value, 'to node', node.name);
            }
        }

        node.services = servicesList;
        processLinks(node, services, true); // add invisible link from node to each service
    }

    function processPorts(node) {
        var ports = node.json[properties.hasBidirectionalPort];
        var portsList = [];

        for (var key in ports) {
            if (!owns.call(ports, key)) {
                continue;
            }

            // Lookup id of port in node dictionary
            var portIndex = utils.indexOfName(nodeDictionary, ports[key].value);
            if (portIndex >= 0) {
                portsList.push(portIndex); // add port to node
                nodeDictionary[portIndex].parentID = node.id; // add parent node to port
            } else {
                console.error('Trying to add nonexistent port', ports[key].value, 'to node', node.name);
            }
        }

        node.ports = portsList;
        processLinks(node, ports, true); // add invisible link from node to each service
    }

    // Adds a link from source to each element in targetList with chosen visibility
    function processLinks(source, targetList, isVisible) {
        for (var key in targetList) {
            if (!owns.call(targetList, key)) {
                continue;
            }

            var target = utils.findByName(nodeDictionary, targetList[key].value);
            if (target != null) {
                linksList.push({
                    source: source,
                    target: target,
                    visible: isVisible
                });
            } else {
                console.error('Trying to link', source, 'to nonexistent target', target);
            }
        }
    }

    /**
     * Display elements from NML or MRS namesapce only.
     * Except, do not display labeltypes or addresses.
     */
    //TODO rewrite to optimize and improve string matching
    function shouldBeDisplayed(obj) {
        if (hasProperty(obj, properties.labelType)) {
            return false;
        }

        if (hasProperty(obj, properties.type)) {
            for (var type in obj[properties.type]) {
                var typeValue = obj[properties.type][type].value;

                if (typeValue === types.address.name) {
                    return false;
                } else if (typeValue.indexOf(prefix.nml) >= 0 || typeValue.indexOf(prefix.mrs) >= 0) {
                    return true;
                }
            }
        }

        console.warn('Could not explicitly decide whether to display an object', obj, 'defaulted to false');
        return false;
    }
    /** END DATA PROCESSING **/


    /** UTILITY FUNCTIONS **/
    function hasProperty(obj, property) {
        for (var objProperty in obj) {
            if (owns.call(obj, objProperty) && objProperty === property) {
                return true;
            }
        }

        return false;
    }

    function hasType(obj, type) {
        // obj hasType foo => obj hasProperty type
        // so check that obj has a type property first
        if (hasProperty(obj, properties.type)) {
            var objTypeProperty = obj[properties.type];

            // obj can have multiple types so check each against the target type
            for (var objType in objTypeProperty) {
                var typeValue = objTypeProperty[objType].value;
                if (owns.call(objTypeProperty, objType) && typeValue === type.name) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * TypeEnum is created on first call if not already defined.
     * TypeEnum is a constant static hash such that:
     * The keys of TypeEnum are fully expanded type names
     * The values of TypeEnum are corresponding icon paths
     * 
     * If TypeEnum holds an icon path for a type it is returned,
     * otherwise a default icon path is returned.
     */
    function getIconPath(obj) {
        // Create TypeEnum if undefined
        if (getIconPath.TypeEnum === undefined) {
            getIconPath.TypeEnum = {};

            for (var key in types) {
                if (owns.call(types, key)) {
                    if (types[key].iconName != null) {
                        getIconPath.TypeEnum[types[key].name] = settings.baseIconPath + types[key].iconName;
                    } else {
                        getIconPath.TypeEnum[types[key].name] = null;
                    }
                }
            }

            Object.freeze(getIconPath.TypeEnum); // make TypeEnum constant
        }

        // A node can have multiple types so if the object has a type property,
        // check each type for a corresponding icon and return the first icon found
        if (hasProperty(obj, properties.type)) {
            for (var key in obj[properties.type]) {
                var type = obj[properties.type][key].value;
                var path = getIconPath.TypeEnum[type];

                if (path != null) {
                    return path;
                } else {
                    console.warn('No icon path found for type', type);
                }
            }
        }

        // If no icon is found return a default
        return settings.baseIconPath + settings.defaultIcon;
    }
    /** END UTILITY FUNCTIONS **/


    /** PUBLIC INTERFACE **/
    return {
        createModel: main,
        settings: settings,
        prefix: prefix,
        type: types,
        property: properties,
        Node: Node,
        data: {
            dictionary: nodeDictionary,
            links: linksList,
            nodes: nodesList
        }
    };
    /** END PUBLIC INTERFACE **/

});