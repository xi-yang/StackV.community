<!--
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
 !-->


<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page errorPage = "/VersaStack-web/errorPage.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/sql" prefix="sql"%> 
<jsp:useBean id="user" class="web.beans.userBeans" scope="session" />
<jsp:setProperty name="user" property="*" />  
<jsp:useBean id="serv" class="web.beans.serviceBeans" scope="page" />
<jsp:setProperty name="serv" property="*" />  
<!DOCTYPE html>
<html>
    <head>
        <meta charset="utf-8">
        <title>Graphical View</title>
        <script src="/VersaStack-web/js/keycloak.js"></script>
        <script src="/VersaStack-web/js/jquery/jquery.js"></script>
        <script src="/VersaStack-web/js/bootstrap.js"></script>
        <script src="/VersaStack-web/js/nexus.js"></script>
        <script src="/VersaStack-web/js/jquery-ui.min.js"></script>

        <link rel="stylesheet" type="text/css" href="/VersaStack-web/css/graphTest.css">
        <link rel="stylesheet" href="/VersaStack-web/css/animate.min.css">
        <link rel="stylesheet" href="/VersaStack-web/css/font-awesome.min.css">
        <link rel='stylesheet prefetch' href='http://fonts.googleapis.com/css?family=Roboto:400,100,400italic,700italic,700'>
        <link rel="stylesheet" href="/VersaStack-web/css/bootstrap.css">
        <link rel="stylesheet" href="/VersaStack-web/css/jquery-ui.min.css">
        <link rel="stylesheet" href="/VersaStack-web/css/jquery-ui.structure.min.css">
        <link rel="stylesheet" href="/VersaStack-web/css/jquery-ui.theme.css">                
        <link rel="stylesheet" href="/VersaStack-web/css/style.css">       
        <link rel="stylesheet" href="/VersaStack-web/css/contextMenu.css">   
        <!-- font awesome icons won't show up otherwise --->
        <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/font-awesome/4.6.1/css/font-awesome.min.css">
        <link rel="stylesheet" href="/VersaStack-web/css/jquery-ui.min.css">

        <script>
            $(document).ready(function () {
                $("#tag-panel").load("/VersaStack-web/tagPanel.jsp", function () {

                });

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
        </script> 

        <script>
            //Based off http://dojotoolkit.org/documentation/tutorials/1.10/dojo_config/ recommendations
            dojoConfig = {
                has: {
                    "dojo-firebug": true,
                    "dojo-debug-messages": true
                },
                async: true,
                parseOnLoad: true,
                packages: [
                    {
                        name: "d3",
                        location: "//d3js.org/",
                        main: "d3.v3"
                    },
                    {
                        name: "local",
                        location: "/VersaStack-web/js/"
                    }
                ]
            };
        </script>
        <script src="//ajax.googleapis.com/ajax/libs/dojo/1.10.0/dojo/dojo.js"></script>

        <script type="text/javascript">

            var settings = {
                ZOOM_FACTOR: .04,
                ZOOM_MIN: .8,
                INIT_ZOOM: 2  //The initial zoom factor effects the preciosion in which we can specify the highlighting effect
                        //However, it also seems to effect the error in zooming
            };
            var ModelConstructor;
            var model;
            var layout;
            var render;
            var d3;
            var utils;
            var DropDownTree;
            var functionMap = {}; // stores objects for funcitonality such as ContextMenu, tag Dialog, etc 

            var outputApi;

            function onload() {
                require(["local/versastack/topology/model",
                    "local/versastack/topology/layout",
                    "local/versastack/topology/render",
                    "local/d3",
                    "local/versastack/utils",
                    "local/versastack/topology/DropDownTree",
                    "local/versastack/topology/ContextMenu",
                    "local/versastack/topology/TagDialog"
                ],
                        function (m, l, r, d3_, utils_, tree, c, td) {
                            $.ajax({
                                crossDomain: true,
                                type: "GET",
                                url: "/VersaStack-web/restapi/service/ready",
                                dataType: "text",

                                success: function (data, textStatus, jqXHR) {
                                    if (data === "true") {
                                        //alert(textStatus);
                                        $('#servicePanel-contents').removeClass("hide");
                                        layout = l;
                                        render = r;
                                        d3 = d3_;
                                        utils = utils_;
                                        map_ = utils.map_;
                                        bsShowFadingMessage = utils.bsShowFadingMessage;
                                        DropDownTree = tree;
                                        ContextMenu = c;
                                        TagDialog = td;
                                        tagDialog = new TagDialog("${sessionStorage.username}");

                                        tagDialog.init();
                                        functionMap['Tag'] = tagDialog;
                                        // possibly pass in map here later for all possible dialogs 
                                        contextMenu = new ContextMenu(d3, render.API, functionMap);//, tagDialog);
                                        contextMenu.init();

                                            ModelConstructor = m;
                                            model = new ModelConstructor();
                                            model.init(1, drawGraph.bind(undefined, outputApi, model), null);    
                                            
                                            $("#tagDialog").draggable();
                                            
                                            window.onbeforeunload = function(){ 
                                                persistVisualization();
                                            };
                                            
                                            persistant_data = localStorage.getItem("viz-data");
                          
                                       } else {
                                           displayError("Visualization Unavailable", d3_);
                                       }
                                   },

                                        ModelConstructor = m;
                                        model = new ModelConstructor();
                                        model.init(1, drawGraph.bind(undefined, outputApi, model), null);

                                        $("#tagDialog").draggable();
                                    } else {
                                        displayError("Visualization Unavailable", d3_);
                                    }
                                },

                                error: function (jqXHR, textStatus, errorThrown) {
                                    console.log("Debugging: timeout at start..");
                                    displayError("Visualization Unavailable", d3_);
                                }
                            });
                        });

                $("#loadingPanel").addClass("hide");
                $("#hoverdiv_viz").removeClass("hide");

                $("#viz").attr("class", "");

                buttonInit();
            }

            function displayError(error, d3_obj) {
                d3_obj.select("#viz").append("text")
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
                        
                        if (sameNodes)  {
                            for (var i = 0; i < nodePositions.length; i++) {
                               var name = nodePositions[i].name;
                               model.nodeMap[name].setPos(nodePositions[i]);
                            }
                            layout.doPersistLayout(model, null, width, height);
                            layout.doPersistLayout(model, null, width, height);
                            outputApi.setZoom(parseFloat(viz_data.zoom));
                            outputApi.setOffsets(parseFloat(viz_data.offsetX), parseFloat(viz_data.offsetY)); 
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
                            outputApi.setZoom(parseFloat(viz_data.zoom));
                            outputApi.setOffsets(parseFloat(viz_data.offsetX), parseFloat(viz_data.offsetY)); 
                            
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
                }
                render.doRender(outputApi, model);
                //  animStart(30);
            }
            
            function reload() {
                $("#loadingPanel").removeClass("hide");
                $("#hoverdiv").addClass("hide");
                $("#viz").attr("class", "loading");

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
                $("#hoverdiv").removeClass("hide");
                $("#viz").attr("class", "");
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
        </script>
    </head>

    <body onload="onload()">
        <!-- NAV BAR -->
        <div id="nav">
        </div>
        <!-- SIDE BAR -->
        <div id="sidebar">            
        </div>

        <div id="filterPanel">
            <div id="filterPanel-contents">
                <button class="button-filter-select" id="nofilter">No Filter</button>
                <c:forEach items="${user.modelNames}" var="filterName">
                    <c:if test="${filterName != 'base'}">
                        <button class="button-filter-select" id="${filterName}">${filterName}</button>
                    </c:if>
                </c:forEach>
                ${jobs}
            </div>
        </div>

        <div class="closed" id="servicePanel">
            <div id="servicePanel-tab">
                Services
            </div>
            <div id ="servicePanel-contents">
                <table id="service-instance-table">
                    <thead>
                        <tr>
                            <th>Alias Name</th>                            
                            <th>Service</th>
                            <th>Instance Status</th>
                        </tr>
                    </thead>
                    <tbody>

                        <c:forEach var="instance" items="${serv.instanceStatusCheck()}">
                            <tr class="service-instance-item" id="${instance[1]}">
                                <td>${instance[3]}</td>        
                                <td>${instance[0]}</td>
                                <td>${instance[2]}</td>
                            </tr>
                        </c:forEach>

                    </tbody>
                </table>
            </div>

        </div>
        <script>
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
                            render.API.setServiceHighlights([]);
                            render.API.highlightServiceElements();
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


            function clickServiceInstanceItem(item) {
                var UUID = $(item).attr('id');

                $.ajax({
                    crossDomain: true,
                    type: "GET",
                    url: "/VersaStack-web/restapi/app/service/availibleitems/" + UUID,
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

                            render.API.setServiceHighlights(modelArr);
                            render.API.highlightServiceElements();

                        }
                    },

                    error: function (jqXHR, textStatus, errorThrown) {
                        //alert("Error getting status.");
                        alert("textStatus: " + textStatus + " errorThrown: " + errorThrown);
                    }
                });
            }
        </script>
        <div id="loadingPanel"></div>
        <div id="displayPanel-tab"></div>
        <div id="displayPanel">
            <div id="displayPanel-contents">
                <button id="modelButton">Display Model</button>
                <button id="fullDiaplayButton">Toggle Full Model</button>
                <button id="recenterButton">Recenter</button> <!-- @ -->
                <button id="resetButton"> Reset Visualization </button>
                <div id="displayName"></div>
                <div id="treeMenu"></div>                
            </div>
            <div id="displayPanel-actions-container">
                <div id="displayPanel-actions">
                    <button id="viz_backButton">Back</button>
                    <button id="viz_forwardButton">Forward</button>
                    <div id="URISeachContainer" style="float:right;padding-left:10px;">
                        Search
                        <input type="text" name="Search" id="URISearchInput" placeholder="Enter URI">
                        <input type="submit" id= "URISearchSubmit" value="Submit">
                    </div>

                    <div id="actionForm"></div>
                </div>
            </div>
        </div>        

        <svg class="loading" id="viz">
        <defs>
        <!--When we highlight topologies without specifiyng a width and length, the get clipped
        The x,y offset are so we avoid clipping the top and left edges.
        I simply added zeroes until this worked in all cases I tested (the extreme case being a vertical or horizontal topology
        We could highlight the element entirly with this filter by using feBlend instead of feComposite,
        However, then (in the case of topologies) the outline would be stuck with the same opacity as the entire element, which is not desireable
        Instead, we will simply duplicate the selected element, and show its outline as an overlay
        -->
    <filter id="outline" width="2000000%" height="2000000%" x="-1000000%" y="-1000000%">
        <!--https://msdn.microsoft.com/en-us/library/hh773213(v=vs.85).aspx-->
        <feMorphology operator="dilate" radius="1"/>
        <feColorMatrix result="a" type="matrix"
                       values="0 0 0 0 .7
                       0 0 0 0 1
                       0 0 0 0 0
                       0 0 0 1 0" />
        <feComposite operator="out" in="a" in2="SourceGraphic"/>
    </filter>


    <filter id="serviceHighlightOutline" width="2000000%" height="2000000%" x="-1000000%" y="-1000000%" >
        <feFlood flood-color="#66ff66" result="base" />
        <feMorphology result="bigger" in="SourceGraphic" operator="dilate" radius="1"/>
        <feColorMatrix result="mask" in="bigger" type="matrix"
                       values="0 0 0 0 0
                       0 0 0 0 0
                       0 0 0 0 0
                       0 0 0 1 0" />
        <feComposite result="drop" in="base" in2="mask" operator="in" />
        <feBlend in="SourceGraphic" in2="drop" mode="normal" />
    </filter>
    <filter id="spaDependOnOutline" width="2000000%" height="2000000%" x="-1000000%" y="-1000000%" >
        <feFlood flood-color="#B3F131" result="base" />
        <feMorphology result="bigger" in="SourceGraphic" operator="dilate" radius="1"/>
        <feColorMatrix result="mask" in="bigger" type="matrix"
                       values="0 0 0 0 0
                       0 0 0 0 0
                       0 0 0 0 0
                       0 0 0 1 0" />
        <feComposite result="drop" in="base" in2="mask" operator="in" />
        <feBlend in="SourceGraphic" in2="drop" mode="normal" />
    </filter>
    <filter id="spaExportToOutline" width="2000000%" height="2000000%" x="-1000000%" y="-1000000%" >
        <feFlood flood-color="#23ABA6" result="base" />
        <feMorphology result="bigger" in="SourceGraphic" operator="dilate" radius="1"/>
        <feColorMatrix result="mask" in="bigger" type="matrix"
                       values="0 0 0 0 0
                       0 0 0 0 0
                       0 0 0 0 0
                       0 0 0 1 0" />
        <feComposite result="drop" in="base" in2="mask" operator="in" />
        <feBlend in="SourceGraphic" in2="drop" mode="normal" />
    </filter>
    <filter id="spaImportFromOutline" width="2000000%" height="2000000%" x="-1000000%" y="-1000000%" >
        <feFlood flood-color="#FD3338" result="base" />
        <feMorphology result="bigger" in="SourceGraphic" operator="dilate" radius="1"/>
        <feColorMatrix result="mask" in="bigger" type="matrix"
                       values="0 0 0 0 0
                       0 0 0 0 0
                       0 0 0 0 0
                       0 0 0 1 0" />
        <feComposite result="drop" in="base" in2="mask" operator="in" />
        <feBlend in="SourceGraphic" in2="drop" mode="normal" />
    </filter>    
    <filter id="subnetHighlight" width="2000000%" height="2000000%" x="-1000000%" y="-1000000%">
        <!--https://msdn.microsoft.com/en-us/library/hh773213(v=vs.85).aspx-->
        <feMorphology operator="dilate" radius="1"/>
        <feColorMatrix result="a" type="matrix"
                       values="0 0 0 0 0
                       0 0 0 0 .8
                       0 0 0 0 .3
                       0 0 0 1 0" />
        <feComposite operator="out" in="a" in2="SourceGraphic"/>
    </filter>
    <filter id="outlineFF" width="2000000%" height="2000000%" x="-500%" y="-500%">
        <!--https://msdn.microsoft.com/en-us/library/hh773213(v=vs.85).aspx-->
        <feMorphology operator="dilate" radius="1"/>
        <feColorMatrix result="a" type="matrix"
                       values="0 0 0 0 .7
                       0 0 0 0 1
                       0 0 0 0 0
                       0 0 0 1 0" />
        <feComposite operator="out" in="a" in2="SourceGraphic"/>
    </filter>       
    <filter id="subnetHighlightFF" width="2000000%" height="2000000%" x="-500%" y="-500%">
        <!--https://msdn.microsoft.com/en-us/library/hh773213(v=vs.85).aspx-->
        <feMorphology operator="dilate" radius="1"/>
        <feColorMatrix result="a" type="matrix"
                       values="0 0 0 0 0
                       0 0 0 0 .8
                       0 0 0 0 .3
                       0 0 0 1 0" />
        <feComposite operator="out" in="a" in2="SourceGraphic"/>
    </filter>    
    <filter id="ghost" width="2000000%" height="2000000%" x="-1000000%" y="-1000000%">
        <feColorMatrix type="saturate" values=".2"/>
    </filter>

    <marker id="marker_arrow_viz" markerWidth="10" markerHeight="10" refx="15" refy="3" orient="auto" markerUnits="strokeWidth">
        <path d="M0,0 L0,6 L9,3 z" fill="black" />
    </marker>

    </defs>
    <!--We nest a g in here because the svg tag itself cannot do transforms
        we separate topologies, edges, and nodes to create an explicit z-order
    -->
    <g id="transform_viz">
    <g id="topology_viz"/>
    <g id="edge1_viz"/>
    <g id="anchor_viz"/>
    <g id="node_viz"/>
    <g id="dialogBox_viz"/>
    <g id="volumeDialogBox_viz"/>
    <g id="switchPopup_viz"/>
    <g id="parentPort_viz"/>
    <g id="edge2_viz" />
    <g id="port_viz"/>
    <g id="volume_viz"/>

    </g>
    </svg>
    <div class="hide" id="hoverdiv_viz"></div>        

    <!-- CONTEXT MENU -->
    <nav id="context-menu" class="context-menu">
        <ul class="context-menu__items">
            <li class="context-menu__item">
                <a href="#" class="context-menu__link" data-action="Tag"><i class="fa  fa-tag"></i> Add Tag</a>
            </li>
        </ul>
    </nav>

    <!-- TAG DIALOG -->
    <div id="tagDialog">
        <div id="tagDialogBar">
            <div id="tagDialogCloserBar">
                <i id="tagDialogCloser" class="fa fa-times" aria-hidden="true"></i>
            </div>
        </div>

        <div id="tagDialogContent">
            <div id="tagDialogLabelInputContainter">
                <input type="text" name="labelInput" id="tagDialogLabelInput" placeholder="Enter label.">
            </div>

            <div id="tagDialogColorInputContainer">
                <div id="tagDialogColorInputLabel">
                    Select Color
                </div>

                <div id="tagDialogColorSelectionTab">

                    <span class="colorBox" id="boxRed"> 
                    </span>
                    <span class="colorBox" id="boxOrange">
                    </span>
                    <span class="colorBox" id="boxYellow">
                    </span>
                    <span class="colorBox" id="boxGreen">
                    </span>
                    <span class="colorBox" id="boxBlue">
                    </span>
                    <span class="colorBox" id="boxPurple">
                    </span>
                </div>
            </div>

            <div id="tagDialogButtonContainer">
                <button id="tagDialogCancel">
                    Cancel
                </button>

                <button id="tagDialogOK">
                    Ok
                </button>
            </div>
        </div>
    </div>

    <div id="dialog_policyAction" title="Policy Action">
    </div>
    <div id="dialog_policyData" title="Policy Data">
    </div>

    <div id="dialog_modelView" title="Model View">
    </div>

    <!-- TAG PANEL -->
    <div id="tag-panel"> 
    </div>
</body>
</html>
