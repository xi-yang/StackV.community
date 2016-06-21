"use strict";
define([
    "local/versastack/utils",
    "local/versastack/topology/DropDownNode"
], function (utils, DropDownNode) {
    function DropDownTree(containerDiv) {
        var map_ = utils.map_;
        /**@type Array.DropDownNode**/
        this.rootNodes = [];
        this.containerDiv = containerDiv;
        this.renderApi = null;
        this.contextMenu = null;
        this.history = [];
        this.topViewShown = false;
        var currentIndex = -1;
        var lastIndex = -1;
        

                
        var that = this;
        
        // Code for all functionality related to the Model Browser goes here. 
        // going back and forth in stuff that already exists 
        document.getElementById("backButton").onclick = function() {
                        console.log("I'm in back : currentIndex: " + currentIndex + " lastIndex: " + lastIndex);

            if (that.history.length > 0  && currentIndex - 1 >= 0 ) {
               currentIndex--;
               that.renderApi.clickNode(that.history[currentIndex][0], that.history[currentIndex][1]);
            }
        };

        document.getElementById("forwardButton").onclick = function() { 
                        console.log("I'm in forward: currentIndex: " + currentIndex + " lastIndex: " + lastIndex);
                           
             if (that.history.length > 0  && currentIndex + 1 <= lastIndex ) {
               currentIndex++;
               that.renderApi.clickNode(that.history[currentIndex][0], that.history[currentIndex][1]);
            }               
        };        
       
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
                that.renderApi.clickNode(uri, "Element");
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
                    that.renderApi.clickNode(that.history[currentIndex][0], that.history[currentIndex][1]);
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
            var ans = new DropDownNode(name, that.renderApi, type, data, that.contextMenu);
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