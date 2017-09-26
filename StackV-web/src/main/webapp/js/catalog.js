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

/* global XDomainRequest, baseUrl, keycloak, Power2, TweenLite, tweenBlackScreen, Mousetrap */
// Tweens
var tweenInstancePanel = new TweenLite("#instance-panel", .5, {ease: Power2.easeInOut, paused: true, top: "30px"});
var tweenCatalogPanel = new TweenLite("#catalog-panel", .5, {ease: Power2.easeInOut, paused: true, bottom: "0"});
var tweenBlackScreen = new TweenLite("#black-screen", .5, {ease: Power2.easeInOut, paused: true, autoAlpha: "1"});

Mousetrap.bind('space', function () {
    if ($("#catalog-panel").hasClass("closed")) {
        openCatalog();
    } else {
        closeCatalog();
    }
});
Mousetrap.bind({
    'shift+left': function () {
        window.location.href = "/StackV-web/orch/graphTest.jsp";
    },
    'shift+right': function () {
        window.location.href = "/StackV-web/ops/details/templateDetails.jsp";
    },
    'space': function () {
        if ($("#catalog-panel").hasClass("closed")) {
            openCatalog();
        } else {
            closeCatalog();
        }
    }
});


function openCatalog() {
    tweenCatalogPanel.play();
    tweenBlackScreen.play();
    $("#catalog-panel").removeClass("closed");
}
function closeCatalog() {
    tweenCatalogPanel.reverse();
    tweenBlackScreen.reverse();
    $("#catalog-panel").addClass("closed");
}

function loadCatalog() {
    loadInstances();
    loadWizard();
    loadEditor();

    if (getURLParameter("profiles")) {
        openCatalog();
        setTimeout(function () {
            $($("ul.catalog-tabs").children()[0]).children().click();
        }, 200);
    }

    $("#black-screen").click(function () {
        $("#info-panel").removeClass("active");
        closeCatalog();
    });

    $(".nav-tabs li").click(function () {
        if ($("#catalog-panel").hasClass("closed")) {
            openCatalog();
        } else if (this.className === 'active') {
            closeCatalog();
        }
    });
}
function loadCatalogNavbar() {
    $("#sub-nav").load("/StackV-web/nav/catalog_navbar.html", function () {
        setRefresh($("#refresh-timer").val());
    });
}

function loadInstances() {
    var userId = keycloak.subject;
    var tbody = document.getElementById("status-body");
    tbody.innerHTML = "";

    var apiUrl = baseUrl + '/StackV-web/restapi/app/panel/' + userId + '/instances';
    $.ajax({
        url: apiUrl,
        type: 'GET',
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
        },
        success: function (result) {
            $("#status-body").empty();

            for (i = 0; i < result.length; i++) {
                var instance = result[i];

                var row = document.createElement("tr");
                row.className = "clickable-row";
                row.setAttribute("data-href", instance[1]);

                var cell1_1 = document.createElement("td");
                cell1_1.innerHTML = instance[3];
                var cell1_2 = document.createElement("td");
                cell1_2.innerHTML = instance[0];
                var cell1_3 = document.createElement("td");
                cell1_3.innerHTML = instance[1];
                var cell1_4 = document.createElement("td");
                cell1_4.innerHTML = instance[2];
                row.appendChild(cell1_1);
                row.appendChild(cell1_2);
                row.appendChild(cell1_3);
                row.appendChild(cell1_4);
                tbody.appendChild(row);
            }

            $(".clickable-row").click(function () {
                sessionStorage.setItem("instance-uuid", $(this).data("href"));
                window.document.location = "/StackV-web/ops/details/templateDetails.jsp";
            });

            tweenInstancePanel.play();
        }
    });

}

function loadWizard() {
    var userId = keycloak.subject;
    var tbody = document.getElementById("wizard-body");
    $("tbody#wizard-body").find("tr").remove();

    var apiUrl = baseUrl + '/StackV-web/restapi/app/panel/' + userId + '/wizard';
    $.ajax({
        url: apiUrl,
        type: 'GET',
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
        },
        success: function (result) {
            // unbind all click functions!
            $("button").off("click");
            for (i = 0; i < result.length; i++) {
                var profile = result[i];

                var row = document.createElement("tr");
                var cell1_1 = document.createElement("td");
                cell1_1.innerHTML = profile[0];
                var cell1_2 = document.createElement("td");
                cell1_2.innerHTML = profile[1];
                var cell1_3 = document.createElement("td");
                cell1_3.innerHTML = "<button class='button-profile-select btn btn-default' id='" + profile[2] + "'>Select</button><button class='button-profile-delete btn btn' id='" + profile[2] + "'>Delete</button>";
                row.appendChild(cell1_1);
                row.appendChild(cell1_2);
                row.appendChild(cell1_3);
                tbody.appendChild(row);
            }

            $(".button-profile-select").on("click", function (evt) {
                var resultID = this.id,
                        apiUrl = baseUrl + '/StackV-web/restapi/app/profile/' + resultID;

                $.ajax({
                    url: apiUrl,
                    type: 'GET',
                    beforeSend: function (xhr) {
                        xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
                    },
                    success: function (result) {
                        $("#black-screen").removeClass("off");
                        $("#profile-modal").modal("show");
                        $("#info-panel-title").html("Profile Details");
                        $("#info-panel-text-area").val(JSON.stringify(result));
                        $(".button-profile-save").attr('id', resultID);
                        $(".button-profile-save-as").attr('id', resultID);
                        $(".button-profile-submit").attr('id', resultID);
                        prettyPrintInfo();
                    },
                    error: function (textStatus, errorThrown) {
                        console.log(textStatus);
                        console.log(errorThrown);
                    }
                });

                evt.preventDefault();
            });

            $(".button-profile-delete").on("click", function (evt) {
                var apiUrl = baseUrl + '/StackV-web/restapi/app/profile/' + this.id;
                $.ajax({
                    url: apiUrl,
                    type: 'DELETE',
                    beforeSend: function (xhr) {
                        xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
                    },
                    success: function (result) {
                        loadWizard();
                    },
                    error: function (textStatus, errorThrown) {
                        console.log(textStatus);
                        console.log(errorThrown);
                    }
                });

                evt.preventDefault();
            });

            $(".button-profile-submit").on("click", function (evt) {
                if ($("#profile-alias").val()) {
                    var profile = JSON.parse($("#info-panel-text-area").val());
                    profile["alias"] = $("#profile-alias").val();

                    var apiUrl = baseUrl + '/StackV-web/restapi/app/service/uuid';
                    $.ajax({
                        url: apiUrl,
                        async: false,
                        type: 'GET',
                        dataType: "text",
                        beforeSend: function (xhr) {
                            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
                            xhr.setRequestHeader("Refresh", keycloak.refreshToken);
                        },
                        success: function (result) {
                            var manifest = profile;
                            manifest["uuid"] = result;
                            manifest["data"]["uuid"] = result;
                            manifest["data"]["options"] = manifest["options"];
                            //manifest["data"] = JSON.parse($("#info-panel-text-area").val());

                            // Render template
//                            var rendered = render(manifest);
//                            if (!rendered) {
//                                swal("Templating Error", "The manifest submitted could not be properly rendered. Please contact a system administrator.", "error");
//                                return;
//                            }

                            //manifest['proceed'] = "true";
                            var apiUrl = baseUrl + '/StackV-web/restapi/app/service';
                            $.ajax({
                                url: apiUrl,
                                type: 'POST',
                                data: JSON.stringify(manifest),
                                contentType: "application/json; charset=utf-8",
                                dataType: "json",
                                beforeSend: function (xhr) {
                                    xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
                                    xhr.setRequestHeader("Refresh", keycloak.refreshToken);
                                },
                                success: function (result) {
                                },
                                error: function (textStatus, errorThrown) {
                                    console.log(textStatus);
                                    console.log(errorThrown);
                                }
                            });
                        }
                    });
                    // reload top table and hide modal
                    reloadData();
                    $("div#profile-modal").modal("hide");
                    $("#black-screen").addClass("off");
                    $("#info-panel").removeClass("active");
                    evt.preventDefault();
                } else {
                    $("#profile-alias").addClass("invalid");
                    $("#profile-alias").change(function () {
                        $(this).removeClass("invalid");
                    });
                }
            });

            // Hide the regular buttons and reveal the save as box
            $("button.button-profile-save-as").on("click", function (evt) {
                $("div.info-panel-regular-buttons").css("display", "none");
                $("div.info-panel-save-as-description").css("display", "block");
            });

            // Reveal the regular buttons and hide the save as boxes
            $("button.button-profile-save-as-cancel").on("click", function (evt) {
                $("div.info-panel-save-as-description").css("display", "none");
                $("div.info-panel-regular-buttons").css("display", "block");
            });


            // After the user has put a new name and description for the new profile
            $(".button-profile-save-as-confirm").on("click", function (evt) {
                var apiUrl = baseUrl + '/StackV-web/restapi/app/profile/new';
                var data = {
                    name: $("#new-profile-name").val(),
                    username: keycloak.tokenParsed.preferred_username,
                    description: $("#new-profile-description").val(),
                    data: JSON.parse($("#info-panel-text-area").val())
                };

                $.ajax({
                    url: apiUrl,
                    type: 'PUT',
                    data: JSON.stringify(data), //stringify to get escaped JSON in backend
                    contentType: "application/json; charset=utf-8",
                    dataType: "json",
                    beforeSend: function (xhr) {
                        xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
                        xhr.setRequestHeader("Refresh", keycloak.refreshToken);
                    },
                    success: function (result) {
                        // revert to regular buttons and close modal
                        $("input#new-profile-name").val("");
                        $("input#new-profile-description").val("");
                        $("div.info-panel-save-as-description").css("display", "none");
                        $("div.info-panel-regular-buttons").css("display", "block");
                        $("div#profile-modal").modal("hide");
                        // reload table
                        loadWizard();
                    },
                    error: function (textStatus, errorThrown) {
                        console.log(textStatus);
                        console.log(errorThrown);
                    }
                });

                // reload the bottom panel
                $("#black-screen").addClass("off");
                $("#info-panel").removeClass("active");
                evt.preventDefault();
            });

            $(".button-profile-save").on("click", function (evt) {
                var apiUrl = baseUrl + '/StackV-web/restapi/app/profile/' + this.id + '/edit';

                $.ajax({
                    url: apiUrl,
                    type: 'PUT',
                    data: $("#info-panel-text-area").val(),
                    contentType: "application/json; charset=utf-8",
                    dataType: "json",
                    beforeSend: function (xhr) {
                        xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
                        xhr.setRequestHeader("Refresh", keycloak.refreshToken);
                    },
                    success: function (result) {
                        // reload the bottom panel
                        loadWizard();
                        $("#profile-modal").modal("hide");
                    },
                    error: function (textStatus, errorThrown) {
                        console.log(textStatus);
                        console.log(errorThrown);
                    }
                });

                $("#black-screen").addClass("off");
                $("#info-panel").removeClass("active");
                evt.preventDefault();
            });
        }
    });
}

function loadEditor() {
    var userId = keycloak.subject;
    var tbody = document.getElementById("editor-body");

    var apiUrl = baseUrl + '/StackV-web/restapi/app/panel/' + userId + '/editor';
    $.ajax({
        url: apiUrl,
        type: 'GET',
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
        },
        success: function (result) {
            for (i = 0; i < result.length; i++) {
                var profile = result[i];

                var row = document.createElement("tr");
                var cell1_1 = document.createElement("td");
                cell1_1.innerHTML = profile[0];
                var cell1_2 = document.createElement("td");
                cell1_2.innerHTML = profile[1];
                var cell1_3 = document.createElement("td");
                cell1_3.innerHTML = "<button class='button-service-select btn btn-default' id='" + profile[2] + "'>Select</button";
                row.appendChild(cell1_1);
                row.appendChild(cell1_2);
                row.appendChild(cell1_3);
                tbody.appendChild(row);
            }
            $(document).on('click', '.button-service-select', function (evt) {
                var ref = "/StackV-web/ops/intent_test.html?intent=" + this.id.toLowerCase();
                window.location.href = ref;

                evt.preventDefault();
            });
        }
    });
}

function getURLParameter(name) {
    return decodeURIComponent((new RegExp('[?|&]' + name + '=' + '([^&;]+?)(&|#|;|$)').exec(location.search) || [null, ''])[1].replace(/\+/g, '%20')) || null;
}


/* REFRESH */
function reloadData() {
    keycloak.updateToken(90).error(function () {
        console.log("Error updating token!");
    }).success(function (refreshed) {
        var timerSetting = $("#refresh-timer").val();
        if (timerSetting > 15) {
            tweenInstancePanel.reverse();
            setTimeout(function () {
                loadInstances();
                refreshSync(refreshed, timerSetting);
            }, 750);
        } else {
            setTimeout(function () {
                loadInstances();
                refreshSync(refreshed, timerSetting);
            }, 500);
        }
    });
}
