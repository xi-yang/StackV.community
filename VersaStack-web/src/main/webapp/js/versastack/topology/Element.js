"use strict";
define(["local/versastack/topology/modelConstants"], function (values) {
    function Element(backing, map, elementMap) {
        this.svgNode = null;
        this.svgNodeText = null;
        this.svgNodeCover = null; //To prevent the cursor from changing when we mouse over the text, we draw an invisible rectangle over it
        this._backing = backing;
        this._map = map;
        this.topLevel = true;
        this.elementMap = elementMap;
        this.misc_elements = [];
        
        this.relationship_to = []; // relationship with owner 
        var that = this;
        
        //We are reloading this element  from a new model
        //Model.js will handle most of the reparsing, but we need to
        //clear out some old data
        this.reload = function (backing, map) {
            this._backing = backing;
            this._map = map;
            this.elementMap = null;
            this.misc_elements = [];
            this.relationship_to = []; // relationship with owner 
        };
        this.getName = function () {
            return this._backing.name;

        };
        /*
        this.getNameBrief = function () {
            return this.getName().split(":").slice(-1).pop();
        };*/

        this.getType = function () {
           
            var types = this._backing[values.type];
            
            var arr = map_(types, function (type) {
                type = type.value;

                return type; 
             });
            
            var index = arr.indexOf("http://www.w3.org/2002/07/owl#NamedIndividual");
            if (index > -1)
                arr.splice(index, 1);
            return arr[0].split("#")[1];
        };
        
        this.populateProperties = function (tree) {
            for (var key in this._backing) {
                             if (key === "name") continue;

                var elements = this._backing[key];
                map_(elements, function (element){
                    var errorVal = element.value;
                    if (errorVal.substring(0,3) === "urn" || errorVal.substring(0,2) === "x-") return 0;

                    var name = key.split("#")[1];
                        if (key === "http://www.w3.org/1999/02/22-rdf-syntax-ns#type"){
                            if (errorVal === "http://www.w3.org/2002/07/owl#NamedIndividual")
                                return 0;
                            else if (errorVal.indexOf("#") !== -1)
                                errorVal = errorVal.split("#")[1];
                        }
                        tree.addChild(key.split("#")[1] + " : " + errorVal, "Property", errorVal);
                    }
                ); 
            }            

        };
        
        this.populateTreeMenu = function (tree) {
            //console.log("~~~~~~~~~~~~\nElement tree debuggins: "  + this.getName());
           // tree.addChild("", "Separator");
            
            if (this.misc_elements.length > 0) {
                //console.log("I have more than one element. No of elements: " ); //+ this.misc_elements.length);
                
                var displayed = [];
               // alert(this.misc_elements);
                for (var i = 0; i < this.misc_elements.length; i++){
                    
                    var el = this.misc_elements[i];
                    //console.log ("my name in loop 1: " + el.getName());

 //                   console.log("el.getName: " + el.getName() + 
                           // alert(" helllo: " + el.hello);
                    if (displayed.indexOf(el) === -1 && el.getName() !== undefined) {
                        var type = el.relationship_to[this.getName()];
                        //type = type.split("#");
                        var elementsNode = tree.addChild(type === undefined?"undefined":type, "Type", null);
                        //displayed.push(el);
                        for (var o in this.misc_elements) {
                            //if(this.misc_elements[o].misc_elements.indexOf(this) !== -1) continue;
//                            console.log("I got this far");
                            if (displayed.indexOf(this.misc_elements[o]) === -1 && 
                                    this.misc_elements[o].relationship_to[this.getName()] === type
                                    && this.misc_elements[o].getName() !== undefined) {
                                //console.log ("name of thing: " + this.misc_elements[o].getName());
                                var elementNode = elementsNode.addChild(this.misc_elements[o].getName(), "Element", this.misc_elements[o]);
//                                                            console.log("I got this far789789");

                                displayed.push(this.misc_elements[o]);
                                // Done to stop infinite calls to propulateTreeMenu if an element
                                // has a relationship with an element directly lower in the hierarchy. 
                                if(this.misc_elements[o].misc_elements.indexOf(this) === -1) {
                                    this.misc_elements[o].populateTreeMenu(elementNode);
                                } 
                            }
                        }
                    }
                }

                //console.log("End element debugging\n ~~~~~~~~~~~ " + this.getName());
            }
            
        };
        
        this.showRelationships = function(tree) {
            if (Object.keys(this.relationship_to).length > 0) {
                tree.addChild("", "Separator", null);
                tree.addChild("Element Used By", "Title", null);

                Object.keys(this.relationship_to).forEach(function (key) {
                    tree.addChild(key + "(*)" + that.relationship_to[key], "Relationship", null);   
                });   
            }
        };
    };
    return Element;
});