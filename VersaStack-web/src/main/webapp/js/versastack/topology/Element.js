"use strict";
define(["local/versastack/topology/modelConstants"], function (values) {
    function Element(backing, map) {
        this.svgNode = null;
        this.svgNodeText = null;
        this.svgNodeCover = null; //To prevent the cursor from changing when we mouse over the text, we draw an invisible rectangle over it
        this._backing = backing;
        this._map = map;
        /**@type Array.Port**/
        this.ports = [];
        this.hello = "hello";
        //We are reloading this port from a new model
        //Model.js will handle most of the reparsing, but we need to
        //clear out some old data
        
        this.reload = function (backing, map) {
            this._backing = backing;
            this._map = map;
        };
        this.getName = function () {
            return this._backing.name;

        };
        /*
        this.getNameBrief = function () {
            return this.getName().split(":").slice(-1).pop();
        };*/

        // refine this 
        this.getType = function () {
           
            var types = this._backing[values.type];
            console.log("types of : " + this.getName());
            
            //alert ("types = " + Object.keys(this._backing));
            // remove named indivdual from this 
            /*var index = types.indexOf(values.namedIndividual);
            if (index > -1)
                types.splice(index, 1);*/
            
            var arr = map_(types, function (type) {
                            type = type.value;
                            
                            console.log("-  " + type + "\n");
                            return type; 
                        });
            //alert("types of " + this.name + ": " + arr.toString());
            
            var index = arr.indexOf("http://www.w3.org/2002/07/owl#NamedIndividual");
            console.log ("index: " + index);
            if (index > -1)
                arr.splice(index, 1);
            console.log ("true type: " + arr[0] + "\n");
            return arr[0];
        };
        
        this.populateTreeMenu = function (tree) {
            var container = tree.addChild(backing.name);

            if (this.ports.length > 0) {
                var portSubnet = container.addChild("Ports");
                map_(this.ports, function (port) {
                    port.populateTreeMenu(portSubnet);
                });
            }
        };
        
                //console.log("Name: " + this.getName() );

    }
    return Element;
});