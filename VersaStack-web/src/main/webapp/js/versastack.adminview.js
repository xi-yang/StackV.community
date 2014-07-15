// Get existing versastack namespace, or define new one
var versastack = versastack || {};

versastack.adminview = function () {
    /** Document and canvas sizes **/
    var width = document.documentElement.clientWidth;
    var height = document.documentElement.clientHeight;
    var canvasWidth = width * 2;
    var canvasHeight = height * 2;

    var node = [];
    var link = [];
    var expand = {};

    var svg, hullg, nodeg, linkg;
    var force, drag, data, map, hull, link, node, curve, tip;
    var fill = d3.scale.category20();

    var owns = Object.prototype.hasOwnProperty;

    var css = {
        classes: {
            nodeText: '.node text',
            nodeImage: '.node image',
            nodeCircle: '.node circle',
            link: 'line.link'
        },
        IDs: {
            zoomSlider: '#zoomSlider',
            zoomValue: '#zoomValue'
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

    // Module execution starts here
    function main(loadingItem) {
        // Wait for json model to finish loading before displaying graph
        if (versastack.loading.finished(loadingItem)) {
            createZoomSlider();
            initForceGraph();
        } else {
            setTimeout(function () {
                main(loadingItem);
            }, 100);
        }
    }

    /** FUNCTIONALITY **/
    function initForceGraph() {
        console.info('Initialising force graph...');

        /**
         * k is porportional to the square root of the graph density
         * and is used as a heuristic to set appropriate values for the
         * charge and gravity of the force graph
         * credit to mbostock on Stack Overflow for the idea
         */
        var k = Math.sqrt(versastack.model.nodes.length / (canvasWidth * canvasHeight));
        console.info('Calculated k-value ' + k);
        //        var charge = settings.baseCharge / k;
        //        var gravity = settings.baseGravity * k;
        //        var distance = settings.baseDistance * (1 + (100 * k));
        var friction = settings.baseFriction;
        var charge = settings.baseCharge;
        var gravity = settings.baseGravity;
        var distance = settings.baseDistance;
        //        var distance = function (l, i) {
        //                var n1 = l.originalSource;
        //                n2 = l.originalSource;
        //                return 30 + Math.min(20 * Math.min((n1.size || (n1.group !== n2.group ? n1.group_data.size : 0)), (n2.size || (n1.group !== n2.group ? n2.group_data.size : 0))), -30 + 30 * Math.min((n1.link_count || (n1.group !== n2.group ? n1.group_data.link_count : 0)), (n2.link_count || (n1.group !== n2.group ? n2.group_data.link_count : 0))), 100);
        //            };
        force = d3.layout.force().size([canvasWidth, canvasHeight]).charge(charge).gravity(gravity).linkDistance(distance).friction(friction).on('tick', tick);
        svg = d3.select('body').append('svg').attr('width', canvasWidth).attr('height', canvasHeight);

        hullg = svg.append('g');
        linkg = svg.append('g');
        nodeg = svg.append('g');

        tip = d3.tip().attr('class', 'd3-tip').offset([-10, 0]).html(function (d) {
            return "<strong>" + d.name + "</strong>";
        });

        svg.call(tip);

        curve = d3.svg.line().interpolate('cardinal-closed').tension(.85);

        data = versastack.model.data;

        for (var i = 0, l = data.links.length; i < l; ++i) {
            var link = data.links[i];
            link.originalSource = link.source;
            link.originalTarget = link.target;
        }

        svg.attr('opacity', 1e-6) // fade in the graph
        .transition().duration(1000).attr('opacity', 1);

        drag = force.drag().on('drag', ondrag);

        console.info('Force graph initialised');
        restart();
    }

    function restart() {
        if (force) {
            force.stop();
        }

        map = network(data, map, getGroupID, expand);
        console.info('output nodes', map.nodes);
        force.nodes(map.nodes).links(map.links);
        hullg.selectAll('path.hull').remove();
        hull = hullg.selectAll('path.hull').data(convexHulls(map.nodes, getGroupID, settings.hullOffset, expand)).enter().append('path').attr('class', 'hull').attr('d', drawCluster).style('fill', function (d) {
            return fill(d.group);
        }).on('click', function (d) {
            console.log('hull click', d, arguments, this, expand[d.group]);
            cycleState(getGroupID(d));
            restart();
        }).on('mousemove', tip.show).on('mouseout', tip.hide);

        link = linkg.selectAll(css.classes.link).data(map.links, getLinkID);
        link.exit().remove();

        link.enter().append('line').classed('link', true).style('stroke-width', function (d) {
            return d.size || 1;
        });

        node = nodeg.selectAll('g').data(map.nodes, getUID);
        node.exit().remove();

        var nodeEnter = node.enter().append('g')
        // if (d.size) -- d.size > 0 when d is a group node.
        .attr('class', function (d) {
            return 'node' + (d.size > 0 ? '' : ' leaf');
        }).on('dblclick', dblclick).on('click', click).call(drag).on('mousemove', tip.show).on('mouseout', tip.hide);

        nodeEnter.append('circle').style('fill', function (d) {
            return fill(getGroupID(d));
        }).attr('r', function (d) {
            return radius(d);
        });

        //        node.append('rect')
        //                .style({'stroke': 'black', 'stroke-width': '4', 'opacity': '0.5', 'fill': randomRGB})
        //                .attr('width', settings.topologyPanelWidth)
        //                .attr('height', settings.topologyPanelHeight)
        //                .attr('x', 0)
        //                .attr('y', -settings.topologyPanelHeight / 2 + settings.iconHeight / 2)
        //                .attr('rx', settings.topologyPanelBorderRounding)
        //                .attr('ry', settings.topologyPanelBorderRounding);
        //
        //        node.append('image')
        //                .attr('xlink:href', function(d) {
        //                    return d.icon;
        //                })
        //                .attr('x', 0)
        //                .attr('y', 0)
        //                .attr('width', settings.iconWidth)
        //                .attr('height', settings.iconHeight);
        //        nodeEnter.append('text').attr('x', function (d) {
        //            return radius(d) / 2 + settings.nodeTextOffsetX;
        //        }).attr('y', function (d) {
        //            return radius(d) / 2 + settings.nodeTextOffsetY;
        //        }).text(function (d) {
        //            return d.name || getUID(d);
        //        });
        force.start();
    }

    function processPrevious(node, index, groups, centroids) {
        if (isTopology(node)) { // if processing a topology node
            groups[index] = node; // then add entry to list of previous groups 
            node.size = 0;
        } else if (isChild(node)) { // else if processing group member node
            // then add its information to group's centroid
            var c = centroids[index] || (centroids[index] = {
                x: 0,
                y: 0,
                count: 0
            });
            c.x += node.x;
            c.y += node.y;
            c.count += 1;
        }
    }

    function network(data, previous, index, expand) {
        console.info('Input data', data);
        expand = expand || {};

        var groupMap = {},
            nodeMap = {},
            linkMap = {},
            previousGroups = {},
            previousCentroids = {},
            outputNodes = [],
            outputLinks = [];

        if (previous) { // process nodes from previous iteration
            previous.nodes.forEach(function (node) {
                processPrevious(node, index(node), previousGroups, previousCentroids);
            });

            console.info('Processed previous', 'previous group', previousGroups, 'previous nodes', previousCentroids);
        }

        // determine nodes
        for (var k = 0, l = data.nodes.length; k < l; ++k) {
            var n = data.nodes[k],
                i = index(n);

            if (isTopology(n)) {
                nodeMap[getUID(n)] = n;
                if (expand[i]) { // display all the children nodes
                    for (var child in n.children) {
                        nodeMap[getUID(n.children[child])] = n.children[child];
                        outputNodes.push(n.children[child]);
                        if (previousGroups[i]) {
                            n.children[child].x = previousGroups[i].x; + Math.random();
                            n.children[child].y = previousGroups[i].y + Math.random();
                        }
                    }
                } else { // only display the topology node
                    outputNodes.push(n);
                    if (previousCentroids[i]) {
                        n.x = previousCentroids[i].x / previousCentroids[i].count;
                        n.y = previousCentroids[i].y / previousCentroids[i].count;
                    }
                    for (var child in n.children) {
                        nodeMap[getUID(n.children[child])] = n;
                    }
                }
            } else if (!isChild(n)) { // stand alone nodes are always displayed
                outputNodes.push(n);
                nodeMap[getUID(n)] = n;
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
            var u = nodeMap[getUID(e.source)],
                v = nodeMap[getUID(e.target)];

            if (u == v) {
                // skip links from node to same (A-A); they are rendered as 0-length lines anyhow. Less links in array = faster animation.
                continue;
            }

            var link = linkMap[getLinkID(e)] || (linkMap[getLinkID(e)] = {
                source: u,
                target: v,
                originalSource: u,
                originalTarget: v,
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

    /** Slider control for graph zoom level **/
    function createZoomSlider() {
        d3.select(css.IDs.zoomSlider).call(
        d3.slider().value(settings.baseZoomValue).orientation("vertical").on('slide', function (evt, value) {
            d3.select(css.IDs.zoomValue).text(Math.round(value));
            resize(value);
        }));
    }

    function resize(size) {
        var scaling = size / settings.baseZoomValue;
        svg.selectAll(css.classes.nodeText).style('font-size', settings.baseFontSize * scaling + 'px');
        svg.selectAll(css.classes.nodeImage).attr('transform', 'scale(' + scaling + ')');
        svg.selectAll(css.classes.link).style('stroke-width', scaling);
        svg.selectAll(css.classes.nodeCircle).attr('transform', 'scale(' + scaling + ')');
    }

    function convexHulls(nodes, index, offset, expand) {
        var hulls = {};
        expand = expand || {};

        // create point sets
        for (var k = 0, l = nodes.length; k < l; ++k) {
            var n = nodes[k],
                i = index(n),
                h = hulls[i] || (hulls[i] = []);

            if (expand[i] == null || !isChild(n)) {
                continue;
            }

            h.push([n.x - offset, n.y - offset]);
            h.push([n.x - offset, n.y + offset]);
            h.push([n.x + offset, n.y - offset]);
            h.push([n.x + offset, n.y + offset]);
        }

        // create convex hulls
        var hullset = [];
        for (i in hulls) {
            hullset.push({
                id: i,
                path: d3.geom.hull(hulls[i]),
                name: getNodeWithID(parseInt(i)).name
            });
        }

        return hullset;
    }

    function cycleState(i) {
        var s = expand[i] || 0;
        s = ++s % 2;
        return expand[i] = s;
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

    function isTopology(node) {
        return node.children != null;
    }

    function isChild(node) {
        return node.parent != null;
    }

    function drawCluster(d) {
        return curve(d.path); // 0.8
    }

    function getGroupID(n) {
        return isChild(n) ? n.parent.id : n.id;
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
        return (isTopology(n) ? 'Topology ' : 'Node ') + n.name;
    }
    /** END UTILITY FUNCTIONS **/


    /** EVENT HANDLER FUNCTIONS **/
    function click(d) {
        if (d3.event.defaultPrevented) {
            return; // click suppressed by drag
        }

        cycleState(getGroupID(d));
        restart();

        //        var key, node, index;
        //        var topology = findByName(versastack.model.topologies, d.name);
        //        
        //        if (topology) {
        //            for (key in topology.nodes) {
        //                if (owns.call(topology.nodes, key)) {
        //                    node = topology.nodes[key];
        //
        //                    console.log('Toggle ' + node.name + ' visible:' + !node.visible);
        //                    node.visible = !node.visible;
        //
        //                    index = indexOfName(node.name);
        //
        //                    if (index < 0) {
        //                        versastack.model.nodes.push(node);
        //                    } else {
        //                        versastack.model.nodes.splice(index, 1);
        //                    }
        //                }
        //            }
        //            tick();
        //        }
    }

    function dblclick(d) {
        //        d3.select(this).classed('fixed', d.fixed = false);
    }

    function ondrag(d) {
        //        d3.select(this).classed('fixed', d.fixed = true);
    }

    function radius(d) {
        return settings.baseRadius + (d.children ? d.children.length : 0) * 4;
    };
    /** END EVENT HANDLER FUNCTIONS **/


    /** GRAPH UPDATE **/
    function tick() {
        if (!hull.empty()) {
            hull.data(convexHulls(map.nodes, getGroupID, settings.hullOffset, expand)).attr('d', drawCluster);
        }

        link = linkg.selectAll(css.classes.link);
        link.attr('x1', function (d) {
            return d.source.x;
        }).attr('y1', function (d) {
            return d.source.y;
        }).attr('x2', function (d) {
            return d.target.x;
        }).attr('y2', function (d) {
            return d.target.y;
        });

        link.attr('opacity', function (d) {
            if (d.visible) {
                return 100;
            } else {
                return 0;
            }
        });

        node = nodeg.selectAll('g');
        node.attr('transform', function (d) {
            var r = radius(d),
                x = Math.max(r, Math.min(canvasWidth - r, d.x)),
                y = Math.max(r, Math.min(canvasHeight - r, d.y));

            return 'translate(' + x + ',' + y + ')';
        });
    }
    /** END GRAPH UPDATE **/


    /** PUBLIC INTERFACE **/
    return {
        display: main,
        css: css,
        settings: settings
    };
    /** END PUBLIC INTERFACE **/

}(); // end versastack.adminview module