"use strict";
define(["local/versastack/topology/modelConstants"], function (values) {
    function Element(backing, map) {
        this.svgNode = null;
        this.svgNodeText = null;
        this.svgNodeCover = null; //To prevent the cursor from changing when we mouse over the text, we draw an invisible rectangle over it
        this._backing = backing;
        this._map = map;
        this.misc_elements = [];
//        /**@type Array.Port**/
//        this.ports = [];
//        this.services = [];
//        this.children = [];
//        this.misc_elements = [];
        
        this.relationship_to = []; // relationship with owner 
        
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
            //console.log("types of : " + this.getName());
            
            //alert ("types = " + Object.keys(this._backing));
            // remove named indivdual from this 
            /*var index = types.indexOf(values.namedIndividual);
            if (index > -1)
                types.splice(index, 1);*/
            
            var arr = map_(types, function (type) {
                            type = type.value;
                            
                           // console.log("-  " + type + "\n");
                            return type; 
                        });
            //alert("types of " + this.name + ": " + arr.toString());
            
            var index = arr.indexOf("http://www.w3.org/2002/07/owl#NamedIndividual");
            //console.log ("index: " + index);
            if (index > -1)
                arr.splice(index, 1);
            //console.log ("true type: " + arr[0] + "\n");
            return arr[0].split("#")[1];
        };
        
        this.populateTreeMenu = function (tree) {
                  var root = tree.addChild(this.getName(), "Element");
//            map_(this.childrenPorts, function (child) {
//                child.populateTreeMenu(root);
//            });
      
//            if (this.services.length > 0) {
//                var serviceNode = tree.addChild("hasService", '"');
//                map_(this.services, function (service) {
//                    service.populateTreeMenu(serviceNode);
//                })
//            }
//            if (this.ports.length > 0) {
//                var portsNode = tree.addChild("hasBidirectionalPort", "");
//                map_(this.ports, function (port) {
//                    port.populateTreeMenu(portsNode);
//                });
//            }
//            if (this.children.length > 0) {
//                var childrenNode = tree.addChild("hasNode", "");
//                map_(this.children, function (child) {
//                    var childNode = childrenNode.addChild(child.getName(), "Node");
//                    child.populateTreeMenu(childNode);
//                });
//            }
//
            if (this.misc_elements.length > 0) {
                var displayed = [];
               // alert(this.misc_elements);
                for (var i = 0; i < this.misc_elements.length; i++){
                    var el = this.misc_elements[i];
                    //alert("el.getName: " + el.getName() + 
                           // alert(" helllo: " + el.hello);
                    if (displayed.indexOf(el) === -1 && el.getName() !== undefined) {
                        var type = el.relationship_to[this];
                        //type = type.split("#");
                        var elementsNode = tree.addChild(type === undefined?"undefined":type, "");
                        var other_elms = [];
                        for (var o in this.misc_elements) {
                            if (displayed.indexOf(this.misc_elements[o]) === -1 && 
                                    this.misc_elements[o].relationship_to[this] === type
                                    && this.misc_elements[o].getName() !== undefined) {
                                other_elms.push(this.misc_elements[o]);
                                //console.log ("name of thing: " + this.misc_elements[o].getName());
                                elementsNode.addChild(this.misc_elements[o].getName(), "Element");;
                                displayed.push(this.misc_elements[o]);
                            }
                        }
                    }
                }


            }
        };
        

    };
    return Element;
});