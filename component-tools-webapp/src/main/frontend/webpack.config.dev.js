/**
 *  Copyright (C) 2006-2023 Talend Inc. - www.talend.com
 *
 *  Licensed under the Apache License, Version 2.0 (the 'License');
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an 'AS IS' BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

// if you use npm directly instead of mvn frontend:npm@watch

// const config = require('./webpack.config');
const webpack = require('webpack');

const config = {
	plugins: []
};
config.devtool = 'inline-source-map';
config.plugins.push(new webpack.DefinePlugin({
	'process.env.NODE_ENV': JSON.stringify('development'),
}));

config.watchOptions = {
	aggregateTimeout: 300,
	poll: 1000,
};

config.devServer = {
	proxy: {
		'/api': {
			target: process.env.API_URL || 'http://localhost:10101',
			changeOrigin: true,
			secure: false,
		},
	},
	historyApiFallback: true,
};

module.exports = config;