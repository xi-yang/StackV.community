/*
 *   nodejs handlebars .ttl template rendering service
 */

var fs = require('fs');
var net = require('net');
var handlebars = require('handlebars');

var log = {
    info: function(msg) {
        console.log(msg);
    },
    okay: function(msg) {
        console.log('\x1b[32m'+msg+'\x1b[0m');
    },
    warn: function(msg) {
        console.log('\x1b[33m'+msg+'\x1b[0m');
    },
    error: function(msg) {
        console.log('\x1b[31m'+msg+'\x1b[0m');
    },
};
process.stdin.on('data', function(data) {
    var line = data.toString().trim();
    switch (line) {
        case 'conn':
            log.info(connection_count);
            break;
        case 'exit':
        case 'close':
            server.close();
            process.exit(0);
    }
});

// return map of file contents indexed by filename, minus extension
function load_dir(dir, evaluate=false) {
    var file_strings = {};
    fs.readdirSync(dir).forEach(function(file) {
        // strip extension
        var prop_name = file.slice(0, file.lastIndexOf('.'));
        file_strings[prop_name] = fs.readFileSync(dir+'/'+file, 'utf8');
        if (evaluate) {
            file_strings[prop_name] = eval(file_strings[prop_name]);
        }
    });
    return file_strings;
}
function compile_templates() {
    /* depending on whether there will be one monolithic template or various ones
     * for different services, this will either be one line or
     * in the style of the above loaders */

    // for now, just using one template
    return {ahc: handlebars.compile(fs.readFileSync('ahc.hb', 'utf8'))};
}
function render(service, input_json){
    // throws TypeError on invalid
    return templates[service](input_json);
}

// initialize handlebars
handlebars.registerPartial(load_dir('partials'));
handlebars.registerHelper(load_dir('helpers', evaluate=true));
var templates = compile_templates();

// initialize server
var connection_count = 0; // probably breaks with async but fine for this testing
var server = net.createServer();
server.on('connection', function(socket) {
    var connection_no = ++connection_count;
    var connection_prefix = '['+connection_no+']: ';
    socket.on('close', function() {
        connection_count--;
        log.info(connection_prefix+'Connection closed.');
    });
    socket.on('data', function(buffer) {
        // validate over schema first, maybe
        try {
            var json = JSON.parse(buffer);
            var service = json.service;
            var rendered_string = render(service, json.data); 
        } catch (e) {
            log.error(connection_prefix+'Invalid JSON input.');
            return;
        }
        socket.write(rendered_string);
        log.okay(connection_prefix+'Template rendered & sent.');
    });
    log.info(connection_prefix+'Connected.');
});

var unix_socket = '/tmp/handlebars.sock';
// remove socket if server was killed and left file
try {
    fs.unlinkSync(unix_socket);
} catch (e) {}

// accept requests
server.listen(unix_socket); // can be changed to tcp port if desired
