(function(context, opts) {
    if (context.parent == 'urn:ogf:network:openstack.com:openstack-cloud')
        return opts.fn(this);
    else
        return opts.inverse(this);
});
