/* global keycloak */

var baseUrl = window.location.origin;

//jQuery time
var current_fs, next_fs, previous_fs; //fieldsets
var left, opacity, scale; //fieldset properties which we will animate
var animating; //flag to prevent quick multi-click glitches
var stage = 1;
var last_stage = 6;

// Page Load Function
$(function () {
    $(".stage1-next").click(function () {
        if (animating)
            return false;
        animating = true;

        fieldset_id = '#2-' + this.value + '-1';

        var form = document.getElementById('msform');
        if (this.value === 'aws') {

        } else {

        }

        current_fs = $(this).parent();
        next_fs = $(fieldset_id);

        configureForm(this.value);

        nextStage(current_fs, next_fs);
    });

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

        current_fs = $(this).parent();
        base_fs = $('#1-base-1');
        previousStage(current_fs, base_fs);
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
        if (animating || $(this).hasClass('disabled'))
            return false;
        animating = true;

        var curr_id = $(".active-fs").attr('id');
        var curr_index = curr_id.charAt(0);
        current_fs = $("#" + curr_id);

        var next_index = $("#progressbar li").index(this) + 1;
        var next_superid = next_index + curr_id.substring(1, 5);

        var next_fs_list = $("[id^=" + next_superid + "]");
        if (next_fs_list.last().children().first().children().length === 0) {
            next_fs = next_fs_list.first();
        } else {
            next_fs = next_fs_list.last();
        }

        if (next_index === 1) {
            resetStages();

            base_fs = $('#1-base-1');
            previousStage(current_fs, base_fs);
        } else if (next_index > curr_index) {
            nextStage(current_fs, next_fs);
        } else if (next_index < curr_index) {
            previousStage(current_fs, next_fs);
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

function configureForm(type) {
    $("#progressbar li").removeClass("disabled");

    var thead = document.getElementById(type + "Stage2-base");
    thead.innerHTML = "";
    var tbody = document.getElementById(type + "Stage2-network");
    tbody.innerHTML = "";

    var row1 = document.createElement("tr");
    var cell1_1 = document.createElement("td");
    var cell1_2 = document.createElement("td");
    cell1_1.innerHTML = '<input type="text" name="netType" placeholder="Network Type" />';
    cell1_2.innerHTML = '<input type="text" name="netCidr" placeholder="Network CIDR" />';
    row1.appendChild(cell1_1);
    row1.appendChild(cell1_2);
    tbody.appendChild(row1);

    var row2 = document.createElement("tr");
    var cell2_1 = document.createElement("td");
    cell2_1.innerHTML = '<input type="text" name="alias" placeholder="Instance Alias" />';
    row2.appendChild(cell2_1);
    thead.appendChild(row2);

    if (type === 'aws') {
        $("#msform").addClass("aws");

        $("#progressbar li").eq(4).addClass("disabled");
        $("#progressbar li").eq(5).addClass("disabled");

        var arow2 = document.createElement("tr");
        var acell2_1 = document.createElement("td");
        var acell2_2 = document.createElement("td");
        acell2_1.innerHTML = '<input type="text" name="conn-dest" placeholder="Direct Connect Destination" />';
        acell2_2.innerHTML = '<input type="text" name="conn-vlan" placeholder="Direct Connect VLAN" />';
        arow2.appendChild(acell2_1);
        arow2.appendChild(acell2_2);
        tbody.appendChild(arow2);
    } else {
        $("#msform").addClass("ops");
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
        },
        //this comes from the custom easing plugin
        easing: 'easeInOutBack'
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
        },
        //this comes from the custom easing plugin
        easing: 'easeInOutBack'
    });
}

function applyTemplate(mode) {
    var form = document.getElementById('msform');
    $("#black-screen").addClass("off");
    if (animating)
        return false;
    animating = true;

    if (mode === 0) {
        base_fs = $('#1-base-1');
        mode_fs = $('#0-template-select');
        nextStage(mode_fs, base_fs);
    } else {
        // Basic AWS Template
        if (mode === 1) {
            current_fs = $("#0-template-select");
            next_fs = $("#2-aws-1");
            configureForm('aws');

            form.elements['netType'].value = 'internal';
            form.elements['netCidr'].value = '10.1.0.0/16';

            var subnetCounter = document.getElementById('awsStage3-subnet');
            subnetCounter.value = 2;
            setSubnets(subnetCounter);

            var sub1RouteCounter = document.getElementById('awsStage3-subnet1-routes');
            sub1RouteCounter.value = 2;
            setSubRoutes(sub1RouteCounter);

            form.elements['subnet1-name'].value = '';
            form.elements['subnet1-cidr'].value = '10.1.0.0/24';
            form.elements['subnet1-route1-to'].value = '206.196.0.0/16';
            form.elements['subnet1-route1-next'].value = 'internet';
            form.elements['subnet1-route2-to'].value = '72.24.24.0/24';
            form.elements['subnet1-route2-next'].value = 'vpn';
            form.elements['subnet1-route-prop'].checked = true;

            form.elements['subnet2-name'].value = '';
            form.elements['subnet2-cidr'].value = '10.1.1.0/24';

            form.elements['conn-dest'].value = 'urn:ogf:network:domain=dragon.maxgigapop.net:node=CLPK:port=1-1-2:link=*';
            form.elements['conn-vlan'].value = 'any';
        }
        // AWS w/ VMs Template
        else if (mode === 2) {
            current_fs = $("#0-template-select");
            next_fs = $("#2-aws-1");
            configureForm('aws');

            form.elements['netType'].value = 'internal';
            form.elements['netCidr'].value = '10.1.0.0/16';

            var subnetCounter = document.getElementById('awsStage3-subnet');
            subnetCounter.value = 2;
            setSubnets(subnetCounter);

            var sub1RouteCounter = document.getElementById('awsStage3-subnet1-routes');
            sub1RouteCounter.value = 2;
            setSubRoutes(sub1RouteCounter);

            var vmCounter = document.getElementById('awsStage4-vm');
            vmCounter.value = 2;
            setVMs(vmCounter);

            form.elements['subnet1-name'].value = 'subnet1';
            form.elements['subnet1-cidr'].value = '10.1.0.0/24';

            form.elements['subnet1-route1-to'].value = '206.196.0.0/16';
            form.elements['subnet1-route1-next'].value = 'internet';
            form.elements['subnet1-route2-to'].value = '72.24.24.0/24';
            form.elements['subnet1-route2-next'].value = 'vpn';
            form.elements['subnet1-route-prop'].checked = true;

            form.elements['subnet2-name'].value = 'subnet2';
            form.elements['subnet2-cidr'].value = '10.1.1.0/24';

            form.elements['vm1-name'].value = 'test_with_vm_types_1';
            $("#awsStage4-vm1-table select").val("1");
            form.elements['vm1-image'].value = 'ami-08111162';
            form.elements['vm1-instance'].value = 't2.micro';

            form.elements['vm2-name'].value = 'test_with_vm_types_2';
            $("#awsStage4-vm2-table select").val("2");
            form.elements['vm2-image'].value = 'ami-fce3c696';
            form.elements['vm2-instance'].value = 't2.small';
            form.elements['vm2-keypair'].value = 'xi-aws-max-dev-key';
            form.elements['vm2-security'].value = 'geni';

            form.elements['conn-dest'].value = 'urn:publicid:IDN+dragon.maxgigapop.net+interface+CLPK:1-1-2:*';
            form.elements['conn-vlan'].value = '3023';
        }
        // Basic OPS Template
        else if (mode === 3) {
            current_fs = $("#0-template-select");
            next_fs = $("#2-ops-1");
            configureForm('ops');

            // Network
            form.elements['netType'].value = 'internal';
            form.elements['netCidr'].value = '10.0.0.0/16';

            // Subnets
            var subnetCounter = document.getElementById('opsStage3-subnet');
            subnetCounter.value = 1;
            setSubnets(subnetCounter);

            var sub1RouteCounter = document.getElementById('opsStage3-subnet1-routes');
            sub1RouteCounter.value = 1;
            setSubRoutes(sub1RouteCounter);

            form.elements['subnet1-name'].value = 'subnet1';
            form.elements['subnet1-cidr'].value = '10.0.0.0/24';
            form.elements['subnet1-route-default'].checked = true;

            // VMs
            var vmCounter = document.getElementById('opsStage4-vm');
            vmCounter.value = 1;
            setVMs(vmCounter);

            var vm1RouteCounter = document.getElementById('opsStage4-vm1-routes');
            vm1RouteCounter.value = 1;
            setVMRoutes(vm1RouteCounter);

            form.elements['vm1-name'].value = 'ops-vtn1-vm1';
            $("#opsStage4-vm1-table select").val("1");
            form.elements['vm1-instance'].value = '2';
            form.elements['vm1-keypair'].value = 'icecube_key';
            form.elements['vm1-security'].value = 'rains';
            form.elements['vm1-floating'].value = '206.196.180.148';
            form.elements['vm1-host'].value = 'msx3';
            form.elements['vm1-route1-to'].value = '192.168.1.0/24';
            form.elements['vm1-route1-next'].value = '192.168.1.1';

            // Gateways    
            var gatewayCounter = document.getElementById('opsStage5-gateway');
            gatewayCounter.value = 2;
            setGateways(gatewayCounter);

            form.elements['gateway1-name'].value = 'cluster-gw1';
            $("#gateway1-type-select").val("port_profile");
            form.elements['gateway1-from'].value = 'MSX-Date-Local';
            form.elements['gateway2-name'].value = 'l2path-aws-dc1';
            $("#gateway2-type-select").val("stitch_port");
            form.elements['gateway2-to'].value = 'urn:ogf:network:domain=wix.internet2.edu:node=sw.net.wix.internet2.edu:port=13/1:link=al2s?vlan=any';

            // SRIOVs
            var SRIOVCounter = document.getElementById('opsStage6-sriov');
            SRIOVCounter.value = 2;
            setSRIOV(SRIOVCounter);
            $("#SRIOV1-vm-select").val("1");
            $("#SRIOV1-gateway-select").val("1");
            form.elements['SRIOV1-name'].value = 'ops-vtn1-vm1';
            form.elements['SRIOV1-ip'].value = '192.168.1.2';
            form.elements['SRIOV1-mac'].value = '11:22:22:33:33:01';
            $("#SRIOV2-vm-select").val("1");
            $("#SRIOV2-gateway-select").val("2");
            form.elements['SRIOV2-name'].value = 'ops-vtn1:vm1:eth2';
            form.elements['SRIOV2-ip'].value = '10.10.0.1';
            form.elements['SRIOV2-mac'].value = '11:22:22:33:33:02';

        }

        nextStage(current_fs, next_fs);
    }
}

var subnetCount;
function setSubnets(input) {
    subnetCount = input.value;

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
        cell1_1.innerHTML = 'Subnet ' + i;
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
        cell2_1.innerHTML = '<input type="text" name="subnet' + i + '-name" id="subnet' + i + '-tag" onchange="updateSubnetNames(this)" placeholder="Name"/>';
        cell2_2.innerHTML = '<input type="text" name="subnet' + i + '-cidr" placeholder="CIDR Block"/>';
        row2.appendChild(cell2_1);
        row2.appendChild(cell2_2);
        tbody1.appendChild(row2);

        var row3 = document.createElement("tr");
        var cell3_1 = document.createElement("td");
        if (stage.substring(0, 3) === 'aws') {
            cell3_1.innerHTML = '<label><input type = "checkbox" name = "subnet' + i + '-route-prop" value = "true"> Enable VPN Routes Propagation</label>';
        } else if (stage.substring(0, 3) === 'ops') {
            cell3_1.innerHTML = '<label><input type = "checkbox" name = "subnet' + i + '-route-default" value = "true"> Enable Default Routing</label>';
        }
        row3.appendChild(cell3_1);
        tbody1.appendChild(row3);

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
    var subnetId = input.id.substring(0, input.id.length - 7);
    var table = document.getElementById(subnetId + '-table').getElementsByTagName('tbody')[1];
    var subnetNum = subnetId.substring(subnetId.length - 1);
    table.innerHTML = "";

    var subRouteCount = input.value;
    for (j = 1; j <= subRouteCount; j++) {
        var row3 = table.insertRow(j - 1);
        var cell3_1 = row3.insertCell(0);

        cell3_1.innerHTML = '<input type="text" name="subnet' + subnetNum + '-route' + j + '-from" placeholder="From"/>' +
                '<input type="text" name="subnet' + subnetNum + '-route' + j + '-to" placeholder="To"/>' +
                '<input type="text" name="subnet' + subnetNum + '-route' + j + '-next" placeholder="Next Hop"/>';
    }
}

var vmCount;
function setVMs(input) {
    vmCount = input.value;

    var stage = input.id;
    var old = input.oldvalue;
    var fieldset = document.getElementById(stage + "-fs");
    var vmTable = document.getElementById(stage + "-route-table");
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

            $(this).toggleClass("closed");
            body1.toggleClass("fade-hide");
            body2.toggleClass("fade-hide");
        });

        var row2 = document.createElement("tr");
        var cell2_1 = document.createElement("td");
        var cell2_2 = document.createElement("td");

        var selectString = '<select name="vm' + i + '-subnet" id="vm' + i + '-subnet-select"><option selected disabled>Select the subnet host</option>';
        for (j = 1; j <= subnetCount; j++) {
            var subnetTag = document.getElementById("subnet" + j + "-tag");

            selectString += '<option value="' + j + '">Subnet ' + j + ' (' + subnetTag.value + ')</option>';
        }
        selectString += '</select>';

        cell2_1.innerHTML = '<td><input type="text" id="vm' + i + '-tag" onchange="updateVMNames(this)" name="vm' + i + '-name" placeholder="Name"></td>';
        cell2_2.innerHTML = selectString;
        row2.appendChild(cell2_1);
        row2.appendChild(cell2_2);
        tbody1.appendChild(row2);

        var row3 = document.createElement("tr");
        var cell3_1 = document.createElement("td");
        var cell3_2 = document.createElement("td");
        cell3_1.innerHTML = '<input type="text" name="vm' + i + '-keypair" placeholder="Keypair Name">';
        cell3_2.innerHTML = '<input type="text" name="vm' + i + '-security" placeholder="Security Group">';
        row3.appendChild(cell3_1);
        row3.appendChild(cell3_2);
        tbody1.appendChild(row3);

        var row4 = document.createElement("tr");
        var cell4_1 = document.createElement("td");
        var cell4_2 = document.createElement("td");
        cell4_1.innerHTML = '<input type="text" name="vm' + i + '-image" placeholder="Image Type">';
        cell4_2.innerHTML = '<input type="text" name="vm' + i + '-instance" placeholder="Instance Type">';
        row4.appendChild(cell4_1);
        row4.appendChild(cell4_2);
        tbody1.appendChild(row4);

        if (stage.substring(0, 3) === 'ops') {
            var row5 = document.createElement("tr");
            var cell5_1 = document.createElement("td");
            var cell5_2 = document.createElement("td");
            cell5_1.innerHTML = '<input type="text" name="vm' + i + '-host" placeholder="Host">';
            cell5_2.innerHTML = '<input type="text" name="vm' + i + '-floating" placeholder="Floating IP">';
            row5.appendChild(cell5_1);
            row5.appendChild(cell5_2);
            tbody1.appendChild(row5);
        }

        table.appendChild(tbody1);
        table.appendChild(tbody2);
        fieldset.appendChild(table);

        // Set inputs for vm routes
        var row = vmTable.insertRow(i - 1);
        var cell = row.insertCell(0);

        cell.innerHTML = '<div class="fs-subtext">How many routes for VM ' + i + '?   ' +
                '<input type="number" class="small-counter" id="' + stage + i + '-routes" ' +
                'onfocus="this.oldvalue = this.value;" ' +
                'onchange="setVMRoutes(this)" /></div>';
    }
}

function setVMRoutes(input) {
    // Grab correct vm table
    var vmId = input.id.substring(0, input.id.length - 7);
    var table = document.getElementById(vmId + '-table').getElementsByTagName('tbody')[1];
    var vmNum = vmId.substring(vmId.length - 1);
    table.innerHTML = "";

    var vmRouteCount = input.value;
    for (j = 1; j <= vmRouteCount; j++) {
        var row3 = table.insertRow(j - 1);
        var cell3_1 = row3.insertCell(0);

        cell3_1.innerHTML = '<input type="text" name="vm' + vmNum + '-route' + j + '-from" placeholder="From"/>' +
                '<input type="text" name="vm' + vmNum + '-route' + j + '-to" placeholder="To"/>' +
                '<input type="text" name="vm' + vmNum + '-route' + j + '-next" placeholder="Next Hop"/>';
    }
}

var gatewayCount;
function setGateways(input) {
    gatewayCount = input.value;

    var stage = input.id;
    var old = input.oldvalue;
    var fieldset = document.getElementById(stage + "-fs");

    fieldset.innerHTML = "";

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
                + '<option value="port_profile">UCS Port Profile</option>'
                + '<option value="stitch_port">L2 Stitch Port</option></select>';
        row2.appendChild(cell2_1);
        row2.appendChild(cell2_2);
        tbody.appendChild(row2);

        var row4 = document.createElement("tr");
        var cell4_1 = document.createElement("td");
        var cell4_2 = document.createElement("td");
        cell4_1.innerHTML = '<input type="text" name="gateway' + i + '-from" placeholder="From"/>' +
                '<input type="text" name="gateway' + i + '-to" placeholder="To"/>' +
                '<input type="text" name="gateway' + i + '-next" placeholder="Next Hop"/>';
        row4.appendChild(cell4_1);
        row4.appendChild(cell4_2);
        tbody.appendChild(row4);

        table.appendChild(tbody);
        fieldset.appendChild(table);
    }
}

var SRIOVCount;
function setSRIOV(input) {
    SRIOVCount = input.value;

    var stage = input.id;
    var old = input.oldvalue;
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
        cell1_1.innerHTML = 'SRIOV ' + i;
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

        var selectString1 = '<select name="SRIOV' + i + '-gateway" id="SRIOV' + i + '-gateway-select"><option selected disabled>Select the hosting Gateway</option>';
        for (j = 1; j <= gatewayCount; j++) {
            var gatewayTag = document.getElementById("gateway" + j + "-tag");

            selectString1 += '<option value="' + j + '">Gateway ' + j + ' (' + gatewayTag.value + ')</option>';
        }
        selectString1 += '</select>';

        var selectString2 = '<select name="SRIOV' + i + '-vm" id="SRIOV' + i + '-vm-select"><option selected disabled>Select the hosting VM</option>';
        for (j = 1; j <= vmCount; j++) {
            var vmTag = document.getElementById("vm" + j + "-tag");

            selectString2 += '<option value="' + j + '">VM ' + j + ' (' + vmTag.value + ')</option>';
        }
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
        cell3_1.innerHTML = '<input type="text" name="SRIOV' + i + '-name" id="SRIOV' + i + '-tag" placeholder="Name"/>';
        row3.appendChild(cell3_1);
        row3.appendChild(cell3_2);
        tbody1.appendChild(row3);

        var row4 = document.createElement("tr");
        var cell4_1 = document.createElement("td");
        var cell4_2 = document.createElement("td");
        cell4_1.innerHTML = '<input type="text" name="SRIOV' + i + '-ip" placeholder="IP Address"/>';
        cell4_2.innerHTML = '<input type="text" name="SRIOV' + i + '-mac" placeholder="MAC Address"/>';
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

function validateVCN() {
    var invalidArr = new Array();
    var type = $("#msform").attr('class');

    // Stage 2
    if ($("input[name='alias']").val() === "") {
        invalidArr.push("Alias field is empty.\n");

        $("#progressbar li").eq(1).addClass("invalid");
        $("input[name='alias']").addClass("invalid");
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

        $("#info-panel-div").html(arrString);
    }
}