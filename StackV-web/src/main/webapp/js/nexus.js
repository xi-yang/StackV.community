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
/* global XDomainRequest, baseUrl, loggedIn, TweenLite, Power2, tweenBlackScreen */
// Service JavaScript Library
baseUrl = window.location.origin;
var keycloak = Keycloak('/StackV-web/data/json/keycloak.json');
var refreshTimer;
var countdownTimer;
var dataTable;

// Page Load Function

$(function () {
    $.ajaxSetup({
        cache: false,
        timeout: 15000
    });

    keycloak.init().success(function (authenticated) {
        if (authenticated) {
            var test = keycloak.isTokenExpired();
            loggedIn = authenticated ? true : false;
            sessionStorage.setItem("loggedIn", loggedIn);
            if (loggedIn) {
                sessionStorage.setItem("username", keycloak.tokenParsed.preferred_username);
                sessionStorage.setItem("subject", keycloak.tokenParsed.sub);
                sessionStorage.setItem("token", keycloak.token);
            }
        } else {
            keycloak.login();
        }
    }).error(function () {
        alert('failed to initialize');
    });
    keycloak.onAuthSuccess = function () {
        loadNavbar();

        if (window.location.pathname === "/StackV-web/ops/catalog.jsp") {
            loadCatalogNavbar();
            loadCatalog();
        } else if (window.location.pathname === "/StackV-web/ops/details/templateDetails.jsp") {
            var uuid = sessionStorage.getItem("instance-uuid");
            if (!uuid) {
                alert("No Service Instance Selected!");
                window.location.replace('/StackV-web/ops/catalog.jsp');
            } else {
                loadDetailsNavbar();
                loadDetails();
            }
        } else if (window.location.pathname === "/StackV-web/ops/admin.jsp") {
            loadAdminNavbar();
            loadAdmin();
        } else if (window.location.pathname === "/StackV-web/ops/acl.jsp") {
            loadACLNavbar();
            loadACLPortal();
        } else if (window.location.pathname === "/StackV-web/ops/srvc/driver.jsp") {
            loadDriverNavbar();
            loadDriverPortal();
        } else if (window.location.pathname === "/StackV-web/ops/intent_test.html") {
            loadIntent(getURLParameter("intent"));            
        }

        if ($("#tag-panel").length) {
            initTagPanel();
        }
    };
    keycloak.onTokenExpire = function () {
        keycloak.updateToken(20).success(function () {
            
            console.log("Token automatically updated!");
        }).error(function () {
            console.log("Automatic token update failed!");
        });
    };

    $("#button-service-cancel").click(function (evt) {
        $("#service-specific").empty();
        $("#button-service-cancel").toggleClass("hide");
        $("#service-overview").toggleClass("hide");

        clearCounters();
    });

    $("#button-service-return").click(function (evt) {
        window.location.href = "/StackV-web/ops/catalog.jsp";

        evt.preventDefault();
    });

    $(".button-group-select").click(function (evt) {
        $ref = "user_groups.jsp?id=" + this.id;
        $ref = $ref.replace('select', '') + " #group-specific";
        // console.log($ref);
        $("#group-specific").load($ref);
        evt.preventDefault();
    });

    $(".clickable-row").click(function () {
        sessionStorage.setItem("uuid", $(this).data("href"));
        window.document.location = "/StackV-web/ops/details/templateDetails.jsp";
    });

    $(".checkbox-level").click(function (evt) {
        evt.preventDefault();
    });

    clearCounters();
});

function loadNavbar() {
    $("#nav").load("/StackV-web/nav/navbar.html", function () {
        if (keycloak.tokenParsed.realm_access.roles.indexOf("admin") <= -1) {
            $(".nav-admin").hide();
        } else {
            // set the active link - get everything after StackV-web
            var url = $(location).attr('href').split(/\/StackV-web\//)[1];
            if (/driver.jsp/.test(url))
                $("li#driver-tab").addClass("active");
            else if (/catalog.jsp/.test(url))
                $("li#catalog-tab").addClass("active");
            else if (/graphTest.jsp/.test(url))
                $("li#visualization-tab").addClass("active");
            else if (/acl.jsp/.test(url))
                $("li#acl-tab").addClass("active");
            else if (/templateDetails.jsp/.test(url))
                $("li#details-tab").addClass("active");
            else if (/admin.jsp/.test(url))
                $("li#admin-tab").addClass("active");

            var apiUrl = baseUrl + '/StackV-web/restapi/app/logging/';
            $.ajax({
                url: apiUrl,
                type: 'GET',
                dataType: "text",
                beforeSend: function (xhr) {
                    xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
                },
                success: function (result) {
                    $("#select-logging-level").val(result);
                },
                error: function (error, status, thrown) {
                    console.log(error);
                    console.log(status);
                    console.log(thrown);
                }
            });

            $("#logout-button").click(function (evt) {
                keycloak.logout();

                evt.preventDefault();
            });
            $("#account-button").click(function (evt) {
                keycloak.accountManagement();

                evt.preventDefault();
            });
        }
    });
}

function prettyPrintInfo() {
    var ugly = document.getElementById('info-panel-text-area').value;
    var obj = JSON.parse(ugly);
    var pretty = JSON.stringify(obj, undefined, 4);
    document.getElementById('info-panel-text-area').value = pretty;
}

//Select Function
function aclSelect(sel) {
    $ref = "privileges.jsp?id=" + sel.value + " #acl-tables";
    $("#acl-tables").load($ref);
}

function installSelect(sel) {
    if (sel.value !== null) {
        $ref = "/StackV-web/ops/srvc/driver.jsp?form_install=" + sel.value + " #service-menu";
        $ref2 = "/StackV-web/ops/srvc/driver.jsp?form_install=" + sel.value + " #service-fields";
    } else {
        $ref = "/StackV-web/ops/srvc/driver.jsp #service-menu";

        $ref2 = "/StackV-web/ops/srvc/driver.jsp #service-fields";


    }
    $("#service-top").load($ref);
    $("#service-bottom").load($ref2);
}

function viewmodeSelect(sel) {
    if (sel.value !== null) {
        $ref = "/StackV-web/ops/srvc/viewcreate.jsp?mode=" + sel.value + " #service-menu";
        $ref2 = "/StackV-web/ops/srvc/viewcreate.jsp?mode=" + sel.value + " #service-fields";
    } else {
        $ref = "/StackV-web/ops/srvc/viewcreate.jsp #service-menu";
        $ref2 = "/StackV-web/ops/srvc/viewcreate.jsp #service-fields";

    }
    $("#service-top").load($ref);
    $("#service-bottom").load($ref2);

    clearCounters();
}

function driverSelect(sel) {
    if (sel.value !== null) {
        $ref = "/StackV-web/ops/srvc/driver.jsp?form_install=install&driver_id=" + sel.value + " #service-fields";
    } else
        $ref = "/StackV-web/ops/srvc/driver.jsp?form_install=install #service-fields";
    $("#service-bottom").load($ref);


    fieldCounter = 0;
}

function topoSelect(sel) {
    if (sel.value !== null) {

        if (sel.value.indexOf("aws") > -1) {
            $ref = "/StackV-web/ops/srvc/vmadd.jsp?vm_type=aws&topo=" + sel.value + " #service-fields";
        } else if (sel.value.indexOf("openstack") > -1) {
            $ref = "/StackV-web/ops/srvc/vmadd.jsp?vm_type=os #service-fields";
        } else if (sel.value.indexOf("versa") > -1) {
            $ref = "/StackV-web/ops/srvc/vmadd.jsp?vm_type=vs #service-fields";
        } else {
            $ref = "/StackV-web/ops/srvc/vmadd.jsp #service-fields";
        }
    } else
        $ref = "/StackV-web/ops/srvc/vmadd.jsp #service-fields";

    $("#service-bottom").load($ref);

    clearCounters();
}



function instanceSelect(sel) {
    if (sel.value !== null) {
        if (sel.value === "instance1") {
            document.getElementsByName("root-path")[0].value = "/dev/xvda";
            document.getElementsByName("root-snapshot")[0].value = "snapshot";
        } else if (sel.value === "instance2") {
            document.getElementsByName("root-path")[0].value = "/dev/sdb";
            document.getElementsByName("root-snapshot")[0].value = "snapshot";
        } else if (sel.value === "instance3") {
            document.getElementsByName("root-path")[0].value = "/dev/sdc";
            document.getElementsByName("root-snapshot")[0].value = "snapshot";
        }
    }
}

function networkSelect(sel) {
    if (sel.value !== null) {
        $ref2 = "/StackV-web/ops/srvc/netcreate.jsp?networkType=" + sel.value + " #service-fields";
    } else {
        $ref2 = "/StackV-web/ops/srvc/netcreate.jsp #service-fields";
    }
    $("#service-bottom").load($ref2);

    clearCounters();
}

// Field Addition Functions
var fieldCounter = 0;
var fieldLimit = 5;
function addPropField() {
    if (fieldCounter === fieldLimit) {
        alert("You have reached the limit of additional properties");
    } else {
        var table = document.getElementById("service-form");
        var tableHeight = table.rows.length;

        var row = table.insertRow(tableHeight - 1);
        var cell1 = row.insertCell(0);
        var cell2 = row.insertCell(1);
        fieldCounter++;
        cell1.innerHTML = '<input type="text" name="apropname'
                + (fieldCounter) + '" placeholder="Additional Property Name" size="30" />';

        cell2.innerHTML = '<input type="text" name="apropval'
                + (fieldCounter) + '" placeholder="Additional Property Value" size="30" />';

    }

}

var volumeCounter = 0;
var volumeLimit = 10;
function addVolume() {
    if (volumeCounter === volumeLimit) {
        alert("You have reached the limit of volumes.");
    } else {
        var table = document.getElementById("volume-table");
        var tableHeight = table.rows.length;

        var row = table.insertRow(tableHeight);
        var cell1 = row.insertCell(0);
        var cell2 = row.insertCell(1);
        var cell3 = row.insertCell(2);
        var cell4 = row.insertCell(3);
        var cell5 = row.insertCell(4);
        volumeCounter++;
        cell1.innerHTML = 'Volume ' + volumeCounter;
        cell2.innerHTML = '<select name="' + volumeCounter + '-path" required>'
                + '<option></option>'
                + '<option value="/dev/xvda">/dev/xvda</option>'
                + '<option value="/dev/sdb">/dev/sdb</option>'
                + '<option value="/dev/sdc">/dev/sdc</option>'
                + '</select>';
        cell3.innerHTML = '<select name="' + volumeCounter + '-snapshot" required>'
                + '<option></option>'
                + '<option value="snapshot1">snapshot</option>'
                + '<option value="snapshot1">snapshot2</option>'
                + '<option value="snapshot1">snapshot3</option>'
                + '</select>';
        cell4.innerHTML = '<input type="number" name="' + volumeCounter + '-size" style="width: 4em; text-align: center;"/>';
        cell5.innerHTML = '<select name="' + volumeCounter + '-type" required>'
                + '<option></option>'
                + '<option value="standard">Standard</option>'
                + '<option value="io1">io1</option>'
                + '<option value="gp2">gp2</option>'
                + '</select>'
                + '<input type="button" class="button-register" value="Remove" onClick="removeVolume(' + tableHeight + ')" />';
    }
}

function removeVolume(row) {
    var table = document.getElementById("volume-table");
    table.deleteRow(row);
}

function openWizard(button) {
    var queryID = button.id.substr(7);

    document.getElementById("wizard-table").toggleClass("hide");
    document.getElementById("queryNumber").value = queryID;
}

function applyTextTemplate(name) {
    var template = document.getElementById(name + "Template");
    var input = document.getElementById(name + "Input");
    var queryNumber = document.getElementById("queryNumber");

    var output = document.getElementById("sparquery" + queryNumber.value);
    output.value = template.value + input.value;
}

function applySelTemplate(name) {
    var template = document.getElementById(name + "Template");
    var input = document.getElementById(name + "Input");
    var output = document.getElementById("sparquery");

    output.value = template.value + input.options[input.selectedIndex].value;
}

var queryCounter = 1;
var queryLimit = 10;
function addQuery() {
    if (queryCounter === queryLimit) {
        alert("You have reached the limit of querys.");
    } else {
        var table = document.getElementById("net-custom-form");
        var tableHeight = table.rows.length;

        var row = table.insertRow(tableHeight - 2);
        var cell1 = row.insertCell(0);
        var cell2 = row.insertCell(1);
        queryCounter++;
        cell1.innerHTML = '<input type="text" id="sparquery' + queryCounter + '" name="sparquery' + queryCounter + '" size="70" />';
        cell2.innerHTML = '<div class="view-flag">'
                + '<input type="checkbox" id="inc' + queryCounter + '" name="viewInclusive' + queryCounter + '"/><label for="inc' + queryCounter + '">Inclusive</label>'
                + '</div><div class="view-flag">'
                + '<input type="checkbox" id="sub' + queryCounter + '" name="subRecursive' + queryCounter + '"/><label for="sub' + queryCounter + '">Subtree Rec.</label>'
                + '</div><div class="view-flag">'
                + '<input type="checkbox" id="sup' + queryCounter + '" name="supRecursive' + queryCounter + '"/><label for="sup' + queryCounter + '">Supertree Rec.</label></div>';
    }
}

var routeCounter = 1;
var routeLimit = 10;
function addRoute() {
    if (routeCounter === routeLimit) {
        alert("You have reached the limit of routes.");
    } else {
        routeCounter++;
        var block = document.getElementById('route-block');

        block.innerHTML = block.innerHTML +
                '<div>' +
                '<input type="text" name="route' + routeCounter + '-from" placeholder="From"/>' +
                '<input type="text" name="route' + routeCounter + '-to" placeholder="To"/>' +
                '<input type="text" name="route' + routeCounter + '-next" placeholder="Next Hop"/>' +
                '</div>';
    }
}

var subRouteCounter = 1;
var subRouteLimit = 10;
function addSubnetRoute(subnetID) {
    if (subRouteCounter === subRouteLimit) {
        alert("You have reached the limit of routes.");
        return;
    }

    subRouteCounter++;
    var block = document.getElementById(subnetID + '-block');

    block.innerHTML = block.innerHTML +
            '<div>' +
            '<input type="text" name="' + subnetID + subRouteCounter + '-from" placeholder="From"/>' +
            '<input type="text" name="' + subnetID + subRouteCounter + '-to" placeholder="To"/>' +
            '<input type="text" name="' + subnetID + subRouteCounter + '-next" placeholder="Next Hop"/>' +
            '</div>';
}

//var VMRouteCounter = 1;
//var VMRouteLimit = 10;
//function addVMRoute(VMID) {
//    if (VMRouteCounter === VMRouteLimit) {
//        alert("You have reached the limit of routes.");
//    }
//
//    VMRouteCounter++;
//    var block = document.getElementById(VMID + '-block');
//
//    block.innerHTML = block.innerHTML +
//            '<div>' +
//            '<input type="text" name="' + VMID + VMRouteCounter + '-from" placeholder="From"/>' +
//            '<input type="text" name="' + VMID + VMRouteCounter + '-to" placeholder="To"/>' +
//            '<input type="text" name="' + VMID + VMRouteCounter + '-next" placeholder="Next Hop"/>' +
//            '</div>';
//}

var SRIOVCounter = 1;
var SRIOVLimit = 10;
function addSRIOV(VMID) {
    if (SRIOVCounter === SRIOVLimit) {
        alert("You have reached the limit of SRIOV connections.");
        return;
    }

    SRIOVCounter++;
    var block = document.getElementById(VMID + '-block');

    block.innerHTML = block.innerHTML +
            '<div>' +
            '<input type="text" name="' + VMID + SRIOVCounter + '-mac" placeholder="SRIOV MAC Address">' +
            '<input type="text" name="' + VMID + SRIOVCounter + '-ip" placeholder="SRIOV IP Address">' +
            '<input type="text" name="' + VMID + SRIOVCounter + '-gateway" placeholder="SRIOV Gateway">' +
            '</div><div id="' + VMID + SRIOVCounter + '-route-block"></div><div>' +
            '<input class="button-register" id="' + VMID + SRIOVCounter + '-route" type="button" value="Add Route" onClick="addSRIOVRoute(this.id)">';

    addSRIOVRoute(VMID + SRIOVCounter + '-route');
}

var SRIOVRouteCounter = 1;
var SRIOVRouteLimit = 20;
function addSRIOVRoute(SRIOVRouteId) {
    if (SRIOVRouteCounter === SRIOVRouteLimit) {
        alert("You have reached the limit of SRIOV routes.");
        return;
    }

    SRIOVRouteCounter++;
    var block = document.getElementById(SRIOVRouteId + '-block');

    block.innerHTML = block.innerHTML +
            '<div>' +
            '<input type="text" name="' + SRIOVRouteId + SRIOVRouteCounter + '-from" placeholder="From">' +
            '<input type="text" name="' + SRIOVRouteId + SRIOVRouteCounter + '-to" placeholder="To">' +
            '<input type="text" name="' + SRIOVRouteId + SRIOVRouteCounter + '-next" placeholder="Next Hop">' +
            '</div>';
}

var VMCounter = 1;
var VMLimit = 10;
function addVM(type, subnetID) {
    if (VMCounter === VMLimit) {
        alert("You have reached the limit of VMs.");
        return;
    } else if (type === 'aws') {
        VMCounter++;
        var block = document.getElementById(subnetID + '-block');

        block.innerHTML = block.innerHTML +
                '<table id="' + subnetID + VMCounter + '-table">' +
                '<tbody>' +
                '<tr><td>VM Name</td><td><input type="text" name="' + subnetID + VMCounter + '"></td></tr>' +
                '<tr><td><input type="text" name="' + subnetID + VMCounter + '-keypair" placeholder="Keypair Name"></td>' +
                '<td><input type="text" name="' + subnetID + VMCounter + '-security" placeholder="Security Group"></td></tr>' +
                '<tr><td><input type="text" name="' + subnetID + VMCounter + '-image" placeholder="Image Type"></td>' +
                '<td><input type="text" name="' + subnetID + VMCounter + '-instance" placeholder="Instance Type"></td></tr>' +
                '</tbody></table>';
    } else if (type === 'ops') {
        VMCounter++;
        var block = document.getElementById(subnetID + '-block');

        block.innerHTML = block.innerHTML +
                '<table id="' + subnetID + VMCounter + '-table">' +
                '<tbody>' +
                '<tr><td>VM Name</td><td><input type="text" name="' + subnetID + VMCounter + '"></td></tr>' +
                '<tr><td><input type="text" name="' + subnetID + VMCounter + '-keypair" placeholder="Keypair Name"></td>' +
                '<td><input type="text" name="' + subnetID + VMCounter + '-security" placeholder="Security Group"></td></tr>' +
                '<tr><td><input type="text" name="' + subnetID + VMCounter + '-image" placeholder="Image Type"></td>' +
                '<td><input type="text" name="' + subnetID + VMCounter + '-instance" placeholder="Instance Type"></td></tr>' +
                '<tr><td><input type="text" name="' + subnetID + VMCounter + '-host" placeholder="VM Host"></td>' +
                '<td><input type="text" name="' + subnetID + VMCounter + '-floating" placeholder="Floating IP"></td></tr>' +
                '<tr><td>SRIOV</td><td>' +
                '<div id="' + subnetID + VMCounter + '-sriov-block">' +
                '</div><div><input class="button-register" id="' + subnetID + VMCounter + '-sriov" type="button" value="Add SRIOV" onClick="addSRIOV(this.id)"></div>' +
                '</td></tr></tbody></table>';

//        addVMRoute(subnetID + VMCounter + '-route');
        addSRIOV(subnetID + VMCounter + '-sriov');
    }
}

var gatewayCounter = 1;
var gatewarLimit = 5;
function addGateway(gatewayID) {
    if (gatewayCounter === gatewarLimit) {
        alert("You have reached the limit of Gateways.");
        return;
    }
    gatewayCounter++;
    var block = document.getElementById(gatewayID + '-block');

    block.innerHTML = block.innerHTML +
            '<table id="' + gatewayID + gatewayCounter + '-table">' +
            '<tbody>' +
            '<tr><td>Name</td>' +
            '<td><input type="text" name="' + gatewayID + gatewayCounter + '"></td></tr>' +
            '<tr><td>From</td>' +
            '<td><input type="text" name="' + gatewayID + gatewayCounter + '-from-value" placeholder="Value"></td>' +
            '<td><input type="text" name="' + gatewayID + gatewayCounter + '-from-type" placeholder="Type"></td></tr>' +
            '<tr><td>To</td>' +
            '<td><input type="text" name="' + gatewayID + gatewayCounter + '-to-value" placeholder="Value"></td>' +
            '<td><input type="text" name="' + gatewayID + gatewayCounter + '-to-type" placeholder="Type"></td></tr>' +
            '</tbody></table>';
}

var subnetCounter = 1;
var subnetLimit = 10;
function addSubnet(type) {
    if (subnetCounter === subnetLimit) {
        alert("You have reached the limit of subnets.");
        return;
    } else if (type === 'aws') {
        var table = document.getElementById("net-custom-form");
        var tableHeight = table.rows.length;
        subnetCounter++;

        var row = table.insertRow(tableHeight - 2);
        row.id = 'subnet' + subnetCounter;

        var cell1 = row.insertCell(0);
        cell1.innerHTML = 'Subnet ' + subnetCounter;
        var cell2 = row.insertCell(1);
        cell2.innerHTML = '<div>' +
                '<input type="text" name="subnet' + subnetCounter + '-name" placeholder="Name"/>' +
                '<input type="text" name="subnet' + subnetCounter + '-cidr" placeholder="CIDR Block"/>' +
                '<div id="subnet' + subnetCounter + '-route-block"></div>' +
                '<div>' +
                '<input type="checkbox" name="subnet' + subnetCounter + '-route-prop" value="true"/>   Enable VPN Routes Propogation' +
                '</div>' +
                '<div>' +
                '<input class="button-register" id="subnet' + subnetCounter + '-route" type="button" value="Add Route" onClick="addSubnetRoute(this.id)">' +
                '</div><br>' +
                '<div id="subnet' + subnetCounter + '-vm-block"></div>' +
                '<div>' +
                '<input class="button-register" id="subnet' + subnetCounter + '-vm" type="button" value="Add VM" onClick="addVM(\'aws\', this.id)">' +
                '</div>' +
                '</div>';

        addSubnetRoute('subnet' + subnetCounter + '-route');
        addVM('aws', 'subnet' + subnetCounter + '-vm');
    } else if (type === 'ops') {
        var table = document.getElementById("net-custom-form");
        var tableHeight = table.rows.length;
        subnetCounter++;

        var row = table.insertRow(tableHeight - 1);
        row.id = 'subnet' + subnetCounter;

        var cell1 = row.insertCell(0);
        cell1.innerHTML = 'Subnet ' + subnetCounter;
        var cell2 = row.insertCell(1);
        cell2.innerHTML = '<div>' +
                '<input type="text" name="subnet' + subnetCounter + '-name" placeholder="Name"/>' +
                '<input type="text" name="subnet' + subnetCounter + '-cidr" placeholder="CIDR Block"/>' +
                '<div id="subnet' + subnetCounter + '-route-block"></div>' +
                '<div>' +
                '<input type="checkbox" name="subnet' + subnetCounter + '-route-prop" value="true"/>   Enable VPN Routes Propogation' +
                '</div>' +
                '<div>' +
                '<input class="button-register" id="subnet' + subnetCounter + '-route" type="button" value="Add Route" onClick="addSubnetRoute(this.id)">' +
                '</div><br>' +
                '<div id="subnet' + subnetCounter + '-vm-block"></div>' +
                '<div>' +
                '<input class="button-register" id="subnet' + subnetCounter + '-vm" type="button" value="Add VM" onClick="addVM(\'ops\', this.id)"></div>' +
                '</div>';

        addSubnetRoute('subnet' + subnetCounter + '-route');
        addVM('ops', 'subnet' + subnetCounter + '-vm');
    }
}

var linkCounter = 1;
var linkLimit = 10;
function addLink() {
    if (linkCounter === linkLimit) {
        alert("You have reached the limit of connections");
    } else {
        var table = document.getElementById("net-custom-form");
        var tableHeight = table.rows.length;
        linkCounter++;

        var row = table.insertRow(tableHeight - 1);
        row.id = 'link' + linkCounter;

        var cell1 = row.insertCell(0);
        cell1.innerHTML = 'Link ' + linkCounter;
        var cell2 = row.insertCell(1);
        cell2.innerHTML = '<div>' +
                '<input type="text" name="linkUri' + linkCounter + '"size="60" placeholder="Link-URI">' +
                '</div>' +
                '<div>' +
                '<input type="text" name="link' + linkCounter + '-src" size="60" placeholder="Source">' +
                '<input type="text" name="link' + linkCounter + '-src-vlan" placeholder="Vlan-tag">' +
                '</div>' +
                '</div>' +
                '<input type="text" name="link' + linkCounter + '-des" size="60" placeholder="Destination">' +
                '<input type="text" name="link' + linkCounter + '-des-vlan" placeholder="Vlan-tag">' +
                '</div>';
    }
}


/* API CALLS */

function checkInstance(uuid) {
    var apiUrl = baseUrl + '/StackV-web/restapi/service/' + uuid + '/status';
    $.ajax({
        url: apiUrl,
        type: 'GET',
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
            xhr.setRequestHeader("Refresh", keycloak.refreshToken);
        },
        success: function (result) {
            var statusElement = document.getElementById("instance-status");
            statusElement.innerHTML = result;
        }
    });
}

function cancelInstance(uuid) {
    var apiUrl = baseUrl + '/StackV-web/restapi/app/service/' + uuid + '/cancel';
    $.ajax({
        url: apiUrl,
        type: 'PUT',
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
        },
        success: function (result) {
            window.location.reload(true);
        }
    });
    //window.location.replace('/StackV-web/ops/catalog.jsp');
}
function forceCancelInstance(uuid) {
    var apiUrl = baseUrl + '/StackV-web/restapi/app/service/' + uuid + '/force_cancel';
    $.ajax({
        url: apiUrl,
        type: 'PUT',
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
        },
        success: function (result) {
            window.location.reload(true);
        }
    });
    //window.location.replace('/StackV-web/ops/catalog.jsp');
}

function reinstateInstance(uuid) {
    var apiUrl = baseUrl + '/StackV-web/restapi/app/service/' + uuid + '/reinstate';
    $.ajax({
        url: apiUrl,
        type: 'PUT',
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
        },
        success: function (result) {
            window.location.reload(true);
        }
    });
    //window.location.replace('/StackV-web/ops/catalog.jsp');
}
function forceReinstateInstance(uuid) {
    var apiUrl = baseUrl + '/StackV-web/restapi/app/service/' + uuid + '/force_reinstate';
    $.ajax({
        url: apiUrl,
        type: 'PUT',
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
        },
        success: function (result) {
            window.location.reload(true);
        }
    });
    //window.location.replace('/StackV-web/ops/catalog.jsp');
}

function forceRetryInstance(uuid) {
    var apiUrl = baseUrl + '/StackV-web/restapi/app/service/' + uuid + '/force_retry';
    $.ajax({
        url: apiUrl,
        type: 'PUT',
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
        },
        success: function (result) {
            window.location.reload(true);
        }
    });
    //window.location.replace('/StackV-web/ops/catalog.jsp');
}

function modifyInstance(uuid) {
    var apiUrl = baseUrl + '/StackV-web/restapi/app/service/' + uuid + '/modify';
    $.ajax({
        url: apiUrl,
        type: 'PUT',
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
        },
        success: function (result) {
            window.location.reload(true);
        }
    });
    //window.location.replace('/StackV-web/ops/catalog.jsp');
}

function forceModifyInstance(uuid) {
    var apiUrl = baseUrl + '/StackV-web/restapi/app/service/' + uuid + '/force_modify';
    $.ajax({
        url: apiUrl,
        type: 'PUT',
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
        },
        success: function (result) {
            window.location.reload(true);
        }
    });
    //window.location.replace('/StackV-web/ops/catalog.jsp');
}

function verifyInstance(uuid) {
    var apiUrl = baseUrl + '/StackV-web/restapi/app/service/' + uuid + '/verify';
    $.ajax({
        url: apiUrl,
        type: 'PUT',
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
            xhr.setRequestHeader("Refresh", keycloak.refreshToken);
        },
        success: function (result) {
            window.location.reload(true);
        }
    });
    //window.location.replace('/StackV-web/ops/catalog.jsp');
}

function deleteInstance(uuid) {
    var apiUrl = baseUrl + '/StackV-web/restapi/app/service/' + uuid + '/delete';
    $.ajax({
        url: apiUrl,
        type: 'PUT',
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
        },
        success: function (result) {
            console.log("DELETION SUCCESSFUL");
            window.location.replace('/StackV-web/ops/catalog.jsp');
        }
    });
}

// TEMPLATING

function resetForm() {
    var form = document.getElementById('custom-form');
    form.reset();
}

function applyDNCTemplate(code) {
    var form = document.getElementById('custom-form');
    form.reset();

    switch (code) {
        case 1:
            //form.elements['topoUri'].value = 'urn:ogf:network:vo1.maxgigapop.net:link';
            form.elements['linkUri1'].value = 'urn:ogf:network:vo1.maxgigapop.net:link=conn1';
            form.elements['link1-src'].value = 'urn:ogf:network:domain=dragon.maxgigapop.net:node=CLPK:port=1-2-3:link=*';
            form.elements['link1-src-vlan'].value = '3021-3029';
            form.elements['link1-des'].value = 'urn:ogf:network:domain=dragon.maxgigapop.net:node=CLPK:port=1-1-2:link=*';
            form.elements['link1-des-vlan'].value = '3021-3029';

            break;
        case 2:
            if (linkCounter === 1) {
                addLink();
            }

            form.elements['linkUri1'].value = 'urn:ogf:network:vo1.maxgigapop.net:link=conn1';
            form.elements['link1-src'].value = 'urn:ogf:network:domain=dragon.maxgigapop.net:node=CLPK:port=1-2-3:link=*';
            form.elements['link1-src-vlan'].value = '3021-3029';
            form.elements['link1-des'].value = 'urn:ogf:network:domain=dragon.maxgigapop.net:node=CLPK:port=1-1-2:link=*';
            form.elements['link1-des-vlan'].value = '3021-3029';
            form.elements['linkUri2'].value = 'urn:ogf:network:vo1.maxgigapop.net:link=conn2';
            form.elements['link2-src'].value = 'urn:ogf:network:domain=dragon.maxgigapop.net:node=CLPK:port=1-2-3:link=*';
            form.elements['link2-src-vlan'].value = '3021-3029';
            form.elements['link2-des'].value = 'urn:ogf:network:domain=dragon.maxgigapop.net:node=CLPK:port=1-1-2:link=*';
            form.elements['link2-des-vlan'].value = '3021-3029';

            break;
        default:

    }
}

function dncModerate() {
    var superstate = document.getElementById("instance-superstate").innerHTML;
    var substate = document.getElementById("instance-substate").innerHTML;

    if (superstate === 'Create') {
        switch (substate) {
            case 'READY':
                $("#instance-cancel").toggleClass("hide");
                break;

            case 'INIT':
                $("#instance-delete").toggleClass("hide");
                break;

            case 'FAILED':
                $("#instance-delete").toggleClass("hide");
                break;
        }
    }
    if (superstate === 'Cancel') {
        switch (substate) {
            case 'READY':
                $("#instance-delete").toggleClass("hide");
                break;

            case 'FAILED':
                $("#instance-delete").toggleClass("hide");
                break;
        }
    }
}


function fl2pModerate(uuid) {
    var superstate = document.getElementById("instance-superstate").innerHTML;
    var substate = document.getElementById("instance-substate").innerHTML;

    if (superstate === 'Create') {
        switch (substate) {
            case 'READY':
                $("#instance-cancel").toggleClass("hide");
                break;

            case 'INIT':
                $("#instance-delete").toggleClass("hide");
                break;

            case 'FAILED':
                $("#instance-delete").toggleClass("hide");
                break;
        }
    }
    if (superstate === 'Cancel') {
        switch (substate) {
            case 'READY':
                $("#instance-delete").toggleClass("hide");
                break;

            case 'FAILED':
                $("#instance-delete").toggleClass("hide");
                break;
        }
    }
    if (superstate === 'Reinstate') {
        switch (substate) {
            case 'READY':
                $("#instance-reinstate").toggleClass("hide");
                break;
            case 'FAILED':
                $("#instance-delete").toggleClass("hide");
                break;

        }
    }
}

/* UTILITY */

function getURLParameter(name) {
    return decodeURIComponent((new RegExp('[?|&]' + name + '=' + '([^&;]+?)(&|#|;|$)').exec(location.search) || [null, ''])[1].replace(/\+/g, '%20')) || null;
}

// Helper method to parse the title tag from the response.
function getTitle(text) {
    return text.match('<title>(.*)?</title>')[1];
}

function clearCounters() {
    volumeCounter = 0;
    fieldCounter = 0;
    queryCounter = 1;
    routeCounter = 1;
    subnetCounter = 1;
    SRIOVCounter = 1;
    SRIOVRouteCounter = 1;
    VMCounter = 1;
    gatewayCounter = 1;
    subRouteCounter = 1;
    linkCounter = 1;
}

function reloadPage() {
    window.location.reload(true);
}

function reloadPanel(panelId) {
    $('#' + panelId).load(document.URL + ' #' + panelId);
}

function emptyElement(id) {
    $("#" + id).empty();
}

// Create the XHR object.
function createCORSRequest(method, url) {
    var xhr = new XMLHttpRequest();
    if ("withCredentials" in xhr) {
        // XHR for Chrome/Firefox/Opera/Safari.
        xhr.open(method, url, true);
    } else if (typeof XDomainRequest !== "undefined") {
        // XDomainRequest for IE.
        xhr = new XDomainRequest();
        xhr.open(method, url);
    } else {
        // CORS not supported.
        xhr = null;
    }
    return xhr;
}

function enableLoading() {
    $("#main-pane").addClass("loading");
}

function disableLoading() {
    $("#main-pane").removeClass("loading");
}

function initTagPanel() {
    $.getScript("/StackV-web/js/stackv/label.js", function (data, textStatus, jqxhr) {
        if ($.trim($("#tag-panel").html()) !== '') {
            loadLabels();
        } else {
            $("#tag-panel").load("/StackV-web/tagPanel.jsp", function () {
                loadLabels();
            });
        }
    });
}

/* REFRESH */
function refreshSync(refreshed, time) {
    if (refreshed) {
        sessionStorage.setItem("token", keycloak.token);
        console.log("Token Refreshed by nexus!");
    }
    var manual = false;
    if (typeof time === "undefined") {
        time = countdown;
    }
    if (document.getElementById('refresh-button').innerHTML === 'Manually Refresh Now') {
        manual = true;
    }
    if (manual === false) {
        countdown = time;
        $("#refresh-button").html('Refresh in ' + countdown + ' seconds');
    } else {
        $("#refresh-button").html('Manually Refresh Now');
    }
}
function pauseRefresh() {
    clearInterval(refreshTimer);
    clearInterval(countdownTimer);
    document.getElementById('refresh-button').innerHTML = 'Paused';
    $("#refresh-timer").attr('disabled', true);
    $("#refresh-button").attr('disabled', true);
}
function resumeRefresh() {
    var timer = $("#refresh-timer");
    if (timer.attr('disabled')) {
        $("#refresh-button").attr('disabled', false);
        timer.attr('disabled', false);
        if (timer.val() === "off") {
            $("#refresh-button").html('Manually Refresh Now');
        } else {
            setRefresh(timer.val());
        }
    }
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
        reloadData();
    }, (time * 1000));
    countdownTimer = setInterval(function () {
        refreshCountdown(time);
    }, 1000);
}
function refreshCountdown() {
    document.getElementById('refresh-button').innerHTML = 'Refresh in ' + (countdown - 1) + ' seconds';
    countdown--;
}
function reloadDataManual() {
    var sel = document.getElementById("refresh-timer");
    if (sel.value !== 'off') {
        timerChange(sel);
    }
    reloadData();
}


/* LOGGING */
var openLogDetails = 0;
function loadDataTable(apiUrl) {
    dataTable = $('#loggingData').DataTable({
        "ajax": {
            url: apiUrl,
            type: 'GET',
            beforeSend: function (xhr) {
                xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
            }
        },
        "dom": 'Bfrtip',
        "buttons": [ 'csv' ],
        "columns": [
            {
                "className": 'details-control',
                "orderable": false,
                "data": null,
                "defaultContent": '',
                "width": "20px"
            },
            {"data": "timestamp", "width": "150px"},
            {"data": "event"},
            {"data": "referenceUUID", "width": "250px"},
            {"data": "level", "width": "70px"},
            {"data": "message", "visible": false, "searchable": false}
        ],
        "createdRow": function (row, data, dataIndex) {
            $(row).addClass("row-" + data.level.toLowerCase());
        },
        "deferRender": true,
        "order": [[1, 'asc']],
        "ordering": false,
        "scroller": {
            displayBuffer: 10
        },
        "scrollX": true,
        "scrollY": "calc(60vh - 130px)",
        "initComplete": function (settings, json) {
            console.log('DataTables has finished its initialisation.');
        }
    });
    new $.fn.dataTable.FixedColumns(dataTable);

    // Add event listener for opening and closing details
    $('#loggingData tbody').on('click', 'td.details-control', function () {
        var tr = $(this).closest('tr');
        var row = dataTable.row(tr);
        if (row.child.isShown()) {
            openLogDetails--;
            // This row is already open - close it
            row.child.hide();
            tr.removeClass('shown');
            if (openLogDetails === 0) {
                resumeRefresh();
            }
        } else {
            openLogDetails++;

            // Open this row
            row.child(formatChild(row.data())).show();
            tr.addClass('shown');
            pauseRefresh();
        }
    });

    var level = sessionStorage.getItem("logging-level");
    if (level !== null) {
        $("#logging-filter-level").val(level);
    }
}
function formatChild(d) {
    // `d` is the original data object for the row
    var retString = '<table cellpadding="5" cellspacing="0" border="0">';
    if (d.message !== "{}") {
        retString += '<tr>' +
                '<td style="width:10%">Message:</td>' +
                '<td style="white-space: normal">' + d.message + '</td>' +
                '</tr>';
    }
    if (d.exception !== "") {
        retString += '<tr>' +
                '<td>Exception:</td>' +
                '<td><textarea class="dataTables-child">' + d.exception + '</textarea></td>' +
                '</tr>';
    }
    if (d.referenceUUID !== "") {
        retString += '<tr>' +
                '<td>UUID:</td>' +
                '<td><textarea class="dataTables-child">' + d.referenceUUID + '</textarea></td>' +
                '</tr>';
    }
    retString += '<tr>' +
            '<td>Logger:</td>' +
            '<td>' + d.logger + '</td>' +
            '</tr>' +
            '</table>';

    return retString;
}
function reloadLogs() {
    if (dataTable) {
        if (sessionStorage.getItem("logging-level") !== null) {
            dataTable.ajax.reload(filterLogs(), false);
        } else {
            dataTable.ajax.reload(null, false);
        }
    }
}
function filterLogs() {
    var level = $("#logging-filter-level").val();
    if (level !== undefined) {
        sessionStorage.setItem("logging-level", level);

        var curr = dataTable.ajax.url();
        var paramArr = curr.split(/[?&]+/);
        var newURL = paramArr[0];

        var refEle;
        for (var x in paramArr) {
            if (paramArr[x].indexOf("refUUID") !== -1) {
                refEle = paramArr[x];
            }
        }

        newURL += "?level=" + level;
        if (refEle) {
            newURL += "&" + refEle;
        }

        dataTable.ajax.url(newURL).load(null, false);
    }
}
function downloadLogs() {
    var ret = [];
    if (dataTable) {
        var data = dataTable.rows().data();
        for (var i in data) {
            var log = data[i];

            ret.push(JSON.stringify(log));
        }
    }
    return ret;
}
