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
var linknum = 1;

function addLink(){
    var panel = document.getElementById("link-body");
    linknum++;
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
    
    input[0].placeholder="Link-URI";
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
        //userID might be keycloak.subject
        userID: sessionStorage.getItem("username"),
        type: "dnc",
        alias: $('#service-name').val(),
        data: generateJSON()
    };
    var sentData = {
        //name might be wrong
        name: $('#service-name').val(),
        description: $("#new-profile-description").val(),
        data: JSON.stringify(innerData)
    };
    
    $.ajax({
        url: apiUrl,
        type: 'PUT',
        data: JSON.stringify(sentData),  //stringify to get escaped JSON in backend
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
    
    for (var i = 1; i <= linknum; i++){
        var terminals = [];
        var src = "linksrc" + i;
        var src_vlan = "linksrc-vlan" + i;
        var des ="linkdes" + i;
        var des_vlan ="linkdes-vlan" + i;
        var source = {
            uri: document.getElementById(src).value,
            vlan_tag: document.getElementById(src_vlan).value
        };
        
        var destination = {
            uri: document.getElementById(des).value,
            vlan_tag: document.getElementById(des_vlan).value
        };
        
        terminals[0] = source;
        terminals[1] = destination;
        
        var data = {
            name: $('#service-name').val() + "-path-" + linknum,
            terminals: terminals
        };
        
        connections[i-1] = data;
    }
    
    DNCdata = {
        connections: connections
    };
    return JSON.stringify(DNCdata);
}

function submit(){
    
}