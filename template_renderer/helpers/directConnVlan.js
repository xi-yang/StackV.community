(function(to) {
    if (to.includes('?vlan')) {
        return to.slice(to.indexOf('?vlan' + 6));
    } else {
        return 'any';
    }
});
