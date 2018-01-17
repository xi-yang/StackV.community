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

var ipaServerCookieValid = false;

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

/**
 * Check if the user is currently logged into IPA server. If not logged in, the log in the user
 */
function ipaLogin(){
    var apiUrl = baseUrl + "/StackV-web/restapi/app/acl/ipa/login";
    
    return $.ajax({
        url: apiUrl,
        type: 'POST',
        data: {
            "username":"admin",
            "password":"max1$fun"
        },
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
        },
        success: function(result) {
            console.log("ipaLogin success: " + JSON.stringify(result));
        },
        error: function(err) {
            console.log("ipaLogin error: " + JSON.stringify(err));
        }
    });
}

/**
 * Call the checkcookie endpoint to see if cookie is still valid (i.e. the user is logged in)
 * If the cookie is expired, the method automatically calls the login function
 * @returns {jqXHR} return the ajax object so it can used with $.when
 */
function checkIpaServerCookie() {
    var apiUrl = baseUrl + "/StackV-web/restapi/app/acl/ipa/checkcookie";
    
    return $.ajax({
        url: apiUrl,
        type: 'GET',
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
        },
        success: function(result) {
            console.log("checkIpaServerCookie success -> result: " + JSON.stringify(result));
            console.log("checkIpaServerCookie expired: " + result["CookieExpired"]);
            if (result["CookieExpired"]) {
                ipaServerCookieValid = false;
            } else {
                ipaServerCookieValid = true;
            }
        },
        error: function(err) {
            console.log("checkIpaServerCookie error -> err: " + JSON.stringify(err));
        }
    });
}

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
 * Changes the Login Access to a service instance (identified by the UUID) for
 * the given username based on grantAccess value
 * @param {string} serviceUUID
 * @param {string} username
 * @param {boolean} grantAccess
 * @returns {boolean} indicates if the change was successful
 */
function changeLoginAccess(serviceUUID, username, grantAccess) {
    console.log("in changeLoginAccess: serviceUUID -> " + serviceUUID + ", username -> " + username + ", grantAccess -> " + grantAccess);
    var changeSuccess = false;
    if (grantAccess) {
        
    } else {
        
    }
    return changeSuccess;
}

/**
 * Changes the Sudo Access to a service instance (identified by the UUID) for
 * the given username based on grantAccess value
 * @param {string} serviceUUID
 * @param {string} username
 * @param {boolean} grantAccess
 * @returns {boolean} indicates if the change was successful
 */
function changeSudoAccess(serviceUUID, username, grantAccess) {
    console.log("in changeSudoAcess: serviceUUID -> " + serviceUUID + ", username -> " + username + ", grantAccess -> " + grantAccess);
    var changeSuccess = false;
    if (grantAccess) {
        
    } else {
        
    }
    return changeSuccess;
}

/**
 * Creates the UserGroup for the specified service with given group name
 * @param {string} serviceUUID
 * @param {string} groupName
 * @param {string} desc 
 * @returns {boolean} returns if the request was successful
 */
function createUserGroupForService(serviceUUID, groupName, desc) {
    console.log("in createUserGroupForService: serviceUUID -> " + serviceUUID + ", groupName -> " + groupName + ", description -> " + desc);
    
    var apiUrl = baseUrl + '/StackV-web/restapi/app/acl/ipa/request';
    var formattedGroupName = serviceUUID + "-" + groupName;
    
    
    // creating the IPA request
    var postData = {
        "method":"group_add",
        "params":[
            [formattedGroupName],
            {"description": desc}
        ],
        "id":0
    };
    
    console.log("createUserGroupService postData: " + JSON.stringify(postData));
    
    var settings = {
        "async": true,
        "crossDomain": true,
        "url": apiUrl,
        "method": "POST",
        "headers": {
            "Content-Type": "application/json",
            "Authorization": "bearer " + keycloak.token,
        },
        "processData": false,
        "data": JSON.stringify(postData),
    };
    
    // have to ensure the user is logged into the IPA server before sending any request
    $.when(checkIpaServerCookie()).done(function(ipaCookieRes){
        console.log("Done ipaCookie: " + ipaCookieRes);
        if (!ipaServerCookieValid) {
            $.when(ipaLogin()).done(function (ipaLoginRes) {
                console.log("Done ipaLogin in createUserGroupForService: " + ipaCookieRes);
                $.ajax(settings).done(function (res){
                    console.log("***ajax call done result: " + JSON.stringify(res));
                });
            });
        } else {
            $.ajax(settings).done(function (res){
                console.log("***ajax call done result: " + JSON.stringify(res));
            });
        }        
    });
    /*
    $.ajax({
        url: apiUrl,
        method: 'POST',
        data: JSON.stringify(postData),
        contentType: 'application/json',
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
        },
        success: function(result) {
            console.log("in createUserGroupForService success: " + JSON.stringify(result));
        },
        error: function(err) {
            console.log("createUserGroupForService error: " + JSON.stringify(err));
        }
    });
    */
    console.log("in createUserGroupForService end");
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
                    if (result) {
                        for (i = 0; i < result.length; i++) {
                            var user = result[i];

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
                            checkBoxLogin.style = "display: block; margin: 0 auto";
                            checkBoxLogin.setAttribute("data-access", "login");
                            checkBoxLogin.setAttribute("data-username", user[0]);
                            checkBoxLogin.type = "checkbox";
                            checkBoxLogin.onclick = function () {                                
                                var uuid = $("#instance-body > tr.acl-instance-selected-row").attr("data-uuid");
                                var username = this.getAttribute("data-username");
                                if (this.checked) {
                                    console.log("Checked the " + this.getAttribute("data-access") + " checkbox for username: " + username);
                                    var result = changeLoginAccess(uuid, username, true);
                                    // check if result returned succesfully and let the user know
                                    // if result is false, then the request was unable to be processed, so reset the checkbox
                                    
                                    //testing - create a user group
                                    createUserGroupForService(uuid,"testusergroup","Testing API call");
                                } else {
                                    console.log("Unchecked the " + this.getAttribute("data-access") + " checkbox for username: " + username);
                                    var result = changeLoginAccess(uuid, username, false);
                                }
                            }
                            cell1_5.appendChild(checkBoxLogin);
                            
                            var cell1_6 = document.createElement("td"); // sudo checkbox
                            var checkBoxSudo = document.createElement("input");                                                        
                            checkBoxSudo.style = "display: block; margin: 0 auto";
                            checkBoxSudo.setAttribute("data-access", "sudo");
                            checkBoxSudo.setAttribute("data-username", user[0]);
                            checkBoxSudo.type = "checkbox";
                            checkBoxSudo.onclick = function() {
                                var uuid = $("#instance-body > tr.acl-instance-selected-row").attr("data-uuid");
                                var username = this.getAttribute("data-username");
                                if (this.checked) {
                                    console.log("Checked the " + this.getAttribute("data-access") + " checkbox for username: " + username);
                                    var result = changeSudoAccess(uuid, username, true);
                                } else {
                                    console.log("Unchecked the " + this.getAttribute("data-access") + " checkbox for username: " + username);
                                    var result = changeSudoAccess(uuid, username, false);
                                }
                            }
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


