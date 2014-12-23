var net = require('net');

var HOST = '0.0.0.0'
var PORT = '12301'

var header = String.fromCharCode(013);
var footer = String.fromCharCode(034) + '\r';

var cannedResponse = 'MSH|^~\\&|MESA_XREF|XYZ_HOSPITAL|VEMR|Connectathon|20141111124708+0000||RSP^K23^RSP_K23|7f0001011499ee4b19c3|P|2.5\r\n'
  + 'MSA|AA|34239b8b-7a36-4d4d-8b75-3e5970e694ba\r\n'
  + 'QAK|b499dc3a-c720-4037-b19c-f30e9ef52361|OK\r\n'
  + 'QPD|IHE PIX Query|b499dc3a-c720-4037-b19c-f30e9ef52361|7612241234567\\S\\\\S\\\\S\\ZAF\\S\\NI^^^SANID&SANID&SANID|^^^ECID&ECID&ECID\r\n'
  + 'PID|||975cac30-68e5-11e4-bf2a-04012ce65b02^^^ECID&ECID&ECID^PI||~^^^^^^S';

net.createServer(function(c) {

  buffer = "";

  c.on('error', function(err) {
    console.log(err);
  });

  c.on('data', function(chunk) {
    console.log('Recieved message chunk:\n' + chunk + '\n');
    buffer += chunk;
    if (buffer.indexOf(footer) > -1) {
      console.log("Received full message. Sending response...");
      c.end(header + cannedResponse + footer);
    }
  });

}).listen(PORT, HOST, function() {
  console.log('PIX server listenting on ' + HOST + ', port ' + PORT);
});
