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
                    var prefix = "/VersaStack-web/resources/";
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

                this.populateTreeMenu = function (tree) {
                    var container = tree.addChild(this.getName(), "Service");
                    map_(this.subnets, function (subnet) {
                        subnet.populateTreeMenu(container);
                    });
                    
                    if (this.misc_elements.length > 0) {
                        var displayed = [];
                        for (var i = 0; i < this.misc_elements.length; i++){
                            var el = this.misc_elements[i];
                            if (displayed.indexOf(el) === -1 && el.getName() !== undefined) {
                                var type = el.relationship_to[this];
                                var elementsNode = tree.addChild(type === undefined?"undefined":type, "");
                                for (var o in this.misc_elements) {
                                    if (displayed.indexOf(this.misc_elements[o]) === -1 && 
                                            this.misc_elements[o].relationship_to[this] === type
                                            && this.misc_elements[o].getName() !== undefined) {
                                       // console.log ("name of thing: " + this.misc_elements[o].getName());
                                        elementsNode.addChild(this.misc_elements[o].getName(), "Element");;
                                        displayed.push(this.misc_elements[o]);
                                    }
                                }
                            }
                        }                     
                    }                    
                };
            }
            return Service;
        });