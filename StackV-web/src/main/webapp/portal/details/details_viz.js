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

function details_viz(token) {
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
            beforeSend: function (xhr) {
                xhr.setRequestHeader("Authorization", "bearer " + token);
            },

            dataType: "text",

            success: function (data, textStatus, jqXHR) {
                if (data === "true") {
                    //                                //alert(textStatus);
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
                    model.init(1, renderModels, null, "default", token);

                    functionMap["ModelBrowser"] = function (o, m, e) {
                        positionDisplayPanel(m + "_displayPanel", e);
                        var browser = document.querySelector("#" + m + "_displayPanel");
                        $(".displayPanel").removeClass("displayPanel-active");
                        browser.classList.add("displayPanel-active");
                        render.API.selectElement(o, outputApiMap[m]);

                    };

                    contextMenu = new ContextMenu(d3, render.API, functionMap);//, tagDialog);
                    //
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
    var viz = "<div id=\"" + prefix + "_viz_div\" class=\"hidden details_viz_div\">" +
        "<div class=\"hover_div\" id=\"hoverdiv_" + prefix + "_viz\"></div>" +
        "<svg class =\"details_viz\" id= \"" + prefix + "_viz\"> " +
        "<defs>" +
        "  <marker id=\"marker_arrow_" + prefix + "_viz\" markerWidth=\"10\" markerHeight=\"10\" refx=\"15\" refy=\"3\" orient=\"auto\" markerUnits=\"strokeWidth\">" +
        "      <path d=\"M0,0 L0,6 L9,3 z\" fill=\"black\" />" +
        "  </marker>" +
        "</defs>" +
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
        viz += "<button class=\"details-viz-button btn btn-default\" id=\"manifest_button\" onclick=\"showManifest()\">Display Manifest</button>";
    }

    viz += "<button class=\"details-viz-button btn btn-default details-viz-recenter-button\" id=\"" + prefix + "_viz_recenter_button\">Recenter</button>" +
        "<button  class=\"details-viz-button btn btn-default\" id=\"" + prefix + "_viz_toggle_model\">View Text Model</button>" +
        "</div>";

    $("#" + div_id).append(viz);
}



function recenterGraph(o, model, width, height) {
    o.resetZoom();
    if (width === undefined && height === undefined) {
        width = $("#" + o.svgContainerName).closest("td").width();
        height = $("#" + o.svgContainerName).closest("td").height();
    }
    layout.stop();

    if (o.svgContainerName.includes("serv")) {
        width = $(window).width() * .85;
        //$("#" + viz_id).width();
        height = $(window).height() * .1; //$("#" + viz_id).height();

        buildTreeViz(model.dept_trees, model.resources, model, width, height, o.svgContainerName);

        layout.doTreeLayout(model, null, width, height);
        layout.doTreeLayout(model, null, width, height);
    } else {
        layout.doLayout(model, null, width, height);
        layout.doLayout(model, null, width, height);
        o.setZoom(.8);
    }

    //layout.force().gravity(1).charge(-900).start();
    //        layout.doLayout(model, null, width, height);
    //        layout.doLayout(model, null, width, height);

    o.resetZoom();
    render.doRender(o, model, false, modelMap, outputApiMap);
}

function displayError(error, d3_obj) {
    d3_obj.select("#viz").append("text")
        .attr("x", $(window).width() / 2)
        .attr("y", $(window).height() / 2)
        .attr("fill", "black")
        .attr("font-size", "80px")
        .text(error);

}


function drawGraph(outputApi, model2) {
    // $("#" + outputApi.svgContainerName).width("50%");
    // $("#" + outputApi.svgContainerName).height("50%");

    if (outputApi.svgContainerName.includes("serv") || outputApi.svgContainerName.includes("sys")) {
        var width = $(window).width() * .85;
        //$("#" + viz_id).width();
        var height = $(window).height() * .1; //$("#" + viz_id).height();
    } else {
        var width = 993;
        //$("#" + viz_id).width();
        var height = 130; //$("#" + viz_id).height();
    }
    var tdID = $("#" + outputApi.svgContainerName).closest("td").attr("id");
    //TODO, figure out why we need to call this twice
    //If we do not, the layout does to converge as nicely, even if we double the number of iterations
    if (outputApi.svgContainerName.includes("serv")) {
        buildTreeViz(model2.dept_trees, model2.resources, model2, width, height, outputApi.svgContainerName);
        layout.doTreeLayout(model2, null, width, height);
        layout.doTreeLayout(model2, null, width, height);
        outputApi.setZoom(.8);
        render.doRender(outputApi, model2, false, modelMap, outputApiMap);

    } else {
        layout.doLayout(model2, null, width, height);
        layout.doLayout(model2, null, width, height);
        outputApi.setZoom(.8);
        render.doRender(outputApi, model2, false, modelMap, outputApiMap);
    }
    //                animStart(30);
}
function displayError(error, d3_obj, viz_id, offset) {
    if (viz_id.includes("serv") || viz_id.includes("sys")) {
        var div_width = $(window).width() * .85;
        //$("#" + viz_id).width();
        var div_height = $(window).height() * .1; //$("#" + viz_id).height();
    } else {
        var div_width = 993;
        //$("#" + viz_id).width();
        var div_height = 130; //$("#" + viz_id).height();
    }
    var x = (div_width / 4) + (div_width / 6);
    var y = (div_height / 2) + (div_height / 2);
    if (offset === undefined) {
        offset = 0;
    }

    if (error === "Empty") {
        $("#" + viz_id + "_div").addClass("emptyViz");
        $("#" + viz_id + "_div").trigger("emptyViz");
    }

    d3_obj.select("#" + viz_id).append("text")
        .attr("x", x + offset)
        .attr("y", y)
        .attr("fill", "black")
        .attr("font-size", "50px")
        .attr("opacity", .4)
        .text(error);
}

function displayText(text, d3_obj, viz_id, x, y, text_id) {
    var currentText = document.getElementById(text_id);
    if (currentText !== null) {
        currentText.innerHTML = text;
        currentText.setAttribute("x", x);
        currentText.setAttribute("y", y);
    } else {
        d3_obj.select("#" + viz_id).append("text")
            .attr("x", x)
            .attr("y", y)
            .attr("fill", "black")
            .attr("font-size", "25px")
            .attr("opacity", .4)
            .attr("id", text_id)
            .text(text);
    }
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
    var uuid = sessionStorage.getItem("instance-uuid");
    window.open("/StackV-web/portal/visual/manifest/manifestPortal.jsp?uuid=" + uuid, "newwindow", config = "height=1200,width=700, top=0,left=800, toolbar=no, menubar=no, scrollbars=no, resizable=no,location=no, directories=no, status=no");
}
function createTextToggle(prefix, textModel) {
    var button = $("#" + prefix + "_viz_toggle_model");
    var viz_svg = button.siblings(".details_viz");
    var viz_div = button.closest("div");
    var width = viz_svg.width();
    width -= width / 4;
    var height = viz_svg.height() + "px";

    if (prefix.includes("serv") || prefix.includes("sys")) {
        width = "inherit";
        height = "25vh";
    }

    var textModelDiv = "<div id=\"" + prefix + "_text_div\" class=\"hide details-viz-text-model\"> <pre class=\"details-viz-pre\" style=\"height: " + height + ";\"> " + textModel
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
    $("#" + prefix + "_viz_recenter_button").attr("disabled", "disabled");
    $("#" + prefix + "_viz_toggle_model").attr("disabled", "disabled");
}

function renderModels(UUID, token) {
    $.ajax({
        crossDomain: true,
        type: "GET",
        url: "/StackV-web/restapi/app/delta/" + UUID,
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + token);
        },
        dataType: "json",
        success: function (data, textStatus, jqXHR) {
            $(".details_viz_div").bind("emptyViz", function () {
                var id = $(this).attr("id");
                if (id.includes("ser") || id.includes("sys"))
                    removeIfEmpty(id);
            });
            if (data) {
                console.log("START :: " + JSON.stringify(data));
                if (data.serviceModelAddition && data.serviceModelAddition !== "{ }") {
                    console.log("LOADING");
                    var servAObj = JSON.parse(data.serviceModelAddition);
                    var servAModel = new ModelConstructor();
                    servAModel.makeServiceDtlModel(servAObj, model);
                    buildTreeLayout(servAModel);
                    modelMap["serva_viz"] = servAModel;
                    var outputApi = new outputApi_(render.API, contextMenu, "serva_viz");
                    outputApiMap["serva_viz"] = outputApi;
                    drawGraph(outputApi, servAModel);
                    console.log("DRAWN");
                    $("#serva_viz_recenter_button").on("click", function (evt, width, height) {
                        recenterGraph(outputApi, servAModel, width, height);
                        evt.preventDefault();
                    });
                    createTextToggle("serva", data.serviceModelAddition);
                } else {
                    displayError("", d3, "serva_viz");
                    disableButtons("serva");
                }

                if (data.serviceModelReduction && data.serviceModelReduction !== "{ }") {
                    var servRObj = JSON.parse(data.serviceModelReduction);
                    var servRModel = new ModelConstructor();
                    servRModel.makeServiceDtlModel(servRObj, model);
                    buildTreeLayout(servRModel);
                    modelMap["servr_viz"] = servRModel;
                    var outputApi2 = new outputApi_(render.API, contextMenu, "servr_viz");
                    outputApiMap["servr_viz"] = outputApi2;
                    drawGraph(outputApi2, servRModel);
                    $("#servr_viz_recenter_button").on("click", function (evt, width, height) {
                        recenterGraph(outputApi2, servRModel, width, height);
                        evt.preventDefault();
                    });
                    createTextToggle("servr", data.serviceModelReduction);
                } else {
                    displayError("", d3, "servr_viz");
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
                    $("#sysa_viz_recenter_button").on("click", function (evt, width, height) {
                        recenterGraph(outputApi3, sysAModel, width, height);
                        evt.preventDefault();
                    });
                    createTextToggle("sysa", data.systemModelAddition);
                } else {
                    displayError("", d3, "sysa_viz");
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
                    $("#sysr_viz_recenter_button").on("click", function (evt, width, height) {
                        recenterGraph(outputApi4, sysrModel, width, height);
                        evt.preventDefault();
                    });
                    createTextToggle("sysr", data.systemModelReduction);
                } else {
                    displayError("", d3, "sysr_viz");
                    disableButtons("sysr");
                }
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
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + token);
        },

        dataType: "json",

        success: function (data, textStatus, jqXHR) {
            if (data.verified_addition && data.verified_addition !== "{ }") {
                var vaObj = JSON.parse(data.verified_addition);
                var vaModel = new ModelConstructor();
                vaModel.initWithMap(vaObj, model);
                modelMap["va_viz"] = vaModel;
                var outputApi5 = new outputApi_(render.API, contextMenu, "va_viz");
                outputApiMap["va_viz"] = outputApi5;
                drawGraph(outputApi5, vaModel);
                $("#va_viz_recenter_button").on("click", function (evt, width, height) {
                    recenterGraph(outputApi5, vaModel, width, height);
                    evt.preventDefault();
                });
                createTextToggle("va", data.verified_addition);
            } else {
                displayError("", d3, "va_viz");
                disableButtons("va");
            }

            if (data.verified_reduction && data.verified_reduction !== "{ }") {
                var vrObj = JSON.parse(data.verified_reduction);
                var vrModel = new ModelConstructor();
                vrModel.initWithMap(vrObj, model);
                modelMap["vr_viz"] = vrModel;
                var outputApi6 = new outputApi_(render.API, contextMenu, "vr_viz");
                outputApiMap["vr_viz"] = outputApi6;
                drawGraph(outputApi6, vrModel);
                $("#vr_viz_recenter_button").on("click", function (evt, width, height) {
                    recenterGraph(outputApi6, vrModel, width, height);
                    evt.preventDefault();
                });
                createTextToggle("vr", data.verified_reduction);
            } else {
                displayError("", d3, "vr_viz");
                disableButtons("vr");
            }

            if (data.unverified_addition && data.unverified_addition !== "{ }") {
                var uaObj = JSON.parse(data.unverified_addition);
                var uaModel = new ModelConstructor();
                uaModel.initWithMap(uaObj, model);
                modelMap["ua_viz"] = uaModel;
                var outputApi7 = new outputApi_(render.API, contextMenu, "ua_viz");
                outputApiMap["ua_viz"] = outputApi7;
                drawGraph(outputApi7, uaModel);
                $("#ua_viz_recenter_button").on("click", function (evt, width, height) {
                    recenterGraph(outputApi7, uaModel, width, height);
                    evt.preventDefault();
                });
                createTextToggle("ua", data.unverified_addition);
            } else {
                displayError("", d3, "ua_viz");
                disableButtons("ua");
            }

            if (data.unverified_reduction && data.unverified_reduction !== "{ }") {
                var urObj = JSON.parse(data.unverified_reduction);
                var urModel = new ModelConstructor();
                urModel.initWithMap(urObj, model);
                modelMap["ur_viz"] = urModel;
                var outputApi8 = new outputApi_(render.API, contextMenu, "ur_viz");
                outputApiMap["ur_viz"] = outputApi8;
                drawGraph(outputApi8, urModel);
                $("#ur_viz_recenter_button").on("click", function (evt, width, height) {
                    recenterGraph(outputApi8, urModel, width, height);
                    evt.preventDefault();
                });
                createTextToggle("ur", data.unverified_reduction);
            } else {
                displayError("", d3, "ur_viz");
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
function removeIfEmpty(viz) {
    var viz = $("#" + viz);
    var table = viz.closest("table");
    if (viz.hasClass("emptyViz")) {
        table.addClass("hide");
        table.addClass("emptyVizTable");
    }
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
                    map_(form.value.split("\n"), function (subnetStr) {
                        if (subnetStr.trim() === toToggle) {
                            add = false;
                        } else {
                            subnetStrs.push(subnetStr);
                        }

                    });
                }
                if (add) {
                    subnetStrs.push(toToggle);
                }
                form.value = subnetStrs.join("\n");
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

    $(".displayPanel").draggable({ handle: ".displayPanelBar" });
}


function getPolicyActions(model) {
    policyActions = [];

    for (var i in model.elementMap) {
        if (model.elementMap[i].getType() === "PolicyAction") {
            policyActions.push(model.elementMap[i]);
        }
    }
    return policyActions;
}

function getDepth(policyAction, model) {
    var misc = policyAction.relationship_to;
    var max = 0;
    for (var elem in misc) {
        if (misc[elem] !== "dependOn")
            continue;
        var depth = getDepth(model.elementMap[elem], model);
        if (depth > max)
            max = depth;
    }
    return 1 + max;
}

function getTallestPolicy(policyActions, model) {
    var maxVal = -1;
    var max;
    for (var i = 0; i < policyActions.length; i++) {
        var depth = getDepth(policyActions[i], model);
        if (depth > maxVal) {
            maxVal = depth;
            max = policyActions[i];
        }
    }
    return max;
}

function containsElement(array, element) {
    for (var i = 0; i < array.length; i++) {
        if (array[i].getName() === element.getName())
            return true;
    }
    return false;
}

function buildTree(root, level, visited, model) {
    var misc = root.misc_elements;
    var res = [];
    if (root === undefined || containsElement(visited, root) || visited.length === Object.keys(model.policyMap).length)
        return res;

    visited.push(root);

    root.level = level;

    ++level;

    for (var i = 0; i < misc.length; i++) {
        if (!containsElement(visited, (misc[i]))) {
            if (misc[i].relationship_to[root.getName()] === "exportTo" ||
                misc[i].relationship_to[root.getName()] === "importFrom") {

                if (misc[i].getType().includes("Policy")) {
                    misc[i].level = level;
                    res = res.concat(buildTree(misc[i], level, visited, model));
                }

            }
        }
    }

    var rel = root.relationship_to;
    for (var elem in rel) {
        if (!containsElement(visited, model.elementMap[elem])) {

            if (model.elementMap[elem].getType().includes("Policy")) {
                res = res.concat(buildTree(model.elementMap[elem], level, visited, model));
            }

        }
    }

    return [root].concat(res);
}

function buildTreeLayout(model) {
    var policyActions = [];
    var resources = [];

    policyActions = getPolicyActions(model);

    var max = getTallestPolicy(policyActions, model);
    var noDep = getRoots(policyActions);

    var tree = buildTree(max, 0, [], model);

    tree = tree.filter(function (elem, index, self) {
        return index === self.indexOf(elem);
    });

    for (var i in model.elementMap) {
        if (!model.elementMap[i].getType().includes("Policy"))
            resources.push(model.elementMap[i]);
    }
    model.dept_trees = getMultipleTrees(noDep, tree, model);
    model.resources = resources;
}

function getRoots(policyActions) {
    var roots = [];
    for (var i = 0; i < policyActions.length; i++) {
        var elems = policyActions[i].misc_elements;
        var isRoot = true;
        for (var j = 0; j < elems.length; j++) {
            for (var k in elems[j].relationship_to) {
                if (k === policyActions[i].getName() && elems[j].relationship_to[k] === "dependOn") {
                    isRoot = false;
                }
            }
        }
        if (isRoot)
            roots.push(policyActions[i]);
    }
    return roots;
}
function getMultipleTrees(roots, first_tree, model) {
    var new_trees = [first_tree];
    for (var i = 0; i < roots.length; i++) {
        for (var j = 0; j < new_trees.length; j++) {
            if (isIsolatedTree(new_trees, roots[i])) {
                var tree = buildTree(roots[i], 0, [], model);
                new_trees.push(tree);
            }
        }
    }
    return new_trees;
}
function isIsolatedTree(trees, root) {
    for (var i = 0; i < trees.length; i++) {
        if (containsElement(trees[i], root))
            return false;
    }
    return true;
}

function getResourcesForTree(tree, model) {
    var resources = [];
    for (var i = 0; i < tree.length; i++) {
        var elems = tree[i].relationship_to;
        for (var name in elems) {
            if (name.startsWith("urn")) {
                resources.push(model.nodeMap[name]);
                var misc_elems = model.elementMap[name].misc_elements;
                for (var j = 0; j < misc_elems.length; j++) {
                    var misc = misc_elems[j];
                    var misc_name = misc.getName();
                    if (misc_name.startsWith("urn") && !containsElement(resources, misc)) {
                        resources.push(model.nodeMap[misc_name]);
                    }
                }
            }
        }
    }
    return resources;
}

function getAllResourcse(model) {
    var elems = model.elementMap;
    var resources = [];
    for (var elem in elems) {
        if (elem.startsWith("urn"))
            resources.push(model.nodeMap[elem]);
    }
    return resources;
}
function buildTreeViz(trees, resources, model, width, height, viz_id) {
    var horiz_constant = (1 / trees.length);
    horiz_constant += horiz_constant * horiz_constant;
    var horiz_offset = 0;
    var multitree_layout = (trees.length > 1);

    for (var treeIndx = 0; treeIndx < trees.length; treeIndx++) {
        var treeElems = trees[treeIndx];
        var treeResources = [];
        if (multitree_layout) {
            treeResources = getResourcesForTree(treeElems, model);
        } else {
            treeResources = getAllResourcse(model);
        }

        var startX = (width / 2) - (horiz_offset);
        var startY = height * Math.log2(treeElems.length) * 1.25;
        var curLevel = treeElems[0].level;
        var prevLevel = -1;
        var ind = 0;
        var e = [[]];

        for (var i = 0; i < treeElems.length; i++) {
            var elem = treeElems[i];
            var policy;
            var type = elem.getType();
            if (type.includes("Policy")) {
                policy = model.policyMap[elem.getName()];
            } else {
                policy = model.nodeMap[elem.getName()];
            }

            if (e[elem.level] !== undefined) {
                e[elem.level].push(policy);
            } else {
                while (e[elem.level] === undefined) {
                    e.push([]);
                }
                e[elem.level].push(policy);
            }
        }


        e[0][0].x = startX;
        e[0][0].y = startY;

        for (var i = 1; i < e.length; i++) {
            var lastStart = e[i - 1][0];
            var lastEnd = e[i - 1][e[i - 1].length - 1];

            var policies = e[i];
            var xOffset = ((lastEnd.x - lastStart.x) / policies.length) + screen.width * .04;
            xOffset = xOffset * horiz_constant;
            var yOffset = e[i - 1][0].y - screen.height * .05;

            for (var j = 0; j < policies.length; j++) {
                if (j === 0) {
                    policies[j].x = lastStart.x - (screen.width * .05 * horiz_constant);
                } else if (j === policies.length - 1 && e[i - 1].length <= e[i].length) {
                    policies[j].x = lastEnd.x + (screen.width * .05 * horiz_constant);
                } else {
                    policies[j].x = policies[j - 1].x + xOffset;
                }
                policies[j].y = yOffset;
            }

        }

        var resStartX = e[e.length - 1][0].x;
        var resStartY = e[e.length - 1][0].y - screen.height * .09;

        for (var i = 0; i < treeResources.length; i++) {
            var elem = treeResources[i];
            elem.x = resStartX;
            elem.y = resStartY;
            resStartX += (screen.width * .04 * horiz_constant);
        }


        horiz_offset += (e[e.length - 1][0].x - startX);
    }
}
