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

/* global XDomainRequest, baseUrl, keycloak */

//jQuery time
var current_fs, next_fs, previous_fs; //fieldsets
var left, opacity, scale; //fieldset properties which we will animate
var animating; //flag to prevent quick multi-click glitches
var stage = 1;
var last_stage = 6;

// Page Load Function
$(function () {
    loadKeycloakACL();

    $(".next").click(function () {
        if (animating)
            return false;
        animating = true;

        current_fs = $(this).parent();
        next_fs = $(this).parent().next();
        nextStage(current_fs, next_fs);
    });

    $(".previous").click(function () {
        if (animating)
            return false;
        animating = true;

        current_fs = $(this).parent();
        previous_fs = $(this).parent().prev();
        previousStage(current_fs, previous_fs);
    });

    $(".reset").click(function () {
        if (animating)
            return false;
        animating = true;

        resetStages();

        current_div = $(this).parent();
        base_div = $('#1-base-1');
        previousStage(current_div, base_div);
    });

    $(".subfs-headrow").click(function () {
        var head = $(this).parent();
        var body = head.next();

        body.toggleClass("hide");
    });

    $("#info-panel").click(function () {
        $("#black-screen").addClass("off");
        $(this).removeClass("active");
    });

    $("#profile-save-label").click(function () {
        if ($("#profile-save-check").is(':checked')) {
            $("#profile-save-body").removeClass("fade-hide");
        } else {
            $("#profile-save-body").addClass("fade-hide");
        }
    });

    $("#progressbar li").click(function () {
        if (animating || $(this).hasClass('disabled') || $("#progressbar li").index(this) === 0)
            return false;
        animating = true;

        var curr_id = $(".active-fs").attr('id');
        var curr_index = curr_id.charAt(0);
        current_div = $("#" + curr_id);

        var next_index = $("#progressbar li").index(this) + 1;
        var next_superid = next_index + "-1";

        var next_div_list = $("[id^=" + next_superid + "]");
        if (next_div_list.last().children().first().children().length === 0) {
            next_div = next_div_list.first();
        } else {
            next_div = next_div_list.last();
        }

        if (next_index === 1) {
            resetStages();

            base_div = $('#1-base-1');
            previousStage(current_div, base_div);
        } else if (next_index > curr_index) {
            nextStage(current_div, next_div);
        } else if (next_index < curr_index) {
            previousStage(current_div, next_div);
        }
    });
});

function startEditor(mode) {
    $("#mode-panel").css("top", "-50%");

    if (mode === 0) {
        $("#wizform").removeClass("disabled");
        $("#0-base-select").addClass("active-fs");
    } else {
        $("#msform").removeClass("disabled");
        $("#0-template-select").addClass("active-fs");
    }
}

function resetStages() {
    $("#progressbar li").addClass("disabled");
    $("#msform")[0].reset();
}

function setProgress(stage_num) {
    // .eq is zero-indexed; stage 1 is actually element 0.
    if (stage < stage_num) {
        for (i = stage; i < stage_num; i++) {
            $("#progressbar li").eq(i).addClass("active");
        }
    } else if (stage > stage_num) {
        for (i = stage - 1; i >= stage_num; i--) {
            $("#progressbar li").eq(i).removeClass("active");
        }
    }

    stage = stage_num;
}

function nextStage(current_fs, incoming_fs) {
    $(".active-fs").removeClass("active-fs");
    incoming_fs.addClass("active-fs");
    var incomingStage = incoming_fs.attr('id').substring(0, 1);
    setProgress(incomingStage);

    //show the next fieldset
    incoming_fs.show();
    //hide the current fieldset with style
    current_fs.animate({opacity: 0}, {
        step: function (now, mx) {
            //as the opacity of current_fs reduces to 0 - stored in "now"
            //1. scale current_fs down to 80%
            //scale = 1 - (1 - now) * 0.2;
            //2. bring next_fs from the right(50%)
            left = (now * 50) + "%";
            //3. increase opacity of next_fs to 1 as it moves in
            opacity = 1 - now;
            //current_fs.css({'transform': 'scale(' + scale + ')'});
            incoming_fs.css({'left': left, 'opacity': opacity});
        },
        duration: 800,
        complete: function () {
            current_fs.hide();
            animating = false;
        }
    });
}

function previousStage(current_fs, incoming_fs) {
    $(".active-fs").removeClass("active-fs");
    incoming_fs.addClass("active-fs");
    var incomingStage = incoming_fs.attr('id').substring(0, 1);
    setProgress(incomingStage);

    //show the next fieldset
    incoming_fs.show();
    //hide the current fieldset with style
    current_fs.animate({opacity: 0}, {
        step: function (now, mx) {
            //as the opacity of current_fs reduces to 0 - stored in "now"
            //1. scale previous_fs from 80% to 100%
            //scale = 0.8 + (1 - now) * 0.2;
            //2. take current_fs to the right(50%) - from 0%
            left = ((1 - now) * 50) + "%";
            //3. increase opacity of previous_fs to 1 as it moves in
            opacity = 1 - now;
            current_fs.css({'left': left});
            incoming_fs.css({'transform': 'scale(' + scale + ')', 'opacity': opacity});
        },
        duration: 800,
        complete: function () {
            current_fs.hide();
            animating = false;
        }
    });
}

function applyTemplate(mode) {
    var form = document.getElementById('msform');


    $("#black-screen").addClass("off");
    if (animating)
        return false;
    animating = true;

    if (mode === 0) {
        template_div = $('#0-1');
        next_div = $('#2-1');
        nextStage(template_div, next_div);
    }
    //changes made here
    // Basic Hybrid Cloud Template
    if (mode === 1) {
        current_div = $("#0-1");
        next_div = $("#2-1");

        // Stage 1

        // Stage 2           
        form.elements['aws-netCidr'].value = '10.0.0.0/16';

        form.elements['ops-netCidr'].value = '10.1.0.0/16';

        // Stage 3
        var subnetCounter = document.getElementById('awsStage3-subnet');
        subnetCounter.value = 1;
        setSubnets(subnetCounter);
        var sub1RouteCounter = document.getElementById('awsStage3-subnet1-routes');
        sub1RouteCounter.value = 1;
        setSubRoutes(sub1RouteCounter);

        var subnetCounter = document.getElementById('opsStage3-subnet');
        subnetCounter.value = 1;
        setSubnets(subnetCounter);

        form.elements['aws-subnet1-name'].value = 'subnet1';
        form.elements['aws-subnet1-cidr'].value = '10.0.0.0/24';
        form.elements['aws-subnet1-route1-to'].value = '206.196.0.0/16';
        form.elements['aws-subnet1-route1-next'].value = 'internet';
//            form.elements['aws-subnet1-route-prop'].checked = true;

        form.elements['ops-subnet1-name'].value = 'subnet1';
        form.elements['ops-subnet1-cidr'].value = '10.1.0.0/24';
//            form.elements['ops-subnet1-route-default'].checked = true;

        // Stage 4
        var vmCounter = document.getElementById('awsStage5-vm');
        vmCounter.value = 1;
        setVMs(vmCounter);

        var vmCounter = document.getElementById('opsStage5-vm');
        vmCounter.value = 1;
        setVMs(vmCounter);
        var vm1VolumeCounter = document.getElementById('opsStage5-vm1-volumes');
        vm1VolumeCounter.value = 2;
        setVMVolumes(vm1VolumeCounter);

        form.elements['aws-vm1-name'].value = 'ec2-vpc1-vm1';
        $("#awsStage4-vm1-table select").val("1");
        form.elements['aws-vm1-image'].value = 'ami-0d1bf860';
        form.elements['aws-vm1-instance'].value = 'm4.large';
        form.elements['aws-vm1-keypair'].value = 'driver_key';
        form.elements['aws-vm1-security'].value = 'geni';

        form.elements['ops-vm1-name'].value = 'ops-vtn1-vm1';
        $("#opsStage4-vm1-table select").val("1");
        form.elements['ops-vm1-instance'].value = '2';
        form.elements['ops-vm1-keypair'].value = 'demo_key';
        form.elements['ops-vm1-security'].value = 'rains';
        form.elements['ops-vm1-floating'].value = '10.10.252.164/24';
        form.elements['ops-vm1-host'].value = 'rvtk-compute3';
        form.elements['ops-vm1-volume1-size'].value = '1024';
        form.elements['ops-vm1-volume1-mount'].value = '/mnt/ceph0_1tb';
        form.elements['ops-vm1-volume2-size'].value = '1024';
        form.elements['ops-vm1-volume2-mount'].value = '/mnt/ceph1_1tb';

        form.elements['bgp-number'].value = '7224';
        form.elements['bgp-key'].value = 'stackv';
        form.elements['bgp-networks'].value = '10.10.0.0/16';
        $("#ops4-bgp-table select").val("1");

        // Gateways    
        var gatewayCounter = document.getElementById('opsStage4-gateway');
        gatewayCounter.value = 2;
        setGateways(gatewayCounter);

        form.elements['gateway1-name'].value = 'ceph-net';
        $("#gateway1-type-select").val("port_profile");
//            form.elements['gateway1-from'].value = 'Ceph-Storage';
        form.elements['gateway2-name'].value = 'intercloud-1';
        $("#gateway2-type-select").val("inter_cloud_network");
//            form.elements['gateway2-to'].value = 'urn:ogf:network:aws.amazon.com:aws-cloud?vlan=any';

        // SRIOVs
        $("#SRIOV1-vm-select").val("1");
        $("#SRIOV1-gateway-select").val("1");
//            form.elements['SRIOV1-name'].value = 'ops-vtn1-vm1';
//            form.elements['SRIOV1-ip'].value = '192.168.1.2';
//            form.elements['SRIOV1-mac'].value = '11:22:22:33:33:01';
//            $("#SRIOV2-vm-select").val("1");
//            $("#SRIOV2-gateway-select").val("2");
//            form.elements['SRIOV2-name'].value = 'ops-vtn1:vm1:eth2';
//            form.elements['SRIOV2-ip'].value = '10.10.0.1';
//            form.elements['SRIOV2-mac'].value = '11:22:22:33:33:02';
        nextStage(current_div, next_div);
    }



}

var awsSubnetCount;
var opsSubnetCount;
function setSubnets(input) {
    var type = input.id.substring(0, 3) + "-";
    if (type === 'aws-') {
        awsSubnetCount = input.value;
    } else if (type === 'ops-') {
        opsSubnetCount = input.value;
    }

    var stage = input.id;
    var old = input.oldvalue;
    var fieldset = document.getElementById(stage + "-fs");
    var subTable = document.getElementById(stage + "-route-table");
    $("#" + stage + "-route-table tr").remove();
    fieldset.innerHTML = "";

    var start = 1;
    for (i = start; i <= input.value; i++) {

        var table = document.createElement("table");
        table.className = 'subfs-table';
        table.id = stage + i + '-table';

        var thead = document.createElement("thead");
        var tbody1 = document.createElement("tbody");
        tbody1.className = 'fade-hide';
        var tbody2 = document.createElement("tbody");
        tbody2.className = 'fade-hide';

        var row1 = document.createElement("tr");
        row1.className = 'subfs-headrow closed';
        var cell1_1 = document.createElement("th");
        var cell1_2 = document.createElement("th");
        cell1_1.innerHTML = type.toUpperCase() + ' Subnet ' + i;
        row1.appendChild(cell1_1);
        row1.appendChild(cell1_2);
        row1.innerHTML += '<br>';
        thead.appendChild(row1);
        table.appendChild(thead);

        row1.addEventListener('click', function () {
            var head = $(this).parent();
            var body1 = head.next();
            var body2 = body1.next();

            $(this).toggleClass("closed");
            body1.toggleClass("fade-hide");
            body2.toggleClass("fade-hide");
        });

        var row2 = document.createElement("tr");
        var cell2_1 = document.createElement("td");
        var cell2_2 = document.createElement("td");
        cell2_1.innerHTML = '<input type="text" name="' + type + 'subnet' + i + '-name" id="' + type + 'subnet' + i + '-tag" onchange="updateSubnetNames(this)" placeholder="Name"/>';
        cell2_2.innerHTML = '<input type="text" name="' + type + 'subnet' + i + '-cidr" placeholder="CIDR Block"/>';
        row2.appendChild(cell2_1);
        row2.appendChild(cell2_2);
        tbody1.appendChild(row2);

        table.appendChild(tbody1);
        table.appendChild(tbody2);
        fieldset.appendChild(table);

        // Set inputs for subnet routes
        var row = subTable.insertRow(i - 1);
        var cell = row.insertCell(0);

        cell.innerHTML = '<div class="fs-subtext">How many routes for Subnet ' + i + '?   ' +
                '<input type="number" class="small-counter" id="' + stage + i + '-routes" ' +
                'onfocus="this.oldvalue = this.value;" ' +
                'onchange="setSubRoutes(this)" /></div>';
    }
}

function setSubRoutes(input) {
    // Grab correct subnet table
    var type = input.id.substring(0, 3) + "-";
    var subnetId = input.id.substring(0, input.id.length - 7);
    var table = document.getElementById(subnetId + '-table').getElementsByTagName('tbody')[1];
    var subnetNum = subnetId.substring(subnetId.length - 1);
    table.innerHTML = "";

    var subRouteCount = input.value;
    for (j = 1; j <= subRouteCount; j++) {
        var row3 = table.insertRow(j - 1);
        var cell3_1 = row3.insertCell(0);

        cell3_1.innerHTML = '<input type="text" name="' + type + 'subnet' + subnetNum + '-route' + j + '-from" placeholder="From"/>' +
                '<input type="text" name="' + type + 'subnet' + subnetNum + '-route' + j + '-to" placeholder="To"/>' +
                '<input type="text" name="' + type + 'subnet' + subnetNum + '-route' + j + '-next" placeholder="Next Hop"/>';
    }
}

var awsVmCount;
var opsVmCount;
function setVMs(input) {
    var subnetCount;
    var type = input.id.substring(0, 3) + "-";
    if (type === 'aws-') {
        awsVmCount = input.value;
        subnetCount = awsSubnetCount;
    } else if (type === 'ops-') {
        opsVmCount = input.value;
        subnetCount = opsSubnetCount;
    }

    var stage = input.id;
    var old = input.oldvalue;
    var fieldset = document.getElementById(stage + "-fs");
    var SRIOVfieldset = document.getElementById("opsStage6-sriov-fs");
    var vmTable = document.getElementById(stage + "-route-table");
    $("#" + stage + "-route-table tr").remove();
    fieldset.innerHTML = "";
    if (type === "ops-") {
        SRIOVfieldset.innerHTML = "";
    }

    var start = 1;
    if (opsVmCount > 0 && type === 'ops-') {
        var toptable = document.createElement("table");
        toptable.className = 'subfs-table';
        toptable.id = "opsStage5-bgp-table";
        var tbody = document.createElement("tbody");
        var row6 = document.createElement("tr");
        var cell6_1 = document.createElement("td");
        var cell6_2 = document.createElement("td");
        cell6_1.innerHTML = '<input type="text" name="bgp-number" placeholder="BGP AS Number">';
        cell6_2.innerHTML = '<input type="text" name="bgp-key" placeholder="BGP Authentication Key">';
        row6.appendChild(cell6_1);
        row6.appendChild(cell6_2);
        tbody.appendChild(row6);

        var row7 = document.createElement("tr");
        var cell7_1 = document.createElement("td");
        var cell7_2 = document.createElement("td");
        cell7_1.innerHTML = '<input type="text" name="bgp-networks" placeholder="BGP Networks (in comma-separated list)">';
        var selectString = '<select name="bgp-vm"><option selected disabled>Select the VM host</option>';
        for (i = start; i <= input.value; i++) {
            selectString += '<option value="' + i + '">VM ' + i + '</option>';
        }
        selectString += '</select>';
        cell7_2.innerHTML = selectString;
        row7.appendChild(cell7_1);
        row7.appendChild(cell7_2);
        tbody.appendChild(row7);

        toptable.appendChild(tbody);
        fieldset.appendChild(toptable);
    }

    for (i = start; i <= input.value; i++) {
        // Set stage 3 data table
        var table = document.createElement("table");
        table.className = 'subfs-table';
        table.id = stage + i + '-table';

        var thead = document.createElement("thead");
        var tbody1 = document.createElement("tbody");
        tbody1.className = 'fade-hide';
        var tbody2 = document.createElement("tbody");
        tbody2.className = 'fade-hide';
        var tbody3 = document.createElement("tbody");
        tbody3.className = 'fade-hide';

        var row1 = document.createElement("tr");
        row1.className = 'subfs-headrow closed';
        var cell1_1 = document.createElement("th");
        var cell1_2 = document.createElement("th");
        cell1_1.innerHTML = 'VM ' + i;
        row1.appendChild(cell1_1);
        row1.appendChild(cell1_2);
        row1.innerHTML += '<br>';
        thead.appendChild(row1);
        table.appendChild(thead);

        row1.addEventListener('click', function () {
            var head = $(this).parent();
            var body1 = head.next();
            var body2 = body1.next();
            var body3 = body2.next();

            $(this).toggleClass("closed");
            body1.toggleClass("fade-hide");
            body2.toggleClass("fade-hide");
            body3.toggleClass("fade-hide");
        });

        var row2 = document.createElement("tr");
        var cell2_1 = document.createElement("td");
        var cell2_2 = document.createElement("td");

        var selectString = '<select name="' + type + 'vm' + i + '-subnet" id="vm' + i + '-subnet-select"><option selected disabled>Select the subnet host</option>';
        for (j = 1; j <= subnetCount; j++) {
            var subnetTag = document.getElementById(type + "subnet" + j + "-tag");

            selectString += '<option value="' + j + '">Subnet ' + j + ' (' + subnetTag.value + ')</option>';
        }
        selectString += '</select>';

        cell2_1.innerHTML = '<td><input type="text" id="vm' + i + '-tag" onchange="updateVMNames(this)" name="' + type + 'vm' + i + '-name" placeholder="Name"></td>';
        cell2_2.innerHTML = selectString;
        row2.appendChild(cell2_1);
        row2.appendChild(cell2_2);
        tbody1.appendChild(row2);

        var row3 = document.createElement("tr");
        var cell3_1 = document.createElement("td");
        var cell3_2 = document.createElement("td");
        cell3_1.innerHTML = '<input type="text" name="' + type + 'vm' + i + '-keypair" placeholder="Keypair Name">';
        cell3_2.innerHTML = '<input type="text" name="' + type + 'vm' + i + '-security" placeholder="Security Group">';
        row3.appendChild(cell3_1);
        row3.appendChild(cell3_2);
        tbody1.appendChild(row3);

        var row4 = document.createElement("tr");
        var cell4_1 = document.createElement("td");
        var cell4_2 = document.createElement("td");
        cell4_1.innerHTML = '<input type="text" name="' + type + 'vm' + i + '-image" placeholder="Image Type">';
        cell4_2.innerHTML = '<input type="text" name="' + type + 'vm' + i + '-instance" placeholder="Instance Type">';
        row4.appendChild(cell4_1);
        row4.appendChild(cell4_2);
        tbody1.appendChild(row4);

        if (stage.substring(0, 3) === 'ops') {
            var row5 = document.createElement("tr");
            var cell5_1 = document.createElement("td");
            var cell5_2 = document.createElement("td");
            cell5_1.innerHTML = '<input type="text" name="' + type + 'vm' + i + '-host" placeholder="Host(\'any\') "value="any">';
            cell5_2.innerHTML = '<input type="text" name="' + type + 'vm' + i + '-floating" placeholder="Floating IP(\'any\') "value="any">';
            row5.appendChild(cell5_1);
            row5.appendChild(cell5_2);
            tbody1.appendChild(row5);
        }

        table.appendChild(tbody1);
        table.appendChild(tbody2);
        table.appendChild(tbody3);
        fieldset.appendChild(table);

        if (stage.substring(0, 3) === 'ops') {
            var SRIOVfs = document.createElement("fieldset");
            SRIOVfs.className = 'subfs';
            SRIOVfs.id = "opsStage6-vm" + i + "-sriov-fs";
            SRIOVfieldset.appendChild(SRIOVfs);

            // Set secondary vm routes
            var row = vmTable.insertRow(i - 1);
            var cell = row.insertCell(0);

            cell.innerHTML = '<div class="fs-subtext">How many routes for VM ' + i + '?   ' +
                    '<input type="number" class="small-counter" id="' + stage + i + '-routes" ' +
                    'onfocus="this.oldvalue = this.value;" ' +
                    'onchange="setVMRoutes(this)" /></div>' +
                    '<div class="fs-subtext">How many SRIOV for VM ' + i + '?   ' +
                    '<input type="number" class="small-counter" id="opsStage6-vm' + i + '-sriov" ' +
                    'onfocus="this.oldvalue = this.value;" ' +
                    'onchange="setVMSRIOV(this)" /></div>' +
                    '<div class="fs-subtext">How many Ceph RBD volumes for VM ' + i + '?   ' +
                    '<input type="number" class="small-counter" id="' + stage + i + '-volumes" ' +
                    'onfocus="this.oldvalue = this.value;" ' +
                    'onchange="setVMVolumes(this)" /></div>';
        }
    }
}

function setVMRoutes(input) {
    // Grab correct vm table
    var type = input.id.substring(0, 3) + "-";
    var vmId = input.id.substring(0, input.id.length - 7);
    var table = document.getElementById(vmId + '-table').getElementsByTagName('tbody')[1];
    var vmNum = vmId.substring(vmId.length - 1);
    table.innerHTML = "";

    var vmRouteCount = input.value;
    for (j = 1; j <= vmRouteCount; j++) {
        var row = table.insertRow(j - 1);
        var cell = row.insertCell(0);

        cell.innerHTML = '<input type="text" name="' + type + 'vm' + vmNum + '-route' + j + '-from" placeholder="From"/>' +
                '<input type="text" name="' + type + 'vm' + vmNum + '-route' + j + '-to" placeholder="To"/>' +
                '<input type="text" name="' + type + 'vm' + vmNum + '-route' + j + '-next" placeholder="Next Hop"/>';
    }
}

function setVMVolumes(input) {
    // Grab correct vm table
    var type = input.id.substring(0, 3) + "-";
    var vmId = input.id.substring(0, input.id.length - 8);
    var table = document.getElementById(vmId + '-table').getElementsByTagName('tbody')[2];
    var vmNum = vmId.substring(vmId.length - 1);
    table.innerHTML = "";

    var vmVolumeCount = input.value;
    for (j = 1; j <= vmVolumeCount; j++) {
        var row = table.insertRow(j - 1);
        var cell1 = row.insertCell(0);
        var cell2 = row.insertCell(0);

        cell1.innerHTML = '<input type="text" name="' + type + 'vm' + vmNum + '-volume' + j + '-mount" placeholder="Mount point"/>';
        cell2.innerHTML = '<input type="text" name="' + type + 'vm' + vmNum + '-volume' + j + '-size" placeholder="Disk size (in GB)"/>';
    }
}

var gatewayCount;
function setGateways(input) {
    gatewayCount = input.value;

    var stage = input.id;
    var old = input.oldvalue;
    var fieldset = document.getElementById(stage + "-fs");

    fieldset.innerHTML = "";

    var table = document.createElement("table");
    table.className = 'subfs-table';
    table.id = stage + i + '-table';

    var thead = document.createElement("thead");
    var tbody = document.createElement("tbody");
    tbody.className = 'fade-hide';

    var row1 = document.createElement("tr");
    row1.className = 'subfs-headrow closed';
    var cell1_1 = document.createElement("th");
    var cell1_2 = document.createElement("th");
    cell1_1.innerHTML = 'Intercloud Gateway';
    row1.appendChild(cell1_1);
    row1.appendChild(cell1_2);
    row1.innerHTML += '<br>';
    thead.appendChild(row1);
    table.appendChild(thead);

    row1.addEventListener('click', function () {
        var head = $(this).parent();
        var body = head.next();

        $(this).toggleClass("closed");
        body.toggleClass("fade-hide");
    });

    var row2 = document.createElement("tr");
    var cell2_1 = document.createElement("td");
    var cell2_2 = document.createElement("td");
    cell2_1.innerHTML = '<td><input type="text" id="gateway0-tag" onchange="updateGatewayNames(this)" name="gateway0-name" value="intercloud-1"></td>';
    cell2_2.innerHTML = '<select id="gateway0-type-select" name="gateway0-type"><option selected value="intercloud">Inter-cloud Network</option></select>';
    row2.appendChild(cell2_1);
    row2.appendChild(cell2_2);
    tbody.appendChild(row2);

    table.appendChild(tbody);
    fieldset.appendChild(table);

    var start = 1;
    for (i = start; i <= input.value; i++) {
        // Set stage 3 data table
        var table = document.createElement("table");
        table.className = 'subfs-table';
        table.id = stage + i + '-table';

        var thead = document.createElement("thead");
        var tbody = document.createElement("tbody");
        tbody.className = 'fade-hide';

        var row1 = document.createElement("tr");
        row1.className = 'subfs-headrow closed';
        var cell1_1 = document.createElement("th");
        var cell1_2 = document.createElement("th");
        cell1_1.innerHTML = 'Gateway ' + i;
        row1.appendChild(cell1_1);
        row1.appendChild(cell1_2);
        row1.innerHTML += '<br>';
        thead.appendChild(row1);
        table.appendChild(thead);

        row1.addEventListener('click', function () {
            var head = $(this).parent();
            var body = head.next();

            $(this).toggleClass("closed");
            body.toggleClass("fade-hide");
        });

        var row2 = document.createElement("tr");
        var cell2_1 = document.createElement("td");
        var cell2_2 = document.createElement("td");
        cell2_1.innerHTML = '<td><input type="text" id="gateway' + i + '-tag" onchange="updateGatewayNames(this)" name="gateway' + i + '-name" placeholder="Name"></td>';
        cell2_2.innerHTML = '<select id="gateway' + i + '-type-select" name="gateway' + i + '-type"><option selected disabled>Select the hosting Gateway</option>'
                + '<option value="ucs">UCS Port Profile</option>'
                + '<option value="intercloud">Inter-cloud Network</option>'
                + '<option value="stitch">L2 Stitch Port</option></select>';
        row2.appendChild(cell2_1);
        row2.appendChild(cell2_2);
        tbody.appendChild(row2);

        var row4 = document.createElement("tr");
        var cell4_1 = document.createElement("td");
        var cell4_2 = document.createElement("td");
        cell4_1.innerHTML = '<input type="text" name="gateway' + i + '-value" placeholder="Value"/>';
        row4.appendChild(cell4_1);
        row4.appendChild(cell4_2);
        tbody.appendChild(row4);

        table.appendChild(tbody);
        fieldset.appendChild(table);
    }
}

function setVMSRIOV(input) {
    var stage = input.id;
    var old = input.oldvalue;
    var vm = input.id.substring(12, 13);
    var fieldset = document.getElementById(stage + "-fs");
    var subTable = document.getElementById(stage + "-route-table");

    $("#" + stage + "-route-table tr").remove();
    fieldset.innerHTML = "";

    var start = 1;
    for (i = start; i <= input.value; i++) {
        // Set stage 3 data table
        var table = document.createElement("table");
        table.className = 'subfs-table';
        table.id = stage + i + '-table';

        var thead = document.createElement("thead");
        var tbody1 = document.createElement("tbody");
        tbody1.className = 'fade-hide';
        var tbody2 = document.createElement("tbody");
        tbody2.className = 'fade-hide';

        var row1 = document.createElement("tr");
        row1.className = 'subfs-headrow closed';
        var cell1_1 = document.createElement("th");
        var cell1_2 = document.createElement("th");
        cell1_1.innerHTML = 'VM ' + vm + ' - SRIOV ' + i;
        row1.appendChild(cell1_1);
        row1.appendChild(cell1_2);
        row1.innerHTML += '<br>';
        thead.appendChild(row1);
        table.appendChild(thead);

        row1.addEventListener('click', function () {
            var head = $(this).parent();
            var body1 = head.next();
            var body2 = body1.next();

            $(this).toggleClass("closed");
            body1.toggleClass("fade-hide");
            body2.toggleClass("fade-hide");
        });

        var selectString1 = '<select name="vm' + vm + '-SRIOV' + i + '-gateway" id="vm' + vm + '-SRIOV' + i + '-gateway-select"><option selected disabled>Select the hosting Gateway</option>';
        for (j = 0; j <= gatewayCount; j++) {
            var gatewayTag = document.getElementById("gateway" + j + "-tag");

            selectString1 += '<option value="' + j + '">Gateway ' + j + ' (' + gatewayTag.value + ')</option>';
        }
        selectString1 += '</select>';

        var selectString2 = '<select name="" id="vm' + vm + '-SRIOV' + i + '-vm-select"><option value="' + vm + '">VM ' + vm + '</option>';
        selectString2 += '</select>';

        var row2 = document.createElement("tr");
        var cell2_1 = document.createElement("td");
        var cell2_2 = document.createElement("td");
        cell2_1.innerHTML = selectString1;
        cell2_2.innerHTML = selectString2;
        row2.appendChild(cell2_1);
        row2.appendChild(cell2_2);
        tbody1.appendChild(row2);

        var row3 = document.createElement("tr");
        var cell3_1 = document.createElement("td");
        var cell3_2 = document.createElement("td");
        cell3_1.innerHTML = '<input type="text" name="vm' + vm + '-SRIOV' + i + '-name" placeholder="Name"/>';
        row3.appendChild(cell3_1);
        row3.appendChild(cell3_2);
        tbody1.appendChild(row3);

        var row4 = document.createElement("tr");
        var cell4_1 = document.createElement("td");
        var cell4_2 = document.createElement("td");
        cell4_1.innerHTML = '<input type="text" name="vm' + vm + '-SRIOV' + i + '-ip" placeholder="IP Address"/>';
        cell4_2.innerHTML = '<input type="text" name="vm' + vm + '-SRIOV' + i + '-mac" placeholder="MAC Address"/>';
        row4.appendChild(cell4_1);
        row4.appendChild(cell4_2);
        tbody1.appendChild(row4);

        table.appendChild(tbody1);
        table.appendChild(tbody2);

        fieldset.appendChild(table);
    }
}

function updateSubnetNames(input) {
    var subnetId = input.id;
    var subnetNum = subnetId.substring(6, 7);

    $('[id$=subnet-select] option[value=' + subnetNum + ']').text(
            'Subnet ' + subnetNum + ' (' + input.value + ')');
}

function updateVMNames(input) {
    var vmId = input.id;
    var vmNum = vmId.substring(2, 3);

    $('[id$=vm-select] option[value=' + vmNum + ']').text(
            'VM ' + vmNum + ' (' + input.value + ')');
}

function updateGatewayNames(input) {
    var gatewayId = input.id;
    var gatewayNum = gatewayId.substring(7, 8);

    $('[id$=gateway-select] option[value=' + gatewayNum + ']').text(
            'Gateway ' + gatewayNum + ' (' + input.value + ')');
}

function validateHybrid() {
    var invalidArr = new Array();
    var type = $("#msform").attr('class');
    var btn = $(document.activeElement);
    if (btn.attr("name") === "save" && !($("input[name='profile-save']").is(':checked'))) {
        invalidArr.push("Profiles require a name in order to be saved. Please check the save box and try again.");
    }

    // Stage 2
    if ($("input[name='alias']").val() === "") {
        invalidArr.push("Alias field is empty.\n");

        $("#progressbar li").eq(1).addClass("invalid");
        $("input[name='alias']").addClass("invalid");
    }
    if ($("input[name='aws-conn-vlan']").val() === "") {
        invalidArr.push("Direct Connect VLAN field is empty.\n");

        $("#progressbar li").eq(1).addClass("invalid");
        $("input[name='aws-conn-vlan']").addClass("invalid");
    }
    if ($("select[name='aws-topoUri']").val() === null) {
        invalidArr.push("AWS Topology field is empty.\n");

        $("#progressbar li").eq(1).addClass("invalid");
        $("select[name='aws-topoUri']").addClass("invalid");
    }
    if ($("select[name='ops-topoUri']").val() === null) {
        invalidArr.push("Openstack Topology field is empty.\n");

        $("#progressbar li").eq(1).addClass("invalid");
        $("select[name='ops-topoUri']").addClass("invalid");
    }
    

    // Stage 3


    // Stage 4


    // Stage 5


    // Stage 6


    // Stage 7
    if ($("input[name='profile-save']").is(':checked')) {
        if ($("input[name='profile-name']").val() === "") {
            invalidArr.push("Profiles require a name in order to be saved.\n");

            $("#progressbar li").eq(6).addClass("invalid");
            $("input[name='profile-name']").addClass("invalid");
        }
    }

    // Results
    if (invalidArr.length === 0) {
        $('<input />').attr('type', 'hidden')
                .attr('name', "authToken")
                .attr('value', keycloak.token)
                .appendTo('#msform');
        $('<input />').attr('type', 'hidden')
                .attr('name', "refreshToken")
                .attr('value', keycloak.refreshToken)
                .appendTo('#msform');

        return true;
    } else {
        infoAlert("Invalid Inputs", invalidArr);

        return false;
    }
}

function infoAlert(title, arr) {
    $("#black-screen").removeClass("off");
    $("#info-panel").addClass("active");

    if (title === "Invalid Inputs") {
        $("#info-panel-title").html(title);
        var arrString = "";
        for (i = 0; i < arr.length; i++) {
            arrString += arr[i] + "\r\n";
        }

        $("#info-panel-fs").html(arrString);
    }
}

function loadKeycloakACL() {

}