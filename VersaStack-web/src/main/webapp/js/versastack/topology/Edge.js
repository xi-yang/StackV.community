"use strict";
define([], function () {
    /**
     * 
     * @param {Port} leftPort
     * @param {Port} rightPort
     */
    function Edge(leftPort, rightPort) {
        /**@type Port**/
        this.leftPort = leftPort;
        /**@type Port**/
        this.rightPort = rightPort;

        this.source = null;
        this.target = null;

        this.svgNode = null;
        this.svgLeadLeft = null;
        this.svgLeadRight = null;
        this.edgeType = null;
        
        this._isProper = function () {
            if (this.leftPort.getType() !== "Port" && this.rightPort.getType() !== "Port") {
                this.source = this.leftPort;
                this.target = this.rightPort;
                return true;
            }
            if (!this.leftPort) {
                console.log("Left Port Missing!");
                return false;
            } else if (!this.rightPort) {
                console.log("Right Port Missing!");
                return false;
            } else {
                var ans = true;
                var leftCursor = this.leftPort.ancestorNode;
                var rightCursor = this.rightPort.ancestorNode;
                if (!leftCursor) {
                    console.log("Left Cursor Missing!");
                    return false;
                } else if (!rightCursor) {
                    console.log("Right Cursor Missing!");
                    return false;
                } else {
                    while (!leftCursor.getVisible()) {
                        leftCursor = leftCursor._parent;
                    }
                    while (!rightCursor.getVisible()) {
                        rightCursor = rightCursor._parent;
                    }
                    this.source = leftCursor;
                    this.target = rightCursor;
                    ans = ans && (this.source.uid < this.target.uid);

                    if (leftPort.getVisible()) {
                        this.source = leftPort;
                    }
                    if (rightPort.getVisible()) {
                        this.target = rightPort;
                    }
                    return ans;
                }
            }
        };

        this.getType = function() {
            return "Edge";
        }

    }
    return Edge;
});