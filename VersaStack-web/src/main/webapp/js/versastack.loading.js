// Get existing versastack namespace, or define new one
var versastack = versastack || {};

versastack.loading = function() {
    var css = {
        classes: {
            indicatorDiv: 'loading-div',
            indicatorImage: 'loading-image'
        }
    };

    var items = {};

    var createItem = function(name) {
        items[name] = {};
    };

    var isFinished = function(name) {
        return (items[name] && items[name].loading === false);
    };

    var startLoading = function(name) {
        items[name].loading = true;
        for (var indicator in items[name].indicators) {
            var method = items[name].indicators[indicator];
            if (method === 'display') {
                display(indicator, true);
            } else if (method === 'visibility') {
                visible(indicator, true);
            } else {
                console.log('Invalid method for displaying loader ', indicator, ' for item ', name);
            }
        }
    };

    var endLoading = function(name) {
        items[name].loading = false;
        for (var indicator in items[name].indicators) {
            var method = items[name].indicators[indicator];
            if (method === 'display') {
                display(indicator, false);
            } else if (method === 'visibility') {
                visible(indicator, false);
            } else {
                console.log('Invalid method for displaying loader ', indicator, ' for item ', name);
            }
        }
    };

    var attachIndicator = function(itemName, indicator, method) {
        items[itemName].indicators = items[itemName].indicators || {};
        items[itemName].indicators[indicator] = method;
    };

    var createIndicator = function(element, indicatorID, imageSource) {
        var div = d3.select(element)
                .append('div')
                .attr('id', indicatorID)
                .classed(css.classes.indicatorDiv, true);

        div.append('img')
                .attr('src', imageSource)
                .attr('alt', 'loading...')
                .classed(css.classes.indicatorImage, true);
    };

    var display = function(selector, boolean) {
        if (boolean) {
            d3.selectAll(selector).style('display', 'block');
        } else {
            d3.selectAll(selector).style('display', 'none');
        }
    };

    var visible = function(selector, boolean) {
        if (boolean) {
            d3.selectAll(selector).style('visibility', 'visible');
        } else {
            d3.selectAll(selector).style('visibility', 'hidden');
        }
    };

    return {
        css: css,
        start: startLoading,
        end: endLoading,
        createItem: createItem,
        createIndicator: createIndicator,
        attach: attachIndicator,
        finished: isFinished
    };
}(); // end loadindicator module