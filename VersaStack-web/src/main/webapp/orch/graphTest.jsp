<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page errorPage = "/VersaStack-web/errorPage.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<jsp:useBean id="user" class="web.beans.userBeans" scope="session" />
<jsp:setProperty name="user" property="*" />  
<jsp:useBean id="serv" class="web.beans.serviceBeans" scope="page" />
<jsp:setProperty name="serv" property="*" />  
<!DOCTYPE html>
<html style="width:100%; height: 100%">
    <head>
        <meta charset="utf-8">
        <title>Graphical View</title>
        <script src="/VersaStack-web/js/jquery/jquery.js"></script>
        <script src="/VersaStack-web/js/nexus.js"></script>

        <link rel="stylesheet" type="text/css" href="/VersaStack-web/css/graphTest.css">
        <link rel="stylesheet" href="/VersaStack-web/css/animate.min.css">
        <link rel="stylesheet" href="/VersaStack-web/css/font-awesome.min.css">
        <link rel='stylesheet prefetch' href='http://fonts.googleapis.com/css?family=Roboto:400,100,400italic,700italic,700'>
        <link rel="stylesheet" href="/VersaStack-web/css/bootstrap.css">
        <link rel="stylesheet" href="/VersaStack-web/css/style.css">       

        <script>
            $(document).ready(function () {
                $("#nav").load("/VersaStack-web/navbar.html");
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

            var outputApi;

            function onload() {
                require(["local/versastack/topology/model",
                    "local/versastack/topology/layout",
                    "local/versastack/topology/render",
                    "local/d3",
                    "local/versastack/utils",
                    "local/versastack/topology/DropDownTree"],
                        function (m, l, r, d3_, utils_, tree) {
                            ModelConstructor = m;
                            model = new ModelConstructor();
                            model.init(1, drawGraph, null);
                            layout = l;
                            render = r;
                            d3 = d3_;
                            utils = utils_;
                            map_ = utils.map_;
                            DropDownTree = tree;

                            outputApi = new outputApi_();
                        });
                buttonInit();
            }
            function drawGraph() {
                var width = document.documentElement.clientWidth / settings.INIT_ZOOM;
                var height = document.documentElement.clientHeight / settings.INIT_ZOOM;
                //TODO, figure out why we need to call this twice
                //If we do not, the layout does to converge as nicely, even if we double the number of iterations
                layout.doLayout(model, null, width, height);
                layout.doLayout(model, null, width, height);


                render.doRender(outputApi, model);
//                animStart(30);
            }
            function reload() {
                var lockNodes = model.listNodes();
                //var posistionLocks = {};
                model = new ModelConstructor(model);
                model.init(2, function () {
                    var width = document.documentElement.clientWidth / outputApi.getZoom();
                    var height = document.documentElement.clientHeight / outputApi.getZoom();
                    //TODO, figure out why we need to call this twice
                    //If we do not, the layout does to converge as nicely, even if we double the number of iterations
                    layout.doLayout(model, lockNodes, width, height);
                    layout.doLayout(model, lockNodes, width, height);

                    render.doRender(outputApi, model);
                }, null);

                var request = new XMLHttpRequest();
                request.open("GET", "/VersaStack-web/restapi/model/");

                request.setRequestHeader("Accept", "application/json");
                request.onload = function () {
                    var modelData = request.responseText;

                    console.log("Data: " + modelData);

                    if (modelData.charAt(0) === '<') {
                        return;
                    }

                    modelData = JSON.parse(modelData);                     
                    $.post("/VersaStack-web/ViewServlet", {newModel: modelData.ttlModel}, function(response) {
                        // handle response from your servlet.
                    });
                };
                request.send();
            }

            function filter(viewModel) {
                var lockNodes = model.listNodes();
                //var posistionLocks = {};
                model = new ModelConstructor(model);
                model.init(2, function () {
                    var width = document.documentElement.clientWidth / outputApi.getZoom();
                    var height = document.documentElement.clientHeight / outputApi.getZoom();
                    //TODO, figure out why we need to call this twice
                    //If we do not, the layout does to converge as nicely, even if we double the number of iterations
                    layout.doLayout(model, lockNodes, width, height);
                    layout.doLayout(model, lockNodes, width, height);

                    render.doRender(outputApi, model);
                }, viewModel);
                
                $.post("/VersaStack-web/ViewServlet", {filterModel: viewModel}, function(response) {
                        // handle response from your servlet.
                });
            }

            function buttonInit() {
                $("#awsButton").click(function (evt) {
                    $("#actionForm").load("/VersaStack-web/ops/srvc/vmadd.jsp?vm_type=aws #service-fields");

                    $("#awsButton").toggleClass("hide");
                    $("#cancelButton").toggleClass("hide");

                    evt.preventDefault();
                });
                $("#cancelButton").click(function (evt) {
                    $("#actionForm").empty();

                    $("#awsButton").toggleClass("hide");
                    $("#cancelButton").toggleClass("hide");

                    evt.preventDefault();
                });
                $("#refreshButton").click(function (evt) {
                    reload();

                    evt.preventDefault();
                });

                $("#modelButton").click(function (evt) {
                    window.open('/VersaStack-web/modelView.jsp', 'newwindow', config = 'height=1200,width=400, toolbar=no, menubar=no, scrollbars=no, resizable=no,location=no, directories=no, status=no');
                });

                $(".button-filter-select").click(function (evt) {

                    if (this.id === "nofilter") {
                        reload();
                    } else {
                        var buttonID = this.id;
                        console.log("Button ID: " + buttonID);
                        var viewModels = ${user.getModels()};
                        console.log("viewModels: " + viewModels);
                        //viewModels = JSON.parse(viewModels);
                        //console.log("viewModels Parsed: " + viewModels);
                        console.log("View Model: " + viewModels[buttonID]);
                        filter(viewModels[buttonID]);
                    }

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


            function outputApi_() {
                var that = this;
                this.getSvgContainer = function () {
                    return d3.select("#viz");
                };

                var displayTree = new DropDownTree(document.getElementById("treeMenu"));
                this.getDisplayTree = function () {
                    return displayTree;
                };

                this.setDisplayName = function (name) {
                    document.getElementById("displayName").innerText = name;
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
                    d3.select("#transform").
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
                    document.getElementById("hoverdiv").innerText = str;
                };
                this.setHoverLocation = function (x, y) {
                    document.getElementById("hoverdiv").style.left = x + "px";
                    document.getElementById("hoverdiv").style.top = y + 10 + "px";
                };
                this.setHoverVisible = function (vis) {
                    document.getElementById("hoverdiv").style.visibility = vis ? "visible" : "hidden";
                };


                var svg = document.getElementById("viz");
                svg.addEventListener("mousewheel", function (e) {
                    e.preventDefault();
                    //The OSX trackpad seems to produce scrolls two orders of magnitude large when using pinch to zoom,
                    //so we ignore the magnitude entirely
                    that.zoom(Math.sign(e.wheelDeltaY) * settings.ZOOM_FACTOR, e.offsetX, e.offsetY);
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
                    if (isPanning && panningEnabled) {
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

        <div id="filterPanel">
            <div>
                <button class="button-filter-select" id="nofilter">No Filter</button>
            </div>
            <div>
                <button class="button-filter-select" id="Test">Test Filter</button>
            </div>
            <div>
                <button class="button-filter-select" id="filter2"></button>
            </div>
            <div>
                <button class="button-filter-select" id="filter3"></button>
            </div>
        </div>

        <div id="displayPanel">
            <button id="refreshButton">Refresh</button>
            <button id="modelButton">Display Model</button>
            <div id="displayName"></div>
            <div id="treeMenu"></div>
            <div id="actionMenu">
                <button id="awsButton">Install AWS</button>
                <button id="cancelButton" class="hide">Minimize</button>
                <div id="actionForm"></div>
            </div>
        </div>
        <div id="hoverdiv"></div>
        <svg id="viz">
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
    <filter id="ghost" width="2000000%" height="2000000%" x="-1000000%" y="-1000000%">
        <feColorMatrix type="saturate" values=".2"/>
    </filter>
    </defs>
    <!--We nest a g in here because the svg tag itself cannot do transforms
        we separate topologies, edges, and nodes to create an explicit z-order
    -->
    <g id="transform">
    <g id="topology"/>
    <g id="edge1"/>
    <g id="anchor"/>
    <g id="node"/>
    <g id="dialogBox"/>
    <g id="switchPopup"/>
    <g id="parentPort"/>
    <g id="edge2" />
    <g id="port"/>

    </g>
    </svg>

</body>

</html>
