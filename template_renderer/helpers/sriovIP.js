(function(address) {
    var ip = address.includes("ipv4") ? address.slice(address.indexOf("ipv4") + 5) : undefined; //TODO this includes the mac address, intended?
    if (ip) {
        return new handlebars.SafeString(",\n       \"ip_address\": \"" + ip + "\"");
    } else {
        return "";
    }
});
