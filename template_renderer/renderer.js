/*
 *   nodejs handlebars .ttl template rendering service
 */

'use strict';

const PORT = 4000;

// modules
const fs = require('fs');
const net = require('net');
const handlebars = require('handlebars');

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
    var template_strings = load_dir('templates');
    var templates = {};
    for (let ts in template_strings) {
        templates[ts] = handlebars.compile(template_strings[ts])
    }
    return templates;
}
function render(intent){
    intent.data.uuid = intent.uuid;
    try {
        return templates[intent.service](intent.data);
    } catch (err) {
        log.error(err);
    }
}

// initialize handlebars
handlebars.registerPartial(load_dir('partials'));
handlebars.registerHelper(load_dir('helpers', true));
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
            var intent = JSON.parse(buffer);
        } catch (err) {
            socket.write('<!-- Invalid JSON input. -->');
            log.warn(connection_prefix+'Invalid JSON input.');
            return;
        }
        var rendered_string = render(intent);
        try {
            socket.write(rendered_string);
        } catch (err) {
            log.error('Error sending rendered template.');
        }
        log.okay(connection_prefix+'Template rendered & sent.');
    });
    log.info(connection_prefix+'Connected.');
});
server.on('error', function(err) {
    if (err.errno === 'EADDRINUSE') {
        log.error('Port '+PORT+' already in use.');
        process.exit(1);
    } else {
        throw err;
    }
});

// accept requests
server.listen(PORT);
