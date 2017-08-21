(function(root) {
    var subnets = [];
    var routes = [];
    var gwVpn = false;
    root.subnets.forEach(function(subnet) {
        subnets.push({
            name: subnet.name,
            cidr: subnet.cidr,
            routes: subnet.routes
        });
        routes.push(...subnet.routes);
        if (subnet.vpn_route_propagation) {
            gwVpn = true;
        }
    });
    if (gwVpn) {
        routes.push({
            to: '0.0.0.0/0',
            nextHop: 'internet'
        });
    }
    var gateways = [];
    root.gateways.forEach(function(gateway) {
        gateways.push({
            type: gateway.type // TODO
        });
    });
    var network = {
        type: 'internal',
        cidr: root.cidr,
        parent: root.parent,
        subnets: subnets,
        routes: routes,
        gateways: gateways
    };
    return new handlebars.SafeString(JSON.stringify(network, null, 4));
});

