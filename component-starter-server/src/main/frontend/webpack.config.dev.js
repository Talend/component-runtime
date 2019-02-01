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
	output: {
		publicPath: '/',
	},
	devServer: {
		before: setupBackend,
		host: '0.0.0.0',
		disableHostCheck: true,
		historyApiFallback: true,
	},
	resolve: {
		symlinks: false,
	},
};

module.exports = webpackConfig;
