"use strict";
define(["local/d3", "local/versastack/utils"],
        function (d3, utils) {
            var map_ = utils.map_;



            function DialogBox(outputApi, renderAPI) {
                this.svgContainer = null;
                this.anchorX = 0;
                this.anchorY = 0;
                this.neckLength = 0;
                this.neckWidth = 0;
                this.width = 0;
                this.height = 0;
                this.bevel = 10;
                this.svgNeck = null;
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
                    this.width = width;
                    this.height = height;
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



                this._setPortEnlarge = function (port, enlarge) {
                    if(port.hasChildren()){
                        return;
                    }
                    var width = that.portWidth;
                    var height = that.portWidth;
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
                    renderAPI.drawHighlight();
                };

                this.render = function () {
                    var container = this.svgContainer.select("#dialogBox");
                    container.selectAll("*").remove();

                    //draw the neck as a triangle
                    container.append("polygon")
                            .attr("points", constructNeckPoints())
                            .style("fill", this.color);

                    container.append("rect")
                            .attr("x", this.anchorX - this.width / 2)
                            .attr("y", this.anchorY - this.neckLength - this.height)
                            .attr("height", this.height)
                            .attr("width", this.width)
                            .attr("rx", this.bevel)
                            .attr("ry", this.bevel)
                            .style("fill", this.color);

                    //draw the ports
                    container = this.svgContainer.select("#port");
                    container.selectAll("*").remove();
                    var x = this.anchorX;
                    var y = this.anchorY - this.neckLength - this.height + this.bevel / 2;

                    var stack = [];
                    map_(this.ports, function (port) {
                        stack.push(port);
                    });
                    while (stack.length > 0) {
                        //We create a closure so that the "port" variable points to the correct object
                        //when the mouse events are called
                        (function () {
                            var port = stack.pop();
                            var width = that.portWidth + (port.getVisibleHeight() - 1) * that.portBufferHorizontal;
                            var height = that.portHeight;
                            if (port.hasChildren()) {
                                map_(port.childrenPorts, function (child) {
                                    stack.push(child);
                                });
                                height = that.portHeight * port.countVisibleLeaves();
                                height += that.portBufferVertical * (port.countVisibleLeaves() + 1);
                            }

                            port.x = x
                            port.y = y;
                            var color;
                            if (port.hasAlias() || port.hasChildren()) {
                                color = that.portColors[port.getVisibleHeight() % that.portColors.length];
                            } else {
                                color = that.portEmptyColor;
                            }
                            if (port.hasChildren()) {
                                port.svgNode = container.append("rect")
                                        .style("fill", color);
                            } else {
                                port.svgNode = container.append("image")
                                        .attr("xlink:href", port.getIconPath());
                            }
                            port.svgNode
                                    .attr("x", port.x - width / 2)
                                    .attr("y", y - that.portHeight / 2) //this correcting is so that incoming edges align properly
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
                                        renderAPI.selectElement(port);
                                    })
                                    .on("dblclick", function () {
                                        port.setFolded(!port.getFolded());
                                        that.render();
                                        renderAPI.redraw();
                                    })
                            port.edgeAnchorLeft = {x: port.x, y: port.y};
                            port.edgeAnchorRight = {x: port.x + width, y: port.y};
                            if (port.hasChildren()) {
                                y += that.portBufferVertical;
                            } else {
                                y += that.portHeight;
                                y += that.portBufferVertical;
                            }
                        })();
                    }
                    ;
                    return this;
                };


                function constructNeckPoints() {
                    return   that.anchorX + "," + that.anchorY + " "
                            + (that.anchorX - that.neckWidth / 2) + "," + (that.anchorY - that.neckLength) + " "
                            + (that.anchorX + that.neckWidth / 2) + "," + (that.anchorY - that.neckLength);
                }
            }


            return DialogBox;
        });