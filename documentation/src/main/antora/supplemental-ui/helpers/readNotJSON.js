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
function extract(text, pos) {
  const content = text.toString('utf-8').trim();
  switch (pos) {
    case 'before':
      const start = content.indexOf('<jsonArray>');
      return start > 0 ? content.substring(0, start) : '';
    default:
      const end = content.indexOf('</jsonArray>');
      return end + '</jsonArray>'.length < content.length ? content.substring(end + '</jsonArray>'.length) : '';
  }
}

module.exports = (text, pos, options) => {
  const content = extract(text, pos);
  return content.length === 0 ? '' : options.fn(content);
};
