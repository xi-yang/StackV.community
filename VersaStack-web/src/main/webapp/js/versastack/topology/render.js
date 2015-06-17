"use strict";

//For debug purpuses only
var selectedNode;
var debugPoint = {x: 0, y: 0};
define([
    "local/d3", "local/versastack/utils"
], function (d3, utils) {
    var map_ = utils.map_;

    var settings = {
        NODE_SIZE: 30,
        TOPOLOGY_SIZE: 45,
        HULL_COLOR: "rgb(0,100,255)",
        HULL_OPACITY: "20%",
        EDGE_COLOR: "rgb(0,0,0)",
        EDGE_WIDTH: 2
    };



    /**@param {outputApi} outputApi
     * @param {Model} model
     **/
    function doRender(outputApi, model) {
        //outputApi may start zoomed in, as a workaround for the limit of how 
        //far out we can zoom. in order to prevent changes in this parameter 
        //affecting the meaning of our size related parameters, we scale them 
        //appropriatly
        settings.NODE_SIZE /= outputApi.getZoom();
        settings.TOPOLOGY_SIZE /= outputApi.getZoom();
        settings.EDGE_WIDTH /= outputApi.getZoom();

        var svgContainer = outputApi.getSvgContainer();
        redraw();

        function redraw() {
            svgContainer.selectAll("*").remove();//Clear the previous drawing
//            makeGrid();
            var nodeList = model.listNodes();
            var edgeList = model.listEdges();

            //Recall that topologies are also considered nodes
            //We render them seperatly to enfore a z-ordering
            map_(nodeList, drawTopology);
            map_(edgeList, drawEdge);
            map_(nodeList, drawNode);

        }

        /**@param {Node} n**/
        function drawNode(n) {
            if (n.isLeaf()) {
                var svgNode = svgContainer.append("image")
                        .attr("xlink:href", n.getIconPath())
                        .attr("x", n.x - settings.NODE_SIZE / 2)
                        .attr("y", n.y - settings.NODE_SIZE / 2)
                        .attr('height', settings.NODE_SIZE)
                        .attr('width', settings.NODE_SIZE)
                        .on("click", onNodeClick.bind(undefined, n))
                        .on("dblclick", onNodeDblClick.bind(undefined, n))
                        .call(makeDragBehaviour(n));
            }
        }
        /**@param {Node} n**/
        function drawTopology(n) {
            if (!n.isLeaf()) {
                //render the convex hull surounding the decendents of n
                var leaves = n.getLeaves();
                if (leaves.length === 0) {
                    return;
                }
                if (leaves.length === 1) {
                    //If all leaves are the same point, then the hull will be just
                    //A single point, and not get rendered.
                    //By forcing it to take distinct points, the stroke-width 
                    //Causes it to render at full size
                    var leaf = leaves[0];
                    leaves.push({x: leaf.x + .01, y: leaf.y + .01});
                }
                while (leaves.length < 3) {
                    //Even with two distinct point, the path will not exist
                    //Adding a third point (even if it is a duplicate) seems to fix this
                    leaves.push(leaves[0]);
                }

                var path = d3.geom.hull()
                        .x(function (n) {
                            return n.x;
                        })
                        .y(function (n) {
                            return n.y;
                        })
                        (leaves);
                var hull = svgContainer.append("path")
                        .style("fill", settings.HULL_COLOR)
                        .style("stroke", settings.HULL_COLOR)
                        .style("stroke-width", settings.TOPOLOGY_SIZE)
                        .style("stroke-linejoin", "round")
                        .style("stroke-opacity", settings.HULL_OPACITY)
                        .datum(path)
                        .attr("d", function (d) {
                            //@param d is the datum set above
                            //see https://developer.mozilla.org/en-US/docs/Web/SVG/Attribute/d
                            if (d.length === 0) {
                                return;
                            }
                            var ans = "M" + d[0].x + " " + d[0].y + " ";
                            for (var i = 1; i < d.length; i++) {
                                ans += "L" + d[i].x + " " + d[i].y + " ";
                            }
                            ans += "Z";
                            return ans;
                        })
                        .on("click", onNodeClick.bind(undefined, n))
                        .on("dblclick", onNodeDblClick.bind(undefined, n))
                        .call(makeDragBehaviour(n));
            }

        }

        /**@param {Node} n**/
        function makeDragBehaviour(n) {
            return d3.behavior.drag()
                    .on("drag", function () {
                        var e = d3.event;
                        move(n, e.dx, e.dy);
                        redraw();
                    })
                    .on("dragstart", function () {
                        outputApi.disablePanning();
                    })
                    .on("dragend", function () {
                        outputApi.enablePanning();
                    });
        }

        /**@param {Edge} e**/
        function drawEdge(e) {
            svgContainer.append("line")
                    .attr("x1", e.source.x)
                    .attr("y1", e.source.y)
                    .attr("x2", e.target.x)
                    .attr("y2", e.target.y)
                    .style("stroke", settings.EDGE_COLOR)
                    .style("stroke-width", settings.EDGE_WIDTH);
        }

        //For debuging
        function makeGrid() {
            svgContainer.append("circle")
                    .attr("cx", debugPoint.x)
                    .attr("cy", debugPoint.y)
                    .attr("r", 10)
                    .style("fill", "red");
            for (var x = 0; x < 200; x += 20) {
                for (var y = 0; y < 100; y += 20) {
                    svgContainer.append("line")
                            .attr("x1", 0)
                            .attr("y1", y)
                            .attr("x2", 1000)
                            .attr("y2", y)
                            .style("stroke", "black")
                            .style("stroke-width", 1);
                    svgContainer.append("line")
                            .attr("x1", x)
                            .attr("y1", 0)
                            .attr("x2", x)
                            .attr("y2", 1000)
                            .style("stroke", "black")
                            .style("stroke-width", 1);
                }
            }
        }

        /**
         * Note that n could also be a topology
         * @param {Node} n**/
        function onNodeClick(n) {
            outputApi.setActiveName(n.getName());
            var services = map_(n.services, /**@param {Service} service**/function (service) {
                return service.getTypeBrief();
            });
            outputApi.setServices(services);
            selectedNode = n;
        }
        /**
         * Note that n could also be a topology
         * @param {Node} n**/
        function onNodeDblClick(n) {
            //The coordinates provided seem not to line up with where the mouse is,
            //So we use the center of mass to stay consistent
            var e = d3.event;
            var chords = n.getCenterOfMass();
            n.toggleFold();
            if (n.isFolded) {
                //there is no guarantee that n is posistioned anywhere near its children
                //to solve this, we force n to be located at the click
                n.x = chords.x;
                n.y = chords.y;
            }
            redraw();
        }


        /**@param {Node} n**/
        function move(n, dx, dy) {
            n.x += dx;
            n.y += dy;
            map_(n.children, function (child) {
                move(child, dx, dy);
            });
        }
    }


    return{
        doRender: doRender
    };
});