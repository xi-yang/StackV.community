"use strict";
define(["local/stackv/utils"], function (utils) {
    // Utility functions
    var bsShowFadingMessage = utils.bsShowFadingMessage;
    var loadCSS = utils.loadCSS;
    var renderTemplate = utils.renderTemplate;
    var getAuthentication = utils.getAuthentication;
    var loadSettings = utils.loadSettings;
    
    // should have option for seperate css file as well 
    function ServicePanel() {
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
            root_container: "servicePanel",
            
            opener: "servicePanel-tab",
                        
            table_container: "servicePanel-contents",
            table_body: "servicePanel-body",
            table_cell_class: "service-instance-item",
            table_cell_highlighted_class: "service-instance-highlighted", 
            error_text_class: "service-unready-message",
            
            template_root: "/StackV-web/data/viz_templates/",
            template_filename: "ServicePanel.html",
            css_root: "/StackV-web/css/visualization/modules/", 
            css_filename: "ServicePanel.css"
         };
        
         var settings = {};
         var auth = {}; // keycloack authentication info
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
            auth = getAuthentication();
            var username = auth.username;
            var subject = auth.subject;
            var token = auth.token;
            var tbody = document.getElementById(settings.table_body);
            var baseUrl = window.location.origin;

            var apiUrl = baseUrl + '/StackV-web/restapi/app/panel/' + subject + '/instances';
            $.ajax({
                url: apiUrl,
                type: 'GET',
                beforeSend: function (xhr) {
                    xhr.setRequestHeader("Authorization", "bearer " + token);
                },
                success: function (result) {
                    for (var i = 0; i < result.length; i++) {
                        var instance = result[i];

                        var row = document.createElement("tr");
                        row.setAttribute("id", instance[1]);
                        row.setAttribute("class", settings.table_cell_class);

                        var cell1_1 = document.createElement("td");
                        cell1_1.innerHTML = instance[3];
                        var cell1_2 = document.createElement("td");
                        cell1_2.innerHTML = instance[0];
                        var cell1_4 = document.createElement("td");
                        cell1_4.innerHTML = instance[2];
                        row.appendChild(cell1_1);
                        row.appendChild(cell1_2);
                        row.appendChild(cell1_4);
                        tbody.appendChild(row);
                    }
                    initServiceInstanceItems();
                },

                error: function (jqXHR, textStatus, errorThrown) {
                    console.log("No service instances.");

                }
            });
            
           $("#" + settings.opener).click(function (evt) {
              openPanel();
              evt.preventDefault();
           });
           
         };  
          
         
        function openPanel() {
            $("#" + settings.root_container).toggleClass("closed");
        }

        function initServiceInstanceItems() {
            $("." + settings.table_cell_class).each(function () {
                var that = this;
                var DELAY = 700, clicks = 0, timer = null;

                $(that).click(function () {
                    clicks++;  //count clicks

                    if (clicks === 1) {
                        timer = setTimeout(function () {
                            clickServiceInstanceItem(that);
                            clicks = 0;
                        }, DELAY);
                    } else {
                        clearTimeout(timer);    //prevent single-click action
                        if ($(that).hasClass(settings.table_cell_highlighted_class)) {
                            $("." + settings.table_cell_class + 
                              "." + settings.table_cell_highlighted_class).removeClass(settings.table_cell_highlighted_class);
                            
//                            render.API.setHighlights([], "serviceHighlighting");
//                            render.API.highlightElements("serviceHighlighting");                        
                            
                            clicks = 0;             //after action performed, reset counter
                        } else {
                            timer = setTimeout(function () {
                                clickServiceInstanceItem(that);
                                clicks = 0;
                            }, DELAY);
                        }
                    }
                }).dblclick(function (e) {
                    e.preventDefault();
                });
            });
        }

        function clickServiceInstanceItem(item) {
            var UUID = $(item).attr('id');
            auth = getAuthentication();
            var token = auth.token;

            $.ajax({
                crossDomain: true,
                type: "GET",
                url: "/StackV-web/restapi/app/service/availibleitems/" + UUID,
                beforeSend: function (xhr) {
                   xhr.setRequestHeader("Authorization", "bearer " + token);
                },
                dataType: "json",

                success: function (data, textStatus, jqXHR) {
                    if (data === null) {
                        bsShowFadingMessage("#" + settings.root_container, "Data not found", "top", 1000);
                    } else {
                        $("." + settings.table_cell_class + 
                          "." + settings.table_cell_highlighted_class).removeClass(settings.table_cell_highlighted_class);
                        $(item).addClass(settings.table_cell_highlighted_class);
                        
                        //alert(data);
                        // Union of verified addition and unverified reduction
                        var unionObj = data;
                        
                        //var result = model.makeSubModel([unionObj]);
                        //var modelArr = model.getModelMapValues(result);
                        var result = PubSub.publish("Model_BuildGenericModel", {
                            map: unionObj
                        });
                        var modelArr =  PubSub.publish("Model_Explode", {
                            model: result
                        }); 

                       // render.API.setHighlights(modelArr, "serviceHighlighting");
                       // render.API.highlightElements("serviceHighlighting");
                        PubSub.subscribe('Renderer_AddHighlight', {
                            elements: modelArr,
                            highlight_name: "serviceHighlighting"
                        });
                    }
                },

                error: function (jqXHR, textStatus, errorThrown) {
                    //alert("Error getting status.");
                    alert("textStatus: " + textStatus + " errorThrown: " + errorThrown);
                }
            });
        }
         
        function subscribeToMediatior() {
            if (window.PubSub !== undefined) {
               /*
                * initerface
                *             
                */
               PubSub.subscribe('ServicePanel_Show', function(data) {
                   $('#' + settings.table_container).removeClass("hide");
               });
               
               PubSub.subscribe('ServicePanel_Hide', function(data) {
                   $('#' + settings.table_container).addClass("hide");
               });

               PubSub.subscribe('ServicePanel_SetErrorText', function(data) {
                   $('#' + settings.table_container).html(data.text).addClass(settings.error_text_class);
               });


            }
        }
         
         return {
             init: init         };
       };
       return ServicePanel;
     });
    