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

function getLastPathSegment(path) {
    if (!path || typeof path !== 'string') {
        return '';
    }
    const segments = path.split('/');
    return segments[segments.length - 1];
}

function replaceLastPathSegment(path, newSegment) {
    if (!path || typeof path !== 'string') {
        return path;
    }

    const segments = path.split('/');
    if (segments.length === 0) {
        return path;
    }

    segments[segments.length - 1] = newSegment;
    return segments.join('/');
}


module.exports = (targetPath, currentPath) => {
  return replaceLastPathSegment(targetPath, getLastPathSegment(currentPath));
};