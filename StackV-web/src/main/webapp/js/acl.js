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

/* global XDomainRequest, baseUrl, keycloak */
var animating = false;

// ACL Load
function loadACLPortal() {
    subloadRoleACLUsers();
    subloadRoleACLGroups();
    subloadRoleACLRoles();

    subloadInstanceACLInstances();
    subloadInstanceACLUsers();

    $(".left-tab").click(function (evt) {
        if (!$("#acl-role-panel").hasClass("active")) {
            $(".active").removeClass("active");
            $("#acl-role-panel").addClass("active");
        }

        evt.preventDefault();
    });
    $(".right-tab").click(function (evt) {
        if (!$("#acl-instance-panel").hasClass("active")) {
            $(".active").removeClass("active");
            $("#acl-instance-panel").addClass("active");
        }

        evt.preventDefault();
    });

    // Roles
    $("#acl-role-exit").click(function (evt) {
        $("#acl-role-panel").removeClass("active");

        evt.preventDefault();
    });
    $(".acl-user-exit").click(function (evt) {
        $("#acl-role-role-div").addClass("closed");
        $("#acl-role-group-div").addClass("closed");
        $(".acl-selected-row").removeClass("acl-selected-row");

        evt.preventDefault();
    });

    $("#acl-role-add").click(function (evt) {
        var subject = $("#acl-user").val();
        var apiUrl = baseUrl + '/StackV-web/restapi/app/keycloak/users/' + subject + '/role';
        keycloak.updateToken(30).success(function () {
            $.ajax({
                url: apiUrl,
                type: 'POST',
                data: "",
                contentType: "application/json; charset=utf-8",
                dataType: "json",
                beforeSend: function (xhr) {
                    xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
                },
                success: function (result) {
                    for (i = 0; i < result.length; i++) {
                        
                    }
                }
            });
        }).error(function () {
            console.log("Fatal Error: Token update failed!");
        });

        evt.preventDefault();
    });

    // Instances
    $("#acl-instance-close").click(function (evt) {
        $("#acl-instance-acl").addClass("closed");
        $("#acl-instance").removeAttr('value');
        $(".acl-selected-row").removeClass("acl-selected-row");

        evt.preventDefault();
    });

    $("#acl-instance-exit").click(function (evt) {
        $("#acl-instance-panel").removeClass("active");

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
                    row.className = "acl-row";
                    row.setAttribute("data-subject", user[4]);

                    var cell1_1 = document.createElement("td");
                    cell1_1.innerHTML = user[0];

                    row.appendChild(cell1_1);
                    tbody.appendChild(row);
                }

                $(".acl-row").click(function () {
                    if (animating === false) {
                        $(".acl-selected-row").removeClass("acl-selected-row");
                        $(this).addClass("acl-selected-row");

                        subloadUserRoleACLGroups($(this).data("subject"));
                        subloadUserRoleACLRoles($(this).data("subject"));
                    }
                });
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

                    $("#acl-group-select").append(new Option(group, group));
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

                    $("#acl-role-select").append(new Option(role, role));
                }
            }
        });
    }).error(function () {
        console.log("Fatal Error: Token update failed!");
    });
}

function subloadUserRoleACLGroups(subject) {
    var tbody = document.getElementById("group-body");
    tbody.innerHTML = "";

    var apiUrl = baseUrl + '/StackV-web/restapi/app/keycloak/users/' + subject + '/groups';
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

                    var row = document.createElement("tr");
                    row.className = "acl-row";

                    var cell1_1 = document.createElement("td");
                    cell1_1.innerHTML = group;

                    row.appendChild(cell1_1);
                    tbody.appendChild(row);
                }

                $(".acl-role-group-div").removeClass("closed");
            }
        });
    }).error(function () {
        console.log("Fatal Error: Token update failed!");
    });
}

function subloadUserRoleACLRoles(subject) {
    var tbody = document.getElementById("role-body");
    tbody.innerHTML = "";

    var apiUrl = baseUrl + '/StackV-web/restapi/app/keycloak/users/' + subject + '/roles';
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

                    var row = document.createElement("tr");
                    row.className = "acl-row";

                    var cell1_1 = document.createElement("td");
                    cell1_1.innerHTML = role;

                    row.appendChild(cell1_1);
                    tbody.appendChild(row);
                }

                $(".acl-role-role-div").removeClass("closed");
            }
        });
    }).error(function () {
        console.log("Fatal Error: Token update failed!");
    });
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
                for (i = 0; i < result.length; i++) {
                    var instance = result[i];

                    var row = document.createElement("tr");
                    row.className = "acl-row";
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

                $(".acl-row").click(function () {
                    if (animating === false) {
                        $(".acl-selected-row").removeClass("acl-selected-row");
                        $(this).addClass("acl-selected-row");

                        subloadACLTable($(this).data("uuid"));
                    }
                });

                $("#acl-instance-panel").removeClass("closed");
            }
        });
    }).error(function () {
        console.log("Fatal Error: Token update failed!");
    });
}

function subloadACLTable(refUUID) {
    animating = true;
    if (!$("#acl-instance-acl").hasClass("closed")) {
        $("#acl-instance-acl").addClass("closed");

        setTimeout(function () {
            reloadACL(refUUID);
        }, 1000);
    } else {
        reloadACL(refUUID);
    }
}

function reloadACL(refUUID) {
    var apiUrl = baseUrl + '/StackV-web/restapi/app/acl/' + refUUID;
    var tbody = document.getElementById("acl-body");
    tbody.innerHTML = "";

    $("#acl-instance").val(refUUID);

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
                    var cell1_1 = document.createElement("td");
                    cell1_1.innerHTML = user[0];
                    var cell1_2 = document.createElement("td");
                    cell1_2.innerHTML = user[1];
                    var cell1_3 = document.createElement("td");
                    cell1_3.innerHTML = user[2];
                    var cell1_4 = document.createElement("td");
                    cell1_4.innerHTML = '<button data-username="' + user[0] + '" class="button-acl-remove btn btn-default pull-right">Remove</button>';
                    row.appendChild(cell1_1);
                    row.appendChild(cell1_2);
                    row.appendChild(cell1_3);
                    row.appendChild(cell1_4);
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
                                    subloadACLTable(refUUID);
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

                animating = false;
                $("#acl-instance-acl").removeClass("closed");
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
                    cell1_4.innerHTML = '<button data-username="' + user[0] + '" class="button-acl-add btn btn-default pull-right">Add</button>';
                    row.appendChild(cell1_1);
                    row.appendChild(cell1_2);
                    row.appendChild(cell1_3);
                    row.appendChild(cell1_4);
                    tbody.appendChild(row);
                }

                $(".button-acl-add").click(function (evt) {
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
                                subloadACLTable(refUUID);
                            }
                        });
                    }).error(function () {
                        console.log("Fatal Error: Token update failed!");
                    });

                    evt.preventDefault();
                });
            }
        });
    }).error(function () {
        console.log("Fatal Error: Token update failed!");
    });
}