(function(ip) {
    if (ip) {
        return new handlebars.SafeString(",\n       \"ip_address\": \"" + ip + "\"");
    } else {
        return "";
    }
});
