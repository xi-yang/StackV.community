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

$(function () {
    setTimeout(catalogLoad, 750);
    setRefresh(60);

    //$("#tag-panel").load("/StackV-web/tagPanel.jsp", null);
});

function catalogLoad() {
    loadInstances();
    loadWizard();
    loadEditor();

    setTimeout(function (){
        $("#instance-panel").removeClass("closed");
        $("#catalog-panel").removeClass("closed");
    }, 250);
}

function loadInstances() {
    var userId = keycloak.subject;
    var tbody = document.getElementById("status-body");
    $("#status-body").empty();

    var apiUrl = baseUrl + '/StackV-web/restapi/app/panel/' + userId + '/instances';
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
                row.className = "clickable-row";
                row.setAttribute("data-href", '/StackV-web/ops/details/templateDetails.jsp?uuid=' + instance[1]);

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
                window.document.location = $(this).data("href");
            });
        }
    });
}

function loadWizard() {
    var userId = keycloak.subject;
    var tbody = document.getElementById("wizard-body");
    $("#wizard-body").empty();

    var apiUrl = baseUrl + '/StackV-web/restapi/app/panel/' + userId + '/wizard';
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
                cell1_3.innerHTML = "<button class='button-profile-select btn btn-default' id='" + profile[2] + "'>Select</button><button class='button-profile-delete btn btn-default' id='" + profile[2] + "'>Delete</button>";
                row.appendChild(cell1_1);
                row.appendChild(cell1_2);
                row.appendChild(cell1_3);
                tbody.appendChild(row);
            }

            $(".button-profile-select").click(function (evt) {
                var apiUrl = baseUrl + '/StackV-web/restapi/app/profile/' + this.id;
                $.ajax({
                    url: apiUrl,
                    type: 'GET',
                    beforeSend: function (xhr) {
                        xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
                    },
                    success: function (result) {
                        $("#black-screen").removeClass("off");
                        $("#info-panel").addClass("active");
                        $("#info-panel-title").html("Profile Details");
                        $("#info-panel-text-area").val(JSON.stringify(result));
                        prettyPrintInfo();
                    },
                    error: function (textStatus, errorThrown) {
                        console.log(textStatus);
                        console.log(errorThrown);
                    }
                });

                evt.preventDefault();
            });

            $(".button-profile-delete").click(function (evt) {
                var apiUrl = baseUrl + '/StackV-web/restapi/app/profile/' + this.id;
                $.ajax({
                    url: apiUrl,
                    type: 'DELETE',
                    beforeSend: function (xhr) {
                        xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
                    },
                    success: function (result) {
                        wizardLoad();
                    },
                    error: function (textStatus, errorThrown) {
                        console.log(textStatus);
                        console.log(errorThrown);
                    }
                });

                evt.preventDefault();
            });

            $(".button-profile-submit").click(function (evt) {
                var apiUrl = baseUrl + '/StackV-web/restapi/app/service';
                $.ajax({
                    url: apiUrl,
                    type: 'POST',
                    data: $("#info-panel-text-area").val(),
                    contentType: "application/json; charset=utf-8",
                    dataType: "json",
                    beforeSend: function (xhr) {
                        xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
                    },
                    success: function (result) {

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

            $(".button-service-select").click(function (evt) {
                var ref = "/StackV-web/ops/srvc/" + this.id.toLowerCase() + ".jsp";
                window.location.href = ref;

                evt.preventDefault();
            });
        }
    });
}

function timerChange(sel) {
    clearInterval(refreshTimer);
    clearInterval(countdownTimer);
    if (sel.value !== 'off') {
        setRefresh(sel.value);
    } else {
        document.getElementById('refresh-button').innerHTML = 'Manually Refresh Now';
    }
}

function setRefresh(time) {
    countdown = time;
    refreshTimer = setInterval(function () {
        reloadTracker(time);
    }, (time * 1000));
    countdownTimer = setInterval(function () {
        refreshCountdown(time);
    }, 1000);
}

function reloadTracker(time) {
    enableLoading();

    var manual = false;
    if (typeof time === "undefined") {
        time = countdown;
    }
    if (document.getElementById('refresh-button').innerHTML === 'Manually Refresh Now') {
        manual = true;
    }

    $('#instance-panel').load(document.URL + ' #status-table', function () {
        loadInstances();

        $(".clickable-row").click(function () {
            window.document.location = $(this).data("href");
        });

        if (manual === false) {
            countdown = time;
            document.getElementById('refresh-button').innerHTML = 'Refresh in ' + countdown + ' seconds';
        } else {
            document.getElementById('refresh-button').innerHTML = 'Manually Refresh Now';
        }

        setTimeout(function () {
            disableLoading();
        }, 750);
    });
}

function refreshCountdown() {
    document.getElementById('refresh-button').innerHTML = 'Refresh in ' + countdown + ' seconds';
    countdown--;
}

function getURLParameter(name) {
  return decodeURIComponent((new RegExp('[?|&]' + name + '=' + '([^&;]+?)(&|#|;|$)').exec(location.search) || [null, ''])[1].replace(/\+/g, '%20')) || null;
}
