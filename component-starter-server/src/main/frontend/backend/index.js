const configuration = require('./configuration.json');
const bodyParser = require('body-parser');
const atob = require('atob');

function getConfiguration(req, res) {
	res.json(configuration);
}

function downloadZip(req, res) {
	const data = atob(req.body.project);
	console.log(data);
	const options = {
		root: `${__dirname}/../public/`,
		dotfiles: 'deny',
		headers: {
			'x-timestamp': Date.now(),
			'x-sent': true,
		},
	};

	const fileName = 'example.zip';
	res.sendFile(fileName, options, err => {
		if (err) {
			res.status(500).send(err);
		} else {
			console.log('Sent:', fileName);
		}
	});
}

function createOnGithub(req, res) {
	console.log(req.body);
	res.json({ success: true });
}

function setup(app) {
	app.use(bodyParser.urlencoded({ extended: true }));
	app.use(bodyParser.json());
	app.get('/api/project/configuration', getConfiguration);
	app.post('/api/project/zip/form', downloadZip);
	app.post('/api/project/github', createOnGithub);
}

module.exports = setup;
