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
 */
// const webpack = require('webpack');
// const config = require('./webpack.config');
const setupBackend = require('./backend');

// config.devServer = Object.assign(config, {
//     before: setupBackend,
//     mode: 'development',
//     devServer: {
//         port: 3000,
//     },
//     devtool: 'source-map',
//     plugins: config.plugins.push(new webpack.EnvironmentPlugin([ 'NODE_ENV' ]))

// });
module.exports = {
	// output: {
	// 	publicPath: './',
	// },
	devServer: {
		setupMiddlewares: setupBackend,
		host: '0.0.0.0',
		historyApiFallback: true,
	},
	// resolve: {
	// 	symlinks: false,
	// },
	// watchOptions: {
	// 	ignored: '**/node_modules',
	// followSymlinks: true, // when symlinks.resolve is false, we need this to make sure dev server picks up the changes in the symlinked files and rebuilds
	// },
};
