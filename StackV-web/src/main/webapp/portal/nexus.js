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
import React from "react";
import ReactDOM from "react-dom";

/* Pages */
import Portal from "./portal";
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
        if (window.location.pathname === "/StackV-web/portal/") {
            ReactDOM.render(React.createElement(Portal, null), document.getElementById("root"));
        } else if (window.location.pathname === "/StackV-web/portal/intent/") {
            loadIntent(getURLParameter("intent"));
        }
    };
});

/* UTILITY */
export function prettyPrintInfo() {
    var ugly = document.getElementById("profile-details-modal-text-area").value;
    var obj = JSON.parse(ugly);
    var pretty = JSON.stringify(obj, undefined, 4);
    document.getElementById("profile-details-modal-text-area").value = pretty;
}
export function getURLParameter(name) {
    return decodeURIComponent((new RegExp("[?|&]" + name + "=" + "([^&;]+?)(&|#|;|$)").exec(location.search) || [null, ""])[1].replace(/\+/g, "%20")) || null;
}