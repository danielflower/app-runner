
var port = parseInt(process.env['APP_PORT'] || "8082");
var appName = process.env['APP_NAME'] || 'myapp'; // everything must be served from /{appname}/

var express = require('express');
var app = express();

app.get('/' + appName, function (req, res) {
    res.send('Hello from ' + appName + '!\n');
});
app.use('/' + appName, express.static('static'));

app.listen(port, 'localhost', function () {
    console.log('Service started at http://localhost:' + port + '/' + appName + '/');
});
