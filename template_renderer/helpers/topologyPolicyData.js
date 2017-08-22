(function(root) {
    function nextHop(routes) {
        routes.map((route) => {
            if (route.next_hop) {
                route.nextHop = route.next_hop;
                delete route.next_hop;
            }
        });
    }
    var subnets = [];
    var routes = [];
    var gwVpn = false;
    root.subnets.forEach(function(subnet) {
        subnets.push({
            name: subnet.name,
            cidr: subnet.cidr,
            routes: nextHop(subnet.routes)
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
    if (root.conditions.includes('aws-form')) {
        gateways = [
            {
                type: 'internet'
            },
            {
                type: 'vpn'
            }
        ];
    } 
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

