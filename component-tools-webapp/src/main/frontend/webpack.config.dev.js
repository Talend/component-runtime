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

const setupBackend = require("./backend");

module.exports = {
  devServer: {
    setupMiddlewares: setupBackend,
    host: "0.0.0.0",
    proxy: {
      "/api": {
        target: process.env.API_URL || "http://localhost:10101",
        changeOrigin: true,
        secure: false,
      },
    },
    historyApiFallback: true,
  },
  resolve: {
    symlinks: false,
    fallback: {
      querystring: false,
    },
  },
  watchOptions: {
    ignored: "**/node_modules",
    followSymlinks: true, // when symlinks.resolve is false, we need this to make sure dev server picks up the changes in the symlinked files and rebuilds
  },
};
