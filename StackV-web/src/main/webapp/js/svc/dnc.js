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

function addLinkDNC(){
    if (isNaN(linknum)){
        var linknum = 1;
    }
    linknum = linknum + 1;
    var panel = document.getElementById("link-body");
    var row = document.createElement("tr");
    var spacer = document.createElement("tr");
    var left = document.createElement("td");
    var right = document.createElement("td");
    var rightdiv = document.createElement("div");  
    var input = [];
    
    for (var i = 0; i < 5; i++){
        input[i] = document.createElement("input");
        input[i].type = "text";
        rightdiv.appendChild(input[i]);
    }
    
    input[0].id = "linkUri" + linknum;
    input[1].id = "linksrc" + linknum;
    input[2].id = "linksrc-vlan" + linknum;
    input[3].id = "linkdes" + linknum;
    input[4].id = "linkdes-vlan" + linknum;
    
    input[0].placeholder="Link-Name";
    input[1].placeholder="Source";
    input[2].placeholder="Vlan-tag";
    input[3].placeholder="Destination";
    input[4].placeholder="Vlan-tag";
    
    left.innerHTML = "Link " + linknum;
    
    spacer.id = "spacer";
    right.style = "width: 600px; text-align: center;";
    
    right.appendChild(rightdiv);
    row.appendChild(left);
    row.appendChild(right);
    panel.append(row);
    panel.append(spacer);
}

function save(){
    var apiUrl = baseUrl + '/StackV-web/restapi/app/profile/new';
    var innerData = {
        userID: sessionStorage.getItem("username"),
        type: "dnc",
        alias: $('#service-name').val(),
        data: generateJSON()
    };
    var sentData = {
        name: $('#service-name').val(),
        description: $("#new-profile-description").val(),
        data: JSON.stringify(innerData)
    };
    
    $.ajax({
        url: apiUrl,
        type: 'PUT',
        data: JSON.stringify(sentData),
        contentType: "application/json; charset=utf-8",
        dataType: "json",
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
            xhr.setRequestHeader("Refresh", keycloak.refreshToken);
        },
        success: function (result) {
        }
    });
}

function generateJSON(){
    connections = [];
    
    for (var i = 1; i <= 1; i++){
        var terminals = [];
        var src = document.getElementById("linksrc" + i).value;
        var src_vlan = document.getElementById("linksrc-vlan" + i).value;
        var des = document.getElementById("linkdes" + i).value;
        var des_vlan = document.getElementById("linkdes-vlan" + i).value;
        
        var source = {
            uri: src,
            vlan_tag: src_vlan
        };
        var destination = {
            uri: des,
            vlan_tag: des_vlan
        };
        
        terminals[0] = source;
        terminals[1] = destination;
        var data = {
            name: "link " + document.getElementById("linkUri" + i).value,
            terminals: terminals
        };
        connections[i-1] = data;
    }
    DNCdata = {
        connections: connections
    };
    
    return DNCdata;
}

function submitToBackend(){
    var apiUrl = baseUrl + '/StackV-web/restapi/app/service';
    
    var sentData = {
        username: sessionStorage.getItem("username"),
        type: "dnc",
        alias: $('#service-name').val(),
        data: generateJSON()
    };
    
    $.ajax({
        url: apiUrl,
        type: 'POST',
        data: JSON.stringify(sentData),
        contentType: "application/json; charset=utf-8",
        dataType: "json",
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
            xhr.setRequestHeader("Refresh", keycloak.refreshToken);
        },
        success: function (result) {
            $('#test').empty();
            document.getElementById("test").innerHTML = "SUCCESS";
        },
        error: function (){
            $('#test').empty();
            document.getElementById("test").innerHTML = "failure";
        } 
    });
}

function test(){
    var apiUrl = baseUrl + '/StackV-web/restapi/driver';
    var sentData = "<driverInstance><properties>" 
            + "<entry><key>topologyUri</key><value>urn:ogf:network:sdn.maxgigapop.net:network</value></entry>"
            + "<entry><key>driverEjbPath</key><value>java:module/GenericRESTDriver</value></entry>"
            + "<entry><key>subsystemBaseUrl</key><value></value>http://206.196.179.139:8080/VersaNS-0.0.1-SNAPSHOT</entry>"
            + "</properties></driverInstance>";
    
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
        },
        error: function (){
            document.getElementById("ret_field").innerHTML = "failure";
        } 
    });

}

