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
    "local/versastack/utils"
], function (utils) {
    var map_ = utils.map_;
    var isURL = utils.isURL;
    function DropDownNode(name, renderApi, type, data, contextMenu, outputApi) {
        /**@type Array.DropDownNode**/
        this.children = [];
        this.name = name;
        this.renderApi = renderApi;
        this.contextMenu = contextMenu;
        this.type = type;
        this.outputApi = outputApi;
        // data associated with the node
        // so for example, the element or the key, value pair. 
        // { key: "key", value: "value", type : "type" }
        this.dataObject = data;
        
        var that = this;

        this.addChild = function (name, type, data) {
            var ans = new DropDownNode(name, that.renderApi, type, data, that.contextMenu, that.outputApi); // changed this.renderApi to that.renderApi
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
     
            if (that.name.substring(0,3) === "urn" || that.name.substring(0,2) === "x-") { 
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
            } else if (that.name.substring(0,3) === "urn") {
                var bullet = document.createElement("span");
                bullet.innerHTML = "•";
                if (that.type !== "Relationship") bullet.style.float = "left";
                line.appendChild(bullet);
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
//                    map_(that.children, function (child) {
//                        console.log("only on click");
//                        var toAdd = child.getHTML();
//                        toAdd.style.display = isExpanded ? "inherit" : "none";
//                        childNodes.push(toAdd);
//                        content.appendChild(toAdd);
//                    });                    
                };            
            }
            
            if (that.type === "Type") {
                text.style = "font-style:italic";
                line.appendChild(text);
                content.appendChild(line);
            } else if (that.type === "Title") {
                text.style = "font-weight:bold;font-style:italic;";
                line.appendChild(text);
                content.appendChild(line);
            } else if (that.type === "Separator") {
                var horiz =  document.createElement("hr"); 
                horiz.style = "font-weight:bold;margin-top: 5px;margin-bottom: 5px;border-top: 1px solid #333;";
                line.appendChild(horiz);
                content.appendChild(line);
            } else if (((that.name.substring(0,3) === "urn") ||
                        (that.name.substring(0,2) === "x-" )) && that.type !== "Relationship") {
                var link = document.createElement("div");
                link.className = "urnLink";
                //link.setAttribute("href", "");
                link.onclick = function () {
                   link.className = "urnLink clicked";
                   if (that.renderApi !== null && that.renderApi !== undefined){
                        console.log(" what is this: " + that.renderApi);
                        that.renderApi.clickNode(that.name, that.type, that.outputApi);
                        
                    }
                };
                link.oncontextmenu  = function (e) {
                   that.contextMenu.panelElemContextListener(e, that.dataObject);
                };
                link.appendChild(text);
                line.appendChild(link);
                content.appendChild(line);
            } else if ( (that.name.substring(0,3) === "urn") ||
                        (that.name.substring(0,2) === "x-" )&& that.type === "Relationship") {
                // copy paste below and above to get desired result 
                var property =  that.name.split("(*)");                
                var link = document.createElement("span");
                link.className = "urnLink";
                //link.setAttribute("href", "");
                link.onclick = function () {
                   link.className = "urnLink clicked";
                   console.log(" what is this: " + that.renderApi);
                   that.renderApi.clickNode(property[0], that.type, that.outputApi);          
                };                
                link.innerHTML = property[0];
                
                var key = document.createElement("span");
                var value = document.createElement("span");               
                value.className = "panelElementProperty";
                key.appendChild(link);
                value.innerHTML = " : " + property[1]; // just using this because it's there 
               
                line.appendChild(key);
                line.appendChild(value);
                content.appendChild(line);
            } else if (that.type === "Property") {
                var key = document.createElement("span");
                var value = document.createElement("span");               
                key.className = "panelElementProperty";
                var property =  that.name.split(":");
                
                if (property[0] !== 'format ' && property[0] !== 'value ' && property[1].substring(0) !== "{"
                        && !isURL(that.dataObject)) {
                    key.innerHTML = property[0] + ":";
                    value.innerHTML = property[1]; // just using this because it's there 
                } else {
                    key.innerHTML = property[0] + ":";
                    property.splice(0,1);
                    value.innerHTML = that.dataObject;
                }
                 line.oncontextmenu  = function (e) {
                   that.contextMenu.panelElemContextListener(e, that.dataObject);
                };
               
                line.appendChild(key);
                line.appendChild(value);
                content.appendChild(line);
            } else {
                line.appendChild(text);
                content.appendChild(line);
            }

            // this was a new changed, forgot what the point of htis was ... @
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

