/* 
 * Copyright (c) 2013-2016 University of Maryland
 * Created by: Jared Welsh
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
var tweenInstalledPanel = new TweenLite("#installed-panel", 1, {ease: Power2.easeInOut, paused: true, top: "0px"});
var tweenAddPanel = new TweenLite("#driver-add-panel", 1, {ease: Power2.easeInOut, paused: true, left: "0px"});
var tweenTemplatePanel = new TweenLite("#driver-template-panel", 1, {ease: Power2.easeInOut, paused: true, right: "0px"});
var tweenContentPanel = new TweenLite("#driver-content-panel", 1, {ease: Power2.easeInOut, paused: true, bottom: "10%"});
var tweenBlackScreen = new TweenLite("#black-screen", .5, {ease: Power2.easeInOut, paused: true, autoAlpha: "1"});
var view = "center";

Mousetrap.bind({
    'shift+left': function () {
        window.location.href = "/StackV-web/ops/details/templateDetails.jsp";
    },
    'shift+right': function () {
        window.location.href = "/StackV-web/ops/acl.jsp";
    },
    'left': function () {
        viewShift("left");
    },
    'right': function () {
        viewShift("right");
    },
    'escape': function () {
        closeContentPanel();
    }
});
function viewShift(dir) {
    switch (view) {
        case "left":
            if (dir === "right") {
                newView("installed");
            }
            break;
        case "center":
            switch (dir) {
                case "left":
                    newView("add");
                    break;
                case "right":
                    newView("template");
                    break;
            }
            view = dir;
            break;
        case "right":
            if (dir === "left") {
                newView("installed");
            }
            break;
    }
}
function newView(panel) {
    resetView();
    switch (panel) {
        case "add":
            tweenAddPanel.play();
            $("#driver-add-tab").addClass("active");
            view = "left";
            break;
        case "installed":
            tweenInstalledPanel.play();
            $("#installed-tab").addClass("active");
            view = "center";
            break;
        case "template":
            tweenTemplatePanel.play();
            $("#driver-template-tab").addClass("active");
            view = "right";
            break;
    }
}
function resetView() {
    switch (view) {
        case "left":
            $("#sub-nav .active").removeClass("active");
            if ($("#driver-content-panel").hasClass("open")) {
                tweenContentPanel.reverse();
                tweenBlackScreen.reverse();
            }
            tweenAddPanel.reverse();
            break;
        case "center":
            $("#sub-nav .active").removeClass("active");
            tweenInstalledPanel.reverse();
            break;
        case "right":
            $("#sub-nav .active").removeClass("active");
            tweenTemplatePanel.reverse();
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

function openContentPanel() {
    if (!$("#driver-content-panel").hasClass("open")) {
        tweenBlackScreen.play();
        tweenContentPanel.play();
        $("#driver-content-panel").addClass("open");
        $("#driver-content-panel").removeClass("hidden");
        $("#driver-content-panel").addClass("active");
    }
}
function closeContentPanel() {
    if ($("#driver-content-panel").hasClass("active")) {
        tweenBlackScreen.reverse();
        tweenContentPanel.reverse();
        $("#driver-content-panel").removeClass("open");
        $("#driver-content-panel").removeClass("active");
        $("#driver-content-panel").addClass("hidden");
    }
}

function loadDriverNavbar() {
    $("#sub-nav").load("/StackV-web/nav/driver_navbar.html", function () {
        setRefresh($("#refresh-timer").val());
        switch (view) {
            case "left":
                $("#driver-add-tab").addClass("active");
                break;
            case "center":
                $("#installed-tab").addClass("active");
                break;
            case "right":
                $("#driver-template-tab").addClass("active");
                break;
        }

        $("#driver-add-tab").click(function () {
            resetView();
            newView("add");
        });
        $("#installed-tab").click(function () {
            resetView();
            newView("installed");
        });
        $("#driver-template-tab").click(function () {
            resetView();
            newView("template");
        });
    });
}

function loadDriverPortal() {
    getAllDetails();

    $(".install-button").click(function () {
        openContentPanel();
    });
}


function driver_tab_fix() {
    document.getElementById("driver-tab1").style.display = "block";
    document.getElementById("saved-tab").style.display = "none";
    document.getElementById("installed-content").style.display = "none";
    document.getElementById("saved-nav-tab").className = "";
    document.getElementById("driver-nav-tab").className = "active";
    document.getElementById("installed-driver-tab").className = "";
}


function saved_tab_fix() {
    document.getElementById("saved-tab").style.display = "block";
    document.getElementById("driver-tab1").style.display = "none";
    document.getElementById("installed-content").style.display = "none";
    document.getElementById("saved-nav-tab").className = "active";
    document.getElementById("driver-nav-tab").className = "";
    document.getElementById("installed-driver-tab").className = "";
}
function installed_tab_fix() {
    document.getElementById("installed-content").style.display = "block";
    document.getElementById("driver-tab1").style.display = "none";
    document.getElementById("saved-tab").style.display = "none";
    document.getElementById("installed-driver-tab").className = "active";
    document.getElementById("driver-nav-tab").className = "";
    document.getElementById("saved-nav-tab").className = "";
}

function activateSide() {
    $("#driver-content-panel").removeClass("hidden");
    $("#driver-content-panel").addClass("active");
    $('#driver-panel-right').addClass('active-detail');
    $('#install-content').addClass('active');
    $('#driver-panel-top').removeClass('no-side-tab');
    $('#driver-panel-bot').removeClass('no-side-tab');
    $('#driver-panel-top').addClass('side-tab');
    $('#driver-panel-bot').addClass('side-tab');
}

function closeSide() {
    
    $("#driver-content-panel").removeClass("active");
    $("#driver-content-panel").addClass("hidden");
    
    document.getElementById("driver-panel-right").className = "inactive";
    $('#install-tab').removeClass = "active";
    $('#install-content').removeClass('active');
    $('#driver-panel-top').removeClass('side-tab');
    $('#driver-panel-bot').removeClass('side-tab');
    $('#driver-panel-top').addClass('no-side-tab');
    $('#driver-panel-bot').addClass('no-side-tab');

}
function installRaw() {
    var first = document.createElement("p");
    var second = document.createElement("textarea");
    var divContent = document.getElementById("install-type");

    $("#info-panel-title").text("Raw Driver");


    first.innerHTML = "Enter Raw XML:";
    first.style.color = "#333";
    second.id = "rawXML";
    second.rows = "15";
    second.cols = "45";
    divContent.appendChild(first);
    divContent.appendChild(second);
}
function installStub() {
    var first = document.createElement("p");
    var second = document.createElement("input");
    var third = document.createElement("p");
    var fourth = document.createElement("textarea");
    var divContent = document.getElementById("install-type");

    $("#info-panel-title").text("Stub System Driver");

    first.innerHTML = "Topology URI:";
    first.style.color = "#333";

    second.type = "text";
    second.id = "TOPURI";

    third.innerHTML = "TTL:";
    third.style.color = "#333";

    fourth.id = "stubModelTtl";
    fourth.rows = "20";
    fourth.cols = "50";

    divContent.appendChild(first);
    divContent.appendChild(second);
    divContent.appendChild(third);
    divContent.appendChild(fourth);
}
function installAWS() {
    var first = document.createElement("p");
    var second = document.createElement("input");
    var third = document.createElement("p");
    var fourth = document.createElement("input");
    var fifth = document.createElement("p");
    var sixth = document.createElement("input");
    
    //Instance Type
    //Image
    //Key Pair
    //Sec Group
    var instance = document.createElement("p");
    var instanceInput = document.createElement("input");
    var image = document.createElement("p");
    var imageInput = document.createElement("input");
    var keyPair = document.createElement("p");
    var keyPairInput = document.createElement("input");
    var secGroup = document.createElement("p");
    var secGroupInput = document.createElement("input");
    
    
    
    var seventh = document.createElement("p");
    var eighth = document.createElement("SELECT");
    var option1 = document.createElement("option");
    var option2 = document.createElement("option");
    var option3 = document.createElement("option");
    var option4 = document.createElement("option");
    var option5 = document.createElement("option");
    var option6 = document.createElement("option");
    var option7 = document.createElement("option");
    var option8 = document.createElement("option");
    var option9 = document.createElement("option");
    var option10 = document.createElement("option");
    var divContent = document.getElementById("install-type");

    
    $("#info-panel-title").text("AWS Driver");


    first.innerHTML = "Topology URI:";
    first.style.color = "#333";

    second.type = "text";
    second.id = "TOPURI";

    third.innerHTML = "Amazon Access ID:";
    third.style.color = "#333";

    fourth.type = "text";
    fourth.id = "aws_access_key_id";

    fifth.innerHTML = "Amazon Secret Key:";
    fifth.style.color = "#333";

    sixth.type = "text";
    sixth.id = "aws_secret_access_key";
    
   
    instance.innerHTML = "Default Instance";
    instance.style.color = "#333";
    
    instanceInput.type = "text";
    instanceInput.id = "instance";
    
    image.innerHTML = "Default Image";
    image.style.color = "#333";
    
    imageInput.type = "text";
    imageInput.id = "image";
    
    keyPair.innerHTML = "Default Key Pair";
    keyPair.style.color = "#333";
    
    keyPairInput.type = "text";
    keyPairInput.id = "key-pair";
    
    secGroup.innerHTML = "Default Sec Group";
    secGroup.style.color = "#333";
    
    secGroupInput.type = "text";
    secGroupInput.id = "sec-group";

    seventh.innerHTML = "Region";
    seventh.style.color = "#333";

    option1.text = "us-east-1";
    option2.text = "us-east-2";
    option3.text = "us-west-1";
    option4.text = "us-west-2";
    option5.text = "eu-west-1";
    option6.text = "eu-central-1";
    option7.text = "ap-southeast-1";
    option8.text = "ap-southeast-2";
    option9.text = "ap-northeast-1";
    option10.text = "sa-east-1";

    eighth.add(option1);
    eighth.add(option2);
    eighth.add(option3);
    eighth.add(option4);
    eighth.add(option5);
    eighth.add(option6);
    eighth.add(option7);
    eighth.add(option8);
    eighth.add(option9);
    eighth.add(option10);
    eighth.id = "region";
    
    divContent.appendChild(first);
    divContent.appendChild(second);
    divContent.appendChild(third);
    divContent.appendChild(fourth);
    divContent.appendChild(fifth);
    divContent.appendChild(sixth);
    divContent.appendChild(instance);
    divContent.appendChild(instanceInput);
    divContent.appendChild(image);
    divContent.appendChild(imageInput);
    divContent.appendChild(keyPair);
    divContent.appendChild(keyPairInput);
    divContent.appendChild(secGroup);
    divContent.appendChild(secGroupInput);
    divContent.appendChild(seventh);
    divContent.appendChild(eighth);
}
function installOpenstack() {
    var divContent = document.getElementById("install-type");

    var extName = document.createElement("p");
    var ext = document.createElement("textarea");
    var content = [];

    $("#info-panel-title").text("Open Stack Driver");

    for (var i = 0; i < 26; i += 2) {
        var textbox = document.createElement("p");
        var input = document.createElement("input");
        textbox.style.color = "#333";
        input.type = "text";
        content[i] = textbox;
        content[i + 1] = input;
    }

    content[0].innerHTML = "Topology URI:";
    content[1].id = "TOPURI";
    content[2].innerHTML = "Openstack Username:";
    content[3].id = "username";
    content[4].innerHTML = "Openstack Password:";
    content[5].id = "password";
    content[6].innerHTML = "NAT Server:";
    content[7].id = "NATServer";
    content[8].innerHTML = "URL:";
    content[9].id = "url";
    content[10].innerHTML = "Tenant:";
    content[11].id = "tenant";
    content[12].innerHTML = "Admin Username:";
    content[13].id = "adminUsername";
    content[14].innerHTML = "Admin Password:";
    content[15].id = "adminPassword";
    content[16].innerHTML = "Admin Tenant:";
    content[17].id = "adminTenant";
    content[18].innerHTML = "Default Image:";
    content[19].id = "defaultImage";
    content[20].innerHTML = "Default Flavor:";
    content[21].id = "defaultFlavor";
    content[22].innerHTML = "Default Key Pair";
    content[23].id = "key-pair";
    content[24].innerHTML = "Default Sec Group";
    content[25].id = "sec-group";
    



    extName.innerHTML = "ModelExt:";
    extName.style.color = "#333";
    ext.id = "modelExt";
    ext.rows = "15";
    ext.cols = "30";


    for (var i = 0; i < 26; i++) {
        divContent.appendChild(content[i]);
    }

   


    divContent.appendChild(extName);
    divContent.appendChild(ext);
}
function installStack() {
    var first = document.createElement("p");
    var second = document.createElement("input");
    var third = document.createElement("p");
    var fourth = document.createElement("input");
    var fifth = document.createElement("p");
    var sixth = document.createElement("input");
    var seventh = document.createElement("p");
    var eighth = document.createElement("input");
    var divContent = document.getElementById("install-type");

    $("#info-panel-title").text("Stack System Driver");

    first.innerHTML = "Topology URI:";
    first.style.color = "#333";

    second.type = "text";
    second.id = "TOPURI";

    third.innerHTML = "Subsystem Base URL:";
    third.style.color = "#333";

    fourth.type = "text";
    fourth.id = "subsystemBaseUrl";

    fifth.innerHTML = "Authorization Server:";
    fifth.style.color = "#333";

    sixth.type = "text";
    sixth.id = "authServer";

    seventh.innerHTML = "Credentials:";
    seventh.style.color = "#333";

    eighth.type = "password";
    eighth.id = "credentials";


    divContent.appendChild(first);
    divContent.appendChild(second);
    divContent.appendChild(third);
    divContent.appendChild(fourth);
    divContent.appendChild(fifth);
    divContent.appendChild(sixth);
    divContent.appendChild(seventh);
    divContent.appendChild(eighth);
}
function installGeneric() {
    var first = document.createElement("p");
    var second = document.createElement("input");
    var third = document.createElement("p");
    var fourth = document.createElement("input");
    var divContent = document.getElementById("install-type");

    $("#info-panel-title").text("Generic REST Driver");

    first.innerHTML = "Topology URI:";
    first.style.color = "#333";

    second.type = "text";
    second.id = "TOPURI";

    third.innerHTML = "Subsystem Base URL:";
    third.style.color = "#333";

    fourth.type = "text";
    fourth.id = "subsystemBaseUrl";

    divContent.appendChild(first);
    divContent.appendChild(second);
    divContent.appendChild(third);
    divContent.appendChild(fourth);
}
function clearPanel() {
    $("#info-panel-title").text("Details");
    $('#install-type').empty();
    $('#install-options').empty();
    $('#install-type-right').empty();
    document.getElementById("install-type-right").style = "";
    document.getElementById("install-type").style = "";
    document.getElementById("install-options").style = "";
    var closeButton = document.createElement("button");
    closeButton.innerHTML = "Close";
    closeButton.className = "button-profile-select btn btn-default";
    closeButton.onclick = function () {
        clearPanel();
        closeContentPanel();
    };
    document.getElementById('install-options').appendChild(closeButton);
}
function clearText() {
    for (var temp of document.getElementsByTagName("input")) {
        temp.value = "";
    }
}
function changeNameInst() {
    var saveButton = document.createElement("button");
    saveButton.innerHTML = "Save Driver";
    saveButton.className = "button-profile-select btn btn-default";
    saveButton.onclick = function () {
        openWindow();
    };
    document.getElementById('install-options').appendChild(saveButton);
    var instButton = document.createElement("button");
    instButton.innerHTML = "Install Driver";
    instButton.className = "button-profile-select btn btn-default";
    instButton.onclick = function () {
        installDriver();
        reloadData();
        closeContentPanel();
    };
    document.getElementById('install-options').appendChild(instButton);
}
function changeNameInstRaw() {
    var instButton = document.createElement("button");
    instButton.className = "button-profile-select btn btn-default";
    instButton.innerHTML = "Install Driver";
    instButton.onclick = function () {
        plugRaw();
        reloadData();
        closeContentPanel();
    };
    document.getElementById('install-options').appendChild(instButton);
}
function openWindow() {
    $('#info-fields').empty();
    $('#info-option').empty();
    var divContent = document.getElementById("info-fields");
    var drivername = document.createElement("p");
    var driver = document.createElement("input");
    var descname = document.createElement("p");
    var desc = document.createElement("input");
    var saveButton = document.createElement("button");
    saveButton.className = "button-profile-select btn btn-default";

    $('#info-panel').addClass("active");

    drivername.innerHTML = "Driver Name:";
    driver.type = "text";
    driver.id = "drivername";
    divContent.appendChild(drivername);
    divContent.appendChild(driver);


    descname.innerHTML = "Description:";

    desc.type = "text";
    desc.id = "description";

    divContent.appendChild(descname);
    divContent.appendChild(desc);

    saveButton.innerHTML = "Save Driver";
    saveButton.onclick = function () {
        addDriver();
    };
    document.getElementById("info-option").appendChild(saveButton);
}
function changeNameDet() {
}
function addDriver() {
    var userId = keycloak.tokenParsed.preferred_username;
    var apiUrl = baseUrl + '/StackV-web/restapi/app/driver/' + userId + '/add';
    var jsonData = [];
    var tempData = {};
    var description = document.getElementById("description").value;
    var driver = document.getElementById("drivername").value;
    var URI = document.getElementById("TOPURI").value;
    var type = document.getElementById("info-panel-title").innerHTML;

    $('#black-screen').addClass("off");
    $('#info-panel').removeClass();
    closeContentPanel();

    for (var temp of document.getElementsByTagName("input")) {
        if (temp !== document.getElementById("description") &&
                temp !== document.getElementById("drivername")
                && temp.value !== '') {
            tempData[temp.id] = temp.value;
        }
    }

    for (var temp of document.getElementsByTagName("textarea")) {
        if (temp.value !== '')
            tempData[temp.id] = temp.value;
    }

    jsonData.push(tempData);

    var settings = JSON.stringify({jsonData});

    var sentData = JSON.stringify({
        username: userId,
        drivername: driver,
        driverDescription: description,
        data: settings,
        topuri: URI,
        drivertype: type
    });

    $.ajax({
        url: apiUrl,
        type: 'PUT',
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
            xhr.setRequestHeader("Refresh", keycloak.refreshToken);
        },
        contentType: 'application/json',
        data: sentData,
        success: function () {
            updateDrivers(URI);
        },
        error: function(){
        }
    });
}
function removeDriverProfile(clickID) {
    var userId = keycloak.tokenParsed.preferred_username;
    var topuri = clickID;
    var apiUrl = baseUrl + '/StackV-web/restapi/app/driver/' + userId + '/delete/' + topuri;
    $.ajax({
        url: apiUrl,
        type: 'DELETE',
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
            xhr.setRequestHeader("Refresh", keycloak.refreshToken);
        },
        success: function (result) {
            updateDrivers(topuri);
        },
        error: function(result){
        }
    });
}
//needs to change
//UPDATE THE API CALLS
//Fixes for Andrew
function updateDrivers(URI) {
    var userId = keycloak.tokenParsed.preferred_username;
    var table = document.getElementById("saved-table");
    var apiUrl = baseUrl + '/StackV-web/restapi/app/driver/' + userId + '/get';
    $.ajax({
        url: apiUrl,
        type: 'GET',
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
            xhr.setRequestHeader("Refresh", keycloak.refreshToken);
        },
        success: function (result) {
            $('#saved-table').empty();
            for (var i = 0; i < result.length; i += 4) {
                var row = document.createElement("tr");
                var drivername = document.createElement("td");
                var description = document.createElement("td");
                var cell3 = document.createElement("td");
                var detButton = document.createElement("button");
                var delButton = document.createElement("button");
                var edButton = document.createElement("button");
                
                detButton.className = "button-profile-select btn btn-default";
                detButton.style.width = "64px";
                
                delButton.className = "button-profile-select btn btn-default";
                delButton.style.width = "64px";

                detButton.innerHTML = "Details";
                detButton.onclick = function () {
                    $("#driver-content-panel").removeClass("hidden");
                    $("#driver-content-panel").addClass("active");
                    $("#info-panel-title").text("Details");
                    clearPanel();
                    activateSide();
                    changeNameDet();
                    getDetailsProfile(URI);
                    openContentPanel();
                };
                
                    
               
                detButton.id = result[i + 3];

                delButton.innerHTML = "Delete";
                delButton.onclick = function () {
                    removeDriverProfile(URI);
                };
               
                delButton.id = result[i + 3];

                edButton.innerHTML = "Edit";
                edButton.style.width = "64px";
                edButton.className = "button-profile-select btn btn-default";
                edButton.onclick = function () {
                    $("#driver-content-panel").removeClass("hidden");
                    $("#driver-content-panel").addClass("active");
                    $("#info-panel-title").text("Details");
                    clearPanel();
                    activateSide();
                    
                    changeNameDet();
                    editDriverProfile(URI);
                    
                    openContentPanel();
                };
                
                edButton.id = result[i + 3];


                drivername.innerHTML = result[i];
                description.innerHTML = result[i + 1];
                cell3.appendChild(detButton);
                cell3.appendChild(edButton);
                cell3.appendChild(delButton);
                cell3.style.width = "200px";

                row.appendChild(drivername);
                row.appendChild(description);
                row.appendChild(cell3);
                table.appendChild(row);
            }
        }
    });
}
function editDriverProfile(clickID) {
    getDetailsProfile(clickID);
    var table = document.getElementById("details_table");
    for (var i = 1; i < table.rows.length; i++) {
        var row = table.rows[i];
        var textbox = document.createElement("input");
        textbox.type = "text";
        textbox.innerHTML = row.cells[1].innerHTML;
        row.cells[1].appendChild(textbox);
    }
}
//FIX THIS
function getDetailsProfile(clickID) {
    var userId = keycloak.tokenParsed.preferred_username;
    var panel = document.getElementById("install-type");
    var botpanel = document.getElementById('install-options');
    var topuri = clickID;
    var apiUrl = baseUrl + '/StackV-web/restapi/app/driver/' + userId + '/getdetails/' + topuri;
    var table = document.createElement("table");
    var thead = document.createElement("thead");
    var head_row = document.createElement("tr");
    var headkey = document.createElement("th");
    var headval = document.createElement("th");
    $(table).addClass('management-table');
    table.id = "details_table";
    headkey.innerHTML = "Key";
    headval.innerHTML = "Value";
    head_row.appendChild(headkey);
    head_row.appendChild(headval);
    thead.appendChild(head_row);
    table.appendChild(thead);
    $.ajax({
        url: apiUrl,
        type: 'GET',
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
            xhr.setRequestHeader("Refresh", keycloak.refreshToken);
        },
        success: function (result) {
            $('#installed-type').empty();
            for (var key in result) {
                if (result.hasOwnProperty(key)) {
                    var row = document.createElement("tr");
                    var tempkey = document.createElement("td");
                    var tempval = document.createElement("td");
                    tempkey.innerHTML = key;
                    tempval.innerHTML = result[key];
                    row.appendChild(tempkey);
                    row.appendChild(tempval);
                    table.appendChild(row);
                }
            }
            
            var instDetailsButton = document.createElement("button");
            instDetailsButton.className = "button-profile-select btn btn-default";
            instDetailsButton.innerHTML = "Install";
            instDetailsButton.className = "button-profile-select btn btn-default";
            
            instDetailsButton.onclick = function () {
                plugDriver(result["TOPURI"]);
            };
            instDetailsButton.className = "button-profile-select btn btn-default";
            botpanel.appendChild(instDetailsButton);
        },
        error: function(){
            alert("Failure");
        }
    });
    panel.appendChild(table);
}

function getAllDetails() {
    var table = document.getElementById("installed-body");
    var apiUrl = baseUrl + '/StackV-web/restapi/driver/';
    $.ajax({
        url: apiUrl,
        type: 'GET',
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
            xhr.setRequestHeader("Refresh", keycloak.refreshToken);
        },
        success: function (result) {
            //fill installed table
            $('#installed-body').empty();
            for (var i = 0; i < result.length; i += 3) {
                var row = document.createElement("tr");
                var drivername = document.createElement("td");
                var description = document.createElement("td");
                var cell3 = document.createElement("td");
                var detButton = document.createElement("button");
                var delButton = document.createElement("button");

                detButton.innerHTML = "Details";
                detButton.style.width = "64px";
                detButton.className = "button-profile-select btn btn-default";
                detButton.onclick = function () {
                    clearPanel();
                    activateSide();
                    changeNameDet();
                    getDetails(this.id);
                    openContentPanel();
                };
                
                detButton.id = result[i];

                delButton.innerHTML = "Delete";
                delButton.style.width = "64px";
                delButton.className = "button-profile-select btn btn-default";
                delButton.onclick = function () {
                    removeDriver(this.id);
                    reloadData();
                };
             
                delButton.id = result[i + 2];


                drivername.innerHTML = result[i + 2];
                description.innerHTML = result[i + 1];
                cell3.appendChild(detButton);
                cell3.appendChild(delButton);
                cell3.style.width = "170px";

                row.appendChild(drivername);
                row.appendChild(description);
                row.appendChild(cell3);
                table.appendChild(row);
            }

            if (view === "center") {
                tweenInstalledPanel.play();
            }
        }
    });
}
function removeDriver(clickID) {
    var topUri = clickID;
    var apiUrl = baseUrl + '/StackV-web/restapi/driver/' + topUri;
    $.ajax({
        url: apiUrl,
        type: 'DELETE',
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
            xhr.setRequestHeader("Refresh", keycloak.refreshToken);
        },
        success: function () {
            updateDrivers(URI);
        },
        error: function(){
            alert("failure");
        }
    });
}
function getDetails(clickID) {
    var driverId = clickID;
    var panel = document.getElementById("install-type");
    var apiUrl = baseUrl + '/StackV-web/restapi/driver/' + driverId;
    var table = document.createElement("table");
    var thead = document.createElement("thead");
    var head_row = document.createElement("tr");
    var headkey = document.createElement("th");
    var headval = document.createElement("th");
    $(table).addClass('management-table');
    headkey.innerHTML = "Key";
    headval.innerHTML = "Value";
    head_row.appendChild(headkey);
    head_row.appendChild(headval);
    thead.appendChild(head_row);
    table.appendChild(thead);

    $.ajax({
        url: apiUrl,
        type: 'GET',
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
            xhr.setRequestHeader("Refresh", keycloak.refreshToken);
        },
        success: function (result) {

            for (var i = 0; i < result.length; i += 2) {
                var row = document.createElement("tr");
                var tempkey = document.createElement("td");
                var tempval = document.createElement("td");
                tempkey.innerHTML = result[i];
                tempval.innerHTML = result[i + 1];
                row.appendChild(tempkey);
                row.appendChild(tempval);
                table.appendChild(row);

            }
        }
    });
    panel.appendChild(table);
}
function plugDriver(topuri) {
    var URI = topuri;
    var userId = keycloak.tokenParsed.preferred_username;
    var apiUrl = baseUrl + '/StackV-web/restapi/app/driver/' + userId + '/install/' + URI;
    var panel = document.getElementById("install-type");
    
    $.ajax({
        url: apiUrl,
        type: 'PUT',
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
            xhr.setRequestHeader("Refresh", keycloak.refreshToken);
        },
        success: function (result) {
            getAllDetails();
            $('#install-type').empty();
            var data = document.createElement("p");

            data.style.color = "#333";
            data.innerHTML = result;
            panel.appendChild(data);
        }
    });
}

function installDriver() {
    var panel = document.getElementById("install-type");
    var apiUrl = baseUrl + '/StackV-web/restapi/app/driver/install';
    var jsonData = [];
    var tempData = {};
    var type = document.getElementById("info-panel-title").innerHTML;

    for (var temp of document.getElementsByTagName("input")) {
        if (temp !== document.getElementById("description") &&
                temp !== document.getElementById("drivername")
                && temp.value !== '') {
            tempData[temp.id] = temp.value;
        }
    }
    for (var temp of document.getElementsByTagName("textarea")) {
        if (temp.value !== '')
            tempData[temp.id] = temp.value;
    }
    for (var temp of document.getElementsByTagName("select")) {
        if (temp !== document.getElementById("select-logging-level")
                && temp !== document.getElementById("refresh-timer")
                && temp.value !== '')
            tempData[temp.id] = temp.value;
    }

    tempData["info-panel-title"] = type;
    jsonData.push(tempData);

    var settings = JSON.stringify({jsonData});

    $.ajax({
        url: apiUrl,
        type: 'PUT',
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
            xhr.setRequestHeader("Refresh", keycloak.refreshToken);
        },
        contentType: 'application/json',
        data: settings,
        success: function (result) {
            var data = document.createElement("p");

            data.style.color = "#333";
            data.innerHTML = result;
            panel.appendChild(data);
            getAllDetails();
        },
        error: function (textStatus, errorThrown) {
            
        }
    });
}
function plugRaw() {
    var apiUrl = baseUrl + '/StackV-web/restapi/driver';
    var sentData = document.getElementById("rawXML").value;

    $.ajax({
        url: apiUrl,
        data: sentData,
        type: 'POST',
        contentType: "application/xml",
        dataType: "xml",
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
            xhr.setRequestHeader("Refresh", keycloak.refreshToken);
        },
        success: function (result) {
            document.getElementById("ret_field").innerHTML = result;
        }
    });

}


/* REFRESH */
function reloadData() {
    keycloak.updateToken(90).error(function () {
        console.log("Error updating token!");
    }).success(function (refreshed) {
        var timerSetting = $("#refresh-timer").val();
        if (timerSetting > 15) {
            if (view === "center") {
                tweenInstalledPanel.reverse();
            }
            setTimeout(function () {
                // Refresh Operations                        
                loadDriverPortal();
                refreshSync(refreshed, timerSetting);
            }, 1000);
        } else {
            setTimeout(function () {
                loadDriverPortal();
                refreshSync(refreshed, timerSetting);
            }, 500);
        }
    });
}