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
/* global baseUrl, keycloak, swal */

var refUUID;
var superState;
var subState;
var lastState;

var $superState = $("#instance-superstate");
var $subState = $("#instance-substate");
var $lastState = $("#instance-laststate");

var buttons;
var instruction;

var $instruction = $("#instruction-block");

var verificationHasDrone = true;
var verificationState;
var verificationResult;
var verificationRun;
var verificationElapsed;
var verificationTime;
var verificationAddition;
var verificationReduction;

function startDetailsEngine(uuid) {
    refUUID = uuid;
    attachListeners();

    updateData();
    renderDetails();
}

function renderDetails() {
    if (subState === "FAILED") {
        $subState.html(subState + " (after " + lastState + ")");
    } else {
        $subState.html(subState);
    }
    $superState.html(superState);

    // Instructions
    var instructionRegEx = /{{(\S*)}}/g.exec(instruction);
    if (instructionRegEx) {
        for (var i = 1; i < instructionRegEx.length; i++) {
            var str = instructionRegEx[i];
            var result = eval(instructionRegEx[i]);
            instruction = instruction.replace("{{" + str + "}}", result);
        }
    }
    if (verificationHasDrone && verificationElapsed) {
        instruction += " (Verification elapsed time: " + verificationElapsed + ")";
    }
    $instruction.html(instruction);

    // Buttons
    $(".instance-command").addClass("hide");
    for (var i in buttons) {
        var button = buttons[i];
        if (button === "verify" && verificationHasDrone) {
            $("#unverify").removeClass("hide");
        } else if (button === "cancel" && superState === "CANCEL") {
            $("#reinstate").removeClass("hide");
        } else if (button === "force_cancel" && superState === "CANCEL") {
            $("#force_reinstate").removeClass("hide");
        } else {
            $("#" + button).removeClass("hide");
        }
    }

    loadVisualization();
}

// --------------------

function updateData() {
    // Frontend superstate and metadata
    var apiUrl = baseUrl + '/StackV-web/restapi/app/details/' + refUUID + '/instance';
    $.ajax({
        url: apiUrl,
        async: false,
        type: 'GET',
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
        },
        success: function (instance) {
            var alias = instance[1];
            var creation = instance[2];
            superState = instance[3];
            lastState = instance[4];

            $("#instance-alias").html(alias);
            $("#instance-uuid").html(refUUID);
            $("#instance-creation-time").html(creation);
        }
    });

    // Substate
    var apiUrl = baseUrl + '/StackV-web/restapi/app/service/' + refUUID + '/substatus';
    $.ajax({
        url: apiUrl,
        async: false,
        type: 'GET',
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
            xhr.setRequestHeader("Refresh", keycloak.refreshToken);
        },
        success: function (result) {
            subState = result;
        }
    });

    // Verification
    var apiUrl = baseUrl + '/StackV-web/restapi/app/details/' + refUUID + '/verification';
    $.ajax({
        url: apiUrl,
        async: false,
        type: 'GET',
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
        },
        success: function (verification) {
            verificationState = verification[0];
            verificationResult = verification[1];
            verificationRun = verification[2];
            verificationTime = verification[3];
            verificationAddition = verification[4];
            verificationReduction = verification[5];
            verificationElapsed = verification[7];

            $.ajax({
                url: apiUrl += '/drone',
                async: false,
                type: 'GET',
                beforeSend: function (xhr) {
                    xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
                },
                success: function (retCode) {
                    verificationHasDrone = (retCode === "1");
                }
            });
        }
    });

    $.ajax({
        type: "GET",
        async: false,
        url: "/StackV-web/data/json/detailsStates.json",
        dataType: "json",
        success: function (data) {
            var dataObj = data[subState];
            instruction = dataObj["instruction"];
            buttons = dataObj["buttons"];

            var verObj = dataObj["verificationResult"];
            if (verObj) {
                var verRes = verObj[verificationResult];

                if (verRes && verRes["instruction"]) {
                    instruction = verRes["instruction"];
                }
                if (verRes && verRes["buttons"]) {
                    buttons = verRes["buttons"];
                }
            }

            var lastObj = dataObj["lastState"];
            if (lastObj) {
                var lastRes = lastObj[lastState];

                if (lastRes && lastRes["instruction"]) {
                    instruction = lastRes["instruction"];
                }
                if (lastRes && lastRes["buttons"]) {
                    buttons = lastRes["buttons"];
                }
            }
        }
    });
}

function attachListeners() {
    $(document).on('click', '.instance-command', function () {
        $(".instance-command").attr('disabled', true);
        pauseRefresh();

        var command = this.id;
        var apiUrl = baseUrl + '/StackV-web/restapi/service/' + refUUID + '/status';
        $.ajax({
            url: apiUrl,
            async: false,
            type: 'GET',
            beforeSend: function (xhr) {
                xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
                xhr.setRequestHeader("Refresh", keycloak.refreshToken);
            },
            success: function (result) {
                if (subState !== result) {
                    $(".instance-command").attr('disabled', false);
                    resumeRefresh();
                    reloadData();
                } else {
                    if ((command === "delete") || (command === "force_delete")) {
                        swal("Confirm deletion?", {
                            buttons: {
                                cancel: "Cancel",
                                delete: {text: "Delete", value: true}
                            }
                        }).then((value) => {
                            if (value) {
                                executeCommand(command);
                            } else {
                                setTimeout(function () {
                                    $(".instance-command").attr('disabled', false);
                                    resumeRefresh();
                                    reloadData();
                                }, 250);
                            }
                        });
                    } else {
                        swal({
                            buttons: false,
                            icon: "success",
                            closeOnEsc: false,
                            timer: 3000
                        });

                        executeCommand(command);
                    }
                }
            }
        });
    });
}
function executeCommand(command) {
    var apiUrl = baseUrl + '/StackV-web/restapi/app/service/' + refUUID + '/' + command;
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
                    window.document.location = "/StackV-web/portal/";
                }, 250);
            }
        }
    });
    switch (command) {
        case "cancel":
        case "force_cancel":
        case "reinstate":
        case "force_reinstate":
            setTimeout(function () {
                $(".instance-command").attr('disabled', false);
                resumeRefresh();
                reloadData();
            }, 2000);
            break;
        case "delete":
        case "force_delete":
            break;
        default:
            setTimeout(function () {
                $(".instance-command").attr('disabled', false);
                resumeRefresh();
                reloadData();
            }, 500);
    }
}
