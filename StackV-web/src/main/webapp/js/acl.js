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
// ipa.js must be loaded in order to access the IPA ACL functions

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
        window.location.href = "/StackV-web/portal/driver/";
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
        
        $(".acl-instance-selected").removeClass("acl-instance-selected");

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

    var apiUrl = baseUrl + '/StackV-web/restapi/app/logging/instances';
    keycloak.updateToken(30).success(function () {
        $.ajax({
            url: apiUrl,
            type: 'GET',
            beforeSend: function (xhr) {
                xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
            },
            success: function (result) {
                if (result) {
                    for (i = 0; i < result.data.length; i++) {
                        var instance = result.data[i];

                        var row = document.createElement("tr");
                        row.className = "acl-instance-row";
                        row.setAttribute("data-uuid", instance["referenceUUID"]);

                        var cell1_1 = document.createElement("td");
                        cell1_1.innerHTML = instance["alias"];
                        var cell1_2 = document.createElement("td");
                        cell1_2.innerHTML = instance["type"];
                        var cell1_3 = document.createElement("td");
                        cell1_3.innerHTML = instance["referenceUUID"];
                        row.appendChild(cell1_1);
                        row.appendChild(cell1_2);
                        row.appendChild(cell1_3);
                        tbody.appendChild(row);
                    }

                    $(".acl-instance-row").click(function () {
                        $(".acl-instance-selected-row").removeClass("acl-instance-selected-row");

                        subloadInstanceACLTable($(this).data("uuid"));
                        
                        $(tbody).addClass("acl-instance-selected");

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
                            
                            checkBoxLogin.onmouseover = function() {
                                var uuid = $("#instance-body > tr.acl-instance-selected-row").attr("data-uuid");
                                var apiServiceStatusUrl = baseUrl + '/StackV-web/restapi/service/' + uuid + '/status';
                                
                                var ajaxSettings = {
                                    "url": apiServiceStatusUrl,
                                    "method": "GET",
                                    "headers": {
                                        "authorization": "bearer " + keycloak.token,
                                        "Refresh": keycloak.refreshToken
                                    }
                                };
                            
                                $.ajax(ajaxSettings).done(function(response) {
                                    if (response === "READY") {
                                        this.enable = true;
                                    } else {
                                        this.disable = true;
                                        swal("Service Instance is not yet ready","Please wait...", "warning");
                                    }
                                });
                            };
                            
                            // NOTE: whole ACL process relies on the username displayed in the table to the username in the IPA server
                            
                            checkBoxLogin.onclick = function () {                                
                                var uuid = $("#instance-body > tr.acl-instance-selected-row").attr("data-uuid");
                                var username = this.getAttribute("data-username");                                                                                                                                                                                                                               
                                
                                if (this.checked) {
                                    // show a loading alert, if after 15 seconds the process doesn't complete - show an error message
                                    swal({
                                        title: "Adding " + username + "...",
                                        text: "Please wait",
                                        icon: "/StackV-web/img/ajax-loader.gif",
                                        buttons: false,
                                        timer: 15000
                                    }).then(function(res) {
                                        swal("The ACL Request could not be completed.","Please try again.", "error");
                                        subloadInstanceACLTable(uuid);
                                        removeACLPolicy(uuid, "login"); //remove any completed steps
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
                                                     // success keys in the returned json (see ipa.js creation methods for json structure)
                                                     if (result["GroupAndRuleCreatedAndRightHostsFound"] === true && result["AddedUsersHostsToGroupAndServicesToRule"] === true) {
                                                         swal("Login ACL Policy Created Successfully!", "Added " + username + " to the Login ACL Policy", "success");
                                                     } else {
                                                         subloadInstanceACLTable(uuid);
                                                         swal("Login ACL Policy Creation Failed!", parseACLPolicyResult(result), "error");                                                         
                                                     }
                                                 });
                                            } else {
                                                // just add the user to the existing policy by adding them to the right user group
                                                var usergroupLogin = "ug-login-" + uuid;
                                                addUsersToUserGroup(username, usergroupLogin).done(function(result) {                                                   
                                                    if (result["error"] === null && result["result"]["completed"] === 1) {
                                                       swal({
                                                           title: "Added " + username + " to the Login ACL Policy!",
                                                           icon: "success"
                                                       });
                                                    } else {
                                                        subloadInstanceACLTable(uuid);
                                                        swal("Could not add " + username + " to the Login ACL Policy","Ensure " + username + " is registered with the IPA server.", "error");                                                        
                                                    }
                                                 });
                                            }


                                        }).fail(function(err) {
                                            // if something fails due not due to the content of the request
                                            subloadInstanceACLTable(uuid);
                                            swal("Request could not be completed","Error: "  + JSON.stringify(err), "error");
                                            removeACLPolicy(uuid, "login");                                            
                                        });                                                                                                                      
                                    });
                                } else {
                                    // show a loading alert, if after 15 seconds the process doesn't complete - show an error message
                                    swal({
                                        title: "Removing " + username + "...",
                                        text: "Please wait",
                                        icon: "/StackV-web/img/ajax-loader.gif",
                                        buttons: false,
                                        timer: 15000
                                    }).then(function(res) {
                                        swal("The ACL Request could not be completed.","Please try again.", "error");
                                        subloadInstanceACLTable(uuid);
                                        removeACLPolicy(uuid, "login"); //remove any completed steps
                                    });;
                                    $.when(ipaLogin()).done(function() {
                                        removeUserFromACLPolicy(username, uuid, "login").done(function(result) {
                                            // if no error
                                            if (result["error"] === null && result["result"]["completed"] === 1) {
                                                swal({
                                                    title: "Removed " + username + " from Login ACL Policy",
                                                    icon: "success"
                                                });
                                            } else {
                                                console.log("Could not remove " + username + "from Login ACL Policy. Error: " + JSON.stringify(result));
                                                subloadInstanceACLTable(uuid);
                                                swal("Not able to remove " + username + " from Login ACL Policy", "Ensure " + username + " is in the ACL policy", "error");                                                
                                            }

                                            // if the login checkbox is unchecked, 
                                            // then uncheck (if checked) the sudo checkbox (which should remove the sudo access for the user)
                                            var sudoCheckbox = document.getElementById("sudoaccess-" + username);                                            
                                            if (sudoCheckbox.checked) {
                                                // there are browser caveats to .click() (https://developer.mozilla.org/en-US/docs/Web/API/HTMLElement/click)
                                                sudoCheckbox.click();

                                                // tried setting the checkbox to boolean but this wouldn't run the onclick function 
                                                // and neither would using jquery's trigger function
                                            }

                                        });                                    
                                    }).fail(function(err) {
                                        subloadInstanceACLTable(uuid);
                                        swal("Request could not be completed","Error: "  + JSON.stringify(err), "error");
                                        removeACLPolicy(uuid, "login"); //remove any completed steps                                    
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
                            
                            checkBoxSudo.onmouseover = function() {
                                var uuid = $("#instance-body > tr.acl-instance-selected-row").attr("data-uuid");
                                var apiServiceStatusUrl = baseUrl + '/StackV-web/restapi/service/' + uuid + '/status';
                                
                                var ajaxSettings = {
                                    "url": apiServiceStatusUrl,
                                    "method": "GET",
                                    "headers": {
                                        "authorization": "bearer " + keycloak.token,
                                        "Refresh": keycloak.refreshToken
                                    }
                                };
                            
                                $.ajax(ajaxSettings).done(function(response) {
                                    if (response === "READY") {
                                        this.enable = true;
                                    } else {
                                        this.disable = true;
                                        swal("Service Instance is not yet ready","Please wait...", "warning");
                                    }
                                });
                            };
                            
                            checkBoxSudo.onclick = function () {                                                                                                                                 
                                var uuid = $("#instance-body > tr.acl-instance-selected-row").attr("data-uuid");
                                var username = this.getAttribute("data-username");
                                
                                if (this.checked) {
                                    // show a loading alert, if after 15 seconds the process doesn't complete - show an error message
                                    swal({
                                        title: "Adding " + username + "...",
                                        text: "Please wait",
                                        icon: "/StackV-web/img/ajax-loader.gif",
                                        buttons: false,
                                        timer: 15000
                                    }).then(function(res) {
                                        subloadInstanceACLTable(uuid);
                                        swal("The ACL Request could not be completed.","Please try again.", "error");
                                        removeACLPolicy(uuid, "sudo"); //remove any completed steps
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
                                                     if (result["GroupAndRuleCreatedAndRightHostsFound"] === true && result["AddedUsersHostsToGroupAndServicesToRule"] === true) {
                                                         swal("Sudo ACL Policy Created Successfully", "Added " + username + " to the Sudo ACL Policy", "success");
                                                     } else {
                                                         swal("Sudo ACL Policy Creation Failed", parseACLPolicyResult(result), "error");
                                                         $('#sudoaccess-' + username).attr("checked", false);
                                                     }
                                                 });
                                            } else {
                                                // just add the user to the existing policy by adding them to the right user group
                                                var usergroupSudo = "ug-sudo-" + uuid;
                                                addUsersToUserGroup(username, usergroupSudo).done(function(result) {                                                    
                                                   if (result["error"] === null && result["result"]["completed"] === 1) {
                                                       swal({
                                                           title: "Added " + username + " to the Sudo ACL Policy",
                                                           icon: "success"
                                                       });
                                                   } else {
                                                       subloadInstanceACLTable(uuid);
                                                       swal("Could not add " + username + " to Sudo ACL Policy", "Ensure " + username + " is registered with the IPA server.", "error");                                                       
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
                                                // and neither would using jquery's trigger function
                                            }

                                        }).fail(function(err) {
                                            subloadInstanceACLTable(uuid);
                                            swal("Request could not be completed","Error: "  + JSON.stringify(err), "error");                                            
                                            removeACLPolicy(uuid, "sudo");
                                        });                                                                                                                      
                                    });
                                } else {
                                    // show a loading alert, if after 15 seconds the process doesn't complete - show an error message
                                    swal({
                                        title: "Removing " + username + "...",
                                        text: "Please wait",
                                        icon: "/StackV-web/img/ajax-loader.gif",
                                        buttons: false,
                                        timer: 15000
                                    }).then(function(res) {
                                        swal("The ACL Request could not be completed.","Please try again.", "error");
                                        subloadInstanceACLTable(uuid);
                                        removeACLPolicy(uuid, "sudo"); //remove any completed steps
                                    });
                                    $.when(ipaLogin()).done(function() {
                                        removeUserFromACLPolicy(username, uuid, "sudo").done(function(result) {                                                                                
                                            // if no error
                                            if (result["error"] === null && result["result"]["completed"] === 1) {
                                                swal({
                                                    title: "Removed " + username + " from Sudo ACL Policy",
                                                    icon: "success"
                                                });
                                            } else {
                                                subloadInstanceACLTable(uuid);
                                                console.log("Could not remove " + username + "from Sudo ACL Policy. Error: " + JSON.stringify(result));
                                                swal("Not able to remove " + username + " from Sudo ACL Policy", "Ensure " + username + " is in the ACL policy", "error");                                                                                                
                                            }

                                        }).fail(function(err) {
                                            subloadInstanceACLTable(uuid);
                                            swal("Request could not be completed","Error: "  + JSON.stringify(err), "error");                                            
                                            removeACLPolicy(uuid, "sudo"); //remove any completed steps
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
                                
                                
                                /**
                                 * Since the user is being removed from the service instance itself,
                                 * the user should be removed from ACL policies they are currently in.
                                 * Steps:
                                 * 1) Get and check the checkboxes for login and sudo access to see if user
                                 * is in an ACL policy
                                 * 2) If the user is in an ACL policy, then remove them                                                          
                                 */
                                
                                // getting and checking the Login checkbox
                                var loginCheckbox = document.getElementById("loginaccess-" + username);                               
                                if (loginCheckbox.checked === true) {                                                
                                    // remove the user from any ACL policy associated with the service instance
                                    swal("Not able to remove " + username + " from Login ACL Policy", "Please uncheck the Login Access checkbox", "error");                         
                                }
                                                                                                
                                // getting and checking the Login checkbox
                                var sudoCheckbox = document.getElementById("sudoaccess-" + username);                                
                                if (sudoCheckbox.checked === true) {                                                
                                   swal("Not able to remove " + username + " from Sudo ACL Policy", "Please uncheck the Sudo Access checkbox", "error");                                    
                                } 
                                
                                if (loginCheckbox.checked === false && sudoCheckbox.checked === false) {
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
                                                $('.button-acl-remove[data-username="' + username + '"]').closest('tr').hide();                                                
                                                
                                                // compares which rows are the same in the acl and users tables and unhides the user row in the users table
                                                var userRows = $("#users-body tr");
                                                userRows.removeClass("hide");
                                                for (userindex = 0; userindex < userRows.length; ++userindex) {
                                                    var aclRows = $("#acl-body tr");
                                                    for (aclindex = 0; aclindex < aclRows.length; ++aclindex) {
                                                        if (userRows[userindex].firstChild.innerHTML === aclRows[aclindex].firstChild.innerHTML) {
                                                            userRows[userindex].className = "";
                                                        }
                                                    }
                                                }                                                                        
                                                tweenInstanceACLPanel.play();
                                            }
                                        });
                                    }).error(function () {
                                        console.log("Fatal Error: Token update failed!");
                                    });

                                evt.preventDefault();
                                }                                
                            });
                        }
                        
                        // compares which rows are the same in the acl and users tables and hides the user row in the users table
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
                    
                    // checks if the users in the table have been added to an ACL policy or not
                    allowedUsers.forEach(function(u) {
                           // check whether the user is already added to the ACL policy and check the checkbox accordingly                            
                            $.when(ipaLogin()).done(function() {                                
                                isUserInAclPolicy(refUUID, "login", u).done(function(aclResult) {                                    
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
                                
                                isUserInAclPolicy(refUUID, "sudo", u).done(function(aclResult) {                                    
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


