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
    
});

function loadAdminNavbar() {
    $("#sub-nav").load("/StackV-web/admin_navbar.html", function () {
        switch (view) {
            case "left":
                $("#logging-tab").addClass("active");
                break;
            case "center":
                $("#admin-tab").addClass("active");
                break;
        }
        
        $("#logging-tab").click(function () {
            resetView();
            newView("logging");
        });
        $("#admin-tab").click(function () {
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
            $("#admin-tab").addClass("active");
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
    var uuid = sessionStorage.getItem("uuid");
    var apiUrl = baseUrl + '/StackV-web/restapi/app/logging/logs';
    $.ajax({
        url: apiUrl,
        type: 'GET',
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
        },
        success: function (logs) {
            var panel = document.getElementById("logging-panel");
            var table = document.createElement("table");
            panel.innerHTML = "";

            table.id = "logging-table";
            table.className = "management-table";

            var thead = document.createElement("thead");
            var row = document.createElement("tr");
            var head = document.createElement("th");
            head.innerHTML = "Logs";
            row.appendChild(head);
            thead.appendChild(row);
            table.appendChild(thead);

            var tbody = document.createElement("tbody");
            var row = document.createElement("tr");
            var cell = document.createElement("td");
            var div = document.createElement("div");
            div.id = "log-div";

            for (i = 0; i < logs.length; i++) {
                var log = logs[i];
                var detail = document.createElement("details");
                if (log["level"] === "WARN") {
                    detail.style = "color:orange";
                }
                if (log["level"] === "ERROR") {
                    detail.style = "color:red";
                }

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

            cell.appendChild(div);
            row.appendChild(cell);
            tbody.appendChild(row);
            table.appendChild(tbody);
            panel.appendChild(table);

            if (view === "left") {
                tweenLoggingPanel.play();
            }
        }
    });
}