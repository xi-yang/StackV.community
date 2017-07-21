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

/* global Mousetrap, keycloak */

var conditions = [];
var factories = {};
var initials = {};
var intent;
var manifest;
var transit = false;
var proceeding = false;
var activeStage;

Mousetrap.bind({
    'left': function () {
        prevStage();
    },
    'right': function () {
        nextStage();
    }
});

function loadIntent(type) {
    $.ajax({
        type: "GET",
        url: "/StackV-web/data/xml/" + type + ".xml",
        dataType: "xml",
        success: function (xml) {
            intent = xml.children[0];
            renderIntent();
            parseSchemaIntoManifest(intent);
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
    var $progress = $("#progressbar");
    for (var i = 1; i < stages.length; i++) {
        // Initialize stage panel
        var stage = stages[i];
        var id = constructID(stage);
        var $div = $("<div>", {class: "intent-stage-div", id: id});
        var $prog = $("<li>");
        if (i === 1) {
            $div.addClass("active");
            $activeStage = $div;
            $prog.addClass("active");
        }
        if (stage.getAttribute("condition")) {
            $div.addClass("conditional");
            $div.attr("data-condition", stage.getAttribute("condition"));

            $prog.addClass("conditional");
            $prog.attr("data-condition", stage.getAttribute("condition"));
        }
        if (stage.getAttribute("proceeding") === "true") {
            $div.addClass("proceeding");
        }
        if (stage.getAttribute("return") === "false") {
            $div.addClass("unreturnable");
        }
        $prog.text(stage.getAttribute("name"));
        panel.append($div);
        stages[id] = $div;
        $currentStageDiv = $div;

        $progress.append($prog);
        // Begin recursive rendering
        renderInputs(stage.children, $currentStageDiv);
        refreshLinks();
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
        var tag = block.children[0].innerHTML;
        var str = block.children[1].innerHTML;
        var condition = block.getAttribute("condition");

        var $label = $("<label>").text(str);
        var $input = $("<input>", {type: "number", name: "block-" + tag, value: 1, min: 1});
        $input.attr("data-block", tag);
        $input.change(function () {
            var eles = $(".block-" + $(this).data("block"));
            var count = eles.length;
            var val = $(this).val();
            var key = eles.first().data("factory");
            var target = eles.first().data("target");

            // Adding elements
            while (val > count) {
                buildClone(key, target);
                count++;
            }
            // Removing elements            
            while (val < count) {
                eles.last().remove();
                eles = $(".block-" + $(this).data("block"));
                count--;
                factories[key]["count"]--;
            }

            recondition();

            refreshLinks();
        });

        if (condition) {
            $label.addClass("conditional");
            $label.attr("data-condition", condition);
        }

        $label.append($input);
        $div.append($label);

        $blockDiv.append($div);
    }
    $panel.append($blockDiv);

    // Render control buttons
    var $controlDiv = $("<div>").attr("id", "intent-panel-meta-control");
    $controlDiv.append($("<button>", {class: "button-control active", id: "intent-submit", html: "Submit"}));
    $panel.append($controlDiv);

    $("#intent-prev").click(function () {
        prevStage();
    });
    $("#intent-next").click(function () {
        nextStage();
    });
    $("#intent-submit").click(function () {
        submitIntent();
    });
}

function renderInputs(arr, $parent) {
    for (var i = 0; i < arr.length; i++) {
        var ele = arr[i];
        if (ele.nodeName === "group") {
            var name = ele.getAttribute("name");
            var factory = ele.getAttribute("factory");
            var label = ele.getAttribute("label");
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
            if (factory === "true") {
                $div.addClass("factory");
                var factObj = {};
                factObj["count"] = 1;
                factories[constructID(ele)] = factObj;
            }
            if (block) {
                $div.addClass("factory");
                $div.addClass("block-" + block);
                var factObj = {};
                factObj["count"] = 1;
                factObj["block"] = block;
                factories[constructID(ele)] = factObj;
            }

            if (label === "false") {
                $div.find(".group-name").addClass("hidden");
            } else {
                $div.addClass("labeled");
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
            var $input = $("<input>", {type: type, class: "intent-input", id: name});
            switch (type) {
                case "button":
                    $input.removeClass("intent-input");
                    $input.click(function (e) {
                        nextStage(true);

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
            if (ele.getElementsByTagName("initial").length > 0) {
                $input.attr("data-initial", ele.getElementsByTagName("initial")[0].innerHTML);
            }

            // Handle multiple choice sourcing
            if (ele.getElementsByTagName("source").length > 0) {
                $input = $("<select>", {id: name, class: "intent-input"});
                var selectName = name;
                var source = ele.getElementsByTagName("source")[0];

                var apiURL = window.location.origin +
                        "/StackV-web/restapi" +
                        source.children[0].innerHTML;
                $.ajax({
                    url: apiURL,
                    type: 'GET',
                    sourceSettings: [source.children[1].innerHTML, source.children[2].innerHTML, source.children[3].innerHTML],
                    beforeSend: function (xhr) {
                        xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
                        xhr.setRequestHeader("Refresh", keycloak.refreshToken);
                    },
                    success: function (instance) {
                        var text = this.sourceSettings[1];
                        var value = this.sourceSettings[2];
                        var interval = this.sourceSettings[0];

                        for (var i = -1; i < (instance.length - parseInt(interval)); i) {
                            var $option = $("<option>");

                            var textIndex = i + parseInt(text);
                            $option.text(instance[textIndex]);
                            var valueIndex = i + parseInt(value);
                            $option.val(instance[valueIndex]);

                            $("#" + selectName).append($option);

                            i += parseInt(interval);
                        }
                    },
                    error: function (xhr, ajaxOptions, thrownError) {
                        if (xhr.status === 404) {
                            console.log(thrownError);
                        }
                    }
                });
            } else if (ele.getElementsByTagName("link").length > 0) {
                $input = $("<select>", {id: name, class: "intent-input"});
                var selectName = name;
                var link = ele.getElementsByTagName("link")[0].innerHTML;
                $input.attr("data-link", link);

                $label.click(function () {
                    refreshLinks();
                });
            } else if (ele.getElementsByTagName("options").length > 0) {
                $input = $("<select>", {id: name, class: "intent-input"});
                var selectName = name;
                var options = ele.getElementsByTagName("options")[0].children;

                for (var j = 0; j < options.length; j++) {
                    var $option = $("<option>");
                    $option.text(options[j].innerHTML);
                    $option.val(options[j].innerHTML);

                    $input.append($option);
                }
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
        var name = head.children[0].innerText.split(" #")[0];
        var key = id.replace(new RegExp("\\_num\\d*", "gm"), "");
        if (factories[key]["block"] === undefined) {
            var $button = $("<button>", {class: "intent-button-factory", text: "Add " + name});
            $button.attr("data-factory", key);
            $button.attr("data-target", fact.parentElement.id);
            $button.click(function (e) {
                // Modify clone for current index
                var key = $(this).data("factory");
                var target = $(this).data("target");

                buildClone(key, target);

                e.preventDefault();
            });
            $(head).append($button);
        } else {
            $(fact).attr("data-factory", key);
            $(fact).attr("data-target", fact.parentElement.id);
        }
    }

    // Step 4: Cache schemas
    for (var i = 0; i < factoryArr.length; i++) {
        var fact = factoryArr[i];
        var id = fact.id;
        var key = id.replace(new RegExp("\\_num\\d*", "gm"), "");

        var $clone = $(fact).clone(true, true);
        $clone.find("[data-initial]").removeAttr("data-initial");
        factories[key]["clone"] = $clone;
    }

    initializeInputs();
    refreshLinks();
}
function recursivelyFactor(id, ele) {
    if (ele) {
        var arr = ele.children;
        var label = $(ele).hasClass("labeled");
        // Replace any matching IDs
        var eleID = ele.id;
        if (eleID) {
            var index = eleID.indexOf(id);
            if (index >= 0) {
                ele.id = eleID.replace(id, id + "_num1");
                if (label && ele.children[0] && ele.children[0].children[0]) {
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

function submitIntent() {
    refreshLinks();
    var json = {};
    $("#intent-panel-body .intent-input").each(function () {
        var cond = $(this).parents('.conditional').length;
        if (cond > 0 && $(this).parents('.conditioned').length < cond) {
            ;
        } else {
            var arr = this.id.split("-");

            var last = json;
            var i;
            for (i = 1; i < (arr.length - 1); i++) {
                var key = arr[i];
                if (!(key in last)) {
                    last[key] = {};
                }
                last = last[key];
            }
            var key = arr[i];
            if ($(this).attr("type") === "checkbox") {
                last[key] = $(this).is(":checked");
            } else {
                last[key] = $(this).val();
            }
        }
    });

    manifest = json;
    parseManifestIntoJSON();
}

// UTILITY FUNCTIONS
function collapseDiv($name, $div) {
    var name = $name.text().toLowerCase();
    var collapseStr = "collapse-" + name.replace(/ /g, "_");
    var $toggle = $("<a>").attr("data-toggle", "collapse")
            .attr("data-target", "#" + collapseStr)
            .addClass("group-collapse-toggle");

    var $collapseDiv = $("<div>", {class: "collapse in", id: collapseStr});

    $name.append($toggle);

    $div.append($collapseDiv);
    return $collapseDiv;
}

function prevStage() {
    if (!proceeding) {
        proceeding = true;
        var active = $activeStage.attr("id");
        var $prev = $activeStage.prev();
        if ($prev.hasClass("unreturnable")) {
            proceeding = false;
            return;
        }
        while ($prev.hasClass("conditional") && !$prev.hasClass("conditioned")) {
            $prev = $prev.prev();
            if ($prev.hasClass("unreturnable")) {
                proceeding = false;
                return;
            }
        }
        if ($prev[0].tagName !== "DIV") {
            proceeding = false;
            return;
        }

        // Update progress bar
        var $prog = $("#progressbar");
        var activeProg = $prog.children(".active")[0];
        activeProg.className = "";
        activeProg.previousElementSibling.className = "active";

        var prevID = $prev.attr("id");

        $activeStage.removeClass("active");
        $("[data-stage=" + active + "").removeClass("active");

        // Activate new rendering
        setTimeout(function () {
            $activeStage = $prev;
            $activeStage.addClass("active");
            $("[data-stage=" + prevID + "").addClass("active");
            proceeding = false;
        }, 500);
    }
}
function nextStage(flag) {
    if (!proceeding) {
        proceeding = true;

        if ($activeStage.hasClass("proceeding") && !flag) {
            proceeding = false;
            return;
        }
        var active = $activeStage.attr("id");
        var $next = $activeStage.next();
        while ($next.hasClass("conditional") && !$next.hasClass("conditioned")) {
            $next = $next.next();
        }
        if ($next.length === 0) {
            proceeding = false;
            return;
        }

        // Update progress bar
        var $prog = $("#progressbar");
        var activeProg = $prog.children(".active")[0];
        activeProg.className = "";
        activeProg.nextElementSibling.className = "active";

        var nextID = $next.attr("id");

        $activeStage.removeClass("active");
        $("[data-stage=" + active + "").removeClass("active");

        // Activate new rendering
        setTimeout(function () {
            $activeStage = $next;
            $activeStage.addClass("active");
            $("[data-stage=" + nextID + "").addClass("active");
            proceeding = false;
        }, 500);
    }
}

function getParentName(key) {
    var arr = key.split("-");
    var index = arr.length - 2;
    if (index >= 0) {
        return arr[index];
    } else {
        return null;
    }
}
function getName(key) {
    var arr = key.split("-");
    var index = arr.length - 1;
    return arr[index];
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
    return retString.replace(/ /g, "_").toLowerCase();
}

function buildClone(key, target) {
    var count = ++factories[key]["count"];
    var $clone = factories[key]["clone"].clone(true, true);
    var name = getName(key);

    var header = $clone[0].children[0].children[0];
    header.innerText = header.innerText.replace("#1", "#" + count);

    var regName = new RegExp(name + "_num1", "g");
    $clone.html($clone.html().replace(regName, name + "_num" + count));
    //$clone.html($clone.html().replace(/#1/g, "#" + count));

    var regColl = new RegExp("collapse-" + name + "_num1", "g");
    $clone.html($clone.html().replace(regColl, "collapse-" + name + "_num" + count));

    // Change element attributes
    $clone.attr("id", $clone.attr("id").replace(regName, name + "_num" + count));

    // Match parent (sub-factories)
    var $target = $("#" + target);
    var $parent = $target.parent();

    if ($parent.attr("id") !== "intent-panel-body" &&
            getParentName($clone.attr("id")) !== getName($parent.attr("id"))) {
        var regParent = new RegExp(getParentName($clone.attr("id")), "g");
        $clone.attr("id", $clone.attr("id").replace(regParent, getName($parent.attr("id"))));
        $clone.html($clone.html().replace(regParent, getName($parent.attr("id"))));
    }

    // Replace control buttons    
    var $button = $("<button>", {class: "intent-button-remove close"});
    $button.attr("aria-label", "Close");
    $button.html('<span aria-hidden="true">&times;</span>');
    $button.click(function () {
        $(this).parent().parent().remove();
    });
    $clone.find("[data-factory=" + key + "]").replaceWith($button);

    $target.append($clone);

    // Reset button listeners  
    $(".intent-button-factory").off("click");
    $(".intent-button-factory").click(function (e) {
        // Modify clone for current index
        var key = $(this).data("factory");
        var target = $(this).data("target");

        buildClone(key, target);

        e.preventDefault();
    });

    recondition();
}

function refreshLinks() {
    var $inputArr = $("[data-link]");
    for (var i = 0; i < $inputArr.length; i++) {
        var $input = $($inputArr[i]);
        var currSelection = $input.val();
        $input.children().remove();
        var link = $input.data("link");

        var targetArr = $(".block-" + link);
        for (var j = 0; j < targetArr.length; j++) {
            var $option = $("<option>");

            var eleID = targetArr[j].id;
            var eleName = $("#" + eleID + "-name").val();
            $option.text("Subnet " + (j + 1) + " (" + eleName + ")");
            $option.val(getName(eleID));

            $input.append($option);
        }

        if (currSelection) {
            $input.val(currSelection);
        }
    }
}

function parseSchemaIntoManifest(schema) {
    var json = {};
    $(schema).find("input").each(function () {
        var arr = [this.children[0].innerHTML.toLowerCase().replace(/ /g, "_")];
        var parent = this.parentElement;
        while (parent.tagName !== "intent") {
            var name = parent.getAttribute("name").toLowerCase().replace(/ /g, "_");
            if (parent.getAttribute("block") || parent.getAttribute("factory")) {
                arr.unshift(name + "_numX");
            } else {
                arr.unshift(name);
            }
            parent = parent.parentElement;
        }

        var last = json;
        var i;
        for (i = 1; i < (arr.length - 1); i++) {
            var key = arr[i];
            if (!(key in last)) {
                last[key] = {};
            }
            last = last[key];
        }
        var key = arr[i];
        last[key] = "";
    });

    manifest = json;
}

var recurCache = {};
function parseManifestIntoJSON() {
    // Step 1: Reorg hierarchy
    $(intent).find("path").each(function () {
        var arr = this.children;
        for (var i = 0; i < arr.length; i++) {
            var target = arr[i].innerHTML.toLowerCase().replace(/ /g, "_");
            var eleName = this.parentElement.getAttribute("name");

            // Find element(s) in manifest
            recurCache = [];
            findKeyDeepCache(manifest, eleName.toLowerCase().replace(/ /g, "_"));

            for (var key in recurCache) {
                var ele = recurCache[key];
                // Find target
                if (target === "root") {
                    for (var child in ele) {
                        manifest[child] = ele[child];
                    }
                } else {
                    var targetEle = findKeyDeep(manifest, ele[target]);
                    delete ele[target];
                    targetEle[key] = ele;
                }
            }

        }
    });

    // Step 2: Convert numerals into proper arrays
    convertToArrays(manifest);

    // Step 3: Trim leaves
    trimLeaves(manifest);
}


// Recursive Functions
function findKeyDeepCache(recur, key) {
    var result = null;
    if (recur instanceof Array) {
        for (var i = 0; i < recur.length; i++) {
            result = findKeyDeepCache(recur[i], key);
            if (result) {
                break;
            }
        }
    } else {
        for (var prop in recur) {
            if (prop.split("_num")[0] === key) {
                recurCache[prop] = recur[prop];
                delete recur[prop];
            }
            if (recur[prop] instanceof Object || recur[prop] instanceof Array) {
                result = findKeyDeepCache(recur[prop], key);
                if (result) {
                    break;
                }
            }
        }
    }
    return result;
}
function findKeyDeep(recur, key) {
    var result = null;
    if (recur instanceof Array) {
        for (var i = 0; i < recur.length; i++) {
            result = findKeyDeep(recur[i], key);
            if (result) {
                break;
            }
        }
    } else {
        for (var prop in recur) {
            if (prop === key) {
                return recur[prop];
            }
            if (recur[prop] instanceof Object || recur[prop] instanceof Array) {
                result = findKeyDeep(recur[prop], key);
                if (result) {
                    break;
                }
            }
        }
    }
    return result;
}

function convertToArrays(recur) {
    for (var prop in recur) {
        if (recur[prop] instanceof Object || recur[prop] instanceof Array) {
            convertToArrays(recur[prop]);
        }
        if (prop.indexOf("_num") > -1) {
            var name = prop.split("_num")[0] + "s";
            if (!(name in recur)) {
                recur[name] = [];
            }

            recur[name].push(recur[prop]);
            delete recur[prop];
        }
    }
}

function trimLeaves(recur) {
    for (var prop in recur) {
        if (recur[prop] instanceof Object || recur[prop] instanceof Array) {
            trimLeaves(recur[prop]);
        }

        if (recur[prop] === "") {
            delete recur[prop];
        } else if (recur[prop] && recur[prop].constructor === Object && Object.keys(recur[prop]).length === 0) {
            delete recur[prop];
        } else if (recur[prop] && recur[prop].constructor === Array && recur[prop][0] === undefined) {
            delete recur[prop];
        }
    }
}

function recondition() {
    for (var i = 0; i < conditions.length; i++) {
        $("[data-condition='" + conditions[i] + "']").addClass("conditioned");
    }
}

function initializeInputs() {
    var $arr = $("[data-initial]");
    for (var i = 0; i < $arr.length; i++) {
        var $input = $($arr[i]);
        var val = $input.data("initial");
        $input.val(val);
        $input.removeAttr("data-initial");
    }
}