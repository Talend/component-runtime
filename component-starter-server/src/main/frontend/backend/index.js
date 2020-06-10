/**
 *  Copyright (C) 2006-2020 Talend Inc. - www.talend.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */const configuration = require('./configuration.json');
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
	app.post('/api/project/openapi/zip/form', downloadZip);
	app.post('/api/project/github', createOnGithub);
}

module.exports = setup;
