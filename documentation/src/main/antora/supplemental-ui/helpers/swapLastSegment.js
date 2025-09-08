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


/*
 * /component name/version/pagePart1/pagePart2
 * 1 - is component
 * 2 - is version
 * 3+ - is page path
*/
const VERSIONED_SEGMENTS = 2

function retrieveLastSegments(path) {
    if (!path || typeof path !== 'string') {
        return '';
    }
    const segments = path.split('/').filter(segment => segment !== '');
    if (segments.length <= VERSIONED_SEGMENTS) {
        console.warn(`Path "${path}" does not have enough segments to extract a page path.`);
        return '';
    }

    return segments.slice(VERSIONED_SEGMENTS).join('/');
}

function replaceLastPathSegment(path, newSegment) {
    if (!path || typeof path !== 'string') {
        return path;
    }

    const segments = path.split('/').filter(segment => segment !== '');
    if (segments.length <= VERSIONED_SEGMENTS) {
        console.warn(`Path "${path}" does not have enough segments to extract base path.`);
        return path;
    }

    const base = segments.slice(0, VERSIONED_SEGMENTS).join('/');
    return '/' + base + '/' + newSegment;
}


module.exports = (targetPath, currentPath) => {
  return replaceLastPathSegment(targetPath, retrieveLastSegments(currentPath));
};