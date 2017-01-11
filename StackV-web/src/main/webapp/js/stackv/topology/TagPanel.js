"use strict";
define(["local/stackv/utils"], function (utils) {
    // Utility functions
    var bsShowFadingMessage = utils.bsShowFadingMessage;
    var loadCSS = utils.loadCSS;
    var renderTemplate = utils.renderTemplate;
    var getAuthentication = utils.getAuthentication;
    var loadSettings = utils.loadSettings;
    
    // should have option for seperate css file as well 
    function TagPanel() {
         /* Settings: 
          *   Users can provide optional settings. 
          *   
          *   Among other things, this object contains identifiers and classes 
          *   that are used by the component's logic.  If the user decides 
          *   to change the html or csss of the component, they can simply 
          *   pass in the changed values and the component's logic will be 
          *   intact. Styling and markup issues that don't affect the component's
          *   logic are handled in its css and html files. 
          *       
          * Settings: {
          *    // general 
          *       template_root: path of html
          *       template_filename: filename of html
          *       css_root: path of css 
          *       css_filename: filename of css
          *       
          *    // ids
          *      root_container: id of root container for component
          *      opener: id of component that opens tag panel on click
          *      clear_tags_button: button that clears tags on click
          *      tag_list:  id of unordered list (ul) that stores tags 
          *    
          *    // classes 
          *      tag_class: css class for tags
          *      color_box_class: css class for color selection classes
          *      color_box_highlighted_class: css class for color highlihgt effect
          *      tag_deletion_icon_classes: ex. fa fa-times tagDeletionIcon
          */

         var defaults = {
            parent_container: "tag-panel",
            root_container: "tagPanel",
            opener: "tagPanel-tab",
            tag_list:  "labelList1",
            clear_tags_button: "ClearAllTagsButton",
            tag_class: "tagPanel-labelItem",
            tag_deletion_icon_classes:  "fa fa-times tagDeletionIcon",    
            color_box_class: "filteredColorBox",
            color_box_highlighted_class: "colorBox-highlighted",         
            template_root: "/StackV-web/data/viz_templates/",
            template_filename: "TagPanel.html",
            css_root: "/StackV-web/css/visualization/modules/", 
            css_filename: "TagPanel.css"
         };
        
         var settings = {};
         var auth = {}; // keycloack authentication info
         var tags = []; // stores tag objects {color, data, label}
         var selectedColors = []; // colors selected for filtering
         var colorIcons;
         var tagObjects;
         var userName;
         
         function init(Settings) {
             settings = loadSettings(Settings, defaults);       
             // Idea, specific "build" function that is used as callback 
             // after rendering of template. Perhaps have all of these modules
             // inherit from a Component module that has all of these 
             renderTemplate(settings, buildPanel.bind(undefined));
             subscribeToMediatior();
         };
         
                          
         function buildPanel() {
             
             tagObjects = document.getElementsByClassName(settings.tag_class);
             colorIcons = document.getElementsByClassName(settings.color_box_class);

             // might want to use module for htis 
             var auth = getAuthentication();
             var userName = auth.username;
             
             var baseUrl = window.location.origin;

             if(auth.loggedIn) {
                 // Populate tags
                 $.ajax({
                    // crossDomain: true,
                     type: "GET",
                     url: baseUrl+"/StackV-web/restapi/app/label/" + userName,
                     dataType: "json",
                     beforeSend: function (xhr) {
                        xhr.setRequestHeader("Authorization", "bearer " + auth.token);
                     },

                     success: function(data,  textStatus,  jqXHR ) {
                         for (var i = 0, len = data.length; i < len; i++) {
                             var dataRow = data[i];
                             createTag(dataRow[0], dataRow[1], dataRow[2]);
                         }
                     },

                     error: function(jqXHR, textStatus, errorThrown ) {
                        console.log(errorThrown + "\n"+textStatus);
                        console.log("Error retrieving tags.");
                     }                  
                 });
             }
             
             // Initialize color box toggles
             for (var i = 0; i < colorIcons.length;  i++) {
                 colorIcons[i].onclick = function() {
                     var selectedColor = this.id.split("box")[1].toLowerCase();
                     var selectedIndex = selectedColors.indexOf(selectedColor);
                     if (selectedIndex === -1) {
                         selectedColors.push(selectedColor);
                         this.classList.add(settings.color_box_highlighted_class);
                     } else {
                         selectedColors.splice(selectedIndex, 1);
                         this.classList.remove(settings.color_box_highlighted_class);
                     }      

                     updateTagList();
                 };
             }

             $("#" + settings.clear_tags_button).click(function() {
                 var auth = getAuthentication();

                 $.ajax({
                     crossDomain: true,
                     type: "DELETE",
                     url: "/StackV-web/restapi/app/label/" + userName + "/clearall",
                     beforeSend: function (xhr) {
                         xhr.setRequestHeader("Authorization", "bearer " + auth.token);
                     },

                     success: function(data,  textStatus,  jqXHR ) {
                         bsShowFadingMessage("#" + settings.root_container, "Tags Cleared", "top", 1000);
                         $("#" + settings.tag_list).empty();
                     },

                     error: function(jqXHR, textStatus, errorThrown ) {
                         bsShowFadingMessage("#" + settings.root_container, "Error clearing tags.", "top", 1000);
                     }                  
                 });                
             });   
             
             // Tag panel opener 
             $("#" + settings.opener).click(function (evt) {
                  openPanel();
                  evt.preventDefault();
             });
         };  
          
        function updateTagList() {
            var tagObjects = document.getElementsByClassName(settings.tag_class);
            for( var i = 0; i < tagObjects.length; i++){
                var curTag = tagObjects.item(i);
                var curColor = curTag.classList.item(1).split("label-color-")[1];
                if (selectedColors.length === 0) {
                    curTag.classList.remove("hide");
                } else if (selectedColors.indexOf(curColor) === -1){
                    curTag.classList.add("hide");
                } else {
                    curTag.classList.remove("hide");
                }
            }
         };
         
         function openPanel() {
            $("#" + settings.root_container).toggleClass("closed");
         }

         function createTag(label, data, color) {
             var tagList = document.querySelector("#" + settings.tag_list);
             var tag = document.createElement("li");
             tag.classList.add(settings.tag_class);
             tag.classList.add("label-color-" + color.toLowerCase());

             var x = document.createElement("i");
             // ex. fa fa-times tagDeletionIcon
             var icon_classes = settings.tag_deletion_icon_classes.split(" ");
             x.classList.add(icon_classes[0]);
             x.classList.add(icon_classes[1]);
             x.classList.add(icon_classes[2]);             
             x.onclick = deleteTag.bind(undefined, label, tag, tagList);

             tag.innerHTML = label;
             tag.appendChild(x);

             tag.onclick = function(e) {
                 // Don't fire for events triggered by children. 
                 if (e.target !== this)
                     return;
                 
                 // might want to abstract this out 
                 var textField = document.createElement('textarea');
                 textField.innerText = data;
                 document.body.appendChild(textField);
                 textField.select();
                 document.execCommand('copy');
                 $(textField).remove();   
                 bsShowFadingMessage("#" + settings.root_container, "Data copied to clipboard.", "top", 1000);
             };
             tagList.appendChild(tag);
         };

         function deleteTag(identifier, htmlElement, list) {
            var auth = getAuthentication();

            $.ajax({
                crossDomain: true,
                type: "DELETE",
                url: "/StackV-web/restapi/app/label/" + userName + "/delete/" + identifier,
                beforeSend: function (xhr) {
                   xhr.setRequestHeader("Authorization", "bearer " + token);
                },

                success: function(data,  textStatus,  jqXHR ) {
                    bsShowFadingMessage("#" + settings.root_container, "Tag Deleted", "top", 1000);
                    list.removeChild(htmlElement);
                },

                error: function(jqXHR, textStatus, errorThrown ) {
                    bsShowFadingMessage("#" + settings.root_container, "Error deleting tag.", "top", 1000);
                }                  
            });                
         };
         
         function subscribeToMediatior() {
             if (window.PubSub !== undefined) {
                /*
                 * initerface: 
                 *    identifier
                 *    label
                 *    color         
                 */
                PubSub.subscribe('TagPanel_createTag', function(data) {
                    createTag(data.label, data.identifier, data.color);
                });     

                PubSub.subscribe('TagPanel_deleteTag', function(data) {

                });
             }
         }
         
         return {
             init: init,
             createTag: createTag,
             deleteTag: deleteTag
         };
       };
       return TagPanel;
     });
    