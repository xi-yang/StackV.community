"use strict";
var forceGlobal;

define([
    "local/d3",
    "local/versastack/utils",
    "local/versastack/topology/Edge"
], function (d3,utils,Edge) {
    var map_=utils.map_;
    var force;
    
    function doLayout(nodes, edges, width, height) {
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
                .alpha(0.1)
                .start();
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