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

/* global Mousetrap */

var conditions = [];
var factories = {};
var intent;
var transit = false;
var activeStage;

Mousetrap.bind({
    'left': function () {
        prevStage();
    },
    'right': function () {
        nextStage();
    }
});

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
    // Stage 1: Initialization                
    initializeIntent();

    // Stage 2: Factorization
    factorizeRendering();
}

function initializeIntent() {
    var panel = $("#intent-panel-body");
    // Initialize meta sidebar
    var meta = intent.children[0];
    initMeta(meta);

    // Begin rendering stages
    var stages = intent.children;
    for (var i = 1; i < stages.length; i++) {

        // Initialize stage panel
        var stage = stages[i];
        var $div = $("<div>", {class: "intent-stage-div", id: constructID(stage)});
        if (i === 1) {
            $div.addClass("active");
            $activeStage = $div;
        }
        if (stage.getAttribute("condition")) {
            $div.addClass("conditional");
            $div.attr("data-condition", stage.getAttribute("condition"));
        }
        panel.append($div);
        $currentStageDiv = $div;

        $div = $("<div>", {class: "intent-stage-factory", id: "factory-" + constructID(stage)});
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

        var str = html.charAt(0).toUpperCase() + html.slice(1);
        var $label = $("<label>").text(str);
        var $input = $("<input>", {type: "number", id: "block-" + html, name: "block-" + html, value: 1});
        $label.append($input);
        $div.append($label);

        $blockDiv.append($div);
    }
    $panel.append($blockDiv);

    // Render control buttons
    var $controlDiv = $("<div>").attr("id", "intent-panel-meta-control");
    $controlDiv.append($("<button>", {class: "button-control active", id: "intent-submit", html: "Submit"}));
    $controlDiv.append($("<button>", {class: "button-control active", id: "intent-prev", html: "Prev"}));
    $controlDiv.append($("<button>", {class: "button-control active", id: "intent-next", html: "Next"}));
    $panel.append($controlDiv);

    $("#intent-prev").click(function () {
        prevStage();
    });
    $("#intent-next").click(function () {
        nextStage();
    });
}

function renderInputs(arr, $parent) {
    for (var i = 0; i < arr.length; i++) {
        var ele = arr[i];
        if (ele.nodeName === "group") {
            var name = ele.getAttribute("name");
            var factory = ele.getAttribute("factory");
            var condition = ele.getAttribute("condition");
            var collapsible = ele.getAttribute("collapsible");
            var block = ele.getAttribute("block");
            var str = name.charAt(0).toUpperCase() + name.slice(1);

            var $div = $("<div>", {class: "intent-group-div", id: constructID(ele)});
            $parent.append($div);
            var $name = $('<div class="group-header col-sm-12"><div class="group-name">' + str + "</div></div>");
            $div.append($name);

            // Handle potential element modifiers            
            var $targetDiv = $div;
            if (collapsible === "true") {
                $targetDiv = collapseDiv($name, $div);
            }
            if (factory === "true" || block) {
                $div.addClass("factory");
                var factObj = {};
                factObj["count"] = 1;
                factories[constructID(ele)] = factObj;
            }
            if (condition) {
                $div.addClass("conditional");
                $div.attr("data-condition", condition);
            }

            // Recurse!
            renderInputs(ele.children, $targetDiv);
        } else if (ele.nodeName === "input") {
            var type = ele.children[1].innerHTML;
            var name = constructID(ele);
            var trigger = ele.getAttribute("trigger");
            var condition = ele.getAttribute("condition");

            var $label = $("<label>").text(ele.children[0].innerHTML);
            var $input = $("<input>", {type: type, id: name, name: name});
            switch (type) {
                case "button":
                    $input.click(function (e) {
                        nextStage();

                        e.preventDefault();
                    });
                    $input.val("Select");
                    break;
            }

            // Handle potential element modifiers
            if (ele.getElementsByTagName("size").length > 0) {
                switch (ele.getElementsByTagName("size")[0].innerHTML) {
                    case "small":
                        $label.addClass("col-sm-4");
                        break;
                    case "large":
                        $label.addClass("col-sm-8");
                        break;
                    default:
                        $label.addClass("col-sm-6");
                }
            } else {
                $label.addClass("col-sm-6");
            }

            if (ele.getElementsByTagName("default").length > 0) {
                $input.val(ele.getElementsByTagName("default")[0].innerHTML);
            }
            if (ele.getElementsByTagName("source").length > 0) {

            }
            if (ele.getElementsByTagName("options").length > 0) {

            }
            if (trigger) {
                switch (type) {
                    case "text":
                        break;
                    case "button":
                        $label.attr("data-trigger", trigger);
                        $label.click(function () {
                            $("[data-condition='" + $(this).data("trigger") + "']").addClass("conditioned");
                            conditions.push($(this).data("trigger"));
                        });
                        break;
                }
            }
            if (condition) {
                $label.addClass("conditional");
                $label.attr("data-condition", condition);
            }

            $label.append($input);
            $parent.append($label);
        }
    }
}

function factorizeRendering() {
    // Step 1: Recursively reformat elements
    var factoryArr = $("#intent-panel-body").find(".factory");
    for (var i = 0; i < factoryArr.length; i++) {
        var fact = factoryArr[i];
        var id = fact.id;
        recursivelyFactor(id, fact);
    }

    // Step 2: Check for collapsible elements
    var collapseArr = $("#intent-panel-body").find(".group-collapse-toggle");
    for (var i = 0; i < collapseArr.length; i++) {
        var toggle = collapseArr[i];
        toggle.setAttribute("data-target", toggle.getAttribute("data-target") + "_num1");

        var collapse = toggle.parentElement.nextSibling;
        collapse.id += "_num1";
    }

    // Step 3: Insert user elements
    for (var i = 0; i < factoryArr.length; i++) {
        var fact = factoryArr[i];
        var id = fact.id;
        var head = fact.children[0];
        
        var $button = $("<button>", {class: "intent-button-factory", text: "Add " + name});
        $button.attr("data-factory", id);
        $button.attr("data-target", fact.parentElement.attr("id"));
        $button.click(function (e) {
            // Modify clone for current index
            var key = $(this).data("factory");
            var target = $(this).data("target");
            var count = ++factories[key]["count"];
            var $clone = factories[key]["clone"].clone(true);

            

            $("#" + target).append($clone);

            e.preventDefault();
        });
        $(head).append($button);
    }

    if (false) {
        var $ele = $("#" + key);
        var newID = key + "_num1";

        // Step 1: Reformat static elements with numbering:              
        //      * Names
        $ele.attr("id", newID);
        var $header = $ele.children(".group-header");
        var name = $header.children(".group-name").text();
        $header.children(".group-name").text(name + " #1");

        //      * Collapsible (Optional)       
        var target = $header.children(".group-collapse-toggle").data("target");
        $header.children(".group-collapse-toggle").attr("data-target", target + "_num1");
        $ele.children(".collapse").attr("id", $ele.children(".collapse").attr("id") + "_num1");

        //      * Children
        var $body = $ele.children();
        if (target) {
            $body = $ele.children(".collapse").children();
        }
        $body.each(function () {
            var $child;
            if ($(this)[0].tagName === "LABEL")
                $child = $(this).children();
            else {
                $child = $(this);
            }
            var name = $child.attr("id").replace(key, newID);
            $child.attr("id", name);
            $child.attr("name", name);
        });

        // Step 2: Add User buttons:


        // Step 3: Cache clones for replication:
        factories[key]["clone"] = $ele.clone(true);
    }
}
function recursivelyFactor(id, ele) {
    if (ele) {
        var arr = ele.children;

        // Replace any matching IDs
        var eleID = ele.id;
        if (eleID) {
            var index = eleID.indexOf(id);
            if (index >= 0) {
                ele.id = eleID.replace(id, id + "_num1");
                if (ele.name) {
                    ele.name = ele.name.replace(id, id + "_num1");
                }
                if (ele.children[0] && ele.children[0].children[0]) {
                    if (ele.children[0].children[0].innerText.indexOf("#1") < 0) {
                        ele.children[0].children[0].innerText += " #1";
                    }
                }
            }
        }

        // Recurse
        if (arr) {
            for (var i = 0; i < arr.length; i++) {
                recursivelyFactor(id, arr[i]);
            }
        }
    }
}

// UTILITY FUNCTIONS
function collapseDiv($name, $div) {
    var name = $name.text().toLowerCase();
    var collapseStr = "collapse-" + name.replace(" ", "_");
    var $toggle = $("<a>").attr("data-toggle", "collapse")
            .attr("data-target", "#" + collapseStr)
            .addClass("group-collapse-toggle");

    var $collapseDiv = $("<div>", {class: "collapse in", id: collapseStr});

    $name.append($toggle);

    $div.append($collapseDiv);
    return $collapseDiv;
}

function prevStage() {
    // Remove active rendering
    var active = $activeStage.attr("id");
    var prev = $activeStage.prev().attr("id");

    $activeStage.removeClass("active");
    $("[data-stage=" + active + "").removeClass("active");

    // Activate new rendering
    setTimeout(function () {
        $activeStage = $activeStage.prev();
        $activeStage.addClass("active");
        $("[data-stage=" + prev + "").addClass("active");
    }, 500);
}
function nextStage() {
    // Remove active rendering
    var active = $activeStage.attr("id");
    var next = $activeStage.next().attr("id");

    $activeStage.removeClass("active");
    $("[data-stage=" + active + "").removeClass("active");

    // Activate new rendering
    setTimeout(function () {
        $activeStage = $activeStage.next();
        $activeStage.addClass("active");
        $("[data-stage=" + next + "").addClass("active");
    }, 500);
}

function constructID(ele) {
    var retString = ele.getAttribute("name");
    if (ele.nodeName === "input") {
        retString = ele.parentElement.getAttribute("name") + "-" + ele.children[0].innerHTML;
        ele = ele.parentElement;
    }
    while (ele.nodeName !== "stage") {
        retString = ele.parentElement.getAttribute("name") + "-" + retString;
        ele = ele.parentElement;
    }
    return retString.replace(" ", "_").toLowerCase();
}