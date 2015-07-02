define([
    "local/versastack/model"
], function(model) {
    var owns = Object.prototype.hasOwnProperty;

    /** CONVERSION FUNCTIONS **/
    function convertIDToName(id) {
        var node = model.data.dictionary[id];

        if (node != null) {
            return node.name;
        }

        return node;
    }

    function convertIDArrayToNames(ids) {
        var names = [];

        if (ids != null) {
            for (var i = 0, l = ids.length; i < l; ++i) {
                names[i] = convertIDToName(ids[i])
            }
        }

        return names;
    }

    /** UTILITY FUNCTIONS **/
    function findByName(array, name) {
        var index = indexOfName(array, name);
        if (index >= 0) {
            return array[index];
        } else {
            return null;
        }
    }

    function indexOfName(array, name) {

        if (array != null) {
            for (var i = 0, l = array.length; i < l; ++i) {
                if (array[i].name === name) {
                    return i;
                }
            }
        }

        return -1;
    }

    function mergeObjects(o1, o2) {
        var o3 = o1 || {};

        for (var element in o2) {
            if (owns.call(o2, element)) {
                o3[element] = o2[element];
            }
        }

        return o3;
    }

    function removePrefix(string, prefix) {
        var regexp = new RegExp(escapeRegExp(prefix), 'ig');
        return string.replace(regexp, '');
    }

    /**
     * escapes all special characters in a regular expression
     * by bobince from
     * stackoverflow.com/questions/3561493/is-there-a-regexp-escape-function-in-javascript
     */
    function escapeRegExp(s) {
        return s.replace(/[-\/\\^$*+?.()|[\]{}]/g, '\\$&');
    }
    /** END UTILITY FUNCTIONS **/


    /** PUBLIC INTERFACE **/
    return {
        idToName: convertIDToName,
        idsToNames: convertIDArrayToNames,
        findByName: findByName,
        indexOfName: indexOfName,
        mergeObjects: mergeObjects,
        removePrefix: removePrefix
    };
    /** END PUBLIC INTERFACE **/

});