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
        SERVICE_SIZE: 10,
        TOPOLOGY_SIZE: 15,
        TOPOLOGY_BUFFER: 30,
        TOPOLOGY_ANCHOR_SIZE: 8,
        TOPOLOGY_ANCHOR_STROKE: 2,
        ENLARGE_FACTOR: .2,
        HULL_COLORS: ["rgb(0,100,255)", "rgb(255,0,255)"],
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
        settings.SERVICE_SIZE /= outputApi.getZoom();
        settings.TOPOLOGY_SIZE /= outputApi.getZoom();
        settings.TOPOLOGY_BUFFER /= outputApi.getZoom();
        settings.EDGE_WIDTH /= outputApi.getZoom();
        settings.TOPOLOGY_ANCHOR_SIZE /= outputApi.getZoom();
        settings.TOPOLOGY_ANCHOR_STROKE /= outputApi.getZoom();

        var svgContainer = outputApi.getSvgContainer();

        redraw();

        var nodeList, edgeList;
        function redraw() {
            svgContainer.select("#topology").selectAll("*").remove();//Clear the previous drawing
            svgContainer.select("#edge").selectAll("*").remove();//Clear the previous drawing
            svgContainer.select("#node").selectAll("*").remove();//Clear the previous drawing
            svgContainer.select("#anchor").selectAll("*").remove();//Clear the previous drawing
            nodeList = model.listNodes();
            edgeList = model.listEdges();

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
                n.svgNode = svgContainer.select("#node").append("image")
                        .attr("xlink:href", n.getIconPath())
                        .on("click", onNodeClick.bind(undefined, n))
                        .on("dblclick", onNodeDblClick.bind(undefined, n))
                        .on("mousemove", onNodeMouseMove.bind(undefined, n))
                        .on("mouseleave", onNodeMouseLeave.bind(undefined, n))
                        .call(makeDragBehaviour(n));
                setElementSize(n, false);
                drawServices(n);
                updateSvgChoordsNode(n);
            }
        }
        /**@param {Node} n**/
        function drawTopology(n) {
            if (!n.isLeaf()) {
                //render the convex hull surounding the decendents of n
                var path = getTopolgyPath(n);
                var color = settings.HULL_COLORS[n.getDepth() % settings.HULL_COLORS.length];
                n.svgNode = svgContainer.select("#topology").append("path")
                        .style("fill", color)
                        .style("stroke", color)
                        .style("stroke-width", settings.TOPOLOGY_SIZE + settings.TOPOLOGY_BUFFER * n.getHeight())
                        .style("stroke-linejoin", "round")
                        .style("opacity", settings.HULL_OPACITY)
                        .attr("d", topologyPathToString(path))
                        .on("click", onNodeClick.bind(undefined, n))
                        .on("dblclick", onNodeDblClick.bind(undefined, n))
                        .on("mousemove", onNodeMouseMove.bind(undefined, n))
                        .on("mouseleave", onNodeMouseLeave.bind(undefined, n))
                        .call(makeDragBehaviour(n));
                n.svgNodeAnchor = svgContainer.select("#anchor").append("rect")
                        .style("fill", "white")
                        .style("stroke", "black")
                        .style("stroke-width", settings.TOPOLOGY_ANCHOR_STROKE)
                        .on("click", onNodeClick.bind(undefined, n))
                        .on("dblclick", onNodeDblClick.bind(undefined, n))
                        .on("mousemove", onNodeMouseMove.bind(undefined, n))
                        .on("mouseleave", onNodeMouseLeave.bind(undefined, n))
                        .call(makeDragBehaviour(n));
                setElementSize(n, false);
                drawServices(n);
                updateSvgChoordsNode(n);

//                //Debug, show the coordinate of the topology node itself
//                svgContainer.select("#topology").append("circle")
//                        .attr("cx", n.x)
//                        .attr("cy", n.y)
//                        .attr("r", 2)
//                        .style("fill", "red")
            }

        }

        /**@param {Node} n**/
        function drawServices(n) {
            n.svgNodeServices = svgContainer.select("#node").append("g");
            map_(n.services, /**@param {Service} service**/function (service) {
                service.svgNode = n.svgNodeServices.append("image")
                        .attr("xlink:href", service.getIconPath())
                        //The click events fold move, and select nodes, in 
                        //which case, we want to behave the same regardless
                        //of if a node or its service was the target. In 
                        //contrast, the mousMove event is for the popup, and
                        //we may want to display different info when we
                        //hover over a service
                        .on("click", onNodeClick.bind(undefined, service))
                        .on("dblclick", onNodeDblClick.bind(undefined, n))
                        .on("mousemove", onNodeMouseMove.bind(undefined, service))
                        .on("mouseleave", onNodeMouseLeave.bind(undefined, service))
                        .call(makeDragBehaviour(n));
                setElementSize(service, false);
            });
            updateSvgChoordsService(n);
        }

        /**@param {Node} n**/
        function computeServiceCoords(n) {
            var ans = {x: null, y: null, transform: ""}
            if (n.isLeaf()) {
                ans.x = n.x - settings.NODE_SIZE / 2;
                ans.y = n.y + settings.NODE_SIZE / 2;
            } else {
                var path = getTopolgyPath(n);
                if (n.getLeaves().length > 1) {
                    //Get the highest point.
                    var highest = 0;
                    for (var i = 0; i < path.length; i++) {
                        if (path[i].y < path[highest].y) {
                            highest = i;
                        }
                    }
                    var p1 = path[highest];
                    //we know want to determine which neighbors of path[ans] give the shallower edge
                    var left = highest === 0 ? path.length - 1 : highest - 1;
                    var right = (highest + 1) % path.length;
                    left = path[left];
                    right = path[right];
                    var leftSlope = Math.abs((p1.y - left.y) / (p1.x - left.x));
                    var rightSlope = Math.abs((p1.y - right.y) / (p1.x - right.x));
                    var p2;
                    if (leftSlope < rightSlope) {
                        p2 = left;
                    } else {
                        p2 = right;
                    }

                    var p = {x: (p1.x + p2.x) / 2, y: (p1.y + p2.y) / 2};
                    //compute the desired distance between the services, and the line p1p2 
                    var normalOffset = settings.TOPOLOGY_SIZE / 2 + settings.TOPOLOGY_BUFFER / 2 * (n.getHeight()) + settings.SERVICE_SIZE;
                    //convert the above offset into the xy plane, and apply it to p
                    var theta = Math.atan2(p1.y - p2.y, p1.x - p2.x);
                    if (theta < -Math.PI / 2) {
                        theta += Math.PI;
                    }
                    if (theta > Math.PI / 2) {
                        theta -= Math.PI;
                    }
                    p.x += normalOffset * Math.sin(theta);
                    p.y -= normalOffset * Math.cos(theta);

                    ans.x = p.x - settings.SERVICE_SIZE * n.services.length / 2;
                    ans.y = p.y;
                    ans.transform = "rotate(" + theta * 180 / Math.PI + " " + p.x + " " + (ans.y + settings.SERVICE_SIZE / 2) + ")";
                } else {
                    ans.x = path[0].x - settings.SERVICE_SIZE * n.services.length / 2;
                    ans.y = path[0].y - settings.TOPOLOGY_SIZE / 2 - settings.TOPOLOGY_BUFFER / 2 * (n.getHeight()) - settings.SERVICE_SIZE;
                }
            }
            return ans;
        }

        function getTopolgyPath(n) {
            var leaves = n.getLeaves();
            if (leaves.length === 0) {
                return;
            }
            if (leaves.length === 1) {
                //If all leaves are the same point, then the hull will be just
                //A single point, and not get rendered.
                //By forcing it to take distinct points, the stroke-width 
                //Causes it to render at full size
                //For some reason, using an svg transform (as we do to implement highlighting" 
                //causes sub-unit diferences to be ignored
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
            return path;
        }
        function topologyPathToString(path) {
            //@param d is the datum set above
            //see https://developer.mozilla.org/en-US/docs/Web/SVG/Attribute/d
            if (path.length === 0) {
                return;
            }
            var ans = "M" + path[0].x + " " + path[0].y + " ";
            for (var i = 1; i < path.length; i++) {
                ans += "L" + path[i].x + " " + path[i].y + " ";
            }
            ans += "Z";
            return ans;
        }

        /**@param {Node} n**/
        function updateSvgChoordsNode(n) {
            var svg = n.svgNode;
            if (!svg) {
                console.log("No svg element in node");
            }
            if (n.isLeaf()) {
                svg.attr("x", n.x - n.size / 2);
                svg.attr("y", n.y - n.size / 2);
            } else {
                var path = getTopolgyPath(n);
                svg.attr("d", topologyPathToString(path));
            }

            var svgAchor = n.svgNodeAnchor;
            if (svgAchor) {
                var choords = n.getCenterOfMass();
                svgAchor.attr("x", choords.x - settings.TOPOLOGY_ANCHOR_SIZE / 2);
                svgAchor.attr("y", choords.y - settings.TOPOLOGY_ANCHOR_SIZE / 2);
            }
            updateSvgChoordsService(n);
        }

        function updateSvgChoordsService(n) {
            var svgServiceContainer = n.svgNodeServices;
            var coords = computeServiceCoords(n);
            map_(n.services, function (service) {
                service.y = coords.y;
                service.x = coords.x;
                service.svgNode
                        .attr("y", coords.y)
                        .attr("x", coords.x)
                        .attr("transform", coords.transform);
                coords.x += settings.SERVICE_SIZE;
            });
        }

        /**@param {Edge} e**/
        function updateSvgChoordsEdge(e) {
            var src = e.source.getCenterOfMass();
            var tgt = e.target.getCenterOfMass();
            e.svgNode.attr("x1", src.x)
                    .attr("y1", src.y)
                    .attr("x2", tgt.x)
                    .attr("y2", tgt.y);
        }

        var lastMouse;
        /**@param {Node} n**/
        var isDragging = false;
        function makeDragBehaviour(n) {
            return d3.behavior.drag()
                    .on("drag", function () {
                        //Using the dx,dy from d3 can lead to some artifacts when also using
                        //These seem to occur when moving between different transforms
                        var e = d3.event.sourceEvent;
                        var dx = (e.clientX - lastMouse.clientX) / outputApi.getZoom();
                        var dy = (e.clientY - lastMouse.clientY) / outputApi.getZoom();
                        lastMouse = e;
                        move(n, dx, dy);

                        //Fix the topolgies above us
                        var cursor = n._parent;
                        while (cursor) {
                            updateSvgChoordsNode(cursor);
                            cursor = cursor._parent;
                        }

                        //fix all edges
                        map_(edgeList, updateSvgChoordsEdge);

                        //As we drag, the cursor may enter and leave the bounding box of n
                        //In onNodeMouseLeave we make the hoverdiv stay visible in this case,
                        //However, we also want it to continue tracking us.
                        outputApi.setHoverLocation(e.clientX, e.clientY);
                        drawHighlight();
                    })
                    .on("dragstart", function () {
                        lastMouse = d3.event.sourceEvent;
                        outputApi.disablePanning();
                        isDragging = true;
                    })
                    .on("dragend", function () {
                        outputApi.enablePanning();
                        isDragging = false;
                    });
        }

        /**@param {Edge} e**/
        function drawEdge(e) {
            e.svgNode = svgContainer.select("#edge").append("line")
                    .style("stroke", settings.EDGE_COLOR)
                    .style("stroke-width", settings.EDGE_WIDTH);
            updateSvgChoordsEdge(e);
        }


        var highlightedNode=null;
        /**
         * Note that n could also be a topology
         * @param {Node} n**/
        function onNodeClick(n) {
            d3.event.stopPropagation();//prevent the click from being handled by the background, which would hide the panel
            highlightedNode=n;
            drawHighlight();
            outputApi.setDisplayName(n.getName());
            /**@type {DropDownTree} displayTree**/
            var displayTree = outputApi.getDisplayTree();
            displayTree.clear();
            
            n.populateTreeMenu(displayTree);
            displayTree.draw();
            selectedNode = n;
        }
        
        function drawHighlight(){
            svgContainer.select("#selectedOverlay").selectAll("*").remove();
            if(highlightedNode && highlightedNode.svgNode){
                var toAppend=highlightedNode.svgNode.node().cloneNode();
                d3.select(toAppend).style("opacity","1").attr("pointer-events","none");
                svgContainer.select("#selectedOverlay").node().appendChild(toAppend);
            }else if(highlightedNode){
                //This shouldn't happen
                console.log("Trying to highlight an element without an svgNode");
            }
        }
        
        /**
         * Note that n could also be a topology
         * @param {Node} n**/
        function onNodeDblClick(n) {
            //We will never send a mouseleave event as the node is being removed
            outputApi.setHoverVisible(false);
            ;
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
            //As we drag a node, the cursor may temporarliy leave the bounding box
            //of said node, causing flicker of the hoverdiv
            if (!isDragging) {
                outputApi.setHoverText(n.getName());
                outputApi.setHoverLocation(d3.event.x, d3.event.y);
                outputApi.setHoverVisible(true);
                setElementSize(n, true);
            }
        }

        function onNodeMouseLeave(n) {
            //As we drag a node, the cursor may temporarliy leave the bounding box
            //of said node, causing flicker of the hoverdiv
            if (!isDragging) {
                outputApi.setHoverVisible(false);
                setElementSize(n, false);
            }
        }

        function setElementSize(n, enlarge) {
            var size, svg, ds, x, y;
            switch (n.getType()) {
                case "Node":
                    var size = settings.NODE_SIZE;
                    svg = n.svgNode;
                    x = n.x - settings.NODE_SIZE / 2;
                    y = n.y - settings.NODE_SIZE / 2;
                    break;
                case "Topology":
                    var size = settings.TOPOLOGY_ANCHOR_SIZE;
                    svg = n.svgNodeAnchor;
                    var choords = n.getCenterOfMass();
                    x = choords.x - settings.TOPOLOGY_ANCHOR_SIZE / 2;
                    y = choords.y - settings.TOPOLOGY_ANCHOR_SIZE / 2;
                    enlarge = false;//Enlarging the topology does not look good
                    break;
                case "Service":
                    var size = settings.SERVICE_SIZE;
                    svg = n.svgNode;
                    x = n.x;
                    y = n.y;
                    break;
                default:
                    console.log("Unknown Type: " + n.getType());
            }
            if (enlarge) {
                ds = size * settings.ENLARGE_FACTOR;
                size += ds;
            } else {
                ds = 0;
            }
            n.size = size;
            svg
                    .attr("width", size)
                    .attr("height", size)
                    .attr("x", x - ds / 2)//make it appear to zoom into center of the icon
                    .attr("y", y - ds / 2);
            drawHighlight();
        }

        /**@param {Node} n**/
        function move(n, dx, dy) {
            n.x += dx;
            n.y += dy;
            map_(n.children, function (child) {
                move(child, dx, dy);
            });
            updateSvgChoordsNode(n);
        }
    }


    return{
        doRender: doRender,
        redraw: function () {
            redraw_();
        }
    };
});