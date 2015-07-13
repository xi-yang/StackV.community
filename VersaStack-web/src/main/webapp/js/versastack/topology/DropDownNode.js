"use strict";
define([
    "local/versastack/utils"
    ], function (utils) {
        var map_=utils.map_;
    function DropDownNode(name) {
        this.children =[];
        this.name=name;
        var that=this;
        
        this.addChild=function(name){
            var ans = new DropDownNode(name);
            this.children.push(ans);
            return ans;
        };
        
        var isExpanded=false;
        
        function _getText(){
            var ans="";
            if(that.children.length!==0){
                ans+=isExpanded?"▼":"▶";
            }else{
                ans+=" ";
            }
            ans+=that.name;
            return ans;
        }
        
        this.getHTML =function(){
           var ans=document.createElement("div");
           
           var content =document.createElement("div");
           content.className="treeMenu";
           var text =document.createElement("div");
           text.innerText=_getText();
           var childNodes=[];
           text.onclick = function(){
               isExpanded = !isExpanded;
               
               var disp=isExpanded?"inherit":"none";
               map_(childNodes,function(child){
                   child.style.display=disp;
               });
               text.innerText=_getText();
           };
           content.appendChild(text);
           
           map_(this.children, function(child){
               var toAdd=child.getHTML();
               toAdd.style.display=isExpanded?"inherit":"none";
               childNodes.push(toAdd);
               content.appendChild(toAdd);
           });
           ans.appendChild(content);
           
           return ans;
           
        };
    }
    return DropDownNode;
});

