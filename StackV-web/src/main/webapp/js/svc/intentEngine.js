/* 
 * Copyright (c) 2013-2017 University of Maryland
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

var conditionMap = {};
var triggerMap = {};
var intent;

loadIntent('dnc');
function loadIntent(type) {
    $.ajax({
        type: "GET",
        url: "/StackV-web/data/xml/" + type + ".xml",
        dataType: "xml",
        success: function (xml) {
            intent = xml.children[0];
            renderIntent();
        },
        error: function (err) {
            console.log('Error Loading XML! \n' + err);
        }
    });
}

function renderIntent() {
    var panel = $("#intent-panel-body");
    // Initialize meta sidebar
    var meta = intent.children[0];
    initMeta(meta);

    // Begin rendering stages
    var stages = intent.children;
    for (var i = 1; i < stages.length; i++) {
        // Initialize stage panel
        var stage = stages[i];
        verifyConditions(stage);
        var stageName = stage.attributes.getNamedItem("name").nodeValue;
        var div = $("<div>", {class: "intent-stage-div", id: stageName.toLowerCase()});
        panel.append(div);

        $currStagePanel = $("#" + stageName.toLowerCase());
        // Begin recursive rendering
        renderInputs(stage.children, $currStagePanel);
    }
}


// UTILITY FUNCTIONS
function initMeta(meta) {
    var panel = $("#intent-panel-meta");
    panel.append($("<div>", {html: meta.children[0].innerHTML, style: "margin-bottom:50px;"}));

    var blocks = meta.children;
    for (var i = 1; i < blocks.length; i++) {
        var $div = $("<div>", style = "margin-bottom:20px;");
        var block = blocks[i];
        var html = block.innerHTML;
        verifyConditions(block);

        var str = html.charAt(0).toUpperCase() + html.slice(1);
        var $label = $("<label>").text(str);
        var $input = $("<input>", {type: "number", id: "block-" + html, name: "meta-block-" + html, value: 0});
        $label.append($input);
        $div.append($label);

        panel.append($div);
    }

}

function renderInputs(arr, $parent) {
    for (var i = 0; i < arr.length; i++) {
        var ele = arr[i];
        verifyConditions(ele);
        if (ele.nodeName === "group") {
            var attr = ele.attributes;
            var name = attr.getNamedItem("name").nodeValue;
            var factoryAttr = attr.getNamedItem("factory");
            var collapsibleAttr = attr.getNamedItem("collapsible");
            var str = name.charAt(0).toUpperCase() + name.slice(1);

            var $div = $("<div>", {class: "intent-group-div"});
            $parent.append($div);

            // Handle potential element modifiers
            if (factoryAttr && factoryAttr.nodeValue === "true") {
                str += " 1";
            }
            var $name = $('<div class="group-header col-sm-12">' + str + "</div>");
            $div.append($name);
            
            if (collapsibleAttr && collapsibleAttr.nodeValue === "true") {
                var collapseStr = "group-" + str.replace(" ", "_");
                var $collapseDiv = $("<div>", {class: "collapse", id: collapseStr});
                
                var $toggle = $("<a>").attr("data-toggle", "collapse")
                        .attr("data-target", "#" + collapseStr)
                        .text("Toggle!");
                $name.append($toggle);
                
                $div = $collapseDiv;
            }                        

            // Recurse!
            renderInputs(ele.children, $div);
        } else if (ele.nodeName === "input") {
            var type = ele.children[1].innerHTML;
            var name = generateInputName(ele);

            var $label = $("<label>").text(ele.children[0].innerHTML);
            var $input = $("<input>", {type: type, id: name, name: name});
            // Handle potential element modifiers
            if (ele.getElementsByTagName("size").length > 0) {
                switch (ele.getElementsByTagName("size")[0].innerHTML) {
                    case "small":
                        $label.addClass("col-sm-3");
                        break;
                    case "medium":
                        $label.addClass("col-sm-6");
                        break;
                    case "large":
                        $label.addClass("col-sm-9");
                        break;
                }
            }
            if (ele.getElementsByTagName("default").length > 0) {
                $input.val(ele.getElementsByTagName("default")[0].innerHTML);
            }
            if (ele.getElementsByTagName("source").length > 0) {

            }
            if (ele.getElementsByTagName("options").length > 0) {

            }

            $label.append($input);
            $parent.append($label);
        }
    }
}


function verifyConditions(ele) {
    var attr = ele.attributes;
    var triggerAttr = attr.getNamedItem("trigger");
    var conditionAttr = attr.getNamedItem("condition");

    if (triggerAttr) {
        triggerMap[ele] = triggerAttr.nodeValue;
    } else if (conditionAttr) {
        conditionMap[ele] = conditionAttr.nodeValue;
    }
}

function generateInputName(ele) {
    var parent = ele.parentElement;
    var parentStr = parent.attributes.getNamedItem("name").nodeValue.toLowerCase().replace(" ", "_");
    var eleStr = ele.children[0].innerHTML.toLowerCase().replace(" ", "_");

    var retString = parentStr + "-" + eleStr;
    while (parent.nodeName !== "stage") {
        parent = parent.parentElement;
        parentStr = parent.attributes.getNamedItem("name").nodeValue.toLowerCase().replace(" ", "_");

        retString = parentStr + "-" + retString;
    }

    return retString;
}