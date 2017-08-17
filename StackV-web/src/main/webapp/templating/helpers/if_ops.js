(function(conditions, opts) {
    function isOPS(condition) {
        return condition === 'openstack-form';
    }
    if (conditions.some(isOPS)) {
        return opts.fn(this);
    } else {
        return opts.inverse(this);
    }
});
