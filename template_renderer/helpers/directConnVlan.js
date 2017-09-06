(function(to) {
    if (to.includes('?vlan')) {
        return to.slice(a.indexOf('?vlan' + 6));
    } else {
        return 'any';
    }
});
