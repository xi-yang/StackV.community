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
var tweenInstancePanel = new TweenLite("#instance-panel", .75, {ease: Power2.easeInOut, paused: true, autoAlpha: 1});

var $catModal = $("#catalog-modal");
var $profModal = $("#profiles-modal");

Mousetrap.bind({
    'shift+left': function () {
        window.location.href = "/StackV-web/orch/graphTest.jsp";
    },
    'shift+right': function () {
        window.location.href = "/StackV-web/portal/details/";
    },
    'space': function () {
        toggleModal('catalog');
    },
    'shift+space': function () {
        toggleModal('profile');
    }
});
function toggleModal(modalName) {
    switch (modalName) {
        case "catalog":
            // Toggle catalog modal
            switch ($catModal.iziModal('getState')) {
                case "opened":
                    $catModal.iziModal('close');
                    break;
                case "closed":
                    switch ($profModal.iziModal('getState')) {
                        case "closed":
                            $catModal.iziModal('open');
                            break;
                        case "opened":
                            $profModal.iziModal('prev');
                            break;
                    }
                    break;
                case "opening":
                case "closing":
                    break;
                default:
                    switch ($profModal.iziModal('getState')) {
                        case "closed":
                            $profModal.iziModal('open');
                            break;
                        case "opened":
                            $profModal.iziModal('close');
                            break;
                    }
                    break;
            }
            break;
        case "profile":
            // Toggle profile modal
            switch ($catModal.iziModal('getState')) {
                case "opened":
                    $catModal.iziModal('next');
                    break;
                case "opening":
                case "closing":
                    break;
                case "closed":
                default:
                    switch ($profModal.iziModal('getState')) {
                        case "closed":
                            $profModal.iziModal('open');
                            break;
                        case "opened":
                            $profModal.iziModal('close');
                            break;
                    }
                    break;
            }
            break;
    }
}

function loadCatalog() {
    loadInstances();
    loadModals();

    loadSystemHealthCheck();
    tweenInstancePanel.play();

    $(".button-service-create").click(function (evt) {
        evt.preventDefault();

        toggleModal("catalog");
    });
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

var profileUpdateTimeout = null;

var catCount = 0;
var catConfig = {
    width: 750,
    group: "cat"
};
var profCount = 0;
var profConfig = {
    width: 750,
    group: "cat"
};
function loadModals() {
    // Initialize
    $("#catalog-modal").html('<div class="catalog-modal-body">' +
            '<p class="catalog-modal-body-header">Select a service type:</p>' +
            '<div id="catalog-modal-service-meta" class="list-group" style="cursor: pointer;"></div>' +
            '<hr><button class="button-catalog-modal-switch btn btn-primary" data-izimodal-open="#profiles-modal">Load Saved Profile</button>' +
            '</div>');
    $("#profiles-modal").html('<div class="profiles-modal-body">' +
            '<p class="profiles-modal-body-header">Select a saved service profile:</p>' +
            '<div id="profiles-modal-service-meta" class="list-group" style="cursor: pointer;"></div>' +
            '<hr><button class="btn btn-primary" data-izimodal-open="#catalog-modal">Return to Service Catalog</button>' +
            '</div>');

    $catModal.iziModal(catConfig);
    $profModal.iziModal(profConfig);

    // Load service metadata. 
    var apiUrl = baseUrl + '/StackV-web/restapi/app/panel/editor';
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

                $service.append('<h4 style="display: inline-block;">' + name + '</h4>');
                if (desc) {
                    $service.append('<p>' + desc + '</p>');
                }

                $("#catalog-modal-service-meta").append($service);
                catCount++;
            }

            $("#catalog-modal-service-meta").on("click", "a", function (evt) {
                window.location.href = "/StackV-web/portal/intent?intent=" + $(this).data("tag");
            });

            moderateModals();
        }
    });

    // Load service profiles.
    var originalProfile;
    var apiUrl = baseUrl + '/StackV-web/restapi/app/panel/wizard';
    $.ajax({
        url: apiUrl,
        type: 'GET',
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
        },
        success: function (result) {
            for (i = 0; i < result.length; i++) {
                var profile = result[i];

                var name = profile[0];
                var desc = profile[1];
                var id = profile[2];
                var owner = profile[3];
                var editable = profile[4];
                var created = profile[5].split(".")[0];
                var lastEdited = profile[6];
                if (lastEdited) {
                    lastEdited = lastEdited.split(".")[0];
                }

                var $profile = $('<a></a>');
                $profile.addClass("list-group-item list-group-item-action flex-column align-items-start");
                $profile.attr("data-id", id);

                $profile.append('<h4 style="display: inline-block;">' + name + '</h4>');

                // Properties                
                var $note = $('<small></small>');
                if (owner !== keycloak.tokenParsed.preferred_username) {
                    $note.css({"color": "#777", "padding": "5px"});
                    $note.text("created by " + owner + " ");
                    $profile.append($note);
                    if (editable === "0") {
                        $profile.css("box-shadow", "inset 0px 0px 2px 0px #ff5f5f");
                        $note.text($note.text() + "(Read only)");
                    }
                }

                var $time = $('<small></small>');
                $time.css({"float": "right", "text-align": "right", "padding-top": "10px"});
                var timeStr = "Created: " + created;
                if (lastEdited) {
                    timeStr += "<br>Last edited: " + lastEdited;
                }
                $time.html(timeStr);
                $profile.append($time);

                if (desc) {
                    $profile.append('<p>' + desc + '</p>');
                }
                // ***

                $("#profiles-modal-service-meta").append($profile);
                profCount++;
            }

            $("#profiles-modal-service-meta").on("click", "a", function (evt) {
                var resultID = $(this).data("id");
                var apiUrl = baseUrl + '/StackV-web/restapi/app/profile/' + resultID;
                $.ajax({
                    url: apiUrl,
                    type: 'GET',
                    beforeSend: function (xhr) {
                        xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
                    },
                    success: function (result) {
                        resetProfileModal();

                        if (result["owner"] === keycloak.tokenParsed.preferred_username
                                || result["editable"] === "1") {
                            $("#info-panel-text-area").removeAttr("readonly");
                            $(".button-profile-save").removeAttr("disabled");
                            $(".button-profile-save-as").removeAttr("disabled");
                            $(".read-only-flag").addClass("hidden");
                        } else {
                            $("#info-panel-text-area").attr("readonly", true);
                            $(".button-profile-save").attr('disabled', true);
                            $(".button-profile-save-as").attr('disabled', true);
                            $(".read-only-flag").removeClass("hidden");
                        }

                        $("#profile-modal").modal("show");
                        $("#info-panel-title").html("Profile Details");
                        $("#info-panel-text-area").val(result["wizard_json"]);
                        originalProfile = result["wizard_json"];
                        $(".button-profile-save").attr('id', resultID);
                        $(".button-profile-save-as").attr('id', resultID);
                        $(".button-profile-submit").attr('id', resultID);

                        if (result["owner"] === keycloak.tokenParsed.preferred_username) {
                            $("#info-panel-management").show();
                            $("#edit-profile-licenses").val(result["licenses"]);
                            
                            $(".button-profile-delete").show();
                        }
                        prettyPrintInfo();
                    },
                    error: function (textStatus, errorThrown) {
                        console.log(textStatus);
                        console.log(errorThrown);
                    }
                });

                evt.preventDefault();
            });


            // Legacy modal listeners.            
            $("#edit-profile-licenses").on("keyup", function () {
                clearTimeout(profileUpdateTimeout);
                profileUpdateTimeout = setTimeout(function () {
                    var apiUrl = baseUrl + '/StackV-web/restapi/app/profile/' + $(".button-profile-save").attr("id") + '/edit/licenses';
                    $.ajax({
                        url: apiUrl,
                        type: 'PUT',
                        data: $("#edit-profile-licenses").val(),
                        contentType: "application/json; charset=utf-8",
                        dataType: "json"
                    });
                }, 1000);
            });

            $(".button-profile-delete").on("click", function (evt) {
                swal("Confirm deletion?", {
                    buttons: {
                        cancel: "Cancel",
                        delete: {text: "Delete", value: true}
                    }
                }).then((value) => {
                    if (value) {
                        var apiUrl = baseUrl + '/StackV-web/restapi/app/profile/' + $(".button-profile-save").attr("id");
                        $.ajax({
                            url: apiUrl,
                            type: 'DELETE',
                            beforeSend: function (xhr) {
                                xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
                            },
                            success: function (result) {
                                $("#profile-modal").modal("hide");
                                reloadModals();
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

                            manifest['proceed'] = "true";
                            var apiUrl = baseUrl + '/StackV-web/restapi/app/profile';
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
                $("div.info-panel-save-as-description").css("display", "inline-block");
            });

            // Reveal the regular buttons and hide the save as boxes
            $("button.button-profile-save-as-cancel").on("click", function (evt) {
                $("div.info-panel-save-as-description").css("display", "none");
                $("div.info-panel-regular-buttons").css("display", "inline-block");
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
                        licenses: $("#new-profile-licenses").val(),
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
                        },
                        error: function (textStatus, errorThrown) {
                            console.log(textStatus);
                            console.log(errorThrown);
                        }
                    });

                    // reload the bottom panel
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
                            $("#profile-modal").modal("hide");
                        },
                        error: function (textStatus, errorThrown) {
                            console.log(textStatus);
                            console.log(errorThrown);
                        }
                    });

                    $("#info-panel").removeClass("active");
                    evt.preventDefault();
                } else {
                    swal('JSON Error', 'Data submitted is not a valid JSON! Please correct and try again.', 'error');
                }
            });
        }
    });
}
function resetProfileModal() {
    clearTimeout(profileUpdateTimeout);

    $("#info-panel-management").hide();
    $("#edit-profile-licenses").val("");

    $("#profile-alias").val("");
    
    $(".button-profile-delete").hide();
}

function reloadModals() {
    // Load service metadata. 
    catCount = 0, profCount = 0;

    var apiUrl = baseUrl + '/StackV-web/restapi/app/panel/editor';
    $.ajax({
        url: apiUrl,
        type: 'GET',
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
        },
        success: function (result) {
            $("#catalog-modal-service-meta").empty();
            for (i = 0; i < result.length; i++) {
                var meta = result[i];

                var name = meta[0];
                var desc = meta[1];
                var tag = meta[2];

                var $service = $('<a></a>');
                $service.addClass("list-group-item list-group-item-action flex-column align-items-start");
                $service.attr("data-tag", tag);

                $service.append('<h4 style="display: inline-block;">' + name + '</h4>');
                if (desc) {
                    $service.append('<p>' + desc + '</p>');
                }

                $("#catalog-modal-service-meta").append($service);
                catCount++;
            }
        }
    });

    // Load service profiles.
    var apiUrl = baseUrl + '/StackV-web/restapi/app/panel/wizard';
    $.ajax({
        url: apiUrl,
        type: 'GET',
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
        },
        success: function (result) {
            $("#profiles-modal-service-meta").empty();
            for (i = 0; i < result.length; i++) {
                var profile = result[i];

                var name = profile[0];
                var desc = profile[1];
                var id = profile[2];
                var owner = profile[3];
                var editable = profile[4];
                var created = profile[5].split(".")[0];
                var lastEdited = profile[6];
                if (lastEdited) {
                    lastEdited = lastEdited.split(".")[0];
                }

                var $profile = $('<a></a>');
                $profile.addClass("list-group-item list-group-item-action flex-column align-items-start");
                $profile.attr("data-id", id);

                $profile.append('<h4 style="display: inline-block;">' + name + '</h4>');

                // Properties                
                var $note = $('<small></small>');
                if (owner !== keycloak.tokenParsed.preferred_username) {
                    $note.css({"color": "#777", "padding": "5px"});
                    $note.text("created by " + owner + " ");
                    $profile.append($note);
                    if (editable === "0") {
                        $profile.css("box-shadow", "inset 0px 0px 2px 0px #ff5f5f");
                        $note.text($note.text() + "(Read only)");
                    }
                }

                var $time = $('<small></small>');
                $time.css({"float": "right", "text-align": "right", "padding-top": "10px"});
                var timeStr = "Created: " + created;
                if (lastEdited) {
                    timeStr += "<br>Last edited: " + lastEdited;
                }
                $time.html(timeStr);
                $profile.append($time);

                if (desc) {
                    $profile.append('<p>' + desc + '</p>');
                }
                // ***

                $("#profiles-modal-service-meta").append($profile);
                profCount++;
            }
        }
    });
}

function moderateModals() {
    // Check if catalog modal has been destroyed.
    if (typeof $catModal.iziModal('getState') === "object") {
        // Check if it requires reconstruction.
        if (catCount > 0) {
            $catModal.iziModal(catConfig);
        }
    } else {
        // Check if it requires destruction.
        if (catCount === 0) {
            $catModal.iziModal('destroy');
        }
    }
}

function isJSONString(str) {
    try {
        JSON.parse(str);
        return true;
    } catch (e) {
        return false;
    }
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
        setTimeout(function () {
            reloadLogs();
            reloadModals();
            loadSystemHealthCheck();
            refreshSync(refreshed, timerSetting);
        }, 500);
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
