"use strict";
define([], function () {
    function Edge(leftPort, rightPort) {
        this.leftPort = leftPort;
        this.rightPort = rightPort;

        this.source = null;
        this.target = null;

        this.svgNode = null;

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

            if (leftPort.isVisible) {
                this.source = leftPort;
            }
            if (rightPort.isVisible) {
                this.target = rightPort;
            }
            return ans;
        };
    }
    return Edge;
});