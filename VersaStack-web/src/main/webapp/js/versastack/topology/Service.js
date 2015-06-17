"use strict";
define(["local/versastack/topology/modelConstants"], function (values) {
    function Service(backing) {
        this._backing = backing;
        this.type = "";

        this.getTypeBrief = function () {
            return this.type.split("#")[1];
        };
        //Initialization
        //get the type
        var types = this._backing[values.type];

        var that = this;
        map_(types, function (type) {
            type = type.value;
            if (type === values.namedIndividual) {
                return;
            }
            that.type = type;
        });
    }
    return Service;
});