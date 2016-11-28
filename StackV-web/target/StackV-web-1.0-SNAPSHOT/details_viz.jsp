<!--
 * Copyright (c) 2013-2016 University of Maryland
 * Created by: Antonio Heard 2016

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
<%@page errorPage = "/StackV-web/errorPage.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/sql" prefix="sql"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<jsp:useBean id="user" class="web.beans.userBeans" scope="session" />
<jsp:setProperty name="user" property="*" />  
<jsp:useBean id="serv" class="web.beans.serviceBeans" scope="page" />
<jsp:setProperty name="serv" property="*" />  
<c:if test="${user.loggedIn == false}">
    <c:redirect url="/index.jsp" />
</c:if>
<link rel="stylesheet" href="/StackV-web/css/jquery-ui.min.css">
<link rel="stylesheet" href="/StackV-web/css/contextMenu.css">           
<link rel="stylesheet" href="/StackV-web/css/jquery-ui.structure.min.css">
<link rel="stylesheet" href="/StackV-web/css/jquery-ui.theme.css">                

<style>
    .hover_div {
        position: fixed;
        visibility: hidden;
        background-color: rgba(255,255,255,.85);
        color: #404040;
        border: 1px solid #999;
        padding: 7px;
    }

    .details_viz {
        width:100%;
        height:100%;
    }

    .inactive_details_viz td{
        border-top: 0px;
        background-color: #777;
    }

    .inactive_details_viz th {
        color: #ccc;
    }
    /****/

    /*#tagDialogBar {
        width:100%;
    }
    #tagDialogCloser{
      color:grey;
      cursor:pointer;
    }

    #tagDialogCloserBar{
      padding-left:80%;    
    }
    #tagDialogContent {
      margin:auto;
      margin-top:10px;
    }*/
    .displayPanel {
        text-align:center;
        background-color:#EDEDED;
        width:25%;
        /*  height:30%;*/
        display:none;
        position:absolute;
        top: 30%;
        left: 30%;
        margin-top: -50px;
        margin-left: -50px;
        border: 1px inset #B5B1B1;
        z-index:1;
    }

    .displayPanel.displayPanel-active {display:block;}
    .urnLink {
        color:blue;
        cursor:pointer;
        overflow: auto; 
        word-wrap: break-word;
    }
    .clicked {
        color:red;
        text-decoration: underline;
    }
    .urnLink:hover { }
    .urnLink:visited {color:purple }

    .panelElementProperty{font-weight:bold;}
    .dropDownArrow {cursor:pointer;}

    .treeMenu{

        margin-left:15px;
        text-align: left;
        /*                min-height:150px;
                        max-height:250px;
                        overflow-y:scroll;
                        overflow: -moz-scrollbars-vertical;
                        clear:both;*/
    }
    .treeMenu-container {
        min-height:150px;
        max-height:250px;
        overflow-y:scroll;
        overflow: -moz-scrollbars-vertical;
        clear:both;
        /* webkit scrollbar stuff */
    }
    .displayName {
        text-align: center;
        visibility: visible;
        padding: 7px;
        width: content-box;
        font-size: 150%;
        overflow-wrap: break-word;
        /* For firefox */
        white-space: pre-wrap;
        word-break: break-all;
    }

    .displayPanel-actions {    
        /*    bottom: 20px;
            position: absolute;   */
        padding-bottom: 2%;

    }

    .displayPanelBar {
        width:100%;
        cursor:default;
    }
    .displayPanelCloser{
        color:grey;
        cursor:pointer;
    }

    .displayPanelCloserBar{
        padding-left:95%;    
        border-bottom: black 1px solid;
    }
    .displayPanel-contents{ 
        padding-right: 3%;
    }
    .details-viz-button{
        float:right;
    }
    #definition_svg {
        height: 0; 
        position: absolute; 
        width: 0;                
    }
    .jSonDialog{
        text-align:left;
    }
</style>

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
    var modelMap = {}; // stores models in <visualization div, model> format 
    var outputApiMap = {};
    var outputApi;

    function onload() {
        $(function () {
            $("#dialog_policyAction").dialog({
                autoOpen: false
            });
            $("#dialog_policyData").dialog({
                autoOpen: false,
                maxHeight: 500,
                minHeight: 50,
                width: "auto",
//                         height: 400,
//                         width: "80%",
//                         maxWidth: 500,  jquery ui bug, this doens't work 
                create: function (event, ui) {
                    //$( "#dialog_policyData" ).css("maxWidth",  "800px" );
                },
                open: function (event, ui) {
                    $("#dialog_policyData").dialog("option", "height", "auto");
                }
            });

        });
        require(["local/stackv/topology/model",
            "local/stackv/topology/layout",
            "local/stackv/topology/render",
            "local/d3",
            "local/stackv/utils",
            "local/stackv/topology/DropDownTree",
            "local/stackv/topology/ContextMenu"],
                function (m, l, r, d3_, utils_, tree, c) {

                    $.ajax({
                        crossDomain: true,
                        type: "GET",
                        url: "/StackV-web/restapi/service/ready",
                        dataType: "text",

                        success: function (data, textStatus, jqXHR) {
                            if (data === "true") {
                                //alert(textStatus);
                                layout = l;
                                render = r;
                                d3 = d3_;
                                utils = utils_;
                                map_ = utils.map_;
                                bsShowFadingMessage = utils.bsShowFadingMessage;
                                positionDisplayPanel = utils.positionDisplayPanel;

                                // possibly pass in map here later for all possible dialogs 
                                ContextMenu = c;
                                DropDownTree = tree;

                                ModelConstructor = m;
                                model = new ModelConstructor();
                                model.init(1, renderModels, null, "default");

                                functionMap['ModelBrowser'] = function (o, m, e) {
                                    positionDisplayPanel(m + "_displayPanel", e);
                                    var browser = document.querySelector("#" + m + "_displayPanel");
                                    $(".displayPanel").removeClass("displayPanel-active");
                                    browser.classList.add("displayPanel-active");
                                    render.API.selectElement(o, outputApiMap[m]);

                                };

                                contextMenu = new ContextMenu(d3, render.API, functionMap);//, tagDialog);
                                contextMenu.init();

                                console.log("after model.");
                            } else {
                                displayError("Backend not Ready", d3, "va_viz", -80);
                                disableButtons("va");
                                displayError("Backend not Ready", d3, "ur_viz", -80);
                                disableButtons("ur");
                                displayError("Backend not Ready", d3, "ua_viz", -80);
                                disableButtons("ua");
                                displayError("Backend not Ready", d3, "vr_viz", -80);
                                disableButtons("vr");

                                displayError("Backend not Ready", d3, "serva_viz", -80);
                                disableButtons("serva");
                                displayError("Backend not Ready", d3, "servr_viz", -80);
                                disableButtons("servr");
                                displayError("Backend not Ready", d3, "sysa_viz", -80);
                                disableButtons("sysa");
                                displayError("Backend not Ready", d3, "sysr_viz", -80);
                                disableButtons("sysr");
                            }
                        },

                        error: function (jqXHR, textStatus, errorThrown) {
                            console.log("Debugging: timeout at start..");
                            // displayError("Visualization Unavailable", d3_);
                            //alert("textStatus: " + textStatus + " errorThrown: " + errorThrown);
                            displayError("Unavailable", d3, "va_viz");
                            disableButtons("va");
                            displayError("Unavailable", d3, "ur_viz");
                            disableButtons("ur");
                            displayError("Unavailable", d3, "ua_viz");
                            disableButtons("ua");
                            displayError("Unavailable", d3, "vr_viz");
                            disableButtons("vr");

                            displayError("Unavailable", d3, "serva_viz");
                            disableButtons("serva");
                            displayError("Unavailable", d3, "servr_viz");
                            disableButtons("servr");
                            displayError("Unavailable", d3, "sysa_viz");
                            disableButtons("sysa");
                            displayError("Unavailable", d3, "sysr_viz");
                            disableButtons("sysr");
                        }
                    });
                    $(".displayPanelCloser").on("click", function () {
                        $(".displayPanel").removeClass("displayPanel-active");

                    });

//                            document.getElementById("displayPanelCloser").onclick = function() {
//                              // $("#displayPanel").removeClass( "displayPanel-active");
//                                $(".displayPanel").removeClass("displayPanel-active");
//
//                            };

                });
    }

    function make_display_panel(div_id, prefix) {
        var display_panel = "<div class=\"displayPanel\" id=\"" + prefix + "_displayPanel\"> " +
                "<div class=\"displayPanelBar\">" +
                "<div class=\"displayPanelCloserBar\"> " +
                "<i id=\"displayPanelCloser\" class=\"fa fa-times displayPanelCloser\" aria-hidden=\"true\"></i>" +
                "</div>" +
                "</div>" +
                "<div class=\"displayPanel-contents\">" +
                "<div class=\"displayName\" id=\"" + prefix + "_displayName\"></div>" +
                "   <div class=\"treeMenu-container\" id=\"" + prefix + "_treeMenu\"></div>    " +
                "</div>" +
                "<div class=\"displayPanel-actions-container\">" +
                "<div class=\"displayPanel-actions\">" +
                "  <button id=\"" + prefix + "_backButton\">Back</button>" +
                "   <button id=\"" + prefix + "_forwardButton\">Forward</button>" +
                "</div>" +
                " </div>" +
                "  </div>  ";
        var div = document.getElementById(div_id);
        div.insertAdjacentHTML("beforeend", display_panel);
        //$("#" + div_id).append(display_panel);

    }

    function make_viz(div_id, prefix) {
        var viz = "<div id=\"" + prefix + "_viz_div\" class=\"hidden\">" +
                "<div class=\"hover_div\" id=\"hoverdiv_" + prefix + "_viz\"></div>" +
                "<svg class =\"details_viz\" id= \"" + prefix + "_viz\"> " +
                '<defs>' +
                '  <marker id="marker_arrow_' + prefix + '_viz" markerWidth="10" markerHeight="10" refx="15" refy="3" orient="auto" markerUnits="strokeWidth">' +
                '      <path d="M0,0 L0,6 L9,3 z" fill="black" />' +
                '  </marker>' +
                '</defs>' +
                "<g id=\"transform_" + prefix + "_viz\"> " +
                "<g id=\"topology_" + prefix + "_viz\"/> " +
                "<g id=\"edge1_" + prefix + "_viz\"/> " +
                "<g id=\"anchor_" + prefix + "_viz\"/> " +
                "<g id=\"node_" + prefix + "_viz\"/> " +
                "<g id=\"dialogBox_" + prefix + "_viz\"/> " +
                "<g id=\"volumeDialogBox_" + prefix + "_viz\"/> " +
                "<g id=\"switchPopup_" + prefix + "_viz\"/> " +
                "<g id=\"parentPort_" + prefix + "_viz\"/> " +
                "<g id=\"edge2_" + prefix + "_viz\" />" +
                "<g id=\"port_" + prefix + "_viz\"/>" +
                "<g id=\"volume_" + prefix + "_viz\"/></g>" +
                "</svg>";

        if (prefix === "va") {
            viz += "<button class=\"details-viz-button\" id=\"manifest_button\" onclick=\"showManifest()\">Display Manifest</button>";
        }

        viz += "<button class=\"details-viz-button\" id=\"" + prefix + "_viz_recenter_button\">Recenter</button>" +
                "<button  class=\"details-viz-button\" id=\"" + prefix + "_viz_toggle_model\">View Text Model</button>" +
                "</div>";

        $("#" + div_id).append(viz);
    }



    function recenterGraph(o, model) {
        o.resetZoom();
        var width = $("#" + o.svgContainerName).closest("td").width();
        var height = $("#" + o.svgContainerName).closest("td").height();

        layout.stop();
        //layout.force().gravity(1).charge(-900).start();
        layout.doLayout(model, null, width, height);
        layout.doLayout(model, null, width, height);

        o.resetZoom();
        render.doRender(o, model, false, modelMap);
    }

    function displayError(error, d3_obj) {
        d3_obj.select("#viz").append("text")
                .attr("x", $(window).width() / 4)
                .attr("y", $(window).height() / 2)
                .attr("fill", "black")
                .attr("font-size", "80px")
                .text(error);

    }


    function drawGraph(outputApi, model2) {
        var width = $("#" + outputApi.svgContainerName).closest("td").width();//document.documentElement.clientWidth / settings.INIT_ZOOM;
        var height = $("#" + outputApi.svgContainerName).closest("td").height(); //document.documentElement.clientHeight / settings.INIT_ZOOM;
        //TODO, figure out why we need to call this twice
        //If we do not, the layout does to converge as nicely, even if we double the number of iterations
        layout.doLayout(model2, null, width, height);
        layout.doLayout(model2, null, width, height);
        outputApi.setZoom(.8);
        render.doRender(outputApi, model2, false, modelMap, outputApiMap);
//                animStart(30);
    }
    function displayError(error, d3_obj, viz_id, offset) {
        var div_width = $("#" + viz_id).width();
        var div_height = $("#" + viz_id).height();
        var x = (div_width / 4) + (div_width / 8);
        var y = (div_height / 2) + (div_height / 8);
        if (offset === undefined) {
            offset = 0;
        }
        d3_obj.select("#" + viz_id).append("text")
                .attr("x", x + offset)
                .attr("y", y)
                .attr("fill", "black")
                .attr("font-size", "50px")
                .attr("opacity", .4)
                .text(error);
    }

    function showDiactivatedViz(viz_id) {
        var viz_container = $("#" + viz_id + "_div");
        //viz_container.addClass("inactive_details_viz");
        viz_container.css({
            "border-top": "0px",
            "background-color": "#777"
        });
        //var viz_table = viz_container.closest("table");
        //viz_table.find("th:nth-child(" + index + ")").css( "color", "#ccc");
    }
    function showManifest() {
        window.open('/StackV-web/ops/details/manifestPortal.jsp?uuid=' + location.search.split("?uuid=")[1], 'newwindow', config = 'height=1200,width=700, top=0,left=800, toolbar=no, menubar=no, scrollbars=no, resizable=no,location=no, directories=no, status=no');
    }
    function createTextToggle(prefix, textModel) {
        var button = $("#" + prefix + "_viz_toggle_model");
        var viz_svg = button.siblings(".details_viz");
        var viz_div = button.closest("div");
        var width = viz_svg.width();
        width -= width / 4;
        var height = viz_svg.height();

        var textModelDiv = "<div id=\"" + prefix + "_text_div\" class=\"hide\"> <pre style=\"width: " + width +
                "px; height: " + height + "px;overflow:scroll;\"> " + textModel
                + "</pre></div>";
        viz_svg.after(textModelDiv);

        button.click(function (evt) {
            if (!viz_svg.hasClass("hide")) {
                viz_svg.addClass("hide");
                button.html("View Visualization");
                $("#" + prefix + "_text_div").removeClass("hide");
            } else {
                button.html("View Text Model");
                $("#" + prefix + "_text_div").addClass("hide");
                viz_svg.removeClass("hide");
            }
        });
    }

    function disableButtons(prefix) {
        $("#" + prefix + "_viz_recenter_button").attr('disabled', 'disabled');
        $("#" + prefix + "_viz_toggle_model").attr('disabled', 'disabled');
    }

    function renderModels() {
        var UUID = location.search.split("?uuid=")[1];
        $.ajax({
            crossDomain: true,
            type: "GET",
            url: "/StackV-web/restapi/service/delta/" + UUID,
            dataType: "json",

            success: function (data, textStatus, jqXHR) {
                if (data.serviceModelAddition && data.serviceModelAddition !== "{ }") {
                    var servAObj = JSON.parse(data.serviceModelAddition);
                    var servAModel = new ModelConstructor();
                    servAModel.makeServiceDtlModel(servAObj, model);
                    modelMap["serva_viz"] = servAModel;
                    var outputApi = new outputApi_(render.API, contextMenu, "serva_viz");
                    outputApiMap["serva_viz"] = outputApi;
                    drawGraph(outputApi, servAModel);
                    $("#serva_viz_recenter_button").click(function (evt) {
                        recenterGraph(outputApi, servAModel);
                        evt.preventDefault();
                    });
                    createTextToggle("serva", data.serviceModelAddition);
                } else {
                    displayError("Empty", d3, "serva_viz");
                    disableButtons("serva");
                }

                if (data.serviceModelReduction && data.serviceModelReduction !== "{ }") {
                    var servRObj = JSON.parse(data.serviceModelReduction);
                    var servRModel = new ModelConstructor();
                    servRModel.makeServiceDtlModel(servRObj, model);
                    modelMap["servr_viz"] = servRModel;
                    var outputApi2 = new outputApi_(render.API, contextMenu, "servr_viz");
                    outputApiMap["servr_viz"] = outputApi2;
                    drawGraph(outputApi2, servRModel);
                    $("#servr_viz_recenter_button").click(function (evt) {
                        recenterGraph(outputApi2, servRModel);
                        evt.preventDefault();
                    });
                    createTextToggle("servr", data.serviceModelReduction);
                } else {
                    displayError("Empty", d3, "servr_viz");
                    disableButtons("servr");
                }

                if (data.systemModelAddition && data.systemModelAddition !== "{ }") {
                    var sysAObj = JSON.parse(data.systemModelAddition);
                    var sysAModel = new ModelConstructor();
                    sysAModel.initWithMap(sysAObj, model);
                    modelMap["sysa_viz"] = sysAModel;
                    var outputApi3 = new outputApi_(render.API, contextMenu, "sysa_viz");
                    outputApiMap["sysa_viz"] = outputApi3;
                    drawGraph(outputApi3, sysAModel);
                    $("#sysa_viz_recenter_button").click(function (evt) {
                        recenterGraph(outputApi3, sysAModel);
                        evt.preventDefault();
                    });
                    createTextToggle("sysa", data.systemModelAddition);
                } else {
                    displayError("Empty", d3, "sysa_viz");
                    disableButtons("sysa");
                }

                if (data.systemModelReduction && data.systemModelReduction !== "{ }") {
                    var sysrObj = JSON.parse(data.systemModelReduction);
                    var sysrModel = new ModelConstructor();
                    sysrModel.initWithMap(sysrObj, model);
                    modelMap["sysr_viz"] = sysrModel;
                    var outputApi4 = new outputApi_(render.API, contextMenu, "sysr_viz");
                    outputApiMap["sysr_viz"] = outputApi4;
                    drawGraph(outputApi4, sysrModel);
                    $("#sysr_viz_recenter_button").click(function (evt) {
                        recenterGraph(outputApi4, sysrModel);
                        evt.preventDefault();
                    });
                    createTextToggle("sysr", data.systemModelReduction);
                } else {
                    displayError("Empty", d3, "sysr_viz");
                    disableButtons("sysr");
                }

            },

            error: function (jqXHR, textStatus, errorThrown) {
                //alert("Error getting status.");
                // alert("textStatus: " + textStatus + " errorThrown: " + errorThrown);
                displayError("Unavailable", d3, "serva_viz");
                disableButtons("serva");
                displayError("Unavailable", d3, "servr_viz");
                disableButtons("servr");
                displayError("Unavailable", d3, "sysa_viz");
                disableButtons("sysa");
                displayError("Unavailable", d3, "sysr_viz");
                disableButtons("sysr");
            }
        });

        $.ajax({
            crossDomain: true,
            type: "GET",
            url: "/StackV-web/restapi/app/service/lastverify/" + UUID,
            dataType: "json",

            success: function (data, textStatus, jqXHR) {
                if (data.verified_addition && data.verified_addition !== '{ }') {
                    var vaObj = JSON.parse(data.verified_addition);
                    var vaModel = new ModelConstructor();
                    vaModel.initWithMap(vaObj, model);
                    modelMap["va_viz"] = vaModel;
                    var outputApi5 = new outputApi_(render.API, contextMenu, "va_viz");
                    outputApiMap["va_viz"] = outputApi5;
                    drawGraph(outputApi5, vaModel);
                    $("#va_viz_recenter_button").click(function (evt) {
                        recenterGraph(outputApi5, vaModel);
                        evt.preventDefault();
                    });
                    createTextToggle("va", data.verified_addition);
                } else {
                    displayError("Empty", d3, "va_viz");
                    disableButtons("va");
                }

                if (data.verified_reduction && data.verified_reduction !== '{ }') {
                    var vrObj = JSON.parse(data.verified_reduction);
                    var vrModel = new ModelConstructor();
                    vrModel.initWithMap(vrObj, model);
                    modelMap["vr_viz"] = vrModel;
                    var outputApi6 = new outputApi_(render.API, contextMenu, "vr_viz");
                    outputApiMap["vr_viz"] = outputApi6;
                    drawGraph(outputApi6, vrModel);
                    $("#vr_viz_recenter_button").click(function (evt) {
                        recenterGraph(outputApi6, vrModel);
                        evt.preventDefault();
                    });
                    createTextToggle("vr", data.verified_reduction);
                } else {
                    displayError("Empty", d3, "vr_viz");
                    disableButtons("vr");
                }

                if (data.unverified_addition && data.unverified_addition !== '{ }') {
                    var uaObj = JSON.parse(data.unverified_addition);
                    var uaModel = new ModelConstructor();
                    uaModel.initWithMap(uaObj, model);
                    modelMap["ua_viz"] = uaModel;
                    var outputApi7 = new outputApi_(render.API, contextMenu, "ua_viz");
                    outputApiMap["ua_viz"] = outputApi7;
                    drawGraph(outputApi7, uaModel);
                    $("#ua_viz_recenter_button").click(function (evt) {
                        recenterGraph(outputApi7, uaModel);
                        evt.preventDefault();
                    });
                    createTextToggle("ua", data.unverified_addition);
                } else {
                    displayError("Empty", d3, "ua_viz");
                    disableButtons("ua");
                }

                if (data.unverified_reduction && data.unverified_reduction !== '{ }') {
                    var urObj = JSON.parse(data.unverified_reduction);
                    var urModel = new ModelConstructor();
                    urModel.initWithMap(urObj, model);
                    modelMap["ur_viz"] = urModel;
                    var outputApi8 = new outputApi_(render.API, contextMenu, "ur_viz");
                    outputApiMap["ur_viz"] = outputApi8;
                    drawGraph(outputApi8, urModel);
                    $("#ur_viz_recenter_button").click(function (evt) {
                        recenterGraph(outputApi8, urModel);
                        evt.preventDefault();
                    });
                    createTextToggle("ur", data.unverified_reduction);
                } else {
                    displayError("Empty", d3, "ur_viz");
                    disableButtons("ur");
                }
            },

            error: function (jqXHR, textStatus, errorThrown) {
                //alert("Error getting status.");
                // alert("textStatus: " + textStatus + " errorThrown: " + errorThrown);
                displayError("Unavailable", d3, "va_viz");
                disableButtons("va");
                displayError("Unavailable", d3, "ur_viz");
                disableButtons("ur");
                displayError("Unavailable", d3, "ua_viz");
                disableButtons("ua");
                displayError("Unavailable", d3, "vr_viz");
                disableButtons("vr");
            }
        });

    }
    function outputApi_(renderAPI, contextMenu, svg) {
        var that = this;
        this.renderApi = renderAPI;
        this.contextMenu = contextMenu;
        this.svgContainerName = svg;

        this.getSvgContainer = function () {
            return d3.select("#" + this.svgContainerName);
        };


        var displayTree = new DropDownTree(document.getElementById(that.svgContainerName + "_treeMenu"), that);
        displayTree.renderApi = this.renderApi;
        displayTree.contextMenu = this.contextMenu;

        this.getDisplayTree = function () {
            return displayTree;
        };

        this.setDisplayName = function (name) {
            document.getElementById(that.svgContainerName + "_displayName").innerText = name;
        };

        var zoomFactor = settings.INIT_ZOOM;
        var offsetX = 0, offsetY = 0;
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
            that.setZoom(.8);
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

        $(".displayPanel").draggable({handle: ".displayPanelBar"});
    }
</script>        
<!-- MAIN PANEL -->
<div id="pane">
</div>
<script>
    make_viz("pane", "va");
    make_viz("pane", "vr");
    make_viz("pane", "ua");
    make_viz("pane", "ur");
    make_viz("pane", "sysa");
    make_viz("pane", "serva");
    make_viz("pane", "servr");
    make_viz("pane", "sysr");
    make_display_panel("pane", "va_viz");
    make_display_panel("pane", "vr_viz");
    make_display_panel("pane", "ua_viz");
    make_display_panel("pane", "ur_viz");
    make_display_panel("pane", "sysa_viz");
    make_display_panel("pane", "serva_viz");
    make_display_panel("pane", "servr_viz");
    make_display_panel("pane", "sysr_viz");

    onload();
</script>
<!-- CONTEXT MENU -->
<nav id="context-menu" class="context-menu">
    <ul class="context-menu__items">
        <li class="context-menu__item">
            <a href="#" class="context-menu__link" data-action="ModelBrowser"><i class="fa  fa-sitemap"></i>View Model Browser</a>
        </li>
    </ul>
</nav>

<div id="dialog_policyAction" title="Policy Action">
</div>
<div id="dialog_policyData" title="Policy Data" >
</div>

<!--          <div id="displayPanel">
                      <div id="displayPanelBar">
            <div id="displayPanelCloserBar">
                <i id="displayPanelCloser" class="fa fa-times" aria-hidden="true"></i>
            </div>
        </div>

            <div id="displayPanel-contents">
                <div id="displayName"></div>
                <div id="treeMenu"></div>                
            </div>
            <div id="displayPanel-actions-container">
                <div id="displayPanel-actions">
                    <button id="backButton">Back</button>
                    <button id="forwardButton">Forward</button>
                </div>
            </div>
           </div>       -->

<!-- DEFINITION SVG -->
<svg id="definition_svg" visibility = "hidden" >
<defs> 
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
<filter id="spaImportFromOutlineFF" width="2000000%" height="2000000%" x="-500%" y="-500%" > 
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
<filter id="spaExportToOutlineFF" width="2000000%" height="2000000%" x="-500%" y="-500%" > 
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
<filter id="spaDependOnOutlineFF" width="2000000%" height="2000000%" x="-500%" y="-500%" > 
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

<filter id="ghost" width="2000000%" height="2000000%" x="-1000000%" y="-1000000%"> 
    <feColorMatrix type="saturate" values=".2"/> 
</filter> 
</defs>
</svg>
