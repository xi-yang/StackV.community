/*
 * Copyright (c) 2013-2016 University of Maryland
 * Created by: Antonio Heard 2016

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

            function VolumeDisplayPopup(outputApi, renderApi) {
                this.svgContainer = null;
                this.dx = 0;
                this.dy = 0;
                this.minWidth = 0;
                this.minHeight = 0;
                this.bevel = 10;
                this.svgLine = null;
                this.svgBubble = null;
                this.color = "";
                this.outputApi = outputApi;
                /**@type Array.Port**/
                //this.portColors = [];
                //this.volumeEmptyColor = "";
                this.volumeHeight = 0;
                this.volumetWidth = 0;
                this.volumeBufferVertical = 0;
                this.volumeBufferHorizontal = 0;
                this.enlargeFactor = 0;
                this.opacity = 1;
                /**@type Node**/
                this.hostNode = null;
                this.visible = false;

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
//                this.setPortColors = function (colors) {
//                    this.portColors = colors;
//                    return this;
//                };
//                this.setPortEmptyColor = function (color) {
//                    this.portEmptyColor = color;
//                    return this;
//                };
                this.setVolumeDimensions = function (width, height) {
                    this.volumeWidth = width;
                    this.volumeHeight = height;
                    return this;
                };
                this.setVolumeBuffer = function (vert, horz) {
                    this.volumeBufferVertical = vert;
                    this.volumeBufferHorizontal = horz;
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
                    map_(this.hostNode.volumes, function (volume) {
                        volume.setVisible(vis);
                    });
                };


                this.updateSvgChoordsVolume = function (volume) {
                    var width, height, x, y;
                    width = that.volumeWidth;
                    height = that.volumeHeight;
                    x = volume.x - width / 2;
                    y = volume.y - height / 2;
                    
                    var dWidth, dHeight;
                    if (volume.enlarged) {
                        dWidth = width * that.enlargeFactor;
                        width += dWidth;
                        dHeight = height * that.enlargeFactor;
                        height += dHeight;
                    } else {
                        dWidth = 0;
                        dHeight = 0;
                    }
                    volume.svgNode
                            .attr("width", width)
                            .attr("height", height)
                            .attr("x", x - dWidth / 2)//make it appear to zoom into center of the icon
                            .attr("y", y - dHeight / 2);
                    if (volume.svgNodeSubnetHighlight) {
                        volume.svgNodeSubnetHighlight
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
                    var volumeContainer = this.svgContainer.select("#volume" + "_" + outputApi.svgContainerName);
                    //var parentPortContainer = this.svgContainer.select("#parentPort");
                    var container = this.svgContainer.select("#volumeDialogBox" + "_" + outputApi.svgContainerName);

                    var stack = [];
                    map_(this.hostNode.volumes, function (volume) {
                        stack.push(volume);
                    });
                    while (stack.length > 0) {
                        //We create a closure so that the "port" variable points to the correct object
                        //when the mouse events are called
                        (function () {
                            var volume = stack.pop();
                           
                            var color;
                            color = that.color;
                         //   alert("color: " + color);
                            
                                volume.svgNode = volumeContainer.append("image")
                                        .attr("xlink:href", volume.getIconPath());
                            if (volume.detailsReference) {
                                volume.svgNode.style("opacity", .4);
                            }
                            volume.svgNode
                                    .on("mousemove", function () {
                                        outputApi.setHoverText(volume.getName());
                                        outputApi.setHoverLocation(d3.event.clientX, d3.event.clientY);
                                        outputApi.setHoverVisible(true);
                                        volume.enlarged = true;
                                        
                                        that.updateSvgChoordsVolume(volume);
                                    })
                                    .on("mouseleave", function () {
                                        outputApi.setHoverVisible(false);
                                        volume.enlarged = false;
                                        that.updateSvgChoordsVolume(volume);
                                    })
                                    .on("click", function () {
                                        renderApi.selectElement(volume, that.outputApi);
                                    })
                                    .on("dblclick", function () {
                                        //volume.setFolded(!volume.getFolded());
                                        renderApi.redrawPopups(that.outputApi);
                                        renderApi.drawHighlight();
                                        renderApi.layoutEdges(that.outputApi);
                                    })
                                  //  .on("contextmenu", that.outputApi.contextMenu.renderedElemContextListener.bind(undefined, volume))                                    
                                    .call(dragBehaviour);
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
                    var volumeTotalHeight = 0;

                    var stack = [];
                    map_(this.hostNode.volumes, function (volume) {
                        stack.push(volume);
                    });
                    var maxWidth = 0;
                    while (stack.length > 0) {
                        var volume = stack.pop();
                        var width = that.volumeWidth + that.volumeBufferHorizontal;
                        maxWidth = Math.max(width, maxWidth);
                        var height = that.volumeHeight;
                        var dy = 0;
                            dy = height / 2;
                        

                        volume.x = x;
                        volume.y = y;
                        that.updateSvgChoordsVolume(volume);
                        y -= that.volumeHeight;
                        y -= that.volumeBufferVertical;
                        volumeTotalHeight += that.volumeHeight + that.volumeBufferVertical;

                    }
                    var height = this.minHeight;
                    if (volumeTotalHeight > height + this.volumeBufferVertical * 2) {
                        height = volumeTotalHeight + this.volumeBufferVertical * 2;
                    }
                    var width = this.minWidth;
                    if (maxWidth > width + 2 * this.volumeBufferHorizontal) {
                        width = maxWidth + 2 * this.volumeBufferHorizontal;
                    }
                    this.svgBubble
                            .attr("x", anchor.x + this.dx - width / 2)
                            .attr("y", anchor.y + this.dy - height - this.bevel / 2)
                            .attr("height", height + this.bevel / 2)
                            .attr("width", width)

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


            return VolumeDisplayPopup;
        });


