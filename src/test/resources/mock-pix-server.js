var net = require('net');

var HOST = '0.0.0.0'
var PORT = '12301'

var header = String.fromCharCode(013);
var footer = String.fromCharCode(034) + '\r';

var cannedResponse = 'MSH|^~\&|PAT_IDENTITY_X_REF_MGR_MISYS|ALLSCRIPTS|PACS_FUJIFILM|FUJIFILM|20090223154549-0500||RSP^K23|OpenPIXPDQ10.243.0.65.19766751187011|P|2.5 MSA|AA|1235421946\r\n'
+ 'QAK|Q231235421946|OK QPD|IHEPIXQuery|Q231235421946|L101^^^IHELOCAL&1.3.6.1.4.1.21367.2009.1.2.310&ISO^PI|^^^IHENA&1.3.6.1.4.1.21367.2009.1.2.300&ISO PID|||101^^^IHENA&1.3.6.1.4.1.21367.2009.1.2.300&ISO^PI||~^^^^^^S'

net.createServer(function(c) {

  c.on('data', function(chunk) {
    console.log('Recieved message chunk:\n' + chunk + '\n\n');

    if (chunk.toString().indexOf(footer) != -1) {
      c.end(header + cannedResponse + footer);
    }
  })

}).listen(PORT, HOST, function() {
  console.log('PIX server listenting on ' + HOST + ', port ' + PORT);
});
