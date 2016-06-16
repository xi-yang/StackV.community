var baseUrl = window.location.origin;

// Page Load Function
$(function () {
    $(".type-select").click(function () {
        if (animating)
            return false;
        animating = true;

        fieldset_id = '#2-' + this.value + '-start';

        current_fs = $(this).parent();
        next_fs = $(fieldset_id);

        //activate next step on progressbar using the index of next_fs
        //$("#progressbar li").eq($("fieldset").index(next_fs)).addClass("active");

        nextStage(current_fs, next_fs);
    });

    $(".field-next").click(function () {
        if (animating)
            return false;
        animating = true;

        current_fs = $(this).parent();
        next_fs = $(this).parent().next();

        var sub_count = document.getElementById(this.id + "-count1");
        var route_count = document.getElementById(this.id + "-count2");
        setStage3(this.id, sub_count.value, route_count.value);
        //activate next step on progressbar using the index of next_fs
        //$("#progressbar li").eq($("fieldset").index(next_fs)).addClass("active");

        nextStage(current_fs, next_fs);
    });

    $(".next").click(function () {
        if (animating)
            return false;
        animating = true;

        current_fs = $(this).parent();
        next_fs = $(this).parent().next();

        //activate next step on progressbar using the index of next_fs
        //$("#progressbar li").eq($("fieldset").index(next_fs)).addClass("active");

        nextStage(current_fs, next_fs);
    });

    $(".previous").click(function () {
        if (animating)
            return false;
        animating = true;

        current_fs = $(this).parent();
        previous_fs = $(this).parent().prev();

        //de-activate current step on progressbar
        //$("#progressbar li").eq($("fieldset").index(current_fs)).removeClass("active");

        previousStage(current_fs, previous_fs);
    });
    
    $(".reset").click(function () {
        if (animating)
            return false;
        animating = true;

        current_fs = $(this).parent();
        base_fs = $('#1-base-start');

        //de-activate current step on progressbar
        //$("#progressbar li").eq($("fieldset").index(current_fs)).removeClass("active");

        previousStage(current_fs, base_fs);
    });
    
    $(".subfs-headrow").click(function () {
        var head = $(this).parent();
        var body = head.next();
        
        body.toggleClass("hide");
    });
});

//jQuery time
var current_fs, next_fs, previous_fs; //fieldsets
var left, opacity, scale; //fieldset properties which we will animate
var animating; //flag to prevent quick multi-click glitches

function nextStage(current_fs, incoming_fs) {
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
    //show the next fieldset
    incoming_fs.show();
    //hide the current fieldset with style
    current_fs.animate({opacity: 0}, {
		step: function(now, mx) {
			//as the opacity of current_fs reduces to 0 - stored in "now"
			//1. scale previous_fs from 80% to 100%
			//scale = 0.8 + (1 - now) * 0.2;
			//2. take current_fs to the right(50%) - from 0%
			left = ((1-now) * 50)+"%";
			//3. increase opacity of previous_fs to 1 as it moves in
			opacity = 1 - now;
			current_fs.css({'left': left});
			incoming_fs.css({'transform': 'scale('+scale+')', 'opacity': opacity});
		}, 
		duration: 800, 
		complete: function(){
			current_fs.hide();
			animating = false;
		}, 
		//this comes from the custom easing plugin
		easing: 'easeInOutBack'
	});
}

function setStage3(stage, count1, count2) {    
    var fieldset = document.getElementById(stage + "-subfs");
    
    for (i = 1; i <= count1; i++) { 
        var table = document.createElement("table");
        table.className = 'subfs-table';
        
        var thead = document.createElement("thead");        
        var row1 = document.createElement("tr");
        row1.className = 'subfs-headrow';
        var cell1_1 = document.createElement("th");
        var cell1_2 = document.createElement("th");
        cell1_1.innerHTML = 'Subnet ' + i;
        row1.appendChild(cell1_1);
        row1.appendChild(cell1_2);
        thead.appendChild(row1);
        table.appendChild(thead);
        
        var row2 = table.insertRow(0);        
        var cell2_1 = row2.insertCell(0);
        var cell2_2 = row2.insertCell(1);
        cell2_1.innerHTML = '<input type="text" name="subnet' + i + '-name" placeholder="Name"/>';
        cell2_2.innerHTML = '<input type="text" name="subnet' + i + '-cidr" placeholder="CIDR Block"/>';
        
        for (j = 1; j <= count2; j++) {
            var row3 = table.insertRow(j);
            var cell3_1 = row3.insertCell(0);
            
            cell3_1.innerHTML = '<input type="text" name="subnet' + i + '-route' + j + '-from" placeholder="From"/>' +
                '<input type="text" name="subnet' + i + '-route' + j + '-to" placeholder="To"/>' + 
                '<input type="text" name="subnet' + i + '-route' + j + '-next" placeholder="Next Hop"/>';
        }
        
        
        fieldset.appendChild(table);
    }
}