/**
 *  Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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

const fs = require("fs");
const application = require("./application.json");
const bodyParser = require("body-parser");
const atob = require("atob");
const { createProxyMiddleware } = require("http-proxy-middleware");

function getApplication(req, res) {
  res.json(application);
}

function getApplicationDetail(req, res) {
  const { detailId } = req.params;
  // const decoded = atob(detailId);
  fs.readFile(
    __dirname + `/details/${req.params.detailId.replaceAll("..", "")}.json`,
    "utf8",
    (err, data) => {
      if (err) {
        res.status(404).json({ message: "Not found" });
        return;
      }
      res.json(JSON.parse(data));
    }
  );
}

function setup(middlewares, devServer) {
  if (!devServer) {
    throw new Error("webpack-dev-server is not defined");
  }

  devServer.app.use(bodyParser.urlencoded({ extended: true }));
  devServer.app.use(bodyParser.json());

  devServer.app.get("/api/v1/application/index", getApplication);
  devServer.app.get("/api/v1/application/detail/:detailId", getApplicationDetail);

  devServer.app.use(
    createProxyMiddleware({
      pathFilter: "/api",
      target: process.env.API_URL || "http://localhost:10101",
      changeOrigin: true,
      secure: false,
    })
  );

  return middlewares;
}

module.exports = setup;
