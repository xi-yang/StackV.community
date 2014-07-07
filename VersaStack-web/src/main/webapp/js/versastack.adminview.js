// Get existing versastack namespace, or define new one
var versastack = versastack || {};

versastack.adminview = function() {
    /** Document and canvas sizes **/
    var width = document.documentElement.clientWidth;
    var height = document.documentElement.clientHeight;
    var canvasWidth = width * 2;
    var canvasHeight = height * 2;
    var svg, drag, force;

    var node = [];
    var link = [];

    var dr = 4,
            off = 15, // cluster hull offset
            expand = {}, // expanded clusters
            data, net, force, hullg, hull, linkg, link, nodeg, node, curve;

    var fill = d3.scale.category20();

    var owns = Object.prototype.hasOwnProperty;

    var css = {
        classes: {
            nodeText: '.node text',
            nodeImage: '.node image',
            link: 'line.link'
        },
        IDs: {
            zoomSlider: '#zoomSlider',
            zoomValue: '#zoomValue'
        }
    };

    var settings = {
        /** 
         * Zoom takes on values in the range [0, 100].
         * The base zoom value defines 1x zoom.
         * Any other zoom value causes a scaling equal to a
         * percentage of the base zoom value. 
         **/
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
        /** Force graph fundamental values **/
        baseCharge: -10, // node-node force strength
        baseGravity: 80, // node-center force strength
        baseDistance: 75 // distance between linked nodes
    };

    // Module execution starts here
    function main(loadingItem) {
        // Wait for json model to finish loading before displaying graph
        if (versastack.loading.finished(loadingItem)) {
            createZoomSlider();
            initForceGraph();
        } else {
            setTimeout(function() {
                main(loadingItem);
            }, 100);
        }
    }

    /** FUNCTIONALITY **/
    function initForceGraph() {
        console.log('Initializing force graph');

        /**
         * k is porportional to the square root of the graph density
         * and is used as a heuristic to set appropriate values for the
         * charge and gravity of the force graph
         * credit to mbostock on Stack Overflow for the idea
         */
        var k = Math.sqrt(versastack.model.nodes.length / (canvasWidth * canvasHeight));
        var charge = settings.baseCharge / k;
        var gravity = settings.baseGravity * k;
//        var distance = settings.baseDistance * (1 + (100 * k));

        console.log('Calculated k-value ' + k);
        console.log('Calculated charge ' + charge);
        console.log('Calculated gravity ' + gravity);
//        console.log('Calculated distance ' + distance);


        force = d3.layout.force()
                .size([canvasWidth, canvasHeight])
                .charge(-600)
                .gravity(.15)
                .linkDistance(function(l, i) {
                    var n1 = l.real_source, n2 = l.real_target;
                    return 30 +
                            Math.min(20 * Math.min((n1.size || (n1.group !== n2.group ? n1.group_data.size : 0)),
                                    (n2.size || (n1.group !== n2.group ? n2.group_data.size : 0))),
                                    -30 +
                                    30 * Math.min((n1.link_count || (n1.group !== n2.group ? n1.group_data.link_count : 0)),
                                            (n2.link_count || (n1.group !== n2.group ? n2.group_data.link_count : 0))),
                                    100);
                })
                .friction(0.6)
                .on('tick', tick);

        svg = d3.select('body').append('svg')
                .attr('width', canvasWidth)
                .attr('height', canvasHeight);

        curve = d3.svg.line()
                .interpolate('cardinal-closed')
                .tension(.85);

        hullg = svg.append('g');
        linkg = svg.append('g');
        nodeg = svg.append('g');

        data = versastack.model.data();
        data.helpers = {};

        for (var i = 0; i < data.links.length; ++i) {
            var l = data.links[i];
            l.real_source = l.source;
            l.real_target = l.target;
        }


        svg.attr('opacity', 1e-6)
                .transition()
                .duration(1000)
                .attr('opacity', 1);

        drag = force.drag()
                .on('drag', ondrag);

        console.log('Success');
        restart();
    }

    function restart() {
        if (force) {
            force.stop();
        }

        net = network(data, net, getGroup, expand);
        force.nodes(net.nodes).links(net.links);

        hullg.selectAll('path.hull').remove();
        hull = hullg.selectAll('path.hull')
                .data(convexHulls(net.nodes, getGroup, off))
                .enter().append('path')
                .attr('class', 'hull')
                .attr('d', drawCluster)
                .style('fill', function(d) {
                    return fill(d.group);
                })
                .on('click', function(d) {
                    console.log('hull click', d, arguments, this, expand[d.group]);
                    cycleState(d.group);
                    restart();
                });

        link = linkg.selectAll(css.classes.link)
                .data(net.links, linkid);
        link.exit().remove();

        link.enter().append('line')
                .classed('link', true)
                .style('stroke-width', function(d) {
                    return d.size || 1;
                });

        node = nodeg.selectAll('g')
                .data(net.nodes, nodeid);
        node.exit().remove();

        var nodeEnter = node.enter()
                .append('g')
                // if (d.size) -- d.size > 0 when d is a group node.
                // d.size < 0 when d is a 'path helper node'.
                .attr('class', function(d) {
                    return 'node' + (d.size > 0 ? '' : d.size < 0 ? ' helper' : ' leaf');
                })
                .on('dblclick', dblclick)
                .on('click', click)
                .call(drag);

        nodeEnter.append('circle')
                .style('fill', function(d) {
                    return fill(d.group);
                })
                .attr('r', function(d) {
                    return d.size > 0 ? d.size + dr : d.size < 0 ? dr - 2 : dr + 1;
                })
                .style('fill', function(d) {
                    return fill(d.group);
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
//
        nodeEnter.append('text')
                .attr('x', function(d) {
                    return (d.size > 0 ? d.size + dr : d.size < 0 ? dr - 2 : dr + 1) / 2 + settings.nodeTextOffsetX;
                })
                .attr('y', function(d) {
                    return (d.size > 0 ? d.size + dr : d.size < 0 ? dr - 2 : dr + 1) / 2 + settings.nodeTextOffsetY;
                })
                .text(function(d) {
                    return d.name || nodeid(d);
                });

        force.start();
    }

    function network(data, prev, index, expand) {
        console.log('Input data', data);
        expand = expand || {};
        var gm = {}, // group map
                nm = {}, // node map
                lm = {}, // link map
                gn = {}, // previous group nodes
                gc = {}, // previous group centroids
                nodes = [], // output nodes
                links = []; // output links

        // process previous nodes for reuse or centroid calculation
        if (prev) {
            prev.nodes.forEach(function(n) {
                var i = index(n), o;
                if (n.size > 0) {
                    gn[i] = n;
                    n.size = 0;
                } else {
                    o = gc[i] || (gc[i] = {x: 0, y: 0, count: 0});
                    o.x += n.x;
                    o.y += n.y;
                    o.count += 1; // keep in mind that we count both regular nodes and 'helpers' here, so don't (re)use .count as a measure for the number of nodes in the group in your own code!
                }
            });

            console.log('Processed previous', 'previous group', gn, 'previous nodes', gc);
        }

        // determine nodes
        for (var k = 0; k < data.nodes.length; ++k) {
            var n = data.nodes[k],
                    i = index(n),
                    l = gm[i] || (gm[i] = gn[i]) || (gm[i] = {group: i, size: 0, nodes: []});

            if (expand[i] === 1) {
                // the node should be directly visible
                nm[nodeid(n)] = n;
                nodes.push(n);
                if (gn[i]) {
                    // place new nodes at cluster location (plus jitter)
                    n.x = gn[i].x + Math.random();
                    n.y = gn[i].y + Math.random();
                }
            } else {
                // the node is part of a collapsed cluster
                if (l.size === 0) {
                    // if new cluster, add to set and position at centroid of leaf nodes
                    nm[nodeid(n)] = l;
                    l.size = 1; // hack to make nodeid() work correctly for the new group node
                    nm[nodeid(l)] = l;
                    l.size = 0; // undo hack
                    nodes.push(l);
                    if (gc[i]) {
                        l.x = gc[i].x / gc[i].count;
                        l.y = gc[i].y / gc[i].count;
                    }
                } else {
                    // have element node point to group node:
                    nm[nodeid(n)] = l; // l = shortcut for: nm[nodeid(l)];
                }
                l.nodes.push(n);
            }
            // always count group size as we also use it to tweak the force graph strengths/distances
            l.size += 1;
            n.group_data = l;
        }
        console.log('Processed nodes', 'group map', gm, 'node map', nm);

        for (i in gm) {
            gm[i].link_count = 0;
        }

        // determine links
        for (k = 0; k < data.links.length; ++k) {
            var e = data.links[k],
                    u = index(e.real_source),
                    v = index(e.real_target);
            if (u != v) {
                gm[u].link_count++;
                gm[v].link_count++;
            }
            // while d3.layout.force does convert link.source and link.target NUMERIC values to direct node references,
            // it doesn't for other attributes, such as .real_source, so we do not use indexes in nm[] but direct node
            // references to skip the d3.layout.force implicit links conversion later on and ensure that both .source/.target
            // and .real_source/.real_target are of the same type and pointing at valid nodes.
            u = nm[nodeid(e.source)];
            v = nm[nodeid(e.target)];
            if (u == v) {
                // skip links from node to same (A-A); they are rendered as 0-length lines anyhow. Less links in array = faster animation.
                continue;
            }
            var ui = nodeid(u), vi = nodeid(v), i = (ui < vi ? ui + '|' + vi : vi + '|' + ui),
                    l = lm[i] || (lm[i] = {source: u, target: v, real_source: u, real_target: v, size: 0, visible: e.visible});
            l.size += 1;
        }
        for (i in lm) {
            links.push(lm[i]);
        }
        console.log('Processed links', 'link map', lm);

        return {nodes: nodes, links: links};
    }


    /** Slider control for graph zoom level **/
    function createZoomSlider() {
        d3.select(css.IDs.zoomSlider).call(
                d3.slider()
                .value(settings.baseZoomValue)
                .on('slide', function(evt, value) {
                    d3.select(css.IDs.zoomValue).text(Math.round(value));
                    resize(value);
                }));
    }

    function resize(size) {
        var scaling = size / settings.baseZoomValue;
        svg.selectAll(css.classes.nodeText).style('font-size', settings.baseFontSize * scaling + 'px');
        svg.selectAll(css.classes.nodeImage).attr('transform', 'scale(' + scaling + ')');
        svg.selectAll(css.classes.link).style('stroke-width', scaling);
    }

    /** PRIVATE UTILITY FUNCTIONS **/
    function convexHulls(nodes, index, offset) {
        var hulls = {};

        // create point sets
        for (var k = 0; k < nodes.length; ++k) {
            var n = nodes[k];
            if (n.size)
                continue;
            var i = index(n),
                    l = hulls[i] || (hulls[i] = []);
            l.push([n.x - offset, n.y - offset]);
            l.push([n.x - offset, n.y + offset]);
            l.push([n.x + offset, n.y - offset]);
            l.push([n.x + offset, n.y + offset]);
        }

        // create convex hulls
        var hullset = [];
        for (i in hulls) {
            hullset.push({group: i, path: d3.geom.hull(hulls[i])});
        }

        return hullset;
    }

    function drawCluster(d) {
        return curve(d.path); // 0.8
    }


    function nodeid(n) {
        return n.size > 0 ? 'Group ' + n.group : n.size < 0 ? '_h_' + n.id : n.name;
    }

    function linkid(l) {
        var u = nodeid(l.source),
                v = nodeid(l.target);
        return u < v ? u + '|' + v : v + '|' + u;
    }

    function getGroup(n) {
        return n.group;
    }

    function cycleState(i) {
        var s = expand[i] || 0;
        s++;
        s %= 2;
        return expand[i] = s;
    }

//    function randomRGB() {
//        return 'rgb(' + randomInt(255) + ',' + randomInt(255) + ',' + randomInt(255) + ')';
//    }
//
//    function randomInt(int) {
//        return Math.floor(Math.random() * int);
//    }

    /** EVENT HANDLER FUNCTIONS **/
    function click(d) {
        if (d3.event.defaultPrevented) {
            return; // click suppressed
        }

        console.log('node click', d, arguments, this, expand[d.group]);
        // clicking on 'path helper nodes' shouln't expand/collapse the group node:
        if (d.size < 0)
            return;
        cycleState(d.group);
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

    /** GRAPH UPDATE **/
    function tick() {
        if (!hull.empty()) {
            hull.data(convexHulls(net.nodes, getGroup, off))
                    .attr('d', drawCluster);
        }

        link = linkg.selectAll(css.classes.link);
        link
                .attr('x1', function(d) {
                    return d.source.x + (d.size > 0 ? d.size + dr : d.size < 0 ? dr - 2 : dr + 1) / 2;
                })
                .attr('y1', function(d) {
                    return d.source.y + (d.size > 0 ? d.size + dr : d.size < 0 ? dr - 2 : dr + 1) / 2;
                })
                .attr('x2', function(d) {
                    return d.target.x + (d.size > 0 ? d.size + dr : d.size < 0 ? dr - 2 : dr + 1) / 2;
                })
                .attr('y2', function(d) {
                    return d.target.y + (d.size > 0 ? d.size + dr : d.size < 0 ? dr - 2 : dr + 1) / 2;
                });
        link
                .attr('opacity', function(d) {
                    if (d.visible) {
                        return 100;
                    } else {
                        return 0;
                    }
                });

        node = nodeg.selectAll('g');
        node.attr('transform', function(d) {
            return 'translate(' + d.x + ',' + d.y + ')';
        });
    }

    /** PUBLIC INTERFACE **/
    return {
        display: main,
        css: css,
        settings: settings
    };
}(); // end versastack.adminview module 