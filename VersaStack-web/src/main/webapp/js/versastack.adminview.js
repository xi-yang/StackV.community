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

    var owns = Object.prototype.hasOwnProperty;

    var css = {
        classes: {
            nodeText: '.node text',
            nodeImage: '.node image',
            link: '.link'
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
//                    nodeTextOffsetX: 30,
//                    nodeTextOffsetY: 15,
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
            }, 250);
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
        var distance = settings.baseDistance * (1 + (100 * k));

        console.log('Calculated k-value ' + k);
        console.log('Calculated charge ' + charge);
        console.log('Calculated gravity ' + gravity);
        console.log('Calculated distance ' + distance);


        force = d3.layout.force()
                .size([canvasWidth, canvasHeight])
                .charge(charge)
                .gravity(gravity)
                .linkDistance(distance)
                .nodes(versastack.model.nodes)
                .links(versastack.model.links)
                .on('tick', tick);

        svg = d3.select('body').append('svg')
                .attr('width', canvasWidth)
                .attr('height', canvasHeight);

        drag = force.drag()
                .on('drag', ondrag);

        console.log('Success');

        restart();
    }

    function restart() {
        link = svg.selectAll('.link')
                .data(versastack.model.links)
                .enter().append('line')
                .classed('link', true);

        node = svg.selectAll('.node')
                .data(versastack.model.nodes);

        // Enter
        node.enter()
                .append('g')
                .classed('node', true)
                .on('dblclick', dblclick)
                .on('click', click)
                .call(drag);

        node.append('rect')
                .style({'stroke': 'black', 'stroke-width': '4', 'opacity': '0.5', 'fill': randomRGB})
                .attr('width', settings.topologyPanelWidth)
                .attr('height', settings.topologyPanelHeight)
                .attr('x', 0)
                .attr('y', -settings.topologyPanelHeight / 2 + settings.iconHeight / 2)
                .attr('rx', settings.topologyPanelBorderRounding)
                .attr('ry', settings.topologyPanelBorderRounding);

        node.append('image')
                .attr('xlink:href', function(d) {
                    return d.icon;
                })
                .attr('x', 0)
                .attr('y', 0)
                .attr('width', settings.iconWidth)
                .attr('height', settings.iconHeight);

        node.append('text')
                .attr('x', settings.iconWidth)
                .attr('y', settings.iconHeight / 2)
                .text(function(d) {
                    return d.name;
                });

        // Exit
        node.exit().remove();

        force.start();
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
    function randomRGB() {
        return 'rgb(' + randomInt(255) + ',' + randomInt(255) + ',' + randomInt(255) + ')';
    }

    function randomInt(int) {
        return Math.floor(Math.random() * int);
    }

    /** EVENT HANDLER FUNCTIONS **/
    function click(d) {
        if (d3.event.defaultPrevented) {
            return; // click suppressed
        }

        console.log('Clicked on');
        console.log(d);

        var key, node, index;
        var topology = findByName(versastack.model.topologies, d.name);
        if (topology) {
            for (key in topology.nodes) {
                if (owns.call(topology.nodes, key)) {
                    node = topology.nodes[key];

                    console.log('Toggle ' + node.name + ' visible:' + !node.visible);
                    node.visible = !node.visible;

                    index = indexOfName(node.name);

                    if (index < 0) {
                        versastack.model.nodes.push(node);
                    } else {
//                                    versastack.model.nodes.splice(index, 1);
                    }
                }
            }
            tick();
        }
    }

    function dblclick(d) {
        d3.select(this).classed('fixed', d.fixed = false);
    }

    function ondrag(d) {
        d3.select(this).classed('fixed', d.fixed = true);
    }

    /** GRAPH UPDATE **/
    function tick() {
        link = svg.selectAll('.link');
        link
                .attr('x1', function(d) {
                    return d.source.x + settings.iconWidth / 2;
                })
                .attr('y1', function(d) {
                    return d.source.y + settings.iconHeight / 2;
                })
                .attr('x2', function(d) {
                    return d.target.x + settings.iconWidth / 2;
                })
                .attr('y2', function(d) {
                    return d.target.y + settings.iconHeight / 2;
                });
        link
                .attr('opacity', function(d) {
                    if (d.visible) {
                        return 100;
                    } else {
                        return 0;
                    }
                });

        node = svg.selectAll('.node');
        node.attr('transform', function(d) {
            return 'translate(' + d.x + ',' + d.y + ')';
        });
        node.attr('opacity', function(d) {
            if (d.visible) {
                return 100;
            } else {
                return 0;
            }
        });
    }

    /** PUBLIC INTERFACE **/
    return {
        display: main,
        css: css,
        settings: settings
    };
}(); // end versastack.adminview module 