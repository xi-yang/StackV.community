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
var tweenAdminPanel = new TweenLite("#admin-panel", 1, {ease: Power2.easeInOut, paused: true, top: "0px"});
var tweenLoggingPanel = new TweenLite("#logging-panel", 1, {ease: Power2.easeInOut, paused: true, left: "0px"});

var view = "left";

Mousetrap.bind({
    'left': function () {
        viewShift("left");
    },
    'right': function () {
        viewShift("right");
    }
});
function viewShift(dir) {
    resetView();
    switch (view) {
        case "left":
            if (dir === "right") {
                newView("admin");
            }
            break;
        case "center":
            switch (dir) {
                case "left":
                    newView("logging");
                    break;
            }
            view = dir;
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

function loadAdminNavbar() {
    setRefresh($("#refresh-timer").val());
    $("#sub-nav").load("/StackV-web/nav/admin_navbar.html", function () {
        switch (view) {
            case "left":
                $("#logging-tab").addClass("active");
                break;
            case "center":
                $("#sub-admin-tab").addClass("active");
                break;
        }

        $("#logging-tab").click(function () {
            resetView();
            newView("logging");
        });
        $("#sub-admin-tab").click(function () {
            resetView();
            newView("admin");
        });
    });
}

function resetView() {
    switch (view) {
        case "left":
            $("#sub-nav .active").removeClass("active");
            tweenLoggingPanel.reverse();
            break;
        case "center":
            $("#sub-nav .active").removeClass("active");
            tweenAdminPanel.reverse();
            break;
    }
}
function newView(panel) {
    switch (panel) {
        case "logging":
            tweenLoggingPanel.play();
            $("#logging-tab").addClass("active");
            view = "left";
            break;
        case "admin":
            tweenAdminPanel.play();
            $("#sub-admin-tab").addClass("active");
            view = "center";
            break;
    }
}

function loadAdmin() {
    // Subfunctions
    subloadAdmin();
}

function subloadAdmin() {
    subloadLogging();
}

function subloadLogging() {
    loadLogs();
    setTimeout(function () {
        if (view === "left") {
            tweenLoggingPanel.play();
        }
    }, 1000);
}


/* LOGGING */

function loadLogs() {
    var apiUrl = baseUrl + '/StackV-web/restapi/app/logging/logs';
    $.ajax({
        url: apiUrl,
        type: 'GET',
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
        },
        success: function (logs) {
            var div = document.getElementById("log-div");
            div.innerHTML = "";
            for (i = 0; i < logs.length; i++) {
                var log = logs[i];
                var detail = document.createElement("details");
                detail.className = "level-" + log["level"];

                /*  log mapping:
                 *      referenceUUID
                 *      marker
                 *      timestamp
                 *      level
                 *      logger
                 *      message
                 *      exception     
                 */

                var summary = document.createElement("summary");
                summary.innerHTML = log["timestamp"] + " - " + log["message"];
                detail.appendChild(summary);
                var data = document.createElement("p");
                data.innerHTML = "UUID: " + log["referenceUUID"];
                detail.appendChild(data);
                var data = document.createElement("p");
                data.innerHTML = "Level: " + log["level"];
                detail.appendChild(data);
                if (log["marker"]) {
                    var data = document.createElement("p");
                    data.innerHTML = "Marker: " + log["marker"];
                    detail.appendChild(data);
                }
                var data = document.createElement("p");
                data.innerHTML = "Logger: " + log["logger"];
                detail.appendChild(data);
                if (log["exception"]) {
                    var data = document.createElement("p");
                    data.innerHTML = "Exception: " + log["exception"];
                    detail.appendChild(data);
                }
                div.appendChild(detail);
            }

            filterLogs();
        }
    });
}

function filterLogs() {
    // Declare variables  
    var input = document.getElementById("filter-search-input");
    var filter = input.value.toUpperCase();
    $('#log-div').children('details').each(function () {
        if (this.innerHTML.toUpperCase().indexOf(filter) > -1) {
            $(this).removeClass("hide");
        } else {
            $(this).addClass("hide");
        }
    });
}


/* REFRESH */
function reloadData() {
    keycloak.updateToken(90).error(function () {
        console.log("Error updating token!");
    }).success(function (refreshed) {
        var timerSetting = $("#refresh-timer").val();
        refreshSync(refreshed, timerSetting);

        // Refresh Operations
        loadLogs();
    });
}