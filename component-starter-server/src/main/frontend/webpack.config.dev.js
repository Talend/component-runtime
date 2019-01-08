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
const webpackConfig = {
	devServer: {
		before: setupBackend,
	},
	resolve: {
		symlinks: false,
	},
};

module.exports = webpackConfig;


module.exports = webpackConfig;