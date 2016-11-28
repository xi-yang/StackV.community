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
define([
    "local/stackv/utils",
    "local/stackv/topology/DropDownNode"
], function (utils, DropDownNode) {
    function DropDownTree(containerDiv, outputApi) {
        var map_ = utils.map_;
        /**@type Array.DropDownNode**/
        this.rootNodes = [];
        this.containerDiv = containerDiv;
        this.renderApi = null;
        this.contextMenu = null;
        this.history = [];
        this.topViewShown = false;
        this.outputApi = outputApi;
        var currentIndex = -1;
        var lastIndex = -1;

                
        var that = this;
        
        // Code for all functionality related to the Model Browser goes here. 
        // going back and forth in stuff that already exists 
        $("#" + that.outputApi.svgContainerName + "_backButton").on('click',  function() {
                console.log("I'm in back : currentIndex: " + currentIndex + " lastIndex: " + lastIndex);

                if (that.history.length > 0  && currentIndex - 1 >= 0 ) {
                    currentIndex--;
                    that.renderApi.clickNode(that.history[currentIndex][0], that.history[currentIndex][1], that.outputApi);
                }
            
        });

        $("#" + that.outputApi.svgContainerName + "_forwardButton").on('click',  function() { 
                console.log("I'm in forward: currentIndex: " + currentIndex + " lastIndex: " + lastIndex);
                           
                if (that.history.length > 0  && currentIndex + 1 <= lastIndex ) {
                    currentIndex++;
                    that.renderApi.clickNode(that.history[currentIndex][0], that.history[currentIndex][1], that.outputApi);
                }              
        });        
       
        this.addToHistory = function(name, type) {
            console.log("I'm here: currentIndex: " + currentIndex);

            if (currentIndex === -1 || this.history[currentIndex][0] !== name) {
                currentIndex ++;
                if (currentIndex !== lastIndex ) {
                    // if youve pressed back and click something new
                    // erase the emd 0f the list from here to there 
                    // essentially staritng a new path 
                    lastIndex = currentIndex; 
                }
                this.history[currentIndex] = [name, type];
                console.log("I'm here: currentIndex: " + currentIndex);
                console.log("my history: name: " + that.history[currentIndex][0] + " type: " + that.history[currentIndex][1]);
            }
        };
        
        var uriSearchSubmit = document.getElementById("URISearchSubmit");
        if (uriSearchSubmit){
            uriSearchSubmit.onclick = function() {
                var uri = document.getElementById("URISearchInput").value;
                that.renderApi.clickNode(uri, "Element", that.outputApi);
            };
        }
        
        var fullDisplay = document.getElementById("fullDiaplayButton");
        if (fullDisplay) {
            fullDisplay.onclick = function() {
                if (!that.topViewShown) {
                    that.topViewShown = true;
                    that.renderApi.selectElement(null);
                } else {
                    that.topViewShown = false;
                    that.renderApi.clickNode(that.history[currentIndex][0], that.history[currentIndex][1], that.outputApi);
                }           
            };
        }
        
        this.clear = function () {
            this.rootNodes = [];
            utils.deleteAllChildNodes(this.containerDiv);
        };
        
        // maybe get it from here 
        this.draw = function () {
            utils.deleteAllChildNodes(this.containerDiv);
            this.rootNodes= map_(this.rootNodes, /**@param {DropDownNode} node**/function (node) {
           //     if (node === undefined)return 0;
                var toAppend = node.getHTML();
                //Every node automatically indents itself.
                //In the case of the root node, this is undesired, so we apply 
                //an opposite indent to counteract it.
//                toAppend.style.marginLeft="-15px";
                that.containerDiv.appendChild(toAppend);
                node.renderApi = that.renderApi;
            });
            if (this.rootNodes.length === 0) {
                this.addChild("test", "", null);
            }
            //("i'm here"); was for seeig where the start is 
        };

        //We use the same method name as DropDownNode.addChild to enable polymorphism
        this.addChild = function (name, type, data) {
            var ans = new DropDownNode(name, that.renderApi, type, data, that.contextMenu, that.outputApi);
            this.rootNodes.push(ans);
            return ans;
        };
       
        this.open = function() {
          $("#displayPanel-tab").addClass("display-open");  
          $("#displayPanel").addClass("display-open");  

        };
    }
    return DropDownTree;
});
