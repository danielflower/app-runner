var http = require('http');

var port = parseInt(process.env['APP_PORT'] || "8082");
var appName = process.env['APP_NAME'] || 'myapp'; // everything must be served from /{appname}/

var requestListener = function (req, res) {
    res.writeHead(200);
    res.end('Hello from ' + appName + '!\n');
};

var server = http.createServer(requestListener);
server.listen(port, 'localhost', function () {
    console.log('Service started at http://localhost:' + port + '/' + appName + '/');
});
