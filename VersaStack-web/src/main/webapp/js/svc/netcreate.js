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

        configureProgress(this.value);

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
        }
        else if (next_index > curr_index) {
            nextStage(current_fs, next_fs);
        }
        else if (next_index < curr_index) {
            previousStage(current_fs, next_fs);
        }
    });
});

function configureProgress(type) {
    $("#progressbar li").removeClass("disabled");

    if (type === 'aws') {
        $("#progressbar li").eq(4).addClass("disabled");
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
    }
    else if (stage > stage_num) {
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
        mode_fs = $('#0-mode-select');
        nextStage(mode_fs, base_fs);
    }
    else {
        // Basic AWS Template
        if (mode === 1) {
            current_fs = $("#0-mode-select");
            next_fs = $("#2-aws-1");
            configureProgress('aws');

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
            current_fs = $("#0-mode-select");
            next_fs = $("#2-aws-1");
            configureProgress('aws');

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

            form.elements['subnet1-name'].value = '';
            form.elements['subnet1-cidr'].value = '10.1.0.0/24';

            form.elements['subnet1-route1-to'].value = '206.196.0.0/16';
            form.elements['subnet1-route1-next'].value = 'internet';
            form.elements['subnet1-route2-to'].value = '72.24.24.0/24';
            form.elements['subnet1-route2-next'].value = 'vpn';
            form.elements['subnet1-route-prop'].checked = true;

            form.elements['subnet2-name'].value = '';
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
    var row = table.insertRow(0);
    var cell = row.insertCell(0);
    cell.innerHTML = '<label><input type = "checkbox" name = "subnet' + subnetNum + '-route-prop" value = "true"> Enable VPN Routes Propagation</label>';


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
        cell1_1.innerHTML = 'VM ' + i;
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

        var selectString = '<select name="vm' + i + '-subnet"><option selected disabled required>Select the subnet host</option>';
        for (j = 1; j <= subnetCount; j++) {
            var subnetTag = document.getElementById("subnet" + j + "-tag");

            selectString += '<option value="' + j + '">Subnet ' + j + ' (' + subnetTag.value + ')</option>';
        }
        selectString += '</select>';

        cell2_1.innerHTML = '<td><input type="text" id="vm' + i + '-tag" name="vm' + i + '-name"></td>';
        cell2_2.innerHTML = selectString;
        row2.appendChild(cell2_1);
        row2.appendChild(cell2_2);
        tbody.appendChild(row2);

        var row3 = document.createElement("tr");
        var cell3_1 = document.createElement("td");
        var cell3_2 = document.createElement("td");
        cell3_1.innerHTML = '<input type="text" name="vm' + i + '-keypair" placeholder="Keypair Name">';
        cell3_2.innerHTML = '<input type="text" name="vm' + i + '-security" placeholder="Security Group">';
        row3.appendChild(cell3_1);
        row3.appendChild(cell3_2);
        tbody.appendChild(row3);

        var row4 = document.createElement("tr");
        var cell4_1 = document.createElement("td");
        var cell4_2 = document.createElement("td");
        cell4_1.innerHTML = '<input type="text" name="vm' + i + '-image" placeholder="Image Type">';
        cell4_2.innerHTML = '<input type="text" name="vm' + i + '-instance" placeholder="Instance Type">';
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
        cell1_1.innerHTML = 'Gateway ' + i;
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
        
        var selectString = '<select name="SRIOV' + i + '-vm"><option selected disabled required>Select the SRIOV host</option>';
        for (j = 1; j <= vmCount; j++) {
            var vmTag = document.getElementById("vm" + j + "-tag");

            selectString += '<option value="' + j + '">VM ' + j + ' (' + vmTag.value + ')</option>';
        }
        selectString += '</select>';

        var row2 = document.createElement("tr");
        var cell2_1 = document.createElement("td");
        var cell2_2 = document.createElement("td");
        cell2_1.innerHTML = '<input type="text" name="SRIOV' + i + '-gateway" id="SRIOV' + i + '-tag" onchange="updateVMNames(this)" placeholder="Name"/>';
        cell2_2.innerHTML = selectString;
        row2.appendChild(cell2_1);
        row2.appendChild(cell2_2);
        tbody1.appendChild(row2);
        
        var row3 = document.createElement("tr");
        var cell3_1 = document.createElement("td");
        var cell3_2 = document.createElement("td");
        cell3_1.innerHTML = '<input type="text" name="SRIOV' + i + '-mac" id="SRIOV' + i + '-tag" onchange="updateVMNames(this)" placeholder="Name"/>';
        cell3_2.innerHTML = '<input type="text" name="SRIOV' + i + '-ip" placeholder="IP Address"/>';
        row3.appendChild(cell3_1);
        row3.appendChild(cell3_2);
        tbody1.appendChild(row3);
                
        table.appendChild(tbody1);
        table.appendChild(tbody2);

        fieldset.appendChild(table);

        // Set inputs for subnet routes
        var row = subTable.insertRow(i - 1);
        var cell = row.insertCell(0);

        cell.innerHTML = '<div class="fs-subtext">How many routes for SRIOV ' + i + '?   ' +
                '<input type="number" class="small-counter" id="' + stage + i + '-routes" ' +
                'onfocus="this.oldvalue = this.value;" ' +
                'onchange="setSRIOVRoutes(this)" /></div>';
    }
}

function setSRIOVRoutes(input) {
    // Grab correct SRIOV table
    var SRIOVId = input.id.substring(0, input.id.length - 7);
    var table = document.getElementById(SRIOVId + '-table').getElementsByTagName('tbody')[1];
    var SRIOVNum = SRIOVId.substring(SRIOVId.length - 1);
    table.innerHTML = "";

    var SRIOVRouteCount = input.value;
    for (j = 1; j <= SRIOVRouteCount; j++) {
        var row3 = table.insertRow(j - 1);
        var cell3_1 = row3.insertCell(0);

        cell3_1.innerHTML = '<input type="text" name="SRIOV' + SRIOVNum + '-route' + j + '-from" placeholder="From"/>' +
                '<input type="text" name="SRIOV' + SRIOVNum + '-route' + j + '-to" placeholder="To"/>' +
                '<input type="text" name="SRIOV' + SRIOVNum + '-route' + j + '-next" placeholder="Next Hop"/>';
    }
}

function updateSubnetNames(input) {
    var subnetId = input.id;
    var subnetNum = subnetId.substring(6, 7);

    $('[id^=4] select option[value=' + subnetNum + ']').text(
            'Subnet ' + subnetNum + ' (' + input.value + ')');
}