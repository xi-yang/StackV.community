"use strict";
define([], function () {
    function map_(arr, f) {

        var ans = new Array(arr.length);
        for (var i = 0; i < arr.length; i++) {
            ans[i] = f(arr[i]);
        }
        return ans;
    }

    function deleteAllChildNodes(elem) {
        while (elem.firstChild) {
            elem.removeChild(elem.firstChild);
        }
    }
    
    function bsShowFadingMessage(container, message, position, delay){
        $(container).popover({content: message, placement: position, trigger: "manual"});
        $(container).popover("show");
        setTimeout(
          function(){
            $(container).popover('hide');
            $(container).popover('destroy');
          },  delay);                                                         
    }
    
    // Get's exact position of event and returns it as an object with x, y 
    // properties. 
    function getElementPosition(e) {
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
    
    function positionUsingPointer(elementID, e) {
        var clickCoords = getElementPosition(e);
        var clickCoordsX = clickCoords.x;
        var clickCoordsY = clickCoords.y;
                  
        var element = document.querySelector("#" + elementID);

        var elemWidth = element.offsetWidth + 4;
        var elemHeight = element.offsetHeight + 4;

        var windowWidth = window.innerWidth;
        var windowHeight = window.innerHeight;

        if ( (windowWidth - clickCoordsX) < elemWidth ) {
          element.style.left = windowWidth - elemWidth + "px";
        } else {
          element.style.left = clickCoordsX + "px";
        }

        if ( (windowHeight - clickCoordsY) < elemHeight ) {
          element.style.top = windowHeight - elemHeight + "px";
        } else {
          element.style.top = clickCoordsY + "px";
        }
          
          //console.log("in positionMenu: menu.style.left: " + menu.style.left + " , menu.style.top: " + menu.style.top);
    }
    
    function isURL(str) {
        var urlPattern = /(http|ftp|https):\/\/[\w-]+(\.[\w-]*)+([\w.,@?^=%&amp;:\/~+#-]*[\w@?^=%&amp;\/~+#-])?/;
        return urlPattern.test(str);
    }
    
    function getRenderedElementParentDiv(elem) {
       return  $(elem.svgNode.node()).closest('.details_viz').attr('id'); 
    }
    /** PUBLIC INTERFACE **/
    return {
        map_: map_,
        deleteAllChildNodes: deleteAllChildNodes,
        bsShowFadingMessage: bsShowFadingMessage,
        positionUsingPointer: positionUsingPointer,
        getElementPosition: getElementPosition, 
        isURL: isURL,
        getRenderedElementParentDiv: getRenderedElementParentDiv,
        isFirefox: function() {
            return typeof InstallTrigger !== 'undefined';
        }
    };
    /** END PUBLIC INTERFACE **/

});