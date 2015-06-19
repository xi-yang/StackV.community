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
        HULL_OPACITY: .2,
        EDGE_COLOR: "rgb(0,0,0)",
        EDGE_WIDTH: 2
    };

    var redraw_;

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
            svgContainer.select("#topology").selectAll("*").remove();//Clear the previous drawing
            svgContainer.select("#edge").selectAll("*").remove();//Clear the previous drawing
            svgContainer.select("#node").selectAll("*").remove();//Clear the previous drawing
            var nodeList = model.listNodes();
            var edgeList = model.listEdges();

            //Recall that topologies are also considered nodes
            //We render them seperatly to enfore a z-ordering
            map_(nodeList, drawTopology);
            map_(edgeList, drawEdge);
            map_(nodeList, drawNode);

        }
        redraw_ = redraw;
        /**@param {Node} n**/
        function drawNode(n) {
            if (n.isLeaf()) {
                svgContainer.select("#node").append("image")
                        .attr("xlink:href", n.getIconPath())
                        .attr("x", n.x - settings.NODE_SIZE / 2)
                        .attr("y", n.y - settings.NODE_SIZE / 2)
                        .attr('height', settings.NODE_SIZE)
                        .attr('width', settings.NODE_SIZE)
                        .on("click", onNodeClick.bind(undefined, n))
                        .on("dblclick", onNodeDblClick.bind(undefined, n))
                        .on("mousemove", onNodeMouseMove.bind(undefined, n))
                        .on("mouseleave", onNodeMouseLeave)
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
                svgContainer.select("#topology").append("path")
                        .style("fill", settings.HULL_COLOR)
                        .style("stroke", settings.HULL_COLOR)
                        .style("stroke-width", settings.TOPOLOGY_SIZE)
                        .style("stroke-linejoin", "round")
//                        .style("stroke-opacity", settings.HULL_OPACITY)
//                        .style("fill-opacity", settings.HULL_OPACITY)
                        .style("opacity", settings.HULL_OPACITY)
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
                        .on("mousemove", onNodeMouseMove.bind(undefined, n))
                        .on("mouseleave", onNodeMouseLeave)
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
            svgContainer.select("#edge").append("line")
                    .attr("x1", e.source.x)
                    .attr("y1", e.source.y)
                    .attr("x2", e.target.x)
                    .attr("y2", e.target.y)
                    .style("stroke", settings.EDGE_COLOR)
                    .style("stroke-width", settings.EDGE_WIDTH);
        }

        /**
         * Note that n could also be a topology
         * @param {Node} n**/
        function onNodeClick(n) {
            d3.event.stopPropagation();//prevent the click from being handled by the background, which would hide the panel
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
            //We will never send a mouseleave event as the node is being removed
            document.getElementById("hoverdiv").style.visibility = "hidden";
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
        function onNodeMouseMove(n) {
            var hovertext = n.getName();
            document.getElementById("hoverdiv").innerText = hovertext;
            document.getElementById("hoverdiv").style.left = d3.event.x + "px";
            document.getElementById("hoverdiv").style.top = d3.event.y + 10 + "px";
            document.getElementById("hoverdiv").style.visibility = "visible";
        }

        function onNodeMouseLeave() {
            document.getElementById("hoverdiv").style.visibility = "hidden";
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
        doRender: doRender,
        redraw: function () {
            redraw_();
        }
    };
});