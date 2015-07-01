"use strict";
define([
    "local/versastack/utils",
    "local/versastack/topology/modelConstants"
    ],function(utils,values){
        var map_=utils.map_;
    function Port(backing,map){
        var that=this;
        this._backing=backing;
        
        this.childrenPorts=[];
        var children=backing[values.hasBidirectionalPort]
        if(children){
            map_(children,function(child){
                child=map[child.value];
                child=new Port(child,map);
                that.childrenPorts.push(child);
            });
        }
        
        
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