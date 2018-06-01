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
define(["./modelConstants", "../utils"], function (values, utils) {
    var map = utils.map_;
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
