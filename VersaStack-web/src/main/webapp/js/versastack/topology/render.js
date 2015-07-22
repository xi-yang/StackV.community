"use strict";

//For debug purpuses only
var selectedNode;
var debugPoint = {x: 0, y: 0};
define([
    "local/d3",
    "local/versastack/utils",
    "local/versastack/topology/PortDisplayPopup",
    "local/versastack/topology/switchServicePopup"
], function (d3, utils, PortDisplayPopup, SwitchPopup) {

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
        EDGE_WIDTH: 2,
        DIALOG_NECK_WIDTH: 3,
        DIALOG_NECK_HEIGHT: 40,
        DIALOG_MIN_WIDTH: 15,
        DIALOG_MIN_HEIGHT: 10,
        DIALOG_BEVEL: 5,
        DIALOG_COLOR: "rgb(255,0,0)",
        DIALOG_OPACITY: "0.7",
        DIALOG_PORT_EMPTY_COLOR: "rgb(128,128,0)",
        DIALOG_PORT_COLORS: ["rgb(64,64,64)", "rgb(128,128,128)"],
        DIALOG_PORT_HEIGHT: 6,
        DIALOG_PORT_WIDTH: 8,
        DIALOG_PORT_LEAD: 8,
        DIALOG_PORT_BUFFER_VERT: 2,
        DIALOG_PORT_BUFFER_HORZ: 3,
        DIALOG_OFFSET_X: 0,
        DIALOG_OFFSET_Y: -20
    };
    var switchSettings = {
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
        EDGE_WIDTH: 2,
        DIALOG_NECK_WIDTH: 5,
        DIALOG_NECK_HEIGHT: 40,
        DIALOG_MIN_WIDTH: 130,
        DIALOG_MIN_HEIGHT: 110,
        SWITCH_MIN_WIDTH: 20,
        SWITCH_MIN_HEIGHT: 8,
        DIALOG_BEVEL: 10,
        DIALOG_COLOR: "rgb(255,0,0)",
        DIALOG_TAB_COLOR: "rgb(31,178,223)",
        DIALOG_BUFFER: 2,
        DIALOG_PORT_COLOR: "rgb(0,0,0)",
        DIALOG_PORT_EMPTY_COLOR: "rgb(128,128,50)",
        DIALOG_PORT_HEIGHT: 4,
        DIALOG_PORT_WIDTH: 8

    };
    var redraw_;
    var API = {};
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
        settings.DIALOG_NECK_WIDTH /= outputApi.getZoom();
        settings.DIALOG_NECK_HEIGHT /= outputApi.getZoom();
        settings.DIALOG_MIN_WIDTH /= outputApi.getZoom();
        settings.DIALOG_MIN_HEIGHT /= outputApi.getZoom();
        settings.DIALOG_BEVEL /= outputApi.getZoom();
        settings.DIALOG_PORT_HEIGHT /= outputApi.getZoom();
        settings.DIALOG_PORT_WIDTH /= outputApi.getZoom();
        settings.DIALOG_PORT_LEAD /= outputApi.getZoom();
        settings.DIALOG_PORT_BUFFER_VERT /= outputApi.getZoom();
        settings.DIALOG_PORT_BUFFER_HORZ /= outputApi.getZoom();
        settings.DIALOG_OFFSET_X /= outputApi.getZoom();
        settings.DIALOG_OFFSET_Y /= outputApi.getZoom();
        //switch setting
        switchSettings.NODE_SIZE /= outputApi.getZoom();
        switchSettings.SERVICE_SIZE /= outputApi.getZoom();
        switchSettings.TOPOLOGY_SIZE /= outputApi.getZoom();
        switchSettings.TOPOLOGY_BUFFER /= outputApi.getZoom();
        switchSettings.EDGE_WIDTH /= outputApi.getZoom();
        switchSettings.TOPOLOGY_ANCHOR_SIZE /= outputApi.getZoom();
        switchSettings.TOPOLOGY_ANCHOR_STROKE /= outputApi.getZoom();
        switchSettings.DIALOG_NECK_WIDTH /= outputApi.getZoom();
        switchSettings.DIALOG_NECK_HEIGHT /= outputApi.getZoom();
        switchSettings.DIALOG_MIN_WIDTH /= outputApi.getZoom();
        switchSettings.SWITCH_MIN_WIDTH /= outputApi.getZoom();
        switchSettings.SWITCH_MIN_HEIGHT /= outputApi.getZoom();
        switchSettings.DIALOG_MIN_HEIGHT /= outputApi.getZoom();
        switchSettings.DIALOG_BEVEL /= outputApi.getZoom();
        switchSettings.DIALOG_PORT_HEIGHT /= outputApi.getZoom();
        switchSettings.DIALOG_PORT_WIDTH /= outputApi.getZoom();
        switchSettings.DIALOG_BUFFER /= outputApi.getZoom();
        var svgContainer = outputApi.getSvgContainer();
        svgContainer.on("click", function () {
            //Clear the selected element.
            //We check the event path so this only happens if we did not actually click on something
            var clickedElem = d3.event.path[0];
            if (clickedElem.id === "viz") {
                selectElement(null);
            }
        });
        var switchPopup = buildSwitchPopup();
        redraw();
        var nodeList, edgeList;
        function redraw() {
            svgContainer.select("#topology").selectAll("*").remove(); //Clear the previous drawing
            svgContainer.select("#edge1").selectAll("*").remove(); //Clear the previous drawing
            svgContainer.select("#edge2").selectAll("*").remove(); //Clear the previous drawing
            svgContainer.select("#node").selectAll("*").remove(); //Clear the previous drawing
            svgContainer.select("#anchor").selectAll("*").remove(); //Clear the previous drawing
            svgContainer.select("#parentPort").selectAll("*").remove();
            nodeList = model.listNodes();
            edgeList = model.listEdges();
            //Recall that topologies are also considered nodes
            //We render them seperatly to enfore a z-ordering
            map_(nodeList, drawTopology);
            map_(nodeList, drawNode);
            drawPopups();
            map_(edgeList, drawEdge);
        }
        /**@param {Node} n**/
        function drawNode(n) {
            if (n.isLeaf()) {
                if (!n.portPopup) {
                    n.portPopup = buildPortDisplayPopup(n);
                }
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
                if (!n.portPopup) {
                    n.portPopup = buildPortDisplayPopup(n);
                }
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
                        .on("click", onServiceClick.bind(undefined, service))
                        .on("dblclick", onNodeDblClick.bind(undefined, n))
                        .on("mousemove", onNodeMouseMove.bind(undefined, service))
                        .on("mouseleave", onNodeMouseLeave.bind(undefined, service))
                        .call(makeDragBehaviour(n));
                setElementSize(service, false);
            });
            updateSvgChoordsService(n);
        }

        function drawPopups() {
            svgContainer.select("#dialogBox").selectAll("*").remove();
            svgContainer.select("#port").selectAll("*").remove();
            svgContainer.select("#parentPort").selectAll("*").remove();
            map_(nodeList, function (n) {
                n.portPopup.render();
            });
            switchPopup.render();
        }

        /**@param {Node} n**/
        function computeServiceCoords(n) {
            var ans = {x: null, y: null, dx: null, dy: null, rotation: null};
            if (n.isLeaf()) {
                ans.x = n.x - settings.NODE_SIZE / 2 + settings.SERVICE_SIZE/2;
                ans.y = n.y + settings.NODE_SIZE / 2 + settings.SERVICE_SIZE/2;
                ans.dx=settings.SERVICE_SIZE;
                ans.dy=0;
                ans.rotation=0;
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
                    var normalOffset = settings.TOPOLOGY_SIZE / 2 + settings.TOPOLOGY_BUFFER / 2 * (n.getHeight()) + settings.SERVICE_SIZE/2;
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
                    //p is now the center point of where we want to draw the services
                    ans.dx = settings.SERVICE_SIZE * Math.cos(theta);
                    ans.dy = settings.SERVICE_SIZE * Math.sin(theta);
                    ans.x = p.x - ans.dx * n.services.length / 2;
                    ans.y = p.y - ans.dy * n.services.length / 2;
                    ans.rotation = theta * 180 / Math.PI;
                } else {
                    ans.x = path[0].x - settings.SERVICE_SIZE * n.services.length / 2 + settings.SERVICE_SIZE/2;
                    ans.y = path[0].y - settings.TOPOLOGY_SIZE / 2 - settings.TOPOLOGY_BUFFER / 2 * (n.getHeight()) - settings.SERVICE_SIZE/2;
                    ans.dx=settings.SERVICE_SIZE;
                    ans.dy=0;
                    ans.rotation=0;
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
            var svgSubnet = n.svgNodeSubnetHighlight;
            if (!svg) {
                console.log("No svg element in node");
                return;
            }
            if (n.isLeaf()) {
                svg.attr("x", n.x - n.size / 2);
                svg.attr("y", n.y - n.size / 2);
            } else {
                var path = getTopolgyPath(n);
                svg.attr("d", topologyPathToString(path));
            }
            if (svgSubnet) {
                if (n.isLeaf()) {
                    svgSubnet.attr("x", n.x - n.size / 2);
                    svgSubnet.attr("y", n.y - n.size / 2);
                } else {
                    var path = getTopolgyPath(n);
                    svgSubnet.attr("d", topologyPathToString(path));
                }
            }
            var svgAchor = n.svgNodeAnchor;
            if (svgAchor) {
                var choords = n.getCenterOfMass();
                svgAchor.attr("x", choords.x - settings.TOPOLOGY_ANCHOR_SIZE / 2);
                svgAchor.attr("y", choords.y - settings.TOPOLOGY_ANCHOR_SIZE / 2);
            }
            updateSvgChoordsService(n);
            n.portPopup.updateSvgChoords();
        }

        function updateSvgChoordsService(n) {
            var coords = computeServiceCoords(n);
            map_(n.services, function (service) {
                service.y = coords.y;
                service.x = coords.x;
                var midY=coords.y + service.dy;
                var midX=coords.x + service.dx;
                service.svgNode
                        .attr("y", coords.y + service.dy - settings.SERVICE_SIZE/2)
                        .attr("x", coords.x + service.dx - settings.SERVICE_SIZE/2)
                        .attr("transform", "rotate("+coords.rotation+" "+midX+" "+midY+")");
                coords.x += coords.dx;
                coords.y+=coords.dy;
            });
        }

        /**@param {Edge} e**/
        function updateSvgChoordsEdge(e) {
            //getCenterOfMass will walk up the chain until it finds a visible element
            var src = e.leftPort.getFirstVisibleParent();
            var tgt = e.rightPort.getFirstVisibleParent();
            var srcChoords = src.getCenterOfMass();
            var tgtChoords = tgt.getCenterOfMass();
            var srcLead = e.svgLeadLeft;
            var tgtLead = e.svgLeadRight;

            //Without loss of generality, let src be on the left
            if (srcChoords.x > tgtChoords.x) {
                var tmp = src;
                src = tgt;
                tgt = tmp;

                tmp = srcChoords;
                srcChoords = tgtChoords;
                tgtChoords = tmp;

                tmp = srcLead;
                srcLead = tgtLead;
                tgtLead = tmp;
            }

            //if we are drawing to a port, we want a a horizontal line coming out of the port, before switching to the actual edge
            if (src.getType() === "Port") {
                srcChoords.x += settings.DIALOG_PORT_LEAD;
                srcLead.style("visibility", "visible")
                        .attr("x1", srcChoords.x)
                        .attr("y1", srcChoords.y)
                        .attr("x2", srcChoords.x - settings.DIALOG_PORT_LEAD)
                        .attr("y2", srcChoords.y);
            } else {
                srcLead.style("visibility", "hidden");
            }
            if (tgt.getType() === "Port") {
                tgtChoords.x -= settings.DIALOG_PORT_LEAD;
                tgtLead.style("visibility", "visible")
                        .attr("x1", tgtChoords.x)
                        .attr("y1", tgtChoords.y)
                        .attr("x2", tgtChoords.x + settings.DIALOG_PORT_LEAD)
                        .attr("y2", tgtChoords.y);
            } else {
                tgtLead.style("visibility", "hidden");
            }

            e.svgNode.attr("x1", srcChoords.x)
                    .attr("y1", srcChoords.y)
                    .attr("x2", tgtChoords.x)
                    .attr("y2", tgtChoords.y);
        }

        var lastMouse;
        /**@param {Node} n**/
        var isDragging = false;
        var didDrag = false;
        function makeDragBehaviour(n) {
            return d3.behavior.drag()
                    .on("drag", function () {
                        didDrag = true;
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


                        //As we drag, the cursor may enter and leave the bounding box of n
                        //In onNodeMouseLeave we make the hoverdiv stay visible in this case,
                        //However, we also want it to continue tracking us.
                        outputApi.setHoverLocation(e.clientX, e.clientY);
                        drawHighlight();
                        switchPopup.render();
                        //fix all edges
                        map_(edgeList, updateSvgChoordsEdge);
                    })
                    .on("dragstart", function () {
                        lastMouse = d3.event.sourceEvent;
                        outputApi.disablePanning();
                        isDragging = true;
                        didDrag = false;
                    })
                    .on("dragend", function () {
                        outputApi.enablePanning();
                        isDragging = false;
                    });
        }

        /**@param {Edge} e**/
        function drawEdge(e) {
            e.svgNode = svgContainer.select("#edge1").append("line")
                    .style("stroke", settings.EDGE_COLOR)
                    .style("stroke-width", settings.EDGE_WIDTH);
            e.svgLeadLeft = svgContainer.select("#edge2").append("line")
                    .style("stroke", settings.EDGE_COLOR)
                    .style("stroke-width", settings.EDGE_WIDTH)
                    .style("visibility", "hidden")
                    .attr("stroke-linecap", "round");
            e.svgLeadRight = svgContainer.select("#edge2").append("line")
                    .style("stroke", settings.EDGE_COLOR)
                    .style("stroke-width", settings.EDGE_WIDTH)
                    .style("visibility", "hidden")
                    .attr("stroke-linecap", "round");
            updateSvgChoordsEdge(e);
        }


        var highlightedNode = null;
        /**
         * Note that n could also be a topology
         * @param {Node} n**/
        function onNodeClick(n) {
            if (d3.event) {
                //In the case of artificial clicks, d3.event may be null
                d3.event.stopPropagation(); //prevent the click from being handled by the background, which would hide the panel
            }
            if (didDrag) {
                return;
            }
            highlightedNode = n;
            drawHighlight();
            outputApi.setDisplayName(n.getName());
            /**@type {DropDownTree} displayTree**/
            var displayTree = outputApi.getDisplayTree();
            displayTree.clear();
            n.populateTreeMenu(displayTree);
            displayTree.draw();
            n.portPopup.toggleVisible();
            drawPopups();
            map_(edgeList, updateSvgChoordsEdge);
            selectElement(n);
            selectedNode = n;
        }

        function onServiceClick(n) {
            selectedNode = n;
            d3.event.stopPropagation();
            if (didDrag) {
                return;
            }
            highlightedNode = n;
            drawHighlight();
            outputApi.setDisplayName(n.getName());
            var displayTree = outputApi.getDisplayTree();
            displayTree.clear();
            n.populateTreeMenu(displayTree);
            displayTree.draw();
            if (n.getTypeBrief() === "SwitchingService") {
                switchPopup.clear()
                        .setOffset(0, 0)
                        .setHostNode(n)
                        .render();
            }
        }

        var previousHighlight = null;
        function drawHighlight() {
            if (previousHighlight) {
                previousHighlight.remove();
                previousHighlight = null;
            }
            if (highlightedNode && highlightedNode.svgNode) {
                var toAppend = highlightedNode.svgNode.node().cloneNode();
                previousHighlight = d3.select(toAppend)
                        .style("filter", "url(#outline)")
                        .style("opacity", "1")
                        .attr("pointer-events", "none");
                var parentNode = highlightedNode.svgNode.node().parentNode;
                if (parentNode) {
                    //If we are coming out of a fold, the parentNode might no longer exist
                    parentNode.appendChild(toAppend);
                }
            } else if (highlightedNode) {
                //This shouldn't happen
                console.log("Trying to highlight an element without an svgNode");
            }
        }

        function selectElement(elem) {
            if (!elem) {
                //deselect element
                outputApi.setDisplayName("");
                outputApi.getDisplayTree().clear();
            } else {
                outputApi.setDisplayName(elem.getName());
                /**@type {DropDownTree} displayTree**/
                var displayTree = outputApi.getDisplayTree();
                displayTree.clear();
                elem.populateTreeMenu(displayTree);
                displayTree.draw();
            }
            highlightedNode = elem;
            drawHighlight();
            selectedNode = elem;
        }
        ;
        /**
         * Note that n could also be a topology
         * @param {Node} n**/
        function onNodeDblClick(n) {
            //We will never send a mouseleave event as the node is being removed
            outputApi.setHoverVisible(false);
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
                    enlarge = false; //Enlarging the topology does not look good
                    break;
                case "Service":
                    var size = settings.SERVICE_SIZE;
                    svg = n.svgNode;
                    x = n.x - settings.SERVICE_SIZE/2;
                    y = n.y - settings.SERVICE_SIZE/2;
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
            n.dx = -ds / 2;
            n.dy = -ds / 2;
            n.size = size;
            svg
                    .attr("width", size)
                    .attr("height", size)
                    .attr("x", x + n.dx)//make it appear to zoom into center of the icon
                    .attr("y", y + n.dy);

            if (n.svgNodeSubnetHighlight) {
                n.svgNodeSubnetHighlight
                        .attr("width", size)
                        .attr("height", size)
                        .attr("x", x + n.dx)
                        .attr("y", y + n.dy);
            }
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

        function buildPortDisplayPopup(n) {
            return new PortDisplayPopup(outputApi, API)
                    .setHostNode(n)
                    .setOffset(settings.DIALOG_OFFSET_X, settings.DIALOG_OFFSET_Y)
                    .setContainer(svgContainer)
                    .setDimensions(settings.DIALOG_MIN_WIDTH, settings.DIALOG_MIN_HEIGHT)
                    .setBevel(settings.DIALOG_BEVEL)
                    .setColor(settings.DIALOG_COLOR)
                    .setPortColors(settings.DIALOG_PORT_COLORS)
                    .setPortEmptyColor(settings.DIALOG_PORT_EMPTY_COLOR)
                    .setPortDimensions(settings.DIALOG_PORT_WIDTH, settings.DIALOG_PORT_HEIGHT)
                    .setPortBuffer(settings.DIALOG_PORT_BUFFER_VERT, settings.DIALOG_PORT_BUFFER_HORZ)
                    .setEnlargeFactor(settings.ENLARGE_FACTOR)
                    .setOpacity(settings.DIALOG_OPACITY);
        }

        function buildSwitchPopup() {
            return new SwitchPopup(outputApi)

                    .setContainer(svgContainer)
                    .setDimensions(switchSettings.DIALOG_MIN_WIDTH, switchSettings.DIALOG_MIN_HEIGHT)
                    .setTabDimensions(switchSettings.SWITCH_MIN_WIDTH, switchSettings.SWITCH_MIN_HEIGHT).setBevel(switchSettings.DIALOG_BEVEL)
                    .setColor(switchSettings.DIALOG_COLOR)
                    .setTabColor(switchSettings.DIALOG_TAB_COLOR)
                    .setPortColor(switchSettings.DIALOG_PORT_COLOR)
                    .setPortEmptyColor(switchSettings.DIALOG_PORT_EMPTY_COLOR)
                    .setPortDimensions(switchSettings.DIALOG_PORT_WIDTH, switchSettings.DIALOG_PORT_HEIGHT)
                    .setBuffer(switchSettings.DIALOG_BUFFER);
        }

        API["redraw"] = redraw;
        API["redrawPopups"] = drawPopups;
        API["doRender"] = doRender;
        API["drawHighlight"] = drawHighlight;
        API["selectElement"] = selectElement;
        API["layoutEdges"] = function () {
            map_(edgeList, updateSvgChoordsEdge);
        };
    }


    return{
        doRender: doRender,
        redraw: function () {
            redraw_();
        }
    };
});