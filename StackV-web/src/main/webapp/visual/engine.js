/* global keycloak */
import * as d3 from "d3";
import $ from "jquery";

import AllMockData from "./data.mock";
import ServerData from "./data-model/server-data/server-data";

var domains = [];

export default function initVisualization(selector) {
    fetchDomains();

    let domainData = {};
    for (let domain of domains) {
        domainData.domain = "null";
    }
    var apiUrl = window.location.origin + "/StackV-web/restapi/model/refresh";
    $.ajax({
        url: apiUrl,
        type: "POST",
        data: JSON.stringify(domainData),
        contentType: "application/json; charset=utf-8",
        dataType: "json",
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
            xhr.setRequestHeader("Refresh", keycloak.refreshToken);
        },
        success: function (result) {
            console.log(result);
        },
    });    

    window.r = AllMockData;
    window.server = ServerData;
    window.d3 = d3;

    import(/* webpackPreload: true */ /* webpackChunkName: "datamodel" */ "./data-model/data-model").then(module => {
        window.d = module.default;
    });

    import(/* webpackPreload: true */ /* webpackChunkName: "visualmodel" */ "./visual-model/visual-model").then(module => {
        const StackVGraphic = module.default;
        window.v = StackVGraphic;

        const view = new StackVGraphic(AllMockData[0], document.querySelector(selector));
        window.data = view.dataModel;
        window.view = view;

        d3.select("#reset-page").on("click", () => {
            view.restart();
        });

        d3.select("#del-state").on("click", () => {
            localStorage.clear();
            window.location.reload();
        });

        d3.select("#dynamic-load-1").on("click", () => {
            view.update(AllMockData[1]);
        });

        d3.select("#dynamic-load-2").on("click", () => {
            view.update(AllMockData[2]);
        });

        d3.select("#dynamic-load-3").on("click", () => {
            view.update(AllMockData[3]);
        });

        view.restart();
    });
}

function fetchDomains() {
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
}
