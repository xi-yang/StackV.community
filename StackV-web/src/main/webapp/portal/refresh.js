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
import { keycloak, page } from "./nexus";
import { openLogDetails, dataTable, reloadLogs } from "./logging";

/* Enabled Scripts */
import { reloadModals } from "./catalog";
import { fetchNewData } from "../visual/engine";
/* */

var refreshTimer;
var countdown;
var countdownTimer;

export function initRefresh(time) {
    $("body").on("change", "#sub-nav #refresh-timer", function () { timerChange(this); });
    $("body").on("click", "#sub-nav #refresh-button", function () { reloadDataManual(this); });
    setRefresh(time);
}

export function refreshSync(refreshed, time) {
    if (refreshed) {
        sessionStorage.setItem("token", keycloak.token);
        sessionStorage.setItem("refresh", keycloak.refreshToken);
    }
    var manual = false;
    if (typeof time === "undefined") {
        time = countdown;
    }
    if (document.getElementById("refresh-button").innerHTML === "Manually Refresh Now") {
        manual = true;
    }
    if (manual === false) {
        countdown = time;
        $("#refresh-button").html("Refresh in " + countdown + " seconds");
    } else {
        $("#refresh-button").html("Manually Refresh Now");
    }
}
export function pauseRefresh() {
    clearInterval(refreshTimer);
    clearInterval(countdownTimer);
    document.getElementById("refresh-button").innerHTML = "Paused";
    $("#refresh-timer").attr("disabled", true);
}
export function resumeRefresh() {
    var timer = $("#refresh-timer");
    if (timer.attr("disabled")) {
        timer.attr("disabled", false);
        if (timer.val() === "off") {
            $("#refresh-button").html("Manually Refresh Now");
        } else {
            setRefresh(timer.val());
        }
    }
}
export function timerChange(sel) {
    clearInterval(refreshTimer);
    clearInterval(countdownTimer);
    if (sel.value !== "off") {
        setRefresh(sel.value);
    } else {
        document.getElementById("refresh-button").innerHTML = "Manually Refresh Now";
        $(".loading-prog").css("width", "0%");
    }
}
export function setRefresh(time) {
    if (time === "off") {
        document.getElementById("refresh-button").innerHTML = "Manually Refresh Now";
        $(".loading-prog").css("width", "0%");
    } else {
        countdown = time;
        countdownTimer = setInterval(function () {
            refreshCountdown(time);
        }, 1000);
        refreshTimer = setInterval(function () {
            $(".loading-prog").css("width", "0%");
            reloadData();
        }, (time * 1000));
    }
}
export function refreshCountdown() {
    $("#refresh-button").html("Refresh in " + (countdown - 1) + " seconds");
    countdown--;

    var setting = $("#refresh-timer").val();

    var prog = (setting - countdown + .5) / setting;
    $(".loading-prog").css("width", (prog * 100) + "%");
}
export function reloadDataManual() {
    var timer = $("#refresh-timer");
    if (timer.attr("disabled")) {
        openLogDetails = 0;
        $("tr.shown").each(function () {
            var row = dataTable.row(this);
            row.child.hide();
            $(this).removeClass("shown");
        });

        resumeRefresh();
    } else {
        var sel = document.getElementById("refresh-timer");
        if (sel.value !== "off") {
            timerChange(sel);
        }
        $(".loading-prog").css("width", "0%");
        reloadData();
    }
}

/* PAGE RELOADS */
export function reloadData() {
    keycloak.updateToken(90).error(function () {
        console.log("Error updating token!");
    }).success(function (refreshed) {
        let timerSetting = $("#refresh-timer").val();
        switch (page) {
        case "catalog":
            setTimeout(function () {
                reloadLogs();
                reloadModals();
            }, 500);
            break;
        case "admin":
            setTimeout(function () {
                reloadLogs();
            }, 500);
            break;

        case "visualization":
            fetchNewData();
            break;
        }

        loadSystemHealthCheck();
        refreshSync(refreshed, timerSetting);
    });
}

/* */

/*
* Calls '/StackV-web/restapi/service/ready'
* The API call returns true or false.
* The prerequiste for this function is having a this div structure in the:
* <div id="system-health-check">
<div id="system-health-check-text"></div>
</div>
*/
function loadSystemHealthCheck() {
    var apiUrl = window.location.origin + "/StackV-web/restapi/service/ready";
    $.ajax({
        url: apiUrl,
        type: "GET",
        dataType: "json",
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
            xhr.setRequestHeader("Refresh", keycloak.refreshToken);
        },
        success: function (result) {
            if (result === true) {
                $("#system-health-span").removeClass("fail").removeClass("glyphicon-ban-circle")
                    .addClass("pass").addClass("glyphicon-ok-circle");
            } else {
                $("#system-health-span").removeClass("pass").removeClass("glyphicon-ok-circle")
                    .addClass("fail").addClass("glyphicon-ban-circle");
            }
        },
        error: function (err) {
            console.log("Error in system health check: " + JSON.stringify(err));
        }
    });
}
