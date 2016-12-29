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
    
    function OMMPanel (renderApi)  {
        var that = this;
        var resourceURIList = [];
        that.renderApi = renderApi;
        
        this.init = function() {
            $( function() {
              $( document ).tooltip({
                  hide: false,
                  show: {
                    delay: 2500
                  },
                  "placement": "bottom",
                  'container':'body',
                  'selector': ''


              });
            } );

            $("#ommPanel-tab").click(function (evt) {
                $("#ommPanel").toggleClass("closed");

                evt.preventDefault();
            });

            document.getElementById("ommClearAllCheckedButton").onclick = function() {
                that.clearResources(false);
            };
            document.getElementById("ommClearAlldButton").onclick = function() { 
                that.clearResources(true);
            };
            document.getElementById("ommRestoreButton").onclick = function() { 
                that.clearResources(false);
            };
            
            
        };
        
        this.makeItem = function(newId, urn) {
           var item = "<div id=\"resource-" + newId + "\" class=\"checkbox resource-item\" title=\""+ urn + "\">";
           item += "<label class=\"ellipsis resource-checkbox\"><input type=\"checkbox\" value=\"\">" + urn + "</label>";
           item += "</div>";
           $("#resource-container").append(item);
        };
        function getUniqueId() {
            do {
               var id = Math.floor(Math.random() * (65535 - 0 + 1)) + 0;
            } while (id in resourceURIList);
            return id;
        }
        
        function addEmptyText() {
            if ($(".resource-item").length === 0){
                $("#resource-container").html("Empty.");
            }
        }
        
        function removeEmptyText() {
          if ($("#resource-container").html().indexOf("Empty.") !== -1) {
              $("#resource-container").html(" ");
          }         
        }
        
        this.addItems = function(items) {
          var attemptedAddDuplicate = false;
          removeEmptyText();
          for (var i = 0; i < items.length; i++) {
              var uri = items[i].getName();
             // in operator for getunique id
              if (!resourceURIList.hasOwnProperty(uri)) {
                var newId = getUniqueId();
                resourceURIList[uri] = newId;
                that.makeItem(newId, uri);
              } else {
                  attemptedAddDuplicate = true;
              }
          }  
          addEmptyText();
          if (attemptedAddDuplicate) {
              bsShowFadingMessage("#ommPanel", "Resource already added.", "top", 1000);
          }
          items = []; // stop highlighting after added to panel 
          that.renderApi.redraw();
        };
        
        this.removeItem = function(urn) {
            $("#" + urn).remove();
        };

        this.clearResources = function(all) {
            var for_removal = [];
            if ($(".resource-item").length === 0) {
                bsShowFadingMessage("#ommPanel", "No resources.", "top", 1000);
            } else {            
                $(".resource-item").each(function( index ) {
                      if ($(this).find("input").prop('checked') || (all === true)) {
                        var id = $(this).attr("id"); 
                        var urn = $(this).attr("title");
                        for_removal.push(urn);
                        var rId = id.substring(10, id.length); 
                        delete resourceURIList[urn];
                        that.removeItem(id);       
                      }
                  });
                addEmptyText();
            }
            if (for_removal.length > 0){
                that.deleteElementsFromModel(for_removal);
            }
        };    
        
        this.deleteElementsFromModel = function(uris) {
            var token = sessionStorage.getItem("token");
            $.ajax({
                crossDomain: true,
                type: "POST",
                url: "/StackV-web/restapi/app/service",
                beforeSend: function (xhr) {
                   xhr.setRequestHeader("Authorization", "bearer " + token);
                },
                data: JSON.stringify({
                    type : "omm",
                    username : "admin",
                    alias : "new_servicetest_1",
                    data : {
				removeResource : uris
			}              
                     }),
                contentType: "application/json",     

                success: function (data, textStatus, jqXHR) {
                    console.log("this was successful");
                },

                error: function (jqXHR, textStatus, errorThrown) {
                    //alert("Error getting status.");
                    alert("textStatus: " + textStatus + " errorThrown: " + errorThrown);
                }
            });

        };
    }
    return OMMPanel;
});
