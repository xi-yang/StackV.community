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

baseUrl = window.location.origin;
var keycloak = Keycloak('/StackV-web/data/json/keycloak.json');

function driverBotTable() {
    var tbody = document.getElementById("installed-body");
    
    for (var i = 0; i < 20; i++){
        
        var row = document.createElement("tr");
        var cell1_1 = document.createElement("td");
        cell1_1.innerHTML = "TEST";
        var cell1_2 = document.createElement("td");
        cell1_2.innerHTML = "TEST";
        var cell1_3 = document.createElement("td");
        cell1_3.innerHTML = "<button style='width: 50px;' onclick='activateDetails();' class='details' id='details-button'>Details</button>";
        cell1_3.innerHTML += "<div class='divider'/>";
        cell1_3.innerHTML += "<button style ='width: 50px;' class='delete' id='delete-button'>Delete</button>";
        cell1_3.style.width = "160px";
        row.appendChild(cell1_1);
        row.appendChild(cell1_2);
        row.appendChild(cell1_3);
        tbody.appendChild(row);
    }
}
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
    var fourth = document.createElement("input");
    var divContent = document.getElementById("install-type");
    var drivername = document.createElement("p");
    var driver = document.createElement("input");
    var descname = document.createElement("p");
    var desc = document.createElement("input");
    
    
    type.innerHTML = "StubSystemDriver";
    type.style.color = "white";
    type.id = "drivertype";
    divContent.appendChild(type);
    
    drivername.innerHTML="Driver Name:";
    drivername.style.color = "white";
    driver.type="text";
    driver.id="drivername";
    divContent.appendChild(drivername);
    divContent.appendChild(driver);
    
    
    first.innerHTML="Topology URI:";
    first.style.color = "white";
    
    second.type="text";
    second.id="TOPURI";
    
    third.innerHTML="TTL:";
    third.style.color = "white";
    
    fourth.type="test";
    fourth.id="TTL";
    
    divContent.appendChild(first);
    divContent.appendChild(second);
    divContent.appendChild(third);
    divContent.appendChild(fourth);
    
    
    descname.innerHTML="Description:";
    descname.style.color = "white";
    desc.type="text";
    desc.id="description";
    divContent.appendChild(descname);
    divContent.appendChild(desc);
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
    var drivername = document.createElement("p");
    var driver = document.createElement("input");
    var descname = document.createElement("p");
    var desc = document.createElement("input");
    
    type.innerHTML = "AwsDriver";
    type.style.color = "white";
    type.id = "drivertype";
    divContent.appendChild(type);
    
    drivername.innerHTML="Driver Name:";
    drivername.style.color = "white";
    driver.type="text";
    driver.id="drivername";
    divContent.appendChild(drivername);
    divContent.appendChild(driver);
    
    
    first.innerHTML="Topology URI:";
    first.style.color = "white";
    
    second.type="text";
    second.id="TOPURI";
    
    third.innerHTML="Amazon Access ID:";
    third.style.color = "white";
    
    fourth.type="text";
    fourth.id = "Amazon-Access_ID";
    
    fifth.innerHTML="Amazon Secret Key:";
    fifth.style.color = "white";
    
    sixth.type="text";
    sixth.id = "Amazon-Secret-Key";
    
    divContent.appendChild(first);
    divContent.appendChild(second);
    divContent.appendChild(third);
    divContent.appendChild(fourth);
    divContent.appendChild(fifth);
    divContent.appendChild(sixth);
    
    
    descname.innerHTML="Description:";
    descname.style.color = "white";
    desc.type="text";
    desc.id="description";
    divContent.appendChild(descname);
    divContent.appendChild(desc);
    
}
function installOpenstack(){
    var type = document.createElement("p");
    var divContent = document.getElementById("install-type");
    var drivername = document.createElement("p");
    var driver = document.createElement("input");
    var descname = document.createElement("p");
    var desc = document.createElement("input");
    var content = [];
    
    type.innerHTML = "OpenStackDriver";
    type.style.color = "white";
    type.id = "drivertype";
    divContent.appendChild(type);
    
    drivername.innerHTML="Driver Name:";
    drivername.style.color = "white";
    driver.type="text";
    driver.id="drivername";
    divContent.appendChild(drivername);
    divContent.appendChild(driver);
    
    
    for (var i = 0; i < 12; i+=2){
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
    content[3].id = "Openstack-Username";
    content[4].innerHTML="Openstack Password";
    content[5].id = "Openstack-Password";
    content[6].innerHTML= "NAT Server:";
    content[7].id = "NAT-Server";
    content[8].innerHTML= "URL";
    content[9].id = "URL";
    content[10].innerHTML= "Tenant:";
    content[11].id = "tenant";
    
    
    for (var i = 0; i < 12; i++){
        divContent.appendChild(content[i]);
    }
    
    
    descname.innerHTML="Description:";
    descname.style.color = "white";
    desc.type="text";
    desc.id="description";
    divContent.appendChild(descname);
    divContent.appendChild(desc);
}
function installStack(){
    var type = document.createElement("p");
    var first = document.createElement("p");
    var second = document.createElement("input");
    var third = document.createElement("p");
    var fourth = document.createElement("input");
    var divContent = document.getElementById("install-type");
    var drivername = document.createElement("p");
    var driver = document.createElement("input");
    var descname = document.createElement("p");
    var desc = document.createElement("input");
    
    type.innerHTML = "StackSystemDriver";
    type.style.color = "white";
    type.id = "drivertype";
    divContent.appendChild(type);
    
    drivername.innerHTML="Driver Name:";
    drivername.style.color = "white";
    driver.type="text";
    driver.id="drivername";
    divContent.appendChild(drivername);
    divContent.appendChild(driver);
    
    
    first.innerHTML="Topology URI:";
    first.style.color = "white";
    
    second.type="text";
    second.id="TOPURI";
    
    third.innerHTML="Subsystem Base URL:";
    third.style.color = "white";
    
    fourth.type="text";
    fourth.id = "Subsystem-Base-URL";
    
    divContent.appendChild(first);
    divContent.appendChild(second);
    divContent.appendChild(third);
    divContent.appendChild(fourth);
    
    
    descname.innerHTML="Description:";
    descname.style.color = "white";
    
    desc.type="text";
    desc.id="description";
    
    divContent.appendChild(descname);
    divContent.appendChild(desc);
}    
function clearPanel(){
    $('#install-type').empty();
    $('#install-options').empty();
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
    saveButton.onclick = function() {addDriver();};
    document.getElementById('install-options').appendChild(saveButton);
    var instButton = document.createElement("button");
    instButton.innerHTML = "Install Driver";
    instButton.onclick = function() {plugDriver();};
    document.getElementById('install-options').appendChild(instButton);
}
function changeNameDet() {
    var detailsButton = document.createElement("button");
    document.getElementById('side-name').innerHTML="Details";
    detailsButton.innerHTML = "get dets";
    detailsButton.onclick = function() { getAllDetails(); };
    document.getElementById('install-options').appendChild(detailsButton);
}

function addDriver() {
    var userId = keycloak.subject;
    var apiUrl = baseUrl + '/StackV-web/restapi/app/driver/' + userId + '/add';
    var jsonData=[];
    var description = document.getElementById("description").value;
    var driver = document.getElementById("drivername").value;
    var URI = document.getElementById("TOPURI").value;
    var type = document.getElementById("drivertype").innerHTML;
    
    for(var temp of document.getElementsByTagName("input")){
        if(temp !== document.getElementById("description") && 
                temp.value !== document.getElementById("drivername")){
            tempid= temp.id;
            jsonData.push({tempid : temp.value});
                }
    }
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
        },
        contentType: 'application/json',
        data: sentData,
        success: function() {
            updateDrivers();
        }
    });
}
function removeDriverProfile(clickID) {
    var userId = keycloak.subject;
    var topuri = clickID;
    var apiUrl = baseUrl + '/StackV-web/restapi/app/driver/' + userId + '/delete/' + topuri;
    $.ajax({
        url: apiUrl,
        type: 'DELETE',
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
        },
        success: function (){
            updateDrivers();
        }
    });
}
function updateDrivers() {
    var userId = keycloak.subject;
    var table = document.getElementById("saved-table");
    var apiUrl = baseUrl + '/StackV-web/restapi/app/driver/' + userId + '/get';
    $.ajax({
        url: apiUrl,
        type: 'GET',
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
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
    var userId = keycloak.subject;
    var panel = document.getElementById("install-type");
    var topuri = clickID;
    var apiUrl = baseUrl + '/StackV-web/restapi/app/driver/' + userId + '/getdetails/' + topuri;
    $.ajax({
        url: apiUrl,
        type: 'GET',
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
        },
        success: function (result){
            
            var data = document.createElement("p");
            
            data.style.color = "white";
            data.innerHTML = result;
            panel.appendChild(data);
        }
    });
}

function getAllDetails(){
    var table = document.getElementById("installed-body");
    var apiUrl = baseUrl + '/StackV-web/restapi/driver/';
    $.ajax({
        url: apiUrl,
        type: 'GET',
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
        },
        success: function (result){
            //fill installed table
            $('#installed-body').empty();
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
                    activateDetails(); changeNameDet(); getDetails(this.id);};
                detButton.style.width = "50px";
                detButton.id = result[i+3];
                
                delButton.innerHTML = "Delete";
                delButton.onclick = function() {removeDriver(this.id);};
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
function removeDriver(clickID) {
    var topUri = clickID;
    var apiUrl = baseUrl + '/StackV-web/restapi/driver/' + topUri;
    $.ajax({
        url: apiUrl,
        type: 'DELETE',
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
        },
        success: function (){
            getAllDetails();
        }
    });
}
function getDetails(clickID) {
    var topUri = clickID;
    var panel = document.getElementById("install-type");
    var apiUrl = baseUrl + '/StackV-web/restapi/driver/' + topUri;
    $.ajax({
        url: apiUrl,
        type: 'GET',
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
        },
        success: function (result){
            //update side panel with info
            var data = document.createElement("p");
            
            data.style.color = "white";
            data.innerHTML = result;
            panel.appendChild(data);
        }
    });
}
function plugDriver(){
    
}