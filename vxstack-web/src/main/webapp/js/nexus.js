
/* global XDomainRequest */

// Service JavaScript Library
var baseUrl = window.location.origin;

// Page Load Function

$(function () {
    $("#nav").load("/VersaStack-web/navbar.html");
    $("#sidebar").load("/VersaStack-web/sidebar.html", function () {
        $("#sidebar-toggle").click(function (evt) {
            $("#sidebar-toggle-1").toggleClass("img-off");
            $("#sidebar-toggle-2").toggleClass("img-off");

            $("#sidebar-contents").toggleClass("sidebar-open");
            $("#main-pane").toggleClass("sidebar-open");

            evt.preventDefault();
        });
    });

    $(".button-service-select").click(function (evt) {
        $ref = "/VersaStack-web/ops/srvc/" + this.id.toLowerCase() + ".jsp";
        window.location.href = $ref;

        //$("#service-overview").toggleClass("hide");
        //$("#button-service-cancel").toggleClass("hide");
        //$("#service-specific").load($ref);
        evt.preventDefault();
    });

    $(".button-profile-select").click(function (evt) {
        var apiUrl = baseUrl + '/VersaStack-web/restapi/app/profile/' + this.id;
        $.ajax({
            url: apiUrl,
            type: 'GET',
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

    $(".button-profile-submit").click(function (evt) {
        var apiUrl = baseUrl + '/VersaStack-web/restapi/app/service';
        $.ajax({
            url: apiUrl,
            type: 'POST',
            data: $("#info-panel-text-area").val(),
            contentType: "application/json; charset=utf-8",
            dataType: "json",
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

    $("#button-service-cancel").click(function (evt) {
        $("#service-specific").empty();
        $("#button-service-cancel").toggleClass("hide");
        $("#service-overview").toggleClass("hide");

        clearCounters();
    });

    $("#button-service-return").click(function (evt) {
        window.location.href = "/VersaStack-web/ops/catalog.jsp";

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
        window.document.location = $(this).data("href");
    });

    $(".delta-table-header").click(function () {
        $("#body-" + this.id).toggleClass("hide");
    });

    $("#black-screen").click(function () {
        $("#black-screen").addClass("off");
        $("#info-panel").removeClass("active");
    });

    $(".nav-tabs li").click(function () {
        if ($(this).parent().parent().hasClass("closed")) {
            $("#catalog-panel").removeClass("closed");
        }
        else if (this.className === 'active') {
            $("#catalog-panel").toggleClass("closed");
        }
    });

    clearCounters();
});

function detailsLoad() {
    var uuid = getUrlParameter('uuid');
    $ref = "/VersaStack-web/ops/details/dncDetails.jsp?uuid=" + uuid + " #instance-pane";

    $("service-specific").load($ref);
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
        $ref = "/VersaStack-web/ops/srvc/driver.jsp?form_install=" + sel.value + " #service-menu";
        $ref2 = "/VersaStack-web/ops/srvc/driver.jsp?form_install=" + sel.value + " #service-fields";
    }
    else {
        $ref = "/VersaStack-web/ops/srvc/driver.jsp #service-menu";

        $ref2 = "/VersaStack-web/ops/srvc/driver.jsp #service-fields";


    }
    $("#service-top").load($ref);
    $("#service-bottom").load($ref2);
}

function viewmodeSelect(sel) {
    if (sel.value !== null) {
        $ref = "/VersaStack-web/ops/srvc/viewcreate.jsp?mode=" + sel.value + " #service-menu";
        $ref2 = "/VersaStack-web/ops/srvc/viewcreate.jsp?mode=" + sel.value + " #service-fields";
    }
    else {
        $ref = "/VersaStack-web/ops/srvc/viewcreate.jsp #service-menu";
        $ref2 = "/VersaStack-web/ops/srvc/viewcreate.jsp #service-fields";

    }
    $("#service-top").load($ref);
    $("#service-bottom").load($ref2);

    clearCounters();
}

function driverSelect(sel) {
    if (sel.value !== null) {
        $ref = "/VersaStack-web/ops/srvc/driver.jsp?form_install=install&driver_id=" + sel.value + " #service-fields";
    }


    else
        $ref = "/VersaStack-web/ops/srvc/driver.jsp?form_install=install #service-fields";
    $("#service-bottom").load($ref);


    fieldCounter = 0;
}

function topoSelect(sel) {
    if (sel.value !== null) {

        if (sel.value.indexOf("aws") > -1) {
            $ref = "/VersaStack-web/ops/srvc/vmadd.jsp?vm_type=aws&topo=" + sel.value + " #service-fields";
        }
        else if (sel.value.indexOf("openstack") > -1) {
            $ref = "/VersaStack-web/ops/srvc/vmadd.jsp?vm_type=os #service-fields";
        }
        else if (sel.value.indexOf("versa") > -1) {
            $ref = "/VersaStack-web/ops/srvc/vmadd.jsp?vm_type=vs #service-fields";
        }
        else {
            $ref = "/VersaStack-web/ops/srvc/vmadd.jsp #service-fields";
        }
    }
    else
        $ref = "/VersaStack-web/ops/srvc/vmadd.jsp #service-fields";

    $("#service-bottom").load($ref);

    clearCounters();
}



function instanceSelect(sel) {
    if (sel.value !== null) {
        if (sel.value === "instance1") {
            document.getElementsByName("root-path")[0].value = "/dev/xvda";
            document.getElementsByName("root-snapshot")[0].value = "snapshot";
        }
        else if (sel.value === "instance2") {
            document.getElementsByName("root-path")[0].value = "/dev/sdb";
            document.getElementsByName("root-snapshot")[0].value = "snapshot";
        }
        else if (sel.value === "instance3") {
            document.getElementsByName("root-path")[0].value = "/dev/sdc";
            document.getElementsByName("root-snapshot")[0].value = "snapshot";
        }
    }
}

function networkSelect(sel) {
    if (sel.value !== null) {
        $ref2 = "/VersaStack-web/ops/srvc/netcreate.jsp?networkType=" + sel.value + " #service-fields";
    }
    else {
        $ref2 = "/VersaStack-web/ops/srvc/netcreate.jsp #service-fields";
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
    }
    else {
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
    }
    else {
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
    }
    else {
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
    }
    else {
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
    }
    else if (type === 'aws') {
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
    }
    else if (type === 'ops') {
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
    }
    else if (type === 'aws') {
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
    }
    else if (type === 'ops') {
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
    }
    else {
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

// API CALLS
function checkInstance(uuid) {
    var apiUrl = baseUrl + '/VersaStack-web/restapi/service/' + uuid + '/status';
    $.ajax({
        url: apiUrl,
        type: 'GET',
        success: function (result) {
            var statusElement = document.getElementById("instance-status");
            statusElement.innerHTML = result;
        }
    });
}

function propagateInstance(uuid) {
    var apiUrl = baseUrl + '/VersaStack-web/restapi/app/service/' + uuid + '/propagate';
    $.ajax({
        url: apiUrl,
        type: 'PUT',
        success: function (result) {
            window.location.reload(true);
        }
    });
}

function commitInstance(uuid) {
    var apiUrl = baseUrl + '/VersaStack-web/restapi/app/service/' + uuid + '/commit';
    $.ajax({
        url: apiUrl,
        type: 'PUT',
        success: function (result) {
            window.location.reload(true);
        }
    });
}

function revertInstance(uuid) {
    var apiUrl = baseUrl + '/VersaStack-web/restapi/app/service/' + uuid + '/revert';
    $.ajax({
        url: apiUrl,
        type: 'PUT',
        success: function (result) {
            window.location.reload(true);
        }
    });
    //window.location.replace('/VersaStack-web/ops/catalog.jsp');
}

function cancelInstance(uuid) {
    var apiUrl = baseUrl + '/VersaStack-web/restapi/app/service/' + uuid + '/cancel';
    $.ajax({
        url: apiUrl,
        type: 'PUT',
        success: function (result) {
            window.location.reload(true);
        }
    });
    //window.location.replace('/VersaStack-web/ops/catalog.jsp');
}
function forceCancelInstance(uuid) {
    var apiUrl = baseUrl + '/VersaStack-web/restapi/app/service/' + uuid + '/force_cancel';
    $.ajax({
        url: apiUrl,
        type: 'PUT',
        success: function (result) {
            window.location.reload(true);
        }
    });
    //window.location.replace('/VersaStack-web/ops/catalog.jsp');
}

function reinstateInstance(uuid) {
    var apiUrl = baseUrl + '/VersaStack-web/restapi/app/service/' + uuid + '/reinstate';
    $.ajax({
        url: apiUrl,
        type: 'PUT',
        success: function (result) {
            window.location.reload(true);
        }
    });
    //window.location.replace('/VersaStack-web/ops/catalog.jsp');
}
function forceReinstateInstance(uuid) {
    var apiUrl = baseUrl + '/VersaStack-web/restapi/app/service/' + uuid + '/force_reinstate';
    $.ajax({
        url: apiUrl,
        type: 'PUT',
        success: function (result) {
            window.location.reload(true);
        }
    });
    //window.location.replace('/VersaStack-web/ops/catalog.jsp');
}

function forceRetryInstance(uuid) {
    var apiUrl = baseUrl + '/VersaStack-web/restapi/app/service/' + uuid + '/force_retry';
    $.ajax({
        url: apiUrl,
        type: 'PUT',
        success: function (result) {
            window.location.reload(true);
        }
    });
    //window.location.replace('/VersaStack-web/ops/catalog.jsp');
}

function modifyInstance(uuid) {
    var apiUrl = baseUrl + '/VersaStack-web/restapi/app/service/' + uuid + '/modify';
    $.ajax({
        url: apiUrl,
        type: 'PUT',
        success: function (result) {
            window.location.reload(true);
        }
    });
    //window.location.replace('/VersaStack-web/ops/catalog.jsp');
}
function forceModifyInstance(uuid) {
    var apiUrl = baseUrl + '/VersaStack-web/restapi/app/service/' + uuid + '/force_modify';
    $.ajax({
        url: apiUrl,
        type: 'PUT',
        success: function (result) {
            window.location.reload(true);
        }
    });
    //window.location.replace('/VersaStack-web/ops/catalog.jsp');
}

function verifyInstance(uuid) {
    var apiUrl = baseUrl + '/VersaStack-web/restapi/app/service/' + uuid + '/verify';
    $.ajax({
        url: apiUrl,
        type: 'PUT',
        success: function (result) {
            window.location.reload(true);
        }
    });
    //window.location.replace('/VersaStack-web/ops/catalog.jsp');
}

function deleteInstance(uuid) {
    var apiUrl = baseUrl + '/VersaStack-web/restapi/app/service/' + uuid + '/delete';
    $.ajax({
        url: apiUrl,
        type: 'PUT',
        success: function (result) {
            console.log("DELETION SUCCESSFUL");
            window.location.replace('/VersaStack-web/ops/catalog.jsp');
        }
    });
}

// TEMPLATING

function resetForm() {
    var form = document.getElementById('custom-form');
    form.reset();
}

function applyNetTemplate(code) {
    var form = document.getElementById('custom-form');
    form.reset();

    switch (code) {
        case 1:
            form.elements['netType'].value = 'internal';
            form.elements['netCidr'].value = '10.1.0.0/16';

            if (subRouteCounter === 1) {
                addSubnetRoute('subnet1-route');
            }
            if (subnetCounter === 1) {
                addSubnet('aws');
            }

            form.elements['subnet1-name'].value = '';
            form.elements['subnet1-cidr'].value = '10.1.0.0/24';
            form.elements['subnet1-route1-to'].value = '206.196.0.0/16';
            form.elements['subnet1-route1-next'].value = 'internet';

            form.elements['subnet1-route2-to'].value = '72.24.24.0/24';
            form.elements['subnet1-route2-next'].value = 'vpn';
            form.elements['subnet1-route-prop'].checked = true;

            form.elements['subnet2-name'].value = '';
            form.elements['subnet2-cidr'].value = '10.1.1.0/24';

            break;

        case 2:
            form.elements['netType'].value = 'internal';
            form.elements['netCidr'].value = '10.1.0.0/16';

            if (subRouteCounter === 1) {
                addSubnetRoute('subnet1-route');
            }
            if (subnetCounter === 1) {
                addSubnet('aws');
            }

            form.elements['subnet1-name'].value = '';
            form.elements['subnet1-cidr'].value = '10.1.0.0/24';
            form.elements['subnet1-route1-to'].value = '206.196.0.0/16';
            form.elements['subnet1-route1-next'].value = 'internet';
            form.elements['subnet1-route2-to'].value = '72.24.24.0/24';
            form.elements['subnet1-route2-next'].value = 'vpn';
            form.elements['subnet1-route-prop'].checked = true;
            form.elements['subnet1-vm1'].value = 'vm1';


            form.elements['subnet2-name'].value = '';
            form.elements['subnet2-cidr'].value = '10.1.1.0/24';
            form.elements['subnet2-vm2'].value = 'vm2';

            form.elements['conn-dest'].value = 'urn:ogf:network:domain=dragon.maxgigapop.net:node=CLPK:port=1-1-2:link=*';
            form.elements['conn-vlan'].value = 'any';

            break;

        case 3:
            form.elements['netType'].value = 'internal';
            form.elements['netCidr'].value = '10.1.0.0/16';

            if (subRouteCounter === 1) {
                addSubnetRoute('subnet1-route');
            }
            if (subnetCounter === 1) {
                addSubnet('aws');
            }

            form.elements['subnet1-name'].value = '';
            form.elements['subnet1-cidr'].value = '10.1.0.0/24';

            form.elements['subnet1-route1-to'].value = '206.196.0.0/16';
            form.elements['subnet1-route1-next'].value = 'internet';
            form.elements['subnet1-route2-to'].value = '72.24.24.0/24';
            form.elements['subnet1-route2-next'].value = 'vpn';
            form.elements['subnet1-route-prop'].checked = true;

            form.elements['subnet1-vm1'].value = 'test_with_vm_types_1';
            form.elements['subnet1-vm1-image'].value = 'ami-08111162';
            form.elements['subnet1-vm1-instance'].value = 't2.micro';

            form.elements['subnet2-name'].value = '';
            form.elements['subnet2-cidr'].value = '10.1.1.0/24';

            form.elements['subnet2-vm2'].value = 'test_with_vm_types_2';
            form.elements['subnet2-vm2-image'].value = 'ami-fce3c696';
            form.elements['subnet2-vm2-instance'].value = 't2.small';
            form.elements['subnet2-vm2-keypair'].value = 'xi-aws-max-dev-key';
            form.elements['subnet2-vm2-security'].value = 'geni';

            break;

        case 4:
            form.elements['netType'].value = 'internal';
            form.elements['netCidr'].value = '10.1.0.0/16';

//            if (subRouteCounter === 1) {
//                addSubnetRoute('subnet1-route');
//            }
//            if (subnetCounter === 1) {
//                addSubnet('ops');
//            }

            form.elements['subnet1-name'].value = '';
            form.elements['subnet1-cidr'].value = '10.1.0.0/24';

//            form.elements['subnet1-route1-to'].value = '206.196.0.0/16';
//            form.elements['subnet1-route1-next'].value = 'internet';
//            form.elements['subnet1-route2-to'].value = '72.24.24.0/24';
//            form.elements['subnet1-route2-next'].value = 'vpn';
            form.elements['subnet1-route-default'].checked = true;

            form.elements['subnet1-vm1'].value = 'vm_OPS';
//            form.elements['subnet1-vm1-instance'].value = 'm1.medium';
//            form.elements['subnet1-vm1-keypair'].value = 'icecube_key';
//            form.elements['subnet1-vm1-security'].value = 'rains';
            form.elements['subnet1-vm1-host'].value = 'msx1';

//            form.elements['subnet2-name'].value = '';
//            form.elements['subnet2-cidr'].value = '10.1.1.0/24';
//            
            break;

        case 5:
            form.elements['netType'].value = 'internal';
            form.elements['netCidr'].value = '10.1.0.0/16';

            if (VMRouteCounter === 1) {
                addVMRoute('subnet1-vm1-route');
            }

            form.elements['subnet1-name'].value = '';
            form.elements['subnet1-cidr'].value = '10.1.0.0/24';

//            form.elements['subnet1-route1-to'].value = '206.196.0.0/16';
//            form.elements['subnet1-route1-next'].value = 'internet';
//            form.elements['subnet1-route2-to'].value = '72.24.24.0/24';
//            form.elements['subnet1-route2-next'].value = 'vpn';
            form.elements['subnet1-route-default'].checked = true;

            form.elements['subnet1-vm1'].value = 'vm_OPS';
            form.elements['subnet1-vm1-instance'].value = '4';
            form.elements['subnet1-vm1-image'].value = '77817b73-baa2-424b-b890-e1a95af1fdf9';
            form.elements['subnet1-vm1-keypair'].value = 'icecube_key';
            form.elements['subnet1-vm1-security'].value = 'rains';
            form.elements['subnet1-vm1-host'].value = 'msx1';
            form.elements['subnet1-vm1-floating'].value = '206.196.180.148';
            form.elements['subnet1-vm1-sriov1-dest'].value = 'urn:ogf:network:domain=dragon.maxgigapop.net:node=CLPK:port=1-2-3:link=*';
            form.elements['subnet1-vm1-sriov1-mac'].value = 'aa:bb:cc:00:00:12';
            form.elements['subnet1-vm1-sriov1-ip'].value = '10.10.0.1/30';
            form.elements['subnet1-vm1-route1-to'].value = '192.168.0.0/24';
            form.elements['subnet1-vm1-route1-next'].value = '10.10.0.2';
            form.elements['subnet1-vm1-route2-to'].value = '206.196.179.0/24';
            form.elements['subnet1-vm1-route2-next'].value = '10.10.0.2';

//            form.elements['subnet2-name'].value = '';
//            form.elements['subnet2-cidr'].value = '10.1.1.0/24';

            break;
    }
}

function applyFL2PTemplate(code) {
    var form = document.getElementById('custom-form');
    form.reset();

    switch (code) {
        case 1:
            form.elements['topUri'].value = 'urn:ogf:network:domain=vo1.versastack.org:link=link1';
            form.elements['eth-src'].value = 'urn:ogf:network:onos.maxgigapop.net:network1:of:0000000000000005:port-s5-eth1';
            form.elements['eth-des'].value = 'urn:ogf:network:onos.maxgigapop.net:network1:of:0000000000000002:port-s2-eth1';

            break;
    }

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


//**



/*
 
 function applyTemplate(code) {
 switch(code) {
 case 1: 
 var form = document.getElementById('');
 
 form.elements[''] = '';
 
 break;
 case 2:
 
 break;
 default:
 
 }
 } 
 
 
 function clearView() {
 localStorage.removeItem('queryJSON');
 
 evt.preventDefault();
 }
 
 function newQuery() {
 $("#query-table").toggleClass("hide");
 
 evt.preventDefault();
 }
 
 function addQuery() {
 var json = localStorage.getItem('queryJSON');
 if (json === null) {
 var arr = [document.getElementById("sparquery").value];
 } 
 else {        
 var arr = JSON.parse(json);
 arr.push(document.getElementById("sparquery").value);
 }
 var newJSON = JSON.stringify(arr);
 localStorage.setItem('queryJSON', newJSON);
 
 $("#service-bottom").load("/VersaStack-web/ops/srvc/viewcreate.jsp?mode=create #service-fields");
 }*/



// Utility Functions


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

var getUrlParameter = function getUrlParameter(sParam) {
    var sPageURL = decodeURIComponent(window.location.search.substring(1)),
            sURLVariables = sPageURL.split('&'),
            sParameterName,
            i;

    for (i = 0; i < sURLVariables.length; i++) {
        sParameterName = sURLVariables[i].split('=');

        if (sParameterName[0] === sParam) {
            return sParameterName[1] === undefined ? true : sParameterName[1];
        }
    }
};

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

// Helper method to parse the title tag from the response.
function getTitle(text) {
    return text.match('<title>(.*)?</title>')[1];
}

function enableLoading() {
    $("#main-pane").addClass("loading");
}

function disableLoading() {
    $("#main-pane").removeClass("loading");
}
