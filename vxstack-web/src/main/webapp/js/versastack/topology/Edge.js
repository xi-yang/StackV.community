/*
 * Copyright (c) 2013-2016 University of Maryland
 * Modified by: Antonio Heard 2016

 * Permission is hereby granted, free of charge, to any person obtaining a copy 
 * of this software and/or hardware specification (the “Work”) to deal in the 
 * Work without restriction, including without limitation the rights to use, 
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of 
 * the Work, and to permit persons to whom the Work is furnished to do so, 
 * subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Work.

 * THE WORK IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL 
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
 * FROM, OUT OF OR IN CONNECTION WITH THE WORK OR THE USE OR OTHER DEALINGS  
 * IN THE WORK.
 */

"use strict";
define(["local/versastack/utils"], function (utils) {
    /**
     * 
     * @param {Port} leftPort
     * @param {Port} rightPort
     */
    var map_ = utils.map_;
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
