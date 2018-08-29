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
    
    function ContextMenu(d3, renderApi, functionMap) {
        var positionUsingPointer = utils.positionUsingPointer;
        var getElementPosition = utils.getElementPosition;
        var getRenderedElementParentDiv = utils.getRenderedElementParentDiv;

        var d3 = d3; // d3 context 
        var renderApi = renderApi;
        var that = this;
        var tagDialog = tagDialog;
        var functionMap = functionMap;

        // Helper functions
        /**
         * Function to check if we clicked inside an element with a particular class
         * name.
         * 
         * @param {Object} e The event
         * @param {String} className The class name to check against
         * @return {Boolean}
         */
        function clickInsideElement( e, className ) {
          var el = e.srcElement || e.target;

          if ( el.classList.contains(className) ) {
            return el;
          } else {
            while ( el = el.parentNode ) {
              if ( el.classList && el.classList.contains(className) ) {
                return el;
              }
            }
          }

          return false;
        }

        /**
         * Get's exact position of event.
         * 
         * @param {Object} e The event passed in
         * @return {Object} Returns the x and y position
         */
        function getPosition(e) {
          var posx = 0;
          var posy = 0;

         if (!e) { 
              var e = window.event;
              //console.log("ContextMenu: getPosition: e passed in is null. ");
         }
         
          if (e.pageX || e.pageY) {
            posx = e.pageX;
            posy = e.pageY;
            //console.log("I'm in ContextMenu: GetPosition , we used e.pageX and e.pageY");
          } else if (e.clientX || e.clientY) {
            posx = e.clientX + document.body.scrollLeft + document.documentElement.scrollLeft;
            posy = e.clientY + document.body.scrollTop + document.documentElement.scrollTop;
          }
          //console.log("I'm in ContextMenu: GetPostion: posx: " + posx + " , posy: " + posy); 
          return {
            x: posx,
            y: posy
          };
        }

        //////////////////////////////////////////////////////////////////////////////
        //////////////////////////////////////////////////////////////////////////////
        //
        // C O R E    F U N C T I O N S
        //
        //////////////////////////////////////////////////////////////////////////////
        //////////////////////////////////////////////////////////////////////////////

        /**
         * Variables.
         */
        var contextMenuClassName = "context-menu";
        var contextMenuItemClassName = "context-menu__item";
        var contextMenuLinkClassName = "context-menu__link";
        var contextMenuActive = "context-menu--active";

        //var taskItemClassName = "myNode";
        this.selectedObject = null;

        var clickCoords;
        var clickCoordsX;
        var clickCoordsY;

        var menu = document.querySelector("#context-menu");
        var menuItems = menu.querySelectorAll(".context-menu__item");
        var menuState = 0;
        var menuWidth;
        var menuHeight;
        var menuPosition;
        var menuPositionX;
        var menuPositionY;

        var windowWidth;
        var windowHeight;

        /**
         * Initialise our application's code.
         */
        this.init = function() {
            //contextListener();
            clickListener();
            keyupListener();
            resizeListener();
        };

        /**
         * Listens for contextmenu events.
         * so instead of just worrying about e, we pass in e and our data object
         * this data object is passed into menu item listener 
         * okay, we can be sure that you only click one element at a time
         * wait no, menu itemListener, down there, is taking the name of the menuItem being accessed
         * , we woul actually pass in what we're looking for here, that would turn out to be the 
         * role that taskItemInContext is playing right now 
         * 
         * again, issue with this i what if someone whats to do multiple objects are once ..
         * right now, lets assume it's one , 
         * 
         * this here taskItemInContext, it's going to be passed to our dialog
         * 
         * so, to clairfy
         * this thing is loaded into dojo and then passed into outputApi in render
         * there everything will get a hold of it. 
         * It is going to be on the graphTopo.jsp page 
         * 
         * each svg node will set it's oncontextmenu event to this, passing in its object
         * we will use these objects based on their type 
         * 
         * lets set the document to do the things in the 'else' clause by default, if nothing is net
         * 
         * okay... now, after that 
         * 
         * we check for clicks, 
         * that code is fine
         * everything else is fine
         * just need to change nodes and get this working with dojo 
         */
        function contextListener() {
            document.addEventListener( "contextmenu", function(e) {
               this.selectedDataObject = null; //maybe we don't want this to become null maybe someone is using this
                                               // when we close the menu... , maybe it doesn't matter and we don't have
                                               // to set it null 
               toggleMenuOff(); 
             });
        }
        
        // different one for display menu 
        this.renderedElemContextListener = function(o) {
          that.selectedObject = o;
          d3.event.preventDefault();
          toggleMenuOn();
          positionMenu(d3.event);
        };
        
        this.panelElemContextListener = function(e, o) {
          that.selectedObject = o;
          e.preventDefault();
          toggleMenuOn();
          positionMenu(e);
        };
        
        /**
         * Listens for click events.
         */
        function clickListener() {
          document.addEventListener( "click", function(e) {
            var clickeElIsLink = clickInsideElement( e, contextMenuLinkClassName );

            if ( clickeElIsLink ) {
              e.preventDefault();
              menuItemListener( clickeElIsLink, e );
            } else {
              var button = e.which || e.button;
              if ( button === 1 ) {
                toggleMenuOff();
              }
            }
          });
        }

        /**
         * Listens for keyup events.
         */
        function keyupListener() {
          window.onkeyup = function(e) {
            if ( e.keyCode === 27 ) {
              toggleMenuOff();
            }
          }
        }

        /**
         * Window resize event listener
         */
        function resizeListener() {
          window.onresize = function(e) {
            toggleMenuOff();
          };
        }

        /**
         * Turns the custom context menu on.
         */
        function toggleMenuOn() {
          if ( menuState !== 1 ) {
            menuState = 1;
            var deleteItem = $("#context-menu").find("[data-action=\"Delete\"]").closest("li");
            
            if (renderApi.multipleHighlighted() ) {
                deleteItem.addClass("hide");
            } else {
                if (deleteItem.hasClass("hide")) {
                    deleteItem.removeClass("hide");
                }
            }
            menu.classList.add( contextMenuActive );
          }
        }

        /**
         * Turns the custom context menu off.
         */
        function toggleMenuOff() {
          if ( menuState !== 0 ) {
            menuState = 0;
            menu.classList.remove( contextMenuActive );
          }
        }

        /**
         * Positions the menu properly.
         * 
         * @param {Object} e The event
         */
        function positionMenu(e) {
          clickCoords = getPosition(e);
          clickCoordsX = clickCoords.x;
          clickCoordsY = clickCoords.y;

          menuWidth = menu.offsetWidth + 4;
          menuHeight = menu.offsetHeight + 4;
          
          windowWidth = window.innerWidth;
          windowHeight = window.innerHeight;

//          if ( (windowWidth - clickCoordsX) < menuWidth ) {
//            menu.style.left = windowWidth - menuWidth + "px";
//          } else {
            menu.style.left = clickCoordsX + "px";
         // }
          
//          if ( (windowHeight - clickCoordsY) < menuHeight ) {
//            menu.style.top = windowHeight - menuHeight + "px";
//          } else {
            menu.style.top = clickCoordsY + "px";
         // }
          
          //console.log("in positionMenu: menu.style.left: " + menu.style.left + " , menu.style.top: " + menu.style.top);
        }
        
        function positionDisplayPanel(elementID, e) {
            var clickCoords = getElementPosition(e);
            var clickCoordsX = clickCoords.x;
            var clickCoordsY = clickCoords.y;

            var element = document.querySelector("#" + elementID);

            var elemWidth = element.offsetWidth + 4;
            var elemHeight = getHeight("#" + elementID) + 4; //element.offsetHeight + 4;

            var windowWidth = window.innerWidth;
            var windowHeight = window.innerHeight;

            element.style.left = clickCoordsX + 20 + "px";
            element.style.top = clickCoordsY - elemHeight + "px";
            
        }
        
        function getHeight(elementName) {
            var previousCss  = $(elementName).attr("style");

            $(elementName)
                .css({
                    position:   'absolute', // Optional if #myDiv is already absolute
                    visibility: 'hidden',
                    display:    'block'
                });

            var optionHeight = $(elementName).height();

            $(elementName).attr("style", previousCss ? previousCss : "");
            return optionHeight;
        }
        
        /**
         * 
         * @param {HTMLElement} link The link that was clicked
         */
        function menuItemListener( link, event ) {
            if (!functionMap) {           
                console.log("Debugging: in ContextMenu.js::: No functionality available.");
                return;
            }
            
            var funcName = link.getAttribute("data-action");
            if (funcName) {
                var func = functionMap[funcName];
                if (func) {
                    if (!renderApi.multipleHighlighted()) {
                        switch(funcName) {
                            case "Tag": func.openDialog(that.selectedObject); break;
                            case "ModelBrowser":
                                //positionDisplayPanel("displayPanel", event);
                                func(that.selectedObject, getRenderedElementParentDiv(that.selectedObject), event); 
                                break;
                            case "Delete": break;
                        }
                    } else {
                        switch (funcName) {
                            case "AddToTrashcan": func.addItems(renderApi.getTrashcan()); break;
                        }
                    }
                } else {
                    console.log("Debugging: in ContextMenu.js::: Menu Item Not Found");
                }
            } else {
                console.log("Debugging: in ContextMenu.js::: Context menu data-attribute not found.")
            }
            
            toggleMenuOff();
        }

    }
    return ContextMenu;    
});
