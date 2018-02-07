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

/* global XDomainRequest, baseUrl, keycloak, TweenLite, Power2, Mousetrap */
// Tweens
var tweenRolePanel = new TweenLite("#acl-role-panel", .5, {ease: Power2.easeInOut, paused: true, right: "5%"});
var tweenRoleGroupsPanel = new TweenLite("#acl-role-group-div", .5, {ease: Power2.easeInOut, paused: true, top: "5px"});
var tweenRoleRolesPanel = new TweenLite("#acl-role-role-div", .5, {ease: Power2.easeInOut, paused: true, bottom: "5%"});
var tweenInstancePanel = new TweenLite("#acl-instance-panel", .5, {ease: Power2.easeInOut, paused: true, left: "5%"});
var tweenInstanceACLPanel = new TweenLite("#acl-instance-acl", .5, {ease: Power2.easeInOut, paused: true, bottom: "0"});


var tweenHideUserPanel = new TweenLite("#acl-role-user-div", .5, {ease: Power2.easInOut, paused: true, left: "-100%"});
var tweenGroupRolePanel = new TweenLite("div#acl-group-role-div", .5, {ease: Power2.easInOut, paused: true, left: "5px"});

var view = "center";

Mousetrap.bind({
    'shift+left': function () {
        window.location.href = "/StackV-web/ops/srvc/driver.jsp";
    },
    'left': function () {
        viewShift("left");
    },
    'right': function () {
        viewShift("right");
    }
});

function viewShift(dir) {
    switch (view) {
        case "left":
            if (dir === "right") {
                newView("instances");
            }
            break;
        case "center":
            if (dir === "left") {
                newView("roles");
            }
            break;
    }
}
function resetView() {
    switch (view) {
        case "left":
            $("#sub-nav .active").removeClass("active");
            tweenRolePanel.reverse();
            break;
        case "center":
            $("#sub-nav .active").removeClass("active");
            tweenInstancePanel.reverse();
            break;
    }
}
function newView(panel) {
    resetView();
    switch (panel) {
        case "roles":
            tweenRolePanel.play();
            $("#roles-tab").addClass("active");
            view = "left";
            break;
        case "instances":
            tweenInstancePanel.play();
            $("#instances-tab").addClass("active");
            view = "center";
            break;
    }
}
function loadACLNavbar() {
    $("#sub-nav").load("/StackV-web/nav/acl_navbar.html", function () {
        switch (view) {
            case "left":
                $("#roles-tab").addClass("active");
                break;
            case "center":
                $("#instances-tab").addClass("active");
                break;
        }

        $("#roles-tab").click(function () {
            resetView();
            newView("roles");
        });
        $("#instances-tab").click(function () {
            resetView();
            newView("instances");
        });
    });
}


// ACL Load
function loadACLPortal() {
    subloadRoleACLUsers();
    subloadRoleACLGroups();
    subloadRoleACLRoles();

    subloadInstanceACLInstances();
    subloadInstanceACLUsers();

    // Roles   
    $("#acl-group-add").click(function (evt) {
        var subject = $("#acl-user").val();
        var apiUrl = baseUrl + '/StackV-web/restapi/app/keycloak/users/' + subject + '/groups';
        keycloak.updateToken(30).success(function () {
            $.ajax({
                url: apiUrl,
                type: 'POST',
                data: '{"id":"' + $("#acl-group-select").val() + '","name":"' + $('#acl-group-select').children("option:selected").text() + '"}',
                contentType: "application/json; charset=utf-8",
                dataType: "json",
                beforeSend: function (xhr) {
                    xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
                },
                success: function () {
                    subloadRoleACLUserGroups();
                    subloadRoleACLUserRoles();
                }
            });
        }).error(function () {
            console.log("Fatal Error: Token update failed!");
        });

        evt.preventDefault();
    });

    $("#acl-role-add").click(function (evt) {
        var subject = $("#acl-user").val();
        var apiUrl = baseUrl + '/StackV-web/restapi/app/keycloak/users/' + subject + '/roles';
        keycloak.updateToken(30).success(function () {
            $.ajax({
                url: apiUrl,
                type: 'POST',
                data: '{"id":"' + $("#acl-role-select").val() + '","name":"' + $('#acl-role-select').children("option:selected").text() + '"}',
                contentType: "application/json; charset=utf-8",
                dataType: "json",
                beforeSend: function (xhr) {
                    xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
                },
                success: function () {
                    subloadRoleACLUserRoles();
                }
            });
        }).error(function () {
            console.log("Fatal Error: Token update failed!");
        });

        evt.preventDefault();
    });

    $(".acl-user-close").click(function (evt) {
        $(".acl-role-selected-row").removeClass("acl-role-selected-row");

        tweenRoleGroupsPanel.reverse();
        tweenRoleRolesPanel.reverse();
        evt.preventDefault();
    });

    // Instances
    $(".acl-instance-close").click(function (evt) {
        $(".acl-instance-selected-row").removeClass("acl-instance-selected-row");
        $(".acl-instance-row").show();

        tweenInstanceACLPanel.reverse();
        evt.preventDefault();
    });
}

// Roles
function subloadRoleACLUsers() {
    var tbody = document.getElementById("user-body");

    var apiUrl = baseUrl + '/StackV-web/restapi/app/keycloak/users';
    keycloak.updateToken(30).success(function () {
        $.ajax({
            url: apiUrl,
            type: 'GET',
            beforeSend: function (xhr) {
                xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
            },
            success: function (result) {
                for (i = 0; i < result.length; i++) {
                    var user = result[i];

                    var row = document.createElement("tr");
                    row.className = "acl-role-row";
                    row.setAttribute("data-subject", user[4]);

                    var cell1_1 = document.createElement("td");
                    cell1_1.innerHTML = user[0];

                    row.appendChild(cell1_1);
                    tbody.appendChild(row);
                }

                $(".acl-role-row").click(function () {
                    $(".acl-role-selected-row").removeClass("acl-role-selected-row");
                    $("#acl-user").val($(this).data("subject"));

                    subloadRoleACLUserGroups();
                    subloadRoleACLUserRoles();

                    $(this).addClass("acl-role-selected-row");
                });

                if (view === "left") {
                    newView("roles");
                }
            }
        });
    }).error(function () {
        console.log("Fatal Error: Token update failed!");
    });
}

function subloadRoleACLGroups() {
    var apiUrl = baseUrl + '/StackV-web/restapi/app/keycloak/groups';

    keycloak.updateToken(30).success(function () {
        $.ajax({
            url: apiUrl,
            type: 'GET',
            beforeSend: function (xhr) {
                xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
            },
            success: function (result) {
                for (i = 0; i < result.length; i++) {
                    var group = result[i];

                    $("#acl-group-select").append(new Option(group[1], group[0]));
                }
            }
        });
    }).error(function () {
        console.log("Fatal Error: Token update failed!");
    });
}

function subloadRoleACLRoles() {
    var apiUrl = baseUrl + '/StackV-web/restapi/app/keycloak/roles';
    keycloak.updateToken(30).success(function () {
        $.ajax({
            url: apiUrl,
            type: 'GET',
            beforeSend: function (xhr) {
                xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
            },
            success: function (result) {
                for (i = 0; i < result.length; i++) {
                    var role = result[i];

                    $("#acl-role-select").append(new Option(role[1], role[0]));
                }
            }
        });
    }).error(function () {
        console.log("Fatal Error: Token update failed!");
    });
}

function subloadRoleACLUserGroups() {
    tweenRoleGroupsPanel.reverse();


    setTimeout(function () {
        keycloak.updateToken(30).success(function () {
            var subject = $("#acl-user").val();
            var tbody = document.getElementById("group-body");
            tbody.innerHTML = "";
            $("#acl-group-select option.hide").removeClass("hide");

            var apiUrl = baseUrl + '/StackV-web/restapi/app/keycloak/users/' + subject + '/groups';
            $.ajax({
                url: apiUrl,
                type: 'GET',
                beforeSend: function (xhr) {
                    xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
                },
                success: function (result) {
                    if (result) {
                        for (i = 0; i < result.length; i++) {
                            var group = result[i];

                            var row = document.createElement("tr");
                            row.className = "acl-user-group";
                            row.setAttribute("data-groupname", group[1]);

                            var cell1_1 = document.createElement("td");
                            cell1_1.innerHTML = group[1] + '<button data-roleid="' + group[0] + '" data-rolename="' + group[1] + '" class="button-group-delete btn btn-default pull-right">Remove</button>';

                            row.appendChild(cell1_1);
                            tbody.appendChild(row);

                            $("#acl-group-select option[value=" + group[0] + "]").addClass("hide");
                            $("#acl-group-select").val(null);
                        }

                        $(".acl-user-group").click(function () {
                            var name = $(this).data('groupname');
                            loadGroupTable(name);
                        });



                        $(".button-group-delete").click(function (evt) {
                            var subject = $("#acl-user").val();

                            var roleID = $(this).data("roleid");
                            var roleName = $(this).data("rolename");
                            var apiUrl = baseUrl + '/StackV-web/restapi/app/keycloak/users/' + subject + '/groups';
                            keycloak.updateToken(30).success(function () {
                                $.ajax({
                                    url: apiUrl,
                                    type: 'DELETE',
                                    data: '{"id":"' + roleID + '","name":"' + roleName + '"}',
                                    contentType: "application/json; charset=utf-8",
                                    dataType: "json",
                                    beforeSend: function (xhr) {
                                        xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
                                    },
                                    success: function (result) {
                                        subloadRoleACLUserGroups();
                                        subloadRoleACLUserRoles();
                                    }
                                });
                            }).error(function () {
                                console.log("Fatal Error: Token update failed!");
                            });

                            evt.preventDefault();
                        });

                        tweenRoleGroupsPanel.play();
                    }
                }
            });
        }).error(function () {
            console.log("Fatal Error: Token update failed!");
        });
    }, 500);
}

function subloadRoleACLUserRoles() {
    tweenRoleRolesPanel.reverse();
    setTimeout(function () {
        keycloak.updateToken(30).success(function () {
            var subject = $("#acl-user").val();
            var tbody = document.getElementById("role-body");
            tbody.innerHTML = "";
            $("#acl-role-select option.hide").removeClass("hide");

            var apiUrl = baseUrl + '/StackV-web/restapi/app/keycloak/users/' + subject + '/roles';
            $.ajax({
                url: apiUrl,
                type: 'GET',
                beforeSend: function (xhr) {
                    xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
                },
                success: function (result) {
                    if (result) {
                        for (i = 0; i < result.length; i++) {
                            var role = result[i];

                            var row = document.createElement("tr");

                            var cell1_1 = document.createElement("td");
                            if (role[2] === "assigned") {
                                cell1_1.innerHTML = role[1] + '<button data-roleid="' + role[0] + '" data-rolename="' + role[1] + '" class="button-role-delete btn btn-default pull-right">Remove</button>';
                            } else {
                                cell1_1.innerHTML = role[1] + ' (delegated from ' + role[2] + ')';
                            }

                            row.appendChild(cell1_1);
                            tbody.appendChild(row);

                            $("#acl-role-select option[value=" + role[0] + "]").addClass("hide");
                            $("#acl-role-select").val(null);
                        }

                        $(".button-role-delete").click(function (evt) {
                            var subject = $("#acl-user").val();

                            var roleID = $(this).data("roleid");
                            var roleName = $(this).data("rolename");
                            var apiUrl = baseUrl + '/StackV-web/restapi/app/keycloak/users/' + subject + '/roles';
                            keycloak.updateToken(30).success(function () {
                                $.ajax({
                                    url: apiUrl,
                                    type: 'DELETE',
                                    data: '{"id":"' + roleID + '","name":"' + roleName + '"}',
                                    contentType: "application/json; charset=utf-8",
                                    dataType: "json",
                                    beforeSend: function (xhr) {
                                        xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
                                    },
                                    success: function (result) {
                                        subloadRoleACLUserRoles();
                                    }
                                });
                            }).error(function () {
                                console.log("Fatal Error: Token update failed!");
                            });

                            evt.preventDefault();
                        });

                        tweenRoleRolesPanel.play();
                    }
                }
            });
        }).error(function () {
            console.log("Fatal Error: Token update failed!");
        });
    }, 500);
}


// Instances
function subloadInstanceACLInstances() {
    var userId = keycloak.subject;
    var tbody = document.getElementById("instance-body");

    var apiUrl = baseUrl + '/StackV-web/restapi/app/panel/' + userId + '/instances';
    keycloak.updateToken(30).success(function () {
        $.ajax({
            url: apiUrl,
            type: 'GET',
            beforeSend: function (xhr) {
                xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
            },
            success: function (result) {
                if (result) {
                    for (i = 0; i < result.length; i++) {
                        var instance = result[i];

                        var row = document.createElement("tr");
                        row.className = "acl-instance-row";
                        row.setAttribute("data-uuid", instance[1]);

                        var cell1_1 = document.createElement("td");
                        cell1_1.innerHTML = instance[3];
                        var cell1_2 = document.createElement("td");
                        cell1_2.innerHTML = instance[0];
                        var cell1_3 = document.createElement("td");
                        cell1_3.innerHTML = instance[1];
                        row.appendChild(cell1_1);
                        row.appendChild(cell1_2);
                        row.appendChild(cell1_3);
                        tbody.appendChild(row);
                    }

                    $(".acl-instance-row").click(function () {
                        $(".acl-instance-selected-row").removeClass("acl-instance-selected-row");

                        subloadInstanceACLTable($(this).data("uuid"));

                        $(this).addClass("acl-instance-selected-row");
                        $(".acl-instance-row").not(".acl-instance-selected-row").hide();
                    });

                    if (view === "center") {
                        newView("instances");
                    }
                }
            }
        });
    }).error(function () {
        console.log("Fatal Error: Token update failed!");
    });
}

function subloadInstanceACLUsers() {
    var apiUrl = baseUrl + '/StackV-web/restapi/app/keycloak/users';
    var tbody = document.getElementById("users-body");
    tbody.innerHTML = "";

    keycloak.updateToken(30).success(function () {
        $.ajax({
            url: apiUrl,
            type: 'GET',
            beforeSend: function (xhr) {
                xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
            },
            success: function (result) {
                if (result) {
                    for (i = 0; i < result.length; i++) {
                        var user = result[i];

                        var row = document.createElement("tr");
                        var cell1_1 = document.createElement("td");
                        cell1_1.innerHTML = user[0];
                        var cell1_2 = document.createElement("td");
                        cell1_2.innerHTML = user[1];
                        var cell1_3 = document.createElement("td");
                        cell1_3.innerHTML = user[2];
                        var cell1_4 = document.createElement("td");
                        cell1_4.innerHTML = '<button data-username="' + user[0] + '" class="button-instanceacl-add btn btn-default pull-right">Add</button>';
                        row.appendChild(cell1_1);
                        row.appendChild(cell1_2);
                        row.appendChild(cell1_3);
                        row.appendChild(cell1_4);
                        tbody.appendChild(row);
                    }

                    $(".button-instanceacl-add").click(function (evt) {
                        var username = $(this).data("username");
                        var refUUID = $("#acl-instance").val();

                        var apiUrl = baseUrl + '/StackV-web/restapi/app/acl/' + refUUID;
                        keycloak.updateToken(30).success(function () {
                            $.ajax({
                                url: apiUrl,
                                type: 'POST',
                                data: username,
                                contentType: "application/json; charset=utf-8",
                                dataType: "json",
                                beforeSend: function (xhr) {
                                    xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
                                },
                                success: function (result) {
                                    subloadInstanceACLTable(refUUID);
                                }
                            });
                        }).error(function () {
                            console.log("Fatal Error: Token update failed!");
                        });

                        evt.preventDefault();
                    });
                }
            }
        });
    }).error(function () {
        console.log("Fatal Error: Token update failed!");
    });
}


/**
 * Currently, just logs in the user using default credentials
 * Check if the user is currently logged into IPA server. If not logged in, the log in the user.
 * @param {type} username
 * @param {type} password
 * @returns {jqXHR}
 */
function ipaLogin(username, password){
    var apiUrl = baseUrl + "/StackV-web/restapi/app/acl/ipa/login";
    
    return $.ajax({
        url: apiUrl,
        type: 'POST',
        data: {
            "username":"admin",
            "password":"max12345"
        },
        beforeSend: function (xhr) {
            // check here if the user is already logged in and the cookie did not expire
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
        },
        success: function(result) {
            console.log("ipaLogin success: " + JSON.stringify(result));
            if (result["Result"] === "Login Successful") {
                return true;
            } else {
                return false;
            }
        },
        error: function(err) {
            console.log("ipaLogin error: " + JSON.stringify(err));
        }
    });
}


/**
 * Creates the UserGroup for the specified service with given group name
 * @param {string} groupName
 * @param {string} desc 
 * @returns {jqXHR}
 */
function createUserGroup(groupName, desc) {
    //console.log("in createUserGroupForService: groupName -> " + groupName + ", description -> " + desc);    
    var apiUrl = baseUrl + '/StackV-web/restapi/app/acl/ipa/request';
        
    // creating the IPA request
    var ipaRequestData = {
        "method":"group_add",
        "params":[
            [groupName],
            {"description": desc}
        ],
        "id":0
    };
    
    // ajax call fields
    // in the beforeSend field, if false is return the request will be cancelled. Can be used to check if the user is logged in
    var ipaAjaxCall = {
        "url": apiUrl,
        "method": "POST",
        "headers": {
            "Content-Type": "application/json",
            "Authorization": "bearer " + keycloak.token
        },
        "data": JSON.stringify(ipaRequestData)
    };
    
    return $.ajax(ipaAjaxCall);
}

/**
 * Adds specified users to specified userGroup
 * @param {type} users
 * @param {type} userGroup
 * @returns {jqXHR}
 */
function addUsersToUserGroup(users,userGroup) {
    var apiUrl = baseUrl + '/StackV-web/restapi/app/acl/ipa/request';
    
    // if only a single user is provided - make sure it is enclosed in an array
    if (!Array.isArray(users)) {
        users = [users];
    }
        
    // creating the IPA request
    var ipaRequestData = {
        "method":"group_add_member",
        "params":[
            [userGroup],
            {"user": users}
        ],
        "id":0
    };
        
    
    // ajax call fields
    // future use: in the beforeSend field, if false is return the request will be cancelled. Can be used to check if the user is logged in
    var ipaAjaxCall = {
        "url": apiUrl,
        "method": "POST",
        "headers": {
            "Content-Type": "application/json",
            "Authorization": "bearer " + keycloak.token
        },
        "data": JSON.stringify(ipaRequestData)
    };
    
    return $.ajax(ipaAjaxCall);
}

/**
 * Creates the HostGroup for the specified service with given group name
 * @param {type} groupName
 * @param {type} desc
 * @returns {jqXHR}
 */
function createHostGroup(groupName, desc) {
    var apiUrl = baseUrl + '/StackV-web/restapi/app/acl/ipa/request';
        
    // creating the IPA request
    var ipaRequestData = {
        "method":"hostgroup_add",
        "params":[
            [groupName],
            {"description": desc}
        ],
        "id":0
    };
    
    // ajax call fields
    // future use: in the beforeSend field, if false is return the request will be cancelled. Can be used to check if the user is logged in
    var ipaAjaxCall = {
        "url": apiUrl,
        "method": "POST",
        "headers": {
            "Content-Type": "application/json",
            "Authorization": "bearer " + keycloak.token
        },
        "data": JSON.stringify(ipaRequestData)
    };
    
    return $.ajax(ipaAjaxCall);
}

/**
 * Gets hosts for the specified service instance
 * @param {String} serviceUUID
 * @returns {jqXHR}
 */
function getHostsForServiceInstance(serviceUUID) {
    var apiUrl = baseUrl + '/StackV-web/restapi/service/manifest/' + serviceUUID;
    
    // ajax call fields
    var settings = {
        "async": true,
        "crossDomain": true,
        "url": apiUrl,
        "method": "POST",
        "headers": {
          "Content-Type": "application/xml",
          "Authorization": "bearer " + keycloak.token
        },
        "data": "<serviceManifest>\n<serviceUUID></serviceUUID>\n<jsonTemplate>\n{\n    \"hostgroup\": [\n        {\n          \"hostname\": \"?fqdn?\",\n          \"sparql\": \"SELECT DISTINCT ?fqdn WHERE {?hypervisor mrs:providesVM ?vm. ?vm mrs:hasNetworkAddress ?na. ?na mrs:type \\\"fqdn\\\". ?na mrs:value ?fqdn.}\",\n          \"required\": \"false\"\n        }\n      ]\n}\n</jsonTemplate>\n</serviceManifest>"
      };
    
    return $.ajax(settings);
}

function addHostsToHostGroup(hosts, hostGroupName) {
    var apiUrl = baseUrl + '/StackV-web/restapi/app/acl/ipa/request';
    
    if (!Array.isArray(hosts)) {
        hosts = [hosts];
    }
        
    // creating the IPA request
    var ipaRequestData = {
        "method":"hostgroup_add_member",
        "params":[
            [hostGroupName],
            {"host":hosts}
        ],
        "id":0
    };
    
    // ajax call fields
    // future use: in the beforeSend field, if false is return the request will be cancelled. Can be used to check if the user is logged in
    var ipaAjaxCall = {
        "url": apiUrl,
        "method": "POST",
        "headers": {
            "Content-Type": "application/json",
            "Authorization": "bearer " + keycloak.token
        },
        "data": JSON.stringify(ipaRequestData)
    };
    
    return $.ajax(ipaAjaxCall);
}

/**
 * Creates an HBAC rule give the ruleName and description
 * @param {type} ruleName
 * @param {type} desc
 * @returns {jqXHR}
 */
function createHBACRule(ruleName, desc) {
    var apiUrl = baseUrl + '/StackV-web/restapi/app/acl/ipa/request';
        
    // creating the IPA request
    var ipaRequestData = {
        "method":"hbacrule_add",
        "params":[
            [ruleName],
            {"description": desc}
        ],
        "id":0
    };
    
    // ajax call fields
    // future use: in the beforeSend field, if false is return the request will be cancelled. Can be used to check if the user is logged in
    var ipaAjaxCall = {
        "url": apiUrl,
        "method": "POST",
        "headers": {
            "Content-Type": "application/json",
            "Authorization": "bearer " + keycloak.token
        },
        "data": JSON.stringify(ipaRequestData)
    };
    
    return $.ajax(ipaAjaxCall);
}

function addUserGroupToHBACRule(userGroup, hbacRule) {
    var apiUrl = baseUrl + '/StackV-web/restapi/app/acl/ipa/request';
    
    if (!Array.isArray(userGroup)) {
        userGroup = [userGroup];
    }
        
    // creating the IPA request
    var ipaRequestData = {
        "method":"hbacrule_add_user",
        "params":[
            [hbacRule],
            {"group": userGroup}
        ],
        "id":0
    };
    
    // ajax call fields
    // future use: in the beforeSend field, if false is return the request will be cancelled. Can be used to check if the user is logged in
    var ipaAjaxCall = {
        "url": apiUrl,
        "method": "POST",
        "headers": {
            "Content-Type": "application/json",
            "Authorization": "bearer " + keycloak.token
        },
        "data": JSON.stringify(ipaRequestData)
    };
    
    return $.ajax(ipaAjaxCall);
}

function addHostGroupToHBACRule(hostGroup, hbacRule) {
    var apiUrl = baseUrl + '/StackV-web/restapi/app/acl/ipa/request';
    
    if (!Array.isArray(hostGroup)) {
        hostGroup = [hostGroup];
    }
    
    // creating the IPA request
    var ipaRequestData = {
        "method":"hbacrule_add_host",
        "params":[
            [hbacRule],
            {"hostgroup": hostGroup}
        ],
        "id":0
    };
    
    // ajax call fields
    // future use: in the beforeSend field, if false is return the request will be cancelled. Can be used to check if the user is logged in
    var ipaAjaxCall = {
        "url": apiUrl,
        "method": "POST",
        "headers": {
            "Content-Type": "application/json",
            "Authorization": "bearer " + keycloak.token
        },
        "data": JSON.stringify(ipaRequestData)
    };
    
    return $.ajax(ipaAjaxCall);
}

function addServicesToHBACRule(services, hbacRule) {
    var apiUrl = baseUrl + '/StackV-web/restapi/app/acl/ipa/request';
    
    if (!Array.isArray(services)) {
        services = [services];
    }
    
    // creating the IPA request
    var ipaRequestData = {
        "method":"hbacrule_add_service",
        "params":[
            [hbacRule],
            {"hbacsvc": services}
        ],
        "id":0
    };
    
    // ajax call fields
    // future use: in the beforeSend field, if false is return the request will be cancelled. Can be used to check if the user is logged in
    var ipaAjaxCall = {
        "url": apiUrl,
        "method": "POST",
        "headers": {
            "Content-Type": "application/json",
            "Authorization": "bearer " + keycloak.token
        },
        "data": JSON.stringify(ipaRequestData)
    };
    
    return $.ajax(ipaAjaxCall);
}

/*
 * Creates a new IPA ACL Login policy (HBAC Rule with user groups and host groups) for the specified UUID
 * Abbreviations: ug -> user group, hg -> host group
 */
function createLoginAclPolicy(serviceUUID, username) {        
    // start by creating login access as both login and sudo require login access
    var ugLoginName = "ug-login-" + serviceUUID;
    var hgLoginName = "hg-login-" + serviceUUID;
    var hbacLoginName = "hbac-login-" + serviceUUID;    
    var loginServices = ["login","sshd"];
    var hosts = [];
    
    var aclLoginPolicyResult = {}; // currently a way to debug errors
    
    // need to change it so when all the ajax calls are done - then return the aclPolicyResult
    
    // the user group, host group, hbac rule all need to created before any
    // (mainly adding users, hosts, and services) is done to them
    
    var createLoginUg = createUserGroup(ugLoginName,"Login user group for service instance: " + serviceUUID);
    var createLoginHg = createHostGroup(hgLoginName, "Login host group for service instance: " + serviceUUID);
    var createLoginHbac = createHBACRule(hbacLoginName,"Login HBAC Rule (login,ssh) for service instance: " + serviceUUID);
    var getLoginHosts = getHostsForServiceInstance(serviceUUID);
    
    return $.when(createLoginUg, createLoginHg, createLoginHbac, getLoginHosts).done(function(ug, hg, hbac, hostsQuery) {
        var ugError = ug[0]["error"];
        var hgError = hg[0]["error"];
        var hbacError = hbac[0]["error"];
        var hostsQueryError = true;
        
        
        console.log("**** login getHostsForServiceInstance hostsQuery raw: " + hostsQuery);
        console.log("*login getHostsForServiceInstance first element in array: " + JSON.stringify(hostsQuery[0]));
        console.log("**login getHostsForServiceInstance: " + JSON.stringify(hostsQuery));
        // verify and parse the loginHosts data
        // looks like below
        //{ serviceUUID: "5578c890-9a1c-4e23-9686-ab70ad274a92", jsonTemplate: "{\"hostgroup\":[{\"hostname\":\"180-146.research.maxgigapop.net\"}]}", jsonModel: null }
        
        // just to verify the right data is gotten
        if (hostsQuery[0]["serviceUUID"] === serviceUUID) {
            hostsQueryError = false;
            aclLoginPolicyResult["RecievedRightHostsForServiceInstance"] = true;
            var parsed = JSON.parse(hostsQuery[0]["jsonTemplate"]);
            var hostsObjs = parsed["hostgroup"];
            hostsObjs.forEach(function(h) {
                hosts.push(h["hostname"]);
            });
            console.log("parsed login hosts: " + hosts);
        } else {
            aclLoginPolicyResult["RecievedRightHostsForServiceInstance"] = false;
            aclLoginPolicyResult["RecievedRightHostsForServiceInstanceError"] = JSON.stringify(hostsQuery);
        }
        
        // if error is null for the IPA requests, then the request was successful
        
        if (ugError === null) {
            aclLoginPolicyResult["CreatedLoginUserGroup"] = true;
        } else {
            aclLoginPolicyResult["CreatedLoginUserGroup"] = false;
            aclLoginPolicyResult["CreatedLoginUserGroupError"] = ugError;
        }
        
        if (hgError === null) {
            aclLoginPolicyResult["CreatedLoginHostGroup"] = true;
        } else {
            aclLoginPolicyResult["CreatedLoginHostGroup"] = false;
            aclLoginPolicyResult["CreatedLoginHostGroupError"] = hgError;
        }
        
        if (hbacError === null) {
            aclLoginPolicyResult["CreatedLoginHBACRule"] = true;
        } else {
            aclLoginPolicyResult["CreatedLoginHBACRule"] = false;
            aclLoginPolicyResult["CreatedLoginHBACRuleError"] = hbacError;
        }
        
        // if no errors in all three -> null is a falsy value
        if (!ugError && !hgError && !hbacError && !hostsQueryError) {
            aclLoginPolicyResult["LoginGroupAndRuleCreatedAndRightHostsFound"] = true;
        }
        
    }).then(function() {
        
        console.log("in then function hosts: " + hosts);
        
        var addLoginUgUsers = addUsersToUserGroup(username, ugLoginName);
        var addLoginHgHosts = addHostsToHostGroup(hosts, hgLoginName);
        var addLoginUgToHbac = addUserGroupToHBACRule(ugLoginName,hbacLoginName);
        var addLoginHgToHbac = addHostGroupToHBACRule(hgLoginName,hbacLoginName);
        var addLoginSrvcsToHbac = addServicesToHBACRule(loginServices, hbacLoginName);
        
        return $.when(addLoginUgUsers, addLoginHgHosts, addLoginUgToHbac, addLoginHgToHbac, addLoginSrvcsToHbac)
                .then(function(ugusers, hghosts, ughbac, hghbac, srvcshbac) {
                    var ugusersError = ugusers[0]["error"];
                    var hghostsError = hghosts[0]["error"];
                    var ughbacError = ughbac[0]["error"];
                    var hghbacError = hghbac[0]["error"];
                    var srvcshbacError = srvcshbac[0]["error"];
                                        
                    
                    if (ugusersError === null) {
                        aclLoginPolicyResult["AddedUsersToLoginUserGroup"] = true;
                    } else {
                        aclLoginPolicyResult["AddedUsersToLoginUserGroup"] = false;
                        aclLoginPolicyResult["AddedUsersToLoginUserGroupError"] = ugusersError;
                    }
                    
                    if (hghostsError === null) {
                        aclLoginPolicyResult["AddedHostsToLoginHostGroup"] = true;
                    } else {
                        aclLoginPolicyResult["AddedHostsToLoginHostGroup"] = false;
                        aclLoginPolicyResult["AddedHostsToLoginHostGroupError"] = hghostsError;
                    }
                    
                    if (ughbacError === null) {
                        aclLoginPolicyResult["AddedLoginUserGroupToLoginHBAC"] = true;
                    } else {
                        aclLoginPolicyResult["AddedLoginUserGroupToLoginHBAC"] = false;
                        aclLoginPolicyResult["AddedLoginUserGroupToLoginHBACError"] = ughbacError;
                    }
                    
                    if (hghbacError === null) {
                        aclLoginPolicyResult["AddedLoginHostGroupToLoginHBAC"] = true;
                    } else {
                        aclLoginPolicyResult["AddedLoginHostGroupToLoginHBAC"] = false;
                        aclLoginPolicyResult["AddedLoginHostGroupToLoginHBACError"] = hghbacError;
                    }
                    
                    if (srvcshbacError === null) {
                        aclLoginPolicyResult["AddedLoginServicesToLoginHBAC"] = true;
                    } else {
                        aclLoginPolicyResult["AddedLoginServicesToLoginHBAC"] = false;
                        aclLoginPolicyResult["AddedLoginServicesToLoginHBACError"] = srvcshbacError;
                    }
                    
                    
                    if (!srvcshbacError && !ugusersError && !hghbacError && !ughbacError) {
                        aclLoginPolicyResult["AddedLoginGroupAndServicesToLoginHBAC"] = true;
                    }
                    return aclLoginPolicyResult;
                }).fail(function(err) {
                    console.log("IPA ACL Login policy creation failed: " + JSON.stringify(err));
                });
    }).fail(function(err) {
        console.log("IPA ACL Login policy creation failed: " + JSON.stringify(err));
    });  
}

/*
 * Creates a new IPA ACL Sudo policy (HBAC Rule with user groups and host groups) for the specified UUID
 * Abbreviations: ug -> user group, hg -> host group
 */
function createSudoAclPolicy(serviceUUID, username) {                
    var ugSudoName = "ug-sudo-" + serviceUUID;
    var hgSudoName = "hg-sudo-" + serviceUUID;
    var hbacSudoName = "hbac-sudo-" + serviceUUID;
    var sudoServices = ["login","sshd","sudo"];    
    var aclSudoPolicyResult = {}; // currently a way to debug errors
    var hosts = [];
    
    // need to change it so when all the ajax calls are done - then return the aclSudoPolicyResult
    
    // the user group, host group, hbac rule all need to created before any
    // (mainly adding users, hosts, and services) is done to them
    
    var createSudoUg = createUserGroup(ugSudoName,"Sudo user group for service instance: " + serviceUUID);
    var createSudoHg = createHostGroup(hgSudoName, "Sudo host group for service instance: " + serviceUUID);
    var createSudoHbac = createHBACRule(hbacSudoName,"Sudo HBAC Rule (login,ssh,sudo) for service instance: " + serviceUUID);
    var getSudoHosts = getHostsForServiceInstance(serviceUUID);
    
    return $.when(createSudoUg, createSudoHg, createSudoHbac, getSudoHosts).done(function(ug, hg, hbac, hostsQuery) {
        var ugError = ug[0]["error"];
        var hgError = hg[0]["error"];
        var hbacError = hbac[0]["error"];
        var hostsQueryError = true;        
        
        console.log("**** sudo getHostsForServiceInstance hostsQuery raw: " + hostsQuery);
        console.log("*sudo getHostsForServiceInstance first element in array: " + JSON.stringify(hostsQuery[0]));
        console.log("**sudo getHostsForServiceInstance: " + JSON.stringify(hostsQuery));
        // verify and parse the loginHosts data
        // looks like below
        //{ serviceUUID: "5578c890-9a1c-4e23-9686-ab70ad274a92", jsonTemplate: "{\"hostgroup\":[{\"hostname\":\"180-146.research.maxgigapop.net\"}]}", jsonModel: null }
        
        // just to verify the right data is gotten
        if (hostsQuery[0]["serviceUUID"] === serviceUUID) {
            hostsQueryError = false;
            aclSudoPolicyResult["RecievedRightHostsForServiceInstance"] = true;
            var parsed = JSON.parse(hostsQuery[0]["jsonTemplate"]);
            var hostsObjs = parsed["hostgroup"];
            hostsObjs.forEach(function(h) {
                hosts.push(h["hostname"]);
            });
            console.log("parsed login hosts: " + hosts);
        } else {
            aclSudoPolicyResult["RecievedRightHostsForServiceInstance"] = false;
            aclSudoPolicyResult["RecievedRightHostsForServiceInstanceError"] = JSON.stringify(hostsQuery);
        }
        
        // if error for ipa requests is null, then the request was successful
        
        if (ugError === null) {
            aclSudoPolicyResult["CreatedSudoUserGroup"] = true;
        } else {
            aclSudoPolicyResult["CreatedSudoUserGroup"] = false;
            aclSudoPolicyResult["CreatedSudoUserGroupError"] = ugError;
        }
        
        if (hgError === null) {
            aclSudoPolicyResult["CreatedSudoHostGroup"] = true;
        } else {
            aclSudoPolicyResult["CreatedSudoHostGroup"] = false;
            aclSudoPolicyResult["CreatedSudoHostGroupError"] = hgError;
        }
        
        if (hbacError === null) {
            aclSudoPolicyResult["CreatedSudoHBACRule"] = true;
        } else {
            aclSudoPolicyResult["CreatedSudoHBACRule"] = false;
            aclSudoPolicyResult["CreatedSudoHBACRuleError"] = hbacError;
        }
        
        // if no errors in all three -> null is a falsy value
        if (!ugError && !hgError && !hbacError && !hostsQueryError) {
            aclSudoPolicyResult["SudoGroupAndRuleCreatedAndRightHostsFound"] = true;
        }
        
    }).then(function() {
        
        console.log("in then function sudo hosts: " + hosts);
        
        var addSudoUgUsers = addUsersToUserGroup(username, ugSudoName);
        var addSudoHgHosts = addHostsToHostGroup(hosts, hgSudoName);
        var addSudoUgToHbac = addUserGroupToHBACRule(ugSudoName,hbacSudoName);
        var addSudoHgToHbac = addHostGroupToHBACRule(hgSudoName,hbacSudoName);
        var addSudoSrvcsToHbac = addServicesToHBACRule(sudoServices, hbacSudoName);
        
        return $.when(addSudoUgUsers, addSudoHgHosts, addSudoUgToHbac, addSudoHgToHbac, addSudoSrvcsToHbac)
                .then(function(ugusers, hghosts, ughbac, hghbac, srvcshbac) {
                    var ugusersError = ugusers[0]["error"];
                    var hghostsError = hghosts[0]["error"];
                    var ughbacError = ughbac[0]["error"];
                    var hghbacError = hghbac[0]["error"];
                    var srvcshbacError = srvcshbac[0]["error"];
                    
                    if (ugusersError === null) {
                        aclSudoPolicyResult["AddedUsersToSudoUserGroup"] = true;
                    } else {
                        aclSudoPolicyResult["AddedUsersToSudoUserGroup"] = false;
                        aclSudoPolicyResult["AddedUsersToSudoUserGroupError"] = ugusersError;
                    }
                    
                    if (hghostsError === null) {
                        aclSudoPolicyResult["AddedHostsToSudoHostGroup"] = true;
                    } else {
                        aclSudoPolicyResult["AddedHostsToSudoHostGroup"] = false;
                        aclSudoPolicyResult["AddedHostsToSudoHostGroupError"] = hghostsError;
                    }
                    
                    if (ughbacError === null) {
                        aclSudoPolicyResult["AddedSudoUserGroupToSudoHBAC"] = true;
                    } else {
                        aclSudoPolicyResult["AddedSudoUserGroupToSudoHBAC"] = false;
                        aclSudoPolicyResult["AddedSudoUserGroupToSudoHBACError"] = ughbacError;
                    }
                    
                    if (hghbacError === null) {
                        aclSudoPolicyResult["AddedSudoHostGroupToSudoHBAC"] = true;
                    } else {
                        aclSudoPolicyResult["AddedSudoHostGroupToSudoHBAC"] = false;
                        aclSudoPolicyResult["AddedSudoHostGroupToSudoHBACError"] = hghbacError;
                    }
                    
                    if (srvcshbacError === null) {
                        aclSudoPolicyResult["AddedSudoServicesToSudoHBAC"] = true;
                    } else {
                        aclSudoPolicyResult["AddedSudoServicesToSudoHBAC"] = false;
                        aclSudoPolicyResult["AddedSudoServicesToSudoHBACError"] = srvcshbacError;
                    }
                    
                    
                    if (!srvcshbacError && !ugusersError && !hghbacError && !ughbacError) {
                        aclSudoPolicyResult["AddedSudoGroupAndServicesToSudoHBAC"] = true;
                    }
                    return aclSudoPolicyResult;
                }).fail(function(err) {
                    console.log("IPA ACL Sudo policy creation failed: " + JSON.stringify(err));
                });
    }).fail(function(err) {
        console.log("IPA ACL Sudo policy creation failed: " + JSON.stringify(err));
    });  
}

/**
 * Searches for the username in the specified ACL Policy
 * @param {String} serviceUUID
 * @param {String} accessType
 * @param {String} username 
 * @returns {jqXhr}
 */
function isUserInAclPolicy(serviceUUID, accessType, username) {
    var apiUrl = baseUrl + '/StackV-web/restapi/app/acl/ipa/request';
    
    // creating the IPA request
    /**
     * Try to find an user (hence user_find) that is in both the correct
     * user group and hbac rule
     */
    var ipaRequestData = {
        "method":"user_find",
        "params":[
            [username],
            {
                "in_group": [
                    "ug-" + accessType + "-" + serviceUUID
                ],
                "in_hbacrule": [
                    "hbac-" + accessType + "-" + serviceUUID
                ]
            }
        ],
        "id":0
    };
    
    
    // ajax call fields
    // future use: in the beforeSend field, if false is return the request will be cancelled. Can be used to check if the user is logged in
    var ipaAjaxCall = {
        "url": apiUrl,
        "method": "POST",
        "headers": {
            "Content-Type": "application/json",
            "Authorization": "bearer " + keycloak.token
        },
        "data": JSON.stringify(ipaRequestData)
    };
    
    return $.ajax(ipaAjaxCall);
}

/**
 * Checks if the ACL policy - given the service instance UUID and access type - exists.
 * Simply queries to see if the HBAC rule was at least created, does not check if 
 * user groups, host groups, or services have been added. (However, the whole HBAC rule
 * with groups and services should already be added if the HBAC rule was created)
 * @param {type} serviceUUID
 * @param {type} accessType
 * @returns {jqXHR}
 */
function checkForExistingACLPolicy(serviceUUID, accessType) {
    var apiUrl = baseUrl + '/StackV-web/restapi/app/acl/ipa/request';
    var hbacrule = "hbac-" + accessType + "-" + serviceUUID;
    
    
    // creating the IPA request
    var ipaRequestData = {
        "method":"hbacrule_find",
        "params":[
            [hbacrule],
            {}
        ],
        "id":0
    };
    
    
    // ajax call fields
    // future use: in the beforeSend field, if false is return the request will be cancelled. Can be used to check if the user is logged in
    var ipaAjaxCall = {
        "url": apiUrl,
        "method": "POST",
        "headers": {
            "Content-Type": "application/json",
            "Authorization": "bearer " + keycloak.token
        },
        "data": JSON.stringify(ipaRequestData)
    };
    
    return $.ajax(ipaAjaxCall);
}

/**
 * Removes the specified user from the user group, effectively removing them from
 * the ACL policy
 * @param {type} username
 * @param {type} serviceUUID
 * @param {type} accessType
 * @returns {undefined}
 */
function removeUserFromACLPolicy(username, serviceUUID, accessType) {
    var apiUrl = baseUrl + '/StackV-web/restapi/app/acl/ipa/request';
    var usergroup = "ug-" + accessType + "-" + serviceUUID;
    
    if (!Array.isArray(username)) {
        username = [username];
    }
    
    // creating the IPA request
    var ipaRequestData = {
        "method":"group_remove_member",
        "params":[
            [usergroup],
            {"user":username}
        ],
        "id":0
    };    
    
    // ajax call fields
    // future use: in the beforeSend field, if false is return the request will be cancelled. Can be used to check if the user is logged in
    var ipaAjaxCall = {
        "url": apiUrl,
        "method": "POST",
        "headers": {
            "Content-Type": "application/json",
            "Authorization": "bearer " + keycloak.token
        },
        "data": JSON.stringify(ipaRequestData)
    };
    
    return $.ajax(ipaAjaxCall);
}


function deleteHostGroup(hostgroupName) {
    var apiUrl = baseUrl + '/StackV-web/restapi/app/acl/ipa/request';
    
    if (!Array.isArray(hostgroupName)) {
        hostgroupName = [hostgroupName];
    }
    
    // creating the IPA request
    var ipaRequestData = {
        "method":"hostgroup_del",
        "params":[
            hostgroupName,
            {}
        ],
        "id":0
    };    
    
    // ajax call fields
    // future use: in the beforeSend field, if false is return the request will be cancelled. Can be used to check if the user is logged in
    var ipaAjaxCall = {
        "url": apiUrl,
        "method": "POST",
        "headers": {
            "Content-Type": "application/json",
            "Authorization": "bearer " + keycloak.token
        },
        "data": JSON.stringify(ipaRequestData)
    };
    
    return $.ajax(ipaAjaxCall);
}

function deleteUserGroup(usergroupName) {
    var apiUrl = baseUrl + '/StackV-web/restapi/app/acl/ipa/request';
    
    if (!Array.isArray(usergroupName)) {
        usergroupName = [usergroupName];
    }
    
    // creating the IPA request
    var ipaRequestData = {
        "method":"group_del",
        "params":[
            usergroupName,
            {}
        ],
        "id":0
    };    
    
    // ajax call fields
    // future use: in the beforeSend field, if false is return the request will be cancelled. Can be used to check if the user is logged in
    var ipaAjaxCall = {
        "url": apiUrl,
        "method": "POST",
        "headers": {
            "Content-Type": "application/json",
            "Authorization": "bearer " + keycloak.token
        },
        "data": JSON.stringify(ipaRequestData)
    };
    
    return $.ajax(ipaAjaxCall);
}

function deleteHBACRule(hbacruleName) {
    var apiUrl = baseUrl + '/StackV-web/restapi/app/acl/ipa/request';
    
    if (!Array.isArray(hbacruleName)) {
        hbacruleName = [hbacruleName];
    }
    
    // creating the IPA request
    var ipaRequestData = {
        "method":"hbacrule_del",
        "params":[
            hbacruleName,
            {}
        ],
        "id":0
    };    
    
    // ajax call fields
    // future use: in the beforeSend field, if false is return the request will be cancelled. Can be used to check if the user is logged in
    var ipaAjaxCall = {
        "url": apiUrl,
        "method": "POST",
        "headers": {
            "Content-Type": "application/json",
            "Authorization": "bearer " + keycloak.token
        },
        "data": JSON.stringify(ipaRequestData)
    };
    
    return $.ajax(ipaAjaxCall);
}

// Delete the ACL Policy of the specified access when the service instance is cancelled.
function removeACLPolicy(serviceUUID, accessType) {
    var ugName = "ug-" + accessType + "-" + serviceUUID;
    var hgName = "hg-" + accessType + "-" + serviceUUID;
    var hbacName = "hbac-" + accessType + "-" + serviceUUID;
    var removeAclPolicyResult = {}; // currently a way to debug errors
    
    // need to change it so when all the ajax calls are done - then return the aclSudoPolicyResult
    
    // the user group, host group, hbac rule all need to created before any
    // (mainly adding users, hosts, and services) is done to them
    
    var deleteUg = deleteUserGroup(ugName);
    var deleteHg = deleteHostGroup(hgName);
    var deleteHbac = deleteHBACRule(hbacName);
    
    return $.when(deleteUg, deleteHg, deleteHbac).done(function(delUg, delHg, delHbac) {
        removeAclPolicyResult["DeletedUserGroup"] = JSON.stringify(delUg);
        removeAclPolicyResult["DeletedHostGroup"] = JSON.stringify(delHg);
        removeAclPolicyResult["DeletedHBAC"] = JSON.stringify(delHbac);
        console.log("Remove ACL policy: " + JSON.stringify(removeAclPolicyResult));
        return removeAclPolicyResult;
    });
}

/**
 * Removes both types (login and sudo) ACL policies
 * @param {type} serviceUUID
 * @returns {undefined}
 */
function removeAllACLPolicies(serviceUUID) {
    var removedAllACLPolicies = {};
    
    var removeLoginPolicy = removeACLPolicy(serviceUUID,"login");
    var removeSudoPolicy = removeACLPolicy(serviceUUID,"sudo");
    
    return $.when(removeLoginPolicy, removeSudoPolicy).done(function(rmLogin, rmSudo) {
        removedAllACLPolicies["RemovedLoginACLPolicy"] = JSON.stringify(rmLogin);
        removedAllACLPolicies["RemovedSudoACLPolicy"] = JSON.stringify(rmSudo);
        console.log("Remove All ACL policies: " + JSON.stringify(removedAllACLPolicies));
        return removedAllACLPolicies;
    });
}


function subloadInstanceACLTable(refUUID) {
    tweenInstanceACLPanel.reverse();
    setTimeout(function () {
        keycloak.updateToken(30).success(function () {
            var apiUrl = baseUrl + '/StackV-web/restapi/app/acl/' + refUUID;
            var tbody = document.getElementById("acl-body");
            tbody.innerHTML = "";

            $("#acl-instance").val(refUUID);
            $.ajax({
                url: apiUrl,
                type: 'GET',
                beforeSend: function (xhr) {
                    xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
                },
                success: function (result) {
                    var allowedUsers = [];
                    if (result) {                        
                        for (i = 0; i < result.length; i++) {
                            var user = result[i];
                            allowedUsers.push(user[0]);

                            var row = document.createElement("tr");
                            
                            var cell1_1 = document.createElement("td");
                            cell1_1.innerHTML = user[0]; // username
                                                        
                            var cell1_2 = document.createElement("td");
                            cell1_2.innerHTML = user[1]; // full name
                            
                            var cell1_3 = document.createElement("td");
                            cell1_3.innerHTML = user[2]; // email
                            
                            var cell1_4 = document.createElement("td"); // remove button
                            cell1_4.innerHTML = '<button data-username="' + user[0] + '" class="button-acl-remove btn btn-default pull-right">Remove</button>';
                            
                            var cell1_5 = document.createElement("td"); // login checkbox
                            var checkBoxLogin = document.createElement("input");
                            checkBoxLogin.id = "loginaccess-" + user[0];
                            checkBoxLogin.style = "display: block; margin: 0 auto";
                            checkBoxLogin.setAttribute("data-access", "login");
                            checkBoxLogin.setAttribute("data-username", user[0]);
                            checkBoxLogin.type = "checkbox";
                            var uuid = $("#instance-body > tr.acl-instance-selected-row").attr("data-uuid");                                                        
                            
                            
                            // NOTE: whole ACL process relies on the username displayed in the table to the username in the IPA server
                            
                            checkBoxLogin.onclick = function () {                                
                                var uuid = $("#instance-body > tr.acl-instance-selected-row").attr("data-uuid");
                                var username = this.getAttribute("data-username");                             
                                if (this.checked) {                                                                
                                    swal({
                                        title: "Adding " + username + "...",
                                        text: "Please wait",
                                        icon: "../img/ajax-loader.gif",
                                        buttons: false
                                    });
                                    // ensure the user is logged in before making any IPA requests
                                    $.when(ipaLogin()).done(function() {
                                        $.when(checkForExistingACLPolicy(uuid, "login")).done(function(existsRes) {
                                            /**
                                             * Check before creation if a policy already exists
                                             * If it exists, then creation will fail. Instead just add
                                             * the user to the existing policy
                                             */                                            

                                            // if no ACL policy exists, then create it
                                            if (existsRes["result"]["count"] === 0) {
                                                 createLoginAclPolicy(uuid,username).done(function(result) {
                                                     console.log("after createLoginPolicy result: " + JSON.stringify(result));
                                                     if (result["LoginGroupAndRuleCreatedAndRightHostsFound"] === true && result["AddedLoginGroupAndServicesToLoginHBAC"] === true) {
                                                         swal("Login ACL Policy Created Successfully!", "Added " + username + " to the Login ACL Policy", "success");
                                                     } else {
                                                         swal("Login ACL Policy Creation Failed!", "Error: " + JSON.stringify(result), "error");
                                                     }
                                                 });
                                            } else {
                                                // just add the user to the existing policy by adding them to the right user group
                                                var usergroupLogin = "ug-login-" + uuid;
                                                addUsersToUserGroup(username, usergroupLogin).done(function(result) {                                                    
                                                    if (result["error"] === null) {
                                                       swal({
                                                           title: "Added " + username + " to the Login ACL Policy!",
                                                           icon: "success"
                                                       });
                                                    } else {
                                                        swal("Could not add " + username + " to the Login ACL Policy", "Error: " + JSON.stringify(result), "error");
                                                    }
                                                 });
                                            }
                                            
                                                                                       
                                        });                                                                                                                      
                                    });
                                } else {
                                    swal({
                                        title: "Removing " + username + "...",
                                        text: "Please wait",
                                        icon: "../img/ajax-loader.gif",
                                        buttons: false
                                    });
                                    $.when(ipaLogin()).done(function() {
                                        removeUserFromACLPolicy(username, uuid, "login").done(function(result) {
                                            // if no error
                                            if (result["error"] === null) {
                                                swal({
                                                    title: "Removed " + username + " from Login ACL Policy",
                                                    icon: "success"
                                                });
                                            } else {
                                                swal("Not able to remove " + username + " from Login ACL Policy", "Error: " + JSON.stringify(result), "error");
                                            }

                                            // if the login checkbox is unchecked, 
                                            // then uncheck (if checked) the sudo checkbox (which should remove the sudo access for the user)
                                            var sudoCheckbox = document.getElementById("sudoaccess-" + username);
                                            console.log("sudo check box checked?: " + sudoCheckbox.checked);
                                            if (sudoCheckbox.checked) {
                                                // there are browser caveats to .click() (https://developer.mozilla.org/en-US/docs/Web/API/HTMLElement/click)
                                                sudoCheckbox.click();
                                                
                                                // tried setting the checkbox to boolean but this wouldn't run the onclick function 
                                                // and neither would using jquery's triggers function
                                            }
                                        
                                        });                                    
                                    });                                    
                                }                                
                            };
                            
                            cell1_5.appendChild(checkBoxLogin);
                            
                            var cell1_6 = document.createElement("td"); // sudo checkbox
                            var checkBoxSudo = document.createElement("input");
                            checkBoxSudo.id = "sudoaccess-" + user[0];
                            checkBoxSudo.style = "display: block; margin: 0 auto";
                            checkBoxSudo.setAttribute("data-access", "sudo");
                            checkBoxSudo.setAttribute("data-username", user[0]);
                            checkBoxSudo.type = "checkbox";                            
                            
                            checkBoxSudo.onclick = function () {
                                var uuid = $("#instance-body > tr.acl-instance-selected-row").attr("data-uuid");
                                var username = this.getAttribute("data-username");
                                if (this.checked) {                                                                        
                                    
                                    swal({
                                        title: "Adding " + username + "...",
                                        text: "Please wait",
                                        icon: "../img/ajax-loader.gif",
                                        buttons: false
                                    });
                                    // ensure the user is logged in before making any IPA requests
                                    $.when(ipaLogin()).done(function() {
                                        $.when(checkForExistingACLPolicy(uuid, "sudo")).done(function(existsRes) {
                                            
                                            /**
                                             * Check before creation if a policy already exists
                                             * If it exists, then creation will fail. Instead just add
                                             * the user to the existing policy
                                             */                                           

                                            // if no ACL policy exists, then create it
                                            if (existsRes["result"]["count"] === 0) {
                                                 createSudoAclPolicy(uuid,username).done(function(result) {                                                     
                                                     if (result["SudoGroupAndRuleCreatedAndRightHostsFound"] === true && result["AddedSudoGroupAndServicesToSudoHBAC"] === true) {
                                                         swal("Sudo ACL Policy Created Successfully", "Added " + username + " to the Sudo ACL Policy", "success");
                                                     } else {
                                                         swal("Sudo ACL Policy Creation Failed", "Error: " + JSON.stringify(result), "error");
                                                     }
                                                 });
                                            } else {
                                                // just add the user to the existing policy by adding them to the right user group
                                                var usergroupSudo = "ug-sudo-" + uuid;
                                                addUsersToUserGroup(username, usergroupSudo).done(function(result) {                                                    
                                                   if (result["error"] === null) {
                                                       swal({
                                                           title: "Added " + username + " to the Sudo ACL Policy",
                                                           icon: "success"
                                                       });
                                                   } else {
                                                       swal("Could not add " + username + " to Sudo ACL Policy", "Error: " + JSON.stringify(result), "error");
                                                   }
                                                });
                                            }
                                            
                                            // if the sudo checkbox is checked, 
                                            // then check the login checkbox (which should autorun the login ACL policy creation)
                                            var loginCheckbox = document.getElementById("loginaccess-" + username);
                                            console.log("login check box checked?: " + loginCheckbox.checked);
                                            if (loginCheckbox.checked === false) {                                                
                                                // there are browser caveats to .click() (https://developer.mozilla.org/en-US/docs/Web/API/HTMLElement/click)
                                                loginCheckbox.click();
                                                
                                                // tried setting the checkbox to boolean but this wouldn't run the onclick function 
                                                // and neither would using jquery's triggers function
                                            }
                                                                                       
                                        });                                                                                                                      
                                    });
                                } else {
                                    swal({
                                        title: "Removing " + username + "...",
                                        text: "Please wait",
                                        icon: "../img/ajax-loader.gif",
                                        buttons: false
                                    });
                                    $.when(ipaLogin()).done(function() {
                                        removeUserFromACLPolicy(username, uuid, "sudo").done(function(result) {                                                                                
                                            // if no error
                                            if (result["error"] === null) {
                                                swal({
                                                    title: "Removed " + username + " from Sudo ACL Policy",
                                                    icon: "success"
                                                });
                                            } else {
                                                swal("Not able to remove " + username + " from Sudo ACL Policy", "Error: " + JSON.stringify(result), "error");
                                            }
                                                                                        
                                        });
                                    });
                                }
                                
                                
                            };
                            
                            cell1_6.appendChild(checkBoxSudo);
                            
                            row.appendChild(cell1_1); //username
                            row.appendChild(cell1_2); //full name                            
                            row.appendChild(cell1_3); //email
                            row.appendChild(cell1_5); //login access checkbox
                            row.appendChild(cell1_6); //sudo access checkbox
                            row.appendChild(cell1_4); //remove button
                            tbody.appendChild(row);

                            $(".button-acl-remove").click(function (evt) {
                                var username = $(this).data("username");
                                var refUUID = $("#acl-instance").val();

                                var apiUrl = baseUrl + '/StackV-web/restapi/app/acl/' + refUUID;
                                keycloak.updateToken(30).success(function () {
                                    $.ajax({
                                        url: apiUrl,
                                        type: 'DELETE',
                                        data: username,
                                        contentType: "application/json; charset=utf-8",
                                        dataType: "json",
                                        beforeSend: function (xhr) {
                                            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
                                        },
                                        success: function (result) {
                                            subloadInstanceACLTable(refUUID);
                                        }
                                    });
                                }).error(function () {
                                    console.log("Fatal Error: Token update failed!");
                                });

                                evt.preventDefault();
                            });
                        }

                        var userRows = $("#users-body tr");
                        userRows.removeClass("hide");
                        for (userindex = 0; userindex < userRows.length; ++userindex) {
                            var aclRows = $("#acl-body tr");
                            for (aclindex = 0; aclindex < aclRows.length; ++aclindex) {
                                if (userRows[userindex].firstChild.innerHTML === aclRows[aclindex].firstChild.innerHTML) {
                                    userRows[userindex].className = "hide";
                                }
                            }
                        }                                                                        
                        tweenInstanceACLPanel.play();
                    }
                    allowedUsers.forEach(function(u) {
                           // check whether the user is already added to the ACL policy and check the checkbox accordingly                            
                            $.when(ipaLogin()).done(function() {                                
                                isUserInAclPolicy(uuid, "login", u).done(function(aclResult) {                                    
                                    var count = aclResult["result"]["count"];
                                    
                                    // only one user should be matched
                                    if (count === 1) {
                                        // the uid of the user is stored as a list
                                        var uidList = aclResult["result"]["result"][0]["uid"];                                        
                                        if (uidList.includes(u)) {                                            
                                            $('#loginaccess-' + u).prop('checked', true);
                                        } else {                                            
                                            $('#loginaccess-' + u).prop('checked', false);
                                        }
                                    }                                                                                                     
                                });
                                
                                isUserInAclPolicy(uuid, "sudo", u).done(function(aclResult) {                                    
                                    var count = aclResult["result"]["count"];
                                    
                                    // only one user should be matched
                                    if (count === 1) {
                                        // the uid of the user is stored as a list
                                        var uidList = aclResult["result"]["result"][0]["uid"];                                        
                                        if (uidList.includes(u)) {                                            
                                            $('#sudoaccess-' + u).prop('checked', true);
                                        } else {                                            
                                            $('#sudoaccess-' + u).prop('checked', false);
                                        }
                                    }                                                                                                     
                                }); 
                            }); 
                        });
                }
            });
        }).error(function () {
            console.log("Fatal Error: Token update failed!");
        });
    }, 500);



}
function subloadRoleACLRoles1() {
    var apiUrl = baseUrl + '/StackV-web/restapi/app/keycloak/roles';
    keycloak.updateToken(30).success(function () {
        $.ajax({
            url: apiUrl,
            type: 'GET',
            beforeSend: function (xhr) {
                xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
            },
            success: function (result) {
                $("#acl-role-select1").find('option').remove().end();
                for (i = 0; i < result.length; i++) {
                    var role = result[i];

                    $("#acl-role-select1").append(new Option(role[1], role[0]));
                }
            }
        });
    }).error(function () {
        console.log("Fatal Error: Token update failed!");
    });
}

function loadGroupTable(groupname) {
    var group = groupname;

    //get the group




    var tbody = document.getElementById("group-role-body");
    var tbody1 = $("#group-role-body");
    tbody.innerHTML = "";

    tbody1.empty();
    tbody1.html("");

    $("#acl-group-role-table > tbody").html("");

    var apiUrl = baseUrl + "/StackV-web/restapi/app/keycloak/groups/" + group;
    alert(apiUrl);
    $.ajax({
        url: apiUrl,
        type: 'GET',
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
            xhr.setRequestHeader("Refresh", keycloak.refreshToken);
        },
        success: function (result) {

            tbody.innerHTML = "";
            tbody1.empty();
            tbody1.html("");

            for (i = 0; i < result.length; i++) {
                var role = result[i];

                var row = document.createElement("tr");

                var cell1_1 = document.createElement("td");
                if (role[2] === "assigned") {
                    cell1_1.innerHTML = role[1] + '<button data-roleid="' + role[0] + '" data-rolename="' + role[1] + '" class="button-role-delete btn btn-default pull-right">Remove</button>';
                } else {
                    cell1_1.innerHTML = role[1] + '<button data-rolename="' + role[1] + '" class="button-role-delete1 btn btn-default pull-right">Remove</button>';
                }

                row.appendChild(cell1_1);
                tbody.appendChild(row);


            }

            subloadRoleACLRoles1();
            tweenHideUserPanel.play();
            tweenGroupRolePanel.play();

            $(".button-role-delete1").click(function (evt) {


                var option = $(this).data("rolename");
                var apiUrl = baseUrl + '/StackV-web/restapi/app/keycloak/roles/' + option;


                $.ajax({
                    url: apiUrl,
                    type: 'GET',
                    beforeSend: function (xhr) {
                        xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
                    },
                    success: function (result) {
                        var str = String(result).split(",");
                        var output = "[{ \"id\": \"" + str[0] + "\", \"name\": \"" + str[1] + "\",\"scopeParamRequired\": " + str[2] + ", \"composite\": " + str[3] + ", \"clientRole\":" + str[4] + ",\"containerId\":\"" + str[5] + "\"}]";

                        var apiUrl2 = baseUrl + '/StackV-web/restapi/app/keycloak/groups/' + groupname;

                        $.ajax({
                            url: apiUrl2,
                            type: 'DELETE',
                            data: output,
                            contentType: "application/json; charset=utf-8",
                            dataType: "json",
                            beforeSend: function (xhr) {
                                xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
                                xhr.setRequestHeader("Refresh", keycloak.refreshToken);
                            },
                            success: function () {
                                loadGroupTable(groupname);
                            }
                        });
                    }
                });
            });

            $(".acl-group-roles").click(function (evt) {
                tweenHideUserPanel.reverse();
                tweenGroupRolePanel.reverse();
                evt.preventDefault();
            });

            $("#acl-group-role-add").click(function () {
                var option = $('#acl-role-select1').children("option:selected").text();

                var apiUrl = baseUrl + '/StackV-web/restapi/app/keycloak/roles/' + option;

                $.ajax({
                    url: apiUrl,
                    type: 'GET',
                    beforeSend: function (xhr) {
                        xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
                    },
                    success: function (result) {
                        var str = String(result).split(",");
                        var output = "[{ \"id\": \"" + str[0] + "\", \"name\": \"" + str[1] + "\",\"scopeParamRequired\": " + str[2] + ", \"composite\": " + str[3] + ", \"clientRole\":" + str[4] + ",\"containerId\":\"" + str[5] + "\"}]";

                        var apiUrl2 = baseUrl + '/StackV-web/restapi/app/keycloak/groups/' + groupname;

                        $.ajax({
                            url: apiUrl2,
                            type: 'POST',
                            data: output,
                            contentType: "application/json; charset=utf-8",
                            dataType: "json",
                            beforeSend: function (xhr) {
                                xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
                                xhr.setRequestHeader("Refresh", keycloak.refreshToken);
                            },
                            success: function () {
                                loadGroupTable(groupname);
                            }
                        });




                    }
                });
            });



        },
        error: function () {
            alert("failure");
        }

    });


}


