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
                
                $("#tag-panel").load("/VersaStack-web/tagPanel.jsp", function() {                    
                    var tp = document.querySelector("#tagPanel");
                    tp.style.left = "calc(40% - 66px)";
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
                    "local/versastack/topology/TagDialog"
                ],
                        function (m, l, r, d3_, utils_, tree, c, td) {
                          $.ajax({
                                   crossDomain: true,
                                   type: "GET",
                                   url: "/VersaStack-web/restapi/service/ready",
                                   dataType: "text", 

                                   success: function(data,  textStatus,  jqXHR ) {
                                       if (data === "true")  {
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
                                            tagDialog = new TagDialog("${user.getUsername()}");

                                            tagDialog.init();
                                            // possibly pass in map here later for all possible dialogs 
                                            contextMenu = new ContextMenu(d3, render.API, tagDialog);//, tagDialog);
                                            contextMenu.init();
                                            
                                            outputApi = new outputApi_(render.API, contextMenu, "viz");

                                            ModelConstructor = m;
                                            model = new ModelConstructor();
                                            model.init(1, drawGraph.bind(undefined, outputApi, model), null);                                            
                                       } else {
                                           displayError("Visualization Unavailable", d3_);
                                       }
                                   },

                                   error: function(jqXHR, textStatus, errorThrown ) {
                                        console.log("Debugging: timeout at start..");
                                        displayError("Visualization Unavailable", d3_);
                                     //alert("textStatus: " + textStatus + " errorThrown: " + errorThrown);
                                   }
                            }); 
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
                $("#hoverdiv_viz").removeClass("hide");

                $("#viz").attr("class", "");

                buttonInit();
            }
            
            function displayError(error, d3_obj) {
               d3_obj.select("#viz").append("text")
                       .attr("x", $(window).width() / 4)
                       .attr("y", $(window).height() / 2 )
                       .attr("fill", "black")
                       .attr("font-size", "80px")
                       .text(error);

               $('#servicePanel-contents').removeClass("hide");
               $('#servicePanel-contents').html("Service instances unavailable.").addClass('service-unready-message');                       
            }
            
            function drawGraph(outputApi, model) {
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
                            
                    outputApi.resetZoom();
                    render.doRender(outputApi, model);

                    evt.preventDefault();
                });               

                $("#modelButton").click(function (evt) {
                    window.open('/VersaStack-web/modelView.jsp', 'newwindow', config = 'height=1200,width=400, toolbar=no, menubar=no, scrollbars=no, resizable=no,location=no, directories=no, status=no');
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
                       
                       if (( (current === "tagDialog") && (tdz < tpz) ) ||
                           ( (current === "tagPanel") &&  (tpz < tdz) ) ) {
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

                var displayTree = new DropDownTree(document.getElementById("treeMenu"));
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
                this.setZoom = function(zoom) {
                    zoomFactor = zoom;
                    this._updateTransform();
                };
                var svg = document.getElementById(this.svgContainerName);
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
            
        <sql:setDataSource var="front_conn" driver="com.mysql.jdbc.Driver"
                             url="jdbc:mysql://localhost:3306/frontend"
                             user="front_view"  password="frontuser"/>

        <sql:query dataSource="${front_conn}" sql="SELECT S.name, I.referenceUUID, I.alias_name FROM service S, service_instance I, user_info U     
                                                  WHERE U.user_id = I.user_id AND S.service_id = I.service_id AND U.username = ?" var="serviceList">
                  <sql:param value="${user.getUsername()}" />
        </sql:query>            

        <div class="closed" id="servicePanel">
            <div id="servicePanel-tab">
                Services
            </div>
            <div id ="servicePanel-contents">
                <table id="service-instance-table">
                    <thead>
                        <tr>
                            <th>Service</th>
                            <th>Alias Name</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach items="${serviceList.rows}" var="service">
                            <tr class="service-instance-item" id="${service.referenceUUID}">
                                <td>${service.name}</td>
                                <td>${service.alias_name}</td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </div>
            
        </div>
        <script>                 
            $(".service-instance-item").each(function() {
                var that = this;
                var DELAY = 700, clicks = 0, timer = null;

                $( that ).click( function() {
                    clicks++;  //count clicks

                    if(clicks === 1) {                          
                        timer = setTimeout(function() {
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
                            timer = setTimeout(function() {
                                clickServiceInstanceItem(that);
                                clicks = 0;   
                            }, DELAY);                            
                        }
                    }
                }).dblclick(function(e) {
                    e.preventDefault();
                });
            });
              
              
            function clickServiceInstanceItem(item) {
                var UUID = $( item ).attr('id');

                $.ajax({
                    crossDomain: true,
                    type: "GET",
                    url: "/VersaStack-web/restapi/app/service/lastverify/" + UUID,
                    dataType: "json", 

                    success: function(data,  textStatus,  jqXHR ) {
                         if (data.verified_addition === null) {
                             bsShowFadingMessage("#servicePanel", "Data not found", "top", 1000);
                         } else {
                            $(".service-instance-item.service-instance-highlighted").removeClass('service-instance-highlighted');
                            $(item).addClass('service-instance-highlighted');

                            var uaObj = JSON.parse(data.verified_addition);
                            var result = model.makeSubModel([ uaObj  ]);
                            var modelArr = model.getModelMapValues(result);

                            render.API.setServiceHighlights(modelArr);
                            render.API.highlightServiceElements();

                         }
                    },

                    error: function(jqXHR, textStatus, errorThrown ) {
                        //alert("Error getting status.");
                        alert("textStatus: " + textStatus + " errorThrown: " + errorThrown);
                    }
               });                     
            }
        </script>
        <div id="loadingPanel"></div>
        <div class="closed" id="displayPanel">
            <div id="displayPanel-contents">
                <button id="modelButton">Display Model</button>
                <button id="fullDiaplayButton">Toggle Full Model</button>
                <button id="testButton">test</button> <!-- @ -->
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
        <div class="hide" id="hoverdiv_viz"></div>        

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

<!-- TAG PANEL -->
<div id="tag-panel"> 
</div>    
</body>

</html>
