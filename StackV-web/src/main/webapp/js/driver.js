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
function driverTopTable() {
    var tbody = document.getElementById("template-body");
    
    for (var i = 0; i < 20; i++){
        
        var row = document.createElement("tr");
        var cell1_1 = document.createElement("td");
        cell1_1.innerHTML = "TEST";
        var cell1_2 = document.createElement("td");
        cell1_2.innerHTML = "TEST";
        var cell1_3 = document.createElement("td");
        cell1_3.innerHTML = "<button style='width: 50px;' onclick='activateDetails();' class='details' id='details-button'>Details</button>";
        cell1_3.innerHTML += "<div class='divider'/>";
        cell1_3.innerHTML += "<button style ='width: 50px;' onclick='activateInstall();' class='install' id='install-button'>Install</button>";
        cell1_3.style.width = "170px";
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
    var divContent = document.getElementById("install-type");
    var first = document.createElement("p");
    var second = document.createElement("input");
    var third = document.createElement("p");
    var fourth = document.createElement("input");
    first.innerHTML="Topology URI:";
    first.style.color = "white";
    second.type="text";
    third.innerHTML="TTL:";
    third.style.color = "white";
    fourth.type="test";
    divContent.appendChild(first);
    divContent.appendChild(second);
    divContent.appendChild(third);
    divContent.appendChild(fourth);
    var descname = document.createElement("p");
    var desc = document.createElement("input");
    descname.innerHTML="Description:";
    descname.style.color = "white";
    desc.type="text";
    divContent.appendChild(descname);
    divContent.appendChild(desc);
}
function installAWS(){
    var divContent = document.getElementById("install-type");
    var first = document.createElement("p");
    var second = document.createElement("input");
    var third = document.createElement("p");
    var fourth = document.createElement("input");
    var fifth = document.createElement("p");
    var sixth = document.createElement("input");
    first.innerHTML="Topology URI:";
    first.style.color = "white";
    second.type="text";
    third.innerHTML="Amazon Access ID:";
    third.style.color = "white";
    fourth.type="text";
    fifth.innerHTML="Amazon Sexret Key:";
    fifth.style.color = "white";
    sixth.type="text";
    divContent.appendChild(first);
    divContent.appendChild(second);
    divContent.appendChild(third);
    divContent.appendChild(fourth);
    divContent.appendChild(fifth);
    divContent.appendChild(sixth);
    var descname = document.createElement("p");
    var desc = document.createElement("input");
    descname.innerHTML="Description:";
    descname.style.color = "white";
    desc.type="text";
    divContent.appendChild(descname);
    divContent.appendChild(desc);
    
}
function installOpenstack(){
    var divContent = document.getElementById("install-type");
    var content = [];
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
    content[2].innerHTML="Openstack Username:";
    content[4].innerHTML="Openstack Password";
    content[6].innerHTML= "NAT Server:";
    content[8].innerHTML= "URL";
    content[10].innerHTML= "Tenant:";
    for (var i = 0; i < 12; i++){
        divContent.appendChild(content[i]);
    }
    var descname = document.createElement("p");
    var desc = document.createElement("input");
    descname.innerHTML="Description:";
    descname.style.color = "white";
    desc.type="text";
    divContent.appendChild(descname);
    divContent.appendChild(desc);
}
function installStack(){
    var divContent = document.getElementById("install-type");
    var first = document.createElement("p");
    var second = document.createElement("input");
    var third = document.createElement("p");
    var fourth = document.createElement("input");
    first.innerHTML="Topology URI:";
    first.style.color = "white";
    second.type="text";
    third.innerHTML="Subsystem Base URL:";
    third.style.color = "white";
    fourth.type="text";
    divContent.appendChild(first);
    divContent.appendChild(second);
    divContent.appendChild(third);
    divContent.appendChild(fourth);
    var descname = document.createElement("p");
    var desc = document.createElement("input");
    descname.innerHTML="Description:";
    descname.style.color = "white";
    desc.type="text";
    divContent.appendChild(descname);
    divContent.appendChild(desc);
}
function clearPanel(){
    $('#install-type').empty();
}
function changeNameInst() {
    document.getElementById('side-name').innerHTML="Install";
}
function changeNameDet() {
    document.getElementById('side-name').innerHTML="Details";
}
function myTest() {
    var apiUrl = baseUrl + '/StackV-web/restapi/app/driver/test';
    $.ajax({
        url: apiUrl,
        type: 'PUT',
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
        },
        success: function (){
            var divContent = document.getElementById("install-type");
            var first = document.createElement("p");
            first.innerHTML="SUCCESS";
            first.style.color = "white";
            divContent.appendChild(first);
            
        },
        error: function (){
            var divContent = document.getElementById("install-type");
            var first = document.createElement("p");
            first.innerHTML="FAILURE";
            first.style.color = "white";
            divContent.appendChild(first);
        }
    });
}
function addDriver () {
    var userId = keycloak.subject;
    var apiUrl = baseUrl + '/StackV-web/restapi/app/driver/' + userId + '/test';
    var settings;
    var description = document.getElementById("description").value;
    
    for(var temp of document.getElementsByTagName("input")){
        settings += temp.value + " ";
    }
    var data = [userId, settings, description];
    
    $.ajax({
        url: apiUrl,
        type: 'PUT',
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
        },
        data: data,
        success: function (result){
            //window.location.reload(true);
            //also update table wth new info
        }
    });
}