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
define([
    "local/versastack/utils",
    "local/versastack/topology/modelConstants"],
        function (utils, values) {
            var map_ = utils.map_;
            function Service(backing, map) {
                var that = this;
                this._backing = backing;
                this._map = map;
                this.type = "";
                this.svgNode = null;
                this.x = 0;
                this.y = 0;
                this.dy = 0;
                this.dx = 0;
                this.size = 0;
                /**@type Array.subnet**/
                this.subnets = [];
                this._map = map;
                this.detailsReference = false;
                
                this.misc_elements = [];
                
                //We are reloading this port from a new model
                //Model.js will handle most of the reparsing, but we need to
                //clear out some old data
                this.reload = function (backing, map) {
                    this._backing = backing;
                    this._map = map;
                    this.type = "";
                    this.subnets = [];
                    init();
                };
                this.getTypeBrief = function () {
                    return this.type.split("#")[1];
                };

                function init() {
                    //get the type
                    var types = that._backing[values.type];

                    map_(types, function (type) {
                        type = type.value;
                        if (type === values.namedIndividual) {
                            return;
                        }
                        that.type = type;
                    });
                }
                init();

                this.getCenterOfMass = function () {
                    return {x: this.x, y: this.y};
                };

                this.getIconPath = function () {
                    var prefix = "/vxstack-web/resources/";
                    var types = this._backing[values.type];
                    var ans = iconMap.default;
                    map_(types, function (type) {
                        type = type.value;
                        if (type in iconMap) {
                            ans = iconMap[type];
                        } else if (type !== values.namedIndividual) {
                            console.log("No icon registered for type: " + type);
                        }
                    });
                    return prefix + ans;
                };

                this.getName = function () {
                    return this._backing.name;
                };

                this.getCenterOfMass = function () {
                    return {x: this.x, y: this.y};
                };


                var iconMap = {};
                {
                    //The curly brackets are for cold folding purposes
                    iconMap["default"] = "default.png";
                    iconMap[values.hypervisorService] = "hypervisor_service.png";
                    iconMap[values.routingService] = "routing_service.png";
                    iconMap[values.storageService] = "storage_service.jpg";
                    iconMap[values.objectStorageService] = iconMap[values.storageService];
                    iconMap[values.blockStorageService] = "block_storage_service.png";
                    iconMap[values.IOPerformanceMeasurementService] = "io_perf_service.png";
                    iconMap[values.hypervisorBypassInterfaceService] = "hypervisor_bypass_interface_service.png";
                    iconMap[values.virtualSwitchingService] = "virtual_switch_service.png";
                    iconMap[values.switchingService] = "switching_service.png";
                    iconMap[values.topopolgySwitchingService] = iconMap[values.switchingService];
                    iconMap[values.DataTransferService] = "data_transfer_service.png";
                    iconMap[values.DataTransferClusterService] = iconMap[values.DataTransferService];
//            iconMap[values.virtualCloudService]="";

                }

                this.getType = function () {
                    return "Service";
                };
            }
            return Service;
        });
