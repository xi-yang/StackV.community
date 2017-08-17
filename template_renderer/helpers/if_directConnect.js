(function(gateways, opts) {
    function isDC(gateway) {
        return gateway === 'AWS Direct Connect';
    }
    if (gateways.some(isDC)) {
        return opts.fn(this);
    } else {
        return opts.inverse(this);
    }
});
