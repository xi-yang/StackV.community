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
var factoryMap = {};
var intent;
var transit = false;
var activeStage;

loadIntent('netcreate');
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
        var $div = $("<div>", {class: "intent-stage-div", id: stageName.toLowerCase()});
        if (i === 1) {
            $div.addClass("active");
            $activeStage = $div;
        }
        panel.append($div);
        $currentStageDiv = $div;

        $div = $("<div>", {class: "intent-stage-factory", id: stageName.toLowerCase() + "-factory"});
        $("#intent-panel-header").append($div);
        // Begin recursive rendering
        renderInputs(stage.children, $currentStageDiv);
    }
}


// UTILITY FUNCTIONS
function initMeta(meta) {
    // Render service tag
    var $panel = $("#intent-panel-meta");
    $panel.append($("<div>", {html: meta.children[0].innerHTML, style: "margin-bottom:50px;height:50px;"}));

    // Render blocks
    var $blockDiv = $("<div>").attr("id", "intent-panel-meta-block");
    var blocks = meta.children;
    for (var i = 1; i < blocks.length; i++) {
        var $div = $("<div>", style = "margin-bottom:20px;");
        var block = blocks[i];
        var html = block.innerHTML;
        verifyConditions(block);

        var str = html.charAt(0).toUpperCase() + html.slice(1);
        var $label = $("<label>").text(str);
        var $input = $("<input>", {type: "number", id: "block-" + html, name: "meta-block-" + html, value: 1});
        $label.append($input);
        $div.append($label);

        $blockDiv.append($div);
    }
    $panel.append($blockDiv);

    // Render control buttons
    var $controlDiv = $("<div>").attr("id", "intent-panel-meta-control");
    $controlDiv.append($("<button>", {id: "intent-submit", html: "Submit"}));
    $panel.append($controlDiv);
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

            var $div = $("<div>", {class: "intent-group-div", id: name});
            $parent.append($div);
            var $name = $('<div class="group-header col-sm-12">' + str + "</div>");
            $div.append($name);

            // Handle potential element modifiers            
            var $targetDiv = $div;
            if (collapsibleAttr && collapsibleAttr.nodeValue === "true") {
                $targetDiv = collapseDiv($name, $div);
            }
            if (factoryAttr && factoryAttr.nodeValue === "true") {
                // Grab current index
                if (!(name in factoryMap)) {
                    factoryMap[name] = 1;

                    var $button = $("<button>")
                            .addClass("button-factory")
                            .attr("data-stage", $currentStageDiv.attr("id"))
                            .attr("data-subject", name)
                            .attr("data-target", $div.attr("id"))
                            .text("Add New " + str);

                    $button.click(function () {
                        if (!transit) {
                            // Ensure rapid clicks aren't processed
                            transit = true;

                            // Update map and retrieve target div
                            factoryMap[name] = factoryMap[name] + 1;
                            var $target = $("#" + $(this).attr("data-target")).parent();

                            // Grab element schema
                            var subject = $(this).attr("data-subject");
                            var coll = intent.getElementsByTagName("group");
                            var arr = [];
                            for (i = 0; i < coll.length; i++) {
                                if (coll[i].getAttribute("name") === subject) {
                                    arr.push(coll[i]);
                                    break;
                                }
                            }

                            renderInputs(arr, $target);

                            transit = false;
                        }
                    });

                    if ($button.data("stage") === $activeStage.attr("id")) {
                        $button.addClass("active");
                    }

                    $(".intent-stage-factory").append($button);
                }

                var index = factoryMap[name];

                // Start with names
                var $header = $div.children(".group-header");
                $header.html($header.text() + " " + index + $header.html().substring($header.text().length));

                // Look for collapse
                $div.children(".collapse").attr("id", "collapse-" + name + "_" + index);
                $header.children(".group-collapse-toggle").attr("data-target", "#collapse-" + name + "_" + index);
            }

            // Recurse!
            renderInputs(ele.children, $targetDiv);
        } else if (ele.nodeName === "input") {
            var type = ele.children[1].innerHTML;
            var name = generateInputName(ele);

            var $label = $("<label>").text(ele.children[0].innerHTML);
            var $input = $("<input>", {type: type, id: name, name: name});
            switch (type) {
                case "button":
                    $input.click(function (e) {
                        
                        
                        e.preventDefault();
                    });
                    break;
            }

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



// UTILITY FUNCTIONS

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
    var parentStr = parent.getAttribute("name").toLowerCase().replace(" ", "_");
    var eleStr = ele.children[0].innerHTML.toLowerCase().replace(" ", "_");

    var retString = eleStr;
    while (parent.nodeName !== "stage") {
        if (parentStr in factoryMap) {
            retString = parentStr + "_" + factoryMap[parentStr] + "-" + retString;
        } else {
            retString = parentStr + "-" + retString;
        }
        parent = parent.parentElement;
        parentStr = parent.getAttribute("name").toLowerCase().replace(" ", "_");
    }
    return parentStr + "-" + retString;
}

function collapseDiv($name, $div) {
    var name = $name.html().toLowerCase();
    var collapseStr = "collapse-" + name.replace(" ", "_");
    var $toggle = $("<a>").attr("data-toggle", "collapse")
            .attr("data-target", "#" + collapseStr)
            .addClass("group-collapse-toggle");

    if (name in factoryMap) {
        var $collapseDiv = $("<div>", {class: "collapse", id: collapseStr});
        $toggle.addClass("collapsed");
    } else {
        var $collapseDiv = $("<div>", {class: "collapse in", id: collapseStr});
    }

    $name.append($toggle);

    $div.append($collapseDiv);
    return $collapseDiv;
}