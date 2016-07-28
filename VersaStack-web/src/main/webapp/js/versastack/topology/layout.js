"use strict";
var forceGlobal;

define([
    "local/d3",
    "local/versastack/utils",
    "local/versastack/topology/Edge"
], function (d3, utils, Edge) {
    var map_ = utils.map_;
    var force;

    //If we are simply updating the model, we want to lock pre-existing elements in place,
    //While posisitoning new elements.
    function doLayout(model, lockNodes, width, height) {
        var nodes = model.listNodes();
        var edges = model.listEdges();
        
        // lay out policies as nodes ...
        var policies = model.listPolicies();
        for (var i in policies) nodes.push(policies[i]);
        
        // same with policy edges
        var polEdges = model.policyEdges;
        for (var i in polEdges) {
            polEdges[i]._isProper();
            if (polEdges[i]) {
                edges.push(polEdges[i]);
            }
        } 
                
        if (lockNodes) {
            map_(lockNodes, function (node) {
                if (node.isLeaf()) {
                    node.fixed = true;
                    node.px = node.x;
                    node.py = node.y;
                }
            });
        }

        //To encourage topologies to clump, we add edges between topolgies and 
        //their children
        map_(nodes, /**@param {Node} n**/function (n) {
            map_(n.children, function (child) {
                edges.push({source: n, target: child});
            });
        });
        force = d3.layout.force()
                .nodes(nodes)
                .links(edges)
                .size([width, height])
                .linkStrength(10)
                .friction(0.9)
                .linkDistance(
                    function(d) { 
                        if (d.source.getType() === "Policy" || d.target.getType() === "Policy") {
                            return 120;
                        } else {
                            return 10;
                        }
                    }     
                )
                .charge(-1000) 
                .gravity(0.5)
                .theta(0.8)
                .alpha(0.1);
        force.on("tick", function () {
            //Make a nodes coordinates equal to its center of mass.
            //This is significant for topologies
            map_(nodes, function (node) {
                var choords = node.getCenterOfMass();
                // recenters nodes if they are out of bounds. 
                if (choords.x > width || choords.x < 0) choords.x = width / 2; 
                if (choords.y > height || choords.y < 0) choords.y = height / 2; 
                node.x = choords.x;
                node.y = choords.y;
                if (isNaN(choords.x) || isNaN(choords.y)) console.log("It's NAN in doLayout\n");    
            });
        });
        force.start();
        for (var i = 0; i < 100; i++) {
            force.alpha(0.1).tick();
        }
        force.stop();
        forceGlobal = force;

    }

    function stop() {
        force.stop();
    }

    function tick() {
        force.start();
        for (var i = 0; i < 1; i++) {
            force.tick();
        }
        force.stop();
    }

    function testLayout(model, lockNodes, width, height) { //@
        var nodes = model.listNodes();
        var edges = model.listEdges();

        // lay out policies as nodes ...
        var policies = model.listPolicies();
        for (var i in policies) nodes.push(policies[i]);
        
        // same with policy edges
        var polEdges = model.policyEdges;
        for (var i in polEdges) {
            polEdges[i]._isProper();
            if (polEdges[i]) {
                edges.push(polEdges[i]);
            }
        } 
        
        if (lockNodes) {
            map_(lockNodes, function (node) {
                if (node.isLeaf()) {
                    node.fixed = true;
                    node.px = node.x;
                    node.py = node.y;
                }
            });
        }

        //To encourage topologies to clump, we add edges between topolgies and 
        //their children
        map_(nodes, /**@param {Node} n**/function (n) {
            map_(n.children, function (child) {
                edges.push({source: n, target: child});
            });
        });
        force = d3.layout.force()
                .nodes(nodes)
                .links(edges)
                .size([width, height])
                .linkStrength(10)
                .friction(0.5)
                .linkDistance(
                    function(d) { 
                        if (d.source.getType() === "Policy" || d.target.getType() === "Policy") {
                            return 120;
                        } else {
                            return 10;
                        }
                    }     
                )
                .charge(-700)
                .gravity(1)
                .theta(0.8)
                .alpha(0.1);
        force.on("tick", function () {
            //Make a nodes coordinates equal to its center of mass.
            //This is significant for topologies
            map_(nodes, function (node) {
                var choords = node.getCenterOfMass();
                // recenters nodes if they are out of bounds. 
                if (choords.x > width || choords.x < 0) choords.x = width / 2; 
                if (choords.y > height || choords.y < 0) choords.y = height / 2; 
                node.x = choords.x;
                node.y = choords.y;
                if (isNaN(choords.x) || isNaN(choords.y)) console.log("It's NAN in doLayout\n");    
            });
        });
        force.start();
        for (var i = 0; i < 100; i++) {
            force.alpha(0.1).tick();
        }
        force.stop();
        forceGlobal = force;

    }

    /** PUBLIC INTERFACE **/
    return {
        doLayout: doLayout,
        stop: stop,
        tick: tick,
        //Debug functions
        force: function () {
            return force;
        },
        testLayout: testLayout
    };
    /** END PUBLIC INTERFACE **/
});
