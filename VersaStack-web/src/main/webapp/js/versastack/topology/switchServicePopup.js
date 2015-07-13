"use strict";
define(["local/d3", "local/versastack/utils"],
        function (d3, utils) {
            var map_ = utils.map_;



            function SwitchPopup(outputApi) {
                this.svgContainer = null;
                this.anchorX = 0;
                this.anchorY = 0;
                this.service = null;
                this.neckLength = 0;
                this.neckWidth = 0;
                this.width = 0;
                this.height = 0;
                this.bevel = 10;
                this.svgNeck = null;
                this.svgBubble = null;
                this.color = "";
                this.ports = [];
                this.portColor = "";
                this.portEmptyColor = "";
                this.portHeight = 0;
                this.portWidth = 0;

                var that = this;
                this.setAnchor = function (x, y) {
                    this.anchorX = x;
                    this.anchorY = y;
                    return this;
                };
                this.setService = function(service){
                    this.service=service;
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
                                that.anchorX+=dx;
                                that.anchorY+=dy;
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

                this.render = function () {
                    var container = this.svgContainer.select("#switchPopup");
                    container.selectAll("*").remove();

                    var serviceChoords = this.service.getCenterOfMass();
                    container.append("line")
                            .attr("x1", this.anchorX)
                            .attr("y1", this.anchorY)
                            .attr("x2", serviceChoords.x)
                            .attr("y2", serviceChoords.y)
                            .style("stroke", this.color);
                    

                    container.append("rect")
                            .attr("x", this.anchorX - this.width / 2)
                            .attr("y", this.anchorY)
                            .attr("height", this.height)
                            .attr("width", this.width)
                            .attr("rx", this.bevel)
                            .attr("ry", this.bevel)
                            .style("fill", this.color)
                            .call(makeDragBehaviour());


                };


                
            }


            return SwitchPopup;
        });
