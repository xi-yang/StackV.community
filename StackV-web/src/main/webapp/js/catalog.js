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

/* global XDomainRequest, baseUrl, keycloak, Power2, TweenLite, tweenBlackScreen, Mousetrap, swal */
// Tweens
var tweenInstancePanel = new TweenLite("#instance-panel", .75, {ease: Power2.easeInOut, paused: true, top: "40px"});

Mousetrap.bind({
    'shift+left': function () {
        window.location.href = "/StackV-web/orch/graphTest.jsp";
    },
    'shift+right': function () {
        window.location.href = "/StackV-web/portal/details/";
    }
});

function loadCatalog() {
    loadInstances();
    loadModals();

    loadSystemHealthCheck();
    tweenInstancePanel.play();
}
function loadCatalogNavbar() {
    $("#sub-nav").load("/StackV-web/nav/catalog_navbar.html", function () {
        setRefresh($("#refresh-timer").val());
    });
}

function loadInstances() {
    var apiUrl = baseUrl + '/StackV-web/restapi/app/logging/instances';
    loadInstanceDataTable(apiUrl);
}

function loadModals() {
    // Initialize
    var $catModal = $("#catalog-modal");
    $catModal.iziModal({
        width: 750,
        group: "cat"
    });
    var $profModal = $("#profiles-modal");
    $profModal.iziModal({
        width: 750,
        group: "cat"
    });

    // Load service metadata    
    var apiUrl = baseUrl + '/StackV-web/restapi/app/panel/' + keycloak.subject + '/editor';
    $.ajax({
        url: apiUrl,
        type: 'GET',
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
        },
        success: function (result) {
            for (i = 0; i < result.length; i++) {
                var meta = result[i];

                var name = meta[0];
                var desc = meta[1];
                var tag = meta[2];

                var $service = $('<a></a>');
                $service.addClass("list-group-item list-group-item-action flex-column align-items-start");
                $service.attr("data-tag", tag);

                $service.append('<h4>' + name + '</h4>');
                $service.append('<p>' + desc + '</p>');

                $("#catalog-modal-service-meta").append($service);
            }

            $("#catalog-modal-service-meta").on("click", "a", function (evt) {
                window.location.href = "/StackV-web/portal/intent?intent=" + $(this).data("tag");
            });
        }
    });
}

var originalProfile;
function loadWizard() {
    var userId = keycloak.subject;
    var tbody = document.getElementById("wizard-body");
    $("tbody#wizard-body").find("tr").remove();

    var apiUrl = baseUrl + '/StackV-web/restapi/app/panel/' + userId + '/wizard';
    $.ajax({
        url: apiUrl,
        type: 'GET',
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
        },
        success: function (result) {
            for (i = 0; i < result.length; i++) {
                var profile = result[i];

                var row = document.createElement("tr");
                var cell1_1 = document.createElement("td");
                cell1_1.innerHTML = profile[0];
                var cell1_2 = document.createElement("td");
                cell1_2.innerHTML = profile[1];
                var cell1_3 = document.createElement("td");
                cell1_3.innerHTML = "<button class='button-profile-select btn btn-default' id='" + profile[2] + "'>Select</button><button class='button-profile-delete btn btn' id='" + profile[2] + "'>Delete</button>";
                row.appendChild(cell1_1);
                row.appendChild(cell1_2);
                row.appendChild(cell1_3);
                tbody.appendChild(row);
            }

            $(".button-profile-select").on("click", function (evt) {
                var resultID = this.id,
                        apiUrl = baseUrl + '/StackV-web/restapi/app/profile/' + resultID;

                $.ajax({
                    url: apiUrl,
                    type: 'GET',
                    beforeSend: function (xhr) {
                        xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
                    },
                    success: function (result) {
                        $("#black-screen").removeClass("off");
                        $("#profile-modal").modal("show");
                        $("#info-panel-title").html("Profile Details");
                        $("#info-panel-text-area").val(JSON.stringify(result));
                        originalProfile = JSON.stringify(result);
                        $(".button-profile-save").attr('id', resultID);
                        $(".button-profile-save-as").attr('id', resultID);
                        $(".button-profile-submit").attr('id', resultID);
                        prettyPrintInfo();
                    },
                    error: function (textStatus, errorThrown) {
                        console.log(textStatus);
                        console.log(errorThrown);
                    }
                });

                evt.preventDefault();
            });

            $(".button-profile-delete").on("click", function (evt) {
                swal("Confirm deletion?", {
                    buttons: {
                        cancel: "Cancel",
                        delete: {text: "Delete", value: true}
                    }
                }).then((value) => {
                    if (value) {
                        var apiUrl = baseUrl + '/StackV-web/restapi/app/profile/' + this.id;
                        $.ajax({
                            url: apiUrl,
                            type: 'DELETE',
                            beforeSend: function (xhr) {
                                xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
                            },
                            success: function (result) {
                                loadWizard();
                            },
                            error: function (textStatus, errorThrown) {
                                console.log(textStatus);
                                console.log(errorThrown);
                            }
                        });
                    }
                });

                evt.preventDefault();
            });

            $(".button-profile-submit").on("click", function (evt) {
                if ($("#profile-alias").val()) {
                    var profile = JSON.parse($("#info-panel-text-area").val());
                    profile["alias"] = $("#profile-alias").val();

                    var apiUrl = baseUrl + '/StackV-web/restapi/app/service/uuid';
                    $.ajax({
                        url: apiUrl,
                        async: false,
                        type: 'GET',
                        dataType: "text",
                        beforeSend: function (xhr) {
                            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
                            xhr.setRequestHeader("Refresh", keycloak.refreshToken);
                        },
                        success: function (result) {
                            var manifest = profile;
                            manifest["uuid"] = result;
                            manifest["data"]["uuid"] = result;
                            manifest["data"]["options"] = manifest["options"];
                            //manifest["data"] = JSON.parse($("#info-panel-text-area").val());

                            // Render template
//                            var rendered = render(manifest);
//                            if (!rendered) {
//                                swal("Templating Error", "The manifest submitted could not be properly rendered. Please contact a system administrator.", "error");
//                                return;
//                            }

                            manifest['proceed'] = "true";
                            var apiUrl = baseUrl + '/StackV-web/restapi/app/service';
                            $.ajax({
                                url: apiUrl,
                                type: 'POST',
                                data: JSON.stringify(manifest),
                                contentType: "application/json; charset=utf-8",
                                dataType: "json",
                                beforeSend: function (xhr) {
                                    xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
                                    xhr.setRequestHeader("Refresh", keycloak.refreshToken);
                                },
                                success: function (result) {
                                },
                                error: function (textStatus, errorThrown) {
                                    console.log(textStatus);
                                    console.log(errorThrown);
                                }
                            });
                        }
                    });
                    // reload top table and hide modal
                    reloadData();
                    $("div#profile-modal").modal("hide");
                    $("#black-screen").addClass("off");
                    $("#info-panel").removeClass("active");
                    evt.preventDefault();
                } else {
                    $("#profile-alias").addClass("invalid");
                    $("#profile-alias").change(function () {
                        $(this).removeClass("invalid");
                    });
                }
            });

            // Hide the regular buttons and reveal the save as box
            $("button.button-profile-save-as").on("click", function (evt) {
                $("div.info-panel-regular-buttons").css("display", "none");
                $("div.info-panel-save-as-description").css("display", "block");
            });

            // Reveal the regular buttons and hide the save as boxes
            $("button.button-profile-save-as-cancel").on("click", function (evt) {
                $("div.info-panel-save-as-description").css("display", "none");
                $("div.info-panel-regular-buttons").css("display", "block");
            });


            // After the user has put a new name and description for the new profile
            $(".button-profile-save-as-confirm").on("click", function (evt) {
                var profileString = $("#info-panel-text-area").val();
                if (isJSONString(profileString)) {
                    var apiUrl = baseUrl + '/StackV-web/restapi/app/profile/new';
                    var data = {
                        name: $("#new-profile-name").val(),
                        username: keycloak.tokenParsed.preferred_username,
                        description: $("#new-profile-description").val(),
                        data: JSON.parse($("#info-panel-text-area").val())
                    };

                    $.ajax({
                        url: apiUrl,
                        type: 'PUT',
                        data: JSON.stringify(data), //stringify to get escaped JSON in backend
                        contentType: "application/json; charset=utf-8",
                        dataType: "json",
                        beforeSend: function (xhr) {
                            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
                            xhr.setRequestHeader("Refresh", keycloak.refreshToken);
                        },
                        success: function (result) {
                            // revert to regular buttons and close modal
                            $("input#new-profile-name").val("");
                            $("input#new-profile-description").val("");
                            $("div.info-panel-save-as-description").css("display", "none");
                            $("div.info-panel-regular-buttons").css("display", "block");
                            $("div#profile-modal").modal("hide");
                            // reload table
                            loadWizard();
                        },
                        error: function (textStatus, errorThrown) {
                            console.log(textStatus);
                            console.log(errorThrown);
                        }
                    });

                    // reload the bottom panel
                    $("#black-screen").addClass("off");
                    $("#info-panel").removeClass("active");
                    evt.preventDefault();
                } else {
                    swal('JSON Error', 'Data submitted is not a valid JSON! Please correct and try again.', 'error');
                }
            });

            $(".button-profile-save").on("click", function (evt) {
                var profileString = $("#info-panel-text-area").val();
                if (isJSONString(profileString)) {
                    var apiUrl = baseUrl + '/StackV-web/restapi/app/profile/' + this.id + '/edit';

                    $.ajax({
                        url: apiUrl,
                        type: 'PUT',
                        data: profileString,
                        contentType: "application/json; charset=utf-8",
                        dataType: "json",
                        beforeSend: function (xhr) {
                            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
                            xhr.setRequestHeader("Refresh", keycloak.refreshToken);
                        },
                        success: function (result) {
                            // reload the bottom panel
                            loadWizard();
                            $("#profile-modal").modal("hide");
                        },
                        error: function (textStatus, errorThrown) {
                            console.log(textStatus);
                            console.log(errorThrown);
                        }
                    });

                    $("#black-screen").addClass("off");
                    $("#info-panel").removeClass("active");
                    evt.preventDefault();
                } else {
                    swal('JSON Error', 'Data submitted is not a valid JSON! Please correct and try again.', 'error');
                }
            });

        }
    });
}
function isJSONString(str) {
    try {
        JSON.parse(str);
        return true;
    } catch (e) {
        return false;
    }
}


function loadEditor() {
    var userId = keycloak.subject;
    var tbody = document.getElementById("editor-body");

    var apiUrl = baseUrl + '/StackV-web/restapi/app/panel/' + userId + '/editor';
    $.ajax({
        url: apiUrl,
        type: 'GET',
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
        },
        success: function (result) {
            for (i = 0; i < result.length; i++) {
                var profile = result[i];

                var row = document.createElement("tr");
                var cell1_1 = document.createElement("td");
                cell1_1.innerHTML = profile[0];
                var cell1_2 = document.createElement("td");
                cell1_2.innerHTML = profile[1];
                var cell1_3 = document.createElement("td");
                cell1_3.innerHTML = "<button class='button-service-select btn btn-default' id='" + profile[2] + "'>Select</button";
                row.appendChild(cell1_1);
                row.appendChild(cell1_2);
                row.appendChild(cell1_3);
                tbody.appendChild(row);
            }
            $(document).on('click', '.button-service-select', function (evt) {
                var ref = "/StackV-web/portal/intent?intent=" + this.id.toLowerCase();
                window.location.href = ref;

                evt.preventDefault();
            });
        }
    });
}

function getURLParameter(name) {
    return decodeURIComponent((new RegExp('[?|&]' + name + '=' + '([^&;]+?)(&|#|;|$)').exec(location.search) || [null, ''])[1].replace(/\+/g, '%20')) || null;
}


/* REFRESH */
function reloadData() {
    keycloak.updateToken(90).error(function () {
        console.log("Error updating token!");
    }).success(function (refreshed) {
        var timerSetting = $("#refresh-timer").val();
        if (timerSetting > 15) {
            setTimeout(function () {
                reloadLogs();
                loadSystemHealthCheck();
                refreshSync(refreshed, timerSetting);
            }, 750);
        } else {
            setTimeout(function () {
                reloadLogs();
                loadSystemHealthCheck();
                refreshSync(refreshed, timerSetting);
            }, 500);
        }
    });
}


/*
 * Calls '/StackV-web/restapi/service/ready'
 * The API call returns true or false.
 * The prerequiste for this function is having a this div structure in the:
 * <div id="system-health-check">
 <div id="system-health-check-text"></div>
 </div>
 */
var systemHealthPass;
function loadSystemHealthCheck() {
    var apiUrl = baseUrl + '/StackV-web/restapi/service/ready';
    $.ajax({
        url: apiUrl,
        type: 'GET',
        dataType: "json",
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
            xhr.setRequestHeader("Refresh", keycloak.refreshToken);
        },
        success: function (result) {
            if (systemHealthPass !== result) {
                if (result === true) {
                    $("#system-health-span").removeClass("fail").removeClass("glyphicon-ban-circle")
                            .addClass("pass").addClass("glyphicon-ok-circle");
                } else {
                    $("#system-health-span").removeClass("pass").removeClass("glyphicon-ok-circle")
                            .addClass("fail").addClass("glyphicon-ban-circle");
                }

                systemHealthPass = result;
            }
        },
        error: function (err) {
            console.log("Error in system health check: " + JSON.stringify(err));
        }
    });
}
