"use strict";
var forceGlobal;

define([
    "local/d3",
    "local/versastack/utils",
    "local/versastack/topology/Edge"
], function (d3,utils,Edge) {
    var map_=utils.map_;
    var force;
    
    //If we are simply updating the model, we want to lock pre-existing elements in place,
    //While posisitoning new elements.
    function doLayout(model, posistionLock, width, height) {
        var nodes=model.listNodes();
        var edges=model.listEdges();
        //To encourage topologies to clump, we add edges between topolgies and 
        //their children
        map_(nodes,/**@param {Node} n**/function(n){
            map_(n.children,function(child){
                edges.push({source:n,target:child});
            });
        });
        
        force = d3.layout.force()
                .nodes(nodes)
                .links(edges)
                .size([width, height])
                .linkStrength(10)
                .friction(0.9)
                .linkDistance(10)
                .charge(-1000)
                .gravity(.5)
                .theta(0.8)
                .alpha(0.1);
        if(posistionLock){
            force.on("tick",function(){
               map_(nodes,function(node){
                   var pos=posistionLock[node.getName()];
                   if(pos){
                       node.x=pos.x;
                       node.y=pos.y;
                   }
               }) 
            });
        }
        force.start();
        for(var i=0; i<100; i++){
            force.tick();
        }
        force.stop();
        forceGlobal=force;
        
    }
    
    function stop(){
        force.stop();
    }

    function tick(){
        force.start();
        for(var i=0; i<1; i++){
            force.tick();
        }
        force.stop();
    }

    /** PUBLIC INTERFACE **/
    return {
        doLayout: doLayout,
        stop: stop,
        tick: tick,
        //Debug functions
        force: function(){return force;}
    };
    /** END PUBLIC INTERFACE **/
});