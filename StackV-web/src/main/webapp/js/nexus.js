'use strict';
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
/* global XDomainRequest, baseUrl, loggedIn, TweenLite, Power2, tweenBlackScreen */
// Service JavaScript Library
var baseUrl = window.location.origin;
var keycloak = Keycloak('/StackV-web/resources/keycloak.json');
var refreshTimer;
var countdown;
var dataTableClass;
var countdownTimer;
var dataTable;
var loggedIn;

var fieldCounter = 0;
var queryCounter = 1;

// Page Load Function

$(function () {
    $.ajaxSetup({
        cache: false,
        timeout: 60000,
        beforeSend: function (xhr) {
            if (keycloak.token === undefined) {
                xhr.setRequestHeader("Authorization", "bearer " + sessionStorage.getItem("token"));
                xhr.setRequestHeader("Refresh", sessionStorage.getItem("refresh"));
            } else {
                xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
                xhr.setRequestHeader("Refresh", keycloak.refreshToken);
            }
        }
    });

    $(".hide-on-load").addClass("hidden");
    setTimeout(function () {
        $(".hide-on-load").removeClass("hidden");
        $(".hide-on-load").removeClass("hide-on-load");
    }, 2000);

    keycloak.init().success(function (authenticated) {
        if (authenticated) {
            var test = keycloak.isTokenExpired();
            loggedIn = authenticated ? true : false;
            sessionStorage.setItem("loggedIn", loggedIn);
            if (loggedIn) {
                sessionStorage.setItem("username", keycloak.tokenParsed.preferred_username);
                sessionStorage.setItem("subject", keycloak.tokenParsed.sub);
                sessionStorage.setItem("token", keycloak.token);
                sessionStorage.setItem("refresh", keycloak.refreshToken);
            }
        } else {
            keycloak.login();
        }
    }).error(function () {
        alert('failed to initialize');
    });
    keycloak.onAuthSuccess = function () {
        loadNavbar();

        if (window.location.pathname === "/StackV-web/portal/") {
            $("li#catalog-tab").addClass("active");
            loadCatalogNavbar();
            loadCatalog();
        } else if (window.location.pathname === "/StackV-web/portal/admin/") {
            $("li#admin-tab").addClass("active");
            loadAdminNavbar();
            loadAdmin();
        } else if (window.location.pathname === "/StackV-web/portal/acl/") {
            $("li#acl-tab").addClass("active");
            loadACLNavbar();
            loadACLPortal();
        } else if (window.location.pathname === "/StackV-web/portal/driver/") {
            $("li#driver-tab").addClass("active");
            loadDriverNavbar();
            loadDriverPortal();
        } else if (window.location.pathname === "/StackV-web/portal/intent/") {
            loadIntent(getURLParameter("intent"));
        } else if (window.location.pathname === "/StackV-web/portal/details/") {
            $("li#details-tab").addClass("active");
            var uuid = sessionStorage.getItem("instance-uuid");
            if (!uuid) {
                alert("No Service Instance Selected!");
                window.location.replace('/StackV-web/');
            } else {
                loadDetailsNavbar();
                loadDetails();
            }
        } else if (window.location.pathname === "/StackV-web/visual/manifest/manifestPortal.jsp") {
            loadManifest();
        } else if (window.location.pathname === "/StackV-web/visual/test/") {
            loadVisualization();
        }

        if ($("#tag-panel").length) {
            initTagPanel();
        }

        setInterval(function () {
            keycloak.updateToken(70);
        }, (60000));
    };
    keycloak.onTokenExpire = function () {
        keycloak.updateToken(20).success(function () {

            console.log("Token automatically updated!");
        }).error(function () {
            console.log("Automatic token update failed!");
        });
    };

    $(".button-group-select").click(function (evt) {
        $ref = "user_groups.jsp?id=" + this.id;
        $ref = $ref.replace('select', '') + " #group-specific";
        // console.log($ref);
        $("#group-specific").load($ref);
        evt.preventDefault();
    });

    $(".clickable-row").click(function () {
        sessionStorage.setItem("uuid", $(this).data("href"));
        window.document.location = "/StackV-web/ops/details/templateDetails.jsp";
    });

    $(".checkbox-level").click(function (evt) {
        evt.preventDefault();
    });

    clearCounters();
});

function loadNavbar() {
    $("#nav").load("/StackV-web/nav/navbar.html", function () {
        verifyPageRoles();

        if (window.location.pathname === "/StackV-web/portal/") {
            $("li#catalog-tab").addClass("active");
        } else if (window.location.pathname === "/StackV-web/portal/admin/") {
            $("li#admin-tab").addClass("active");
        } else if (window.location.pathname === "/StackV-web/portal/acl/") {
            $("li#acl-tab").addClass("active");
        } else if (window.location.pathname === "/StackV-web/portal/driver/") {
            $("li#driver-tab").addClass("active");
        } else if (window.location.pathname === "/StackV-web/portal/details/") {
            $("li#details-tab").addClass("active");
        } else if (window.location.pathname === "/StackV-web/visual/graphTest.jsp"
                || window.location.pathname === "/StackV-web/visual/test/") {
            $("li#visualization-tab").addClass("active");
        }

        var apiUrl = baseUrl + '/StackV-web/restapi/app/logging/';
        $.ajax({
            url: apiUrl,
            type: 'GET',
            dataType: "text",
            beforeSend: function (xhr) {
                xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
            },
            success: function (result) {
                $("#select-logging-level").val(result);
            },
            error: function (error, status, thrown) {
                console.log(error);
                console.log(status);
                console.log(thrown);
            }
        });

        $("#logout-button").click(function (evt) {
            keycloak.logout();

            evt.preventDefault();
        });
        $("#account-button").click(function (evt) {
            keycloak.accountManagement();

            evt.preventDefault();
        });
    });
}

function verifyPageRoles() {
    if (keycloak.tokenParsed.realm_access.roles.indexOf("A_Admin") <= -1) {
        $(".nav-admin").hide();
    }
    if (!keycloak.tokenParsed.realm_access.roles.includes("F_Drivers-R")) {
        $("#driver-tab").hide();
    }
    if (!keycloak.tokenParsed.realm_access.roles.includes("F_ACL-R")) {
        $("#acl-tab").hide();
    }
    if (!keycloak.tokenParsed.realm_access.roles.includes("F_Visualization-R")) {
        $("#visualization-tab").hide();
    }

    switch (window.location.pathname) {
        case "/StackV-web/portal/acl/":
            if (keycloak.tokenParsed.realm_access.roles.indexOf("F_ACL-R") === -1) {
                window.location.href = "/StackV-web/portal/";
            }
            break;
        case "/StackV-web/portal/admin/":
            if (keycloak.tokenParsed.realm_access.roles.indexOf("A_Admin") === -1) {
                window.location.href = "/StackV-web/portal/";
            }
            break;
        case "/StackV-web/portal/details/":
            if (keycloak.tokenParsed.realm_access.roles.indexOf("A_Admin") === -1) {
                var uuid = sessionStorage.getItem("instance-uuid");
                var apiUrl = baseUrl + '/StackV-web/restapi/app/access/instances/' + uuid;
                $.ajax({
                    url: apiUrl,
                    type: 'GET',
                    beforeSend: function (xhr) {
                        xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
                    },
                    success: function (result) {
                        if (result === "false") {
                            sessionStorage.removeItem("instance-uuid");
                            window.location.href = "/StackV-web/portal/";
                        }
                    }
                });
            }
            break;
        case "/StackV-web/portal/driver/":
            if (keycloak.tokenParsed.realm_access.roles.indexOf("F_Drivers-R") === -1) {
                window.location.href = "/StackV-web/portal/";
            }
            break;
        case "/StackV-web/portal/intent/":
            let intent = getURLParameter("intent").toUpperCase();
            if (keycloak.tokenParsed.realm_access.roles.indexOf("F_Services-" + intent) === -1) {
                window.location.href = "/StackV-web/portal/";
            }
            break;
        case "/StackV-web/visual/test/":
            if (keycloak.tokenParsed.realm_access.roles.indexOf("F_Visualization-R") === -1) {
                window.location.href = "/StackV-web/portal/";
            }
            break;
    }
}

function prettyPrintInfo() {
    var ugly = document.getElementById('profile-details-modal-text-area').value;
    var obj = JSON.parse(ugly);
    var pretty = JSON.stringify(obj, undefined, 4);
    document.getElementById('profile-details-modal-text-area').value = pretty;
}

//Select Function
function installSelect(sel) {
    if (sel.value !== null) {
        $ref = "/StackV-web/portal/driver/?form_install=" + sel.value + " #service-menu";
        $ref2 = "/StackV-web/portal/driver/?form_install=" + sel.value + " #service-fields";
    } else {
        $ref = "/StackV-web/portal/driver/ #service-menu";

        $ref2 = "/StackV-web/portal/driver/ #service-fields";


    }
    $("#service-top").load($ref);
    $("#service-bottom").load($ref2);
}

function driverSelect(sel) {
    if (sel.value !== null) {
        $ref = "/StackV-web/portal/driver/?form_install=install&driver_id=" + sel.value + " #service-fields";
    } else
        $ref = "/StackV-web/portal/driver/?form_install=install #service-fields";
    $("#service-bottom").load($ref);


    fieldCounter = 0;
}

/* API CALLS */

function checkInstance(uuid) {
    var apiUrl = baseUrl + '/StackV-web/restapi/service/' + uuid + '/status';
    $.ajax({
        url: apiUrl,
        type: 'GET',
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
            xhr.setRequestHeader("Refresh", keycloak.refreshToken);
        },
        success: function (result) {
            var statusElement = document.getElementById("instance-status");
            statusElement.innerHTML = result;
        }
    });
}

function cancelInstance(uuid) {
    var apiUrl = baseUrl + '/StackV-web/restapi/app/service/' + uuid + '/cancel';
    $.ajax({
        url: apiUrl,
        type: 'PUT',
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
        },
        success: function (result) {
            window.location.reload(true);
        }
    });
    //window.location.replace('/StackV-web/');
}
function forceCancelInstance(uuid) {
    var apiUrl = baseUrl + '/StackV-web/restapi/app/service/' + uuid + '/force_cancel';
    $.ajax({
        url: apiUrl,
        type: 'PUT',
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
        },
        success: function (result) {
            window.location.reload(true);
        }
    });
    //window.location.replace('/StackV-web/');
}

function reinstateInstance(uuid) {
    var apiUrl = baseUrl + '/StackV-web/restapi/app/service/' + uuid + '/reinstate';
    $.ajax({
        url: apiUrl,
        type: 'PUT',
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
        },
        success: function (result) {
            window.location.reload(true);
        }
    });
    //window.location.replace('/StackV-web/');
}
function forceReinstateInstance(uuid) {
    var apiUrl = baseUrl + '/StackV-web/restapi/app/service/' + uuid + '/force_reinstate';
    $.ajax({
        url: apiUrl,
        type: 'PUT',
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
        },
        success: function (result) {
            window.location.reload(true);
        }
    });
    //window.location.replace('/StackV-web/');
}

function forceRetryInstance(uuid) {
    var apiUrl = baseUrl + '/StackV-web/restapi/app/service/' + uuid + '/force_retry';
    $.ajax({
        url: apiUrl,
        type: 'PUT',
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
        },
        success: function (result) {
            window.location.reload(true);
        }
    });
    //window.location.replace('/StackV-web/');
}

function modifyInstance(uuid) {
    var apiUrl = baseUrl + '/StackV-web/restapi/app/service/' + uuid + '/modify';
    $.ajax({
        url: apiUrl,
        type: 'PUT',
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
        },
        success: function (result) {
            window.location.reload(true);
        }
    });
    //window.location.replace('/StackV-web/');
}

function forceModifyInstance(uuid) {
    var apiUrl = baseUrl + '/StackV-web/restapi/app/service/' + uuid + '/force_modify';
    $.ajax({
        url: apiUrl,
        type: 'PUT',
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
        },
        success: function (result) {
            window.location.reload(true);
        }
    });
    //window.location.replace('/StackV-web/');
}

function verifyInstance(uuid) {
    var apiUrl = baseUrl + '/StackV-web/restapi/app/service/' + uuid + '/verify';
    $.ajax({
        url: apiUrl,
        type: 'PUT',
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
            xhr.setRequestHeader("Refresh", keycloak.refreshToken);
        },
        success: function (result) {
            window.location.reload(true);
        }
    });
    //window.location.replace('/StackV-web/');
}

function deleteInstance(uuid) {
    var apiUrl = baseUrl + '/StackV-web/restapi/app/service/' + uuid + '/delete';
    $.ajax({
        url: apiUrl,
        type: 'PUT',
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
        },
        success: function (result) {
            console.log("DELETION SUCCESSFUL");
            window.location.replace('/StackV-web/');
        }
    });
}

/* UTILITY */
function getURLParameter(name) {
    return decodeURIComponent((new RegExp('[?|&]' + name + '=' + '([^&;]+?)(&|#|;|$)').exec(location.search) || [null, ''])[1].replace(/\+/g, '%20')) || null;
}

// Helper method to parse the title tag from the response.
function getTitle(text) {
    return text.match('<title>(.*)?</title>')[1];
}

function clearCounters() {
    fieldCounter = 0;
    queryCounter = 1;
}

function reloadPage() {
    window.location.reload(true);
}

function reloadPanel(panelId) {
    $('#' + panelId).load(document.URL + ' #' + panelId);
}

function emptyElement(id) {
    $("#" + id).empty();
}

// Create the XHR object.
function createCORSRequest(method, url) {
    var xhr = new XMLHttpRequest();
    if ("withCredentials" in xhr) {
        // XHR for Chrome/Firefox/Opera/Safari.
        xhr.open(method, url, true);
    } else if (typeof XDomainRequest !== "undefined") {
        // XDomainRequest for IE.
        xhr = new XDomainRequest();
        xhr.open(method, url);
    } else {
        // CORS not supported.
        xhr = null;
    }
    return xhr;
}

function enableLoading() {
    $("#main-pane").addClass("loading");
}

function disableLoading() {
    $("#main-pane").removeClass("loading");
}

function initTagPanel() {
    $.getScript("/StackV-web/js/stackv/label.js", function (data, textStatus, jqxhr) {
        if ($.trim($("#tag-panel").html()) !== '') {
            loadLabels();
        } else {
            $("#tag-panel").load("/StackV-web/tagPanel.jsp", function () {
                loadLabels();
            });
        }
    });
}

/* REFRESH */
function refreshSync(refreshed, time) {
    if (refreshed) {
        sessionStorage.setItem("token", keycloak.token);
        console.log("Token Refreshed by nexus!");
    }
    var manual = false;
    if (typeof time === "undefined") {
        time = countdown;
    }
    if (document.getElementById('refresh-button').innerHTML === 'Manually Refresh Now') {
        manual = true;
    }
    if (manual === false) {
        countdown = time;
        $("#refresh-button").html('Refresh in ' + countdown + ' seconds');
    } else {
        $("#refresh-button").html('Manually Refresh Now');
    }
}
function pauseRefresh() {
    clearInterval(refreshTimer);
    clearInterval(countdownTimer);
    document.getElementById('refresh-button').innerHTML = 'Paused';
    $("#refresh-timer").attr('disabled', true);
}
function resumeRefresh() {
    var timer = $("#refresh-timer");
    if (timer.attr('disabled')) {
        timer.attr('disabled', false);
        if (timer.val() === "off") {
            $("#refresh-button").html('Manually Refresh Now');
        } else {
            setRefresh(timer.val());
        }
    }
}
function timerChange(sel) {
    clearInterval(refreshTimer);
    clearInterval(countdownTimer);
    if (sel.value !== 'off') {
        setRefresh(sel.value);
    } else {
        document.getElementById('refresh-button').innerHTML = 'Manually Refresh Now';
        $(".loading-prog").css("width", "0%");
    }
}
function setRefresh(time) {
    countdown = time;
    countdownTimer = setInterval(function () {
        refreshCountdown(time);
    }, 1000);
    refreshTimer = setInterval(function () {
        $(".loading-prog").css("width", "0%");
        reloadData();
    }, (time * 1000));
}
function refreshCountdown() {
    $('#refresh-button').html('Refresh in ' + (countdown - 1) + ' seconds');
    countdown--;

    var setting = $("#refresh-timer").val();

    var prog = (setting - countdown + .5) / setting;
    $(".loading-prog").css("width", (prog * 100) + "%");
}
function reloadDataManual() {
    var timer = $("#refresh-timer");
    if (timer.attr('disabled')) {
        openLogDetails = 0;
        $("tr.shown").each(function () {
            var row = dataTable.row(this);
            row.child.hide();
            $(this).removeClass('shown');
        });

        resumeRefresh();
    } else {
        var sel = document.getElementById("refresh-timer");
        if (sel.value !== 'off') {
            timerChange(sel);
        }
        $(".loading-prog").css("width", "0%");
        reloadData();
    }
}


/* LOGGING */
var openLogDetails = 0;
var cachedStart = 0;
var justRefreshed = 0;
var now = new Date();
function loadLoggingDataTable(apiUrl) {
    dataTableClass = "logging";
    dataTable = $('#loggingData').DataTable({
        "ajax": {
            url: apiUrl,
            type: 'GET',
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
        "buttons": ['csv'],
        "columns": [
            {
                "className": 'details-control',
                "orderable": false,
                "data": null,
                "defaultContent": '',
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
        "dom": 'Bfrtip',
        "initComplete": function (settings, json) {
            console.log('DataTables has finished its initialization.');
        },
        "order": [[1, 'asc']],
        "ordering": false,
        "processing": true,
        "scroller": {
            displayBuffer: 15
        },
        "scrollX": true,
        "scrollY": "calc(60vh - 130px)",
        "serverSide": true
    });

    $("#loggingData").on('preXhr.dt', function () {
        // Event for opening loading animation
    });
    $("#loggingData").on('draw.dt', function () {
        // Event for closing loading animation
    });

    // Add event listener for opening and closing details
    $('#loggingData tbody').on('click', 'td.details-control', function () {
        let tr = $(this).closest('tr');
        var row = dataTable.row(tr);
        if (row.child.isShown()) {
            openLogDetails--;
            // This row is already open - close it
            row.child.hide();
            tr.removeClass('shown');
            if (openLogDetails === 0 && dataTable.scroller.page().start === 0) {
                resumeRefresh();
            }
        } else {
            openLogDetails++;

            // Open this row
            row.child(formatChild(row.data())).show();
            tr.addClass('shown');
            pauseRefresh();
        }
    });

    $('div.dataTables_scrollBody').scroll(function () {
        if (dataTable.scroller.page().start === 0 && openLogDetails === 0) {
            resumeRefresh();
        } else {
            pauseRefresh();
        }
    });

    dataTable.on('draw', function () {
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
}
function formatChild(d) {
    // `d` is the original data object for the row
    var retString = '<table cellpadding="5" cellspacing="0" border="0">';
    if (d.message !== "{}") {
        retString += '<tr>' +
                '<td style="width:10%">Message:</td>' +
                '<td style="white-space: normal">' + d.message + '</td>' +
                '</tr>';
    }
    if (d.exception !== "") {
        retString += '<tr>' +
                '<td>Exception:</td>' +
                '<td><textarea class="dataTables-child">' + d.exception + '</textarea></td>' +
                '</tr>';
    }
    if (d.referenceUUID !== "") {
        retString += '<tr>' +
                '<td>UUID:</td>' +
                '<td>' + d.referenceUUID + '</td>' +
                '</tr>';
    }
    retString += '<tr>' +
            '<td>Logger:</td>' +
            '<td>' + d.logger + '</td>' +
            '</tr>' +
            '</table>';

    return retString;
}

function loadInstanceDataTable(apiUrl) {
    dataTableClass = "instance";
    dataTable = $('#loggingData').DataTable({
        "ajax": {
            url: apiUrl,
            type: 'GET',
            beforeSend: function (xhr) {
                xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
            }
        },
        "buttons": ['csv'],
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
        "dom": 'Bfrtip',
        "initComplete": function (settings, json) {
            console.log('DataTables has finished its initialization.');
        },
        "ordering": false,
        "pageLength": 6,
        "scrollX": true,
        "scrollY": "375px"
    });

    $('#loggingData tbody').on('click', 'tr.instance-row', function () {
        sessionStorage.setItem("instance-uuid", this.children[2].innerHTML);
        window.document.location = "/StackV-web/portal/details/";
    });
}

function reloadLogs() {
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
    var nowStr = ('0' + now.getHours()).slice(-2) + ":" + ('0' + now.getMinutes()).slice(-2) + ":" + ('0' + now.getSeconds()).slice(-2);
    $time.text(nowStr);
}
function filterLogs() {
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
function downloadLogs() {
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
