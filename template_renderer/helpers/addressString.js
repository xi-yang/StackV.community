(function(name, interfaces, refUuid){
    var addressMRL = '';
    interfaces.forEach(function(i) {
        var addressString = undefined;
        if (i.type.toUpperCase() === 'ETHERNET' && i.hasOwnProperty('address')) {
            addressString = i.address;
            addressString = addressString.includes('ipv') ? addressString.substring(addressString.indexOf('ipv') + 5) : addresString;
            addressString = addressString.includes('/') ? addressString.substring(0, addressString.indexOf('/')) : addressString;
        }
        if (addressString) {
            addressMRL += '    mrs:hasNetworkAddress    ' + '&lt;urn:ogf:network:service+' + refUuid + ':resource+virtual_machines:tag+' + name + ':eth0:floating&gt;.\n\n' +
                '&lt;urn:ogf:network:service+' + refUuid + ':resource+virtual_machines:tag+' + name + ':eth0:floating&gt;\n' +
                '    a        mrs:NetworkAddress;\n    mrs:type   "floating-ip";\n' + '    mrs:value       "' + addressString + '" .\n\n';
        } else {
            addressMRL = ' .\n\n';
        }
    });
    return new handlebars.SafeString(addressMRL);
});
