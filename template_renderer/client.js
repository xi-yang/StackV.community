// js version of the corresponding java adapter for testing
//
// usage:
// $ node client.js input.json [output.ttl]

const fs = require('fs');
const net = require('net');

try {
    var input = fs.readFileSync(process.argv[2]);
} catch (err) {
    console.log('provide input JSON as argument');
    process.exit(1);
}

var output = 'test.ttl';
if (process.argv[3]) output = process.argv[3];

var connection = net.connect(4000);
connection.on('data', function(data) {
    fs.writeFileSync(output, data);
    process.exit(0);
});
connection.write(input);
