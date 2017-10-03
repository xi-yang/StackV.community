(function(root, opts) {
    if (root.options.includes('aws-form') || // array
            root.parent.includes('amazon')) {   // string
        return opts.fn(this);
    }
    
    return opts.inverse(this);
});
