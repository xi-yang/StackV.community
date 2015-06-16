define([
    "local/d3", "local/versastack/model", "local/versastack/utils", "local/versastack/loading", "local/d3.tip.v0.6.3"
], function(d3, model, utils, loading) {
    /** Document and canvas sizes **/
    var width = document.documentElement.clientWidth;
    var height = document.documentElement.clientHeight;
    var canvasWidth = width * 1.5;
    var canvasHeight = height * 1.5;
    var node = [];
    var link = [];
    var expand = {};
    var layout = {};
    var layoutExpand = {};
    var svg, hullg, nodeg, linkg;
    var force, drag, data, map, hull, curve, lastClicked, tip;
    var fill = d3.scale.category20();
    var css = {
        classes: {
            nodeText: '.node text',
            nodeImage: '.node image',
            nodeCircle: '.node circle',
            link: 'line.link'
        },
        IDs: {
//            zoomSlider: '#zoomSlider',
            zoomValue: '#zoomValue',
            infobox: '#networks'
        }
    };
    var settings = {
        /*
         * Zoom takes on values in the range [0, 100].
         * The base zoom value defines 1x zoom.
         * Any other zoom value causes a scaling equal to a
         * percentage of the base zoom value. 
         */
        baseZoomValue: 50,
        /** Font size in px **/
        baseFontSize: 12,
        /** Icon image size **/
        iconWidth: 30,
        iconHeight: 30,
        /** Node text offsets **/
        nodeTextOffsetX: 5,
        nodeTextOffsetY: 1,
        /** Topology panel attributes **/
        topologyPanelWidth: 370,
        topologyPanelHeight: 50,
        topologyPanelBorderRounding: 5,
        /*
         * Force graph fundamental values 
         * charge - node-node force strength
         * gravity - node-center force strength
         * distance - minimum distance between linked nodes
         */
        baseCharge: -5000,
        baseGravity: .2,
        baseDistance: 100,
        baseFriction: .2,
        /** Circle size **/
        baseRadius: 10,
        /** Hull size **/
        hullOffset: 15
    };

    /** FUNCTIONALITY **/
    function initForceGraph() {
        console.info('Initialising force graph...');

        /**
         * k is porportional to the square root of the graph density
         * and is used as a heuristic to set appropriate values for the
         * charge and gravity of the force graph
         * credit to mbostock on Stack Overflow for the idea
         */
        var k = Math.sqrt(data.nodes.length / (canvasWidth * canvasHeight));
        console.info('Calculated k-value ' + k);
        //        var charge = settings.baseCharge / k;
        //        var gravity = settings.baseGravity * k;
        //        var distance = settings.baseDistance * (1 + (100 * k));
        var friction = settings.baseFriction;
        var charge = settings.baseCharge;
        var gravity = settings.baseGravity;
        var distance = settings.baseDistance;
        force = d3.layout.force().size([canvasWidth, canvasHeight]).charge(charge).gravity(gravity).linkDistance(distance).friction(friction).on('tick', tick)
                .on('start', function() {
                    setTimeout(function() {
                        force.alpha(force.alpha() / 3);
                    }, 250);
                    setTimeout(function() {
                        force.alpha(force.alpha() / 3);
                    }, 500);
                    setTimeout(function() {
                        force.stop();
                    }, 1000);
                });
        svg = d3.select('#topologyLarge').attr('width', canvasWidth).attr('height', canvasHeight);
        hullg = svg.append('g');
        linkg = svg.append('g');
        nodeg = svg.append('g');
        tip = d3.tip().attr('class', 'd3-tip').offset([-10, 0]).html(function(d) {
            return "<strong>" + d.name + "</strong>";
        });
        svg.call(tip);
        curve = d3.svg.line().interpolate('cardinal-closed').tension(.85);
        svg.attr('opacity', 1e-6) // fade in the graph
                .transition().duration(1000).attr('opacity', 1.5);
        drag = force.drag().on('drag', ondrag);
        console.info('Force graph initialised');
        loading.end(loadingItem, true);
        restart();
    }

    function restart() {
        if (force) {
            force.stop();
        }

        map = network(data, map, expand);
        
        force.nodes(map.nodes).links(map.links);
        hullg.selectAll('path.hull').remove();
        hull = hullg.selectAll('path.hull').data(convexHulls(map.nodes, settings.hullOffset, expand)).enter().append('path').attr('class', 'hull').attr('d', drawCluster).style('fill', function(d) {
            return fill(d.id);
        }).on('dblclick', function(d) {
            console.log('hull click', d, arguments, this, expand[d.id]);
            cycleState(d.id);
            restart();
        }).on('mousemove', tip.show).on('mouseout', tip.hide);
        link = linkg.selectAll(css.classes.link).data(map.links, getLinkID);
        link.exit().remove();
        link.enter().append('line').classed('link', true).style('stroke-width', function(d) {
            return d.size || 1;
        });
        node = nodeg.selectAll('g').data(map.nodes, getUID);
        node.exit().remove();
        var nodeEnter = node.enter().append('g')
                // if (d.size) -- d.size > 0 when d is a group node.
                .attr('class', function(d) {
                    return 'node' + (d.size > 0 ? '' : ' leaf');
                }).on('dblclick', dblclick).on('click', click).call(drag).on('mousemove', tip.show).on('mouseout', tip.hide);

        nodeEnter.append("svg:image")
                .attr("class", "circle")
                .attr("xlink:href", function(d) {
                    return d.icon;
                })
                .attr("x", "-15px")
                .attr("y", "-15px")
                .attr("width", "30px")
                .attr("height", "30px");
        force.start();
        for (var i = 0; i < 3; i++)
            force.tick();
    }

    function processPrevious(node, groups, centroids) {
        if (node.hasNode) {
            groups[node.id] = node; // then add entry to list of previous groups 
            node.size = 0;
        }

        if (isChild(node)) { // else if processing group member node
// then add its information to group's centroid
            var c = centroids[node.parent] || (centroids[node.parent] = {
                x: 0,
                y: 0,
                count: 0
            });
            c.x += node.x;
            c.y += node.y;
            c.count += 1;
        }
    }

    function network(data, previous, expand) {
        console.info('Input data', data);
        var groupMap = {},
                nodeMap = {},
                linkMap = {},
                previousGroups = {},
                previousCentroids = {},
                outputNodes = [],
                outputLinks = [];
        if (previous) { // process nodes from previous iteration
            previous.nodes.forEach(function(node) {
                processPrevious(node, previousGroups, previousCentroids);
            });
            console.info('Processed previous', 'previous group', previousGroups, 'previous nodes', previousCentroids);
        }

// determine nodes
        for (var k = 0, l = data.nodes.length; k < l; ++k) {
            var n = data.nodes[k],
                    i = n.id;
            if (n.hasNode || (n.children != null && n.children.length > 0) || n.hasService || n.hasPort) {
                if (isDisplayed(n)) {
                    nodeMap[n.id] = n.id;
                    outputNodes.push(n);
                    if (expand[n.id]) { // display all the children nodes
                        for (var child in n.children) {
                            nodeMap[n.children[child]] = n.children[child];
                            outputNodes.push(data.dictionary[n.children[child]]);
                            if (previousGroups[i]) {
                                n.children[child].x = previousGroups[i].x;
                                n.children[child].y = previousGroups[i].y;
                            }
                        }
                        for (var child in n.services) {
                            nodeMap[n.services[child]] = n.services[child];
                            outputNodes.push(data.dictionary[n.services[child]]);
                            if (previousGroups[i]) {
                                n.services[child].x = previousGroups[i].x;
                                n.services[child].y = previousGroups[i].y;
                            }
                        }
                        for (var child in n.ports) {
                            nodeMap[n.ports[child]] = n.ports[child];
                            outputNodes.push(data.dictionary[n.ports[child]]);
                            if (previousGroups[i]) {
                                n.ports[child].x = previousGroups[i].x;
                                n.ports[child].y = previousGroups[i].y;
                            }
                        }
                    } else { // only display the node
                        if (previousCentroids[i]) {
                            n.x = previousCentroids[i].x / previousCentroids[i].count;
                            n.y = previousCentroids[i].y / previousCentroids[i].count;
                        }

                        for (var child in n.children) {
                            nodeMap[n.children[child]] = n.id;
                        }
                        for (var child in n.services) {
                            nodeMap[n.services[child]] = n.id;
                        }
                        for (var child in n.ports) {
                            nodeMap[n.ports[child]] = n.id;
                        }
                    }
                } else { // only display the parent
                    var parent = getSuper(n);
                    nodeMap[n.id] = parent.id;
                    for (var child in n.children) {
                        nodeMap[n.children[child]] = parent.id;
                    }
                    for (var child in n.services) {
                        nodeMap[n.services[child]] = parent.id;
                    }
                    for (var child in n.ports) {
                        nodeMap[n.ports[child]] = parent.id;
                    }
                }
            } else if (!isChild(n)) { // stand alone nodes are always displayed
                outputNodes.push(n);
                nodeMap[n.id] = n.id;
            }
        }

        for (i in groupMap) {
            groupMap[i].link_count = 0;
        }

// determine links
        for (k = 0, l = data.links.length; k < l; ++k) {
            var e = data.links[k];
            // while d3.layout.force does convert link.source and link.target NUMERIC values to direct node references,
            // it doesn't for other attributes, such as .real_source, so we do not use indexes in nm[] but direct node
            // references to skip the d3.layout.force implicit links conversion later on and ensure that both .source/.target
            // and .real_source/.real_target are of the same type and pointing at valid nodes.
            var u = nodeMap[e.source.id],
                    v = nodeMap[e.target.id];
            if (u == v) {
// skip links from node to same (A-A); they are rendered as 0-length lines anyhow. Less links in array = faster animation.
                continue;
            }

            var link = linkMap[getLinkID(e)] || (linkMap[getLinkID(e)] = {
                source: data.dictionary[u],
                target: data.dictionary[v],
                size: 0,
                visible: e.visible
            });
            link.size += 1;
        }

        for (i in linkMap) {
            outputLinks.push(linkMap[i]);
        }

        return {
            nodes: outputNodes,
            links: outputLinks
        };
    }

    function convexHulls(nodes, offset, expand) {
        var hulls = {};
        var hullsX = {};
        var hullsY = {};
        // create point sets
        for (var k = 0, l = nodes.length; k < l; ++k) {
            var n = nodes[k],
                    i = n.parentID || n.id;
            if (expand[i] === 0 || (!isChild(n) && !(n.hasNode || n.hasService))) {
                continue;
            }

            var hx = hullsX[i] || (hullsX[i] = []);
            var hy = hullsY[i] || (hullsY[i] = []);
            hx.push(n.x - offset);
            hx.push(n.x + offset);
            hy.push(n.y - offset);
            hy.push(n.y + offset);
        }

        for (var k = 0, l = nodes.length; k < l; ++k) {
            var n = nodes[k],
                    i = n.parentID || n.id;
            if (expand[i] === 0 || (!isChild(n) && !(n.hasNode || n.hasService))) {
                continue;
            }

            var h = hulls[i] || (hulls[i] = []);
            var minX = arrayMin(hullsX[i]);
            var maxX = arrayMax(hullsX[i]);
            var minY = arrayMin(hullsY[i]);
            var maxY = arrayMax(hullsY[i]);
            h.push([minX, minY]);
            h.push([minX, maxY]);
            h.push([maxX, minY]);
            h.push([maxX, maxY]);
        }

// create convex hulls
        var hullset = [];
        for (i in hulls) {
            hullset.push({
                id: i,
                path: d3.geom.hull(hulls[i]),
                name: data.dictionary[i].name
            });
        }

        return hullset;
    }

    function arrayMin(list) {
        var min = list[0];
        for (var i = 0, l = list.length; i < l; ++i) {
            if (min > list[i]) {
                min = list[i];
            }
        }

        return min;
    }

    function arrayMax(list) {
        var max = list[0];
        for (var i = 0, l = list.length; i < l; ++i) {
            if (max < list[i]) {
                max = list[i];
            }
        }

        return max;
    }

    function cycleState(i) {
        var s = expand[i];
        expand[i] = ++s % 2;
    }

    function toggleLock() {
        if (toggleLock.fixed === undefined) {
            toggleLock.fixed = true;
        } else {
            toggleLock.fixed = !toggleLock.fixed;
        }

        for (var i = 0, l = map.nodes.length; i < l; ++i) {
            map.nodes[i].fixed = toggleLock.fixed;
        }

        if (toggleLock.fixed === true) {
            d3.select('#lockButton').html('Unlock');
        } else {
            d3.select('#lockButton').html('Lock');
        }
    }

    function loadLayout() {
        expand = JSON.parse(layoutExpand);
        data.nodes = [];
        data.links = [];
        for (var i = 0, l = layout.nodes.length; i < l; ++i) {
            var n = layout.nodes[i];
            data.nodes[i] = {};
            data.nodes[i].x = n.x;
            data.nodes[i].y = n.y;
            data.nodes[i].fixed = true;
            data.nodes[i].icon = n.icon;
            data.nodes[i].id = n.id;
            data.nodes[i].index = n.index;
            data.nodes[i].jsonObj = n.jsonObj;
            data.nodes[i].name = n.name;
            data.nodes[i].weight = n.weight;
            data.nodes[i].children = n.children;
            data.nodes[i].parentID = n.parentID;
            data.nodes[i].services = n.services;
        }

        for (var i = 0, l = layout.links.length; i < l; ++i) {
            var k = layout.links[i];
            data.links[i] = {};
            data.links[i].originalSource = k.originalSource;
            data.links[i].originalTarget = k.originalTarget;
            data.links[i].source = k.source;
            data.links[i].target = k.target;
            data.links[i].visible = k.visible;
        }

        console.log('loaded layout', data);
        restart();
    }

    function saveLayout() {
        layout.nodes = [];
        layout.links = [];
        for (var i = 0, l = data.nodes.length; i < l; ++i) {
            var n = data.nodes[i];
            layout.nodes[i] = {};
            layout.nodes[i].x = n.x;
            layout.nodes[i].y = n.y;
            layout.nodes[i].fixed = true;
            layout.nodes[i].icon = n.icon;
            layout.nodes[i].id = n.id;
            layout.nodes[i].index = n.index;
            layout.nodes[i].jsonObj = n.jsonObj;
            layout.nodes[i].name = n.name;
            layout.nodes[i].weight = n.weight;
            layout.nodes[i].children = n.children;
            layout.nodes[i].parentID = n.parentID;
            layout.nodes[i].services = n.services;
        }

        for (var i = 0, l = data.links.length; i < l; ++i) {
            var k = data.links[i];
            layout.links[i] = {};
            layout.links[i].originalSource = k.originalSource;
            layout.links[i].originalTarget = k.originalTarget;
            layout.links[i].source = k.source;
            layout.links[i].target = k.target;
            layout.links[i].visible = k.visible;
        }

        console.log('Saved layout', layout);
        layoutExpand = JSON.stringify(expand);
    }
    /** END FUNCTIONALITY **/


    /** UTILITY FUNCTIONS **/
    function getNodeWithID(id) {
        for (var node in data.nodes) {
            if (data.nodes[node].id === id) {
                return data.nodes[node];
            }
        }

        return null;
    }

    function isChild(node) {
        return node.parentID != null;
    }

    function drawCluster(d) {
        return curve(d.path); // 0.8
    }

    function isDisplayed(n) {
        var parent = n.parentID;
        while (parent != null) {
            if (!expand[parent]) {
                return false;
            }

            parent = data.dictionary[parent].parentID;
        }

        return true;
    }

    function getSuper(n) {
        var parent = n;
        while (parent.parentID != null) {
            if (isDisplayed(parent)) {
                return parent;
            } else {
                parent = data.dictionary[parent.parentID];
            }
        }

        return parent;
    }

    function getLinkID(d) {
        var u = d.source.name,
                v = d.target.name;
        if (u < v) {
            return u + '|' + v;
        } else {
            return v + '|' + u;
        }
    }

    function getUID(n) {
        return (n.isTopology ? 'Topology ' : 'Node ') + n.name;
    }
    /** END UTILITY FUNCTIONS **/


    /** EVENT HANDLER FUNCTIONS **/
    function click(d) {
        if (d3.event.defaultPrevented) {
            return; // click suppressed by drag
        }
        updateInfobox(d);
    }

    function addNode() {
        var node = new model.Node('test addNode ' + Math.floor(Math.random() * 10000), data.dictionary.length, data.dictionary[53].json, data.dictionary[53].icon);
        node.parentID = lastClicked.id;
        console.log('Adding node', node, 'to', lastClicked);
        var childrenList = lastClicked.children || [];
        childrenList.push(node.id);
        data.dictionary.push(node);
        data.nodes.push(node);
        data.links.push({
            source: lastClicked,
            target: node,
            visible: true
        });
        updateInfobox.open = false;
        updateInfobox(lastClicked);
        restart();
    }

// TODO document updateInfobox.open behavior
    function updateInfobox(d) {
//        if (updateInfobox.open === undefined) {
//            updateInfobox.open = false;
//        }
//        if (lastClicked === d && updateInfobox.open) {
//            d3.select(css.IDs.infobox).style({
//                'margin-left': '-180px'
//            }).html('');
//            updateInfobox.open = false;
//        } else {

        var html = '<button id="networksButton" data-dojo-type="dijit/form/Button" type="button">Show Test Networks</button>';
        html += '<br /><strong>Clicked!<br />Name: <span style="color:#718087">' + d.name + '</span><br />ID: <span style="color:#718087">~' + d.id + '</span></strong>';
        html += '<br /><button id="addNodeButton" data-dojo-type="dijit/form/Button" type="button">Add Test Node</button>';
        if (d.hasParent()) {
            html += '<br /><strong>Parent: <span style="color:#718087"';
            var parent = data.dictionary[d.parentID];
            html += ('<br />~' + parent.id + ': ' + parent.name);
            html += '</span></strong>';
        }

        if (d.children != null && d.children.length > 0) {
            html += '<br /><strong>Child Nodes: <span style="color:#718087"';
            for (var c in d.children) {
                var child = data.dictionary[d.children[c]];
                html += ('<br />~' + child.id);
            }
            html += '</span></strong>';
        }

        if (d.hasService) {
            html += '<br /><strong>Services: <span style="color:#718087"';
            for (var s in d.services) {
                var service = data.dictionary[d.services[s]];
                html += ('<br />~' + service.id);
            }
            html += '</span></strong>';
        }

        if (d.hasPort) {
            html += '<br /><strong>Ports: <span style="color:#718087"';
            for (var p in d.ports) {
                var port = data.dictionary[d.ports[p]];
                html += ('<br />~' + port.id);
            }
            html += '</span></strong>';
        }

        d3.select(css.IDs.infobox).html(html);
        updateInfobox.open = true;
//        }

        lastClicked = d;
        require(["dojo/on", "dijit/registry", "dojo/ready"], function(on, registry, ready) {
            ready(function() {
                registry.byId('networks').set('content', html);
                var networksButton = registry.byId("networksButton");
                on(networksButton, 'click', callSwitchPanelsWith("Networks"));
                var addNodeButton = registry.byId("addNodeButton");
                on(addNodeButton, 'click', addNode);
            });
        });
    }

    function dblclick(d) {
        cycleState(d.id);
        restart();
    }

    function ondrag(d) {
//        d3.select(this).classed('fixed', d.fixed = true);
    }

    function radius(d) {
        var totalChildren = 0;
        for (var c in d.children) {
            totalChildren += radius(data.dictionary[d.children[c]]) - settings.baseRadius + .5;
        }

        for (var c in d.ports) {
            totalChildren += radius(data.dictionary[d.ports[c]]) - settings.baseRadius + .25;
        }

        for (var c in d.services) {
            totalChildren += radius(data.dictionary[d.services[c]]) - settings.baseRadius + .5;
        }

        return settings.baseRadius + totalChildren;
    }
    /** END EVENT HANDLER FUNCTIONS **/


    /** GRAPH UPDATE **/
    function tick() {
        if (!hull.empty()) {
            hull.data(convexHulls(map.nodes, settings.hullOffset, expand)).attr('d', drawCluster);
        }

        link = linkg.selectAll(css.classes.link);
        link.attr('x1', function(d) {
            return d.source.x;
        }).attr('y1', function(d) {
            return d.source.y;
        }).attr('x2', function(d) {
            return d.target.x;
        }).attr('y2', function(d) {
            return d.target.y;
        });
        link.attr('opacity', function(d) {
            if (d.visible) {
                return 100;
            } else {
                return 0;
            }
        });
        node = nodeg.selectAll('g');
        node.attr('transform', function(d) {
//            var r = radius(d),
//                    x = Math.max(r, Math.min(canvasWidth - r, d.x)),
//                    y = Math.max(r, Math.min(canvasHeight - r, d.y));

            return 'translate(' + d.x + ',' + d.y + ')';
        });
    }

    function processData(form) {
        console.log(form);
    }
    /** END GRAPH UPDATE **/


    /** PUBLIC INTERFACE **/
    return {
        initForceGraph: initForceGraph,
        css: css,
        settings: settings,
        toggleLock: toggleLock,
        load: loadLayout,
        save: saveLayout,
        addNode: addNode,
        processData: processData
    };
    /** END PUBLIC INTERFACE **/

});