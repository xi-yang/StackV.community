(function(data) {
    return new handlebars.SafeString(JSON.stringify(data.hash, null, 4));
})
