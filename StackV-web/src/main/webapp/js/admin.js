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
/* global XDomainRequest, baseUrl, keycloak, loggedIn, TweenLite, Power2, Mousetrap */
// Tweens
var tweenAdminPanel = new TweenLite("#admin-panel", 1, {ease: Power2.easeInOut, paused: true, top: "0px", opacity: "1", display: "block"});
var tweenLoggingPanel = new TweenLite("#logging-panel", 1, {ease: Power2.easeInOut, paused: true, left: "0px", opacity: "1", display: "block"});

var view = "left";
var dataTable = null;

Mousetrap.bind({
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
                newView("admin");
            }
            break;
        case "center":
            if (dir === "left") {
                newView("logging");
                view = dir;
            }
            break;
    }
}

$(function () {
    $(".checkbox-level").change(function () {
        if ($(this).is(":checked")) {
            $("#log-div").removeClass("hide-" + this.name);
        } else {
            $("#log-div").addClass("hide-" + this.name);
        }
    });
    $("#filter-search-clear").click(function () {
        $("#filter-search-input").val("");
        loadLogs();
    });
});

function loadAdminNavbar() {
    $("#sub-nav").load("/StackV-web/nav/admin_navbar.html", function () {
        setRefresh($("#refresh-timer").val());
        switch (view) {
            case "left":
                $("#logging-tab").addClass("active");
                break;
            case "center":
                $("#sub-admin-tab").addClass("active");
                break;
        }

        $("#logging-tab").click(function () {
            resetView();
            newView("logging");
        });
        $("#sub-admin-tab").click(function () {
            resetView();
            newView("admin");
        });
    });
}

function resetView() {
    switch (view) {
        case "left":
            $("#sub-nav .active").removeClass("active");
            tweenLoggingPanel.reverse();
            break;
        case "center":
            $("#sub-nav .active").removeClass("active");
            tweenAdminPanel.reverse();
            break;
    }
}
function newView(panel) {
    resetView();
    switch (panel) {
        case "logging":
            tweenLoggingPanel.play();
            $("#logging-tab").addClass("active");
            view = "left";
            break;
        case "admin":
            tweenAdminPanel.play();
            $("#sub-admin-tab").addClass("active");
            view = "center";
            break;
    }
}

function loadAdmin() {
    var apiUrl = baseUrl + '/StackV-web/restapi/app/logging/logs';
    loadDataTable(apiUrl);
    setTimeout(function () {
        if (view === "left") {
            tweenLoggingPanel.play();
            $('div.dataTables_filter input').focus();
        }
    }, 500);
    reloadLogs();
}


/* REFRESH */
function reloadData() {
    keycloak.updateToken(90).error(function () {
        console.log("Error updating token!");
    }).success(function (refreshed) {
        setTimeout(function () {
            var timerSetting = $("#refresh-timer").val();
            refreshSync(refreshed, timerSetting);

            // Refresh Operations
            reloadLogs();
        }, 500);
    });
}

function executeRequest(){
    
    var url_request = $("#API-request").val();
    var url = $("#URL").val();
    //var apiUrl = baseUrl + '/StackV-web/restapi/app/'+"option" + document.getElementById("URL").value;
    var apiUrl = baseUrl + "/StackV-web/restapi/app/"+ url;
    var type = url_request;
    
    var input = $("#api_result").val();
    
    url_arr = url.split("/");
    
    if(type === "GET"){
        $.ajax({
            url: apiUrl,
            type: type,
            beforeSend: function (xhr) {
                xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
                xhr.setRequestHeader("Refresh", keycloak.refreshToken);
            },
            success: function (result) {
                
                if(typeof result === "string"){
                    if(url_arr[0] === "keycloak"){
                        var resultArr = String(result).split(",");
                        var jsonStr = "[";
                        var index = 0;
                        for(index = 0;index < resultArr.length;index+=2){
                            if(index+2 == resultArr.length){
                                jsonStr += "["+"\""+resultArr[index+1]+"\""+" , "+"\""+resultArr[index]+"\""+"]";
                            } else {
                                jsonStr += "["+"\""+resultArr[index+1]+"\""+" , "+"\""+resultArr[index]+"\""+"],";
                            }
                        }
                        jsonStr += "]";
                        var jsonFormat = JSON.parse(jsonStr);
                 
                        $("#api_result").val(JSON.stringify(jsonFormat,null,2));
                    } else {
                        $("#api_result").val(result);
                    }
                } else {
                    $("#api_result").val(JSON.stringify(result,null,2));
                }
                
            },
            error: function () {
                $("#api_result").val("Failure");
            }
        });
    } else if(type === "PUT"){
        $.ajax({
            url: apiUrl,
            type: type,
            data: input,
            contentType: "application/json; charset=utf-8",
            dataType: "json",
            beforeSend: function (xhr) {
                xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
                xhr.setRequestHeader("Refresh", keycloak.refreshToken);
            },
            success: function (result) {
                $("#api_result").val("Success");
            },
            error: function () {
                $("#api_result").val("Failure");
            }
        });
    } else if(type === "POST"){
        $.ajax({
            url: apiUrl,
            type: type,
            data: input,
            contentType: "application/json; charset=utf-8",
            dataType: "json",
            beforeSend: function (xhr) {
                xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
                xhr.setRequestHeader("Refresh", keycloak.refreshToken);
            },
            success: function (result) {
                $("#api_result").val("Success");
            },
            error: function () {
                $("#api_result").val("Failure");
            }
        });
    } else if(type === "DELETE"){
        $.ajax({
        url: apiUrl,
        type: type,
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
            xhr.setRequestHeader("Refresh", keycloak.refreshToken);
        },
        success: function (result) {
            $("#api_result").val("success");
        },
        error: function () {
            $("#api_result").val("failure");
        }
    });
    }
    
}
