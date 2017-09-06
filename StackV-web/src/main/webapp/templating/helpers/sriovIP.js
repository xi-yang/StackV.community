(function(ip) {
    if (ip) {
        return new Handlebars.SafeString(",\n       \"ip_address\": \"" + ip + "\"");
    } else {
        return "";
    }
});
