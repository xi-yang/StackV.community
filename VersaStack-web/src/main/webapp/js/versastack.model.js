// Get existing versastack namespace, or define new one
var versastack = versastack || {};

versastack.model = function () {
    var nodeDictionary = [];
    var linksList = [];
    var nodesList = [];
    var topologyList = [];

    var owns = Object.prototype.hasOwnProperty;

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

        // Load JSON at specified path (if exists) and process it into graph
        var graph = jsonPath || settings.defaultGraphPath;
        d3.json(graph, processJSON);
    }

    /** DATA PROCESSING **/
    function processJSON(error, json) {
        console.info('Processing json...', json);

        // Create a lookup dictionary of JSON nodes
        populateNodesDictionary(json);

        // Parse JSON into graph data structures and objects
        console.info('Generating nodes and links from json...');
        for (var key in nodeDictionary) {
            var node = nodeDictionary[key];

            if (isTopology(node.jsonObj)) { // store topology nodes
                processTopology(node);
            } else if (shouldBeDisplayed(node.jsonObj)) { // store all other nodes that are displayed
                nodesList.push(node);
            } else { // ignored nodes
                console.log('Ignored element ' + node.name);
            }

            // Add any alias links that exist
            if (hasProperty(node.jsonObj, properties.isAlias)) {
                var aliasList = node.jsonObj[properties.isAlias];
                processLinks(node, aliasList, true); // add visible link from node to each element of aliasList
            }
        }

        if (settings.loadingItem) {
            versastack.loading.end(settings.loadingItem); // finished loading
        }

        console.info('Finished processing', 'nodes', nodesList, 'links', linksList);
    }

    function populateNodesDictionary(json) {
        console.info('Creating lookup dictionary...');
        var i = 0; // used to assign unique node ids
        for (var key in json) {
            var obj = json[key];
            var name = removePrefix(key, prefix.base);
            var iconPath = getIconPath(obj);

            var node = {
                name: name,
                jsonObj: obj,
                icon: iconPath,
                id: i++
            };

            nodeDictionary.push(node);
        }
    }

    // Store topology node and relations
    function processTopology(node) {
        // foo (hasNode | hasService) bar => bar is a child of foo,
        // so merge both properties to get a full list of children nodes
        var children = mergeObjects(node.jsonObj[properties.hasNode], node.jsonObj[properties.hasService]);
        var childrenNodes = [];

        // Add relationship between topology and each child
        for (var key in children) {
            if (!owns.call(children, key)) {
                continue;
            }

            // Lookup direct reference to the child node in node dictionary
            var child = findByName(nodeDictionary, removePrefix(children[key].value, prefix.base));
            if (child != null) {
                child.parent = node; // add parent relationship to child node
                childrenNodes.push(child); // add child relationship to parent node
            } else {
                console.error('Trying to add nonexistent child', children[key].value, 'to topology', node.name);
            }
        }

        node.children = childrenNodes;

        console.info('Adding topology', node);
        topologyList.push(node);
        nodesList.push(node);

        processLinks(node, children, false); // add invisible link from topology to each children
    }

    // Adds a link from source to each element in targetList with chosen visibility
    function processLinks(source, targetList, isVisible) {
        for (var key in targetList) {
            if (owns.call(targetList, key)) {
                var target = findByName(nodeDictionary, removePrefix(targetList[key].value, prefix.base));
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
    }

    /**
     * Display elements from NML or MRS namesapce only.
     * Except, do not display labeltypes or addresses.
     */
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
    function indexOfName(array, search) {
        for (var i = 0, l = array.length; i < l; ++i) {
            if (array[i].name === search) {
                return i;
            }
        }

        return -1;
    }

    function findByName(array, name) {
        var index = indexOfName(array, name);
        if (index >= 0) {
            return array[index];
        } else {
            return null;
        }
    }

    function mergeObjects(o1, o2) {
        var o3 = o1 || {};

        for (var element in o2) {
            if (owns.call(o2, element)) {
                o3[element] = o2[element];
            }
        }

        return o3;
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
        // obj hasType foo => obj hasProperty type
        // so check that obj has a type property first
        if (hasProperty(obj, properties.type)) {
            var objTypeProperty = obj[properties.type];

            // obj can have multiple types so check each against the target type
            for (var objType in objTypeProperty) {
                var typeValue = objTypeProperty[objType].value;
                if (owns.call(objTypeProperty, objType) && typeValue === type) {
                    return true;
                }
            }
        }

        return false;
    }

    function isTopology(obj) {
        return hasType(obj, types.topology.name);
    }

    function removePrefix(string, prefix) {
        var regexp = new RegExp(escapeRegExp(prefix), 'ig');
        return string.replace(regexp, '');
    }

    /**
     * escapes all special characters in a regular expression
     * by bobince from
     * stackoverflow.com/questions/3561493/is-there-a-regexp-escape-function-in-javascript
     */
    function escapeRegExp(s) {
        return s.replace(/[-\/\\^$*+?.()|[\]{}]/g, '\\$&');
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
                    getIconPath.TypeEnum[types[key].name] = settings.baseIconPath + types[key].iconName;
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
        dictionary: nodeDictionary,
        links: linksList,
        nodes: nodesList,
        topologies: topologyList,
        data: {
            nodes: nodesList,
            links: linksList
        }
    };
    /** END PUBLIC INTERFACE **/

}(); // end versastack.model module