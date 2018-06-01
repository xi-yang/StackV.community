"use strict";
/*
* Copyright (c) 2013-2018 University of Maryland
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
import { keycloak } from "./nexus";
import { resumeRefresh, pauseRefresh } from "./refresh";

export var openLogDetails = 0;
var cachedStart = 0;
var justRefreshed = 0;
var dataTableClass;
export var dataTable;
var now = new Date();
export function loadLoggingDataTable(apiUrl) {
    dataTableClass = "logging";
    dataTable = $("#loggingData").DataTable({
        "ajax": {
            url: apiUrl,
            type: "GET",
            beforeSend: function (xhr) {
                xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
            },
            data: function (d) {
                if (justRefreshed > 0) {
                    d.start = cachedStart;
                    justRefreshed--;
                }
            }
        },
        "buttons": ["csv"],
        "columns": [
            {
                "className": "details-control",
                "orderable": false,
                "data": null,
                "defaultContent": "",
                "width": "20px"
            },
            {"data": "timestamp", "width": "150px"},
            {"data": "level", "width": "60px"},
            {"data": "event"},
            {"data": "referenceUUID", "width": "275px"},
            {"data": "message", "visible": false, "searchable": false}
        ],
        "createdRow": function (row, data, dataIndex) {
            $(row).addClass("row-" + data.level.toLowerCase());
        },
        "dom": "Bfrtip",
        "initComplete": function (settings, json) {
            console.log("DataTables has finished its initialization.");
        },
        "order": [[1, "asc"]],
        "ordering": false,
        "processing": true,
        "scroller": {
            displayBuffer: 15
        },
        "scrollX": true,
        "scrollY": "calc(60vh - 130px)",
        "serverSide": true
    });

    $("#loggingData").on("preXhr.dt", function () {
        // Event for opening loading animation
    });
    $("#loggingData").on("draw.dt", function () {
        // Event for closing loading animation
    });

    // Add event listener for opening and closing details
    $("#loggingData tbody").on("click", "td.details-control", function () {
        let tr = $(this).closest("tr");
        var row = dataTable.row(tr);
        if (row.child.isShown()) {
            openLogDetails--;
            // This row is already open - close it
            row.child.hide();
            tr.removeClass("shown");
            if (openLogDetails === 0 && dataTable.scroller.page().start === 0) {
                resumeRefresh();
            }
        } else {
            openLogDetails++;

            // Open this row
            row.child(formatChild(row.data())).show();
            tr.addClass("shown");
            pauseRefresh();
        }
    });

    $("div.dataTables_scrollBody").scroll(function () {
        if (dataTable.scroller.page().start === 0 && openLogDetails === 0) {
            resumeRefresh();
        } else {
            pauseRefresh();
        }
    });

    dataTable.on("draw", function () {
        cachedStart = dataTable.ajax.params().start;
    });

    setInterval(function () {
        drawLoggingCurrentTime();
    }, (1000));

    var level = sessionStorage.getItem("logging-level");
    if (level !== null) {
        $("#logging-filter-level").val(level);
    } else {
        sessionStorage.setItem("logging-level", "INFO");
        $("#logging-filter-level").val("INFO");
    }

    $("#logging-filter-level").change(function () {
        filterLogs(this);
    });    
}
function formatChild(d) {
    // `d` is the original data object for the row
    var retString = "<table cellpadding=\"5\" cellspacing=\"0\" border=\"0\">";
    if (d.message !== "{}") {
        retString += "<tr>" +
        "<td style=\"width:10%\">Message:</td>" +
        "<td style=\"white-space: normal\">" + d.message + "</td>" +
        "</tr>";
    }
    if (d.exception !== "") {
        retString += "<tr>" +
        "<td>Exception:</td>" +
        "<td><textarea class=\"dataTables-child\">" + d.exception + "</textarea></td>" +
        "</tr>";
    }
    if (d.referenceUUID !== "") {
        retString += "<tr>" +
        "<td>UUID:</td>" +
        "<td>" + d.referenceUUID + "</td>" +
        "</tr>";
    }
    retString += "<tr>" +
    "<td>Logger:</td>" +
    "<td>" + d.logger + "</td>" +
    "</tr>" +
    "</table>";

    return retString;
}

export function loadInstanceDataTable(apiUrl) {
    dataTableClass = "instance";
    dataTable = $("#loggingData").DataTable({
        "ajax": {
            url: apiUrl,
            type: "GET",
            beforeSend: function (xhr) {
                xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
            }
        },
        "buttons": ["csv"],
        "columns": [
            {"data": "alias"},
            {"data": "type", width: "110px"},
            {"data": "referenceUUID", "width": "250px"},
            {"data": "state", "width": "125px"}
        ],
        "createdRow": function (row, data, dataIndex) {
            $(row).addClass("instance-row");
            $(row).attr("data-verification_state", data.verification);
            $(row).attr("data-last_state", data.lastState);
            $(row).attr("data-owner", data.owner);
        },
        "dom": "Bfrtip",
        "initComplete": function (settings, json) {
            console.log("DataTables has finished its initialization.");
        },
        "ordering": false,
        "pageLength": 6,
        "scrollX": true,
        "scrollY": "375px"
    });

    $("#loggingData tbody").on("click", "tr.instance-row", function () {
        sessionStorage.setItem("instance-uuid", this.children[2].innerHTML);
        window.document.location = "/StackV-web/portal/details/";
    });
}

export function reloadLogs() {
    justRefreshed = 2;
    if (dataTable && dataTableClass === "logging") {
        if (sessionStorage.getItem("logging-level") !== null) {
            dataTable.ajax.reload(filterLogs(), false);
        } else {
            dataTable.ajax.reload(null, false);
        }
    } else {
        dataTable.ajax.reload(null, false);
    }
}
function drawLoggingCurrentTime() {
    now = new Date();
    var $time = $("#log-time");
    var nowStr = ("0" + now.getHours()).slice(-2) + ":" + ("0" + now.getMinutes()).slice(-2) + ":" + ("0" + now.getSeconds()).slice(-2);
    $time.text(nowStr);
}
export function filterLogs() {
    var level = $("#logging-filter-level").val();
    if (level !== undefined) {
        sessionStorage.setItem("logging-level", level);

        var curr = dataTable.ajax.url();
        var paramArr = curr.split(/[?&]+/);
        var newURL = paramArr[0];

        var refEle;
        for (var x in paramArr) {
            if (paramArr[x].indexOf("refUUID") !== -1) {
                refEle = paramArr[x];
            }
        }

        newURL += "?level=" + level;
        if (refEle) {
            newURL += "&" + refEle;
        }

        dataTable.ajax.url(newURL).load(null, false);
    }
}
export function downloadLogs() {
    var ret = [];
    if (dataTable) {
        var data = dataTable.rows().data();
        for (var i in data) {
            var log = data[i];

            ret.push(JSON.stringify(log));
        }
    }
    return ret;
}
