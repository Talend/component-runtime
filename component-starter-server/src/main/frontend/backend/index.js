/**
 *  Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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
 */ const configuration = require('./configuration.json');
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
	res.sendFile(fileName, options, (err) => {
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

function setup(middlewares, devServer) {
	if (!devServer) {
		throw new Error('webpack-dev-server is not defined');
	}

	devServer.app.use(bodyParser.urlencoded({ extended: true }));
	devServer.app.use(bodyParser.json());

	// Use the `unshift` method if you want to run a middleware before all other middlewares
	// or when you are migrating from the `onBeforeSetupMiddleware` option
	middlewares.unshift({
		name: 'project-configuration',
		path: '/api/project/configuration',
		middleware: getConfiguration,
	});

	middlewares.unshift({
		name: 'project-zip',
		path: '/api/project/zip/form',
		middleware: downloadZip,
	});

	middlewares.unshift({
		name: 'project-openapi-zip',
		path: '/api/project/openapi/zip/form',
		middleware: downloadZip,
	});

	middlewares.unshift({
		name: 'project-github',
		path: '/api/project/github',
		middleware: createOnGithub,
	});

	return middlewares;
}

module.exports = setup;
