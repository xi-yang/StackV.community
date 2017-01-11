"use strict";
define(["local/stackv/topology/model",
    "local/stackv/topology/layout",
    "local/stackv/topology/render",
    "local/d3",
    "local/stackv/utils",
    "local/stackv/topology/DropDownTree",
    "local/stackv/topology/ContextMenu",
    "local/stackv/topology/TagDialog",
    "local/stackv/topology/OMMPanel",
    "local/stackv/topology/TagPanel"],
        function (m, layout, render, d3, utils, 
                  DropDownTree, ContextMenu, TagDialog, OMMPanel,
                  TagPanel) {
            // iterms to load should be passed in 
            function Visualization() {
                    var settings = {
                        ZOOM_FACTOR: .04,
                        ZOOM_MIN: .8,
                        INIT_ZOOM: 2  //The initial zoom factor effects the preciosion in which we can specify the highlighting effect
                                //However, it also seems to effect the error in zooming
                    };
                    var ModelConstructor;
                    var model;
                    var functionMap = {}; // stores objects for funcitonality such as ContextMenu, tag Dialog, etc 

                    var outputApi;
                    var map_;
                    var bsShowFadingMessage;
                    var tagDialog;
                    var persistant_data;
                    var contextMenu;
                    var ommPanel;
                    var tagDialog;
                    var tagPanel;
                    var viz_settings = {
                          fullpage_viz: true,
                          name: "viz"
                    };
                    
                    
                    // in these modules, nothing can happen until the 
                    // templates are loaded 
                    this.init = function () {
                        renderTemplate();
                    };
                    
                    // probablyy should pass function as callback 
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
                    
                    function buildViz() {
                        initDialogs();
                        
                        var userId = sessionStorage.getItem("subject");
                        var username = sessionStorage.getItem("username");
                        var token = sessionStorage.getItem("token");

                        $.ajax({
                            crossDomain: true,
                            type: "GET",
                            url: "/StackV-web/restapi/service/ready",
                            beforeSend: function (xhr) {
                                xhr.setRequestHeader("Authorization", "bearer " + token);
                            },
                            dataType: "text",

                            success: function (data, textStatus, jqXHR) {
                                console.log(data);
                                if (data === "true") {
                                    //alert(textStatus);
                                    $('#servicePanel-contents').removeClass("hide");
                                    map_ = utils.map_;
                                    bsShowFadingMessage = utils.bsShowFadingMessage;
                                    tagDialog = new TagDialog(userId);

                                    ommPanel = new OMMPanel(render.API);
                                    ommPanel.init();
                                    functionMap["AddToTrashcan"] = ommPanel;

                                    tagDialog.init();
                                    // possibly pass in map here later for all possible dialogs 
                                    
                                    contextMenu =  ContextMenu();
                                    var menu = [{
                                        name: 'Add Tag',
                                        img: 'images/create.png',
                                        title: 'add tag',
                                        fun: function () {
                                            PubSub.publish("TagDialog_open", {identifier: data.trigger.id});
                                        }
                                    }];
                                    contextMenu.init({}, menu);

                                    ModelConstructor = m;
                                    model = new ModelConstructor();
                                    outputApi = new outputApi_(render.API, contextMenu, viz_settings.name);
                                    model.init(1, drawGraph.bind(undefined, outputApi, model), null);    
                                   
                                    // modules loaded should be dynamic 
                                    tagPanel =  TagPanel();
                                    tagPanel.init();


                                    window.onbeforeunload = function(){ 
                                        persistVisualization();
                                    };

                                    persistant_data = localStorage.getItem("viz-data");

                                   } else {
                                       displayError("Visualization Unavailable", d3);
                                   }
                               },

                            error: function (jqXHR, textStatus, errorThrown) {
                                console.log("Debugging: timeout at start..");
                                displayError("Visualization Unavailable", d3);
                            }
                        });

                        buildServicePanel(userId, token);

                        $("#loadingPanel").addClass("hide");
                        $("#hoverdiv_" + viz_settings.name).removeClass("hide");

                        $("#" + viz_settings.name).removeClass("loading");

                        buttonInit();
                    };
                    
                    function displayError(error, d3_obj) {
                        d3_obj.select("#" + viz_settings.name).append("text")
                                .attr("x", $(window).width() / 4)
                                .attr("y", $(window).height() / 2)
                                .attr("fill", "black")
                                .attr("font-size", "80px")
                                .text(error);

                        $('#servicePanel-contents').removeClass("hide");
                        $('#servicePanel-contents').html("Service instances unavailable.").addClass('service-unready-message');
                    }

                    function allNodesMatch(nodePositions, nodes){
                        // may want to include intersect here 
                        for (var node in nodePositions) {
                            if (nodePositions[node].name !== nodes[node].getName())
                                return false;
                        }
                        return true;
                    }
                    // if they dont all match, we want to find the one that doesn't. 

                   function removeOldFromPersist(nodePositions, nodeNames){   
                        var pos = nodePositions;
                        for (var i = 0; i < pos.length; i++) {
                            if (!nodeNames.includes(pos[i].name)) {
                                pos.splice(i, 1);
                                i = 0;
                            }
                        }
                        return pos;
                    }     

                    function getNewNodes(nodePositions, nodeNames) {
                        var totalNodes = [];
                        var newNodes = [];

                        for (var i = 0; i < nodePositions.length; i++) {
                            totalNodes.push(nodePositions[i].name);
                        }    

                        for (var i = 0; i < nodeNames.length; i++) {
                            if (!totalNodes.includes(nodeNames[i])) {
                                newNodes.push(nodeNames[i]);
                            }
                        }

                        return newNodes;
                    }

                    function AddNewToPersist(nodePositions, nodeNames, width, height, nodeSize) {
                        var newNodes = getNewNodes(nodePositions, nodeNames);
                        var newTopLevelTopologies = [];

                        for (var i = 0; i < newNodes.length; i++) {
                            var node = model.nodeMap[newNodes[i]];
                            if (node.isTopology && node._parent === null) {
                                newTopLevelTopologies.push(newNodes[i]);
                                newNodes.splice(i, 1);
                            }
                        }

                        // position new topologies 
                        var top_offset = 0;
                        for (var i = 0; i< newTopLevelTopologies.length; i++) {
                            var pos = {};
                            pos.name = newTopLevelTopologies[i];
                            var totalTopoSize = 0;
                            pos.x = width/2 + top_offset;
                            pos.y = height/2 + top_offset;
                            pos.dx = 0;
                            pos.dy = 0;

                            var node = model.nodeMap[newTopLevelTopologies[i]];
                            var children = node.children;
                            var maxSize = children.length * nodeSize;
                            for (var j = 0; j < children.length; j++) {
                                var randX = (Math.random() * (maxSize/2)) + pos.x;
                                var randY = (Math.random() * (maxSize/2)) + pos.y;   
                                var childPos = {};
                                childPos.x = randX;
                                childPos.y = randY;
                                childPos.dx = 0;
                                childPos.dy = 0;
                                childPos.name = children[j].getName();
                                var index = newNodes.indexOf(childPos.name);
                                newNodes.splice(index, 1);
                                model.nodeMap[childPos.name].setPos(childPos);
                            }
                            var plusOrMinus = Math.random() < 0.5 ? -1 : 1;
                            top_offset = pos.x + (plusOrMinus * (maxSize / 2));

                            pos.size = maxSize / 4;
                            model.nodeMap[pos.name].setPos(pos);
                        }

                        for (var i = 0; i< newNodes.length; i++) {
                            var node = model.nodeMap[newNodes[i]];
                            var maxSize = (node._parent.children.length * nodeSize) /3;
                            var randX = (Math.random() * maxSize) + node._parent.x;
                            var randY = (Math.random() * maxSize) + node._parent.y; 

                            var pos = {};
                            pos.x = randX;
                            pos.y = randY;
                            pos.dx = 0;
                            pos.dy = 0;
                            pos.size = nodeSize;
                            pos.name = newNodes[i];                           
                            nodePositions.push(pos);
                            model.nodeMap[pos.name].setPos(pos);
                        }

                        //return nodePositions;                        
                    }


                    function loadPersistedVisualization(outputApi, model, width, height) {
                        if (persistant_data !== undefined && persistant_data !== "undefined") {
                            var viz_data = JSON.parse(persistant_data);
                            if (viz_data !== null) {                    
                                var nodePositions = JSON.parse(viz_data['nodes']);

                                var nodes = model.listNodes();
                                var sameNodes = !(nodePositions.length !== nodes.length || !allNodesMatch(nodePositions, nodes));
                                width = width / parseFloat(viz_data.zoom);
                                height = height / parseFloat(viz_data.zoom);
                                if (sameNodes)  {
                                    for (var i = 0; i < nodePositions.length; i++) {
                                       var name = nodePositions[i].name;
                                       model.nodeMap[name].setPos(nodePositions[i]);
                                    }
                                    layout.doPersistLayout(model, null, width, height);
                                    layout.doPersistLayout(model, null, width, height);
                                    outputApi.setOffsets(parseFloat(viz_data.offsetX), parseFloat(viz_data.offsetY)); 
                                    render.doRender(outputApi, model);
                                    outputApi.setZoom(parseFloat(viz_data.zoom));

                                } else {
                                    var nodeNames = model.listNodeNames();

                                    nodePositions = removeOldFromPersist(nodePositions, nodeNames);
                                    AddNewToPersist(nodePositions, nodeNames, width, height, 21);

                                    for (var i = 0; i < nodePositions.length; i++) {
                                       var name = nodePositions[i].name;
                                       model.nodeMap[name].setPos(nodePositions[i]);
                                    }
                                    layout.doPersistLayout(model, null, width, height);
                                    layout.doPersistLayout(model, null, width, height);
                                    outputApi.setOffsets(parseFloat(viz_data.offsetX), parseFloat(viz_data.offsetY)); 
                                    render.doRender(outputApi, model);
                                    outputApi.setZoom(parseFloat(viz_data.zoom));

                                    //return false;
                                }
                            } else {
                                return false;
                            }
                        } else {
                            return false;
                        }
                        return true;
                    }

                    function persistVisualization() {
                        var nodePositions = [];
                        var nodes = model.listNodes();

                        // doing this for security purposes, dont want to persist model 
                        // data on client, just positions 
                        for (var i = 0; i < nodes.length; i++) {
                            var nodePos = nodes[i].getRenderedObj();
                            nodePos.name = nodes[i].getName();
                            nodePositions[i] = nodePos;
                        }
                        var toPersist = JSON.stringify(nodePositions);
                        var offsets = outputApi.getOffset();

                        var viz_data = {
                            "nodes" : toPersist,
                            "zoom"  : outputApi.getZoom(),
                            "offsetX" : offsets[0],
                            "offsetY" : offsets[1]
                        };

                        try {
                            localStorage.setItem("viz-data", JSON.stringify(viz_data));
                        } catch (err) {
                            console.log(err);
                        }                
                    }

                    function drawGraph(outputApi, model) {
                        var width = document.documentElement.clientWidth / settings.INIT_ZOOM;
                        var height = document.documentElement.clientHeight / settings.INIT_ZOOM;
                        //TODO, figure out why we need to call this twice
                        //If we do not, the layout does to converge as nicely, even if we double the number of iterations

                        if (!loadPersistedVisualization(outputApi, model, width, height)) {       
                            layout.doLayout(model, null, width, height);
                            layout.doLayout(model, null, width, height);
                            render.doRender(outputApi, model);
                        }
                        //  animStart(30);
                    }

                    function reload() {
                        $("#loadingPanel").removeClass("hide");
                        $("#hoverdiv_" + viz_settings.name).addClass("hide");
                        $("#" + viz_settings.name).attr("class", "loading");

                        var lockNodes = model.listNodes();
                        //var posistionLocks = {};
                        model = new ModelConstructor(model);
                        model.init(2, function () {
                            var width = document.documentElement.clientWidth / outputApi.getZoom();
                            var height = document.documentElement.clientHeight / outputApi.getZoom();
                            //TODO, figure out why we need to call this twice
                            //If we do not, the layout does to converge as nicely, even if we double the number of iterations
                            layout.doLayout(model, null, width, height);
                            layout.doLayout(model, null, width, height);

                            //layout.force().gravity(1).charge(-900).start();
                            //commented this out for demo 0421106
        //                    layout.testLayout(model, null, width, height);  //@
        //                    layout.testLayout(model, null, width, height);                    
                            render.doRender(outputApi, model);
                            outputApi.renderApi.selectElement(null);
                        }, null);

                        $("#loadingPanel").addClass("hide");
                        $("#hoverdiv_" + viz_settings.name).removeClass("hide");
                        $("" + viz_settings.name).attr("class", "");
                    }

                    function buttonInit() { //@
                        $("#recenterButton").click(function (evt) {
                            outputApi.resetZoom();
                            var width = document.documentElement.clientWidth / outputApi.getZoom();
                            var height = document.documentElement.clientHeight / outputApi.getZoom();
                            //TODO, figure out why we need to call this twice
                            //If we do not, the layout does to converge as nicely, even if we double the number of iterations
        //                    layout.doLayout(model, null, width, height);
        //                    layout.doLayout(model, null, width, height);
                            layout.stop();
                            //layout.force().gravity(1).charge(-900).start();
                            layout.testLayout(model, null, width, height);
                            layout.testLayout(model, null, width, height);

                            outputApi.resetZoom();
                            render.doRender(outputApi, model);

                            evt.preventDefault();
                        });

                        $("#modelButton").click(function (evt) {
                            var string = model.modelString;

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
                            outputApi.resetZoom();
                            drawGraph(outputApi, model);
                        });

                        $("#displayPanel-tab").click(function (evt) {
                            $("#displayPanel").toggleClass("closed");

                            evt.preventDefault();
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

                        $("#servicePanel-tab").click(function (evt) {
                            $("#servicePanel").toggleClass("closed");
                            evt.preventDefault();
                        });
                    }

                    //animStart and animStop are primarily intended as debug functions
                    //They can be used to see how the layout engine behaves, and restore
                    //a sane layout it it has been messed up.
                    var animIntervalId;
                    function animStart(rate) {
                        animStop();
                        animIntervalId = setInterval(function () {
                            layout.tick();
                            render.redraw();
                        }, rate);
                    }
                    function animStop() {
                        clearInterval(animIntervalId);
                    }


                    function outputApi_(renderAPI, contextMenu, svg) {
                        var that = this;
                        this.renderApi = renderAPI;
                        this.contextMenu = contextMenu;
                        this.svgContainerName = svg;

                        this.getSvgContainer = function () {
                            return d3.select("#" + this.svgContainerName);
                        };

                        var displayTree = new DropDownTree(document.getElementById("treeMenu"), that);
                        displayTree.renderApi = this.renderApi;
                        displayTree.contextMenu = this.contextMenu;

                        this.getDisplayTree = function () {
                            return displayTree;
                        };

                        this.setDisplayName = function (name) {
                            document.getElementById("displayName").innerText = name;
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
                    
                    function buildServicePanel(userId, token) {
                        var tbody = document.getElementById("servicePanel-body");
                        var baseUrl = window.location.origin;

                        var apiUrl = baseUrl + '/StackV-web/restapi/app/panel/' + userId + '/instances';
                        $.ajax({
                            url: apiUrl,
                            type: 'GET',
                            beforeSend: function (xhr) {
                                xhr.setRequestHeader("Authorization", "bearer " + token);
                            },
                            success: function (result) {
                                for (var i = 0; i < result.length; i++) {
                                    var instance = result[i];

                                    var row = document.createElement("tr");
                                    row.setAttribute("id", instance[1]);
                                    row.setAttribute("class", "service-instance-item");

                                    var cell1_1 = document.createElement("td");
                                    cell1_1.innerHTML = instance[3];
                                    var cell1_2 = document.createElement("td");
                                    cell1_2.innerHTML = instance[0];
                                    var cell1_4 = document.createElement("td");
                                    cell1_4.innerHTML = instance[2];
                                    row.appendChild(cell1_1);
                                    row.appendChild(cell1_2);
                                    row.appendChild(cell1_4);
                                    tbody.appendChild(row);
                                }
                                initServiceInstanceItems();
                            },

                            error: function (jqXHR, textStatus, errorThrown) {
                                console.log("No service instances.");

                            }
                        });
                    }
                    
                    
                    function initServiceInstanceItems() {
                        $(".service-instance-item").each(function () {
                            var that = this;
                            var DELAY = 700, clicks = 0, timer = null;

                            $(that).click(function () {
                                clicks++;  //count clicks

                                if (clicks === 1) {
                                    timer = setTimeout(function () {
                                        clickServiceInstanceItem(that);
                                        clicks = 0;
                                    }, DELAY);
                                } else {
                                    clearTimeout(timer);    //prevent single-click action
                                    if ($(that).hasClass("service-instance-highlighted")) {
                                        $(".service-instance-item.service-instance-highlighted").removeClass('service-instance-highlighted');
                                        render.API.setHighlights([], "serviceHighlighting");
                                        render.API.highlightElements("serviceHighlighting");                        
                                        clicks = 0;             //after action performed, reset counter
                                    } else {
                                        timer = setTimeout(function () {
                                            clickServiceInstanceItem(that);
                                            clicks = 0;
                                        }, DELAY);
                                    }
                                }
                            }).dblclick(function (e) {
                                e.preventDefault();
                            });
                        });
                    }

                    function clickServiceInstanceItem(item) {
                        var UUID = $(item).attr('id');

                        $.ajax({
                            crossDomain: true,
                            type: "GET",
                            url: "/StackV-web/restapi/app/service/availibleitems/" + UUID,
                            beforeSend: function (xhr) {
                               xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
                            },
                            dataType: "json",

                            success: function (data, textStatus, jqXHR) {
                                if (data === null) {
                                    bsShowFadingMessage("#servicePanel", "Data not found", "top", 1000);
                                } else {
                                    $(".service-instance-item.service-instance-highlighted").removeClass('service-instance-highlighted');
                                    $(item).addClass('service-instance-highlighted');
                                    //alert(data);
                                    // Union of verified addition and unverified reduction
                                    var unionObj = data;
                                    var result = model.makeSubModel([unionObj]);
                                    var modelArr = model.getModelMapValues(result);

                                    render.API.setHighlights(modelArr, "serviceHighlighting");
                                    render.API.highlightElements("serviceHighlighting");

                                }
                            },

                            error: function (jqXHR, textStatus, errorThrown) {
                                //alert("Error getting status.");
                                alert("textStatus: " + textStatus + " errorThrown: " + errorThrown);
                            }
                        });
                    }
                    function initDialogs() {
                        $(document).ready(function () {
                           $("#omm-panel").load("/StackV-web/ommPanel.html");

                           $("#displayPanel-tab").click(function (evt) {
                               $("#displayPanel").toggleClass("display-open");
                               $("#displayPanel-tab").toggleClass("display-open");

                               evt.preventDefault();
                           });

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
        };
        return Visualization;
});
