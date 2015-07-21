"use strict";
define(["local/versastack/topology/modelConstants"], function (values) {
    function Subnet(backing, map) {
        this.svgNode = null;
        this.svgNodeText = null;
        this.svgNodeCover = null; //To prevent the cursor from changing when we mouse over the text, we draw an invisible rectangle over it
        this._backing = backing;

        /**@type Array.Port**/
        this.ports = [];
        this.getName = function () {
            return this._backing.name;
        };
        this.getNameBrief = function(){
            return this.getName().split(":").slice(-1).pop();
        }
        this.populateTreeMenu = function (tree) {
            var container = tree.addChild(backing.name);

            if (this.ports.length > 0) {
                var portSubnet = container.addChild("Ports");
                map_(this.ports, function (port) {
                    port.populateTreeMenu(portSubnet);
                });
            }
        };
    }
    return Subnet;
});