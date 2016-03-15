"use strict";
define([
    "local/versastack/utils"
], function (utils) {
    var map_ = utils.map_;
    function DropDownNode(name, renderApi, type) {
        /**@type Array.DropDownNode**/
        this.children = [];
        this.name = name;
        this.renderApi = renderApi;
        this.type = type;
        
        var that = this;

        this.addChild = function (name, type) {
            var ans = new DropDownNode(name, this.renderApi, type);
            this.children.push(ans);
            return ans;
        };

        var isExpanded = false;

        function _getText() {
            var ans = "";
            if (that.children.length !== 0) {
               // ans += isExpanded ? "▼" : "▶";
            } /*else {
                ans += "&nbsp;&nbsp;&nbsp;"; //space literal
            }*/
            ans += that.name;
            return ans;
        }

        this.getHTML = function () {
            var ans = document.createElement("div");

            var content = document.createElement("div");
            content.className = "treeMenu";
            
            var line = document.createElement("div");
            //line.style.textAlign = "center";
            //       console.log("that.name: " + that.name);
            //console.log("that.type: " + that.type);
     
            if (that.name.substring(0,3) === "urn") { 
                var text = document.createElement("a");
                text.className = "urnLink";
            } else {
                var text = document.createElement("div");
            }
    
            if (that.children.length !== 0) {
                var drop = document.createElement("span");
                drop.innerHTML = isExpanded ? "▼" : "▶";
                drop.style.float = "left";         
                drop.className = "dropDownArrow";
                line.appendChild(drop);
            }
            
            text.innerHTML = _getText();
            var childNodes = [];
//            text.onclick = function () {
//                isExpanded = !isExpanded;
//
//                var disp = isExpanded ? "inherit" : "none";
//                map_(childNodes, function (child) {
//                    child.style.display = disp;
//                });
//                text.innerHTML = _getText();
//            };
            if (that.children.length !== 0) {

            drop.onclick = function () {
                isExpanded = !isExpanded;
                drop.innerHTML = isExpanded ? "▼" : "▶";
                var disp = isExpanded ? "inherit" : "none";
                map_(childNodes, function (child) {
                    child.style.display = disp;
                });
                text.innerHTML = _getText();
            };            
        }
            if (that.name.substring(0,3) === "urn") {
                var link = document.createElement("div");
                link.className = "urnLink";
                //link.setAttribute("href", "");
                link.onclick = function () {
                   link.className = "urnLink clicked";
                   if (that.renderApi !== null && that.renderApi !== undefined){
                        console.log(" what is this: " + that.renderApi);
                        that.renderApi.clickNode(that.name, that.type);
                        
                    }
                };
                link.appendChild(text);
                line.appendChild(link);
                content.appendChild(line);
            } else if (that.type === "Property") {
                var key = document.createElement("span");
                var value = document.createElement("span");               
                key.className = "panelElementProperty";
                var property =  that.name.split(":");
                key.innerHTML = property[0] + ":";
                value.innerHTML = property[1]; // just using this because it's there 
                line.appendChild(key);
                line.appendChild(value);
                content.appendChild(line);
            } else {
                line.appendChild(text);
                content.appendChild(line);
            }

            map_(this.children, function (child) {
                var toAdd = child.getHTML();
                toAdd.style.display = isExpanded ? "inherit" : "none";
                childNodes.push(toAdd);
                content.appendChild(toAdd);
            });
            ans.appendChild(content);

            return ans;

        };
    }
    return DropDownNode;
});

