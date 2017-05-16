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
/* global XDomainRequest, baseUrl, keycloak, loggedIn, TweenLite, Power2, Mousetrap */
// Tweens
var tweenDetailsPanel = new TweenLite("#details-panel", 1, {ease: Power2.easeInOut, paused: true, top: "0px"});
var tweenLoggingPanel = new TweenLite("#logging-panel", 1, {ease: Power2.easeInOut, paused: true, left: "0px"});
var tweenVisualPanel = new TweenLite("#visual-panel", 1, {ease: Power2.easeInOut, paused: true, right: "0px"});

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
            $("#sub-nav .active").removeClass("active");
            tweenVisualPanel.reverse();
            break;
    }
}

$(function () {
    $(".checkbox-level").change(function () {
        if ($(this).is(":checked")) {
            $("#log-div").removeClass("hide-" + this.name);
        } else {
            $("#log-div").addClass("hide-" + this.name);
        }
    });
    $("#filter-search-clear").click(function () {
        $("#filter-search-input").val("");
        loadLogs();
    });
});

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
    // Subfunctions    
    subloadInstance();
    subloadLogging();
}

/* LOGGING */
function subloadLogging() {
    var uuid = sessionStorage.getItem("instance-uuid");
    var apiUrl = baseUrl + '/StackV-web/restapi/app/logging/logs?refUUID=' + uuid;
    loadDataTable(apiUrl);
    setTimeout(function () {
        if (view === "left") {
            tweenLoggingPanel.play();
            $('div.dataTables_filter input').focus();
        }
    }, 1000);
    reloadLogs();
}

function subloadInstance() {
    var uuid = sessionStorage.getItem("instance-uuid");

    var apiUrl = baseUrl + '/StackV-web/restapi/app/details/' + uuid + '/instance';
    $.ajax({
        url: apiUrl,
        type: 'GET',
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
        },
        success: function (instance) {
            /*  instance mapping:
             *      0 - verification_state
             *      1 - name
             *      2 - alias_name
             *      3 - creation_time
             *      4 - super_state     */

            var panel = document.getElementById("details-panel");
            panel.innerHTML = "";

            $("#details-panel").append("<div id='instance-verification' class='hide'>" + instance[0] + "</div>");
            var table = document.createElement("table");

            table.id = "instance-details-table";
            table.className = "management-table";

            var thead = document.createElement("thead");
            var row = document.createElement("tr");
            var head = document.createElement("th");
            head.innerHTML = instance[1] + " Service Details";
            row.appendChild(head);
            head = document.createElement("th");
            head.innerHTML = '';
            row.appendChild(head);
            thead.appendChild(row);
            table.appendChild(thead);

            var tbody = document.createElement("tbody");
            var row = document.createElement("tr");
            var cell = document.createElement("td");
            cell.innerHTML = "Instance Alias";
            row.appendChild(cell);
            cell = document.createElement("td");
            cell.innerHTML = instance[2];
            row.appendChild(cell);
            tbody.appendChild(row);

            row = document.createElement("tr");
            cell = document.createElement("td");
            cell.innerHTML = "Reference UUID";
            row.appendChild(cell);
            cell = document.createElement("td");
            cell.innerHTML = uuid;
            row.appendChild(cell);
            tbody.appendChild(row);

            row = document.createElement("tr");
            cell = document.createElement("td");
            cell.innerHTML = "Creation Time";
            row.appendChild(cell);
            cell = document.createElement("td");
            cell.id = "instance-creation-time";
            cell.innerHTML = instance[3];
            row.appendChild(cell);
            tbody.appendChild(row);

            row = document.createElement("tr");
            cell = document.createElement("td");
            cell.innerHTML = "Instance State";
            row.appendChild(cell);
            cell = document.createElement("td");
            cell.id = "instance-superstate";
            cell.innerHTML = instance[4];
            row.appendChild(cell);
            tbody.appendChild(row);

            row = document.createElement("tr");
            cell = document.createElement("td");
            cell.innerHTML = "Operation Status";
            row.appendChild(cell);
            cell = document.createElement("td");
            cell.id = "instance-substate";
            row.appendChild(cell);
            tbody.appendChild(row);

            row = document.createElement("tr");
            row.className = "instruction-row";
            cell = document.createElement("td");
            cell.innerHTML = '<div id="instruction-block"></div>';
            cell.colSpan = "2";
            row.appendChild(cell);
            tbody.appendChild(row);

            row = document.createElement("tr");
            row.className = "button-row";
            cell = document.createElement("td");
            cell.innerHTML = '<div class="service-instance-panel">'
                    + '<button class="btn btn-default hide instance-command" id="reinstate">Reinstate</button>'
                    + '<button class="btn btn-default hide instance-command" id="force_reinstate">Force Reinstate</button>'
                    + '<button class="btn btn-default hide instance-command" id="cancel">Cancel</button>'
                    + '<button class="btn btn-default hide instance-command" id="force_cancel">Force Cancel</button>'
                    + '<button class="btn btn-default hide instance-command" id="force_retry">Force Retry</button>'
                    + '<button class="btn btn-default hide instance-command" id="modify">Modify</button>'
                    + '<button class="btn btn-default hide instance-command" id="force_modify">Force Modify</button>'
                    + '<button class="btn btn-default hide instance-command" id="reverify">Re-Verify</button>'
                    + '<button class="btn btn-default hide instance-command" id="delete">Delete</button>'
                    + '<button class="btn btn-default hide instance-command" id="force_delete">Force Delete</button>'
                    + '</div>';
            cell.colSpan = "2";
            row.appendChild(cell);
            tbody.appendChild(row);

            table.appendChild(tbody);
            panel.insertBefore(table, panel.firstChild);

            $(".delta-table-header").click(function () {
                $("#body-" + this.id).toggleClass("hide");
            });

            $(".instance-command").click(function () {
                var command = this.id;
                var apiUrl = baseUrl + '/StackV-web/restapi/app/service/' + uuid + '/' + command;
                $.ajax({
                    url: apiUrl,
                    type: 'PUT',
                    beforeSend: function (xhr) {
                        xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
                        xhr.setRequestHeader("Refresh", keycloak.refreshToken);
                    },
                    success: function () {
                        if (command === "delete" || command === "force_delete") {
                            setTimeout(function () {
                                sessionStorage.removeItem("instance-uuid");
                                window.document.location = "/StackV-web/ops/catalog.jsp";
                            }, 250);
                        } else {
                            reloadData();
                        }
                    }
                });
            });

            // Next steps
            loadStatus(uuid);
        }
    });
}

function subloadVerification() {
    var uuid = sessionStorage.getItem("instance-uuid");
    var apiUrl = baseUrl + '/StackV-web/restapi/app/details/' + uuid + '/verification';
    $.ajax({
        url: apiUrl,
        type: 'GET',
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
        },
        success: function (verification) {
            /*  verification mapping:
             *      0 - verification_run
             *      1 - creation_time
             *      2 - addition
             *      3 - reduction
             *      4 - service_instance_id */
            var panel = document.getElementById("details-panel");

            var div = document.createElement("div");
            div.className = "hide";
            div.id = "verification-run";
            div.innerHTML = verification[0];
            panel.appendChild(div);
            div = document.createElement("div");
            div.className = "hide";
            div.id = "verification-time";
            div.innerHTML = verification[1];
            panel.appendChild(div);
            div = document.createElement("div");
            div.className = "hide";
            div.id = "verification-addition";
            div.innerHTML = verification[2];
            panel.appendChild(div);
            div = document.createElement("div");
            div.className = "hide";
            div.id = "verification-reduction";
            div.innerHTML = verification[3];
            panel.appendChild(div);

            var table = document.createElement("table");
            table.className = "management-table hide verification-table";


            var thead = document.createElement("thead");
            thead.className = "delta-table-header";
            thead.id = "delta-" + verification[4];
            var row = document.createElement("tr");
            var head = document.createElement("th");
            row.appendChild(head);
            head = document.createElement("th");
            head.innerHTML = "Verified";
            row.appendChild(head);
            head = document.createElement("th");
            head.innerHTML = "Unverified";
            row.appendChild(head);

            thead.appendChild(row);
            table.appendChild(thead);

            var tbody = document.createElement("tbody");
            tbody.className = "delta-table-body";
            tbody.id = "body-delta-" + verification[4];

            row = document.createElement("tr");
            row.id = "verification-addition-row";
            var cell = document.createElement("td");
            cell.innerHTML = "Addition";
            row.appendChild(cell);
            cell = document.createElement("td");
            cell.id = "ver-add";
            row.appendChild(cell);
            cell = document.createElement("td");
            cell.id = "unver-add";
            row.appendChild(cell);
            tbody.appendChild(row);

            row = document.createElement("tr");
            row.id = "verification-reduction-row";
            cell = document.createElement("td");
            cell.innerHTML = "Reduction";
            row.appendChild(cell);
            cell = document.createElement("td");
            cell.id = "ver-red";
            row.appendChild(cell);
            cell = document.createElement("td");
            cell.id = "unver-red";
            row.appendChild(cell);
            tbody.appendChild(row);

            row = document.createElement("tr");
            cell = document.createElement("td");
            cell.colSpan = "3";
            cell.innerHTML = '<button class="details-model-toggle btn btn-default" onclick="toggleTextModel(\'.verification-table', '#delta-System\');">Toggle Text Model</button>';
            row.appendChild(cell);
            tbody.appendChild(row);

            table.appendChild(tbody);
            panel.appendChild(table);

            $(".delta-table-header").click(function () {
                $("#body-" + this.id).toggleClass("hide");
            });

            // Next step
            subloadDelta();
        }
    });
}

function subloadDelta() {
    var uuid = sessionStorage.getItem("instance-uuid");
    var apiUrl = baseUrl + '/StackV-web/restapi/app/details/' + uuid + '/delta';
    $.ajax({
        url: apiUrl,
        type: 'GET',
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
        },
        success: function (result) {
            for (i = 0; i < result.length; i++) {
                /*  delta mapping:
                 *      0 - type
                 *      1 - service_delta_id
                 *      2 - super_state
                 *      3 - delta   */
                var delta = result[i];

                var table = document.createElement("table");
                table.className = "management-table delta-table";
                table.id = "delta-" + delta[0];

                var thead = document.createElement("thead");
                thead.className = "delta-table-header";
                thead.id = "delta-" + delta[1];
                var row = document.createElement("tr");
                var head = document.createElement("th");
                head.innerHTML = "Delta Details";
                row.appendChild(head);
                head = document.createElement("th");
                row.appendChild(head);
                thead.appendChild(row);
                table.appendChild(thead);

                var tbody = document.createElement("tbody");
                tbody.className = "delta-table-body";
                tbody.id = "body-delta-" + delta[1];

                row = document.createElement("tr");
                var cell = document.createElement("td");
                cell.innerHTML = "Delta State";
                row.appendChild(cell);
                cell = document.createElement("td");
                cell.innerHTML = delta[2];
                row.appendChild(cell);
                tbody.appendChild(row);

                row = document.createElement("tr");
                cell = document.createElement("td");
                cell.innerHTML = "Delta Type";
                row.appendChild(cell);
                cell = document.createElement("td");
                cell.innerHTML = delta[0];
                row.appendChild(cell);
                tbody.appendChild(row);

                row = document.createElement("tr");
                cell = document.createElement("td");
                row.appendChild(cell);
                cell = document.createElement("td");
                cell.id = '';
                cell.innerHTML = delta[3];
                row.appendChild(cell);
                tbody.appendChild(row);

                row = document.createElement("tr");
                cell = document.createElement("td");
                cell.colSpan = "2";
                cell.innerHTML = '<button  class="details-model-toggle btn btn-default" onclick="toggleTextModel(\'.'
                        + delta[0] + '-delta-table\', \'#delta-' + delta[0] + '\');">Toggle Text Model</button>';
                row.appendChild(cell);
                tbody.appendChild(row);

                table.appendChild(tbody);
                document.getElementById("details-panel").appendChild(table);

                $(".delta-table-header").click(function () {
                    $("#body-" + this.id).toggleClass("hide");
                });
            }

            // Next step
            subloadACL();
            // loadVisualization();
        }
    });
}

function subloadACL() {
    var uuid = sessionStorage.getItem("instance-uuid");
    var apiUrl = baseUrl + '/StackV-web/restapi/app/details/' + uuid + '/acl';
    $.ajax({
        url: apiUrl,
        type: 'GET',
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
        },
        success: function (verification) {
            /*  acl mapping:
             */
            var panel = document.getElementById("details-panel");

            var table = document.createElement("table");
            table.className = "management-table hide acl-table";

            var thead = document.createElement("thead");
            thead.className = "delta-table-header";
            var row = document.createElement("tr");
            var head = document.createElement("th");
            row.appendChild(head);
            head = document.createElement("th");
            head.innerHTML = "Access Control";
            row.appendChild(head);

            thead.appendChild(row);
            table.appendChild(thead);

            var tbody = document.createElement("tbody");
            tbody.className = "delta-table-body";
            tbody.id = "acl-body";

            row = document.createElement("tr");
            var cell = document.createElement("td");
            cell.innerHTML = '<select id="acl-select" size="5" name="acl-select" multiple></select>';
            row.appendChild(cell);
            tbody.appendChild(row);

            row = document.createElement("tr");
            cell = document.createElement("td");
            cell.innerHTML = '<label>Give user access: <input type="text" name="acl-input" /></label>';
            row.appendChild(cell);
            tbody.appendChild(row);

            table.appendChild(tbody);
            panel.appendChild(table);

            $(".delta-table-header").click(function () {
                $("#body-" + this.id).toggleClass("hide");
            });

            // Next step            
            loadACL(uuid);
        }
    });
}

function loadACL() {
    var select = document.getElementById("acl-select");
    $("#acl-select").empty();

    var apiUrl = baseUrl + '/StackV-web/restapi/app/panel/' + keycloak.subject + '/acl';
    $.ajax({
        url: apiUrl,
        type: 'GET',
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
        },
        success: function (result) {
            for (i = 0; i < result.length; i++) {
                select.append("<option>" + result[i] + "</option>");
            }
        }
    });
}

function loadStatus(refUuid) {
    var ele = document.getElementById("instance-substate");
    var apiUrl = baseUrl + '/StackV-web/restapi/app/service/' + refUuid + '/substatus';
    $.ajax({
        url: apiUrl,
        type: 'GET',
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
            xhr.setRequestHeader("Refresh", keycloak.refreshToken);
        },
        success: function (result) {
            ele.innerHTML = result;

            deltaModerate();
            instructionModerate();
            buttonModerate();

            if (view === "center") {
                tweenDetailsPanel.play();
            }
            loadVisualization();
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
    cell.innerHTML = '<button  class="details-model-toggle btn btn-default" onclick="toggleTextModel(\'.' + type.toLowerCase() + '-delta-table\', \'#delta-' + type + '\');">Toggle Text Model</button>';
    row.appendChild(cell);
    tbody.appendChild(row);

    table.appendChild(tbody);

    panel.appendChild(table);

}

function loadVisualization() {
    $("#details-viz").load("/StackV-web/details_viz.html", function () {
        document.getElementById("visual-panel").innerHTML = "";
        var State = document.getElementById("instance-substate").innerHTML;
        var verificationState = document.getElementById("instance-verification").innerHTML;

        var States = {
            "INIT": 0,
            "COMPILED": 1,
            "COMMITTED": 2,
            "FAILED": 3,
            "READY": 4
        };

        var tabs = [
            {
                "name": "Service",
                "state": "INIT",
                "createContent": createVizTab.bind(undefined, "Service")
            },
            {
                "name": "System",
                "state": "COMPILED",
                "createContent": createVizTab.bind(undefined, "System")
            },
            {
                "name": "Verification",
                "state": "FAILED",
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

                if ((tab.name === "Verification") && (verificationState === null || verificationState === "-1"))
                    continue;

                if (States[tab.state] <= States[State]) {
                    createTab(tab, tabBar);
                    tabContent.appendChild(tab.createContent());
                }
            }
            tabBar.lastChild.classList.add("active");
            tabContent.lastChild.classList.add("active");

            var visualization_panel = document.getElementById("visual-panel");
            visualization_panel.appendChild(tabBar);
            visualization_panel.appendChild(tabContent);

            setEvent();
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
                } else {
                    if ($("#instance-details-table").hasClass("hide") && !$(".viz-hdr.expanded").not(this).length) {
                        $("#instance-details-table").removeClass("hide");
                    }

                    tab.find(".viz-cell").not(cell).removeClass("hide");
                    tab.find(".viz-hdr").closest("th").not(hdr).removeClass("hide");
                    tab.find(".viz-table").removeClass("hide");

                    table.height("10%");
                    viz.removeClass("expanded-viz-div");
                    $(this).removeClass("expanded");
                    $(this).addClass("unexpanded");
                    button.trigger("click", [viz.width(), viz.height()]);

                    text_model_pre.removeClass("expanded");
                    text_model_pre.width("initial");
                    text_model_pre.height(text_model_pre_height / 2.5);

                }

            });
        }

        if (view === "right") {
            tweenVisualPanel.play();
        }
    });

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

// Moderation Functions

function deltaModerate() {
    if (document.getElementById("verification-time") !== null) {
        var subState = document.getElementById("instance-substate").innerHTML;
        var verificationTime = document.getElementById("verification-time").innerHTML;
        var verificationAddition = document.getElementById("verification-addition").innerHTML;
        var verificationReduction = document.getElementById("verification-reduction").innerHTML;

        var verAdd = document.getElementById("ver-add").innerHTML;
        var unverAdd = document.getElementById("unver-add").innerHTML;
        var verRed = document.getElementById("ver-red").innerHTML;
        var unverRed = document.getElementById("unver-red").innerHTML;

        if ((subState === 'READY' || subState !== 'FAILED') && verificationTime !== '') {
            $("#delta-System").addClass("hide");
            $(".verification-table").removeClass("hide");

            if (verificationAddition === '' || (verAdd === '{ }' && unverAdd === '{ }')) {
                $("#verification-addition-row").addClass("hide");
            }
            if (verificationReduction === '' || (verRed === '{ }' && unverRed === '{ }')) {
                $("#verification-reduction-row").addClass("hide");
            }
        }
    }
}

function instructionModerate() {
    if (document.getElementById("verification-run") !== null) {
        var subState = document.getElementById("instance-substate").innerHTML;
        var verificationState = document.getElementById("instance-verification").innerHTML;
        var verificationRun = document.getElementById("verification-run").innerHTML;
        var blockString = "";

        // State -1 - Error during validation/reconstruction
        if ((subState === 'READY' || subState === 'FAILED') && verificationState === "") {
            blockString = "Service encountered an error during verification. Please contact your technical supervisor for further instructions.";
        }
        // State 0 - Before Verify
        else if (subState !== 'READY' && subState !== 'FAILED') {
            blockString = "Service is still processing. Please hold for further instructions.";
        }
        // State 1 - Ready & Verifying
        else if (subState === 'READY' && verificationState === '0') {
            blockString = "Service is verifying.";
        }
        // State 2 - Ready & Verified
        else if (subState === 'READY' && verificationState === '1') {
            blockString = "Service has been successfully verified.";
        }
        // State 3 - Ready & Unverified
        else if (subState === 'READY' && verificationState === '-1') {
            blockString = "Service was not able to be verified.";
        }
        // State 4 - Failed & Verifying
        else if (subState === 'FAILED' && verificationState === '0') {
            blockString = "Service is verifying. (Run " + verificationRun + "/5)";
        }
        // State 5 - Failed & Verified
        else if (subState === 'FAILED' && verificationState === '1') {
            blockString = "Service has been successfully verified.";
        }
        // State 6 - Failed & Unverified
        else if (subState === 'FAILED' && verificationState === '-1') {
            blockString = "Service was not able to be verified.";
        }

        document.getElementById("instruction-block").innerHTML = blockString;
    }
}

function buttonModerate() {
    var superState = document.getElementById("instance-superstate").innerHTML;
    var subState = document.getElementById("instance-substate").innerHTML;
    var verificationState = document.getElementById("instance-verification").innerHTML;

    if (superState === 'Create') {
        // State 0 - Stuck
        if (verificationState === "" || verificationState === "null" || subState === "INIT") {
            $("#force_delete").toggleClass("hide");
            $("#force_cancel").toggleClass("hide");
            $("#force_retry").toggleClass("hide");
            $("#reverify").toggleClass("hide");
        }
        // State 1 - Ready & Verifying
        if (subState === 'READY' && verificationState === '0') {

        }
        // State 2 - Ready & Verified
        else if (subState === 'READY' && verificationState === '1') {
            $("#cancel").toggleClass("hide");
            $("#modify").toggleClass("hide");
        }
        // State 3 - Ready & Unverified
        else if (subState === 'READY' && verificationState === '-1') {
            $("#force_cancel").toggleClass("hide");
            $("#reverify").toggleClass("hide");
        }
        // State 4 - Failed & Verifying
        else if (subState === 'FAILED' && verificationState === '0') {

        }
        // State 5 - Failed & Verified
        else if (subState === 'FAILED' && verificationState === '1') {
            $("#force_cancel").toggleClass("hide");
            $("#force_modify").toggleClass("hide");
            $("#force_delete").toggleClass("hide");
        }
        // State 6 - Failed & Unverified
        else if (subState === 'FAILED' && verificationState === '-1') {
            $("#force_cancel").toggleClass("hide");
            $("#force_retry").toggleClass("hide");
            $("#reverify").toggleClass("hide");
            $("#force_delete").toggleClass("hide");
        }
    } else if (superState === 'Cancel') {
        // State 0 - Stuck
        if (verificationState === "" || verificationState === "null" || subState === "INIT") {
            $("#force_delete").toggleClass("hide");
            $("#force_retry").toggleClass("hide");
            $("#reverify").toggleClass("hide");
        }
        // State 1 - Ready & Verifying
        if (subState === 'READY' && verificationState === '0') {

        }
        // State 2 - Ready & Verified
        else if (subState === 'READY' && verificationState === '1') {
            $("#reinstate").toggleClass("hide");
            $("#modify").toggleClass("hide");
            $("#delete").toggleClass("hide");
        }
        // State 3 - Ready & Unverified
        else if (subState === 'READY' && verificationState === '-1') {
            $("#force_delete").toggleClass("hide");
            $("#force_reinstate").toggleClass("hide");
            $("#reverify").toggleClass("hide");
        }
        // State 4 - Failed & Verifying
        else if (subState === 'FAILED' && verificationState === '0') {

        }
        // State 5 - Failed & Verified
        else if (subState === 'FAILED' && verificationState === '1') {
            $("#force_reinstate").toggleClass("hide");
            $("#force_modify").toggleClass("hide");
            $("#delete").toggleClass("hide");
        }
        // State 6 - Failed & Unverified
        else if (subState === 'FAILED' && verificationState === '-1') {
            $("#force_delete").toggleClass("hide");
            $("#force_reinstate").toggleClass("hide");
            $("#force_retry").toggleClass("hide");
            $("#reverify").toggleClass("hide");
        }
    } else if (superState === 'Reinstate') {
        // State 0 - Stuck
        if (verificationState === "" || verificationState === "null" || subState === "INIT") {
            $("#force_delete").toggleClass("hide");
            $("#force_retry").toggleClass("hide");
            $("#reverify").toggleClass("hide");
        }
        // State 1 - Ready & Verifying
        if (subState === 'READY' && verificationState === '0') {

        }
        // State 2 - Ready & Verified
        else if (subState === 'READY' && verificationState === '1') {
            $("#cancel").toggleClass("hide");
            $("#modify").toggleClass("hide");
        }
        // State 3 - Ready & Unverified
        else if (subState === 'READY' && verificationState === '-1') {
            $("#force_cancel").toggleClass("hide");
            $("#reverify").toggleClass("hide");
        }
        // State 4 - Failed & Verifying
        else if (subState === 'FAILED' && verificationState === '0') {

        }
        // State 5 - Failed & Verified
        else if (subState === 'FAILED' && verificationState === '1') {
            $("#force_cancel").toggleClass("hide");
            $("#force_modify").toggleClass("hide");
        }
        // State 6 - Failed & Unverified
        else if (subState === 'FAILED' && verificationState === '-1') {
            $("#force_cancel").toggleClass("hide");
            $("#force_retry").toggleClass("hide");
            $("#reverify").toggleClass("hide");
        }
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
                subloadInstance();
                reloadLogs();
                $(".delta-table-header").click(function () {
                    $("#body-" + this.id).toggleClass("hide");
                });
                refreshSync(refreshed, timerSetting);
            }, 1000);
        } else {
            setTimeout(function () {
                subloadInstance();
                reloadLogs();
                $(".delta-table-header").click(function () {
                    $("#body-" + this.id).toggleClass("hide");
                });
                refreshSync(refreshed, timerSetting);
            }, 500);
        }
    });
}
