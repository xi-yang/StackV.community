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
    var loadSettings = utils.loadSettings;

    function ContextMenu(Setings, menu) {

        var defaults = {
            menu_trigger: ".model_object"
        };
        var menu;
        
        var settings = {};
        var auth = {}; // keycloack authentication info

        /**
         * Initialise our application's code.
         */
        function init(Settings, Menu) {
            settings = loadSettings(Settings, defaults);    
            subscribeToMediatior();
            menu = Menu;
            
            $(settings.menu_trigger).contextMenu(menu, {
                    triggerOn:'contextmenu'
            }); 
        }
        
        function subscribeToMediatior() {
            /*
             * interface:
             *  option_name
             */
          PubSub.subscribe('ContextMenu_enable_option', function(data) {
              var updateObj = [{
                name: data.option_name,
                disable: false
              }];
              $(settings.menu_trigger).contextMenu('update', updateObj, {});
          });     

          PubSub.subscribe('ContextMenu_disable_option', function(data) {
              var updateObj = [{
                name: data.option_name,
                disable: true
              }];              
              $(settings.menu_trigger).contextMenu('update', updateObj, {});
          });
        }   
        
        return {
            init: init
        };
    }
    return ContextMenu;    
});
