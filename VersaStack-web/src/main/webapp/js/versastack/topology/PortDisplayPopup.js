"use strict";
define(["local/d3", "local/versastack/utils"],
        function (d3, utils) {
            var map_ = utils.map_;



            function PortDisplayPopup(outputApi, renderApi) {
                this.svgContainer = null;
                this.anchorX = 0;
                this.anchorY = 0;
                this.neckLength = 0;
                this.neckWidth = 0;
                this.minWidth = 0;
                this.minHeight = 0;
                this.bevel = 10;
                this.svgLine = null;
                this.svgBubble = null;
                this.color = "";
                this.ports = [];
                this.portColors = [];
                this.portEmptyColor = "";
                this.portHeight = 0;
                this.portWidth = 0;
                this.portBufferVertical = 0;
                this.portBufferHorizontal = 0;
                this.enlargeFactor = 0;
                this.opacity=1;
                this.hostNode = null;

                var that = this;
                this.setAnchor = function (x, y) {
                    this.anchorX = x;
                    this.anchorY = y;
                    return this;
                };
                this.setNeck = function (length, width) {
                    this.neckLength = length;
                    this.neckWidth = width;
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
                this.setOpacity=function(opacity){
                    this.opacity=opacity;
                    return this;
                };
                this.setPorts = function (ports) {
                    //Return the old ports to being invisible
                    var stack = [];
                    map_(this.ports, function (port) {
                        stack.push(port);
                    });
                    while (stack.length > 0) {
                        var port = stack.pop();
                        map_(port.childrenPorts, function (port) {
                            stack.push(port);
                        });
                        port.isVisible = false;
                    }
                    ;

                    this.ports = ports;

                    //Mark the new ports as visible
                    map_(this.ports, function (port) {
                        stack.push(port);
                    });
                    while (stack.length > 0) {
                        var port = stack.pop();
                        map_(port.childrenPorts, function (port) {
                            stack.push(port);
                        });
                        port.isVisible = true;
                    }
                    ;

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
                this.move=function(dx,dy){
                    this.anchorX+=dx;
                    this.anchorY+=dy;
                    return this;
                };
               

                this._setPortEnlarge = function (port, enlarge) {
                    if (port.hasChildren()) {
                        return;
                    }
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
                    var container = this.svgContainer.select("#dialogBox");
                    container.selectAll("*").remove();

                    this.height = this.minHeight;


                    //draw the ports
                    var portContainer = this.svgContainer.select("#port");
                    portContainer.selectAll("*").remove();
                    var x = this.anchorX;
                    var y = this.anchorY - this.bevel;
                    var portTotalHeight = 0;

                    var stack = [];
                    map_(this.ports, function (port) {
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
                                port.svgNode = portContainer.append("rect")
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
                                        that.render();
                                        renderApi.redraw();
                                    })
                                    .call(dragBehaviour);
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

                    //Draw the box itself.
                    //We do this after the ports because are size is dependent on how many ports we draw
                    //The HTML template handles the layering inspite our out of order rendering.

                    var boxContainer=container.append("g")
                            .style("opacity",this.opacity);

                    boxContainer.append("rect")
                            .attr("x", this.anchorX - width / 2)
                            .attr("y", this.anchorY - height - this.bevel / 2)
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
                            .attr("x1", this.anchorX)
                            .attr("y1", this.anchorY-height/2)
                            .attr("stroke-linecap","round")
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
                                        
                                        that.anchorX+=dx;
                                        that.anchorY+=dy;
                                        
                                        outputApi.setHoverLocation(e.clientX, e.clientY);
                                        renderApi.drawHighlight();
                                        renderApi.layoutEdges();
                                        that.render();
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