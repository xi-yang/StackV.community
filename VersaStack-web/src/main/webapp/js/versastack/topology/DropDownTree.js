"use strict";
define([
    "local/versastack/utils",
    "local/versastack/topology/DropDownNode"
    ], function (utils,DropDownNode) {
    function DropDownTree(containerDiv) {
        var map_=utils.map_;
        this.rootNodes = [];
        this.containerDiv=containerDiv;
        
        var that=this;
        this.clear=function(){
            this.rootNodes=[];
            utils.deleteAllChildNodes(this.containerDiv);
        };
        
        this.draw = function(containerDiv){
            utils.deleteAllChildNodes(this.containerDiv);
            map_(this.rootNodes,/**@param {DropDownNode} node**/function(node){
                var toAppend=node.getHTML();
                //Every node automatically indents itself.
                //In the case of the root node, this is undesired, so we apply 
                //an opposite indent to counteract it.
//                toAppend.style.marginLeft="-15px";
                that.containerDiv.appendChild(toAppend);
            });
        };
        
        //We use the same method name as DropDownNode.addChild to enable polymorphism
        this.addChild=function(name){
            var ans=new DropDownNode(name);
            this.rootNodes.push(ans);
            return ans;
        }

    }
    return DropDownTree;
});