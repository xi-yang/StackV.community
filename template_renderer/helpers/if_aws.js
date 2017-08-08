(function(context, opts) {
    if (context.parent == 'urn:ogf:network:aws.amazon.com:aws-cloud')
        return opts.fn(this);
    else
        return opts.inverse(this);
});
