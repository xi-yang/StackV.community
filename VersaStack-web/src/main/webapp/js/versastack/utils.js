/*
 * Copyright (c) 2013-2016 University of Maryland
 * Modified by: Antonio Heard 2016

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

    function isURL(str) {
        var urlPattern = /(http|ftp|https):\/\/[\w-]+(\.[\w-]*)+([\w.,@?^=%&amp;:\/~+#-]*[\w@?^=%&amp;\/~+#-])?/;
        return urlPattern.test(str);
    }
    
    function getRenderedElementParentDiv(elem) {
       return  $(elem.svgNode.node()).closest('.details_viz').attr('id'); 
    }
   
    function GetJsonString(str) {
        var json;
        try {
            json = JSON.parse(str);
        } catch (e) {
            return null;
        }
        return json;
    }
    
    function formatPolicyData(data) {
        var json = GetJsonString(data);
        if (json !== null) {
            return JSON.stringify(json, null, 4);
        }
        
        var start = data.indexOf("%$");
        var end = data.indexOf("%", start+2);
        var endComma = data.lastIndexOf(",");

        var newstr = "";
        var parsedData = "";
        var parseString = "";
        if (start > 0 && end > 0) {
            if (endComma !== -1)
                newstr = data.slice(0, start-1) + '\"' + data.slice(start,end+1) + '\"' + data.slice(endComma+1); 
            else 
                newstr = data.slice(0, start-1) + '\"' + data.slice(start,end+1) + '\"' + data.slice(end+1); 

            parsedData = JSON.parse(newstr);
            parseString = JSON.stringify(parsedData, null, 4);
            start = parseString.indexOf("%$")-1;
            end = parseString.indexOf("%", start+2)-1;
            parseString =  parseString.slice(0, start-1)  + parseString.slice(start+1 ,end+2) + parseString.slice(end+3);
        } else {
            parsedData = JSON.parse(data);
            parseString = JSON.stringify(parsedData, null, 4);
        }        
        return parseString;
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
        positionDisplayPanel: positionDisplayPanel,
        isFirefox: function() {
            return typeof InstallTrigger !== 'undefined';
        }, 
        formatPolicyData: formatPolicyData
    };
    /** END PUBLIC INTERFACE **/

});