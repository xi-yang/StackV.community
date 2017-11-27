/*
 * Copyright (c) 2013-2016 University of Maryland
 * Created by: Alberto Jimenez
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and/or hardware specification (the “Work”) to deal in the
 * Work without restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Work, and to permit persons to whom the Work is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Work.
 *
 * THE WORK IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE WORK OR THE USE OR OTHER DEALINGS
 * IN THE WORK.
 */
/* global XDomainRequest, baseUrl, keycloak, loggedIn, TweenLite, Power2, Mousetrap, swal */
// Tweens
var tweenDetailsPanel = new TweenLite("#details-panel", 1, {ease: Power2.easeInOut,
    paused: true, top: "0px", opacity: "1", display: "block"});
var tweenLoggingPanel = new TweenLite("#logging-panel", 1, {ease: Power2.easeInOut,
    paused: true, left: "0px", opacity: "1", display: "block"});
var tweenVisualPanel = new TweenLite("#visual-panel", 1, {ease: Power2.easeInOut,
    paused: true, right: "0px", opacity: "1", display: "block"});

var view = "center";

Mousetrap.bind({
    'shift+left': function () {
        window.location.href = "/StackV-web/ops/catalog.jsp";
    },
    'shift+right': function () {
        window.location.href = "/StackV-web/ops/srvc/driver.jsp";
    },
    'left': function () {
        viewShift("left");
    },
    'right': function () {
        viewShift("right");
    }
});
function viewShift(dir) {
    switch (view) {
        case "left":
            if (dir === "right") {
                newView("details");
            }
            break;
        case "center":
            switch (dir) {
                case "left":
                    newView("logging");
                    break;
                case "right":
                    newView("visual");
                    break;
            }
            view = dir;
            break;
        case "right":
            if (dir === "left") {
                newView("details");
            }
            break;
    }
}
function newView(panel) {
    resetView();
    switch (panel) {
        case "logging":
            tweenLoggingPanel.play();
            $("#logging-tab").addClass("active");
            view = "left";
            break;
        case "details":
            tweenDetailsPanel.play();
            $("#sub-details-tab").addClass("active");
            view = "center";
            break;
        case "visual":
            tweenVisualPanel.play();
            $("#visual-tab").addClass("active");
            view = "right";
            break;
    }
}
function resetView() {
    switch (view) {
        case "left":
            $("#sub-nav .active").removeClass("active");
            tweenLoggingPanel.reverse();
            break;
        case "center":
            $("#sub-nav .active").removeClass("active");
            tweenDetailsPanel.reverse();
            break;
        case "right":
            closeVisTabs();
            $("#sub-nav .active").removeClass("active");
            tweenVisualPanel.reverse();
            break;
    }
}

function loadDetailsNavbar() {
    $("#sub-nav").load("/StackV-web/nav/details_navbar.html", function () {
        setRefresh($("#refresh-timer").val());
        switch (view) {
            case "left":
                $("#logging-tab").addClass("active");
                break;
            case "center":
                $("#sub-details-tab").addClass("active");
                break;
            case "right":
                $("#visual-tab").addClass("active");
                break;
        }

        $("#logging-tab").click(function () {
            resetView();
            newView("logging");
        });
        $("#sub-details-tab").click(function () {
            resetView();
            newView("details");
        });
        $("#visual-tab").click(function () {
            resetView();
            newView("visual");
        });
    });
}


/* DETAILS */

function loadDetails() {
    var uuid = sessionStorage.getItem("instance-uuid");
    startDetailsEngine(uuid);

    var apiUrl = baseUrl + '/StackV-web/restapi/app/logging/logs?refUUID=' + uuid;
    loadDataTable(apiUrl);
    reloadLogs();

    tweenDetailsPanel.play();
}

function subloadStatus(refUuid) {
    var ele = $("#instance-substate");
    var last = $("#instance-laststate");
    var apiUrl = baseUrl + '/StackV-web/restapi/app/service/' + refUuid + '/substatus';
    $.ajax({
        url: apiUrl,
        async: false,
        type: 'GET',
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
            xhr.setRequestHeader("Refresh", keycloak.refreshToken);
        },
        success: function (result) {
            if (result === "FAILED") {
                last.html(" (After " + lastState + ")");
            }

            subState = result;
            ele.html(result);

            if (view === "center") {
                tweenDetailsPanel.play();
            }
            loadVisualization();
            subloadVerification();
        }
    });
}

hasDrone = true;
function subloadVerification() {
    var uuid = sessionStorage.getItem("instance-uuid");
    var apiUrl = baseUrl + '/StackV-web/restapi/app/details/' + uuid + '/verification';
    $.ajax({
        url: apiUrl,
        async: false,
        type: 'GET',
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
        },
        success: function (verification) {
            /*  verification mapping:
             *      1 - state
             *      1 - verification_run
             *      2 - creation_time
             *      3 - addition
             *      4 - reduction
             *      5 - service_instance_id  */

            verifyState = verification[0];
            verificationRun = verification[1];
            verificationTime = verification[2];
            verificationAddition = verification[3];
            verificationReduction = verification[4];

            $.ajax({
                url: apiUrl += '/drone',
                async: false,
                type: 'GET',
                beforeSend: function (xhr) {
                    xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
                },
                success: function (retCode) {
                    hasDrone = (retCode === "1");
                }
            });

            instructionModerate();
            buttonModerate();
        }
    });
}

function buildDeltaTable(type) {
    var panel = document.getElementById("details-panel");

    var table = document.createElement("table");
    table.className = "management-table details-table " + type.toLowerCase() + "-delta-table";
    table.id = type.toLowerCase() + "-delta-table";

    var thead = document.createElement("thead");
    thead.className = "delta-table-header";
    var row = document.createElement("tr");
    var head = document.createElement("th");
    head.innerHTML = type + " Delta";
    row.appendChild(head);

    head = document.createElement("th");
    head.innerHTML = "Verified";
    row.appendChild(head);

    head = document.createElement("th");
    head.innerHTML = "Unverified";
    row.appendChild(head);

    row.appendChild(head);

    thead.appendChild(row);
    table.appendChild(thead);

    var tbody = document.createElement("tbody");
    tbody.className = "delta-table-body";
    //tbody.id = "acl-body";

    row = document.createElement("tr");
    var prefix = type.substring(0, 4).toLowerCase();
    var add = document.createElement("td");
    row.appendChild(add);

    add = document.createElement("td");
    add.id = prefix + "-add";
    row.appendChild(add);

    var red = document.createElement("td");
    red.id = prefix + "-red";
    row.appendChild(red);

    tbody.appendChild(row);
    row = document.createElement("tr");
    var cell = document.createElement("td");
    cell.colSpan = "3";
    cell.innerHTML = '<button  class="details-model-toggle btn btn-default" onclick="toggleTextModel(\'.'
            + type.toLowerCase() + '-delta-table\', \'#delta-' + type + '\');">Toggle Text Model</button>';
    row.appendChild(cell);
    tbody.appendChild(row);

    table.appendChild(tbody);

    panel.appendChild(table);

}

function loadVisualization() {
    if (!(subState === "INIT" || (subState === "FAILED" && lastState === "INIT"))) {
        $("#details-viz").load("/StackV-web/details_viz.html", function () {
            document.getElementById("visual-panel").innerHTML = "";

            var States = {
                "INIT": 0,
                "COMPILED": 1,
                "COMMITTING": 2,
                "COMMITTING-PARTIAL": 2,
                "COMMITTED": 3,
                "FAILED": 4,
                "READY": 5
            };

            var tabs = [
                {
                    "name": "Service",
                    "state": "COMPILED",
                    "createContent": createVizTab.bind(undefined, "Service")
                },
                {
                    "name": "System",
                    "state": "COMMITTING",
                    "createContent": createVizTab.bind(undefined, "System")
                },
                {
                    "name": "Verification",
                    "state": "COMMITTED",
                    "createContent": createVizTab.bind(undefined, "Verification")
                }
            ];

            createTabs();
            function createVizTab(viz_type) {
                var div = document.createElement("div");
                div.classList.add("viz");
                div.id = "sd_" + viz_type;
                div.appendChild(buildViz(viz_type + " Addition"));
                div.appendChild(buildViz(viz_type + " Reduction"));

                div.classList.add("tab-pane");
                div.classList.add("fade");
                div.classList.add("in");
                div.classList.add("viz-tab-content");
                return div;
            }

            function buildHeaderLink(id, text) {
                var link = document.createElement("a");
                ;
                link.href = "#";
                link.classList.add("viz-hdr");
                link.classList.add("unexpanded");
                link.id = id;
                link.text = text;
                return link;
            }

            function buildViz(viz_type) {
                var table = document.createElement("table");
                table.classList.add("management-table");
                table.classList.add("viz-table");
                var headerRow = document.createElement("tr");
                var vizRow = document.createElement("tr");
                var additionHeader = document.createElement("th");
                var reductionHeader = document.createElement("th");
                var additionCell = document.createElement("td");
                additionCell.classList.add("viz-cell");
                var reductionCell = document.createElement("td");
                reductionCell.classList.add("viz-cell");

                switch (viz_type) {

                    case "System Addition":
                        table.id = "sd_System_Addition";

                        var a = buildHeaderLink("sd_System_Addition_Link", "Addition");
                        additionHeader.appendChild(a);
                        additionCell.classList.add("viz-cell");
                        additionCell.id = "sd_System_Addition_Viz";

                        vizRow.appendChild(additionCell);

                        if (!$("#sysa_viz_div").hasClass("emptyViz")) {
                            var sysa_viz_div = document.getElementById("sysa_viz_div");
                            additionCell.appendChild(sysa_viz_div);
                            sysa_viz_div.classList.remove("hidden");
                        }
                        break;
                    case "System Reduction":
                        table.id = "sd_System_Reduction";

                        var a = buildHeaderLink("sd_System_Reduction_Link", "Reduction");
                        reductionHeader.appendChild(a);
                        reductionCell.classList.add("viz-cell");
                        reductionCell.id = "sd_System_Reduction_Viz";

                        vizRow.appendChild(reductionCell);

                        if (!$("#sysr_viz_div").hasClass("emptyViz")) {
                            //  $(".system-delta-table").removeClass("hide");
                            var sysr_viz_div = document.getElementById("sysr_viz_div");
                            reductionCell.appendChild(sysr_viz_div);
                            sysr_viz_div.classList.remove("hidden");
                        }

                        break;
                    case "Service Addition":
                        table.id = "sd_Service_Addition";

                        var a = buildHeaderLink("sd_Service_Addition_Link", "Addition");
                        additionHeader.appendChild(a);
                        additionCell.classList.add("viz-cell");
                        additionCell.id = "sd_Service_Addition_Viz";

                        vizRow.appendChild(additionCell);

                        if (!$("#serva_viz_div").hasClass("emptyViz")) {
                            // $(".service-delta-table").removeClass("hide");
                            var serva_viz_div = document.getElementById("serva_viz_div");
                            additionCell.appendChild(serva_viz_div);
                            serva_viz_div.classList.remove("hidden");
                        }

                        break;
                    case "Service Reduction":
                        table.id = "sd_Service_Reduction";

                        var a = buildHeaderLink("sd_Service_Reduction_Link", "Reduction");
                        reductionHeader.appendChild(a);
                        reductionCell.classList.add("viz-cell");
                        reductionCell.id = "sd_Service_Addition_Viz";

                        vizRow.appendChild(reductionCell);

                        if (!$("#servr_viz_div").hasClass("emptyViz")) {
                            var servr_viz_div = document.getElementById("servr_viz_div");
                            reductionCell.appendChild(servr_viz_div);
                            servr_viz_div.classList.remove("hidden");
                        }
                        break;
                    case "Verification Addition":
                        var a = buildHeaderLink("sd_Unverified_Addition_Link", "Unverified Addition");
                        additionHeader.appendChild(a);
                        additionCell.classList.add("viz-cell");
                        additionCell.id = "sd_Unverified_Addition_Viz";

                        vizRow.appendChild(additionCell);

                        a = buildHeaderLink("sd_Verified_Addition_Link", "Verified Addition");
                        reductionHeader.appendChild(a);
                        reductionCell.classList.add("viz-cell");
                        reductionCell.id = "sd_Verified_Addition_Viz";

                        vizRow.appendChild(reductionCell);


                        if (!$("#va_viz_div").hasClass("emptyViz") || !$("#ua_viz_div").hasClass("emptyViz")) {
                            var va_viz_div = document.getElementById("va_viz_div");
                            var ua_viz_div = document.getElementById("ua_viz_div");

                            additionCell.appendChild(ua_viz_div);
                            reductionCell.appendChild(va_viz_div);

                            ua_viz_div.classList.remove("hidden");
                            va_viz_div.classList.remove("hidden");

                        }

                        break;
                    case "Verification Reduction":

                        var a = buildHeaderLink("sd_Unverified_Reduction_Link", "Unverified Reduction");
                        additionHeader.appendChild(a);
                        additionCell.classList.add("viz-cell");
                        additionCell.id = "sd_Unverified_Reduction_Viz";

                        vizRow.appendChild(additionCell);

                        a = buildHeaderLink("sd_Verified_Reduction_Link", "Verified Reduction");
                        reductionHeader.appendChild(a);
                        reductionCell.classList.add("viz-cell");
                        reductionCell.id = "sd_Verified_Reduction_Viz";

                        vizRow.appendChild(reductionCell);

                        if (!$("#vr_viz_div").hasClass("emptyViz") || !$("#ur_viz_div").hasClass("emptyViz")) {
                            var ur_viz_div = document.getElementById("ur_viz_div");
                            var vr_viz_div = document.getElementById("vr_viz_div");

                            additionCell.appendChild(ur_viz_div);
                            reductionCell.appendChild(vr_viz_div);

                            ur_viz_div.classList.remove("hidden");
                            vr_viz_div.classList.remove("hidden");
                        }
                        break;
                }
                if (viz_type.includes("Verification")) {
                    headerRow.appendChild(additionHeader);
                    headerRow.appendChild(reductionHeader);
                } else if (viz_type.includes("Addition")) {
                    headerRow.appendChild(additionHeader);
                } else {
                    headerRow.appendChild(reductionHeader);
                }
                table.appendChild(headerRow);
                table.appendChild(vizRow);

                return table;
            }



            function createTabs() {
                $(".verification-table").addClass("hide");
                $(".system-delta-table").addClass("hide");
                $(".service-delta-table").addClass("hide");
                $("#delta-Service").addClass("hide");
                $("#delta-System").addClass("hide");


                var tabBar = document.createElement("ul");
                tabBar.classList.add("nav");
                tabBar.classList.add("nav-tabs");

                var tabContent = document.createElement("div");
                tabContent.classList.add("tab-content");
                tabContent.classList.add("viz-tab-content");

                for (var i = 0; i < tabs.length; i++) {
                    var tab = tabs[i];

                    if ((tab.name === "Verification") && (verificationState === null))
                        continue;

                    if (States[tab.state] <= States[subState]) {
                        createTab(tab, tabBar);
                        tabContent.appendChild(tab.createContent());
                    }
                }
                if (tabBar.lastChild) {
                    tabBar.lastChild.classList.add("active");
                    tabContent.lastChild.classList.add("active");

                    var visualization_panel = document.getElementById("visual-panel");
                    visualization_panel.appendChild(tabBar);
                    visualization_panel.appendChild(tabContent);

                    setEvent();
                }
            }

            function make_tab_id(tab) {
                var id = tab.name.replace(/\s+/g, '');
                return "sd_" + id;
            }
            function createTab(tab, tabBar) {
                var li = document.createElement("li");
                var a = document.createElement("a");
                a.href = "#" + make_tab_id(tab);
                a.text = tab.name;
                a.setAttribute("data-toggle", "tab");
                li.appendChild(a);
                tabBar.appendChild(li);
            }

            function setEvent(container) {
                //$(".viz-hdr")
                //$(".details-viz-button").click();

                $(".viz-hdr").on("click", function () {
                    var tab = $(this).closest(".tab-pane");

                    var hdr = $(this).closest("th");
                    var cell = hdr.closest('table').find('td').eq(hdr.index());
                    var table = $(this).closest("table");
                    var viz = cell.children().eq(0);
                    var text_model = viz.children(".details-viz-text-model");
                    var text_model_pre = text_model.children("pre").eq(0);
                    ;

                    var text_model_pre_width = text_model_pre.width();
                    var text_model_pre_height = text_model_pre.height();

                    if (viz.hasClass("emptyViz"))
                        return;

                    var button = viz.children(".details-viz-recenter-button");

                    if ($(this).hasClass("unexpanded")) {
                        if (!$("#instance-details-table").hasClass("hide"))
                            $("#instance-details-table").addClass("hide");

                        tab.find(".viz-cell").not(cell).addClass("hide");
                        tab.find(".viz-hdr").closest("th").not(hdr).addClass("hide");
                        tab.find(".viz-table").not(table).addClass("hide");

                        viz.addClass("expanded-viz-div");
                        table.height("95%");
                        $(this).removeClass("unexpanded");
                        $(this).addClass("expanded");
                        button.trigger("click", [viz.width(), viz.height()]);

                        text_model_pre.width("inherit");
                        text_model_pre.addClass("expanded");
                        text_model_pre.height(viz.height() * 2);

                        pauseRefresh();
                    } else {
                        if ($("#instance-details-table").hasClass("hide") && !$(".viz-hdr.expanded").not(this).length) {
                            $("#instance-details-table").removeClass("hide");
                        }

                        tab.find(".viz-cell").not(cell).removeClass("hide");
                        tab.find(".viz-hdr").closest("th").not(hdr).removeClass("hide");
                        tab.find(".viz-table").not(".emptyVizTable").removeClass("hide");

                        table.height("10%");
                        viz.removeClass("expanded-viz-div");
                        $(this).removeClass("expanded");
                        $(this).addClass("unexpanded");
                        button.trigger("click", [viz.width(), viz.height()]);

                        text_model_pre.removeClass("expanded");
                        text_model_pre.width("initial");
                        text_model_pre.height(text_model_pre_height / 2.5);

                        resumeRefresh();
                    }

                });
            }

            if (view === "right") {
                tweenVisualPanel.play();
            }
        });
    }
}
function closeVisTabs() {
    $(".viz-hdr.expanded").click();
}

function toggleTextModel(viz_table, text_table) {
    if (!$(viz_table.toLowerCase()).length) {
        alert("Visualization not found");
    } else if (!$(text_table).length) {
        alert("Text model not found");
    } else {
        $(viz_table.toLowerCase()).toggleClass("hide");
        // delta-Service, service verification etc must always display before
        // everything else.
        if (text_table.toLowerCase().indexOf("service") > 0) {
            $(text_table).insertAfter("#instance-details-table");
        }
        $(text_table).toggleClass("hide");

    }
}

/* REFRESH */
function reloadData() {
    keycloak.updateToken(90).error(function () {
        console.log("Error updating token!");
    }).success(function (refreshed) {
        var timerSetting = $("#refresh-timer").val();
        if (timerSetting > 15) {
            switch (view) {
                case "left":
                    /*tweenLoggingPanel.reverse();*/
                    break;
                case "center":
                    tweenDetailsPanel.reverse();
                    break;
                case "right":
                    tweenVisualPanel.reverse();
                    break;
            }
            setTimeout(function () {
                reloadLogs();
                updateData();
                renderDetails();
                $(".delta-table-header").click(function () {
                    $("#body-" + this.id).toggleClass("hide");
                });
                refreshSync(refreshed, timerSetting);
            }, 250);
        } else {
            reloadLogs();
            updateData();
            renderDetails();
            $(".delta-table-header").click(function () {
                $("#body-" + this.id).toggleClass("hide");
            });
            refreshSync(refreshed, timerSetting);
        }
    });
}  