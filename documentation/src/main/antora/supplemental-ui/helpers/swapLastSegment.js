/**
 *  Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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

'use strict';

function retrieveLastSegments(path) {
    if (!path || typeof path !== 'string') {
        return '';
    }
    const segments = path.split('/').filter(segment => segment !== '');
    if (segments.length < 3) {
        console.warn(`Path "${path}" does not have enough segments to extract a page path.`);
        return '';
    }

    return segments.slice(2).join('/');
}

function replaceLastPathSegment(path, newSegment) {
    if (!path || typeof path !== 'string') {
        return path;
    }

    const segments = path.split('/').filter(segment => segment !== '');
    if (segments.length < 3) {
        console.warn(`Path "${path}" does not have enough segments to extract base path.`);
        return path;
    }

    const base = segments.slice(0, 2).join('/')
    return '/' + base + '/' + newSegment;
}


module.exports = (targetPath, currentPath) => {
  return replaceLastPathSegment(targetPath, retrieveLastSegments(currentPath));
};