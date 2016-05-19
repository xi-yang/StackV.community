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
    
    /** PUBLIC INTERFACE **/
    return {
        map_: map_,
        deleteAllChildNodes: deleteAllChildNodes,
        bsShowFadingMessage: bsShowFadingMessage
    };
    /** END PUBLIC INTERFACE **/

});