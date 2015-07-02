"use strict";
define([
    "local/versastack/utils",
    "local/versastack/topology/Edge",
    "local/versastack/topology/modelConstants"
    ],function(utils,Edge,values){
        var map_=utils.map_;
    function Port(backing,map){
        var that=this;
        this._backing=backing;
        this.childrenPorts=[];
        this.parentPort = null;
        this.ancestorNode=null;
        this.alias=null;
        
        var children=backing[values.hasBidirectionalPort]
        if(children){
            map_(children,function(child){
                child=map[child.value];
                child=new Port(child,map);
                that.childrenPorts.push(child);
            });
        }
        
        //return all the edges involving this port, or its decendents
        this.getEdges=function(){
            var ans=[];
            if(this.alias){
                ans.push(new Edge(this,this.alias));
            }
            map_(this.childrenPorts,function(child){
                ans=ans.concat(child.getEdges());
            });
            return ans;
        };
        
        this.setNode=function(node){
            this.ancestorNode=node;
            map_(this.childrenPorts,function(child){
                child.setNode(node);
            });
        };
        
        this.getName=function(){
            return backing.name;
        };
        this.populateTreeMenu=function(tree){
            var root=tree.addChild(this.getName());
            map_(this.childrenPorts,function(child){
                child.populateTreeMenu(root);
            });
        };
    }
    
    return Port;
});