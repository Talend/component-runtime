/**
 *  Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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
class Api {
  get(link) {
    return fetch(`api/v1${link}`)
      .then(resp => resp.json());
  }

  getIcon(link) {
    return fetch(`api/v1${link}`)
      .then(resp => resp.arrayBuffer())
      .then(buffer => btoa(new Uint8Array(buffer).reduce((data, byte) => data + String.fromCharCode(byte), '')));
  }

  loadComponents() {
    return fetch('api/v1/application/index')
      .then(resp => resp.json());
  }
}

export default new Api();
