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
    routes.push({
        to: '0.0.0.0/0',
        nextHop: 'internet'
    });
    root.subnets.forEach(function(subnet) {
        var net = {
            name: subnet.name,
            cidr: subnet.cidr,
            routes: nextHop(subnet.routes)
        }
        if (subnet.vpn_route_propagation) {
            if (!net.routes) {
                net.routes = [];
            }
            net.routes.push({
                from: 'vpn',
                to: '0.0.0.0/0',
                nextHop: 'vpn',
            });
        }
        if (subnet.vpn_route_propagation) {
            if (!net.routes) {
                net.routes = [];
            }
            net.routes.push({
                to: '0.0.0.0/0',
                nextHop: 'internet',
            });
        }
        subnets.push(net)
    });
    var network = {
        type: 'internal',
        cidr: root.cidr,
        parent: root.parent,
        subnets: subnets,
        routes: routes,
    };
    if (root.options.includes('aws-form') ||
            root.parent.includes('amazon')) {
        var gateways = [
            {
                type: 'internet'
            },
            {
                type: 'vpn'
            }
        ];
        network.gateways = gateways;
    } 
    return new handlebars.SafeString(JSON.stringify(network, null, 4));
});

