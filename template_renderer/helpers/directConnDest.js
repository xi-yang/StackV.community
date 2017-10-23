(function(to) {
    if (to.includes('?vlan')) {
        return new handlebars.SafeString(to.slice(0, to.indexOf('?vlan')));
    } else {
        return new handlebars.SafeString(to);
    }
});
