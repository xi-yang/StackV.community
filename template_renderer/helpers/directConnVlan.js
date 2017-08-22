(function(to) {
    if (to.includes('?vlan')) {
        return new handlebars.SafeString(to.slice(to.indexOf('?vlan' + 6)));
    } else {
        return 'any';
    }
});
