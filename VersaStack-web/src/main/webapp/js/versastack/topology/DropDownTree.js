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
        this.history = [];
        var currentIndex = -1;
        var lastIndex = -1;
        

                
        var that = this;
        // going back and forth in stuff that already exists 
        document.getElementById("backButton").onclick = function() {
                        console.log("I'm in back : currentIndex: " + currentIndex + " lastIndex: " + lastIndex);

            if (that.history.length > 0  && currentIndex - 1 >= 0 ) {
               currentIndex--;
               if (that.renderApi !== null && that.renderApi !== undefined)
                   that.renderApi.clickNode(that.history[currentIndex][0], that.history[currentIndex][1]);
            }
        };

        document.getElementById("forwardButton").onclick = function() { 
                        console.log("I'm in forward: currentIndex: " + currentIndex + " lastIndex: " + lastIndex);
                           
             if (that.history.length > 0  && currentIndex + 1 <= lastIndex ) {
               currentIndex++;
               if (that.renderApi !== null && that.renderApi !== undefined) {
                   that.renderApi.clickNode(that.history[currentIndex][0], that.history[currentIndex][1]);
                    console.log("in innter");
                }
               console.log("in outer");
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
        
        this.clear = function () {
            this.rootNodes = [];
            utils.deleteAllChildNodes(this.containerDiv);
        };
        
        // maybe get it from here 
        this.draw = function () {
            utils.deleteAllChildNodes(this.containerDiv);
            this.rootNodes= map_(this.rootNodes, /**@param {DropDownNode} node**/function (node) {
                var toAppend = node.getHTML();
                //Every node automatically indents itself.
                //In the case of the root node, this is undesired, so we apply 
                //an opposite indent to counteract it.
//                toAppend.style.marginLeft="-15px";
                that.containerDiv.appendChild(toAppend);
                if (that.renderApi !== null &&  that.renderApi !== undefined) {
                    node.renderApi = that.renderApi;
                    console.log("I did this");
                    
                    console.log("node.renderApi: "  + node.renderApi);
                    if (node.renderApi === undefined)
                        console.log("it's undefined");
                }
            });
            if (this.rootNodes.length === 0) {
                addChild("test", "");
            }
            //("i'm here"); was for seeig where the start is 
        };

        //We use the same method name as DropDownNode.addChild to enable polymorphism
        this.addChild = function (name, type) {
            var ans = new DropDownNode(name, that.renderApi, type);
            this.rootNodes.push(ans);
            return ans;
        };
       
        
    }
    return DropDownTree;
});