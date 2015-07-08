"use strict";
define(["local/d3", "local/versastack/utils"],
        function (d3, utils) {
            var map_ = utils.map_;



            function DialogBox() {
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
                this.color="";
                this.ports=[];
                this.portColor="";
                this.portEmptyColor="";
                this.portHeight=0;
                this.portWidth=0;
                
                var that=this;
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
                this.setColor=function(color){
                    this.color=color;
                    return this;
                };
                this.setPorts=function(ports){
                    map_(this.ports,function(port){
                        port.isVisible=false;
                    })
                    this.ports=ports;
                    map_(this.ports,function(port){
                        port.isVisible=true;
                    })
                    return this;
                };
                this.setPortColor=function(color){
                    this.portColor=color;
                    return this;
                };
                this.setPortEmptyColor=function(color){
                    this.portEmptyColor=color;
                    return this;
                };
                this.setPortDimensions=function(width,height){
                    this.portWidth=width;
                    this.portHeight=height;
                    return this;
                }

                this.render = function () {
                    var container = this.svgContainer.select("#dialogBox");
                    container.selectAll("*").remove();

                    //draw the neck as a triangle
                    container.append("polygon")
                            .attr("points", constructNeckPoints())
                            .style("fill",this.color);

                    container.append("rect")
                            .attr("x", this.anchorX - this.width / 2)
                            .attr("y", this.anchorY - this.neckLength - this.height)
                            .attr("height", this.height)
                            .attr("width", this.width)
                            .attr("rx", this.bevel)
                            .attr("ry", this.bevel)
                            .style("fill",this.color);
                    
                    //draw the ports
                    var x=this.anchorX-this.width/2 + this.bevel/2;
                    var y=this.anchorY-this.neckLength-this.height+this.bevel/2;
                    map_(this.ports,function(port){
                       port.x=x;
                       port.y=y;
                       container.append("rect")
                               .attr("x",x)
                               .attr("y",y-that.portHeight/2)
                               .attr("height",that.portHeight)
                               .attr("width",that.portWidth)
                               .attr("fill",port.hasAlias()?that.portColor:that.portEmptyColor);
                       y+=that.portHeight*2;
                    });
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