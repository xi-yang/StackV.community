(function(conditions, opts) {
    function isAWS(condition) {
        return condition == 'aws-form';
    }
    if (conditions.some(isAWS)) {
        return opts.fn(this);
    } else {
        return opts.inverse(this);
    }
});
