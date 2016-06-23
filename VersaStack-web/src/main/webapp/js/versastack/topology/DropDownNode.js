"use strict";
define([
    "local/versastack/utils"
], function (utils) {
    var map_ = utils.map_;
    function DropDownNode(name, renderApi, type, data, contextMenu) {
        /**@type Array.DropDownNode**/
        this.children = [];
        this.name = name;
        this.renderApi = renderApi;
        this.contextMenu = contextMenu;
        this.type = type;
        // data associated with the node
        // so for example, the element or the key, value pair. 
        // { key: "key", value: "value", type : "type" }
        this.dataObject = data;
        
        var that = this;

        this.addChild = function (name, type, data) {
            var ans = new DropDownNode(name, that.renderApi, type, data, that.contextMenu); // changed this.renderApi to that.renderApi
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
            } else if (that.name.substring(0,3) === "urn" && that.type !== "Relationship") {
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
                link.oncontextmenu  = function (e) {
                   that.contextMenu.panelElemContextListener(e, that.dataObject);
                };
                link.appendChild(text);
                line.appendChild(link);
                content.appendChild(line);
            } else if (that.name.substring(0,3) === "urn" && that.type === "Relationship") {
                // copy paste below and above to get desired result 
                var property =  that.name.split("(*)");                
                var link = document.createElement("span");
                link.className = "urnLink";
                //link.setAttribute("href", "");
                link.onclick = function () {
                   link.className = "urnLink clicked";
                   console.log(" what is this: " + that.renderApi);
                   that.renderApi.clickNode(property[0], that.type);          
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
                key.innerHTML = property[0] + ":";
                value.innerHTML = property[1]; // just using this because it's there 
                
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

