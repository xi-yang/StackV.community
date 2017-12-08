(function(context, opts) {
    for (var subnet in context.subnets) {
        for (var vm in subnet.vms) {
            for (var sriov in vm.sriovs) {
                for (var gateway in context.gateways) {
                    if (sriov.hosting_gateway === gateway.name && gateway.type === 'L2 Stitch Port') {
                        return opts.fn(this);
                    }
                }
            }
        }
    }
    return opts.inverse(this);
});
