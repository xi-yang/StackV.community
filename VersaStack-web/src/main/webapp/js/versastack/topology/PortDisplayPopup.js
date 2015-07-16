"use strict";
define(["local/d3", "local/versastack/utils"],
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
                this.svgEdgeClip=null;
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

                this.width=0;

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


                this._setPortEnlarge = function (port, enlarge) {
                    if (port.hasChildren()) {
                        return;
                    }
                    port.enlarged = enlarge;
                    var width = that.portWidth;
                    var height = that.portHeight;
                    var dWidth, dHeight;
                    if (enlarge) {
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
                            .attr("x", port.x - width / 2)//make it appear to zoom into center of the icon
                            .attr("y", port.y - height / 2);
                    renderApi.drawHighlight();
                };

                this.render = function () {
                    if (!this.visible) {
                        return;
                    }
                    var container = this.svgContainer.select("#dialogBox");

                    this.height = this.minHeight;


                    //draw the ports
                    var portContainer = this.svgContainer.select("#port");
                    var parentPortContainer = this.svgContainer.select("#parentPort");

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
                        //We create a closure so that the "port" variable points to the correct object
                        //when the mouse events are called
                        (function () {
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
                            port.svgNode
                                    .attr("x", port.x - width / 2)
                                    .attr("y", y - dy) //this correcting is so that incoming edges align properly
                                    .attr("height", height)
                                    .attr("width", width)
                                    .on("mousemove", function () {
                                        outputApi.setHoverText(port.getName());
                                        outputApi.setHoverLocation(d3.event.x, d3.event.y);
                                        outputApi.setHoverVisible(true);
                                        that._setPortEnlarge(port, true);
                                    })
                                    .on("mouseleave", function () {
                                        outputApi.setHoverVisible(false);
                                        that._setPortEnlarge(port, false);
                                    })
                                    .on("click", function () {
                                        renderApi.selectElement(port);
                                    })
                                    .on("dblclick", function () {
                                        port.setFolded(!port.getFolded());
                                        renderApi.redrawPopups();
                                        renderApi.drawHighlight();
                                        renderApi.layoutEdges();
                                    })
                                    .call(dragBehaviour);
                            that._setPortEnlarge(port, port.enlarged);
                            port.edgeAnchorLeft = {x: port.x, y: port.y};
                            port.edgeAnchorRight = {x: port.x + width, y: port.y};
                            if (port.hasChildren()) {
                                y -= that.portBufferVertical;
                                portTotalHeight += that.portBufferVertical;
                            } else {
                                y -= that.portHeight;
                                y -= that.portBufferVertical;
                                portTotalHeight += that.portHeight + that.portBufferVertical;
                            }
                        })();
                    }
                    ;

                    var height = this.minHeight;
                    if (portTotalHeight > height + this.portBufferVertical * 2) {
                        height = portTotalHeight + this.portBufferVertical * 2;
                    }
                    var width = this.minWidth;
                    if (maxWidth > width + 2 * this.portBufferHorizontal) {
                        width = maxWidth + 2 * this.portBufferHorizontal;
                    }
                    this.width=width;

                    //Draw the box itself.
                    //We do this after the ports because are size is dependent on how many ports we draw
                    //The HTML template handles the layering inspite our out of order rendering.

                    var boxContainer = container.append("g")
                            .style("opacity", this.opacity);

                    boxContainer.append("rect")
                            .attr("x", anchor.x + this.dx - width / 2)
                            .attr("y", anchor.y + this.dy - height - this.bevel / 2)
                            .attr("height", height + this.bevel / 2)
                            .attr("width", width)
                            .attr("rx", this.bevel)
                            .attr("ry", this.bevel)
                            .style("fill", this.color)
                            .call(dragBehaviour);

                    //connect the box to the node;
                    var dst = this.hostNode.getCenterOfMass();
                    this.svgLine = boxContainer.append("line")
                            .style("stroke", this.color)
                            .style("stroke-width", this.neckWidth)
                            .attr("x1", anchor.x + this.dx)
                            .attr("y1", anchor.y + this.dy - height / 2)
                            .attr("stroke-linecap", "round")
                            .attr("x2", dst.x)
                            .attr("y2", dst.y);

                    //Fill in the clip-path so that the popup layer edges render
                    //The dimaensions of this rect should be the same as the box itself
                    var edgeClipContainer=this.svgContainer.select("#edge2Clip");
                    edgeClipContainer.append("rect")
                            .attr("x", anchor.x + this.dx - width / 2)
                            .attr("y", anchor.y + this.dy - height - this.bevel / 2)
                            .attr("height", height + this.bevel / 2)
                            .attr("width", width)
                            .attr("rx", this.bevel)
                            .attr("ry", this.bevel);
                    edgeClipContainer.append("line")
                            .style("stroke", this.color)
                            .style("stroke-width", this.neckWidth)
                            .attr("x1", anchor.x + this.dx)
                            .attr("y1", anchor.y + this.dy - height / 2)
                            .attr("stroke-linecap", "round")
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
                            renderApi.redrawPopups();
                            renderApi.drawHighlight();
                            renderApi.layoutEdges();
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