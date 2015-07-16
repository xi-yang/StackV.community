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

        //We render the edges in multiple layers, so there are multiple svgNodes
        this.svgNodes = [];

        this._isProper = function () {
            var ans = true;
            var leftCursor = this.leftPort.ancestorNode;
            var rightCursor = this.rightPort.ancestorNode;
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
        };
        
        
    }
    return Edge;
});