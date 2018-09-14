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

/* global XDomainRequest, Power2, TweenLite, iziToast */
import Mousetrap from "mousetrap";

import { prettyPrintInfo, keycloak } from "./nexus";
import { initRefresh, reloadData } from "./refresh";
import { loadInstanceDataTable } from "./logging";

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

var tweenInstancePanel = new TweenLite("#instance-panel", .75, { ease: Power2.easeInOut, paused: true, autoAlpha: 1 });

var $catModal = $("#catalog-modal");
var $profModal = $("#profiles-modal");
var $detailsModal = $("#profile-details-modal");
var $licenseModal = $("#profile-license-modal");

var $alertModal = $("#alert-modal");

/*ReactDOM.render(
    React.createElement(ButtonPanel, { test: "yes" }, null),
    document.getElementById("test-id")
);*/

function toggleModal(modalName) {
    switch (modalName) {
        case "catalog":
            // Toggle catalog modal
            switch ($catModal.iziModal("getState")) {
                case "opened":
                    $catModal.iziModal("close");
                    break;
                case "closed":
                    switch ($profModal.iziModal("getState")) {
                        case "closed":
                            $catModal.iziModal("open");
                            break;
                        case "opened":
                            $profModal.iziModal("prev");
                            break;
                    }
                    break;
                case "opening":
                case "closing":
                    break;
                default:
                    switch ($profModal.iziModal("getState")) {
                        case "closed":
                            $profModal.iziModal("open");
                            break;
                        case "opened":
                            $profModal.iziModal("close");
                            break;
                    }
                    break;
            }
            break;
        case "profile":
            // Toggle profile modal
            switch ($catModal.iziModal("getState")) {
                case "opened":
                    $catModal.iziModal("next");
                    break;
                case "opening":
                case "closing":
                    break;
                case "closed":
                default:
                    switch ($profModal.iziModal("getState")) {
                        case "closed":
                            $profModal.iziModal("open");
                            break;
                        case "opened":
                            $profModal.iziModal("close");
                            break;
                    }
                    break;
            }
            break;
    }
}

export function loadCatalog() {
    Mousetrap.bind("shift+left", function () { window.location.href = "/StackV-web/orch/graphTest.jsp"; });
    Mousetrap.bind("shift+right", function () { window.location.href = "/StackV-web/portal/details/"; });
    Mousetrap.bind("space", function () { toggleModal("catalog"); });
    Mousetrap.bind("shift+space", function () { toggleModal("profile"); });

    $("#sub-nav").load("/StackV-web/nav/catalog_navbar.html", function () {
        initRefresh($("#refresh-timer").val());
    });

    loadInstances();
    loadModals();

    tweenInstancePanel.play();

    $(".button-service-create").click(function (evt) {
        evt.preventDefault();

        toggleModal("catalog");
    });
}

function loadInstances() {
    var apiUrl = window.location.origin + "/StackV-web/restapi/app/logging/instances";
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
var alertConfig = {
    title: "Error",
    icon: "icon-power_settings_new",
    headerColor: "#BD5B5B",
    width: 600,
    timeout: 15000,
    timeoutProgressbar: true,
    transitionIn: "fadeInDown",
    transitionOut: "fadeOutDown",
    pauseOnHover: true
};
var detailsConfig = {
    width: 800,
    headerColor: "#85ac97",
    onClosed: function () {
        $profModal.iziModal("open");
    }
};
var licenseConfig = {
    width: 400,
    headerColor: "#e7c642"
};

var oldProfileName, oldProfileDescription;
function loadModals() {
    // Initialize
    $catModal.html("<div class=\"catalog-modal-body\">" +
        "<p class=\"catalog-modal-body-header\">Select a service type:</p>" +
        "<div id=\"catalog-modal-service-meta\" class=\"list-group\" style=\"cursor: pointer;\"></div>" +
        "<hr><button class=\"button-catalog-modal-switch btn btn-primary\" data-izimodal-open=\"#profiles-modal\">Load Saved Profile</button>" +
        "</div>");
    $profModal.html("<div class=\"profiles-modal-body\">" +
        "<p class=\"profiles-modal-body-header\">Select a saved service profile:</p>" +
        "<div id=\"profiles-modal-service-meta\" class=\"list-group\" style=\"cursor: pointer;\"></div>" +
        "<hr><button class=\"btn btn-primary\" data-izimodal-open=\"#catalog-modal\">Return to Service Catalog</button><button id=\"button-profile-blank-add\" class=\"btn btn-default hidden\" style=\"margin-left: 10px;\">Add Blank Profile</button><input class=\"form-control\" type=\"text\" id=\"profileBlankName\">" +
        "</div>");
    $detailsModal.html("<div style=\"height: 80vh;\" class=\"profile-details-modal-body\">" +
        "<div id=\"profile-details-modal-meta\"><div><div class=\"profile-details-modal-meta-name\"><input class=\"form-control hidden\" id=\"profileEditName\" placeholder=\"New Name\"><p class=\"profile-details-modal-meta-name-text\"></p><button class=\"btn btn-default btn-xs hidden button-profile-meta-edit\"><span class=\"glyphicon glyphicon-pencil\" aria-hidden=\"true\"></span></button></div><div class=\"profile-details-modal-meta-description\"><textarea class=\"form-control hidden\" id=\"profileEditDescription\" placeholder=\"New Description\"></textarea><p class=\"profile-details-modal-meta-description-text\"></p></div><p class=\"profile-details-modal-meta-author\"></p></div><hr>" +
        "<div style=\"padding-right:10px;\" class=\"panel-group profile-details-modal-meta-sharing hidden\"><div class=\"panel panel-default\"><div class=\"panel-heading\"><h4 class=\"panel-title\"><a data-toggle=\"collapse\" href=\"#sharing-collapse\" class=\"\" aria-expanded=\"true\">Profile Sharing</a></h4></div><div id=\"sharing-collapse\" class=\"panel-collapse collapse in\" aria-expanded=\"true\" style=\"\"><ul class=\"list-group profile-details-modal-meta-sharing-list\"></ul><div class=\"panel-footer\" style=\"height:85px;\"><button class=\"button-profile-license-new btn-sm btn btn-default\">Add New User</button><div><label class=\"profile-details-modal-meta-editable control-label\">Allow Editing<input type=\"checkbox\" style=\"margin-left: 10px;\" id=\"profileEditable\" checked=\"checked\" value=\"on\"></label></div></div></div></div></div>" +
        "<div style=\"padding-right:10px;\" class=\"panel-group profile-details-modal-meta-saving\"><div class=\"panel panel-default\"><div class=\"panel-heading\"><h4 class=\"panel-title\"><a data-toggle=\"collapse\" href=\"#saving-collapse\" class=\"\" aria-expanded=\"true\">Save As</a></h4></div><div id=\"saving-collapse\" class=\"panel-collapse collapse\" aria-expanded=\"false\" style=\"\"><form class=\"form-horizontal\"><input type=\"text\" class=\"form-control\" id=\"savingProfileName\" placeholder=\"Profile Name\" style=\"/* margin: auto; *//* width: 90%; *//* margin-top: 5px; *//* margin-bottom: 5px; */\"><input type=\"text\" class=\"form-control\" id=\"savingProfileDescription\" placeholder=\"Profile Description\" style=\"/* margin: auto; *//* width: 90%; *//* margin-top: 5px; *//* margin-bottom: 5px; */\"></form><div class=\"panel-footer\"><button class=\"button-profile-save-new btn-sm btn btn-default\">Save New Profile</button></div></div></div></div>" +
        "<div class=\"profile-details-modal-meta-buttons\"><p class=\"hidden read-only-flag\" style=\"color: #ff5f5f;font-size: 1.25em;padding-right: 50px;\">Read Only</p><button style=\"display: none;\" class=\"button-profile-delete btn btn-danger\">Delete</button><button class=\"button-profile-save btn btn-default\">Save</button><input id=\"profile-alias\" placeholder=\"Instance Alias\"><button class=\"button-profile-submit btn btn-default\">Submit</button></div></div>" +
        "<div id=\"profile-details-modal-text\"><textarea readonly id=\"profile-details-modal-text-area\"></textarea></div>" +
        "</div>");
    $licenseModal.html("<div id=\"profile-license-modal-body\" style=\"margin-bottom: 20px;padding: 15px;\"><form class=\"form-horizontal\"><div class=\"form-group profile-license-modal-username\"><label class=\"col-sm-2 control-label\">Username</label>" +
        "<div class=\"col-sm-10 profile-license-modal-username-div\"></div></div><div class=\"form-group\"><label for=\"licenseRemaining\" class=\"col-sm-2 control-label\">Licenses</label><div class=\"col-sm-4\"><input type=\"number\" class=\"form-control\" id=\"licenseRemaining\"  style=\"width: 80%;margin-left: 25px;\"></div></div>" +
        "<div class=\"form-group\"><label for=\"inputPassword\" class=\"col-sm-2 control-label\">Type</label><div class=\"col-sm-10\"><label class=\"radio-inline\" style=\"float: left;margin-left: 25px;\"><input type=\"radio\" name=\"licenseProfileType\" id=\"ticketRadio\" value=\"ticket\" checked=\"\">Tickets</label><label class=\"radio-inline\" style=\"float: left;\"><input type=\"radio\" name=\"licenseProfileType\" id=\"allocationRadio\" value=\"allocation\">Allocations</label></div></div>" +
        "<div class=\"form-group\"><button class=\"button-license-delete hidden btn btn-warning\" style=\"padding-right: 25;\">Remove</button><button class=\"button-license-update hidden btn btn-warning\" style=\"padding-right: 25;\">Update</button><button class=\"button-license-add hidden btn btn-warning\" style=\"padding-right: 25;\">Submit</button></div></form></div>");

    $catModal.iziModal(catConfig);
    $profModal.iziModal(profConfig);
    $alertModal.iziModal(alertConfig);
    $detailsModal.iziModal(detailsConfig);
    $licenseModal.iziModal(licenseConfig);

    // Load service metadata.
    var apiUrl = window.location.origin + "/StackV-web/restapi/app/panel/editor";
    $.ajax({
        url: apiUrl,
        type: "GET",
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
        },
        success: function (result) {
            for (let i = 0; i < result.length; i++) {
                var meta = result[i];

                var name = meta[0];
                var desc = meta[1];
                var tag = meta[2];

                var $service = $("<a></a>");
                $service.addClass("list-group-item list-group-item-action flex-column align-items-start");
                $service.attr("data-tag", tag);

                $service.append("<h4 style=\"display: inline-block;\">" + name + "</h4>");
                if (desc) {
                    $service.append("<p>" + desc + "</p>");
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
    apiUrl = window.location.origin + "/StackV-web/restapi/app/panel/wizard";
    $.ajax({
        url: apiUrl,
        type: "GET",
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
        },
        success: function (result) {
            if (keycloak.tokenParsed.realm_access.roles.includes("F_Profiles-W")) {
                $("#button-profile-blank-add").removeClass("hidden");
            }

            for (let i = 0; i < result.length; i++) {
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

                var $profile = $("<a></a>");
                $profile.addClass("list-group-item list-group-item-action flex-column align-items-start");
                $profile.attr("data-id", id);

                $profile.append("<h4 style=\"display: inline-block;\">" + name + "</h4>");

                // Properties
                var $note = $("<small></small>");
                if (owner !== keycloak.tokenParsed.preferred_username) {
                    $note.css({ "color": "#777", "padding": "5px" });
                    $note.text("created by " + owner + " ");
                    $profile.append($note);
                    if (editable === "0") {
                        $profile.css("box-shadow", "inset 0px 0px 2px 0px #ff5f5f");
                        $note.text($note.text() + "(Read only)");
                    }
                }

                var $time = $("<small></small>");
                $time.css({ "float": "right", "text-align": "right", "padding-top": "10px" });
                var timeStr = "Created: " + created;
                if (lastEdited) {
                    timeStr += "<br>Last edited: " + lastEdited;
                }
                $time.html(timeStr);
                $profile.append($time);

                if (desc) {
                    $profile.append("<p>" + desc + "</p>");
                }
                // ***

                $("#profiles-modal-service-meta").append($profile);
                profCount++;
            }

            $("#button-profile-blank-add").click(function () {
                if ($("#profileBlankName").hasClass("opened")) {
                    // Save to DB
                    var apiUrl = window.location.origin + "/StackV-web/restapi/app/profile/new";
                    $.ajax({
                        url: apiUrl,
                        type: "PUT",
                        data: JSON.stringify({
                            "name": $("#profileBlankName").val(),
                            "username": keycloak.tokenParsed.preferred_username,
                            "data": {}
                        }),
                        beforeSend: function (xhr) {
                            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
                        },
                        success: function () {
                            $("#profileBlankName").removeClass("opened");
                            $("#button-profile-blank-add").text("Add Blank Profile");
                            reloadModals();
                        }
                    });
                } else {
                    $("#profileBlankName").addClass("opened");
                    $(this).text("Submit");
                }
            });

            $("#profiles-modal-service-meta").on("click", "a", function (evt) {
                var profileID = $(this).data("id");
                $profModal.attr("data-profile-id", profileID);
                var apiUrl = window.location.origin + "/StackV-web/restapi/app/profile/" + profileID;
                $.ajax({
                    url: apiUrl,
                    type: "GET",
                    beforeSend: function (xhr) {
                        xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
                    },
                    success: function (result) {
                        resetProfileModal();

                        var $textArea = $("#profile-details-modal-text-area");
                        $(".profile-details-modal-meta-name-text").text(result["name"]);
                        $(".profile-details-modal-meta-description-text").text(result["description"]);

                        if (result["owner"] === keycloak.tokenParsed.preferred_username
                            || result["editable"] === "1") {
                            $(".profile-details-modal-meta-saving").removeClass("hidden");
                            $textArea.removeAttr("readonly");
                            $(".button-profile-save").removeAttr("disabled");
                            $(".button-profile-save-as").removeAttr("disabled");
                            $(".read-only-flag").addClass("hidden");
                        } else {
                            $textArea.attr("readonly", true);
                            $(".button-profile-save").attr("disabled", true);
                            $(".button-profile-save-as").attr("disabled", true);
                            $(".read-only-flag").removeClass("hidden");
                        }

                        $textArea.val(result["wizard_json"]);
                        originalProfile = result["wizard_json"];
                        $(".button-profile-save").attr("id", profileID);
                        $(".button-profile-save-as").attr("id", profileID);
                        $(".button-profile-submit").attr("id", profileID);

                        // Owner of profile
                        if (result["owner"] === keycloak.tokenParsed.preferred_username) {
                            $(".button-profile-meta-edit").removeClass("hidden");
                            $(".profile-details-modal-meta-sharing").removeClass("hidden");
                            for (var i in result["licenses"]) {
                                var license = result["licenses"][i];

                                if (license["type"] === "ticket") {
                                    var $opt = $("<li class=\"list-group-item license-" + license["type"] + "\">"
                                        + "<p style=\"display: inline;\">" + license["username"]
                                        + "</p><p style=\"display: inline;float: right;color: #777777;font-size: .9em;\" data-remaining=\"" + license["remaining"] + "\">" + license["remaining"] + " use(s)</p></li>");
                                    $(".profile-details-modal-meta-sharing-list").append($opt);
                                } else {
                                    var apiUrl = window.location.origin + "/StackV-web/restapi/app/profile/" + profileID + "/uses/" + license["username"];
                                    $.ajax({
                                        url: apiUrl,
                                        type: "GET",
                                        async: false,
                                        contentType: "application/json; charset=utf-8",
                                        success: function (result) {
                                            var $opt = $("<li class=\"list-group-item license-" + license["type"] + "\">"
                                                + "<p style=\"display: inline;\">" + license["username"]
                                                + "</p><p id=\"" + license["username"] + "-slots-used\" style=\"display: inline;float: right;color: #777777;font-size: .9em;\" data-remaining=\"" + license["remaining"] + "\">" + result + "/" + license["remaining"] + " slot(s)</p></li>");
                                            $(".profile-details-modal-meta-sharing-list").append($opt);
                                        }
                                    });
                                }
                            }

                            $(".button-profile-delete").show();

                            $(".profile-details-modal-meta-editable").show();
                            if (result["editable"] === "1") {
                                $("#profileEditable").prop("checked", true);
                            }
                        }
                        // Licensee of profile
                        else {
                            $(".profile-details-modal-meta-sharing").addClass("hidden");
                            var remaining = 1;
                            var type = "ticket";
                            for (let i in result["licenses"]) {
                                let license = result["licenses"][i];
                                if (license["username"] === keycloak.tokenParsed.preferred_username) {
                                    remaining = license["remaining"];
                                    type = license["type"];
                                }
                            }

                            var metaText = "Created by " + result["owner"] + ".<br>";
                            if (type === "ticket") {
                                if (remaining > 1) {
                                    metaText += remaining + " uses remaining.";
                                } else {
                                    metaText += remaining + " use remaining.";
                                }
                                $(".profile-details-modal-meta-author").html(metaText);
                            } else {
                                let apiUrl = window.location.origin + "/StackV-web/restapi/app/profile/" + profileID + "/uses/" + keycloak.tokenParsed.preferred_username;
                                $.ajax({
                                    url: apiUrl,
                                    type: "GET",
                                    contentType: "application/json; charset=utf-8",
                                    success: function (result) {
                                        var used = result;
                                        metaText += "Using " + used + " out of " + remaining + " slots.";

                                        if (used >= remaining) {
                                            $(".button-profile-submit").attr("disabled", true);
                                            $(".profile-details-modal-meta-author").addClass("invalid");
                                        }
                                        $(".profile-details-modal-meta-author").html(metaText);
                                    }
                                });
                            }
                        }

                        prettyPrintInfo();

                        $profModal.iziModal("close");
                        $detailsModal.iziModal("open");
                    },
                    error: function (textStatus, errorThrown) {
                        console.log(textStatus);
                        console.log(errorThrown);
                    }
                });

                evt.preventDefault();
            });

            $(".button-profile-license-new").click(function (evt) {
                resetLicenseModal();

                var $select = $("<select class=\"form-control\" id=\"licenseUsername\" style=\"width: 70%;margin-left: 25px;\"></select>");

                var apiUrl = window.location.origin + "/StackV-web/restapi/app/keycloak/users";
                $.ajax({
                    url: apiUrl,
                    type: "GET",
                    contentType: "application/json; charset=utf-8",
                    success: function (result) {
                        var $existingArr = $(".profile-details-modal-meta-sharing-list li p:first-child");
                        for (var i in result) {
                            var existing = false;
                            var user = result[i][0];
                            $existingArr.each(function (i, val) {
                                if (val.innerText === user) {
                                    existing = true;
                                }
                            });
                            if (!existing) {
                                var $opt = $("<option>");
                                $opt.val(user).text(user);
                                $select.append($opt);
                            }
                        }
                        $(".profile-license-modal-username-div").append($select);
                        $(".button-license-add").removeClass("hidden");
                        $licenseModal.iziModal("open");
                    }
                });

                evt.preventDefault();
            });

            $(".profile-details-modal-meta-sharing-list").on("click", "li", function (evt) {
                resetLicenseModal();

                $(".profile-license-modal-username-div").append("<p class=\"form-control-static\">" + $(this).children()[0].innerHTML + "</p>");
                $("#licenseRemaining").val($($(this).children()[1]).data("remaining"));

                $(".button-license-update").removeClass("hidden");
                $(".button-license-delete").removeClass("hidden");

                if ($(this).hasClass("license-allocation")) {
                    $("#allocationRadio").prop("checked", true);
                } else {
                    $("#ticketRadio").prop("checked", true);
                }

                $licenseModal.iziModal("open");

                evt.preventDefault();
            });

            $("#profile-details-modal-meta").on("click", ".button-profile-meta-edit", function () {
                var $span = $(this.children[0]);
                if ($span.hasClass("glyphicon-pencil")) {
                    oldProfileName = $(".profile-details-modal-meta-name-text").text();
                    oldProfileDescription = $(".profile-details-modal-meta-description-text").text();

                    $("#profileEditName").val(oldProfileName);
                    $("#profileEditDescription").val(oldProfileDescription);

                    $(".profile-details-modal-meta-name-text").text(null);
                    $(".profile-details-modal-meta-description-text").text(null);

                    $("#profileEditName").removeClass("hidden");
                    $("#profileEditDescription").removeClass("hidden");
                    $span.removeClass("glyphicon-pencil").addClass("glyphicon-ok");
                } else {
                    var apiUrl = window.location.origin + "/StackV-web/restapi/app/profile/" + $(".button-profile-save").attr("id") + "/meta";
                    $.ajax({
                        url: apiUrl,
                        type: "PUT",
                        data: JSON.stringify({
                            "name": $("#profileEditName").val(),
                            "description": $("#profileEditDescription").val()
                        }),
                        contentType: "application/json; charset=utf-8",
                        success: function () {
                            $(".profile-details-modal-meta-name-text").text($("#profileEditName").val());
                            $(".profile-details-modal-meta-description-text").text($("#profileEditDescription").val());

                            $("#profileEditName").val(null);
                            $("#profileEditDescription").val(null);

                            $("#profileEditName").addClass("hidden");
                            $("#profileEditDescription").addClass("hidden");
                            $span.addClass("glyphicon-pencil").removeClass("glyphicon-ok");
                        }, error: function () {
                            $(".profile-details-modal-meta-name-text").text(oldProfileName);
                            $(".profile-details-modal-meta-description-text").text(oldProfileDescription);

                            $("#profileEditName").val(null);
                            $("#profileEditDescription").val(null);

                            $("#profileEditName").addClass("hidden");
                            $("#profileEditDescription").addClass("hidden");
                            $span.addClass("glyphicon-pencil").removeClass("glyphicon-ok");
                        }
                    });
                }
            });

            $(".button-license-add").on("click", function (evt) {
                var apiUrl = window.location.origin + "/StackV-web/restapi/app/profile/" + $(".button-profile-save").attr("id") + "/licenses";
                $.ajax({
                    url: apiUrl,
                    type: "POST",
                    data: JSON.stringify({
                        "username": $("#licenseUsername").val(),
                        "type": $("input[name=licenseProfileType]:checked").val(),
                        "remaining": $("#licenseRemaining").val()
                    }),
                    contentType: "application/json; charset=utf-8",
                    success: function () {
                        reloadModals();
                        $licenseModal.iziModal("close");
                    }
                });

                evt.preventDefault();
            });

            $(".button-license-update").on("click", function (evt) {
                var apiUrl = window.location.origin + "/StackV-web/restapi/app/profile/" + $(".button-profile-save").attr("id") + "/licenses";
                $.ajax({
                    url: apiUrl,
                    type: "PUT",
                    data: JSON.stringify({
                        "username": $(".profile-license-modal-username-div p")[0].innerHTML,
                        "type": $("input[name=licenseProfileType]:checked").val(),
                        "remaining": $("#licenseRemaining").val()
                    }),
                    contentType: "application/json; charset=utf-8",
                    success: function () {
                        reloadModals();
                        $licenseModal.iziModal("close");
                    }
                });

                evt.preventDefault();
            });

            $(".button-license-delete").on("click", function (evt) {
                var apiUrl = window.location.origin + "/StackV-web/restapi/app/profile/" + $(".button-profile-save").attr("id") + "/licenses";
                $.ajax({
                    url: apiUrl,
                    type: "PUT",
                    data: JSON.stringify({
                        "username": $(".profile-license-modal-username-div p")[0].innerHTML,
                        "remaining": "0"
                    }),
                    contentType: "application/json; charset=utf-8",
                    success: function () {
                        reloadModals();
                        $licenseModal.iziModal("close");
                    }
                });

                evt.preventDefault();
            });

            // Legacy modal listeners.
            $(".button-profile-delete").on("click", function (evt) {
                var apiUrl = window.location.origin + "/StackV-web/restapi/app/profile/" + $(".button-profile-save").attr("id");
                $.ajax({
                    url: apiUrl,
                    type: "DELETE",
                    beforeSend: function (xhr) {
                        xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
                    },
                    success: function () {
                        $profModal.removeAttr("data-profile-id");
                        reloadModals();
                        $detailsModal.iziModal("close");
                    },
                    error: function (textStatus, errorThrown) {
                        console.log(textStatus);
                        console.log(errorThrown);
                    }
                });

                evt.preventDefault();
            });

            $(".button-profile-submit").on("click", function (evt) {
                if ($("#profile-alias").val()) {
                    var profile = JSON.parse($("#profile-details-modal-text-area").val());
                    profile["alias"] = $("#profile-alias").val();

                    var apiUrl = window.location.origin + "/StackV-web/restapi/app/service/uuid";
                    $.ajax({
                        url: apiUrl,
                        async: false,
                        type: "GET",
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
                            manifest["profileID"] = $(".button-profile-submit").attr("id");

                            manifest["proceed"] = "true";
                            var apiUrl = window.location.origin + "/StackV-web/restapi/app/profile";
                            $.ajax({
                                url: apiUrl,
                                type: "POST",
                                data: JSON.stringify(manifest),
                                contentType: "application/json; charset=utf-8",
                                dataType: "json",
                                beforeSend: function (xhr) {
                                    xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
                                    xhr.setRequestHeader("Refresh", keycloak.refreshToken);
                                },
                                success: function (result) {
                                    $profModal.iziModal("close");
                                },
                                error: function (jqXHR, textStatus, errorThrown) {
                                    console.log(jqXHR.status + " | " + textStatus + " | " + errorThrown);

                                    if (jqXHR.status === 401) {
                                        $alertModal.iziModal("setSubtitle", "You are not authorized for the service associated with this profile.");
                                        $alertModal.iziModal("setTop", 100);
                                        $alertModal.iziModal("open");
                                    }
                                }
                            });
                        }
                    });
                    // reload top table and hide modal
                    reloadData();
                    $detailsModal.iziModal("close");
                    evt.preventDefault();
                } else {
                    $("#profile-alias").addClass("invalid");
                    $("#profile-alias").change(function () {
                        $(this).removeClass("invalid");
                    });
                }
            });

            // After the user has put a new name and description for the new profile
            $(".button-profile-save-new").on("click", function (evt) {
                var profileString = $("#profile-details-modal-text-area").val();
                if (isJSONString(profileString)) {
                    var apiUrl = window.location.origin + "/StackV-web/restapi/app/profile/new";
                    var data = {
                        name: $("#savingProfileName").val(),
                        username: keycloak.tokenParsed.preferred_username,
                        description: $("#savingProfileDescription").val(),
                        data: JSON.parse($("#profile-details-modal-text-area").val())
                    };

                    $.ajax({
                        url: apiUrl,
                        type: "PUT",
                        data: JSON.stringify(data), //stringify to get escaped JSON in backend
                        contentType: "application/json; charset=utf-8",
                        dataType: "json",
                        beforeSend: function (xhr) {
                            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
                            xhr.setRequestHeader("Refresh", keycloak.refreshToken);
                        },
                        success: function () {
                            reloadData();
                            $detailsModal.iziModal("close");
                        },
                        error: function (textStatus, errorThrown) {
                            console.log(textStatus);
                            console.log(errorThrown);
                        }
                    });

                    evt.preventDefault();
                }
            });

            $(".button-profile-save").on("click", function (evt) {
                var profileString = $("#profile-details-modal-text-area").val();
                if (isJSONString(profileString)) {
                    var apiUrl = window.location.origin + "/StackV-web/restapi/app/profile/" + this.id + "/edit";

                    $.ajax({
                        url: apiUrl,
                        type: "PUT",
                        data: JSON.stringify({
                            "data": profileString,
                            "editable": $("#profileEditable").prop("checked")
                        }),
                        contentType: "application/json; charset=utf-8",
                        dataType: "json",
                        beforeSend: function (xhr) {
                            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
                            xhr.setRequestHeader("Refresh", keycloak.refreshToken);
                        },
                        success: function () {
                            iziToast.success({
                                timeout: 3000,
                                title: "OK",
                                message: "Profile saved!",
                                position: "topRight",
                                pauseOnHover: false
                            });
                        },
                        error: function (textStatus, errorThrown) {
                            console.log(textStatus);
                            console.log(errorThrown);
                        }
                    });

                    $("#info-panel").removeClass("active");
                    evt.preventDefault();
                }
            });
        }
    });
}
function resetProfileModal() {
    clearTimeout(profileUpdateTimeout);
    $(".button-profile-meta-edit").addClass("hidden");
    $(".profile-details-modal-meta-saving").addClass("hidden");
    $(".profile-details-modal-meta-sharing").addClass("hidden");

    $(".profile-details-modal-meta-sharing-list").empty();
    $("#info-panel-share-edit :not(:disabled)").remove();
    $(".profile-details-modal-meta-author").removeClass("invalid");
    $(".button-profile-submit").removeAttr("disabled");

    $("#profile-alias").val(null);
    $("#profile-details-modal-text-area").val(null);
    $("#info-panel-share-remaining").val(null);
    $("#savingProfileName").val(null);
    $("#savingProfileDescription").val(null);

    $profModal.removeData("profile-id");

    $("#profileEditable").prop("checked", false);
    $("#profile-details-modal-meta-text").html(null);

    $(".profile-details-modal-meta-name-text").text(null);
    $(".profile-details-modal-meta-description-text").text(null);
    $(".profile-details-modal-meta-author").text(null);

    $(".profile-details-modal-meta-editable").hide();
    $("#info-panel-management").hide();
    $(".button-profile-delete").hide();
}

function resetLicenseModal() {
    $(".profile-license-modal-username-div").empty();
    $("#licenseRemaining").val(null);

    $licenseModal.find("button").addClass("hidden");
}

export function reloadModals() {
    if (!keycloak.tokenParsed.realm_access.roles.includes("F_Profiles-W")) {
        $("#button-profile-blank-add").addClass("hidden");
    }

    // Load service metadata.
    catCount = 0, profCount = 0;

    var apiUrl = window.location.origin + "/StackV-web/restapi/app/panel/editor";
    $.ajax({
        url: apiUrl,
        type: "GET",
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
        },
        success: function (result) {
            $("#catalog-modal-service-meta").empty();
            for (let i = 0; i < result.length; i++) {
                var meta = result[i];

                var name = meta[0];
                var desc = meta[1];
                var tag = meta[2];

                var $service = $("<a></a>");
                $service.addClass("list-group-item list-group-item-action flex-column align-items-start");
                $service.attr("data-tag", tag);

                $service.append("<h4 style=\"display: inline-block;\">" + name + "</h4>");
                if (desc) {
                    $service.append("<p>" + desc + "</p>");
                }

                $("#catalog-modal-service-meta").append($service);
                catCount++;
            }
        }
    });

    // Load service profiles.
    apiUrl = window.location.origin + "/StackV-web/restapi/app/panel/wizard";
    $.ajax({
        url: apiUrl,
        type: "GET",
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
        },
        success: function (result) {
            $("#profiles-modal-service-meta").empty();
            for (let i = 0; i < result.length; i++) {
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

                var $profile = $("<a></a>");
                $profile.addClass("list-group-item list-group-item-action flex-column align-items-start");
                $profile.attr("data-id", id);

                $profile.append("<h4 style=\"display: inline-block;\">" + name + "</h4>");

                // Properties
                var $note = $("<small></small>");
                if (owner !== keycloak.tokenParsed.preferred_username) {
                    $note.css({ "color": "#777", "padding": "5px" });
                    $note.text("created by " + owner + " ");
                    $profile.append($note);
                    if (editable === "0") {
                        $profile.css("box-shadow", "inset 0px 0px 2px 0px #ff5f5f");
                        $note.text($note.text() + "(Read only)");
                    }
                }

                var $time = $("<small></small>");
                $time.css({ "float": "right", "text-align": "right", "padding-top": "10px" });
                var timeStr = "Created: " + created;
                if (lastEdited) {
                    timeStr += "<br>Last edited: " + lastEdited;
                }
                $time.html(timeStr);
                $profile.append($time);

                if (desc) {
                    $profile.append("<p>" + desc + "</p>");
                }
                // ***

                $("#profiles-modal-service-meta").append($profile);
                profCount++;
            }
        }
    });

    // Reload open profile
    if ($profModal.data("profile-id")) {
        var profileID = $profModal.data("profile-id");
        let apiUrl = window.location.origin + "/StackV-web/restapi/app/profile/" + profileID;
        $.ajax({
            url: apiUrl,
            type: "GET",
            async: false,
            beforeSend: function (xhr) {
                xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
            },
            success: function (result) {
                if (!$(".profile-details-modal-meta-sharing").hasClass("hidden")) {
                    $(".profile-details-modal-meta-sharing-list").empty();
                    for (var i in result["licenses"]) {
                        var license = result["licenses"][i];

                        if (license["type"] === "ticket") {
                            let $opt = $("<li class=\"list-group-item license-" + license["type"] + "\">"
                                + "<p style=\"display: inline;\">" + license["username"]
                                + "</p><p style=\"display: inline;float: right;color: #777777;font-size: .9em;\" data-remaining=\"" + license["remaining"] + "\">" + license["remaining"] + " use(s)</p></li>");
                            $(".profile-details-modal-meta-sharing-list").append($opt);
                        } else {
                            let $opt = $("<li class=\"list-group-item license-" + license["type"] + "\">"
                                + "<p style=\"display: inline;\">" + license["username"]
                                + "</p><p id=\"" + license["username"] + "-slots-used\" style=\"display: inline;float: right;color: #777777;font-size: .9em;\" data-remaining=\"" + license["remaining"] + "\">" + "/" + license["remaining"] + " slot(s)</p></li>");
                            $(".profile-details-modal-meta-sharing-list").append($opt);

                            var apiUrl = window.location.origin + "/StackV-web/restapi/app/profile/" + profileID + "/uses/" + license["username"];
                            $.ajax({
                                url: apiUrl,
                                type: "GET",
                                async: false,
                                contentType: "application/json; charset=utf-8",
                                success: function (result) {
                                    var $slot = $("#" + license["username"] + "-slots-used");
                                    $slot.text(result + $slot.text());
                                }
                            });
                        }
                    }
                }
            },
            error: function (textStatus, errorThrown) {
                console.log(textStatus);
                console.log(errorThrown);
            }
        });


    }
}

function moderateModals() {
    // Check if catalog modal has been destroyed.
    if (typeof $catModal.iziModal("getState") === "object") {
        // Check if it requires reconstruction.
        if (catCount > 0) {
            $catModal.iziModal(catConfig);
        }
    } else {
        // Check if it requires destruction.
        if (catCount === 0) {
            $catModal.iziModal("destroy");
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
    return decodeURIComponent((new RegExp("[?|&]" + name + "=" + "([^&;]+?)(&|#|;|$)").exec(location.search) || [null, ""])[1].replace(/\+/g, "%20")) || null;
}
