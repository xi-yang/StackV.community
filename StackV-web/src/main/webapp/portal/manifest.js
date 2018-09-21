import { keycloak } from "./nexus";

export function loadManifest() {
    var uuid = location.search.split("?uuid=")[1];
    var apiUrl = window.location.origin + "/StackV-web/restapi/app/details/" + uuid + "/instance";
    $.ajax({
        url: apiUrl,
        type: "GET",
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
        },
        success: function (instance) {
            var serviceName = instance[0];
            switch (serviceName) {
                case "Dynamic Network Connection":
                    subloadManifest("dnc-manifest-template.xml");
                    break;
                case "EdgeCloud Connection":
                    subloadManifest("ecc-manifest-template.xml");
                    break;
                case "Virtual Cloud Network":
                    $.get({
                        url: "/StackV-web/restapi/service/property/" + uuid + "/host/",
                        beforeSend: function (xhr) {
                            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
                        },

                        success: function (data) {
                            if (data === "ops") {
                                subloadManifest("vcn-ops-manifest-template.xml");
                            } else if (data === "aws") {
                                subloadManifest("vcn-aws-manifest-template.xml");
                            }
                        },
                        dataType: "text"
                    });
                    break;
                case "Advanced Hybrid Cloud":
                    subloadManifest("ahc-manifest-template.xml");
                    break;
            }
        }
    });
}

function subloadManifest(templateURL) {
    var uuid = location.search.split("?uuid=")[1];
    $.get({
        url: "/StackV-web/data/xml/manifest-templates/" + templateURL,
        success: function (data) {
            var template = data;
            $.ajax({
                type: "POST",
                crossDomain: true,
                url: "/StackV-web/restapi/service/manifest/" + uuid,

                data: template,
                beforeSend: function (xhr) {
                    xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
                },

                headers: {
                    "Accept": "application/json",
                    "Content-Type": "application/xml"
                },
                success: function (data, textStatus, jqXHR) {
                    var manifest = JSON.parse(data.jsonTemplate);
                    var name = Object.keys(manifest)[0];
                    manifest = manifest[name];
                    var baseTable = $("#manifest_table_body");
                    baseTable.append("<tr><td colspan=\"2\"><b>" + name + "</b><br/><b>UUID</b>: " + data.serviceUUID + "</td></tr>");

                    parseMap(manifest, baseTable, "table");
                },
                error: function (jqXHR, textStatus, errorThrown) {
                    var exceptionObj = JSON.parse(jqXHR.responseText);
                    alert("exception: " + exceptionObj.exception);
                },

                dataType: "json"
            });
        },
        dataType: "text"
    });
}

function parseMap(map, container, container_type, base) {
    for (var key in map) {
        var string = "";
        let isMap, isString;
        if (container_type === "table") {
            var new_container;
            if (map.constructor !== Array) {
                string = $("<tr><td>" + key + "</td></tr>");
                container.append(string);
                new_container = string;
            } else {
                new_container = container;
            }
            isMap = Object.keys(map[key]).length > 0;
            isString = (typeof map[key]) === "string";
            let n1, n3;
            if ((map[key].constructor === Array) && !isString) {
                n1 = $("<td></td>");
                new_container.append(n1);
                n3 = $("<ul class=\"manifest-list\" style=\"padding-left:.5em;\"></ul>");
                n1.append(n3);
                for (var i in map[key]) {
                    if ((typeof map[key]) === "string") {
                        string += "<li><b> " + map[key][i] + "</b>: " + map[key][i] + "</li>";
                        container.append(string);
                        string = "";
                    } else if (map[key].constructor === Array) {
                        parseMap(map[key][i], n3, "list", base);
                    }
                }
            } else if (isMap && !isString) {
                n1 = $("<td></td>");
                new_container.append(n1);
                n3 = $("<ul class=\"manifest-list\" style=\"padding-left:0;\"></ul>");
                n1.append(n3);
                parseMap(map[key], n3, "list", base);
            } else {
                string = $("<td>" + map[key] + "</td>");
                new_container.append(string);
            }
        } else if (container_type === "list") {
            isMap = Object.keys(map[key]).length > 0;
            isString = (typeof map[key]) === "string";
            let n3;
            if ((map[key].constructor === Array || isMap) && !isString) {
                n3 = $("<ul class=\"manifest-list\"></ul>");
                let item;
                if (map[key].constructor === Array) {
                    item = $("<li><b>" + key + "</b></li>");
                    n3.append(item);
                    container.append(n3);
                } else {
                    if ($.trim(key).length !== 1) { // hack will replace 
                        item = $("<li><b>" + key + "</b></li>");
                        n3.append(item);
                        container.append(n3);
                    } else {
                        item = $("<li></li>");
                        item.append(n3);
                        container.append(item);
                    }
                }
                for (let i in map[key]) {
                    if ((typeof map[key][i]) === "string") {// && ($.trim(map[key[i]]) !== '')) {
                        string += "<li><b> " + i + "</b>: " + map[key][i] + "</li>";
                        n3.append(string);
                        string = "";
                    } else {
                        var bullet = $("<li></li>");
                        n3.append(bullet);
                        var newList = $("<ul class=\"manifest-list\"></ul>");
                        bullet.append(newList);
                        parseMap(map[key][i], newList, "list", base);
                    }
                }
            } else {
                if ($.trim(map[key]) !== "") {

                    var n3 = $("<ul class=\"manifest-list\"></ul>");

                    string = $("<li><b> " + key + "</b>: " + map[key] + "</li>");
                    n3.append(string);
                    container.append(n3);
                }
            }
        }
    }
}
