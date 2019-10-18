/**
 *  Copyright (C) 2006-2019 Talend Inc. - www.talend.com
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
'use strict';

const path = require('path')
const webpack = require('webpack');
const autoprefixer = require('autoprefixer');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const ExtractTextPlugin = require('extract-text-webpack-plugin');
const CopyWebpackPlugin = require('copy-webpack-plugin');
const icons = require.resolve('@talend/icons/dist/info');

const extractCSS = new ExtractTextPlugin({ filename: '[name]-[hash].css' });

const SASS_DATA = `@import '~@talend/bootstrap-theme/src/theme/guidelines';
`;

function getCommonStyleLoaders(enableModules) {
  let cssOptions = {};
  if (enableModules) {
    cssOptions = { sourceMap: true, modules: true, importLoaders: 1, localIdentName: '[name]__[local]___[hash:base64:5]' };
  }
  return [
    { loader: 'css-loader', options: cssOptions },
    { loader: 'postcss-loader', options: { sourceMap: true, plugins: () => [autoprefixer({ browsers: ['last 2 versions'] })] } },
    { loader: 'resolve-url-loader' },
  ];
}

function getSassLoaders(enableModules) {
  return getCommonStyleLoaders(enableModules).concat({ loader: 'sass-loader', options: { sourceMap: true, data: SASS_DATA } });
}

module.exports = {
  entry: ['babel-polyfill', 'whatwg-fetch', './src/index.js'],
  devtool: process.env.NODE_ENV === 'development' ? 'source-map' : undefined,
  output: {
    path: `${__dirname}/dist`,
    publicPath: '/',
    filename: '[name]-[hash].js',
  },
  module: {
    loaders: [
      {
        test: /\.js$/,
        exclude: /node_modules/,
        use: { loader: 'babel-loader' },
      },
      {
        test: /\.js$/,
        include: /component-kit\.js/,
        use: { loader: 'babel-loader' },
      },
      {
        test: /\.css$/,
        use: extractCSS.extract(getCommonStyleLoaders()),
        exclude: /@talend/,
      },
      {
        test: /\.scss$/,
        use: extractCSS.extract(getSassLoaders()),
        include: /theme.scss/,
      },
      {
        test: /\.scss$/,
        use: extractCSS.extract(getSassLoaders(true)),
        exclude: /theme.scss/,
      },
      {
        test: /\.woff(2)?(\?v=\d+\.\d+\.\d+)?$/,
        loader: 'url-loader',
        options: { name: './fonts/[name].[ext]', limit: 50000, mimetype: 'application/font-woff' },
      },
    ],
  },
  plugins: [
    extractCSS,
    new HtmlWebpackPlugin({
      filename: './index.html',
      template: './src/public/index.html',
      title: 'Talend Components Dev Tester',
    }),
    new CopyWebpackPlugin([
      { from: 'src/assets' },
      { from: path.join(path.dirname(icons), 'svg-bundle') },
    ]),
    new webpack.DefinePlugin({
    	'process.env.ICON_BUNDLE': 'true',
    }),
],
};
