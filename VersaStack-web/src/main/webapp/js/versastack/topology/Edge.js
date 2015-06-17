"use strict"
define([], function () {
    function Edge(left, right) {
        this.left = left;
        this.right = right;

        this.source = left;
        this.target = right;

        this._isProper = function () {
            var ans = true;
            while (!this.left.isVisible) {
                this.left = this.left.parent;
            }
            while (!this.right.isVisible) {
                this.right = this.right.parent;
            }
            ans &= this.left.uid < this.right.uid;
            this.source = left;
            this.target = right;
            return ans;
        };
    }
    return Edge;
});