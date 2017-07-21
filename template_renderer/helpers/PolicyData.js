(function(obj) {
    delete obj['UUID'];
    return new handlebars.SafeString(JSON.stringify(obj));
})
