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
var uuid = getURLParameter("uuid");
var token = sessionStorage.getItem("token");
$(function () {
    keycloak.updateToken().success(function () {

    }).error(function () {
        alert("Token could not be refreshed!");

    });

    deltaModerate();
    instructionModerate();
    buttonModerate();
    loadACL(uuid);
    loadStatus(uuid);
    loadVisualization();
    setRefresh(60);
});

function timerChange(sel) {
    clearInterval(refreshTimer);
    clearInterval(countdownTimer);
    if (sel.value !== 'off') {
        setRefresh(sel.value);
    } else {
        document.getElementById('refresh-button').innerHTML = 'Manually Refresh Now';
    }
}

function setRefresh(time) {
    countdown = time;
    refreshTimer = setInterval(function () {
        reloadInstance(time);
    }, (time * 1000));
    countdownTimer = setInterval(function () {
        refreshCountdown(time);
    }, 1000);
}

function reloadInstance(time) {
    enableLoading();

    var manual = false;
    if (typeof time === "undefined") {
        time = countdown;
    }
    if (document.getElementById('refresh-button').innerHTML === 'Manually Refresh Now') {
        manual = true;
    }

    $('#details-panel').load(document.URL + ' #details-panel', function () {
        deltaModerate();
        instructionModerate();
        buttonModerate();
        loadACL(uuid);
        loadStatus(uuid);
        loadVisualization();

        $(".delta-table-header").click(function () {
            $("#body-" + this.id).toggleClass("hide");
        });

        if (manual === false) {
            countdown = time;
            document.getElementById('refresh-button').innerHTML = 'Refresh in ' + countdown + ' seconds';
        } else {
            document.getElementById('refresh-button').innerHTML = 'Manually RefreshNow ';
        }

        setTimeout(function () {
            disableLoading();
        }, 750);
    });
}

function refreshCountdown() {
    document.getElementById('refresh-button').innerHTML = 'Refresh in ' + countdown + ' seconds';
    countdown--;
}

function loadVisualization() {
    $("#details-viz").load("/VersaStack-web/details_viz.jsp", function () {
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

            $("#sys-red").append($("#sysr_viz_div"));
            $("#sys-add").append($("#sysa_viz_div"));

            $("#sys-red").find("#sysr_viz_div").removeClass("hidden");
            $("#sys-add").find("#sysa_viz_div").removeClass("hidden");
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
        $(text_table).toggleClass("hide");
    }
}

// Moderation Functions

function deltaModerate() {
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

function instructionModerate() {
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

function buttonModerate() {
    var superState = document.getElementById("instance-superstate").innerHTML;
    var subState = document.getElementById("instance-substate").innerHTML;
    var verificationState = document.getElementById("instance-verification").innerHTML;

    if (superState === 'Create') {
        // State 0 - Stuck 
        if (verificationState === "") {
            $("#instance-fdelete").toggleClass("hide");
            $("#instance-fcancel").toggleClass("hide");
            $("#instance-fretry").toggleClass("hide");
            $("#instance-reverify").toggleClass("hide");
        }
        // State 1 - Ready & Verifying
        if (subState === 'READY' && verificationState === '0') {

        }
        // State 2 - Ready & Verified
        else if (subState === 'READY' && verificationState === '1') {
            $("#instance-cancel").toggleClass("hide");
            $("#instance-modify").toggleClass("hide");
        }
        // State 3 - Ready & Unverified
        else if (subState === 'READY' && verificationState === '-1') {
            $("#instance-fcancel").toggleClass("hide");
            $("#instance-reverify").toggleClass("hide");
        }
        // State 4 - Failed & Verifying
        else if (subState === 'FAILED' && verificationState === '0') {

        }
        // State 5 - Failed & Verified
        else if (subState === 'FAILED' && verificationState === '1') {
            $("#instance-fcancel").toggleClass("hide");
            $("#instance-fmodify").toggleClass("hide");
        }
        // State 6 - Failed & Unverified
        else if (subState === 'FAILED' && verificationState === '-1') {
            $("#instance-fcancel").toggleClass("hide");
            $("#instance-fretry").toggleClass("hide");
            $("#instance-reverify").toggleClass("hide");
        }
    } else if (superState === 'Cancel') {
        // State 0 - Stuck 
        if (verificationState === "") {
            $("#instance-fdelete").toggleClass("hide");
            $("#instance-fretry").toggleClass("hide");
            $("#instance-reverify").toggleClass("hide");
        }
        // State 1 - Ready & Verifying
        if (subState === 'READY' && verificationState === '0') {

        }
        // State 2 - Ready & Verified
        else if (subState === 'READY' && verificationState === '1') {
            $("#instance-reinstate").toggleClass("hide");
            $("#instance-modify").toggleClass("hide");
            $("#instance-delete").toggleClass("hide");
        }
        // State 3 - Ready & Unverified
        else if (subState === 'READY' && verificationState === '-1') {
            $("#instance-fdelete").toggleClass("hide");
            $("#instance-freinstate").toggleClass("hide");
            $("#instance-reverify").toggleClass("hide");
        }
        // State 4 - Failed & Verifying
        else if (subState === 'FAILED' && verificationState === '0') {

        }
        // State 5 - Failed & Verified
        else if (subState === 'FAILED' && verificationState === '1') {
            $("#instance-freinstate").toggleClass("hide");
            $("#instance-fmodify").toggleClass("hide");
            $("#instance-delete").toggleClass("hide");
        }
        // State 6 - Failed & Unverified
        else if (subState === 'FAILED' && verificationState === '-1') {
            $("#instance-fdelete").toggleClass("hide");
            $("#instance-freinstate").toggleClass("hide");
            $("#instance-fretry").toggleClass("hide");
            $("#instance-reverify").toggleClass("hide");
        }
    } else if (superState === 'Reinstate') {
        // State 0 - Stuck 
        if (verificationState === "") {
            $("#instance-fdelete").toggleClass("hide");
            $("#instance-fretry").toggleClass("hide");
            $("#instance-reverify").toggleClass("hide");
        }
        // State 1 - Ready & Verifying
        if (subState === 'READY' && verificationState === '0') {

        }
        // State 2 - Ready & Verified
        else if (subState === 'READY' && verificationState === '1') {
            $("#instance-cancel").toggleClass("hide");
            $("#instance-modify").toggleClass("hide");
        }
        // State 3 - Ready & Unverified
        else if (subState === 'READY' && verificationState === '-1') {
            $("#instance-fcancel").toggleClass("hide");
            $("#instance-reverify").toggleClass("hide");
        }
        // State 4 - Failed & Verifying
        else if (subState === 'FAILED' && verificationState === '0') {

        }
        // State 5 - Failed & Verified
        else if (subState === 'FAILED' && verificationState === '1') {
            $("#instance-fcancel").toggleClass("hide");
            $("#instance-fmodify").toggleClass("hide");
        }
        // State 6 - Failed & Unverified
        else if (subState === 'FAILED' && verificationState === '-1') {
            $("#instance-fcancel").toggleClass("hide");
            $("#instance-fretry").toggleClass("hide");
            $("#instance-reverify").toggleClass("hide");
        }
    }
}

function loadACL() {
    var userId = sessionStorage.getItem("subject");
    var select = document.getElementById("acl-select");
    $("#acl-select").empty();

    var apiUrl = baseUrl + '/VersaStack-web/restapi/app/panel/' + userId + '/acl';
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

    var apiUrl = baseUrl + '/VersaStack-web/restapi/app/service/' + refUuid + '/substatus';
    $.ajax({
        url: apiUrl,
        type: 'GET',
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
        },
        success: function (result) {
            for (i = 0; i < result.length; i++) {
                ele.innerHTML = result;
                console.log(result);
            }
        }
    });
}

function getURLParameter(name) {
    return decodeURIComponent((new RegExp('[?|&]' + name + '=' + '([^&;]+?)(&|#|;|$)').exec(location.search) || [null, ''])[1].replace(/\+/g, '%20')) || null;
}