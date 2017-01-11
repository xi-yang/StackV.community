/*
 * Copyright (c) 2013-2016 University of Maryland
 * Created by: Antonio Heard 2016

 * Permission is hereby granted, free of charge, to any person obtaining a copy 
 * of this software and/or hardware specification (the “Work”) to deal in the 
 * Work without restriction, including without limitation the rights to use, 
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of 
 * the Work, and to permit persons to whom the Work is furnished to do so, 
 * subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Work.

 * THE WORK IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL 
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
 * FROM, OUT OF OR IN CONNECTION WITH THE WORK OR THE USE OR OTHER DEALINGS  
 * IN THE WORK.
 */

"use strict";
define(["local/stackv/utils"], function (utils) {
    var bsShowFadingMessage = utils.bsShowFadingMessage;
    var loadCSS = utils.loadCSS;
    var renderTemplate = utils.renderTemplate;
    var getAuthentication = utils.getAuthentication;
    var loadSettings = utils.loadSettings;

    function TagDialog (Settings) {
        var currentColor = null;
        var label = null;
        var identifier = null;
        var username;
        var selectedColorBox = null;
        
        
        var defaults = {
           parent_container: "tag-panel",
           root_container: "tagDialog",
           input_label: "tagDialogLabelInput",
           color_box_class: "colorBox",
           color_box_highlighted_class: "colorBox-highlighted",         
           template_root: "/StackV-web/data/viz_templates/",
           template_filename: "TagDialog.html",
           css_root: "/StackV-web/css/visualization/modules/", 
           css_filename: "TagDialog.css"
        };
        var settings;
        
        var dialog;
        
        var colorBoxes;
        
        function init(Settings) {
             settings = loadSettings(Settings, defaults);
             renderTemplate(settings, buildDialog.bind(undefined));
             subscribeToMediatior();
        };
        
        
        function buildDialog() {
             colorBoxes = document.getElementsByClassName(settings.color_box_class);
             dialog = $( "#" + settings.root_container ).dialog({
              autoOpen: false,
              height: 400,
              width: 350,
              buttons: {
                "OK": createLabel,
                Cancel: function() {
                  dialog.dialog( "close" );
                }
              },
              close: function() {
              }
            });
            
            for (var i = 0; i < colorBoxes.length;  i++) {
                colorBoxes[i].onclick = function() {
                    if (selectedColorBox) {
                         selectedColorBox.classList.remove(settings.color_box_highlighted_class);
                    }
                    selectedColorBox = this;
                    selectedColorBox.classList.add(settings.color_box_highlighted_class);
                    currentColor = selectedColorBox.id.split("box")[1];
                 };
            }            
        }
        
        function createLabel() {
            label = document.getElementById(settings.input_label).value;
            
            var auth = getAuthentication();
            username = auth.username;
            var token = auth.token;
            
            if (username === ""){
                alert("Error: Please log in to submit tags. ");
                closeDialog();
            }


            var serializedData = JSON.stringify({
                    user:  username,
                    identifier: label,
                    label: identifier,
                    color: currentColor.toLowerCase()
            });
           
            $.ajax({
                crossDomain: true,
                type: "PUT",
                url: "/StackV-web/restapi/app/label",
                beforeSend: function (xhr) {
                    xhr.setRequestHeader("Authorization", "bearer " + token);
                },
                data: serializedData,
                contentType: "application/json", 

                success: function(data,  textStatus,  jqXHR ) {
                    PubSub.publish("TagPanel_createTag", {
                        identifier: identifier,
                        label: label,
                        color: currentColor.toLowerCase()
                    });
                    bsShowFadingMessage("#" + settings.root_container, "Tag added.", "top", 1000);                       
                    closeDialog();
                },
                error: function(jqXHR, textStatus, errorThrown ) {
                   //alert(errorThrown + "\n"+textStatus);
                   alert("Error adding tag.");
                   closeDialog();
                }

            });
            var labelInput = document.getElementById(settings.input_label);
            $('#' + settings.input_label).val("");

            labelInput.vallue = ""; 
        }

        function closeDialog() {
            $('#' + settings.input_label).removeAttr('value');
            dialog.close();
        };
        
        function subscribeToMediatior() {
            if (window.PubSub !== undefined) {
               /*
                * initerface: 
                *    identifier
                */
               PubSub.subscribe('TagDialog_open', function(data) {
                   identifier = data.identifier;
                   dialog.open();
               });     
               
               PubSub.subscribe('TagDialog_close', function() {
                   closeDialog();   
               });
            }
        }
    
        return {
          init: init  
        };
    }
    return TagDialog;
});

