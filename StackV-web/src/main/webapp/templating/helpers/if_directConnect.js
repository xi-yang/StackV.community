(function(gateways, opts) {
    gateways.forEach(function(g) {
        if (g.type === 'AWS Direct Connect') {
            return opts.fn(this);
        }
    });
    return opts.inverse(this);
});
