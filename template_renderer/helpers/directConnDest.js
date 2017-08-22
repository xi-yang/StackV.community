(function(to) {
    // returns original string if it does not contain ?vlan
    return new handlebars.SafeString(to.slice(0, to.indexOf('?vlan')));
});
