(function(address) {
    return (address.includes("mac") ? address.slice(address.indexOf("mac") + 4) : undefined);
});
