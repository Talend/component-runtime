const config = require('./webpack.config');
const webpack = require('webpack');

config.devtool = 'inline-source-map';
config.plugins.push(new webpack.DefinePlugin({
	'process.env.NODE_ENV': JSON.stringify('development'),
}));

config.watchOptions = {
	aggregateTimeout: 300,
	poll: 1000,
};

module.exports = config;