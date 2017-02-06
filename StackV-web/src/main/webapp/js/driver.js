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

/* global XDomainRequest, baseUrl, keycloak */

function activateDetails(){
    $('#driver-panel-right').addClass('active-detail');
    $('#detail-content').addClass('active');
    $('#driver-panel-top').removeClass('no-side-tab');
    $('#driver-panel-bot').removeClass('no-side-tab');
    $('#driver-panel-top').addClass('side-tab');
    $('#driver-panel-bot').addClass('side-tab');
}

function activateSide(){
    $('#driver-panel-right').addClass('active-detail');
    $('#install-content').addClass('active');
    $('#driver-panel-top').removeClass('no-side-tab');
    $('#driver-panel-bot').removeClass('no-side-tab');
    $('#driver-panel-top').addClass('side-tab');
    $('#driver-panel-bot').addClass('side-tab');
}

function closeSide(){
    document.getElementById("driver-panel-right").className = "inactive";
    $('#install-tab').removeClass = "active";
    $('#install-content').removeClass('active');
    $('#driver-panel-top').removeClass('side-tab');
    $('#driver-panel-bot').removeClass('side-tab');
    $('#driver-panel-top').addClass('no-side-tab');
    $('#driver-panel-bot').addClass('no-side-tab');
    
}
function installStub(){
    var type = document.createElement("p");
    var first = document.createElement("p");
    var second = document.createElement("input");
    var third = document.createElement("p");
    var fourth = document.createElement("textarea");
    var divContent = document.getElementById("install-type");
    
    type.innerHTML = "StubSystemDriver";
    type.style.color = "white";
    type.id = "drivertype";
    divContent.appendChild(type);
    
    first.innerHTML="Topology URI:";
    first.style.color = "white";
    
    second.type="text";
    second.id="TOPURI";
    
    third.innerHTML="TTL:";
    third.style.color = "white";

    fourth.id="stubModelTtl";
    fourth.rows = "20";
    fourth.cols = "50";
    
    divContent.appendChild(first);
    divContent.appendChild(second);
    divContent.appendChild(third);
    divContent.appendChild(fourth);
}
function installAWS(){
    var type = document.createElement("p");
    var first = document.createElement("p");
    var second = document.createElement("input");
    var third = document.createElement("p");
    var fourth = document.createElement("input");
    var fifth = document.createElement("p");
    var sixth = document.createElement("input");
    var divContent = document.getElementById("install-type");

    type.innerHTML = "AwsDriver";
    type.style.color = "white";
    type.id = "drivertype";
    divContent.appendChild(type);
    
    
    first.innerHTML="Topology URI:";
    first.style.color = "white";
    
    second.type="text";
    second.id="TOPURI";
    
    third.innerHTML="Amazon Access ID:";
    third.style.color = "white";
    
    fourth.type="text";
    fourth.id = "aws_access_key_id";
    
    fifth.innerHTML="Amazon Secret Key:";
    fifth.style.color = "white";
    
    sixth.type="text";
    sixth.id = "aws_secret_access_key";
    
    divContent.appendChild(first);
    divContent.appendChild(second);
    divContent.appendChild(third);
    divContent.appendChild(fourth);
    divContent.appendChild(fifth);
    divContent.appendChild(sixth);    
}
function installOpenstack(){
    var type = document.createElement("p");
    var divContent = document.getElementById("install-type");
    var divContentRight = document.getElementById("install-type-right");

    var extName = document.createElement("p");
    var ext = document.createElement("textarea");
    var content = [];
    
    divContent.style = "float: left;";
    divContentRight.style = "float: right;";
    document.getElementById("install-options").style = "position: absolute; bottom: 300px; right: 285px";
    
    type.innerHTML = "OpenStackDriver";
    type.style.color = "white";
    type.id = "drivertype";
    divContent.appendChild(type);
    
    for (var i = 0; i < 22; i+=2){
        var textbox = document.createElement("p");
        var input = document.createElement("input");
        textbox.style.color="white";
        if((i + 1) !== 7){
            input.type="text";
        }
        else {
            input.type="checkbox";
        }
        content[i]=textbox;
        content[i+1]=input;
    }
    
    content[0].innerHTML="Topology URI:";
    content[1].id = "TOPURI";
    content[2].innerHTML="Openstack Username:";
    content[3].id = "username";
    content[4].innerHTML="Openstack Password:";
    content[5].id = "password";
    content[6].innerHTML= "NAT Server:";
    content[7].id = "NATServer";
    content[8].innerHTML= "URL:";
    content[9].id = "url";
    content[10].innerHTML= "Tenant:";
    content[11].id = "tenant";
    content[12].innerHTML= "Admin Username:";
    content[13].id = "adminUsername";
    content[14].innerHTML= "Admin Password:";
    content[15].id = "adminPassword";
    content[16].innerHTML= "Admin Tenant:";
    content[17].id = "adminTenant";
    content[18].innerHTML= "Default Image:";
    content[19].id = "defaultImage";
    content[20].innerHTML= "Default Flavor:";
    content[21].id = "defaultFlavor";
    
    
    extName.innerHTML= "ModelExt:";
    extName.style.color = "white";
    ext.id = "modelExt";
    ext.rows = "10";
    ext.cols = "30";
    
    
    for (var i = 0; i < 16; i++){
        divContent.appendChild(content[i]);
    }
    
    for (var i = 16; i < 22; i++){
        divContentRight.appendChild(content[i]);
    }
    
    
    divContentRight.appendChild(extName);
    divContentRight.appendChild(ext);
}
function installStack(){
    var type = document.createElement("p");
    var first = document.createElement("p");
    var second = document.createElement("input");
    var third = document.createElement("p");
    var fourth = document.createElement("input");
    var divContent = document.getElementById("install-type");
    
    type.innerHTML = "StackSystemDriver";
    type.style.color = "white";
    type.id = "drivertype";
    divContent.appendChild(type);
    
    first.innerHTML="Topology URI:";
    first.style.color = "white";
    
    second.type="text";
    second.id="TOPURI";
    
    third.innerHTML="Subsystem Base URL:";
    third.style.color = "white";
    
    fourth.type="text";
    fourth.id = "subsystemBaseUrl";
    
    divContent.appendChild(first);
    divContent.appendChild(second);
    divContent.appendChild(third);
    divContent.appendChild(fourth);
}    
function clearPanel(){
    $('#install-type').empty();
    $('#install-options').empty();
    $('#install-type-right').empty();
    document.getElementById("install-type-right").style = "";
    document.getElementById("install-type").style = "";
    document.getElementById("install-options").style = "";
    var closeButton = document.createElement("button");
    closeButton.innerHTML = "Close";
    closeButton.onclick = function() {clearPanel(); closeSide();};
    document.getElementById('install-options').appendChild(closeButton);
}
function clearText(){
    for(var temp of document.getElementsByTagName("input")){
        temp.value = "";
    }
}
function changeNameInst() {
    var saveButton = document.createElement("button");
    document.getElementById('side-name').innerHTML="Install";
    saveButton.innerHTML = "Save Driver";
    saveButton.onclick = function() {openWindow();};
    document.getElementById('install-options').appendChild(saveButton);
    var instButton = document.createElement("button");
    instButton.innerHTML = "Install Driver";
    instButton.onclick = function() {installDriver();};
    document.getElementById('install-options').appendChild(instButton);
}
function openWindow(){
    $('#info-fields').empty();
    $('#info-option').empty();
    var divContent = document.getElementById("info-fields");
    var drivername = document.createElement("p");
    var driver = document.createElement("input");
    var descname = document.createElement("p");
    var desc = document.createElement("input");
    var saveButton = document.createElement("button");
    
    $('#black-screen').removeClass();
    $('#info-panel').addClass("active");
    
    drivername.innerHTML="Driver Name:";
    driver.type="text";
    driver.id="drivername";
    divContent.appendChild(drivername);
    divContent.appendChild(driver);
    
    
    descname.innerHTML="Description:";
    
    desc.type="text";
    desc.id="description";
    
    divContent.appendChild(descname);
    divContent.appendChild(desc);
    
    saveButton.innerHTML = "Save Driver";
    saveButton.onclick = function() {addDriver();};
    document.getElementById("info-option").appendChild(saveButton);
}
function changeNameDet() {
    document.getElementById('side-name').innerHTML="Details";
}
function addDriver() {
    var userId = keycloak.tokenParsed.preferred_username;
    var apiUrl = baseUrl + '/StackV-web/restapi/app/driver/' + userId + '/add';
    var jsonData=[];
    var tempData = {};
    var description = document.getElementById("description").value;
    var driver = document.getElementById("drivername").value;
    var URI = document.getElementById("TOPURI").value;
    var type = document.getElementById("drivertype").innerHTML;
    
    $('#black-screen').addClass("off");
    $('#info-panel').removeClass();
    
    for(var temp of document.getElementsByTagName("input")){
        if(temp !== document.getElementById("description") && 
                temp !== document.getElementById("drivername")
                && temp.value !== ''){
            tempData[temp.id] = temp.value;
        }
    }
    
    for(var temp of document.getElementsByTagName("textarea")){
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
        success: function() {
            updateDrivers();
        }
    });
}
//meds to change
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
        success: function (){
            updateDrivers();
        }
    });
}
//needs to change
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
        success: function (result){
            $('#saved-table').empty();
            for (var i = 0; i < result.length; i += 4){
                var row = document.createElement("tr");
                var drivername = document.createElement("td");
                var description = document.createElement("td");
                var cell3 = document.createElement("td");
                var detButton = document.createElement("button");
                var delButton = document.createElement("button");
                var spacer = document.createElement("div");
                
                detButton.innerHTML = "Details";
                detButton.onclick = function() {clearPanel(); activateSide(); 
                    activateDetails(); changeNameDet(); getDetailsProfile(this.id);};
                detButton.style.width = "50px";
                detButton.id = result[i+3];
                
                delButton.innerHTML = "Delete";
                delButton.onclick = function() {removeDriverProfile(this.id);};
                delButton.style.width = "50px";
                delButton.id = result[i+3];
                
                spacer.style.width = "25px";
                
                drivername.innerHTML = result[i];
                description.innerHTML = result[i+1];
                cell3.appendChild(detButton);
                cell3.appendChild(spacer);
                cell3.appendChild(delButton);
                cell3.style.width = "170px";
                
                row.appendChild(drivername);
                row.appendChild(description);
                row.appendChild(cell3);
                table.appendChild(row);
            }
        }
    });
}

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
        success: function (result){
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
            instDetailsButton.innerHTML = "Install";
            instDetailsButton.onclick = function() {plugDriver(result["TOPURI"]);};
            botpanel.appendChild(instDetailsButton);
        }
    });
    panel.appendChild(table);
}

function getAllDetails(){
    var table = document.getElementById("installed-body");
    var apiUrl = baseUrl + '/StackV-web/restapi/driver/';
    $.ajax({
        url: apiUrl,
        type: 'GET',
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
            xhr.setRequestHeader("Refresh", keycloak.refreshToken);
        },
        success: function (result){
            //fill installed table
            $('#installed-body').empty();
            for (var i = 0; i < result.length; i += 3){
                var row = document.createElement("tr");
                var drivername = document.createElement("td");
                var description = document.createElement("td");
                var cell3 = document.createElement("td");
                var detButton = document.createElement("button");
                var delButton = document.createElement("button");
                var spacer = document.createElement("div");
                
                detButton.innerHTML = "Details";
                detButton.onclick = function() {clearPanel(); activateSide(); 
                    activateDetails(); changeNameDet(); getDetails(this.id);};
                detButton.style.width = "50px";
                detButton.id = result[i];
                
                delButton.innerHTML = "Delete";
                delButton.onclick = function() {removeDriver(this.id);};
                delButton.style.width = "50px";
                delButton.id = result[i+2];
                
                spacer.style.width = "25px";
                
                drivername.innerHTML = result[i+2];
                description.innerHTML = result[i+1];
                cell3.appendChild(detButton);
                cell3.appendChild(spacer);
                cell3.appendChild(delButton);
                cell3.style.width = "170px";
                
                row.appendChild(drivername);
                row.appendChild(description);
                row.appendChild(cell3);
                table.appendChild(row);
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
        success: function (){
            getAllDetails();
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
        success: function (result){
            
            for (var i = 0; i < result.length; i+=2) {
                var row = document.createElement("tr");
                var tempkey = document.createElement("td");
                var tempval = document.createElement("td");
                tempkey.innerHTML = result[i];
                tempval.innerHTML = result[i+1];
                row.appendChild(tempkey);
                row.appendChild(tempval);
                table.appendChild(row);
                
            }
        }
    });
    panel.appendChild(table);
}
function plugDriver(topuri){
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
        success: function (result){
            getAllDetails();
            $('#install-type').empty();
            var data = document.createElement("p");
            
            data.style.color = "white";
            data.innerHTML = result;
            panel.appendChild(data);
        }
    });   
}

function installDriver(){
    var panel = document.getElementById("install-type");
    var apiUrl = baseUrl + '/StackV-web/restapi/app/install/driver';
    var jsonData=[];
    var tempData = {};
    var type = document.getElementById("drivertype").innerHTML;
    
    for(var temp of document.getElementsByTagName("input")){
        if(temp !== document.getElementById("description") && 
                temp !== document.getElementById("drivername")
                && temp.value !== ''){
            tempData[temp.id] = temp.value;
        }
    }
    
    for(var temp of document.getElementsByTagName("textarea")){
        if (temp.value !== '')
            tempData[temp.id] = temp.value;
    }
    
    tempData["drivertype"] = type;
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
        success: function(result) {
            getAllDetails();
            $('#install-type').empty();
            var data = document.createElement("p");
            
            data.style.color = "white";
            data.innerHTML = result;
            panel.appendChild(data);
        }
    });
}