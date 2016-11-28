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
            function SwitchPopup(outputApi) {
                this.svgContainer = null;
                this.dx = 0;
                this.dy = 0;
                this.buffer = 0;
                this.innerBuffer;
                this.bevel = 10;
                this.svgNeck = null;
                this.svgBubble = null;
                this.opacity = .7;
                this.color = "";
                this.tabColor = "";
                this.tabColorSelected = "";
                this.tabWidth = 0;
                this.tabHeight = 0;
                this.ports = [];
                this.portColor = "";
                this.portEmptyColor = "";
                this.portHeight = 0;
                this.portWidth = 0;
                this.textSize = 0;
                /**@type Service**/
                this.hostNode = null;
                this.outputApi = outputApi;
                
                var that = this;
                this.setOffset = function (x, y) {
                    this.dx = x;
                    this.dy = y;
                    return this;
                };
                this.setHostNode = function (hostNode) {
                    this.hostNode = hostNode;
                    return this;
                }
                this.setOpacity = function (opacity) {
                    this.opacity = opacity;
                    return this;
                };
                this.setBuffer = function (buffer) {
                    this.buffer = buffer;
                    return this;
                };
                this.setInnerBuffer = function (innerBuffer) {
                    this.innerBuffer = innerBuffer;
                    return this;
                };
                this.setDimensions = function (width, height) {
                    this.width = width;
                    this.height = height;
                    return this;
                };
                this.setTabDimensions = function (tabWidth, tabHeight) {
                    this.tabWidth = tabWidth;
                    this.tabHeight = tabHeight;
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
                this.setTabColor = function (tabColor) {
                    this.tabColor = tabColor;
                    return this;
                };
                this.setTabColorSelected = function (tabColorSelected) {
                    this.tabColorSelected = tabColorSelected;
                    return this;
                };
                this.setPorts = function (ports) {
                    map_(this.ports, function (port) {
                        port.isVisible = false;
                    });
                    this.ports = ports;
                    map_(this.ports, function (port) {
                        port.isVisible = true;
                    });
                    return this;
                };
                this.setPortColor = function (color) {
                    this.portColor = color;
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
                this.setHostNode = function (n) {
                    this.hostNode = n;
                    return this;
                };
                this.setTextSize = function (textSize) {
                    this.textSize = textSize;
                    return this;
                };

                var isDragging = false;
                var lastMouse;
                function makeDragBehaviour() {
                    return d3.behavior.drag()
                            .on("drag", function () {
                                //Using the dx,dy from d3 can lead to some artifacts when also using
                                //These seem to occur when moving between different transforms
                                var e = d3.event.sourceEvent;
                                var dx = (e.clientX - lastMouse.clientX) / outputApi.getZoom();
                                var dy = (e.clientY - lastMouse.clientY) / outputApi.getZoom();
                                lastMouse = e;
                                that.dx += dx;
                                that.dy += dy;
                                that.render();
                                isDragging = true;
                            })
                            .on("dragstart", function () {
                                lastMouse = d3.event.sourceEvent;
                                outputApi.disablePanning();
                            })
                            .on("dragend", function () {
                                outputApi.enablePanning();
                                isDragging = false;
                            });
                }

                var previousHighlights = [];
                var clickSubnet = null;
                function eraseHighlights() {
                    map_(previousHighlights, function (removeHighlights) {
                        if (removeHighlights.svgNodeSubnetHighlight) {
                            removeHighlights.svgNodeSubnetHighlight.remove();
                            removeHighlights.svgNodeSubnetHighlight = null;
                        }
                    });
                    previousHighlights = [];
                }
                this.clear = function () {
                    this.hostNode = null;
                    eraseHighlights();
                    clickSubnet = null;
                    this.hostNode = null;
                    var container = this.svgContainer.select("#switchPopup" + "_" + outputApi.svgContainerName);
                    eraseHighlights();
                    container.selectAll("*").remove();
                    return this;
                };

                this.selectSubnet = function (subnet) {
                    if (clickSubnet) {
                        clickSubnet.svgNode.style("fill", that.tabColor);
                    }
                    clickSubnet = subnet;
                    clickSubnet.svgNode.style("fill", that.tabColorSelected);
//                    outputApi.setDisplayName(clickSubnet.getName());
//                    var displayTree = outputApi.getDisplayTree();
//                    displayTree.clear();
                    outputApi.renderApi.clickNode(clickSubnet.getName(), 'Subnet', that.outputApi);
//                    subnet.populateTreeMenu(displayTree);
//                    displayTree.draw();
                    eraseHighlights();
                    map_(subnet.ports, function (port) {
                        if (port.ancestorNode === null) return;
                        var toHighlight = port.getFirstVisibleParent();
                        //It is possible that multiple ports of the subnet will resolve to the same node
                        //when deciding what we should highlight. To avoid issues in removing highlights,
                        //we make sure to not highlight the same element multiple times.
                        if (toHighlight.svgNode && !toHighlight.svgNodeSubnetHighlight) {
                            var toAppend = toHighlight.svgNode.node().cloneNode();
                            previousHighlights.push(toHighlight);
                            toHighlight.svgNodeSubnetHighlight = d3.select(toAppend)
                                    .style("filter", "url(#subnetHighlight)")
                                    .style("opacity", "1")
                                    .attr("pointer-events", "none");
                            var parentNode = toHighlight.svgNode.node().parentNode;
                            if (parentNode) {
                                parentNode.appendChild(toAppend);
                            } else {
                                console.log("Trying to highlight an element without a parent")
                            }
                        } else if (!toHighlight.svgNode) {
                            console.log("trying to highlight an element without an svgNode")
                        }
                    });
                }


                this.render = function () {
                    if (!this.hostNode || this.hostNode.subnets.length === 0) {
                        return;
                    }
                    var container = this.svgContainer.select("#switchPopup" + "_" + outputApi.svgContainerName);
                    container.selectAll("*").remove();
                    eraseHighlights();
                    var anchor = this.hostNode.getCenterOfMass();
                    var boxContainer = container.append("g")
                            .style("opacity", this.opacity);
                    //draw switch popup
                    //Because the tabWidth is dynamic, we wait to position things until after we know it
                    this.svgBubble = boxContainer.append("rect")
                            .attr("rx", this.bevel)
                            .attr("ry", this.bevel)
                            .style("fill", this.color)
                            .call(makeDragBehaviour());
                    var serviceChoords = this.hostNode.getCenterOfMass();
                    this.svgNeck = boxContainer.append("line")
                            .attr("x2", serviceChoords.x)
                            .attr("y2", serviceChoords.y)
                            .style("stroke", this.color)
                            .attr("stroke-linecap", "round");
                    var tabWidth = 0;
                    var innerHeight = 0;
                    //draw subnet tab
                    var subnetContainer = this.svgContainer.select("#tab");
                    subnetContainer.selectAll("*").remove();
                    map_(this.hostNode.subnets, /**@param {Subnet} subnet**/function (subnet) {

                        var onMouseMove = function () {
                            if (!isDragging) {
                                outputApi.setHoverText(subnet.getName());
                                outputApi.setHoverVisible(true);
                            }
                            outputApi.setHoverLocation(d3.event.clientX, d3.event.clientY);

                        };
                        var OnMouseLeave = function () {
                            if (!isDragging) {
                                outputApi.setHoverVisible(false);
                            }
                        };
                        var onClick = function () {
                            outputApi.formSelect(subnet);
                            that.selectSubnet(subnet);
                        };


                        subnet.svgNode = container.append("rect")
                                .style("fill", that.tabColor)
                                .on("mousemove", onMouseMove)
                                .on("mouseleave", OnMouseLeave)
                                .on("click", onClick)
                                .call(makeDragBehaviour());
                        subnet.svgNodeText = container.append("text")
                                .text(subnet.getNameBrief())
                                .attr("font-size", that.textSize)
                                .on("mousemove", onMouseMove)
                                .on("mouseleave", OnMouseLeave)
                                .on("click", onClick)
                                .call(makeDragBehaviour());
                        tabWidth = Math.max(tabWidth, subnet.svgNodeText.node().getComputedTextLength());
                        subnet.svgNodeCover = container.append("rect")
                                .style("opacity", "0")
                                .on("mousemove", onMouseMove)
                                .on("mouseleave", OnMouseLeave)
                                .on("contextmenu", that.outputApi.contextMenu.renderedElemContextListener.bind(undefined, subnet))                                             
                                .on("click", onClick)
                                .call(makeDragBehaviour());
                    });
                    var width = tabWidth;
                    var height = 0;
                    width = Math.max(this.width, width + that.buffer * 2);
                    //now that we know what the tabwidth is, we can finish posistioning everything
                    var x = anchor.x - width / 2 + that.dx + that.bevel / Math.sqrt(2);
                    var y = anchor.y + that.dy + that.bevel / Math.sqrt(2);
                    map_(this.hostNode.subnets, /**@param {Subnet} subnet**/function (subnet) {
                        subnet.svgNode
                                .attr("x", x)
                                .attr("y", y)
                                .attr("height", that.textSize + that.innerBuffer * 2)
                                .attr("width", width);
                        
                        if (subnet.detailsReference) {
                            subnet.svgNode.style("opacity", .4);
                        }

                        subnet.svgNodeCover
                                .attr("x", x)
                                .attr("y", y)
                                .attr("height", that.textSize + that.innerBuffer * 2)
                                .attr("width", width);
                        subnet.svgNodeText
                                .attr("x", x + that.innerBuffer)
                                .attr("y", y + that.innerBuffer + that.textSize);
                        y += that.textSize + that.innerBuffer * 2 + that.buffer;
                        height += that.textSize + that.innerBuffer * 2 + that.buffer;
                    });
                    var bubbleX = anchor.x - tabWidth / 2 + this.dx;
                    var bubbleY = anchor.y + this.dy;
                    var bubbleWidth = width + that.buffer * 2 + that.bevel / Math.sqrt(2);
                    var bubbleHeight = height + that.bevel + that.buffer;
                    this.svgBubble
                            .attr("x", bubbleX)
                            .attr("y", bubbleY)
                            .attr("height", bubbleHeight)
                            .attr("width", bubbleWidth);
                    this.svgNeck
                            .attr("x1", bubbleX + bubbleWidth / 2)
                            .attr("y1", bubbleY + bubbleHeight / 2)

                    if (clickSubnet) {
                        this.selectSubnet(clickSubnet);
                    }
                };
            }
            return SwitchPopup;
        });
