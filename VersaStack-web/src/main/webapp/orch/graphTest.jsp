<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page errorPage = "/VersaStack-web/errorPage.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<jsp:useBean id="user" class="web.beans.userBeans" scope="session" />
<jsp:setProperty name="user" property="*" />  
<jsp:useBean id="serv" class="web.beans.serviceBeans" scope="page" />
<jsp:setProperty name="serv" property="*" />  
<!DOCTYPE html>
<html>
    <head>
        <meta charset="utf-8">
        <title>Graphical View</title>
        <script src="/VersaStack-web/js/jquery/jquery.js"></script>
        <script src="/VersaStack-web/js/bootstrap.js"></script>
        <script src="/VersaStack-web/js/nexus.js"></script>

        <link rel="stylesheet" type="text/css" href="/VersaStack-web/css/graphTest.css">
        <link rel="stylesheet" href="/VersaStack-web/css/animate.min.css">
        <link rel="stylesheet" href="/VersaStack-web/css/font-awesome.min.css">
        <link rel='stylesheet prefetch' href='http://fonts.googleapis.com/css?family=Roboto:400,100,400italic,700italic,700'>
        <link rel="stylesheet" href="/VersaStack-web/css/bootstrap.css">
        <link rel="stylesheet" href="/VersaStack-web/css/style.css">       
      <link rel="stylesheet" href="/VersaStack-web/css/contextMenu.css">   
      <!-- font awesome icons won't show up otherwise --->
<link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/font-awesome/4.6.1/css/font-awesome.min.css">

        <script>
            $(document).ready(function () {
                $("#nav").load("/VersaStack-web/navbar.html");

                $("#sidebar").load("/VersaStack-web/sidebar.html", function () {
                    if (${user.isAllowed(1)}) {
                        var element = document.getElementById("service1");
                        element.classList.remove("hide");
                    }
                    if (${user.isAllowed(2)}) {
                        var element = document.getElementById("service2");
                        element.classList.remove("hide");
                    }
                    if (${user.isAllowed(3)}) {
                        var element = document.getElementById("service3");
                        element.classList.remove("hide");
                    }
                    if (${user.isAllowed(4)}) {
                        var element = document.getElementById("service4");
                        element.classList.remove("hide");
                    }
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

            var outputApi;

            function onload() {
                require(["local/versastack/topology/model",
                    "local/versastack/topology/layout",
                    "local/versastack/topology/render",
                    "local/d3",
                    "local/versastack/utils",
                    "local/versastack/topology/DropDownTree",
                    "local/versastack/topology/ContextMenu",
                    "local/versastack/topology/TaggingDialog"
                ],
                        function (m, l, r, d3_, utils_, tree, c, td) {
                            ModelConstructor = m;
                            model = new ModelConstructor();
                            model.init(1, drawGraph, null);
                            layout = l;
                            render = r;
                            d3 = d3_;
                            utils = utils_;
                            map_ = utils.map_;
                            DropDownTree = tree;
                            ContextMenu = c; 
                            TaggingDialog = td;
                            taggingDialog = new TaggingDialog("${user.getUsername()}");
                            
                            taggingDialog.init();
                            // possibly pass in map here later for all possible dialogs 
                            contextMenu = new ContextMenu(d3, render.API, taggingDialog);//, taggingDialog);
                            contextMenu.init();
                            outputApi = new outputApi_(render.API, contextMenu);
                        });

                var request = new XMLHttpRequest();
                request.open("GET", "/VersaStack-web/data/json/umd-anl-all.json");

                request.setRequestHeader("Accept", "application/json");
                request.onload = function () {
                    var modelData = request.responseText;

                    if (modelData.charAt(0) === '<') {
                        return;
                    }

                    modelData = JSON.parse(modelData);
                    $.post("/VersaStack-web/ViewServlet", {newModel: modelData.ttlModel}, function (response) {
                        // handle response from your servlet.
                    });
                };
                request.send();

                $("#loadingPanel").addClass("hide");
                $("#hoverdiv").removeClass("hide");
                $("#viz").attr("class", "");
                
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
                
//                var ns = model.listNodes();
//                for (var i in ns) {
//                    ns[i].svgNode.on("contextmenu", contextMenu.setContextListenerRendered.bind(undefined, ns[i]));
//                    if (ns[i].svgNodeAnchor) {
//                        ns[i].svgNodeAnchor.on("contextmenu", contextMenu.setContextListenerRendered.bind(undefined, ns[i]));
//                    }
//                }
                  outputApi.initD3MenuEvents();
//                animStart(30);
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
                     outputApi.initD3MenuEvents();
                    outputApi.renderApi.selectElement(null);
                }, null);

//                var request = new XMLHttpRequest();
//                request.open("GET", "/VersaStack-web/restapi/model/");
//
//                request.setRequestHeader("Accept", "application/json");
//                request.onload = function () {
//                    var modelData = request.responseText;
//
//                    if (modelData.charAt(0) === '<') {
//                        return;
//                    }
//
//                    modelData = JSON.parse(modelData);
//                    $.post("/VersaStack-web/ViewServlet", {newModel: modelData.ttlModel}, function (response) {
//                        // handle response from your servlet.
//                    });
//                };
//                request.send();

                $("#loadingPanel").addClass("hide");
                $("#hoverdiv").removeClass("hide");
                $("#viz").attr("class", "");
            }

            function filter(viewName, viewModel) {
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
                    layout.doLayout(model, lockNodes, width, height);
                    layout.doLayout(model, lockNodes, width, height);

                    render.doRender(outputApi, model);
                }, viewModel);

                $.post("/VersaStack-web/ViewServlet", {filterName: viewName, filterModel: viewModel.ttlModel}, function (response) {
                    // handle response from your servlet.
                });

                $("#loadingPanel").addClass("hide");
                $("#hoverdiv").removeClass("hide");
                $("#viz").attr("class", "");
            }

            function buttonInit() { //@
                $("#testButton").click(function (evt) {
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
                    
//                    var zoom = d3.behavior.zoom();
//                    var viewCenter = [];
//
//                    viewCenter[0] = (-1)*zoom.translate()[0] + (0.5) * (  width/zoom.scale() );
//                    viewCenter[1] = (-1)*zoom.translate()[1] + (0.5) * ( height/zoom.scale() );
        
                    outputApi.resetZoom();
                    render.doRender(outputApi, model);
                    outputApi.initD3MenuEvents();

                    evt.preventDefault();
                });
                $("#stopButton").click(function (evt) {
                   layout.stop(); 
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
                    $(".current-filter").removeClass("current-filter");

                    var viewModels = ${user.getModels()};
                    if (this.id === "nofilter") {
                        reload();
                    } else {
                        filter(this.id, viewModels[this.id]);
                    }

                    $(this).addClass("current-filter");

                    evt.preventDefault();
                });
            
                $("#taggingPanel-tab").click(function (evt) {
                    $("#taggingPanel").toggleClass("closed");

                    evt.preventDefault();
                });
                
                $("#displayPanel-tab").click(function (evt) {
                    $("#displayPanel").toggleClass("closed");

                    evt.preventDefault();
                });
                
                $("#taggingsPanel-tab").click(function (evt) {
                    $("#taggingsPanel").toggleClass("closed");

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


            function outputApi_(renderAPI, contextMenu) {
                var that = this;
                this.renderApi = renderAPI;
                this.contextMenu = contextMenu;
                
                this.getSvgContainer = function () {
                    return d3.select("#viz");
                };

                var displayTree = new DropDownTree(document.getElementById("treeMenu"));
                displayTree.renderApi = this.renderApi;
                displayTree.contextMenu = this.contextMenu;
                
                this.getDisplayTree = function () {
                    return displayTree;
                };

                this.setDisplayName = function (name) {
                    document.getElementById("displayName").innerText = name;
                };

                this.initD3MenuEvents = function() {
                    var ns = model.listNodes();
                    for (var i in ns) {
                        ns[i].svgNode.on("contextmenu", contextMenu.setContextListenerRendered.bind(undefined, ns[i]));
                        if (ns[i].svgNodeAnchor) {
                            ns[i].svgNodeAnchor.on("contextmenu", contextMenu.setContextListenerRendered.bind(undefined, ns[i]));
                        }
                    }

                    var ns = model.listServices();
                    for (var i in ns) {
                        if (!ns[i].svgNode) console.log("graphTest.jsp: initD3MenuEvnts: name of service  with null svgNode: " + ns[i].getName());
                        ns[i].svgNode.on("contextmenu", contextMenu.setContextListenerRendered.bind(undefined, ns[i]));
                        if (ns[i].svgNodeAnchor) {
                            ns[i].svgNodeAnchor.on("contextmenu", contextMenu.setContextListenerRendered.bind(undefined, ns[i]));
                        }
                    }

                    var ns = model.listSubnets();
                    for (var i in ns) {
                        ns[i].svgNode.on("contextmenu", contextMenu.setContextListenerRendered.bind(undefined, ns[i]));
                        if (ns[i].svgNodeAnchor) {
                            ns[i].svgNodeAnchor.on("contextmenu", contextMenu.setContextListenerRendered.bind(undefined, ns[i]));
                        }
                    }


                };

                this.initD3MenuPortEvents = function(ports) {
                    for (var i in ports) {
                        ports[i].svgNode.on("contextmenu", contextMenu.setContextListenerRendered.bind(undefined, ports[i]));
                        if (ports[i].svgNodeAnchor) {
                            ports[i].svgNodeAnchor.on("contextmenu", contextMenu.setContextListenerRendered.bind(undefined, ports[i]));
                        }
                    }                
                };

                this.initD3MenuVolumeEvents = function(volumes) {
                    for (var i in volumes) {
                        volumes[i].svgNode.on("contextmenu", contextMenu.setContextListenerRendered.bind(undefined, volumes[i]));
                        if (volumes[i].svgNodeAnchor) {
                            volumes[i].svgNodeAnchor.on("contextmenu", contextMenu.setContextListenerRendered.bind(undefined, volumes[i]));
                        }
                    }            
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

                this.resetZoom = function () {   // @
                    zoomFactor = settings.INIT_ZOOM;
                    offsetX = 0;
                    offsetY = 0;                    
                    this._updateTransform();
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
                    // && (e.which ==== 1) stops d3 bug of dragging to enable on context menu 
                    if (isPanning && panningEnabled && (e.which === 1) )   {
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

        <div class="closed" id="taggingsPanel">
            <div id="taggingsPanel-tab">
                Tags
            </div>
            <div id ="taggingsPanel-contents">
                <div id="taggingPanel-colorPanel">
                    <div id="taggingPanel-colorPanelTitle"> Filter Colors</div>
           <div id="taggingPanelColorSelectionTab" style=" float:left;">
                <span class="filteredColorBox" id="boxRed"> 
                </span>
                <span class="filteredColorBox" id="boxOrange">
                </span>
                <span class="filteredColorBox" id="boxYellow">
                </span>
                <span class="filteredColorBox" id="boxGreen">
                </span>
                <span class="filteredColorBox" id="boxBlue">
                </span>
                <span class="filteredColorBox" id="boxPurple">
                </span>
                   
                    </div>
                </div>
                <div id="taggingPanel-labelPanel">
                    <div id="taggingPanel-labelPanelTitle">Labels</div>
                    <ul class="taggingPanel-labelList" id="labelList1">
<!--                      <li class="taggingPanel-labelItem label-color-red"> Label</li>
                      <li class="taggingPanel-labelItem label-color-blue"> Label</li>
                      <li class="taggingPanel-labelItem label-color-orange"> Label </li>
                      <li class="taggingPanel-labelItem label-color-purple"> Label </li>-->
                      
                    </ul>
                </div>
             </div>
        </div>

        <div id="loadingPanel"></div>
        <div class="closed" id="displayPanel">
            <div id="displayPanel-contents">
                <button id="refreshButton">Refresh</button>
                <button id="modelButton">Display Model</button>
                <button id="fullDiaplayButton">Toggle Full Model</button>
                <button id="testButton">test</button> <!-- @ -->
                <button id="stopButton">stop</button> <!-- @ -->
                <div id="displayName"></div>
                <div id="treeMenu"></div>                
            </div>
            <div id="displayPanel-actions-container">
            <div id="displayPanel-actions">
                <button id="backButton">Back</button>
                <button id="forwardButton">Forward</button>
                <div id="URISeachContainer" style="float:right;padding-left:10px;">
                    Search
                    <input type="text" name="Search" id="URISearchInput" placeholder="Enter URI">
                    <input type="submit" id= "URISearchSubmit" value="Submit">
                </div>

                <div id="actionForm"></div>
            </div>
            <div id="displayPanel-tab">^^^^^</div>
            </div>
        </div>        
        <div class="hide" id="hoverdiv"></div>        

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
    <g id="volumeDialogBox"/>
    <g id="switchPopup"/>
    <g id="parentPort"/>
    <g id="edge2" />
    <g id="port"/>
    <g id="volume"/>

    </g>
    </svg>
    
  <nav id="context-menu" class="context-menu">
      <ul class="context-menu__items">
        <li class="context-menu__item">
          <a href="#" class="context-menu__link" data-action="View"><i class="fa fa-eye"></i> Add Tag</a>
        </li>
      </ul>
    </nav>

<div id="taggingDialog">
  <div id="taggingDialogBar">
    <div id="taggingDialogCloser">
<i class="fa fa-times" aria-hidden="true"></i>
    </div>
  </div>
  
  <div id="taggingDialogContent">
    <div id="taggingDialogLabelInputContainter">
    <input type="text" name="labelInput" id="taggingDialogLabelInput" placeholder="Enter label.">
    </div>
    
    <div id="taggingDialogColorInputContainer">
      <div id="taggingDialogColorInputLabel">
        Select Color
      </div>
      
      <div id="taggingDialogColorSelectionTab">

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
    
    <div id="taggingDialogButtonContainer">
      <button id="taggingDialogCancel">
        Cancel
      </button>
      
      <button id="taggingDialogOK">
        Ok 
      </button>
    </div>
  </div>
</div>    
    
    <script>
       (function() {
            var tags = []; // stores tag objects {color, data, label}
            var selectedColors = []; // colors selected for filtering
            
            var colorBoxes = document.getElementsByClassName("filteredColorBox");
            var tagHTMLs = document.getElementsByClassName("taggingPanel-labelItem");
            var that = this;
            
            this.init = function() {
                var userName = "${user.getUsername()}";
                $.ajax({
                    crossDomain: true,
                    type: "GET",
                    url: "/VersaStack-web/restapi/app/label/" + userName,
                    dataType: "json",
        
                    success: function(data,  textStatus,  jqXHR ) {
                        for (var i = 0, len = data.length; i < len; i++) {
                            var dataRow = data[i];
                            that.createTag(dataRow[0], dataRow[1], dataRow[2]);
                        }
                    },
                    
                    error: function(jqXHR, textStatus, errorThrown ) {
                       alert(errorThrown + "\n"+textStatus);
                       alert("Error retrieving tags.");
                    }                  
                });

                for (var i = 0; i < colorBoxes.length;  i++) {
                    colorBoxes[i].onclick = function() {
                        var selectedColor = this.id.split("box")[1].toLowerCase();
                        var selectedIndex = selectedColors.indexOf(selectedColor);
                        if (selectedIndex === -1) {
                            selectedColors.push(selectedColor);
                            this.classList.add( "colorBox-highlighted");
                        } else {
                            selectedColors.splice(selectedIndex, 1);
                            this.classList.remove("colorBox-highlighted");
                        }      
                        
                        that.updateTagList();
                    };
                }
            };  
            
            this.updateTagList = function() {
               var tagHTMLs = document.getElementsByClassName("taggingPanel-labelItem");
               for( var i = 0; i < tagHTMLs.length; i++){
                   var curTag = tagHTMLs.item(i);
                   var curColor = curTag.classList.item(1).split("label-color-")[1];
                   if (selectedColors.length === 0) {
                       curTag.classList.remove("hide");
                   } else if (selectedColors.indexOf(curColor) === -1){
                       curTag.classList.add("hide");
                   } else {
                       curTag.classList.remove("hide");
                   }
               }
            };
            
            this.createTag = function(label, data, color) {
                var tagList = document.querySelector("#labelList1");
                var tag = document.createElement("li");
                tag.classList.add("taggingPanel-labelItem");
                tag.classList.add("label-color-" + color.toLowerCase());
                tag.innerHTML = label;
                tag.onclick = function() {
                    var textField = document.createElement('textarea');
                    textField.innerText = data;
                    document.body.appendChild(textField);
                    textField.select();
                    document.execCommand('copy');
                    $(textField).remove();                    
                };
                tagList.appendChild(tag);
            };
    
            this.init();
        })();
    </script>
</body>

</html>
