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
define(["local/d3", "local/stackv/utils"],
        function (d3, utils) {
            var map_ = utils.map_;

            function PortDisplayPopup(outputApi, renderApi) {
                this.svgContainer = null;
                this.dx = 0;
                this.dy = 0;
                this.minWidth = 0;
                this.minHeight = 0;
                this.bevel = 10;
                this.svgLine = null;
                this.svgBubble = null;
                this.color = "";
                /**@type Array.Port**/
                this.portColors = [];
                this.portEmptyColor = "";
                this.portHeight = 0;
                this.portWidth = 0;
                this.portBufferVertical = 0;
                this.portBufferHorizontal = 0;
                this.enlargeFactor = 0;
                this.opacity = 1;
                /**@type Node**/
                this.hostNode = null;
                this.visible = false;
                this.outputApi = outputApi;

                var that = this;
                this.setOffset = function (x, y) {
                    this.dx = x;
                    this.dy = y;
                    return this;
                };
                this.setDimensions = function (width, height) {
                    this.minWidth = width;
                    this.minHeight = height;
                    return this;
                };
                this.setBevel = function (r) {
                    this.bevel = r;
                    return this;
                };
                this.setContainer = function (container) {
                    this.svgContainer = container;
                    return this;
                };
                this.setColor = function (color) {
                    this.color = color;
                    return this;
                };
                this.setOpacity = function (opacity) {
                    this.opacity = opacity;
                    return this;
                };
                this.setPortColors = function (colors) {
                    this.portColors = colors;
                    return this;
                };
                this.setPortEmptyColor = function (color) {
                    this.portEmptyColor = color;
                    return this;
                };
                this.setPortDimensions = function (width, height) {
                    this.portWidth = width;
                    this.portHeight = height;
                    return this;
                };
                this.setPortBuffer = function (vert, horz) {
                    this.portBufferVertical = vert;
                    this.portBufferHorizontal = horz;
                    return this;
                };
                this.setEnlargeFactor = function (x) {
                    this.enlargeFactor = x;
                    return this;
                };
                this.setHostNode = function (n) {
                    this.hostNode = n;
                    return this;
                };
                this.move = function (dx, dy) {
                    this.dx += dx;
                    this.dy += dy;
                    return this;
                };

                this.toggleVisible = function () {
                    this.setVisible(!this.visible);
                };
                this.setVisible = function (vis) {
                    this.visible = vis;
                    map_(this.hostNode.ports, function (port) {
                        port.setVisible(vis);
                    });
                };


                this.updateSvgChoordsPort = function (port) {
                    var width, height, x, y;
                    if (port.hasChildren()) {
                        width = that.portWidth + (port.getVisibleHeight() - 1) * that.portBufferHorizontal;
                        height = (that.portHeight + that.portBufferVertical) * port.countVisibleLeaves() + that.portBufferVertical * 2;
                        x = port.x - width / 2;
                        y = port.y + that.portBufferVertical * 2 - height;
                    } else {
                        width = that.portWidth;
                        height = that.portHeight;
                        x = port.x - width / 2;
                        y = port.y - height / 2;
                    }
                    var dWidth, dHeight;
                    if (port.enlarged) {
                        dWidth = width * that.enlargeFactor;
                        width += dWidth;
                        dHeight = height * that.enlargeFactor;
                        height += dHeight;
                    } else {
                        dWidth = 0;
                        dHeight = 0;
                    }
                    port.svgNode
                            .attr("width", width)
                            .attr("height", height)
                            .attr("x", x - dWidth / 2)//make it appear to zoom into center of the icon
                            .attr("y", y - dHeight / 2);
                    if (port.svgNodeSubnetHighlight) {
                        port.svgNodeSubnetHighlight
                                .attr("width", width)
                                .attr("height", height)
                                .attr("x", x - dWidth / 2)
                                .attr("y", y - dHeight / 2);
                    }
                    renderApi.drawHighlight();
                };

                this.render = function () {
                    if (!this.visible) {
                        return;
                    }
                    this.setVisible(true);
                    //draw the ports
                    var portContainer = this.svgContainer.select("#port" + "_" + outputApi.svgContainerName);
                    var parentPortContainer = this.svgContainer.select("#parentPort" + "_" + outputApi.svgContainerName);
                    var container = this.svgContainer.select("#dialogBox" + "_" + outputApi.svgContainerName);

                    var stack = [];
                    map_(this.hostNode.ports, function (port) {
                        stack.push(port);
                    });
                    while (stack.length > 0) {
                        //We create a closure so that the "port" variable points to the correct object
                        //when the mouse events are called
                        (function () {
                            var port = stack.pop();
                            if (port) {
                                map_(port.childrenPorts, function (child) {
                                    stack.push(child);
                                });


                                var color;
                                if (port.hasAlias() || port.hasChildren()) {
                                    color = that.portColors[port.getVisibleHeight() % that.portColors.length];
                                } else {
                                    color = that.portEmptyColor;
                                }
                                if (port.hasChildren()) {
                                    port.svgNode = parentPortContainer.append("rect")
                                            .style("fill", color);
                                } else {
                                    port.svgNode = portContainer.append("image")
                                            .attr("xlink:href", port.getIconPath());
                                }
                                if (port.detailsReference) {
                                    port.svgNode.style("opacity", .4);
                                }

                                port.svgNode
                                        .on("mousemove", function () {
                                            outputApi.setHoverText(port.getName());
                                            outputApi.setHoverLocation(d3.event.clientX, d3.event.clientY);
                                            outputApi.setHoverVisible(true);
                                            if (!port.hasChildren()) {
                                                port.enlarged = true;
                                            }
                                            that.updateSvgChoordsPort(port);
                                        })
                                        .on("mouseleave", function () {
                                            outputApi.setHoverVisible(false);
                                            port.enlarged = false;
                                            that.updateSvgChoordsPort(port);
                                        })
                                        .on("click", function () {
                                            renderApi.selectElement(port, that.outputApi);
                                        })
                                        .on("dblclick", function () {
                                            port.setFolded(!port.getFolded());
                                            renderApi.redrawPopups(that.outputApi);
                                            renderApi.drawHighlight();
                                            renderApi.layoutEdges(that.outputApi);
                                        });
                                if (that.outputApi.contextMenu) {
                                    port.svgNode.on("contextmenu", that.outputApi.contextMenu.renderedElemContextListener.bind(undefined, port));
                                }
                                port.svgNode.call(dragBehaviour);
                            }
                        })();
                    }
                    //Draw the box itself.
                    //We do this after the ports because are size is dependent on how many ports we draw
                    //The HTML template handles the layering inspite our out of order rendering.

                    //We embed the popup in a group to apply the opacity. 
                    //Without doing this (and applying the opacity directly on 
                    //the elements) there would be an artifact when the elements
                    //overlap.
                    var boxContainer = container.append("g")
                            .style("opacity", this.opacity);

                    this.svgBubble = boxContainer.append("rect")
                            .attr("rx", this.bevel)
                            .attr("ry", this.bevel)
                            .style("fill", this.color)
                            .call(dragBehaviour);

                    //connect the box to the node;
                    this.svgLine = boxContainer.append("line")
                            .style("stroke", this.color)
                            .style("stroke-width", this.neckWidth)
                            .attr("stroke-linecap", "round");
                    this.updateSvgChoords();
                    return this;
                };

                this.updateSvgChoords = function () {
                    if (!this.visible) {
                        return;
                    }
                    var anchor = this.hostNode.getCenterOfMass();
                    var x = anchor.x + this.dx;
                    var y = anchor.y + this.dy - this.bevel;
                    var portTotalHeight = 0;

                    var stack = [];
                    map_(this.hostNode.ports, function (port) {
                        stack.push(port);
                    });
                    var maxWidth = 0;
                    while (stack.length > 0) {
                        var port = stack.pop();
                        var width = that.portWidth + (port.getVisibleHeight() - 1) * that.portBufferHorizontal;
                        maxWidth = Math.max(width, maxWidth);
                        var height = that.portHeight;
                        var dy = 0;
                        if (port.hasChildren()) {
                            map_(port.childrenPorts, function (child) {
                                stack.push(child);
                            });
                            height = that.portHeight * port.countVisibleLeaves();
                            height += that.portBufferVertical * (port.countVisibleLeaves() + 1);
                            dy = height - that.portHeight / 2;
                        } else {
                            dy = height / 2;
                        }

                        port.x = x;
                        port.y = y;
                        that.updateSvgChoordsPort(port);
                        if (port.hasChildren()) {
                            y -= that.portBufferVertical;
                            portTotalHeight += that.portBufferVertical;
                        } else {
                            y -= that.portHeight;
                            y -= that.portBufferVertical;
                            portTotalHeight += that.portHeight + that.portBufferVertical;
                        }
                    }
                    var height = this.minHeight;
                    if (portTotalHeight > height + this.portBufferVertical * 2) {
                        height = portTotalHeight + this.portBufferVertical * 2;
                    }
                    var width = this.minWidth;
                    if (maxWidth > width + 2 * this.portBufferHorizontal) {
                        width = maxWidth + 2 * this.portBufferHorizontal;
                    }
                    this.svgBubble
                            .attr("x", anchor.x + this.dx - width / 2)
                            .attr("y", anchor.y + this.dy - height - this.bevel / 2)
                            .attr("height", height + this.bevel / 2)
                            .attr("width", width);
                    // new chane, added debugging @
                    console.log("\n]nEntering PortDisplayPopup updateSvgChoords");
                    console.log("x: " + (anchor.x + this.dx - width / 2));
                    console.log("y: " + (anchor.y + this.dy - height - this.bevel / 2));
                    console.log("anchor.x: " + anchor.x);
                    console.log("anchor.y: " + anchor.y);
                    console.log("this.dx: " + this.dx);
                    console.log("this.dy: " + this.dy);
                    console.log("\nLeaving  PortDisplayPopup updateSvgChoords \n\n");
                    //connect the box to the node;
                    var dst = this.hostNode.getCenterOfMass();
                    this.svgLine
                            .attr("x1", anchor.x + this.dx)
                            .attr("y1", anchor.y + this.dy - height / 2)
                            .attr("x2", dst.x)
                            .attr("y2", dst.y);
                    return this;
                };

                var lastMouse;
                var dragBehaviour = d3.behavior.drag()
                        .on("drag", function () {
                            var e = d3.event.sourceEvent;
                            var dx = (e.clientX - lastMouse.clientX) / outputApi.getZoom();
                            var dy = (e.clientY - lastMouse.clientY) / outputApi.getZoom();
                            lastMouse = e;

                            that.dx += dx;
                            that.dy += dy;

                            outputApi.setHoverLocation(e.clientX, e.clientY);
                            that.updateSvgChoords();
                            renderApi.drawHighlight();
                            renderApi.highlightElements("serviceHighlighting");
                            renderApi.layoutEdges(that.outputApi);
                        })
                        .on("dragstart", function () {
                            lastMouse = d3.event.sourceEvent;
                            outputApi.disablePanning();
                        })
                        .on("dragend", function () {
                            outputApi.enablePanning();
                        });
            }


            return PortDisplayPopup;
        });
