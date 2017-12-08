(function(type) {
    switch (type) {
        case 'Multi-Path P2P VLAN':
            return 'MCE_MPVlanConnection';
        case 'Multi-Point VLAN Bridge':
            return 'MCE_MultiPointVlanBridge';
    }
});
