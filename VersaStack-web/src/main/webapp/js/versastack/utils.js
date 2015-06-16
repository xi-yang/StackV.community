define([], function() {
    function map_(arr,f){
        var ans=new Array(arr.length);
        for(var i=0; i<arr.length; i++){
            ans[i]=f(arr[i]);
        }
        return ans;
    }
    /** PUBLIC INTERFACE **/
    return {
        map_:map_
    };
    /** END PUBLIC INTERFACE **/

});