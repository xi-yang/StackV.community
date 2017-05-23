define(["local/stackv/topology/model", 
        "local/stackv/topology/layout",
        "local/stackv/topology/render",
        "local/d3", 
        "local/stackv/utils"
       ], function(Model, Layout, Render, d3, Utils) {
	function Visualization(data) {
                var id = data.id;
                var viz_settings = data.settings;
                        
                    var settings = {
                        ZOOM_FACTOR: .04,
                        ZOOM_MIN: .8,
                        INIT_ZOOM: 2  //The initial zoom factor effects the preciosion in which we can specify the highlighting effect
                                //However, it also seems to effect the error in zooming
                    };

                var hasMediator = window.PubSub !== undefined;
                var _Model =  new Model();
                var _Render = Render;
                var _Layout = Layout;
                var _OutputApi;
                var _Logger = {};


                var _States = {
                    UNINIT: "unInit", 
                    INIT_STARTED : "initStarted",
                    MODEL_LOADED : "modelLoaded",
                    RENDERED : "rendered", 
                    INIT_FINISHED: "InitFinished"
                };
                var _CurrentState = _States.UNINIT;
                var _PastStates = [];	

                if (hasMediator) {
                    PubSub.subscribe(id + ".initalized", function(msg, data) {
                        console.log("i'm here : " + data.name);
                        if(!_PastStates.includes(data.name))
                            _PastStates.push(data.name);
                        PubSub.publish(id + ".initUpdate", {
                                init : _PastStates
                        });
                    });
                } else {
                   console.log("Error: PubSub not found.");
                }

                // DRY, probably want a variable like "pubSub available"
                // Also might want to store this error somewhere 
                var _Mediator = {
                    subscribe: function(message, callback) {
                        if (hasMediator) {
                                return PubSub.subscribe(id + "." + message, callback); 
                        } else {
                                console.log("Error: PubSub not found.");
                        }
                    },
                    publish: function(message, data) {
                        if (hasMediator) {
                                PubSub.publish(id + "." + message, data);
                        } else {
                                console.log("Error: PubSub not found.");
                        }
                    }, 
                    unsubscribe: function(token) {
                            if (hasMediator) {
                                PubSub.unsubscribe( token );
                            } else {
                                console.log("Error: PubSub not found.")
                            } 
                    }
                }; 



            var initalized = false; // dojo included inital modules  
            var required_module_paths = [];
            var names = [];
            var modules = []; // 
            var moduleInstances = [];
            
            function renderTemplate() {
                var template_root = "/StackV-web/data/viz_templates/";

                $.get(template_root + 'viz.mst', function(template) {
                  $.get(template_root + 'svg_defs.html', function(defs) {
                    var rendered = Mustache.render(template, viz_settings, {
                          svg_defs: defs
                    });
                    $('body').append(rendered);
                    buildViz();
                  });
                });
            }
                    

            function init() {
                if (!initalized) {
                  createDefineArrays();
                  require(required_module_paths, _mainFunction.bind(null));
                } else {
                    console.log("Visualization Error: Visualization already initalized.");
                }
            }

            function createDefineArrays() {
                for (var i = 0; i < data.data_array.length; i++) {
                    var path = data.data_array[i].module_path;
                    if (!required_module_paths.includes(path)) {
                        required_module_paths.push(path);
                        modules.push({});
                    }
                }
            }
            
            function initModules() {
                for (var i = 0; i < modules.length; i++) {
                    initModuleInstance(modules[i](), false, 
                        data.data_array[i].uses_properties, 
                        data.data_array[i].initReqs, 
                        data.data_array[i].initArgs);
                }
            }


            // initalizeAfter: [a,b,c,d,e]
            function initModuleInstance(instance, initalized, need_properties, initReqs, initArgs) {
                moduleInstances.push(instance);
                if (need_properties) {
                        instance.setVizProperties(_Model, _Render, _OutputApi, _Layout);
                }
                if (!initalized) {
                        instance.setInitProperties(initArgs, initReqs);
                }
               	instance.setMediator(_Mediator);
               	instance.initMediator();      
            }
            
            function updateInitState(State) {
                _State =  State; 
                _PastStates.push(_State);
                PubSub.publish(id + ".initUpdate", {
                        init : _PastStates
                });
                console.log("updating status");
            }

            function _mainFunction() {
                initalized = true; 
                modules = Array.from(arguments); 
                renderTemplate();
            }
            
            function buildViz() {
                _OutputApi = new outputApi_(_Render.API, undefined, viz_settings.name);
                initModules();
                updateInitState(_States.INIT_STARTED);
                _Model.init(1, function () {     				   			
                    updateInitState(_States.MODEL_LOADED);
                    drawGraph(_OutputApi, _Model, function() {
                        updateInitState(_States.RENDERED);
                        $("#loadingPanel").addClass("hide");
                        $("#hoverdiv_" + viz_settings.name).removeClass("hide");
                        $("#" + viz_settings.name).removeClass("loading");
                    });
                }, null, null);
            }
            
            function drawGraph(outputApi, model, callback) {
                var width = document.documentElement.clientWidth / settings.INIT_ZOOM;
                var height = document.documentElement.clientHeight / settings.INIT_ZOOM;
                //TODO, figure out why we need to call this twice
                //If we do not, the layout does to converge as nicely, even if we double the number of iterations
                
                if (moduleAvailable("VizPersistence"))  {
                    _Mediator.publish("VizPersistence_load", {
                        width: width,
                        height: height
                    });
                } else {       
                    _Layout.doLayout(model, null, width, height);
                    _Layout.doLayout(model, null, width, height);
                    _Render.doRender(outputApi, model, undefined, undefined, undefined, _Mediator);
                }
                //  animStart(30);
                 if (callback !== undefined) {
                     callback();
                 }
            }

            // can use this if module is initalized 
            function registerModule(data_obj) {
                if (!initalized) {
                    data.data_array.push(data_obj);
                } else  {
                    // should propabably be a data structure that contains all these 
                    console.log("Visualization Error: Visualization already initalized.");
                }
            }
            
            /*
             * OutputAPI Object  
             */
            function outputApi_(renderAPI, contextMenu, svg) {
                var that = this;
                this.renderApi = renderAPI;
                this.contextMenu = contextMenu;
                this.svgContainerName = svg;

                this.getSvgContainer = function () {
                    return d3.select("#" + this.svgContainerName);
                };


                var zoomFactor = settings.INIT_ZOOM;
                var offsetX = 0, offsetY = 0;
                this.getOffset = function(){
                    return [offsetX, offsetY];
                };
                this.setOffsets = function(x, y){
                    offsetX = x;
                    offsetY = y;
                    this._updateTransform();
                };
                this.zoom = function (amount, mouseX, mouseY) {
                    /*
                     * There seems to be some error (rounding?) when we zoom far out
                     * 
                     * In addition to zooming, we also translate the image so that the point under the cursor appears stationary.
                     * To understand this conversion, there are two coordinate systems to consdier:
                     *      the svg coordinate system (in which all svg objects are stationary throughout the translation), and
                     *      the mouse coordinate system, that corresponds to what we see
                     *      
                     *       The conversion (along a single axis) between these two systems is given by:
                     *       X_mouse=zoomFactor*(X_svg + offsetX)
                     *       To find the new offset, after increasing zoomFactor by zoomDelta, we solve:
                     *         zoomFactor*(X_svg + offsetX)=(zoomFactor+zoomDelta)*(X_svg + (offsetX+doX))
                     *        for doX.
                     *        the new offset is now offsetX+doX
                     *        
                     */
                    var zoomFactorNew = zoomFactor * (1 + amount);
                    var zoomDelta = zoomFactorNew - zoomFactor;
                    if (zoomFactorNew < settings.ZOOM_MIN) {
                        return;
                    }
                    zoomFactor = zoomFactorNew;
                    //Translate so that the point under the mouse does not move
                    //get the svg coordinate of the mouse
                    var svgCoords = this.convertCoords(mouseX, mouseY);
                    offsetX -= zoomDelta * (svgCoords.x + offsetX) / zoomFactor;
                    offsetY -= zoomDelta * (svgCoords.y + offsetY) / zoomFactor;
                    this._updateTransform();
                };
                this.getZoom = function () {
                    return zoomFactor;
                };
                this.scroll = function (dx, dy) {
                    offsetX += dx / zoomFactor;
                    offsetY += dy / zoomFactor;
                    this._updateTransform();
                };

                this._updateTransform = function () {
                    d3.select("#transform" + "_" + this.svgContainerName).
                            attr("transform", "scale(" + zoomFactor + ")translate(" + offsetX + "," + offsetY + ")");
                };
                this._updateTransform();
                //Return the svg coordinates of the point under the given mouse coords
                //The mouseCoords should be from event.offsetX (offsetY)
                this.convertCoords = function (mouseX, mouseY) {
                    var ans = {};
                    ans.x = mouseX / zoomFactor - offsetX;
                    ans.y = mouseY / zoomFactor - offsetY;
                    return ans;
                };

                this.setHoverText = function (str) {
                    document.getElementById("hoverdiv" + "_" + this.svgContainerName).innerText = str;
                };
                this.setHoverLocation = function (x, y) {
                    document.getElementById("hoverdiv" + "_" + this.svgContainerName).style.left = x + "px";
                    document.getElementById("hoverdiv" + "_" + this.svgContainerName).style.top = y + 10 + "px";
                };
                this.setHoverVisible = function (vis) {
                    document.getElementById("hoverdiv" + "_" + this.svgContainerName).style.visibility = vis ? "visible" : "hidden";
                };

                this.resetZoom = function () {   // @
                    zoomFactor = settings.INIT_ZOOM;
                    offsetX = 0;
                    offsetY = 0;
                    this._updateTransform();
                };
                this.setZoom = function (zoom) {
                    zoomFactor = zoom;
                    this._updateTransform();
                };
                var svg = document.getElementById(this.svgContainerName);

                svg.addEventListener("wheel", function (e) {
                    e.preventDefault();
                    //The OSX trackpad seems to produce scrolls two orders of magnitude large when using pinch to zoom,
                    //so we ignore the magnitude entirely
                    that.zoom(Math.sign(-e.deltaY) * settings.ZOOM_FACTOR, e.offsetX, e.offsetY);
                    return false;
                }, false);
                //Interface to (de)select elements for interaction with the form
                //If the provided element is currently being used in the form, remove it from the form,
                //Otherwise use it to help poupulate the form.
                //The precises meaning of this depends on the element and form type
                this.formSelect = function (n) {
                    if (!n.getType) {
                        console.log("Trying to form select object without getType function");
                        return;
                    }
                    switch (n.getType()) {
                        case "Subnet":
                            var forms = document.getElementsByName("subnets");
                            if (forms.length === 0) {
                                break;
                            }
                            if (forms.length > 1) {
                                console.log("More than 1 subnets forms");
                                break;
                            }
                            var form = forms[0];
                            var subnetStrs = [];
                            var toToggle = n.getNameBrief();
                            var add = true;
                            //If the form is already empty, we would add an extranous empty line
                            if (form.value.trim() !== "") {
                                map_(form.value.split('\n'), function (subnetStr) {
                                    if (subnetStr.trim() === toToggle) {
                                        add = false;
                                    } else {
                                        subnetStrs.push(subnetStr);
                                    }
                                    ;
                                });
                            }
                            if (add) {
                                subnetStrs.push(toToggle);
                            }
                            form.value = subnetStrs.join('\n');
                            break;
                        default:
                            console.log("Unhandled type in formSelect: " + n.getType());
                    }
                };

                //Panning
                //d3 does not seem to provide a way for us to avoid capturing mouse events that are handled by nodes
                //This means that our panning code will be called even when the user is only trying to move a node
                //as a work around, we provide a way to enable/disable panning
                var panningEnabled = true;
                var isPanning = false;
                var moved = false;
                this.enablePanning = function () {
                    panningEnabled = true;
                };
                this.disablePanning = function () {
                    panningEnabled = false;
                };
                svg.addEventListener("mousedown", function (e) {
                    moved = false;
                    isPanning = true;
                });
                svg.addEventListener("mousemove", function (e) {
                    // && (e.which ==== 1) stops d3 bug of dragging to enable on context menu 
                    if (isPanning && panningEnabled && (e.which === 1)) {
                        moved = true;
                        that.scroll(e.movementX, e.movementY);
                    }
                });
                svg.addEventListener("mouseup", function (e) {
                    isPanning = false;
                    if (moved) {
                        e.preventDefault();
                    }
                });
            }

            /*
             * 
             * Assorted functions 
             * 
             * 
             */
                    function displayError(error, d3_obj) {
                        d3_obj.select("#" + viz_settings.name).append("text")
                                .attr("x", $(window).width() / 4)
                                .attr("y", $(window).height() / 2)
                                .attr("fill", "black")
                                .attr("font-size", "80px")
                                .text(error);

                                                           /**
                                     *  Remove HIDE FROM SERVICE PANEL 
                                     *  
                                     *  
                                     *  
                                     *  
                                     *  
                                     */
                        /*
                         * 
                         * 
                         * 
                         *  service panel set error text 
                         */
                         //$('#servicePanel-contents').html("Service instances unavailable.").addClass('service-unready-message');

                    }

                     function reload() {
                        $("#loadingPanel").removeClass("hide");
                        $("#hoverdiv_" + viz_settings.name).addClass("hide");
                        $("#" + viz_settings.name).attr("class", "loading");

                        var lockNodes = _Model.listNodes();
                        //var posistionLocks = {};
                        _Model = new Model(_Model);
                        _Model.init(2, function () {
                            var width = document.documentElement.clientWidth / _OutputApi.getZoom();
                            var height = document.documentElement.clientHeight / _OutputApi.getZoom();
                            //TODO, figure out why we need to call this twice
                            //If we do not, the layout does to converge as nicely, even if we double the number of iterations
                            _Layout.doLayout(_Model, null, width, height);
                            _Layout.doLayout(_Model, null, width, height);

                            //layout.force().gravity(1).charge(-900).start();
                            //commented this out for demo 0421106
        //                    layout.testLayout(model, null, width, height);  //@
        //                    layout.testLayout(model, null, width, height);                    
                            _Render.doRender(_OutputApi, _Model, undefined, undefined, undefined, _Mediator);
                            _OutputApi.renderApi.selectElement(null);
                        }, null);

                        $("#loadingPanel").addClass("hide");
                        $("#hoverdiv_" + viz_settings.name).removeClass("hide");
                        $("" + viz_settings.name).attr("class", "");
                    }

                    function buttonInit() { //@
                        $("#recenterButton").click(function (evt) {
                            _OutputApi.resetZoom();
                            var width = document.documentElement.clientWidth / _OutputApi.getZoom();
                            var height = document.documentElement.clientHeight / _OutputApi.getZoom();
                            //TODO, figure out why we need to call this twice
                            //If we do not, the layout does to converge as nicely, even if we double the number of iterations
        //                    layout.doLayout(model, null, width, height);
        //                    layout.doLayout(model, null, width, height);
                            _Layout.stop();
                            //layout.force().gravity(1).charge(-900).start();
                            _Layout.testLayout(_Model, null, width, height);
                            _Layout.testLayout(_Model, null, width, height);

                            _OutputApi.resetZoom();
                            _Render.doRender(_OutputApi, _Model, undefined, undefined, undefined, _Mediator);

                            evt.preventDefault();
                        });

                        $("#modelButton").click(function (evt) {
                            var string = _Model.modelString;

                            // We detach DOM children and append them back to the model view
                            // dialog after it's been opened to save time when opening dialogs
                            // that have large amounts of text 
                            // Reference: http://johnculviner.com/a-jquery-ui-dialog-open-performance-issue-and-how-to-fix-it/
                            var detached = $("#dialog_modelView").children().detach();

                            $("#dialog_modelView").dialog({
                                autoOpen: false,
                                width: 600,
                                height: ($(window).height() * (3 / 4)),
                                open: function () {
                                    detached.appendTo($("#dialog_modelView"));
                                    $("#dialog_modelView").html("<pre class=\"jSonDialog\">" + string + "</pre>");
                                }
                            });

                            $("#dialog_modelView").dialog("open");

                            // JQuery UI automatically focuses on the first dialog, we remove all
                            // focus by using the blur method. 
                            $('.ui-dialog :button').blur();
                        });
                        $("#resetButton").click(function(evt) {
                            localStorage.setItem("viz-data", null);
                            persistant_data = "undefined";
                            _OutputApi.resetZoom();
                            drawGraph(_OutputApi, _Model);
                        });


                        // Brings tagPanel or tagDialog to the foreground if one is 
                        // clicked and behind the other. Will probably need to be 
                        // generalized soon. 
                        function bringToForeground(current) {
                            var tagDialogElement = document.querySelector("#tagDialog");
                            var tagPanelElement = document.querySelector("#tagPanel");
                            if (!tagPanelElement.classList.contains("closed") &&
                                    tagDialogElement.classList.contains("tagDialog-active"))
                            {
                                var tagDialog = document.getElementById("tagDialog");
                                var tagPanel = document.getElementById("tagPanel");
                                var tdz = parseInt(window.getComputedStyle(tagDialog, null).zIndex);
                                var tpz = parseInt(window.getComputedStyle(tagPanel, null).zIndex);

                                if (((current === "tagDialog") && (tdz < tpz)) ||
                                        ((current === "tagPanel") && (tpz < tdz))) {
                                    tagDialog.style.zIndex = tpz;
                                    tagPanel.style.zIndex = tdz;
                                }
                            }
                        }

                        $("#tagDialog").click(function (evt) {
                            bringToForeground("tagDialog");
                            evt.preventDefault();
                        });

                        $("#tagPanel").click(function (evt) {
                            bringToForeground("tagPanel");
                            evt.preventDefault();
                        });

                    }
                     function initDialogs() {
                        $(document).ready(function () {
                           $("#omm-panel").load("/StackV-web/ommPanel.html");

                             $(function() {
                                $( "#dialog_policyAction" ).dialog({
                                    autoOpen: false
                                });
                                $( "#dialog_policyData" ).dialog({
                                    autoOpen: false,
                                    maxHeight: 500,
                                    minHeight: 175,
                                    width: "auto",
                                    //maxWidth: 500,  jquery ui bug, this doens't work 
                                    create: function (event, ui) {
                                        $( "#dialog_policyData" ).css("maxWidth",  "400px" );
                                    }, 
                                    open: function( event, ui ) {
                                        $( "#dialog_policyData" ).dialog( "option", "height", "auto" );
                                    }
                                });         

                           });

                       });   
                    }
                    
                    function moduleAvailable(moduleName) {
                        for(var i = 0; i < modules.length; i++) {
                            if (moduleName === modules[i].name)
                                    return true;
                        }
                        return false; 
                    }
            return {
                init : init, 
                registerModule : registerModule, 
                // you should probably be able ot pass in itialized instances 
                registerModuleInstance: function(instance, initalized, need_properties) {
                            initModuleInstance(instance, need_properties);
                }
            };
	};

	return Visualization;
});