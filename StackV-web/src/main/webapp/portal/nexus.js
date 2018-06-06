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
/* global XDomainRequest, Keycloak, window.location.origin, loggedIn, TweenLite, Power2, tweenBlackScreen */
// Service JavaScript Library
import $ from "jquery";
import "bootstrap";

import { loadClipbook } from "./clipbook";

/* Pages */
import { loadAdmin } from "./admin/admin";
import { loadACLPortal } from "./acl/acl";
import { loadCatalog } from "./catalog";
import { loadDetails } from "./details/details";
import { loadDriverPortal } from "./driver/driver";
import { loadIntent } from "./intent/intentEngine";
/* */

export var page;
export var keycloak = Keycloak({
    "realm": "StackV",
    "realm-public-key": "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAukpMOpeNbu+lfAdcKJRk00Fxln6lvA3RGoEv+BE/cjbbjvg6Rsr1p94XFKuHifx3Kmngtd00XEnyDxg5ODHFrtXi+z1DYbx4m3ajkZSVaWXwkOqnPPC327PHvTgd2Tf475lW0yR01iZMDLyjjERbkps3guiqB6gnMSiuqtEBFSekCXCtkxYrFl8RFFAVfzAW5lRXvySO50gQHGUvV/FevtpgNU6HS3sIa9uitSd+WgqCotZW6u9C3FygnuKt8VNqvNv7MP7hVt0rlMo/yP1OgYB0jBpHf1tvwYMFvz6kEauk1HfbYZCvTT1Yr7AHM5i8NZzwGeK444QAyLrBhT2oNQIDAQAB",
    "auth-server-url": "https://k152.maxgigapop.net:8543/auth",
    "ssl-required": "external",
    "clientId": "StackV",
    "credentials": {
        "secret": "ae53fbea-8812-4c13-918f-0065a1550b7c"
    },
    "use-resource-role-mappings": true
});
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

    switch (window.location.pathname) {
    case "/StackV-web/portal/":
        page = "catalog";
        break;
    case "/StackV-web/portal/admin/":
        page = "admin";
        break;
    case "/StackV-web/portal/acl/":
        page = "acl";
        break;
    case "/StackV-web/portal/driver/":
        page = "driver";
        break;
    case "/StackV-web/portal/details/":
        page = "details";
        break;
    case "/StackV-web/portal/intent/":
        page = "intent";
        break;
    case "/StackV-web/visual/manifest/manifestPortal.jsp":
        page = "manifest";
        break;
    case "/StackV-web/visual/graphTest.jsp":
    case "/StackV-web/visual/":
        page = "visualization";
    }

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
        alert("failed to initialize");
    });
    keycloak.onAuthSuccess = function () {
        window.keycloak = keycloak;

        loadNavbar();

        switch (page) {
        case "catalog":
            $("li#catalog-tab").addClass("active");
            loadCatalog();
            break;
        case "admin":
            $("li#admin-tab").addClass("active");
            loadAdmin();
            break;
        case "acl":
            $("li#acl-tab").addClass("active");
            loadACLPortal();
            break;
        case "driver":
            $("li#driver-tab").addClass("active");
            loadDriverPortal();
            break;
        case "intent":
            loadIntent(getURLParameter("intent"));
            break;
        case "details":
            $("li#details-tab").addClass("active");
            var uuid = sessionStorage.getItem("instance-uuid");
            if (!uuid) {
                alert("No Service Instance Selected!");
                window.location.replace("/StackV-web/");
            } else {
                loadDetails();
            }
            break;
        case "manifest":
            //loadManifest();
            break;
        case "visualization":
            import(/* webpackChunkName: "engine" */ "../visual/engine").then(module => {
                module.default("#vis-panel");
                $("#vis-panel").css("opacity", "1");
            });
            break;
        }

        loadClipbook();

        setInterval(function () {
            keycloak.updateToken(70);
        }, (60000));
    };
    keycloak.onTokenExpire = function () {
        keycloak.updateToken(20).success(function () {
            sessionStorage.setItem("username", keycloak.tokenParsed.preferred_username);
            sessionStorage.setItem("subject", keycloak.tokenParsed.sub);
            sessionStorage.setItem("token", keycloak.token);
            sessionStorage.setItem("refresh", keycloak.refreshToken);
        }).error(function () {
        });
    };

    /*$(".button-group-select").click(function (evt) {
    $ref = "user_groups.jsp?id=" + this.id;
    $ref = $ref.replace("select", "") + " #group-specific";
    // console.log($ref);
    $("#group-specific").load($ref);
    evt.preventDefault();
});*/

    $(".clickable-row").click(function () {
        sessionStorage.setItem("uuid", $(this).data("href"));
        window.document.location = "/StackV-web/ops/details/templateDetails.jsp";
    });

    $(".checkbox-level").click(function (evt) {
        evt.preventDefault();
    });

    clearCounters();
});

export function loadNavbar() {
    $("#nav").load("/StackV-web/nav/navbar.html", function () {
        verifyPageRoles();

        switch (page) {
        case "catalog":
            $("li#catalog-tab").addClass("active");
            break;
        case "admin":
            $("li#admin-tab").addClass("active");
            break;
        case "acl":
            $("li#acl-tab").addClass("active");
            break;
        case "driver":
            $("li#driver-tab").addClass("active");
            break;
        case "details":
            $("li#details-tab").addClass("active");
            break;
        case "visualization":
            $("li#visualization-tab").addClass("active");
            break;
        }

        var apiUrl = window.location.origin + "/StackV-web/restapi/app/logging/";
        $.ajax({
            url: apiUrl,
            type: "GET",
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

export function verifyPageRoles() {
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

    if (getURLParameter("intent")) {
        var intent = getURLParameter("intent").toUpperCase();
    }
    switch (page) {
    case "acl":
        if (keycloak.tokenParsed.realm_access.roles.indexOf("F_ACL-R") === -1) {
            window.location.href = "/StackV-web/portal/";
        }
        break;
    case "admin":
        if (keycloak.tokenParsed.realm_access.roles.indexOf("A_Admin") === -1) {
            window.location.href = "/StackV-web/portal/";
        }
        break;
    case "details":
        if (keycloak.tokenParsed.realm_access.roles.indexOf("A_Admin") === -1) {
            var uuid = sessionStorage.getItem("instance-uuid");
            var apiUrl = window.location.origin + "/StackV-web/restapi/app/access/instances/" + uuid;
            $.ajax({
                url: apiUrl,
                type: "GET",
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
    case "driver":
        if (keycloak.tokenParsed.realm_access.roles.indexOf("F_Drivers-R") === -1) {
            window.location.href = "/StackV-web/portal/";
        }
        break;
    case "intent":
        if (keycloak.tokenParsed.realm_access.roles.indexOf("F_Services-" + intent) === -1) {
            window.location.href = "/StackV-web/portal/";
        }
        break;
    case "visualization":
        if (keycloak.tokenParsed.realm_access.roles.indexOf("F_Visualization-R") === -1) {
            window.location.href = "/StackV-web/portal/";
        }
        break;
    }
}

export function prettyPrintInfo() {
    var ugly = document.getElementById("profile-details-modal-text-area").value;
    var obj = JSON.parse(ugly);
    var pretty = JSON.stringify(obj, undefined, 4);
    document.getElementById("profile-details-modal-text-area").value = pretty;
}

/* API CALLS */

export function checkInstance(uuid) {
    var apiUrl = window.location.origin + "/StackV-web/restapi/service/" + uuid + "/status";
    $.ajax({
        url: apiUrl,
        type: "GET",
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

export function cancelInstance(uuid) {
    var apiUrl = window.location.origin + "/StackV-web/restapi/app/service/" + uuid + "/cancel";
    $.ajax({
        url: apiUrl,
        type: "PUT",
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
        },
        success: function (result) {
            window.location.reload(true);
        }
    });
    //window.location.replace('/StackV-web/');
}
export function forceCancelInstance(uuid) {
    var apiUrl = window.location.origin + "/StackV-web/restapi/app/service/" + uuid + "/force_cancel";
    $.ajax({
        url: apiUrl,
        type: "PUT",
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
        },
        success: function (result) {
            window.location.reload(true);
        }
    });
    //window.location.replace('/StackV-web/');
}

export function reinstateInstance(uuid) {
    var apiUrl = window.location.origin + "/StackV-web/restapi/app/service/" + uuid + "/reinstate";
    $.ajax({
        url: apiUrl,
        type: "PUT",
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
        },
        success: function (result) {
            window.location.reload(true);
        }
    });
    //window.location.replace('/StackV-web/');
}
export function forceReinstateInstance(uuid) {
    var apiUrl = window.location.origin + "/StackV-web/restapi/app/service/" + uuid + "/force_reinstate";
    $.ajax({
        url: apiUrl,
        type: "PUT",
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
        },
        success: function (result) {
            window.location.reload(true);
        }
    });
    //window.location.replace('/StackV-web/');
}

export function forceRetryInstance(uuid) {
    var apiUrl = window.location.origin + "/StackV-web/restapi/app/service/" + uuid + "/force_retry";
    $.ajax({
        url: apiUrl,
        type: "PUT",
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
        },
        success: function (result) {
            window.location.reload(true);
        }
    });
    //window.location.replace('/StackV-web/');
}

export function modifyInstance(uuid) {
    var apiUrl = window.location.origin + "/StackV-web/restapi/app/service/" + uuid + "/modify";
    $.ajax({
        url: apiUrl,
        type: "PUT",
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
        },
        success: function (result) {
            window.location.reload(true);
        }
    });
    //window.location.replace('/StackV-web/');
}

export function forceModifyInstance(uuid) {
    var apiUrl = window.location.origin + "/StackV-web/restapi/app/service/" + uuid + "/force_modify";
    $.ajax({
        url: apiUrl,
        type: "PUT",
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
        },
        success: function (result) {
            window.location.reload(true);
        }
    });
    //window.location.replace('/StackV-web/');
}

export function verifyInstance(uuid) {
    var apiUrl = window.location.origin + "/StackV-web/restapi/app/service/" + uuid + "/verify";
    $.ajax({
        url: apiUrl,
        type: "PUT",
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

export function deleteInstance(uuid) {
    var apiUrl = window.location.origin + "/StackV-web/restapi/app/service/" + uuid + "/delete";
    $.ajax({
        url: apiUrl,
        type: "PUT",
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
        },
        success: function (result) {
            console.log("DELETION SUCCESSFUL");
            window.location.replace("/StackV-web/");
        }
    });
}

/* UTILITY */
export function getURLParameter(name) {
    return decodeURIComponent((new RegExp("[?|&]" + name + "=" + "([^&;]+?)(&|#|;|$)").exec(location.search) || [null, ""])[1].replace(/\+/g, "%20")) || null;
}

// Helper method to parse the title tag from the response.
export function getTitle(text) {
    return text.match("<title>(.*)?</title>")[1];
}

export function clearCounters() {
    fieldCounter = 0;
    queryCounter = 1;
}

export function reloadPage() {
    window.location.reload(true);
}

export function reloadPanel(panelId) {
    $("#" + panelId).load(document.URL + " #" + panelId);
}

export function emptyElement(id) {
    $("#" + id).empty();
}

// Create the XHR object.
export function createCORSRequest(method, url) {
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

export function enableLoading() {
    $("#main-pane").addClass("loading");
}

export function disableLoading() {
    $("#main-pane").removeClass("loading");
}
