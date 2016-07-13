"use strict";
define(["local/versastack/topology/modelConstants"], function (values) {
    function Subnet(backing, map) {
        this.svgNode = null;
        this.svgNodeText = null;
        this.svgNodeCover = null; //To prevent the cursor from changing when we mouse over the text, we draw an invisible rectangle over it
        this._backing = backing;
        this._map = map;
        /**@type Array.Port**/
        this.ports = [];
        this.detailsReference = false;
        
        //We are reloading this port from a new model
        //Model.js will handle most of the reparsing, but we need to
        //clear out some old data
        this.reload = function (backing, map) {
            this._backing = backing;
            this._map = map;
            this._ports = [];
        };
        this.getName = function () {
            return this._backing.name;

        };
        this.getNameBrief = function () {
            return this.getName().split(":").slice(-1).pop();
        };

        this.getType = function () {
            return "Subnet";
        };
    }
    return Subnet;
});
