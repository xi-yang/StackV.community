(function(root, opts) {
    if (root.options.includes('openstack-form') ||   // array
            root.parent.includes('openstack')) {        // string
        return opts.fn(this);
    } else {
        return opts.inverse(this);
    }
});
