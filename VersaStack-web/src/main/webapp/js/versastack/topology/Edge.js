"use strict";
define([], function () {
    function Edge(left, right) {
        this.left = left;
        this.right = right;

        this.source = left;
        this.target = right;

        this._isProper = function () {
            var ans = true;
            var leftCursor=this.left;
            var rightCursor=this.right;
            while (!leftCursor.getVisible()) {
                leftCursor = leftCursor._parent;
            }
            while (!rightCursor.getVisible()) {
                rightCursor = rightCursor._parent;
            }
            this.source = leftCursor;
            this.target = rightCursor;
            ans = ans && (this.source.uid < this.target.uid);
            return ans;
        };
    }
    return Edge;
});