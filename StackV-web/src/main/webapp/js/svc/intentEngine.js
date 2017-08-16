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

/* global Mousetrap, keycloak, Power2, baseUrl */

var conditions = [];
var factories = {};
var initials = {};
var bindings = {};
var gsap = {};
var intent;
var intentType;
var manifest;
var package = {};
var transit = false;
var proceeding = false;
var activeStage;
var simpleManifest = false;

Mousetrap.bind({
    'left': function () {
        prevStage();
    },
    'right': function () {
        nextStage();
    }
});

function loadIntent(type) {
    intentType = type;
    $.ajax({
        type: "GET",
        url: "/StackV-web/data/xml/" + type + ".xml",
        dataType: "xml",
        success: function (xml) {
            intent = xml.children[0];
            renderIntent();
            parseSchemaIntoManifest(intent);
            if (!getURLParameter("preload")) {
                switch (type) {
                    case "dnc":
                        //preloadDNC();
                        break;
                    case "hybridcloud":
                        preloadAHC();
                        break;
                    case "vcn":
                        preloadOPSVCN();
                        break;
                }
            }
        },
        error: function (err) {
            console.log('Error Loading XML! \n' + err);
        }
    });

    refreshTimer = setInterval(function () {
        keycloak.updateToken(90);
    }, (60000));
}

function renderIntent() {
    // Stage 1: Initialization                
    initializeIntent();

    // Stage 2: Factorization
    factorizeRendering();
}

function initializeIntent() {
    var panel = $("#intent-panel-body");
    gsap["intent"] = new TweenLite("#intent-panel", 0.5, {ease: Power2.easeInOut, paused: true, opacity: "1", display: "block"});
    // Initialize meta sidebar
    var meta = intent.children[0];
    initMeta(meta);

    if (intent.getAttribute("simplify") === "true") {
        simpleManifest = true;
    }

    // Begin rendering stages
    var stages = intent.children;
    var $progress = $("#intent-progress");
    for (var i = 1; i < stages.length; i++) {
        // Initialize stage panel
        var stage = stages[i];
        var id = constructID(stage);
        var $div = $("<div>", {class: "intent-stage-div", id: id});
        var $prog = $("<li>", {id: "prog-" + id});
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

        var $a = $("<a>").text(stage.getAttribute("name"));
        $prog.append($a);

        panel.append($div);
        stages[id] = $div;
        $currentStageDiv = $div;
        gsap[id] = new TweenLite("#" + id, 0.5, {ease: Power2.easeInOut, paused: true, opacity: "1", display: "block"});

        if (i === 1) {
            $activeStage = $div;
            gsap[id].play();
            $prog.addClass("active");
            $activeStage.addClass("active");
        }

        $progress.append($prog);
        // Begin recursive rendering
        renderInputs(stage.children, $currentStageDiv);
        refreshLinks();
    }
    $progress.append($("<li>"));
    moderateControls();
    recondition();
    setTimeout(function () {
        gsap["intent"].play();
    }, 500);
}


// UTILITY FUNCTIONS
function initMeta(meta) {
    // Render service tag
    var $panel = $("#intent-panel-meta");
    $("#meta-title").text(meta.children[0].innerHTML);
    $("#meta-alias").change(function () {
        $(this).removeClass("invalid");        
    });

    // Render blocks
    var $blockDiv = $("<div>").attr("id", "intent-panel-meta-block");
    var blocks = meta.getElementsByTagName("block");
    for (var i = 0; i < blocks.length; i++) {
        var $div = $("<div>", style = "margin-bottom:20px;");
        var block = blocks[i];
        var tag = block.children[0].innerHTML;
        var str = block.children[1].innerHTML;
        var condition = block.getAttribute("condition");

        var $label = $("<label>").text(str);
        var $input = $("<input>", {type: "number", name: "block-" + tag, value: 1, min: 0});
        $input.attr("data-block", tag);
        $input.change(function () {
            var eles = $(".block-" + $(this).data("block"));
            var val = $(this).val();
            var key = eles.first().data("factory");
            var target = eles.first().data("target");

            var count = eles.length;
            if (eles.first().hasClass("block-removed")) {
                count = 0;
            }

            // Adding elements
            while (val > count) {
                if (count === 0) {
                    // Replace first element (superficially)
                    eles.last().remove();
                    buildClone(key, target);
                    count++;
                } else {
                    buildClone(key, target);
                    count++;
                }
            }
            // Removing elements            
            while (val < count) {
                if (count === 1) {
                    // Remove last element (superficially)
                    eles.last().empty().addClass("block-removed");

                    factories[key]["count"]--;
                    break;
                } else {
                    eles.last().remove();

                    eles = $(".block-" + $(this).data("block"));
                    count--;
                    factories[key]["count"]--;
                }
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

    $("#intent-prev").click(function () {
        prevStage();
    });
    $("#intent-next").click(function () {
        nextStage();
    });
    $("#intent-submit").click(function () {
        submitIntent(false);
    });
    $("#intent-save").click(function () {
        submitIntent(true);
    });

    $("#button-profile-save").click(function () {
        saveManifest();
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
            var start = ele.getAttribute("start");
            var def = ele.getAttribute("default");
            var collapsible = ele.getAttribute("collapsible");
            var block = ele.getAttribute("block");
            var str = name.charAt(0).toUpperCase() + name.slice(1);
            var eleID = constructID(ele);

            var $div = $("<div>", {class: "intent-group-div", id: eleID});
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
                factories[eleID] = factObj;
            }
            if (block) {
                $div.addClass("factory");
                $div.addClass("block-" + block);
                var factObj = {};
                factObj["count"] = 1;
                factObj["block"] = block;
                factories[eleID] = factObj;
            }
            if (start) {
                $div.attr("data-start", start);
            }
            if (def) {
                $div.attr("data-default", def);
            }

            if (label === "false") {
                var $groupName = $div.find(".group-name").addClass("unlabeled");
                $groupName.text($groupName.text().split("#")[0]);
            } else {
                $div.addClass("labeled");
            }

            if (condition) {
                $div.addClass("conditional");
                $div.attr("data-condition", condition);
            }

            if ($(ele).children("bound").length > 0) {
                $div.attr("data-bound", eleID);
                var boundArr = $(ele).children("bound");
                if (!(eleID in bindings)) {
                    bindings[eleID] = {};
                }
                var binding = bindings[eleID];

                for (var j = 0; j < boundArr.length; j++) {
                    var name = boundArr[j].children[0].innerHTML;
                    var val = boundArr[j].children[1].innerHTML;
                    if (!(name in binding)) {
                        binding[name] = {};
                    }
                    binding[name][val] = {};

                    var minEle = boundArr[j].children[2];
                    if (minEle) {
                        binding[name][val]["min"] = minEle.innerHTML;
                    }
                    var maxEle = boundArr[j].children[3];
                    if (maxEle) {
                        binding[name][val]["max"] = maxEle.innerHTML;
                    }
                }
            }

            // Recurse!
            renderInputs(ele.children, $targetDiv);
        } else if (ele.nodeName === "input") {
            var type = ele.children[1].innerHTML;
            var name = constructID(ele);
            var trigger = ele.getAttribute("trigger");
            var condition = ele.getAttribute("condition");
            var required = ele.getAttribute("required");

            var $label = $("<label>").text(ele.children[0].innerHTML);
            var $input = $("<input>", {type: type, class: "intent-input", id: name});
            var $message = null;
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

            if (ele.children[0].innerHTML.toLowerCase() === "name" && ele.parentElement.tagName === "group") {
                $input.attr("data-name", ele.parentElement.getAttribute("name") + "_1");
                $input.val(ele.parentElement.getAttribute("name") + "_1");
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
                    case "xlarge":
                        $label.addClass("col-sm-12");
                        break;
                    default:
                        $label.addClass("col-sm-6");
                }
            } else {
                $label.addClass("col-sm-6");
            }

            if (ele.getElementsByTagName("default").length > 0) {
                $input.attr("data-default", ele.getElementsByTagName("default")[0].innerHTML);
                $input.val(ele.getElementsByTagName("default")[0].innerHTML);
            }
            if (ele.getElementsByTagName("initial").length > 0) {
                $input.attr("data-initial", ele.getElementsByTagName("initial")[0].innerHTML);
            }

            if (required === "true") {
                $input.attr("data-required", true);
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
                var $default = $("<option>", {selected: true}).text("");
                $input.append($default);
                var nameVal = ele.getElementsByTagName("link")[0].getAttribute("name");
                if (nameVal) {
                    $input.addClass("nameVal");
                }
            } else if (ele.getElementsByTagName("options").length > 0) {
                $input = $("<select>", {id: name, class: "intent-input"});
                var selectName = name;
                var options = ele.getElementsByTagName("options")[0].children;
                if (!ele.getAttribute("required")) {
                    var $null = $("<option>").text("N/A").val("");
                    $input.append($null);
                }

                var def;
                for (var j = 0; j < options.length; j++) {
                    var $option;
                    if (options[j].getAttribute("condition") !== null) {
                        $option = $("<option>", {disabled: true});
                        $option.attr("data-condition-select", options[j].getAttribute("condition"));
                    } else {
                        $option = $("<option>");
                    }

                    $option.text(options[j].innerHTML);
                    $option.val(options[j].innerHTML);

                    if (options[j].getAttribute("trigger") !== null) {
                        $option.attr("data-trigger", options[j].getAttribute("trigger"));
                        $input.change(function () {
                            var sel = $(this).children(":selected");
                            var trigger = sel.data("trigger");
                            if (trigger) {
                                addCondition(trigger);
                            } else {
                                removeCondition(trigger);
                            }
                        });
                    }

                    if (options[j].getAttribute("default") !== null) {
                        def = $option.val();
                    }

                    $input.append($option);
                }

                if (def) {
                    $input.val(def);
                    var sel = $input.children(":selected");
                    var trigger = sel.data("trigger");
                    if (trigger) {
                        addCondition(trigger);
                    }
                }
            }
            if (trigger) {
                switch (type) {
                    case "text":
                        break;
                    case "button":
                        $label.attr("data-trigger", trigger);
                        $label.click(function () {
                            addCondition($(this).data("trigger"));
                        });
                        break;
                }
            }
            if (condition) {
                $label.addClass("conditional");
                $label.attr("data-condition", condition);
            }

            if (ele.getElementsByTagName("valid").length > 0) {
                var validRef = ele.getElementsByTagName("valid")[0].innerHTML;
                $input.attr("data-valid", validRef);

                $input.change(function () {
                    $(this).removeClass("invalid");
                    var $stage = $($(this).parents(".intent-stage-div")[0]);
                    if ($stage.find(".invalid").length === 0) {
                        $("#prog-" + $stage.attr("id")).removeClass("invalid");
                    }
                });
            }

            $label.append($input);
            if ($message) {
                $label.append($message);
            }
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
        //collapse.id += "_num1";
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
            if ($(fact).data("bound")) {
                $button.attr("data-bound", $(fact).data("bound"));
            }
            $button.click(function (e) {
                // Modify clone for current index
                var key = $(this).data("factory");
                var target = $(this).parent().parent().attr("id");

                buildClone(key, target, $(this));

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

    // Step 5: Finishing work        
    initializeInputs();
    refreshLinks();
    enforceBounds();
    expandStarters();
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

function submitIntent(save) {
    gsap["intent"].reverse();
    refreshLinks();
    $(".intent-input.invalid").removeClass("invalid");
    $(".intent-input-message").remove();

    // Validate
    var validation = $("[data-valid]");
    var valid = true;
    if (!$("#meta-alias").val()) {
        valid = false;
        $("#meta-alias").addClass("invalid");
    }

    for (var i = 0; i < validation.length; i++) {
        var $input = $(validation[i]);
        if (isEnabledInput($input)) {
            var validRef = $input.data("valid");
            var validEle = intent.children[0].getElementsByTagName("validation")[0];
            var constEle = null;
            for (var j = 0; j < validEle.children.length; j++) {
                var constraint = validEle.children[j];
                if (constraint.children[0].innerHTML === validRef) {
                    constEle = constraint;
                    break;
                }
            }
            if (constEle) {
                var regex = constEle.children[1].innerHTML;
                regex = regex.replace(/\\/g, "\\");
                regex = new RegExp(regex, "gm");

                var $message = $("<div>", {class: "intent-input-message"});
                if (constEle.getElementsByTagName("message").length > 0) {
                    $message.text(constEle.getElementsByTagName("message")[0].innerHTML);
                }

                $input.parent().append($message);

                if (($input.val() === null || $input.val() === "")) {
                    if ($input.data("required")) {
                        valid = false;
                        $input.addClass("invalid");
                        var $stage = $($input.parents(".intent-stage-div")[0]);
                        $("#prog-" + $stage.attr("id")).addClass("invalid");
                    }
                } else if ($input.val().match(regex) === null) {
                    valid = false;
                    $input.addClass("invalid");
                    var $stage = $($input.parents(".intent-stage-div")[0]);
                    $("#prog-" + $stage.attr("id")).addClass("invalid");
                }
            }
        }
    }

    if (valid) {
        // Parse manifest
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

        // Check for saving
        if (save) {
            openSaveModal();
            setTimeout(function () {
                gsap["intent"].play();
            }, 250);
        } else {
            // Submit to backend
            var apiUrl = baseUrl + '/StackV-web/restapi/app/service';
            $.ajax({
                url: apiUrl,
                type: 'POST',
                data: JSON.stringify(package),
                contentType: "application/json; charset=utf-8",
                dataType: "json",
                beforeSend: function (xhr) {
                    xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
                    xhr.setRequestHeader("Refresh", keycloak.refreshToken);
                },
                success: function (result) {
                    window.location.href = "/StackV-web/ops/catalog.jsp";
                },
                error: function (result) {
                    window.location.href = "/StackV-web/ops/catalog.jsp";
                }
            });
        }
    } else {
        setTimeout(function () {
            gsap["intent"].play();
        }, 500);
    }
}

// UTILITY FUNCTIONS
function collapseDiv($name, $div) {
    var name = $name.text().toLowerCase();
    var parent = getParentName($div.attr("id"));
    var collapseStr = "collapse-" + parent.replace(/ /g, "_") + "-" + name.replace(/ /g, "_");
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
        if ($prev[0].tagName === "IMG") {
            proceeding = false;
            return;
        }

        refreshLinks();

        // Update progress bar
        var $prog = $("#intent-progress");
        var $activeProg = $($prog.children(".active")[0]);
        $activeProg.removeClass("active");
        $activeProg.prev().addClass("active");

        var currID = $activeStage.attr("id");
        $activeStage.removeClass("active");
        var prevID = $prev.attr("id");
        gsap[currID].reverse();
        $activeStage = $prev;
        $activeStage.addClass("active");

        moderateControls();

        // Activate new rendering
        setTimeout(function () {
            gsap[prevID].play();
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
        var $next = $activeStage.next();
        while ($next.hasClass("conditional") && !$next.hasClass("conditioned")) {
            $next = $next.next();
        }
        if ($next.length === 0) {
            proceeding = false;
            return;
        }

        refreshLinks();

        // Update progress bar
        var $prog = $("#intent-progress");
        var $activeProg = $($prog.children(".active")[0]);
        $activeProg.removeClass("active");
        $activeProg.next().addClass("active");

        var currID = $activeStage.attr("id");
        $activeStage.removeClass("active");
        var nextID = $next.attr("id");
        gsap[currID].reverse();
        $activeStage = $next;
        $activeStage.addClass("active");

        moderateControls();

        // Activate new rendering
        setTimeout(function () {
            gsap[nextID].play();
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

function buildClone(key, target, $factoryBtn) {
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
    $clone.addClass("factored");

    if ($parent.attr("id") !== "intent-panel-body" &&
            getParentName($clone.attr("id")) !== getName($parent.attr("id"))) {
        var regParent = new RegExp(getParentName($clone.attr("id")), "g");
        $clone.attr("id", $clone.attr("id").replace(regParent, getName($parent.attr("id"))));
        $clone.html($clone.html().replace(regParent, getName($parent.attr("id"))));
    }

    var cloneID = $clone.attr("id");

    // Replace control buttons    
    var $button = $("<button>", {class: "intent-button-remove close"});
    $button.attr("aria-label", "Close");
    $button.html('<span aria-hidden="true">&times;</span>');
    $button.click(function () {
        var $btn = $(this);
        var id = $btn.parent().parent().attr("id");
        gsap[id].reverse();
        setTimeout(function () {
            var $ele = $btn.parent().parent();
            var $first = firstOf($ele);

            $ele.remove();
            refreshNumerals($first);
            refreshNames();
            enforceBounds();
        }, 500);
    });
    $clone.find("[data-factory=" + key + "]").replaceWith($button);

    if ($factoryBtn) {
        $factoryBtn.parent().parent().after($clone);
    } else {
        $target.append($clone);
    }

    gsap[cloneID] = new TweenLite("#" + cloneID, 0.5, {ease: Power2.easeInOut, paused: true, opacity: "1", display: "block"});
    gsap[cloneID].play();

    // Reset button listeners  
    $(".intent-button-factory").off("click");
    $(".intent-button-factory").click(function (e) {
        // Modify clone for current index
        var key = $(this).data("factory");
        var target = $(this).parent().parent().attr("id");

        buildClone(key, target, $(this));

        e.preventDefault();
    });

    var $defArr = $clone.find("[data-default]");
    for (var i = 0; i < $defArr.length; i++) {
        var $input = $($defArr[i]);
        $input.val($input.data("default"));
    }

    recondition();
    refreshLinks();
    if ($factoryBtn) {
        refreshNumerals($factoryBtn.parent().parent());
    }

    refreshNames();
    enforceBounds();

    if ($clone.children(".collapse").length > 0) {
        var id = "#" + $($clone.children(".collapse")[0]).attr("id");
        var arr = $clone.find(".group-collapse-toggle");
        for (var i = 0; i < arr.length; i++) {
            if ($(arr[i]).data("target") === id) {
                $(arr[i]).click();
                break;
            }
        }
    }
}

function refreshLinks() {
    var $inputArr = $("[data-link]");
    for (var i = 0; i < $inputArr.length; i++) {
        var $input = $($inputArr[i]);
        var nameVal = $input.hasClass("nameVal");
        var currSelection = $input.val();
        $input.children().remove();
        var link = $input.data("link");
        var $default = $("<option>", {selected: true}).text("");
        $input.append($default);

        var targetArr = $(".block-" + link);
        for (var j = 0; j < targetArr.length; j++) {
            var $option = $("<option>");

            var eleID = targetArr[j].id;
            var eleName = $("#" + eleID + "-name").val();
            $option.text((j + 1) + " (" + eleName + ")");
            if (nameVal) {
                $option.val(eleName);
            } else {
                $option.val(getName(eleID));
            }

            $input.append($option);
        }

        if (currSelection) {
            $input.val(currSelection);
        }
    }
}

function refreshNumerals($ele) {
    var $name = $($ele.children()[0].children[0]);
    if (!$name.hasClass("unlabeled")) {
        var arr = $name.html().split("#");
        var count = 1;

        var $next = $ele.next();
        if ($next.length > 0) {
            var $nextName = $($next.children()[0].children[0]);
            var nextArr = $nextName.html().split("#");
            while (nextArr && (nextArr[0] === arr[0])) {
                $nextName.html(arr[0] + "#" + ++count);

                $next = $next.next();
                if ($next.length === 0)
                    break;
                $nextName = $($next.children()[0].children[0]);
                nextArr = $nextName.html().split("#");
            }
        }
    }
}
function refreshNames() {
    var $nameArr = $("[data-name]");
    for (var i = 0; i < $nameArr.length; i++) {
        var $input = $($nameArr[i]);
        if ($input.val() === "" || $input.val().match(new RegExp(/^connection_\d+$/))) {
            var name = $input.data("name");

            var $parent = $input.parent();
            while (!$parent.hasClass("intent-group-div")) {
                $parent = $parent.parent();
                if (!$parent)
                    return;
            }
            var numName = $parent.children(".group-header").children(".group-name").text();
            name = name.split("_")[0] + "_" + numName.split("#")[1];
            $input.val(name);
        }
    }

}

function moderateControls() {
    var proceeding = true;
    var returning = true;
    $(".intent-controls").removeClass("blocked");
    var $prevButton = $("#intent-prev");
    var $nextButton = $("#intent-next");

    var $prev = $activeStage.prev();
    if ($prev.hasClass("unreturnable")) {
        returning = false;
    }
    while ($prev.hasClass("conditional") && !$prev.hasClass("conditioned")) {
        $prev = $prev.prev();
        if ($prev.hasClass("unreturnable")) {
            returning = false;
        }
    }
    if ($prev[0].tagName === "IMG") {
        returning = false;
    }

    if ($activeStage.hasClass("proceeding")) {
        proceeding = false;
    }
    var $next = $activeStage.next();
    while ($next.hasClass("conditional") && !$next.hasClass("conditioned")) {
        $next = $next.next();
    }
    if ($next.length === 0) {
        proceeding = false;
    }

    if (!returning) {
        $prevButton.addClass("blocked");
    }
    if (!proceeding) {
        $nextButton.addClass("blocked");
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
                    if (targetEle) {
                        // Input link path
                        delete ele[target];
                        targetEle[key] = ele;
                    } else {
                        // Explicit JSON path
                        var pathArr = target.split("/");

                        var obj = manifest;
                        for (var j = 0; j < pathArr.length; j++) {
                            if (!(pathArr[j] in obj)) {
                                obj[pathArr[j]] = {};
                            }
                            obj = obj[pathArr[j]];
                        }

                        obj[key] = ele;
                    }
                }
            }
        }
    });

    // Step 2: Convert numerals into proper arrays
    convertToArrays(manifest);

    // Step 2.5: Simplify manifest
    if (simpleManifest) {
        simplifyManifest(manifest);
    }

    // Step 3: Trim leaves
    trimLeaves(manifest);

    // Step 4: Finishing and initialization
    var newManifest = {};
    newManifest["data"] = manifest;
    manifest = newManifest;
    manifest["service"] = intentType;

    var apiUrl = baseUrl + '/StackV-web/restapi/app/service/uuid';
    $.ajax({
        url: apiUrl,
        async: false,
        type: 'GET',
        dataType: "text",
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
            xhr.setRequestHeader("Refresh", keycloak.refreshToken);
        },
        success: function (result) {
            manifest["uuid"] = result;

            // Render template
            var rendered = render(manifest);
            delete manifest["data"]["uuid"];

            package["service"] = intentType;
            package["alias"] = $("#meta-alias").val();
            package["data"] = rendered;
        }
    });
}

function enforceBounds() { // @TODO expand to include factorized elements
    var arr = $("button[data-bound]");
    for (var i = 0; i < arr.length; i++) {
        var $button = $(arr[i]);
        var key = $button.data("bound");
        var $scope = $button.parent().parent().parent();

        $scope.find("[data-bound=" + key + "] .intent-button-remove").removeAttr("disabled");
        $button.removeAttr("disabled");

        var eleCount = $scope.children("div[data-bound=" + key + "]").length;
        for (var name in bindings[key]) {
            var currVal = $("#" + name).val();
            for (var val in bindings[key][name]) {
                var changes = true;
                while (changes) {
                    changes = false;
                    if (val === currVal) {
                        // Check for min
                        if ("min" in bindings[key][name][val]) {
                            var min = bindings[key][name][val]["min"];
                            if (eleCount <= min) {
                                while (eleCount < min) {
                                    $button.click();
                                    eleCount = $scope.children("div[data-bound=" + key + "]").length;
                                    changes = true;
                                }
                                $scope.find("[data-bound=" + key + "] .intent-button-remove").attr("disabled", true);
                            }
                        }

                        // Check for max
                        if ("max" in bindings[key][name][val]) {
                            var max = bindings[key][name][val]["max"];
                            if (eleCount >= max) {
                                while (eleCount > max) {
                                    $scope.children("[data-bound=" + key + "]").last().remove();
                                    eleCount = $scope.children("div[data-bound=" + key + "]").length;
                                    changes = true;
                                }
                                $button.attr("disabled", true);
                            }
                        }
                    }
                }
            }
        }

    }
}

function stripID($ele) {
    return id = $ele.attr("id").replace(new RegExp("\\_num\\d*", "gm"), "");
}

function firstOf($ele) {
    var id = $ele.attr("id");
    return $("#" + id.slice(0, id.lastIndexOf("_num")) + "_num1");
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

function simplifyManifest(manifest) {
    for (var prop in manifest) {
        simplifyManifestSub(manifest[prop], prop);
    }
}
function simplifyManifestSub(recur, key) {
    for (var prop in recur) {
        var index = prop.indexOf(key);
        if (index > -1) {
            var newProp = prop.slice(prop.indexOf(key) + key.length + 1);
            recur[newProp] = recur[prop];
            delete recur[prop];

            simplifyManifestSub(recur[newProp], key);
        } else if (recur[prop] instanceof Object || recur[prop] instanceof Array) {
            simplifyManifestSub(recur[prop], key);
        }
    }
}

function trimLeaves(recur) {
    for (var prop in recur) {
        if (recur[prop] instanceof Object || recur[prop] instanceof Array) {
            trimLeaves(recur[prop]);
        }

        if (recur[prop] === "" || recur[prop] === null) {
            delete recur[prop];
        } else if (recur[prop] && recur[prop].constructor === Object && Object.keys(recur[prop]).length === 0) {
            delete recur[prop];
        } else if (recur[prop] && recur[prop].constructor === Array && recur[prop][0] === undefined) {
            delete recur[prop];
        }
    }
}

function recondition() {
    $("[data-condition]").removeClass("conditioned");
    $("[data-condition-select]").attr("disabled", true);

    for (var i = 0; i < conditions.length; i++) {
        $("[data-condition='" + conditions[i] + "']").addClass("conditioned");
        $("[data-condition-select='" + conditions[i] + "']").removeAttr("disabled");
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

    var $boundArr = $("[data-bound]");
    for (var i = 0; i < $boundArr.length; i++) {
        var $ele = $($boundArr[i]);
        for (var key in bindings[$ele.data("bound")]) {
            var $input = $("#" + key);
            if (!$input.hasClass("binding")) {
                $input.change(function () {
                    enforceBounds();
                });

                $input.addClass("binding");
            }
        }
    }
}

function expandStarters() {
    var $arr = $("[data-start]");
    for (var i = 0; i < $arr.length; i++) {
        var $input = $($arr[i]);
        var val = $input.data("start");
        var count = 1;

        // TODO
    }
}

function addCondition(trigger) {
    conditions.push(trigger);
    recondition();
}
function removeCondition(trigger) {
    var index = conditions.indexOf(trigger);
    conditions.splice(index, 1);
    recondition();
}

function isEnabledInput($input) {
    var cond = $input.parents('.conditional').length;
    if (cond > 0 && $input.parents('.conditioned').length < cond) {
        return false;
    }
    if ($input.hasClass("conditional") && !$input.hasClass("conditioned")) {
        return false;
    }
    return true;
}

function openSaveModal() {
    $("#saveModal").modal();
}
function saveManifest() {
    var scaffManifest = {};
    scaffManifest["name"] = $("#profile-name").val();
    scaffManifest["description"] = $("#profile-description").val();
    scaffManifest["username"] = sessionStorage.getItem("username");
    scaffManifest["data"] = manifest;

    // Save to DB
    var apiUrl = baseUrl + '/StackV-web/restapi/app/profile/new';
    $.ajax({
        url: apiUrl,
        type: 'PUT',
        data: JSON.stringify(scaffManifest),
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
        },
        success: function (result) {
            window.location.href = "/StackV-web/ops/catalog.jsp?profiles=open";
        }
    });
}

// TESTING

function preloadDNC() {
    $("#meta-alias").val("Preloaded DNC Test");
    $("#connections-type").val("Multi-Point VLAN Bridge");

    setTimeout(function () {
        $("button[data-factory=connections-connection-terminal]").click();
        $("#connections-connection_num1-name").val("mpvb1");

        $("#connections-connection_num1-terminal_num1-uri").val("urn:ogf:network:odl.maxgigapop.net:network:node=openflow_1:port=openflow_1_1");
        $("#connections-connection_num1-terminal_num1-vlan_tag").val("101");

        $("#connections-connection_num1-terminal_num2-uri").val("urn:ogf:network:odl.maxgigapop.net:network:node=openflow_2:port=openflow_2_1");
        $("#connections-connection_num1-terminal_num2-vlan_tag").val("102");
        $("#connections-connection_num1-terminal_num2-mac_address_list").val("02:50:f2:00:00:01,02:50:f2:00:00:04");

        $("#connections-connection_num1-terminal_num3-uri").val("urn:ogf:network:odl.maxgigapop.net:network:node=openflow_3:port=openflow_3_1");
        $("#connections-connection_num1-terminal_num3-vlan_tag").val("103");
    }, 500);
}
function preloadAWSVCN() {
    $("#meta-alias").val("Preloaded VCN AWS Test");

    $("#details-network-parent").val("urn:ogf:network:aws.amazon.com:aws-cloud");
    $("#details-network-type").val("internal");
    $("#details-network-cidr").val("10.0.0.0/24");

    $("#subnets-subnet_num1-name").val("TestSub1");
    $("#subnets-subnet_num1-cidr_block").val("10.0.0.0/24");
    $("#subnets-subnet_num1-route_num1-from").val("TestFrom");
    $("#subnets-subnet_num1-route_num1-to").val("TestTo");
    $("#subnets-subnet_num1-route_num1-next_hop").val("TestNext");

    $("#vms-vm_num1-name").val("TestVM1");
    $("#vms-vm_num1-subnet_host").val("subnet_num1");
    $("#vms-vm_num1-keypair_name").val("driver_key");
    $("#vms-vm_num1-security_group").val("geni");
    $("#vms-vm_num1-route_num1-from").val("TestFrom");
    $("#vms-vm_num1-route_num1-to").val("TestTo");
    $("#vms-vm_num1-route_num1-next_hop").val("TestNext");

    $("#gateways-gateway_num1-name").val("TestGate");
    $("#gateways-gateway_num1-route_num1-to").val("TestTo");
}
function preloadOPSVCN() {
    $("#meta-alias").val("Preloaded VCN OPS Test");
    //$("[name=block-gateways]").val(2);
    //$("[name=block-sriovs]").val(2);

    setTimeout(function () {
        $("#details-network-parent").val("urn:ogf:network:aws.amazon.com:openstack-cloud");
        $("#details-network-type").val("internal");
        $("#details-network-cidr").val("10.0.0.0/16");

        $("#subnets-subnet_num1-name").val("subnet1");
        $("#subnets-subnet_num1-cidr").val("10.0.0.0/24");
        $("#subnets-subnet_num1-default_routing").prop("checked", true);

        $("#vms-vm_num1-name").val("ops-vtn1-vm1");
        $("#vms-vm_num1-subnet_host").val("subnet_num1");
        $("#vms-vm_num1-keypair_name").val("demo-key");
        $("#vms-vm_num1-security_group").val("rains");
        $("#vms-vm_num1-route_num1-to").val("0.0.0.0/0");
        $("#vms-vm_num1-route_num1-next_hop").val("206.196.179.145");

        $("#gateways-gateway_num1-name").val("ceph-net");
        $("#gateways-gateway_num1-type").val("UCS Port Profile");
        $("#gateways-gateway_num1-route_num1-from").val("Ceph-Storage");
        $("#gateways-gateway_num1-route_num1-type").val("port_profile");
        $("#gateways-gateway_num2-name").val("ext-net");
        $("#gateways-gateway_num2-type").val("UCS Port Profile");
        $("#gateways-gateway_num2-route_num1-from").val("External-Access");
        $("#gateways-gateway_num2-route_num1-type").val("port_profile");

        $("#sriovs-sriov_num1-hosting_gateway").val("gateway_num1");
        $("#sriovs-sriov_num1-hosting_vm").val("vm_num1");
        $("#sriovs-sriov_num1-name").val("ops-vtn1:vm2:eth2");
        $("#sriovs-sriov_num1-ip_address").val("10.10.200.164");
        $("#sriovs-sriov_num1-mac_address").val("aa:bb:cc:ff:01:12");
        $("#sriovs-sriov_num2-hosting_gateway").val("gateway_num2");
        $("#sriovs-sriov_num2-hosting_vm").val("vm_num1");
        $("#sriovs-sriov_num2-name").val("ops-vtn1:vm1:eth1");
        $("#sriovs-sriov_num2-ip_address").val("206.196.179.157");
        $("#sriovs-sriov_num2-mac_address").val("aa:bb:cc:dd:01:57");
    }, 500);
}
function preloadAHC() {
    $("#meta-alias").val("Preloaded AHC Test");

    $("#network-aws-parent").val("urn:ogf:network:aws.amazon.com:aws-cloud");
    $("#network-aws-cidr").val("10.0.0.0/16");

    $("#network-openstack-parent").val("urn:ogf:network:openstack.com:openstack-cloud");
    $("#network-openstack-cidr").val("10.1.0.0/16");

    $("#subnets-aws_subnet_num1-name").val("subnet1");
    $("#subnets-aws_subnet_num1-cidr").val("10.0.0.0/24");

    $("#subnets-aws_subnet_num1-route_num1-from").val("vpn");
    $("#subnets-aws_subnet_num1-route_num1-to").val("0.0.0.0/0");
    $("#subnets-aws_subnet_num1-route_num1-next_hop").val("vpn");

    $("#subnets-openstack_subnet_num1-name").val("subnet1");
    $("#subnets-openstack_subnet_num1-cidr").val("10.1.0.0/24");

    $("#gateways-gateway_num1-name").val("intercloud-1");
    $("#gateways-gateway_num1-type").val("Inter Cloud Network");

    $("#vms-aws_vm_num1-name").val("vm1");
    $("#vms-aws_vm_num1-keypair_name").val("driver_key");
    $("#vms-aws_vm_num1-security_group").val("geni");
    $("#vms-aws_vm_num1-image").val("ami-0d1bf860");
    $("#vms-aws_vm_num1-instance_type").val("m4.large");
    $("#vms-aws_vm_num1-interface_num1-type").val("Ethernet");

    $("#vms-openstack_vm_num1-name").val("vtn2-vm1");
    $("#vms-openstack_vm_num1-keypair_name").val("demo-key");
    $("#vms-openstack_vm_num1-security_group").val("rains");
    $("#vms-openstack_vm_num1-instance_type").val("5");

    $("#vms-openstack_bgp-asn").val("7224");
    $("#vms-openstack_bgp-authentication_key").val("versastack");
    $("#vms-openstack_bgp-networks").val("10.10.0.0/16");

    $("#vms-openstack_vm_num1-route_num1-to").val("10.10.0.0/16");
    $("#vms-openstack_vm_num1-route_num1-next_hop").val("10.1.0.1");

    $("#vms-openstack_vm_num1-interface_num1-name").val("vtn2-vm1:eth1");
    $("#vms-openstack_vm_num1-interface_num1-type").val("SRIOV");
    $("#vms-openstack_vm_num1-interface_num1-address").val("ipv4+10.10.0.1/24,mac+aa:bb:cc:ff:01:11");
}