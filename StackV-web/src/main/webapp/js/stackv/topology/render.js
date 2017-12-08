/*
 * Copyright (c) 2013-2016 University of Maryland
 * Modified by: Antonio Heard 2016

 * Permission is hereby granted, free of charge, to any person obtaining a copy 
 * of this software and/or hardware specification (the “Work”) to deal in the 
 * Work without restriction, including without limitation the rights to use, 
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of 
 * the Work, and to permit persons to whom the Work is furnished to do so, 
 * subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Work.

 * THE WORK IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL 
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
 * FROM, OUT OF OR IN CONNECTION WITH THE WORK OR THE USE OR OTHER DEALINGS  
 * IN THE WORK.
 */

"use strict";

//For debug purpuses only
var selectedNode;
var debugPoint = {x: 0, y: 0};
define([
    "local/d3",
    "local/stackv/utils",
    "local/stackv/topology/PortDisplayPopup",
    "local/stackv/topology/switchServicePopup",
    "local/stackv/topology/VolumeDisplayPopup",
    "local/stackv/topology/modelConstants"

], function (d3, utils, PortDisplayPopup, SwitchPopup, VolumeDisplayPopup, values) {

    var map_ = utils.map_;
    var isFirefox = utils.isFirefox;
    var getRenderedElementParentDiv = utils.getRenderedElementParentDiv;
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
        DIALOG_OFFSET_Y: -20,
        BASE_ITEM_OPACITY: .05
    };
    var volumeSettings = {
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
        DIALOG_COLOR: "rgb(94,209,98)",
        DIALOG_OPACITY: "0.7",
        DIALOG_PORT_EMPTY_COLOR: "rgb(128,128,0)",
        DIALOG_PORT_COLORS: ["rgb(64,64,64)", "rgb(128,128,128)"],
        DIALOG_PORT_HEIGHT: 6,
        DIALOG_PORT_WIDTH: 8,
        DIALOG_PORT_LEAD: 8,
        DIALOG_PORT_BUFFER_VERT: 2,
        DIALOG_PORT_BUFFER_HORZ: 3,
        DIALOG_OFFSET_X: -20,
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
        DIALOG_MIN_WIDTH: 10,
        DIALOG_MIN_HEIGHT: 10,
        SWITCH_MIN_WIDTH: 20,
        SWITCH_MIN_HEIGHT: 8,
        DIALOG_BEVEL: 10,
        DIALOG_COLOR: "rgb(255,0,0)",
        //This should be consistent with the subnet highlight color defined in graphText.html
        //Note that this value was found empircally from the browser, not by computing from the values in graphTest.html
        //  which gave a somewhat different result
        DIALOG_TAB_COLOR_SELECTED: "rgb(1, 231, 148)",
        DIALOG_TAB_COLOR: "rgb(31,178,223)",
        DIALOG_TAB_TEXT_SIZE: 12,
        DIALOG_BUFFER: 2,
        DIALOG_INNER_BUFFER: 2,
        DIALOG_PORT_COLOR: "rgb(0,0,0)",
        DIALOG_PORT_EMPTY_COLOR: "rgb(128,128,50)",
        DIALOG_PORT_HEIGHT: 4,
        DIALOG_PORT_WIDTH: 8

    };
    var redraw_;
    var API = {};
    var firstRun = true;
    var isDragging = false;
    var didDrag = false;
    var highlightedNode = null;
    var previousHighlight = null;
    
    var highlightedNodesMap = [];
    var prevHighlightedNodesMap = [];
    
    var trashcan = [];
    
    var lastMouse;
    var switchPopup = {};
    var topLevelTopologies = [];
    /**@param {outputApi} outputApi
     * @param {Model} model
     * @param (fullSize) boolean
     **/
    function doRender(outputApi, model, fullSize, modelMap, outputApiMap) {
        // default parameter
        var fullSize = typeof fullSize !== 'undefined' ?  fullSize : true;
        var modelMap = typeof modelMap !== 'undefined' ?  modelMap : null;
        var outputApiMap = typeof outputApiMap !== 'undefined' ?  outputApiMap : null;
        
        var svgContainer = outputApi.getSvgContainer();
        if (firstRun) {
            firstRun = false;
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
            switchSettings.DIALOG_INNER_BUFFER /= outputApi.getZoom();
            switchSettings.DIALOG_TAB_TEXT_SIZE /= outputApi.getZoom();
            
            if (fullSize) {
                svgContainer.on("click", function () {
                    //Clear the selected element.
                    //We check the event path so this only happens if we did not actually click on something
                    var clickedElem = d3.event.path[0];
                    if (clickedElem.id === outputApi.svgContainerName) {
                        selectElement(null);
                    }
                });
                selectElement(null); // show top level topologies in display panel 
            }
        } 
        
        if (!switchPopup[outputApi.svgContainerName] || !fullSize) {
            switchPopup[outputApi.svgContainerName] = buildSwitchPopup();
        }
        redraw();
        var nodeList, edgeList, policyList;
        function redraw(currentOutputApi) {
            if (!fullSize && currentOutputApi) {
                var svgc = d3.select("#" + currentOutputApi.svgContainerName);
                var svgcn = currentOutputApi.svgContainerName;
                var currentModel = modelMap[currentOutputApi.svgContainerName];
                var nList = currentModel.listNodes();
                var pList = currentModel.listPolicies();
                var eList = currentModel.listEdges();
            } else {
                var svgc = svgContainer;
                var svgcn = outputApi.svgContainerName;
                var currentModel = model;
                var nList = currentModel.listNodes();
                var pList = currentModel.listPolicies();
                var eList = currentModel.listEdges();                
                
                nodeList = nList;
                policyList = pList;
                edgeList = eList;
            }
            svgc.select("#topology" + "_" + svgcn).selectAll("*").remove(); //Clear the previous drawing
            svgc.select("#edge1" + "_" + svgcn).selectAll("*").remove(); //Clear the previous drawing
            svgc.select("#edge2" + "_" + svgcn).selectAll("*").remove(); //Clear the previous drawing
            svgc.select("#node" + "_" + svgcn).selectAll("*").remove(); //Clear the previous drawing
            svgc.select("#anchor" + "_" + svgcn).selectAll("*").remove(); //Clear the previous drawing
            svgc.select("#parentPort" + "_" + svgcn).selectAll("*").remove();
            svgc.select("#switchPopup" + "_" + svgcn).selectAll("*").remove();
            svgc.select("#volume" + "_" + svgcn).selectAll("*").remove();
                        
            var polEdges = currentModel.policyEdges;
            for (var i in polEdges) edgeList.push(polEdges[i]);

            //Recall that topologies are also considered nodes
            //We render them seperatly to enfore a z-ordering
            map_(nList, drawTopology);
            map_(nList, drawNode);
            map_(pList, drawPolicy);
            if (!fullSize && currentOutputApi) {
                drawPopups(currentOutputApi);
            } else {
                drawPopups();
            }
            map_(eList, drawEdge);
        }
        redraw_ = redraw;
        /**@param {Node} n**/
        function drawNode(n) {

                if (n.isLeaf()) {
                    if (!n.portPopup) {
                        n.portPopup = buildPortDisplayPopup(n);
                    }
                    if (!n.volumePopup) {
                        n.volumePopup = buildVolumeDisplayPopup(n);
                    }
                    n.svgNode = svgContainer.select("#node" + "_" + outputApi.svgContainerName).append("image")
                            .attr("xlink:href", n.getIconPath())
                            .on("click", onNodeClick.bind(undefined, n))
                            .on("dblclick", onNodeDblClick.bind(undefined, n))
                            .on("mousemove", onNodeMouseMove.bind(undefined, n))
                            .on("mouseleave", onNodeMouseLeave.bind(undefined, n));                    
                    if (outputApi.contextMenu) {
                        n.svgNode.on("contextmenu", outputApi.contextMenu.renderedElemContextListener.bind(undefined, n));    
                    } 
                    if (n.detailsReference) {
                        n.svgNode.style("opacity", .4);
                    }
                    n.svgNode.call(makeDragBehaviour(n));
                    
                    setElementSize(n, false);
                    drawServices(n);
                    updateSvgChoordsNode(n);
                }
        }
        
        function drawPolicy(p) {
            p.svgNode = svgContainer.select("#node" + "_" + outputApi.svgContainerName).append("image")
                    .attr("xlink:href", p.getIconPath())
                    .on("click", onPolicyClick.bind(undefined, p))
                    .on("dblclick", onPolicyDblClick.bind(undefined, p))
                    .on("mousemove", onPolicyMouseMove.bind(undefined, p))
                    .on("mouseleave", onPolicyMouseLeave.bind(undefined, p));      
          
          var shortName = p.getName().split(":");
          shortName = shortName[shortName.length-1];
            
         p.nameText = svgContainer.select("#node" + "_" + outputApi.svgContainerName).append("text")
                .attr("dx", p.x)
                .attr("dy", p.y+(screen.height*.03)) // reposition
                .text(shortName  )
                .attr("font-family", "sans-serif")
                .attr("font-size", "15px")
        	.attr("text-overflow", "ellipsis")
        	.style("text-anchor", "middle");



            if (outputApi.contextMenu) {
                p.svgNode.on("contextmenu", outputApi.contextMenu.renderedElemContextListener.bind(undefined, p));    
            } 
            if (p.detailsReference) {
                p.svgNode.style("opacity", .4);
            }
            p.svgNode.call(makeDragBehaviour(p));

            setElementSize(p, false);
            //drawServices(n);
            updateSvgChoordsPolicy(p);           
        }
        
        /**@param {Node} n**/
        function drawTopology(n) {

            if (!n.isLeaf()) {
                if (!n.portPopup) {
                    n.portPopup = buildPortDisplayPopup(n);
                }
                if (!n.volumePopup) {
                    n.volumePopup = buildVolumeDisplayPopup(n);
                }

                //render the convex hull surounding the decendents of n
                var path = getTopolgyPath(n);
                var color = settings.HULL_COLORS[n.getDepth() % settings.HULL_COLORS.length];
                
                 n.svgNode = svgContainer.select("#topology" + "_" + outputApi.svgContainerName).append("path")
                    .style("fill", color)
                    .style("stroke", color)
                    .style("stroke-width", settings.TOPOLOGY_SIZE + settings.TOPOLOGY_BUFFER * n.getHeight())
                    .style("stroke-linejoin", "round")
                    .style("opacity", settings.HULL_OPACITY)
                    .attr("d", topologyPathToString(path))
                    .on("click", onNodeClick.bind(undefined, n))
                    .on("dblclick", onNodeDblClick.bind(undefined, n))
                    .on("mousemove", onNodeMouseMove.bind(undefined, n))
                    .on("mouseleave", onNodeMouseLeave.bind(undefined, n));
                if (outputApi.contextMenu) {
                        n.svgNode.on("contextmenu", outputApi.contextMenu.renderedElemContextListener.bind(undefined, n));    
                }
                n.svgNode.call(makeDragBehaviour(n));
                
                n.svgNodeAnchor = svgContainer.select("#anchor" + "_" + outputApi.svgContainerName).append("rect")
                        .style("fill", "white")
                        .style("stroke", "black")
                        .style("stroke-width", settings.TOPOLOGY_ANCHOR_STROKE)
                        .on("click", onNodeClick.bind(undefined, n))
                        .on("dblclick", onNodeDblClick.bind(undefined, n))
                        .on("mousemove", onNodeMouseMove.bind(undefined, n))
                        .on("mouseleave", onNodeMouseLeave.bind(undefined, n));  
                if (outputApi.contextMenu) {
                    n.svgNodeAnchor.on("contextmenu", outputApi.contextMenu.renderedElemContextListener.bind(undefined, n));                
                }
                if (n.detailsReference) {
                    n.svgNode.style("opacity", .4);
                }
                
                n.svgNodeAnchor.call(makeDragBehaviour(n));
                
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
            n.svgNodeServices = svgContainer.select("#node" + "_" + outputApi.svgContainerName).append("g");
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
                        .on("mouseleave", onNodeMouseLeave.bind(undefined, service));

                if (outputApi.contextMenu) {
                    service.svgNode.on("contextmenu", outputApi.contextMenu.renderedElemContextListener.bind(undefined, service));
                }    
                if (service.detailsReference) {
                    service.svgNode.style("opacity", .4);
                }
                
                service.svgNode.call(makeDragBehaviour(n));

                setElementSize(service, false);
            });
            updateSvgChoordsService(n);
        }

        function drawPopups(currentOutputApi) {
            if (!fullSize && currentOutputApi) {
                var svgc = d3.select("#" + currentOutputApi.svgContainerName);
                var svgcn = currentOutputApi.svgContainerName;
                var currentModel = modelMap[currentOutputApi.svgContainerName];
                var nList  = currentModel.listNodes();
            } else {
                var svgc = svgContainer;
                var svgcn = outputApi.svgContainerName;
                var currentModel = model;
                var nList = nodeList;
            }
            svgc.select("#dialogBox" + "_" + svgcn).selectAll("*").remove();
            svgc.select("#port" + "_" + svgcn).selectAll("*").remove();
            svgc.select("#parentPort" + "_" + svgcn).selectAll("*").remove();

            svgc.select("#volumeDialogBox" + "_" + svgcn).selectAll("*").remove();
            svgc.select("#volume" + "_" + svgcn).selectAll("*").remove();
    
            map_(nList, function (n) {
                n.portPopup.render();
                n.volumePopup.render();
            });
            switchPopup[svgcn].render();
        }

        /**@param {Node} n**/
        function computeServiceCoords(n) {
            var ans = {x: null, y: null, dx: null, dy: null, rotation: null};
            if (n.isLeaf()) {
                ans.x = n.x - settings.NODE_SIZE / 2 + settings.SERVICE_SIZE / 2;
                ans.y = n.y + settings.NODE_SIZE / 2 + settings.SERVICE_SIZE / 2;
                ans.dx = settings.SERVICE_SIZE;
                ans.dy = 0;
                ans.rotation = 0;

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
                    var normalOffset = settings.TOPOLOGY_SIZE / 2 + settings.TOPOLOGY_BUFFER / 2 * (n.getHeight()) + settings.SERVICE_SIZE / 2;
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

                    ans.x = path[0].x - settings.SERVICE_SIZE * n.services.length / 2 + settings.SERVICE_SIZE / 2;
                    ans.y = path[0].y - settings.TOPOLOGY_SIZE / 2 - settings.TOPOLOGY_BUFFER / 2 * (n.getHeight()) - settings.SERVICE_SIZE / 2;
                    ans.dx = settings.SERVICE_SIZE;
                    ans.dy = 0;
                    ans.rotation = 0;

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

        function updateSvgChoordsPolicy(n) {
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
            
            n.nameText.attr("dx", n.x);
            n.nameText.attr("dy", n.y + screen.height*.03);

            var svgAchor = n.svgNodeAnchor;
            if (svgAchor) {
                var choords = n.getCenterOfMass();
                svgAchor.attr("x", choords.x - settings.TOPOLOGY_ANCHOR_SIZE / 2);
                svgAchor.attr("y", choords.y - settings.TOPOLOGY_ANCHOR_SIZE / 2);
            }
            updateSvgChoordsService(n);
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
            n.volumePopup.updateSvgChoords();
        }

        function updateSvgChoordsService(n) {
            var coords = computeServiceCoords(n);
            map_(n.services, function (service) {
                service.y = coords.y;
                service.x = coords.x;
                var midY = coords.y + service.dy - settings.SERVICE_SIZE / 2 + service.size / 2;
                var midX = coords.x + service.dx - settings.SERVICE_SIZE / 2 + service.size / 2;
                service.svgNode
                        .attr("y", coords.y + service.dy - settings.SERVICE_SIZE / 2)
                        .attr("x", coords.x + service.dx - settings.SERVICE_SIZE / 2)
                        .attr("transform", "rotate(" + coords.rotation + " " + midX + " " + midY + ")");
                coords.x += coords.dx;
                coords.y += coords.dy;
            });
        }

        /**@param {Edge} e**/
        function updateSvgChoordsEdge(e) {
            //getCenterOfMass will walk up the chain until it finds a visible element
            if (!e.leftPort || !e.rightPort) {
                return;
            }
            
            var src = e.leftPort.getFirstVisibleParent();
            var tgt = e.rightPort.getFirstVisibleParent();
            var srcChoords = src.getCenterOfMass();
            var tgtChoords = tgt.getCenterOfMass();
            var srcLead = e.svgLeadLeft;
            var tgtLead = e.svgLeadRight;

            //Without loss of generality, let src be on the left
            if (e.edgeType === null) {                
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
            
            if (e.edgeType !== null) {                
                e.svgNode.attr("x1", srcChoords.x)
                        .attr("y1", srcChoords.y)
                        .attr("x2", tgtChoords.x  )
                        .attr("y2", tgtChoords.y );
            } else {
                e.svgNode.attr("x1", srcChoords.x)
                        .attr("y1", srcChoords.y)
                        .attr("x2", tgtChoords.x)
                        .attr("y2", tgtChoords.y);                
            }
        }




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
                            if (n.getType() === 'Policy') {
                                updateSvgChoordsPolicy(cursor);
                            } else { 
                                updateSvgChoordsNode(cursor);
                            }
                            cursor = cursor._parent;
                        }


                        //As we drag, the cursor may enter and leave the bounding box of n
                        //In onNodeMouseLeave we make the hoverdiv stay visible in this case,
                        //However, we also want it to continue tracking us.
                        outputApi.setHoverLocation(e.clientX, e.clientY);
                        drawHighlight();
                        highlightElements("serviceHighlighting");
                        highlightElements("trashcan");
                        switchPopup[outputApi.svgContainerName].render();
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
            e.svgNode = svgContainer.select("#edge1" + "_" + outputApi.svgContainerName).append("line")
                    .style("stroke", settings.EDGE_COLOR)
                    .style("stroke-width", settings.EDGE_WIDTH);
            
            if (e.edgeType === "dependOn") {
                d3.select(e.svgNode.node())
                        .style("stroke", settings.EDGE_COLOR)
                        .style("stroke-width", settings.EDGE_WIDTH)
                        .style("opacity", "1")
                        .attr("marker-end", "url(#marker_arrow_" + outputApi.svgContainerName + ")")
                        .on("mousemove", onPolicyMouseMove.bind(undefined, e))
                        .on("mouseleave", onPolicyMouseLeave.bind(undefined, e));   
                if (isFirefox()) {
                    d3.select(e.svgNode.node()).style("filter", "url(#spaDependOnOutlineFF)");
                } else {
                    d3.select(e.svgNode.node()).style("filter", "url(#spaDependOnOutline)");
                }
            } else if (e.edgeType === "importFrom") {
                 d3.select(e.svgNode.node())
                    .style("stroke", settings.EDGE_COLOR)
                    .style("stroke-width", settings.EDGE_WIDTH)                
                    .style("opacity", "1")
                    .attr("marker-end", "url(#marker_arrow_" + outputApi.svgContainerName + ")")                
                    .on("mousemove", onPolicyMouseMove.bind(undefined, e))
                    .on("mouseleave", onPolicyMouseLeave.bind(undefined, e));   
                if (isFirefox()) {
                    d3.select(e.svgNode.node()).style("filter", "url(#spaImportFromOutlineFF)");
                } else {
                    d3.select(e.svgNode.node()).style("filter", "url(#spaImportFromOutline)");
                }
            } else if (e.edgeType === "exportTo") {
                d3.select(e.svgNode.node())
                        .style("stroke", settings.EDGE_COLOR)
                        .style("stroke-width", settings.EDGE_WIDTH)                
                        .style("opacity", "1")
                        .attr("marker-end", "url(#marker_arrow_" + outputApi.svgContainerName + ")")                
                        .on("mousemove", onPolicyMouseMove.bind(undefined, e))
                        .on("mouseleave", onPolicyMouseLeave.bind(undefined, e));
                if (isFirefox()) {
                    d3.select(e.svgNode.node()).style("filter", "url(#spaExportToOutlineFF)");
                } else {
                    d3.select(e.svgNode.node()).style("filter", "url(#spaExportToOutline)");
                }
            } else if (e.edgeType !== null) {
                d3.select(e.svgNode.node())
                        .style("stroke", settings.EDGE_COLOR)
                        .style("stroke-width", settings.EDGE_WIDTH)                
                        .style("opacity", "1")
                        .attr("marker-end", "url(#marker_arrow_" + outputApi.svgContainerName + ")")                
                        .on("mousemove", onPolicyMouseMove.bind(undefined, e))
                        .on("mouseleave", onPolicyMouseLeave.bind(undefined, e));
            }
            
            e.svgLeadLeft = svgContainer.select("#edge2" + "_" + outputApi.svgContainerName).append("line")
                    .style("stroke", settings.EDGE_COLOR)
                    .style("stroke-width", settings.EDGE_WIDTH)
                    .style("visibility", "hidden")
                    .attr("stroke-linecap", "round");
            e.svgLeadRight = svgContainer.select("#edge2" + "_" + outputApi.svgContainerName).append("line")
                    .style("stroke", settings.EDGE_COLOR)
                    .style("stroke-width", settings.EDGE_WIDTH)
                    .style("visibility", "hidden")
                    .attr("stroke-linecap", "round");
            updateSvgChoordsEdge(e);
        }

        
        function onPolicyClick(n, currentOutputApi) {
            if (!fullSize && currentOutputApi) {
              var o = currentOutputApi;  
              var m = modelMap[o.svgContainerName];
            } else if (!fullSize) { 
              var divname = getRenderedElementParentDiv(n);
              var o = outputApiMap[divname];
              var m = modelMap[o.svgContainerName];
            } else{ 
              var o = outputApi;
              var m = model;
            }
             if (d3.event) {
                //In the case of artificial clicks, d3.event may be null
                d3.event.stopPropagation(); //prevent the click from being handled by the background, which would hide the panel
            }
            if (didDrag) {
                return;
            }
            //debug
            if (d3.event && d3.event.altKey) {
                n.isGhost = !n.isGhost;
                n.svgNode.style("filter", n.isGhost ? "url(#ghost)" : "none");
                return;
            }
            highlightedNode = n;
            drawHighlight();
            if (o.getDisplayTree()) {
                o.setDisplayName(n.getName());
                /**@type {DropDownTree} displayTree**/
                var displayTree = o.getDisplayTree();
                displayTree.clear();
                var e = m.elementMap[n.getName()];
                e.populateProperties(displayTree);
            
                if (e.misc_elements.length > 0 )
                    displayTree.addChild("", "Separator", null);

                e.populateTreeMenu(displayTree);
                displayTree.addToHistory(e.getName(), "Policy");
                displayTree.draw();
                displayTree.topViewShown = false;    
                if (fullSize) {
                    displayTree.open();
                }
            }
            highlightElements("serviceHighlighting"); // show stuff that was highlighting when it has an svg node 

            map_(edgeList, updateSvgChoordsEdge);
            selectElement(n);
            selectedNode = n;           
        }
        
        /**
         * Note that n could also be a topology
         * @param {Node} n**/
        function onPolicyDblClick(n) {
            var policyType = n.getTypeDetailed();
            if (policyType === "PolicyData") {
                $("#dialog_policyData").text("");
                
                if (n.data !== undefined && n.data !== null){
                    $("#dialog_policyData").append("<pre class=\"jSonDialog\">" + n.data  + "</pre>");
                }else{ 
                    $("#dialog_policyData").append("<pre class=\"jSonDialog\">N/A</pre>");
                }
                $("#dialog_policyData").dialog("open");
                $('.ui-dialog :button').blur();
            } else if (policyType === "PolicyAction") {
                $("#dialog_policyAction").text("");
                $("#dialog_policyAction").append(n.data);
                $("#dialog_policyAction").dialog("open");
                $('.ui-dialog :button').blur();
            }
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
        function onPolicyMouseMove(n) {
            //As we drag a node, the cursor may temporarliy leave the bounding box
            //of said node, causing flicker of the hoverdiv
            if (!isDragging) {
                if  (n.edgeType !== undefined && n.edgeType !== null) {
                    outputApi.setHoverText(n.edgeType);                    
                } else {    
                    outputApi.setHoverText(n.getName());
                    setElementSize(n, true);

                }
                outputApi.setHoverLocation(d3.event.clientX, d3.event.clientY);
                outputApi.setHoverVisible(true);
            }
        }

        function onPolicyMouseLeave(n) {
            //As we drag a node, the cursor may temporarliy leave the bounding box
            //of said node, causing flicker of the hoverdiv
            if (!isDragging) {
                outputApi.setHoverVisible(false);
                  if  (n.edgeType === null) {
                     setElementSize(n, false);
                 }
            }
        }
        
        /**
         * Note that n could also be a topology
         * @param {Node} n**/
        function onNodeClick(n, currentOutputApi) {
            if (!fullSize && currentOutputApi) {
              var o = currentOutputApi;  
              var m = modelMap[o.svgContainerName];
            } else if (!fullSize) { 
              var divname = getRenderedElementParentDiv(n);
              var o = outputApiMap[divname];
              var m = modelMap[o.svgContainerName];
            } else{ 
              var o = outputApi;
              var m = model;
            }
            if (d3.event) {
                //In the case of artificial clicks, d3.event may be null
                d3.event.stopPropagation(); //prevent the click from being handled by the background, which would hide the panel
            }
            if (didDrag) {
                return;
            }
            //debug
            if (d3.event && d3.event.altKey) {
                n.isGhost = !n.isGhost;
                n.svgNode.style("filter", n.isGhost ? "url(#ghost)" : "none");
                return;
            }
            if (d3.event && d3.event.shiftKey) {
                if (highlightedNodesMap["trashcan"] === undefined) {
                    highlightedNodesMap["trashcan"] = [];
                }
                
                highlightedNodesMap["trashcan"].push(n);
            }
            highlightedNode = n;
            drawHighlight();
            if (o.getDisplayTree()) {
                o.setDisplayName(n.getName());
                /**@type {DropDownTree} displayTree**/
                var displayTree = o.getDisplayTree();
                displayTree.clear();
                var e = m.elementMap[n.getName()];
                e.populateProperties(displayTree);
            
                if (e.misc_elements.length > 0 )
                    displayTree.addChild("", "Separator", null);

                e.populateTreeMenu(displayTree);
                displayTree.addToHistory(e.getName(), "Node");
                displayTree.draw();
                displayTree.topViewShown = false;    
                if (fullSize) {
                    displayTree.open();
                }                
            }
            // Only show these popups if there are acutally ports and volumes 
            if (n.ports.length !== 0) 
                n.portPopup.toggleVisible();
            if (n.volumes.length !== 0)
                 n.volumePopup.toggleVisible();
            drawPopups();
            highlightElements("serviceHighlighting"); // show stuff that was highlighting when it has an svg node 
            highlightElements("trashcan");
            map_(edgeList, updateSvgChoordsEdge);
            selectElement(n);
            selectedNode = n;
        }

        function onServiceClick(n, currentOutputApi) {
            if (!fullSize && currentOutputApi) {
              var o = currentOutputApi;  
              var m = modelMap[o.svgContainerName];
            } else if (!fullSize) { 
              var divname = getRenderedElementParentDiv(n);
              var o = outputApiMap[divname];
              var m = modelMap[o.svgContainerName];
            } else{ 
              var o = outputApi;
              var m = model;
            }
            selectedNode = n;
            if (d3.event) {
                //In the case of artificial clicks, d3.event may be null
                d3.event.stopPropagation(); //prevent the click from being handled by the background, which would hide the panel
            }
            if (didDrag) {
                return;
            }
            //debug
            if (d3.event && d3.event.altKey) {
                n.isGhost = !n.isGhost;
                n.svgNode.style("filter", n.isGhost ? "url(#ghost)" : "none");
                return;
            }
            if (d3.event && d3.event.shiftKey) {
                if (highlightedNodesMap["trashcan"] === undefined) {
                    highlightedNodesMap["trashcan"] = [];
                }
                
                highlightedNodesMap["trashcan"].push(n);
            }
            highlightedNode = n;
            drawHighlight();
            if (o.getDisplayTree()) {
                o.setDisplayName(n.getName());
                var displayTree = o.getDisplayTree();
                displayTree.clear();
                var e = m.elementMap[n.getName()];
                e.populateProperties(displayTree);

                if (e.misc_elements.length > 0 )
                 displayTree.addChild("", "Separator", null);

                e.populateTreeMenu(displayTree);
                displayTree.addToHistory(e.getName(), "Service");
                displayTree.draw();
                displayTree.topViewShown = false;
                if (fullSize) {
                    displayTree.open();
                }                
            }
            if (n.getTypeBrief() === "SwitchingService") {

                if (switchPopup[o.svgContainerName].hostNode === n) {
                    switchPopup[o.svgContainerName].clear();
                } else {
                    switchPopup[o.svgContainerName].clear()
                            .setOffset(settings.DIALOG_OFFSET_X, -settings.DIALOG_OFFSET_Y)
                            .setHostNode(n)
                            .render();
                }

            }
        }
        
        // generalize these 
        function setHighlights(x, feature){          
            highlightedNodesMap[feature] = x;
        }
        
        function removeHighlights(feature) {
            highlightedNodesMap[feature] = [];
            prevHighlightedNodesMap[feature] = [];
        }
        
        function highlightElements(feature){
            if (fullSize) {
                if (prevHighlightedNodesMap[feature] !== []) {

                    for (var i in prevHighlightedNodesMap[feature] ) {
                        prevHighlightedNodesMap[feature] [i].remove();
                    }
                    prevHighlightedNodesMap[feature]  = [];
                }

                for (var i in highlightedNodesMap[feature] ) {
                    var type = highlightedNodesMap[feature][i].getType();

                    if (highlightedNodesMap[feature][i] && highlightedNodesMap[feature][i].svgNode) {

                        var toAppend = highlightedNodesMap[feature][i].svgNode.node().cloneNode();
                        prevHighlightedNodesMap[feature].push(d3.select(toAppend)
                                    .style("opacity", "1")
                                    .attr("pointer-events", "none"));
                            
                        var last = prevHighlightedNodesMap[feature].length - 1;
                        if (feature === "trashcan") {
                             prevHighlightedNodesMap[feature][last].style("filter", "url(#trashcanHighlight)"); 
                        } else {
                            if (!isFirefox()) {
                                prevHighlightedNodesMap[feature][last].style("filter", "url(#subnetHighlight)"); 
                            } else {
                                prevHighlightedNodesMap[feature][last].style("filter", "url(#subnetHighlightFF)");
                            }
                        }
                        var parentNode = highlightedNodesMap[feature][i].svgNode.node().parentNode;
                        if (parentNode) {
                            //If we are coming out of a fold, the parentNode might no longer exist
                            parentNode.appendChild(toAppend);
                        }
                    }                
                }           
                map_(edgeList, updateSvgChoordsEdge);         
            }
        }
        
        function drawHighlight() {
            if (previousHighlight) {
                previousHighlight.remove();
                previousHighlight = null;
            }
            if (highlightedNode && highlightedNode.svgNode) {
                var toAppend = highlightedNode.svgNode.node().cloneNode();
                previousHighlight = d3.select(toAppend)
                            .style("opacity", "1")
                            .attr("pointer-events", "none");                    

                if (!isFirefox()) {
                    previousHighlight.style("filter", "url(#outline)");
                } else {
                    previousHighlight.style("filter", "url(#outlineFF)");
                }
                                            
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
        
        function selectElement(elem, currentOutputApi) {
            if (!fullSize && currentOutputApi) {
              var o = currentOutputApi;
              var m = modelMap[o.svgContainerName];  
            } else {
              var m = model;
              var o = outputApi;
            }
            
            if (outputApi.getDisplayTree()) {
                if (!elem) {
                    //deselect element
                    o.setDisplayName("Topologies");
                    var displayTree = o.getDisplayTree();
                    o.getDisplayTree().clear();
                    
                    if (topLevelTopologies.length === 0) {
                        for (var key in m.elementMap) {
                             var e = m.elementMap[key];
                             if (e.getType() === "Topology" && e.topLevel) {
                                 topLevelTopologies.push(e);                                
                                 var child = displayTree.addChild(e.getName(), "Element", e);
                                 e.populateTreeMenu(child);
                             }
                        }
                        // if still no top level topoligies  
                        if (topLevelTopologies.length === 0) {
                            displayTree.addChild("No top level topologies.", "Title", "No top level topologies.");
                        }
                    } else {
                        for (var i in topLevelTopologies) {
                            var topology = topLevelTopologies[i];
                            var child = displayTree.addChild(topology.getName(), "Element", e);
                            topology.populateTreeMenu(child);                        
                        }
                    }
                    
                    displayTree.draw();
                } else {
                    o.setDisplayName(elem.getName());
                    /**@type {DropDownTree} displayTree**/
                    var displayTree = o.getDisplayTree();
                    displayTree.clear();
                    var e = m.elementMap[elem.getName()];
                    e.populateProperties(displayTree);

                    if (e.misc_elements.length > 0 )
                        displayTree.addChild("", "Separator", null);

                    e.populateTreeMenu(displayTree);
                    e.showRelationships(displayTree);
                    displayTree.draw();
                    if (fullSize) {
                        displayTree.open();
                    }                

                }
            }
            highlightedNode = elem;
            drawHighlight();
            selectedNode = elem;
        }
        
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
                outputApi.setHoverLocation(d3.event.clientX, d3.event.clientY);
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
                case "Policy":
                    var size = settings.NODE_SIZE;
                    svg = n.svgNode;
                    x = n.x - settings.NODE_SIZE / 2;
                    y = n.y - settings.NODE_SIZE / 2;
                    //if (isNaN(n)) console.log("NAN N");                    
                    if (isNaN(settings.NODE_SIZE)) alert("NAN SETTINGS");
                    if (isNaN(x) || isNaN(y)) alert("NAN NODE" + "\n" + settings.NODE_SIZE
                            + "\nx: " + n.x + "\ny: " + n.y);
                    break;
                case "Topology":
                    var size = settings.TOPOLOGY_ANCHOR_SIZE;
                    svg = n.svgNodeAnchor;
                    var choords = n.getCenterOfMass();
                    x = choords.x - settings.TOPOLOGY_ANCHOR_SIZE / 2;
                    y = choords.y - settings.TOPOLOGY_ANCHOR_SIZE / 2;
                    enlarge = false; //Enlarging the topology does not look good
                    if (isNaN(x) || isNaN(y)) alert("NAN TOPO" + "\n" + settings.TOPOLOGY_ANCHOR_SIZE
                            + "\nx: " + choords.x + "\ny: " + choords.y);
                    break;
                case "Service":
                    var size = settings.SERVICE_SIZE;
                    svg = n.svgNode;
                    x = n.x - settings.SERVICE_SIZE / 2;
                    y = n.y - settings.SERVICE_SIZE / 2;
                    if (isNaN(x) || isNaN(y)) alert("NAN SERVICE" + "\n" + settings.SERVICE_SIZE
                            + "\nx: " + n.x + "\ny: " + n.y);
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
            
            if (n.getType() === 'Policy') {
                updateSvgChoordsPolicy(n);
            } else { 
                updateSvgChoordsNode(n);
            }

        }
        
        function buildVolumeDisplayPopup(n) {
             return new VolumeDisplayPopup(outputApi, API)
                    .setHostNode(n)
                    .setOffset(volumeSettings.DIALOG_OFFSET_X, volumeSettings.DIALOG_OFFSET_Y)
                    .setContainer(svgContainer)
                    .setDimensions(volumeSettings.DIALOG_MIN_WIDTH, volumeSettings.DIALOG_MIN_HEIGHT)
                    .setBevel(volumeSettings.DIALOG_BEVEL)
                    .setColor(volumeSettings.DIALOG_COLOR)
                    .setVolumeDimensions(volumeSettings.DIALOG_PORT_WIDTH, volumeSettings.DIALOG_PORT_HEIGHT)
                    .setVolumeBuffer(volumeSettings.DIALOG_PORT_BUFFER_VERT, volumeSettings.DIALOG_PORT_BUFFER_HORZ)
                    .setEnlargeFactor(volumeSettings.ENLARGE_FACTOR)
                    .setOpacity(volumeSettings.DIALOG_OPACITY);           
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
                    .setTabDimensions(switchSettings.SWITCH_MIN_WIDTH, switchSettings.SWITCH_MIN_HEIGHT)
                    .setBevel(switchSettings.DIALOG_BEVEL)
                    .setColor(switchSettings.DIALOG_COLOR)
                    .setTabColor(switchSettings.DIALOG_TAB_COLOR)
                    .setTabColorSelected(switchSettings.DIALOG_TAB_COLOR_SELECTED)
                    .setPortColor(switchSettings.DIALOG_PORT_COLOR)
                    .setPortEmptyColor(switchSettings.DIALOG_PORT_EMPTY_COLOR)
                    .setPortDimensions(switchSettings.DIALOG_PORT_WIDTH, switchSettings.DIALOG_PORT_HEIGHT)
                    .setBuffer(switchSettings.DIALOG_BUFFER)
                    .setInnerBuffer(switchSettings.DIALOG_INNER_BUFFER)
                    .setTextSize(switchSettings.DIALOG_TAB_TEXT_SIZE);
        }
        function clickNode(name, type, currentOutputApi) {
            if (!fullSize && currentOutputApi) {
              var o = currentOutputApi;  
              var m = modelMap[o.svgContainerName];
            } else {
              var o = outputApi;
              var m = model;
            }
            
           var element = m.elementMap[name];
           if (element === undefined) {
               if (fullSize) {
                   alert("Element not found. Please enter valid URN.");
               }
           } else {
                type = element.getType();
                
                switch (type) {
                 case "Topology":
                 case "Node":
                     onNodeClick(m.nodeMap[name], o);
                     if (o.getDisplayTree()) {
                        o.getDisplayTree().addToHistory(name, type);
                        o.getDisplayTree().topViewShown = false;
                     }
                     console.log("i'm node: " + model.nodeMap[name].getName());

                     break;
                 case "SwitchingService":
                 case "HypervisorService":
                 case "RoutingService":
                 case "VirtualCloudService":
                 case "BlockStorageService":
                 case "ObjectStorageService":
                 case "VirtualSwitchService":
                 case "HypervisorBypassInterfaceService":
                 case "StorageService":
                 case "IOPerformanceMeasurementService":
                 case "DataTransferService":
                 case "DataTransferClusterService":
                 case "NetworkObject":
                 case "Service":
                     if (o.svgContainerName.indexOf("servr") < 0) {
                        onServiceClick(m.serviceMap[name], o);
                     } else {
                        onNodeClick(m.nodeMap[name], o);
                     }                 
                     if (o.getDisplayTree()) {
                        o.getDisplayTree().addToHistory(name, type);
                        o.getDisplayTree().topViewShown = false;
                     }
                     console.log("i'm service");
                     break;
                 case "Port":
                 case "BidirectionalPort":
                     if (o.svgContainerName.indexOf("servr") < 0) {
                        selectElement(m.portMap[name], o);
                     } else {
                        selectElement(m.nodeMap[name], o);
                     }
                     if (o.getDisplayTree()) {
                        o.getDisplayTree().addToHistory(name, type);  
                        o.getDisplayTree().topViewShown = false;
                     }
                     console.log("i'm port");
                     break;
                 case "Volume":
                     if (o.svgContainerName.indexOf("servr") < 0) {
                        selectElement(m.volumeMap[name], o);   
                     } else {
                        selectElement(m.nodeMap[name], o);
                     }
                     if (o.getDisplayTree()) {
                        o.getDisplayTree().addToHistory(name, type);   
                        o.getDisplayTree().topViewShown = false;
                     }
                     console.log("i'm volume");
                     break;
                 case "PolicyData":
                 case "PolicyAction":
                     selectElement(m.policyMap[name], o);    
                     if (o.getDisplayTree()) {
                        o.getDisplayTree().addToHistory(name, type);   
                        o.getDisplayTree().topViewShown = false;
                     }                    
                    break;
                 default:
                     selectElement(m.elementMap[name], o);                    
                     if (o.getDisplayTree()) {
                        o.getDisplayTree().addToHistory(name, type);
                        o.getDisplayTree().topViewShown = false;
                     }
                     console.log("I'm element");
                     break;
               }
           }
        }
        

        API["redraw"] = redraw;
        API["redrawPopups"] = drawPopups;
        API["doRender"] = doRender;
        API["drawHighlight"] = drawHighlight;
        API["selectElement"] = selectElement;
        API["layoutEdges"] = function (o) {
            if (!fullSize) {
              var e = modelMap[o.svgContainerName].listEdges();
            } else {
              var e = edgeList;
            }
            map_(e, updateSvgChoordsEdge);
        };
        API["clickNode"] = clickNode;
        API["highlightElements"] = highlightElements;
        API["setHighlights"] = setHighlights;
        API["removeHighlights"] = removeHighlights;
        API["multipleHighlighted"] = function () {
            return highlightedNodesMap["trashcan"] !== undefined && highlightedNodesMap["trashcan"].length > 0;
        };
        API["getTrashcan"] = function () {
            return highlightedNodesMap["trashcan"];
        };
    }


    return{
        doRender: doRender,
        redraw: function () {
            redraw_();
        }, 
        API: API
    };
});
