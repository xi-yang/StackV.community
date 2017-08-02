/**
 * nodejs handlebars .ttl template rendering service
 */

'use strict';

const PORT = 4000;

// modules
const fs = require('fs');
const net = require('net');
const handlebars = require('handlebars');

if (!fs.existsSync('log')) fs.mkdir('log');
var log_file = fs.createWriteStream('log/'+Date.now());
var log = {
    info: function(msg) {
        console.log(msg);
        log_file.write('INFO | ' +msg+'\n');
    },
    okay: function(msg) {
        console.log('\x1b[32m'+msg+'\x1b[0m');
        log_file.write('OKAY | ' +msg+'\n');
    },
    warn: function(msg) {
        console.log('\x1b[33m'+msg+'\x1b[0m');
        log_file.write('WARN | ' +msg+'\n');
    },
    error: function(msg) {
        console.log('\x1b[31m'+msg+'\x1b[0m');
        log_file.write('ERRR | ' +msg+'\n');
    },
};
var last_cmd = 'reload';
process.stdin.on('data', function(data) {
    var line = data.toString().trim();
    if (line) last_cmd = line;
    // basic commands for development/testing purposes
    switch (last_cmd) {
        case 'reload':
            load_hb(); 
            break;
        case 'conn':
            log.info(connection_count+' active connections.');
            break;
        case 'exit':
        case 'close':
            server.close();
            log_file.close();
            process.exit(0);
    }
});

/**
 * Load and map contents of all files from specified directory
 *
 * @param {string} dir
 * @param {boolean} [evaluate=false] - set true to eval files as js expressions
 *                                     (useful for loading handlebars helpers)
 * @returns {object} |extensionless filename, contents|
 */
function load_dir(dir, evaluate=false) {
    var file_strings = {};
    fs.readdirSync(dir).forEach(function(file) {
        // strip extension
        var prop_name = file.slice(0, file.indexOf('.'));
        file_strings[prop_name] = fs.readFileSync(dir+'/'+file, 'utf8');
        if (evaluate) {
            file_strings[prop_name] = eval(file_strings[prop_name]);
        }
    });
    return file_strings;
}

/**
 * Load, compile, and map all handlebars templates
 *
 * @returns {object} |extensionless filename, compiled template|
 */
function compile_templates() {
    var template_strings = load_dir('templates');
    var templates = {};
    for (let ts in template_strings) {
        templates[ts] = handlebars.compile(template_strings[ts])
    }
    return templates;
}

/**
 * Render template of desired service
 *
 * @param {object} intent
 * @param {string} intent.uuid - delta UUID
 * @param {string} intent.service - desired service e.g. 'ahc', 'dnc'
 * @param {object} intent.data - parsed JSON manifest of service
 *                               as specified in StackV wiki
 * @returns {string} rendered intent in .ttl format
 */
function render(intent){
    intent.data.uuid = intent.uuid;
    try {
        return templates[intent.service](intent.data);
    } catch (err) {
        log.error(err);
    }
}

// initialize/reload handlebars templates/partials/helpers
var templates;
function load_hb() {
    handlebars.registerPartial(load_dir('partials'));
    handlebars.registerHelper(load_dir('helpers', true));
    templates = compile_templates();
    log.info('Templates loaded.');
}
load_hb();

// initialize server
var connection_count = 0;
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
            log.okay(connection_prefix+'Template rendered & sent.');
        } catch (err) {
            log.error('Error sending rendered template.');
        }
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
log.info('Server listening on port '+PORT+'.');
