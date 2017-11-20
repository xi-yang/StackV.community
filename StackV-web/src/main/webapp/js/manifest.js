/* global baseUrl */

load();
function load() {
    var uuid = location.search.split("?uuid=")[1];
    var token = sessionStorage.getItem("token");
    var apiUrl = baseUrl + '/StackV-web/restapi/app/details/' + uuid + '/instance';
    $.ajax({
        url: apiUrl,
        type: 'GET',
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + token);
        },
        success: function (instance) {
            var serviceName = instance[0];
            switch (serviceName) {
                case "Dynamic Network Connection":
                    loadManifest("dnc-manifest-template.xml");
                    break;
                case "Virtual Cloud Network":
                    $.get({
                        url: "/StackV-web/restapi/service/property/" + uuid + "/host/",
                        beforeSend: function (xhr) {
                            xhr.setRequestHeader("Authorization", "bearer " + token);
                        },

                        success: function (data) {
                            if (data === "ops") {
                                loadManifest("vcn-ops-manifest-template.xml");
                            } else if (data === "aws") {
                                loadManifest("vcn-aws-manifest-template.xml");
                            }
                        },
                        dataType: "text"
                    });                    
                    break;
                case "Advanced Hybrid Cloud":
                    loadManifest("ahc-manifest-template.xml");
                    break;
            }
        }
    });
}

function loadManifest(templateURL) {
    var uuid = location.search.split("?uuid=")[1];
    $.get({
        url: "/StackV-web/data/xml/manifest-templates/" + templateURL,
        success: function (data) {
            var template = data;
            var token = sessionStorage.getItem("token");
            $.ajax({
                type: "POST",
                crossDomain: true,
                url: "/StackV-web/restapi/service/manifest/" + uuid,

                data: template,
                beforeSend: function (xhr) {
                    xhr.setRequestHeader("Authorization", "bearer " + token);
                },

                headers: {
                    'Accept': 'application/json',
                    'Content-Type': 'application/xml'
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
        if (container_type === "table") {
            if (map.constructor !== Array) {
                string = $("<tr><td>" + key + "</td></tr>");
                container.append(string);
                var new_container = string;
            } else {
                var new_container = container;
            }
            var isMap = Object.keys(map[key]).length > 0;
            var isString = (typeof map[key]) === "string";
            if ((map[key].constructor === Array) && !isString) {
                var n1 = $("<td></td>");
                new_container.append(n1);
                var n3 = $("<ul class=\"manifest-list\" style=\"padding-left:.5em;\"></ul>");
                n1.append(n3);
                for (var i in map[key]) {
                    if ((typeof map[key[i]]) === "string") {
                        string += "<li><b> " + i + "</b>: " + map[key][i] + "</li>";
                        container.append(string);
                        string = "";
                    } else if (map[key][i].constructor === Array) {
                        parseMap(map[key][i], n3, "list", base);
                    }
                }
            } else if (isMap && !isString) {
                var n1 = $("<td></td>");
                new_container.append(n1);
                var n3 = $("<ul class=\"manifest-list\" style=\"padding-left:0;\"></ul>");
                n1.append(n3);
                parseMap(map[key], n3, "list", base);
            } else {
                string = $("<td>" + map[key] + "</td>");
                new_container.append(string);
            }
        } else if (container_type === "list") {
            var isMap = Object.keys(map[key]).length > 0;
            var isString = (typeof map[key]) === "string";
            if ((map[key].constructor === Array || isMap) && !isString) {
                var n3 = $("<ul class=\"manifest-list\"></ul>");
                if (map[key].constructor === Array) {
                    var item = $("<li><b>" + key + "</b></li>");
                    n3.append(item);
                    container.append(n3);
                } else {
                    if ($.trim(key).length !== 1) { // hack will replace 
                        var item = $("<li><b>" + key + "</b></li>");
                        n3.append(item);
                        container.append(n3);
                    } else {
                        var item = $("<li></li>");
                        item.append(n3);
                        container.append(item);
                    }
                }
                for (var i in map[key]) {
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
                if ($.trim(map[key]) !== '') {

                    var n3 = $("<ul class=\"manifest-list\"></ul>");

                    string = $("<li><b> " + key + "</b>: " + map[key] + "</li>");
                    n3.append(string);
                    container.append(n3);
                }
            }
        }
    }
}