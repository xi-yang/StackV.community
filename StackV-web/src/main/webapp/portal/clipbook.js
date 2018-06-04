"use strict";

/*
* Copyright (c) 2013-2018 University of Maryland
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

/* global XDomainRequest, Mousetrap, iziToast */
var clipbook = {};
const $clipModal = $("#clipbook-modal");
const clipConfig = {
    title: "Clipbook",
    headerColor: "#676e77",
    width: "60vh",
    transitionIn: "fadeInUp",
    transitionOut: "fadeOutDown",
    group: "clip"
};

export function loadClipbook() {
    $clipModal.iziModal(clipConfig);
    $("#clipbook-modal .iziModal-content").append("<div class='clipbook-data'><div class='panel panel-default'><div class='clipbook-add' style='display: none;'></div><div class='list-group clipbook-list'></div><div style='height:50px;'><button class='btn btn-default' id='button-clipbook-add'>Add</button><button style='display: none;' class='btn btn-success' id='button-clipbook-submit'>Submit</button></div></div></div>");
    $(".clipbook-add").append("<div class='form-group'><label for='input-clipbook-name'>Clip Name</label><input type='text' class='form-control' id='input-clipbook-name' placeholder='Name'></div>");
    $(".clipbook-add").append("<div class='form-group'><label for='input-clipbook-text'>Clip Text</label><textarea type='text' class='form-control' id='input-clipbook-text' placeholder='Text'></textarea></div>");

    Mousetrap.bind("command+shift+x", () => { reloadClipboard();$clipModal.iziModal("open"); });
    $(".clipbook-list").on("click", "a", function() {
        copyTextToClipboard($(this).attr("data-clip"));
    });
    $(".clipbook-list").on("click", "span", function() {
        deleteFromClipbook($(this).parent().text());
        syncClipbook();
    });

    clipbook = [
        {
            "name": "testClip1",
            "clip": "urn:ogf:network:service+45e188ec-d820-4e69-a618-08045ec58835:resource+virtual_clouds:tag+vpc1",
            "color": "blue"
        },
        {
            "name": "testClip2",
            "clip": "urn:ogf:network:service+45e188ec-d820-4e69-a618-08045ec58835:resource+virtual_clouds:tag+vpc2",
            "color": "green"
        }
    ];

    syncClipbook();
    $("#button-clipbook-add").click(function() {
        if ($(this).hasClass("btn-default")) {
            toggleClipbook("add");
        } else {
            toggleClipbook("list");
        }
    });
    $("#button-clipbook-submit").click(function() {
        let name = $("#input-clipbook-name").val();
        let text = $("#input-clipbook-text").val();
        addToClipbook(name, text);
        syncClipbook();
        $("#button-clipbook-add").click();
    });
}

function toggleClipbook(mode) {
    switch (mode) {
    case "add":
        // Toggle to add mode.
        $(".clipbook-list").hide();
        $(".clipbook-add").show();
        $("#button-clipbook-add").text("Cancel");
        $("#button-clipbook-submit").show();

        $("#button-clipbook-add").removeClass("btn-default");
        $("#button-clipbook-add").addClass("btn-primary");
        break;
    case "list":
        // Toggle to list mode.
        $(".clipbook-add").hide();
        $("#button-clipbook-add").text("Add");
        $(".clipbook-list").show();
        $("#button-clipbook-submit").hide();

        $("#button-clipbook-add").removeClass("btn-primary");
        $("#button-clipbook-add").addClass("btn-default");
        break;
    }
}

export function openClipbookAdd() {
    $("#input-clipbook-name").val();
    $clipModal.iziModal("open");
    toggleClipbook("add");
}

function syncClipbook() {
    reloadClipboard();
}

function reloadClipboard() {
    var $list = $(".clipbook-list").empty();
    for (let clip of clipbook) {
        if (clip !== undefined) {
            let $item = $("<a class='list-group-item' data-clip='" + clip.clip + "'><p>" + clip.name + "<span style='float:right;z-index:99;' class='glyphicon glyphicon-remove-circle'></span></p><div class='clip-text'>" + clip.clip + "</div></a>");
            $item.css({"box-shadow": clip.color + " 0px 0px 1px 1px inset"});
            $list.append($item);
        }
    }
}

function addToClipbook(name, clip, color) {
    var newClip = {};
    newClip.name = name;
    newClip.clip = clip;
    if (color !== undefined) {
        newClip.color = color;
    }

    clipbook.push(newClip);
}
function deleteFromClipbook(name) {
    for (let i in clipbook) {
        if (clipbook[i] && clipbook[i].name === name) {
            delete clipbook[i];
        }
    }
}
function copyTextToClipboard(text) {
    var textArea = document.createElement("textarea");
    textArea.style.position = "fixed";
    textArea.style.top = 0;
    textArea.style.left = 0;
    textArea.style.width = "2em";
    textArea.style.height = "2em";
    textArea.style.padding = 0;
    textArea.style.border = "none";
    textArea.style.outline = "none";
    textArea.style.boxShadow = "none";
    textArea.style.background = "transparent";

    textArea.value = text;

    document.body.appendChild(textArea);
    textArea.focus();
    textArea.select();

    try {
        document.execCommand("copy");
        iziToast.show({
            title: "Copied!",
            position: "topRight",
            color: "green",
            timeout: "2000"
        });
    } catch (err) {
        iziToast.show({
            title: "Error!",
            position: "topRight",
            color: "red",
            timeout: "4000"
        });
    }

    document.body.removeChild(textArea);
}
