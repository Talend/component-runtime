const express = require('express');
const add = require('./add.json');
const basic = require('./basic.json');
const components = require('./components.json');
const servicenow =  require('./servicenow.json');

const app = express();

app.use(function use(req, res, next) {
	console.log(req.url);
	res.header('Access-Control-Allow-Origin', '*');
	res.header('Access-Control-Allow-Methods', 'GET, PUT, POST, DELETE, OPTIONS');
	res.header('Access-Control-Max-Age', '1000');
	res.header('Access-Control-Allow-Headers', 'Origin, X-Requested-With, Content-Type, Accept');
	next();
});

app.get('/api/v1/forms/add', (req, res) => {
	res.json(add);
});
app.get('/api/v1/application/index', (req, res) => {
	res.json(components);
});
app.get('/api/v1/application/detail/c2VydmljZW5vdyNTZXJ2aWNlTm93I1NlcnZpY2VOb3dPdXRwdXQ', (req, res) => {
	res.json(servicenow);
});
app.post('/api/v1/application/action', (req, res) => {
	// action=urlValidation&family=ServiceNow&type=validation
	//body = arg0: "asd"
	// TODO check query params and body
	// response {"comment":"no protocol: asd","status":"KO"}
	res.json({"comment":"no protocol: asd","status":"KO"});
})
app.get('/api/v1/action/execute', (req, res) => {
	res.json({})
});

app.post('/api/v1/action/execute', (req, res) => {
	res.json(basic);
});

app.listen(3000, () => console.log('Example app listening on port 3000!'));
