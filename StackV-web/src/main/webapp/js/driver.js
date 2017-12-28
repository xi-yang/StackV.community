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

/* global XDomainRequest, baseUrl, keycloak, loggedIn, TweenLite, Power2, Mousetrap, swal */
var tweenInstalledPanel = new TweenLite("#installed-panel", 1, {ease: Power2.easeInOut, paused: true, top: "0px"});
var tweenAddPanel = new TweenLite("#driver-add-panel", 1, {ease: Power2.easeInOut, paused: true, left: "0px"});
var tweenTemplatePanel = new TweenLite("#driver-template-panel", 1, {ease: Power2.easeInOut, paused: true, right: "0px"});
var tweenContentPanel = new TweenLite("#driver-content-panel", 1, {ease: Power2.easeInOut, paused: true, bottom: "10%"});
var tweenDriverOverflowDetailsPanel = new TweenLite("#driver-overflow-details-panel", 1, {ease: Power2.easeInOut, paused: true, bottom:"10%"});
var tweenBlackScreen = new TweenLite("#black-screen", .5, {ease: Power2.easeInOut, paused: true, autoAlpha: "1"});
var view = "center";

function loadSystemHealthCheck(){
    var apiUrl = baseUrl + '/StackV-web/restapi/service/ready';
    var dialogObj = $('#system-health-check');
    var dialogText = $('#system-health-check-text');
    $.ajax({
        url: apiUrl,
        type: 'GET',
        dataType: "json",
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
            xhr.setRequestHeader("Refresh", keycloak.refreshToken);
        },
        success: function(result) {
            if (result["responseText"] === "true") {
                dialogText.text("System is fully intialized!");
                dialogObj.dialog({
                    open: function(event, ui) {
                        // resolving conflicting close buttons between jquery and boostrao
                        $(".ui-dialog-titlebar-close", ui.dialog | ui).hide();                           
                    },
                    position: {
                      my: "right bottom",
                      at: "right bottom",
                      of: window
                    },
                    dialogClass: 'dialog-titlebar-ready',
                    show: "slide",
                    resizeable: false,
                    draggable: true,
                    title: "System Health Check",
                    height: 100,
                    width: 250,
                    modal: false,
                    buttons: [
                        {
                            text:"Close",
                            "class": "button-profile-select btn btn-default",
                            click: function () {
                                $(this).dialog("close");
                            }
                        }
                    ]
                });
            } else {
                dialogText.text("System is not yet fully initialized! Please wait...")
                dialogObj.dialog({
                    open: function(event, ui) {
                        // resolving conflicting close buttons between jquery and boostrao
                        $(".ui-dialog-titlebar-close", ui.dialog | ui).hide();                           
                    },
                    position: {
                      my: "right bottom",
                      at: "right bottom",
                      of: window
                    },
                    dialogClass: 'dialog-titlebar-not-ready',
                    show: "slide",
                    resizeable: false,
                    draggable: true,
                    title: "System Health Check",
                    height: 100,
                    width: 250,
                    modal: false,
                });
            }
        },
        error: function(err) {
            console.log("Error in system health check: " + JSON.stringify(err));
        }
    });
}


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


//opens the dialog to view details about a driver (of currently installed drivers)
function openContentPanel() {
    if (!$("#driver-content-panel").hasClass("open")) {
        tweenBlackScreen.play();
        tweenContentPanel.play();
        $("#driver-content-panel").addClass("open");
        $("#driver-content-panel").removeClass("hidden");
        $("#driver-content-panel").addClass("active");
    }
}

//closes the dialog to view details about a driver (of currently installed drivers)
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
    updateDrivers(); //explicitly calling the function to load the driver templates
    
    // call the system health check every 1.5 seconds when the drivers are loaded
    setInterval(loadSystemHealthCheck(), 1500);

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
    saveButton.className = "button-profile-select btn btn-default";
    saveButton.innerHTML = "Save Driver";
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

/*
 * Opens an input box for the user to enter the driver template name and description
 * for the driver they want to save as a template (during the installation process).
 */
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

/*
 * Saves the driver as a template. Reads values directly from the input box
 * @returns {undefined}
 */
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
            // since the driver is being added during the installation process, do not jump out of the installation process
           reloadData(); // just reload the data so if users decide to cancel installation, the template still shows up
        },
        error: function () {
        }
    });
}

/*
 * Deletes saved templates based on topuri
 */

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
            reloadData();
        },
        error: function (result) {
            console.log("Error in removeDriverProfile: " + result);
        }
    });
}

// shows the (latest) template's panel
function updateDrivers() {
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
            
            /*
             * The return format for the rest query is the columns of each record
             * in the SQL table separated by commas. Each record is also separated
             * by commas as well.
             * Example: Given there are records in the templates table like so:
             * Record 1: Driver1Name | Driver1Type | Driver1TopUri | Driver1Desc | Driver1Data
             * Record 2: Driver2Name | Driver2Type | Driver2TopUri | Driver2Desc | Driver2Data
             * 
             * The result of the query would like this:
             * Driver1Name,Driver1Type,Driver1TopUri,Driver1Desc,Driver1Data,Driver2Name,Driver2Type,Driver2TopUri,Driver2Desc,Driver2Data
             */
            for (var i = 0; i < result.length; i += 5) {
                var row = document.createElement("tr");
                var drivername = document.createElement("td");
                var description  = document.createElement("td");
                var buttonsCell = document.createElement("td");
                var detButton = document.createElement("button");
                var delButton = document.createElement("button");
                var edButton = document.createElement("button");
                var installButton = document.createElement("button");
                
                // currently using the topuri as the unique identifier
                var uri = result[i + 2];
                

                detButton.className = "button-profile-select btn btn-default";
                detButton.style.width = "64px";
                detButton.innerHTML = "Details";
                detButton.id = uri;
                
                detButton.onclick = function () {
                    $("#driver-content-panel").removeClass("hidden");
                    $("#driver-content-panel").addClass("active");
                    $("#info-panel-title").text("Details");
                    clearPanel();
                    activateSide();
                    getDetailsProfile(this.id);
                    openContentPanel();
                };
                
                
                delButton.className = "button-profile-select btn btn-danger";
                delButton.style.width = "64px";
                delButton.innerHTML = "Delete";
                delButton.id = uri;
                
                //storing driver profile name for deletion confirmation dialog
                delButton.setAttribute("del-button-for", result[i]);
                delButton.onclick = function () {
                    var driverId = this.id;
                    var driverNameFromAttribute = this.getAttribute("del-button-for");
                    
                    //setting text of jquery dialog
                    $("#dialog-confirm-text").text(driverNameFromAttribute);
                    //jquery dialog
                    $("#dialog-confirm").dialog({
                        open: function(event, ui) {
                            // resolving conflicting close buttons between jquery and boostrao
                            $(".ui-dialog-titlebar-close", ui.dialog | ui).hide();                           
                        },
                        show: "slide",
                        resizeable: false,
                        draggable: false,
                        title: "Are you sure you want to delete this template?",
                        height: "auto",
                        width: 400,
                        modal: true,
                        buttons: [
                            {
                                text: "Delete",
                                "class": "button-profile-select btn btn-danger",
                                click: function () {
                                    removeDriverProfile(driverId);
                                    $(this).dialog("close");
                                }
                            },
                            {
                                text:"Close",
                                "class": "button-profile-select btn btn-default",
                                click: function () {
                                    $(this).dialog("close");
                                }
                            }
                        ]
                    });
                };

                edButton.innerHTML = "Edit";
                edButton.style.width = "64px";
                edButton.className = "button-profile-select btn btn-warning";
                edButton.id = uri;
                edButton.onclick = function () {
                    $("#driver-content-panel").removeClass("hidden");
                    $("#driver-content-panel").addClass("active");
                    $("#info-panel-title").text("Details");
                    clearPanel();
                    activateSide();                   
                    editDriverProfile(this.id);

                    openContentPanel();
                };
                
                
                installButton.innerHTML = "Install";
                installButton.style.width = "64px";
                installButton.className = "button-profile-select btn btn-primary";
                installButton.id = uri;
                installButton.onclick = function () {                    
                    plugDriver(this.id); // Install the profile as a driver
                }
                


                drivername.innerHTML = result[i];
                description.innerHTML = result[i + 3];
                
                buttonsCell.appendChild(detButton);
                buttonsCell.appendChild(installButton);
                buttonsCell.appendChild(edButton);
                buttonsCell.appendChild(delButton);
                buttonsCell.style.width = "350px";

                row.appendChild(drivername);
                row.appendChild(description);
                row.appendChild(buttonsCell);
                table.appendChild(row);
            }
        }
    });
}
function editDriverProfile(clickID) {
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
            var topuri = result["TOPURI"]; //pulling out the unedited TOPURI key as it is acting as a primary key
            for (var key in result) {
                if (result.hasOwnProperty(key)) {
                    var row = document.createElement("tr");
                    var tempkey = document.createElement("td");
                    var tempval = document.createElement("td");
                    var editableVal = document.createElement("textarea");
                    editableVal.rows = "1";
                    
                    tempkey.innerHTML = key;
                    editableVal.id = key;
                    editableVal.value = result[key];
                    tempval.appendChild(editableVal);
                    row.appendChild(tempkey);
                    row.appendChild(tempval);
                    table.appendChild(row);
                    
                }
            }

            var saveEditedProfileButton = document.createElement("button");
            saveEditedProfileButton.className = "button-profile-select btn btn-success";
            
            //since topuri is the primary key, will pass the unedited topuri into saveEditedDriverProfile as to change that one profile
            saveEditedProfileButton.id = topuri;
            saveEditedProfileButton.innerHTML = "Save Changes";
            saveEditedProfileButton.onclick = function () {
                saveEditedDriverProfile(this.id); // function get values in the install-options div and updates driver
                
            };
            
            botpanel.appendChild(saveEditedProfileButton);
            $("#info-panel-title").text("Edit Details");
        },
        error: function (xhr, status, error) {
            console.log("Failure. Status: "  + status +  ", errorThrown: " + error);
        }
    });
    panel.appendChild(table);
}

/*
 * Get value in the install-options divs and updates the driver profile
 */
function saveEditedDriverProfile(oldtopuri){
    var userId = keycloak.tokenParsed.preferred_username;
    var apiUrl = baseUrl + '/StackV-web/restapi/app/driver/' + userId + '/edit/' + oldtopuri;
    var jsonData = [];
    var tempData = {};
    var newtopuri = document.getElementById("TOPURI").value; // get the new topuri since it also has be to changed in the table
    
    
    
    // read all the inputs present (should only be the inputs present on the edit details screen)
    for (var temp of document.getElementsByTagName("textarea")) {
        if (temp !== '') {            
            tempData[temp.id] = temp.value;
        }    
    }
    jsonData.push(tempData);
    
    var dataColumn = JSON.stringify({jsonData});
    var sendData = JSON.stringify({
        username: userId,
        topuri: newtopuri,
        data: dataColumn
    });
    
    $.ajax({
        url: apiUrl,
        type: 'PUT',
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
            xhr.setRequestHeader("Refresh", keycloak.refreshToken);
        },
        contentType: 'application/json',
        data: sendData,
        success: function(result) {
            // update all previous id attributes of the old topuri with the new top uri
            $('button[id=' + oldtopuri + ']').attr("id", newtopuri);
            
            closeContentPanel(); //hide the edit driver panel
            //setting text of the jquery dialog
            $("#dialog-confirm-text").text(result);
            //jquery dialog
            $("#dialog-confirm").dialog({
                open: function(event, ui) {
                    //conflict fix for bootstrap and jquery close button
                    $(".ui-dialog-titlebar-close", ui.dialog | ui).hide();
                },
                show: "slide",
                resizable: false,
                draggable: false,
                title: "Edit Driver Result",
                height: "auto",
                width: 400,
                modal: true,
                buttons: [
                    {
                        text: "OK",
                        "class": "button-profile-select btn btn-default",
                        click: function () {
                            $(this).dialog("close");
                        }
                    }
                ]

            });
        },
        error: function(err) {
            console.log("Error in saveEditedDriverProfile: " + err);
        }
    });
}

/*
 * @param {string} clickID - the topology URI
 * Creating the drivers details panel for a saved template/profile
 */
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
            instDetailsButton.className = "button-profile-select btn btn-primary";
            instDetailsButton.innerHTML = "Install";
            instDetailsButton.onclick = function () {
                plugDriver(result["TOPURI"]);
            };
            botpanel.appendChild(instDetailsButton);
        },
        error: function (xhr, status, error) {
            console.log("Failure. Status: "  + status +  ", errorThrown: " + error);
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
        error: function () {
            window.location.href = "/StackV-web/";
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
                
                drivername.innerHTML = result[i + 2];
                description.innerHTML = result[i + 1];

                detButton.innerHTML = "Details";
                detButton.style.width = "64px";
                detButton.className = "button-profile-select btn btn-default";
                detButton.onclick = function () {
                    clearPanel();
                    activateSide();
                    getDetails(this.id);
                    openContentPanel();
                };

                detButton.id = result[i];
                delButton.innerHTML = "Delete";
                delButton.style.width = "64px";
                delButton.className = "button-profile-select btn btn-danger";
                delButton.setAttribute("del-button-for", result[i + 2]);       
                delButton.onclick = function () {
                   var driverNameFromAttr = this.getAttribute("del-button-for");
                   var driverId = this.id;
                   
          
                   //setting text of the jquery dialog
                   $("#dialog-confirm-text").text(driverNameFromAttr);
                   //jquery dialog
                   $("#dialog-confirm").dialog({
                       open: function(event, ui) {
                           // bootsrap and jquery have a conflict when displaying certain buttons 
                           // (in this case the close - 'X' ) in the top right hand corner of the dialog.
                           // So current solution is to hide that button and still allow us to use
                           // bootstrap styling within the jquery dialog
                           $(".ui-dialog-titlebar-close", ui.dialog | ui).hide();
                           
                           // other solution is to separate the two with: $.fn.bootstrapBtn = $.fn.button.noConflict();
                           // but prevents use of boostrap styling in the jquery dialog
                       },
                       show: "slide",
                       resizable: false,
                       draggable: false,
                       title: "Are you sure you want to delete this driver?",
                       height: "auto",
                       width: 400,
                       modal: true,
                       buttons: [
                           {
                               text: "Delete",
                               "class": "button-profile-select btn btn-danger",
                               click: function () {
                                   removeDriver(driverId);
                                   $(this).dialog("close");
                               }
                           },
                           {
                               text: "Cancel",
                               "class": "button-profile-select btn btn-default",
                               click: function () {
                                   $(this).dialog("close");
                               }
                           }
                       ]
                       
                   });
                  };

                delButton.id = result[i + 2];
                
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
    console.log("in removeDriver: topURI -> " + topUri);
    $.ajax({
        url: apiUrl,
        type: 'DELETE',
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
            xhr.setRequestHeader("Refresh", keycloak.refreshToken);
        },
        success: function () {            
            updateDrivers(topUri);
            reloadData();
        },
        error: function (result) {
            clearPanel();
            activateSide();
            console.log("removeDriver error: " + result);

            $("#info-panel-title").text("Failed Due to Service Instances:");
            var body = $("#info-panel-body");
            var bodyText = "";
            for (var i = 1; i < result.length; i++) {
                console.log("result[i]: " + result[i]);
                bodyText += result[i] + "\n"
            }
            console.log("bodyText: " + bodyText);
            body.text(bodyText);

            openContentPanel();

        }
    });
}

/*
 * Gets the details of one single isntalled driver
 */
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
        beforeSend: function(xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
            xhr.setRequestHeader("Refresh", keycloak.refreshToken);
        },
        success: function(result) {
            for (var i = 0; i < result.length; i += 2) {
                var row = document.createElement("tr");
                var tempkey = document.createElement("td");
                var tempval = document.createElement("td");
                tempkey.innerHTML = result[i];
                var tempvalString = result[i + 1];
                // create a shorter version (up to 120 characters) of the values of the driver's keys
                var tempvalStringShort = tempvalString.substring(0,121) + "...";
                
                // handles if the details overflow the size of the table cell
                // currently handled by replacing text with a button that brings up popup with text of details
                // can also implement a tooltip
                if (tempvalString.length > 80) {
                    //if the value is greater than 80 - make a button which will show the info in a pane
                    var valDetailsBtn = document.createElement("button");
                    
                    //assigning the text of the key (detail name) to the btn id
                    valDetailsBtn.id = tempkey.innerHTML;
                    
                    // need to store the string somewhere unique to the button so it can be accessed later by its onclick function.
                    // the title attribute value will be used by the tooltip when it is initialized so the string cannot be stored
                    // in the title attribute. Instead the shortened string is stored in the title attribute and the full string is
                    // stored in a custom attribute called details-value
                    valDetailsBtn.title = tempvalStringShort;
                    valDetailsBtn.setAttribute("details-value", tempvalString);
                    
                    valDetailsBtn.innerHTML = "View Value";
                    
                    //bootstrap tooltip attribute
                    valDetailsBtn.setAttribute("data-toggle","tooltip");
                    valDetailsBtn.setAttribute("data-placement", "right");
                                                            
                    valDetailsBtn.className = "button-profile-select btn btn-default";
                    valDetailsBtn.onclick = function () {
                        var detailName = this.id;
                        var detailValue = this.getAttribute("details-value");
                        
                        // setting the value of the detail to body of the dialog 
                        $("#dialog-overflow-details-text").text(detailValue);
                        
                        $("#dialog-overflow-details").dialog({
                            open: function(event, ui) {
                                // bootsrap and jquery have a conflict when displaying certain buttons 
                                // - in this case the close - 'X'  in the top right hand corner of the dialog.
                                // So current solution is to hide that button and still allow us to use
                                // bootstrap within the jquery dialog
                                $(".ui-dialog-titlebar-close", ui.dialog | ui).hide();

                                // other solution is to separate the two with: $.fn.bootstrapBtn = $.fn.button.noConflict();
                                // but prevents use of boostrap styling in the jquery dialog
                            },
                            minHeight: 0,
                            create: function() {
                              $(this).css("maxHeight",450);  
                            },
                            show: "slide",
                            width: 400,
                            resizable: false,
                            draggable: false,
                            title: detailName,
                            modal: true,
                            buttons: [
                                {
                                    text: "Close",
                                    "class": "button-profile-select btn btn-default",
                                    click: function () {
                                        $(this).dialog("close");
                                    }
                                }
                            ]

                        });
                        
                        
                    };
                    tempval.appendChild(valDetailsBtn);
                } else {
                    // if the value string is less than 80, then just display it in the display
                    tempval.innerHTML = tempvalString;
                }
                row.appendChild(tempkey);
                row.appendChild(tempval);
                table.appendChild(row);
                panel.appendChild(table);
            }
            $('[data-toggle="tooltip"]').tooltip(); //activating tooltip
        }
    });
}

/*
 * Installs a driver from its driver profile
 */
function plugDriver(topuri) {
    var URI = topuri;
    var userId = keycloak.tokenParsed.preferred_username;
    var apiUrl = baseUrl + '/StackV-web/restapi/app/driver/' + userId + '/install/' + URI;

    $.ajax({
        url: apiUrl,
        type: 'PUT',
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
            xhr.setRequestHeader("Refresh", keycloak.refreshToken);
        },
        success: function (result) {
            getAllDetails();
            
            clearPanel();
            closeContentPanel();
            //setting text of the jquery dialog
            $("#dialog-confirm-text").text(result);
            //jquery dialog
            $("#dialog-confirm").dialog({
                open: function(event, ui) {
                    // bootsrap and jquery have a conflict when displaying certain buttons 
                    // (in this case the close - 'X' ) in the top right hand corner of the dialog.
                    // So current solution is to hide that button and still allow us to use
                    // bootstrap styling within the jquery dialog
                    $(".ui-dialog-titlebar-close", ui.dialog | ui).hide();
                },
                show: "slide",
                resizable: false,
                draggable: false,
                title: "Driver Installation Result",
                height: "auto",
                width: 400,
                modal: true,
                buttons: [
                    {
                        text: "OK",
                        "class": "button-profile-select btn btn-default",
                        click: function () {
                            $(this).dialog("close");
                        }
                    }
                ]

            });
        },
        error: function(result) {
            console.log("Error in plugDriver: " + result);
        }
    });
}

/*
 * Installs a driver from json
 */
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

    tempData["driverType"] = type;
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

            //setting text of the jquery dialog
            $("#dialog-confirm-text").text(result);
            //jquery dialog
            $("#dialog-confirm").dialog({
                open: function(event, ui) {
                    // bootsrap and jquery conflict fix
                    $(".ui-dialog-titlebar-close", ui.dialog | ui).hide();
                },
                show: "slide",
                resizable: false,
                draggable: false,
                title: "Driver Installation Result",
                height: "auto",
                width: 400,
                modal: true,
                buttons: [
                    {
                        text: "OK",
                        "class": "button-profile-select btn btn-default",
                        click: function () {
                            $(this).dialog("close");
                        }
                    }
                ]

            });
            
            //delay the getAllDetails call by 3 seconds
            setTimeout(getAllDetails(), 3000);
        },
        error: function (textStatus, errorThrown) {
            console.log("installDriver error => textStatus " + textStatus + ", errorThrown: " + errorThrown);
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
                loadSystemHealthCheck();
                refreshSync(refreshed, timerSetting);
            }, 1000);
        } else {
            setTimeout(function () {
                loadDriverPortal();
                loadSystemHealthCheck();
                refreshSync(refreshed, timerSetting);
            }, 500);
        }
    });
}
