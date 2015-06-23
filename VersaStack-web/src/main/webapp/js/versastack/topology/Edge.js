"use strict"
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
            while (!leftCursor.isVisible) {
                leftCursor = leftCursor._parent;
            }
            while (!rightCursor.isVisible) {
                rightCursor = rightCursor._parent;
            }
            ans &= this.left.uid < this.right.uid;
            this.source = left;
            this.target = right;
            return ans;
        };
    }
    return Edge;
});