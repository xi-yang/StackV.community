/* global keycloak */
import * as d3 from "d3";
import $ from "jquery";
import { initRefresh, reloadData } from "../portal/refresh";
import StackVGraphic from "stackv-visualization";

export default function initVisualization(selector) {
    $("#sub-nav").load("/StackV-web/nav/visualization_navbar.html", function () {
        initRefresh($("#refresh-timer").val());
    });

    /*const $detailsModal = $("#details-modal");
    const detailsConfig = {
        title: "Details",
        headerColor: "#3e4d5f",
        width: "50vh",
        transitionIn: "fadeInDown",
        transitionOut: "fadeOutUp",
        top: "104px",
        overlay: false,
        onClosed: function () {
            window.view.highlighter.hide();
        }
    };
    $detailsModal.iziModal(detailsConfig);*/

    fetchDomains();
    window.d3 = d3;
    window.v = StackVGraphic;

    var apiUrl = window.location.origin + "/StackV-web/restapi/model/refresh/all";
    var ret;
    $.ajax({
        url: apiUrl,
        type: "GET",
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
            xhr.setRequestHeader("Refresh", keycloak.refreshToken);
        },
        success: function (result) {
            console.log(result);
            const view = new StackVGraphic(result, document.querySelector(selector));
            window.data = view.dataModel;
            window.view = view;

            d3.select("#reset-page").on("click", () => {
                view.restart();
            });

            d3.select("#del-state").on("click", () => {
                localStorage.clear();
                window.location.reload();
            });

            $("#refresh-test").click(f => {

            });

            view.restart();
        },
    });
}

function fetchDomains() {
    var domains = [];
    var apiUrl = window.location.origin + "/StackV-web/restapi/driver";
    $.ajax({
        url: apiUrl,
        type: "GET",
        async: false,
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
            xhr.setRequestHeader("Refresh", keycloak.refreshToken);
        },
        success: function (result) {
            domains.length = 0;
            for (let i = 2; i < result.length; i += 3) {
                domains.push(result[i]);
            }
        },
    });

    let domainData = {};
    for (let domain of domains) {
        domainData[domain] = "null";
    }

    window.domainData = domainData;
}

export function fetchNewData() {
    fetchDomains();
    var apiUrl = window.location.origin + "/StackV-web/restapi/model/refresh";
    var ret;
    $.ajax({
        url: apiUrl,
        type: "POST",
        async: false,
        data: JSON.stringify(window.domainData),
        contentType: "application/json; charset=utf-8",
        dataType: "json",
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
            xhr.setRequestHeader("Refresh", keycloak.refreshToken);
        },
        success: function (result) {
            console.log(result);
            ret = result;
        },
    });

    window.view.update(ret);
}
