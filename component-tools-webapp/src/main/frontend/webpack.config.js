/**
 *  Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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

const path = require('path');
const fs = require('fs');
const webpack = require('webpack');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const ExtractTextPlugin = require('extract-text-webpack-plugin');
const autoprefixer = require('autoprefixer');

const extractTextPluginFilename = 'static/css/[name].[contenthash:8].css';

const SASS_DATA = `
$brand-primary: #4F93A7;
$brand-success: #B9BF15;
@import '~@talend/bootstrap-theme/src/theme/guidelines';
`;

module.exports = {
  devtool: 'source-map',
  context: __dirname + '/src',
  entry: './index',
  output: {
    path: __dirname + '/dist',
    filename: 'component-kit-tools-webapp.min.js'
  },
  module: {
    loaders: [
      {
        oneOf: [
          {
            test: [/\.bmp$/, /\.gif$/, /\.jpe?g$/, /\.png$/],
            loader: 'url-loader',
            options: {
              limit: 10000,
              name: 'static/media/[name].[hash:8].[ext]',
            }
          },
          {
            test: /\.(js|jsx)$/,
            include: path.resolve(fs.realpathSync(process.cwd()), 'src'),
            loader: 'babel-loader',
            options: {
              babelrc: false,
              presets: [ 'babel-preset-react-app' ],
              compact: true
            }
          },
          {
            test: /\.css$/,
            loader: ExtractTextPlugin.extract(
              Object.assign(
                {
                  fallback: 'style-loader',
                  use: [
                    {
                      loader: 'css-loader',
                      options: {
                        importLoaders: 1,
                        minimize: true,
                        sourceMap: true
                      }
                    },
                    {
                      loader: 'postcss-loader',
                      options: {
                        ident: 'postcss',
                        plugins: () => [
                          require('postcss-flexbugs-fixes'),
                          autoprefixer({
                            browsers: [
                              '>1%',
                              'last 4 versions',
                              'Firefox ESR',
                              'not ie < 9'
                            ],
                            flexbox: 'no-2009'
                          })
                        ]
                      }
                    }
                  ]
                },
                {
                  publicPath: Array(extractTextPluginFilename.split('/').length).join('../')
                }
              )
            )
          },
          {
            loader: 'file-loader',
            include: [/favicon\.ico$/],
            options: {
              name: '[name].[ext]',
            }
          },
          {
            loader: 'file-loader',
            exclude: [/\.js$/, /\.html$/, /\.json$/],
            options: {
              name: 'static/media/[name].[hash:8].[ext]',
            }
          }
        ]
      }
    ]
  },
  plugins: [
      new HtmlWebpackPlugin({
        inject: true,
        template: './public/index.html',
        minify: {
          removeComments: true,
          collapseWhitespace: true,
          removeRedundantAttributes: true,
          useShortDoctype: true,
          removeEmptyAttributes: true,
          removeStyleLinkTypeAttributes: true,
          keepClosingSlash: true,
          minifyJS: true,
          minifyCSS: true,
          minifyURLs: true,
        },
      }),
      new ExtractTextPlugin({
        filename: extractTextPluginFilename
      })
    ],
    node: {
      dgram: 'empty',
      fs: 'empty',
      net: 'empty',
      tls: 'empty',
    }
}
