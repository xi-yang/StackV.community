/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
"use strict";
define([], function () {
    function ContextMenu(d3, renderApi, tagDialog) {

        var d3 = d3; // d3 context 
        var renderApi = renderApi;
        var that = this;
        var tagDialog = tagDialog;
        //////////////////////////////////////////////////////////////////////////////
        //////////////////////////////////////////////////////////////////////////////
        //
        // H E L P E R    F U N C T I O N S
        //
        //////////////////////////////////////////////////////////////////////////////
        //////////////////////////////////////////////////////////////////////////////

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
              console.log("ContextMenu: getPosition: e passed in is null. ");
         }
         
          if (e.pageX || e.pageY) {
            posx = e.pageX;
            posy = e.pageY;
            console.log("I'm in ContextMenu: GetPosition , we used e.pageX and e.pageY");
          } else if (e.clientX || e.clientY) {
            posx = e.clientX + document.body.scrollLeft + document.documentElement.scrollLeft;
            posy = e.clientY + document.body.scrollTop + document.documentElement.scrollTop;
          }
          console.log("I'm in ContextMenu: GetPostion: posx: " + posx + " , posy: " + posy); 
          return {
            x: posx,
            y: posy
          }
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
            //taskItemInContext = clickInsideElement( e, taskItemClassName );
              this.selectedDataObject = null; //maybe we don't want this to become null maybe someone is using this
                                              // when we close the menu... , maybe it doesn't matter and we don't have
                                              // to set it null 
              toggleMenuOff(); 
          });
        }
        
        // different one for display menu 
        this.setContextListenerRendered = function(o) {
          that.selectedObject = o;
          d3.event.preventDefault();
          toggleMenuOn();
          positionMenu(d3.event);
          renderApi.selectElement(o);
        };
        
        this.setContextListenerPanelObj = function(e, o) {
          that.selectedObject = o;
          e.preventDefault();
          toggleMenuOn();
          positionMenu(e);
          // probably not what we want here 
          //if (o.getType() !== "Property") renderApi.selectElement(o);
        };
        /**
         * Listens for click events.
         */
        function clickListener() {
          document.addEventListener( "click", function(e) {
            var clickeElIsLink = clickInsideElement( e, contextMenuLinkClassName );

            if ( clickeElIsLink ) {
              e.preventDefault();
              menuItemListener( clickeElIsLink );
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
            menu.classList.add( contextMenuActive );
            console.log("i'm here\n\n");
          }
        }

        /**
         * Turns the custom context menu off.
         */
        function toggleMenuOff() {
          if ( menuState !== 0 ) {
            menuState = 0;
            menu.classList.remove( contextMenuActive );
            console.log("i'm here \n\n");
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

          if ( (windowWidth - clickCoordsX) < menuWidth ) {
            menu.style.left = windowWidth - menuWidth + "px";
          } else {
            menu.style.left = clickCoordsX + "px";
          }
          
          if ( (windowHeight - clickCoordsY) < menuHeight ) {
            menu.style.top = windowHeight - menuHeight + "px";
          } else {
            menu.style.top = clickCoordsY + "px";
          }
          
          console.log("in positionMenu: menu.style.left: " + menu.style.left + " , menu.style.top: " + menu.style.top);
        }

        /**
         * Dummy action function that logs an action when a menu item link is clicked
         * 
         * @param {HTMLElement} link The link that was clicked
         */
        function menuItemListener( link ) {
          //console.log( "Task ID - " + this.selectedObject.getAttribute("data-id") + ", Task action - " + link.getAttribute("data-action"));
            //alert("type: " + that.selectedObject.getType());
          // Testing if this is an Element or a property 
            tagDialog.openDialog(that.selectedObject);  
            toggleMenuOff();
        }

        /**
         * Run the app.
         */
        // init(); // call this is graph.topo 

          }
          return ContextMenu;    
});
