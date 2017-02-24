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

/* global XDomainRequest, baseUrl, keycloak */
/* DETAILS */

$(function () {
    setTimeout(detailsLoad(), 750);
    setRefresh(60);
});

function detailsLoad() {
    var uuid = sessionStorage.getItem("uuid");

    // Subfunctions
    subloadInstance();
    subloadDelta();
    subloadVerification();
    subloadACL();

    // Moderation
    setTimeout(function () {
        loadStatus(uuid);
        loadACL(uuid);

        loadVisualization();
    }, 400);
}

function subloadInstance() {
    var uuid = sessionStorage.getItem("uuid");
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

            $("#details-panel").append("<div id='instance-verification' class='hide'>" + instance[0] + "</div>");
            var panel = document.getElementById("details-panel");

            var table = document.createElement("table");

            table.id = "details-table";
            table.className = "management-table";

            var thead = document.createElement("thead");
            var row = document.createElement("tr");
            var head = document.createElement("th");
            head.innerHTML = instance[1] + " Service Details";
            row.appendChild(head);
            head = document.createElement("th");
            head.innerHTML = '<div id="refresh-panel" class="form-inline">'
                    + '<label for="refresh-timer">Auto-Refresh Interval</label>'
                    + '<select id="refresh-timer" onchange="timerChange(this)" class="form-control">'
                    + '<option value="off">Off</option>'
                    + '<option value="5">5 sec.</option>'
                    + '<option value="10">10 sec.</option>'
                    + '<option value="30">30 sec.</option>'
                    + '<option value="60" selected>60 sec.</option>'
                    + '</select>'
                    + '</div>'
                    + '<button class="button-header btn btn-sm" id="refresh-button" onclick="reloadDetails()">Refresh in    seconds</button>';
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
                            enableLoading();
                            setTimeout(function () {
                                window.document.location = "/StackV-web/ops/catalog.jsp";
                            }, 250);
                        } else {
                            reloadDetails();
                        }
                    }
                });
            });
        }
    });
}

function subloadDelta() {
    var uuid = sessionStorage.getItem("uuid");
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
        }
    });
}

function subloadVerification() {
    var uuid = sessionStorage.getItem("uuid");
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
        }
    });
}

function subloadACL() {
    var uuid = sessionStorage.getItem("uuid");
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
        }
    });
}

function buildDeltaTable(type) {
    var panel = document.getElementById("details-panel");

    var table = document.createElement("table");
    table.className = "management-table hide " + type.toLowerCase() + "-delta-table";

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
    var verification = document.getElementsByClassName("verification-table");
    if (verification) {
        panel.insertBefore(table, verification[0]);
    } else {
        panel.appendChild(table);
    }
}

function loadVisualization() {
    $("#details-viz").load("/StackV-web/details_viz.html", function () {
        // Loading Verification visualization
        $("#ver-add").append($("#va_viz_div"));
        $("#ver-add").find("#va_viz_div").removeClass("hidden");

        $("#unver-add").append($("#ua_viz_div"));
        $("#unver-add").find("#ua_viz_div").removeClass("hidden");

        $("#ver-red").append($("#vr_viz_div"));
        $("#ver-red").find("#vr_viz_div").removeClass("hidden");

        $("#unver-red").append($("#ur_viz_div"));
        $("#unver-red").find("#ur_viz_div").removeClass("hidden");

        // Loading Service Delta visualization
        $("#delta-Service").addClass("hide");
        buildDeltaTable("Service");
        buildDeltaTable("System");

        $(".service-delta-table").removeClass("hide");

        $("#serv-add").append($("#serva_viz_div"));
        $("#serv-add").find("#serva_viz_div").removeClass("hidden");

        $("#serv-red").append($("#servr_viz_div"));
        $("#serv-red").find("#servr_viz_div").removeClass("hidden");

        // Loading System Delta visualization
        var subState = document.getElementById("instance-substate").innerHTML;
        var verificationTime = document.getElementById("verification-time").innerHTML;
        if ((subState !== 'READY' && subState === 'FAILED') || verificationTime === '') {
            $("#delta-System").addClass("hide");
            $("#delta-System").insertAfter(".system-delta-table");

            $(".system-delta-table").removeClass("hide");

            // Toggle button should toggle  between system delta visualization and delta-System table
            // if the verification failed
            document.querySelector(".system-delta-table .details-model-toggle").onclick = function () {
                toggleTextModel('.system-delta-table', '#delta-System');
            };

            $("#syst-red").append($("#sysr_viz_div"));
            $("#syst-add").append($("#sysa_viz_div"));

            $("#syst-red").find("#sysr_viz_div").removeClass("hidden");
            $("#syst-add").find("#sysa_viz_div").removeClass("hidden");
        } else {
            // Toggle button should toggle between  verification visualization and delta-System table
            // if the verification succeeded
            $("#delta-System").insertAfter(".verification-table");
            document.querySelector("#delta-System .details-model-toggle").onclick = function () {
                toggleTextModel('.verification-table', '#delta-System');
            };
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
            $(text_table).insertAfter("#details-table");
        }
        $(text_table).toggleClass("hide");

    }
}

/* REFRESH */
function timerChange(sel) {
    clearInterval(refreshTimer);
    clearInterval(countdownTimer);
    if (sel.value !== 'off') {
        setRefresh(sel.value);
    } else {
        document.getElementById('refresh-button').innerHTML = 'Manually Refresh Now';
    }
}

function refreshCountdown() {
    document.getElementById('refresh-button').innerHTML = 'Refresh in ' + countdown + ' seconds';
    countdown--;
}

function setRefresh(time) {
    countdown = time;
    refreshTimer = setInterval(function () {
        reloadDetails(time);
    }, (time * 1000));
    countdownTimer = setInterval(function () {
        refreshCountdown(time);
    }, 1000);
}

function reloadDetails(time) {
    enableLoading();
    keycloak.updateToken(90).error(function () {
        console.log("Error updating token!");
    }).success(function (refreshed) {
        if (refreshed) {
            sessionStorage.setItem("token", keycloak.token);
            console.log("Token Refreshed by nexus!");
        }

        var timerSetting = $("#refresh-timer").val();
        var uuid = getURLParameter("uuid");
        var manual = false;
        if (typeof time === "undefined") {
            time = countdown;
        }
        if (document.getElementById('refresh-button').innerHTML === 'Manually Refresh Now') {
            manual = true;
        }

        $('#details-panel').load(document.URL + ' #details-table', function () {
            loadDetails();

            setTimeout(function () {
                $("#refresh-timer").val(timerSetting);
                if (manual === false) {
                    countdown = time;
                    $("#refresh-button").html('Refresh in ' + countdown + ' seconds');
                } else {
                    $("#refresh-button").html('Manually Refresh Now');
                }

                $(".delta-table-header").click(function () {
                    $("#body-" + this.id).toggleClass("hide");
                });

                disableLoading();
            }, 750);
        });
    });
}
