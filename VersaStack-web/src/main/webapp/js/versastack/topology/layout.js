"use strict";
define([
    "local/d3"
], function (d3) {
    var force;

    function doLayout(nodes, edges, width, height) {
        force = d3.layout.force()
                .nodes(nodes)
                .links(edges)
                .size([width, height])
                .linkStrength(0.1)
                .friction(0.9)
                .linkDistance(20)
                .charge(-30)
                .gravity(0.1)
                .theta(0.8)
                .alpha(0.1)
                .start();
        for(var i=0; i<100; i++){
            force.tick();
        }
        force.stop();
        
    }
    
    function stop(){
        force.stop();
    }

    /** PUBLIC INTERFACE **/
    return {
        doLayout: doLayout,
        stop: stop
    };
    /** END PUBLIC INTERFACE **/
});