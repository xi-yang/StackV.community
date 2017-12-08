var substringMatcher = function (strs) {
    return function findMatches(q, cb) {
        var matches, substrRegex;

        // an array that will be populated with substring matches
        matches = [];

        // regex used to determine if a string contains the substring `q`
        substrRegex = new RegExp(q, 'i');

        // iterate through the pool of strings and for any string that
        // contains the substring `q`, add it to the `matches` array
        $.each(strs, function (i, str) {
            if (substrRegex.test(str)) {
                matches.push(str);
            }
        });

        cb(matches);
    };
};

var states = ['/app/keycloak/users', '/app/profile', '/model', '/service/verify/',
    '/service/ready/reset','/service/instance','/service/verify/','/service/delta/',
    '/service/manifest/','/service','/model/systeminstance','/model/systeminstance/',
    '/model/view/','/driver','/delta'];

$('.typeahead').typeahead({
    hint: true,
    highlight: true,
    minLength: 1
},
        {
            name: 'states',
            source: substringMatcher(states)
        });

$('the-basics .typeahead').width('100%');