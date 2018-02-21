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
const Opal = global.Opal;
const fs = require('fs');
const path = require('path');

// not yet a js api for that so create an opal class
const GlobalIncludeProcessor = (() => {
  const superclass = Opal.module(null, 'Asciidoctor').Extensions.IncludeProcessor;
  const scope = Opal.klass(Opal.module(null, 'TalendComponentKit'), superclass, 'IncludeProcessor', function () {});

  Opal.defn(scope, '$initialize', function initialize () {
    Opal.send(this, Opal.find_super_dispatcher(this, 'initialize', initialize));
  });

  const file = path => {
    // 1. absolute path is not handled by antora
    // 2. relative local include leads to including raw html to handling it here
    return [path, '../antora/modules/ROOT/pages/' + path].find(item => fs.existsSync(item));
  };

  Opal.defn(scope, '$handles?', file);
  Opal.defn(scope, '$process', (doc, reader, target, attrs) => {
    let contents = fs.readFileSync(file(target), "utf8");
    const tags = getTags(attrs)
    if (tags) [contents, startLineNum] = applyTagFiltering(contents, tags)
    reader.pushInclude(contents, target, path.basename(target), 1, attrs)
  });

  return scope
})();

module.exports = {
  register: function (registry, context) {
    // drop old one to replace it otherwise we are always second
    registry.$include_processor(GlobalIncludeProcessor.$new());
    const custom = registry.include_processor_extensions[registry.include_processor_extensions.length - 1];
    const first = registry.include_processor_extensions[0];
    registry.include_processor_extensions[0] = custom;
    registry.include_processor_extensions[registry.include_processor_extensions.length - 1] = first;
  }
};

//
// copied form antora, should be dropped when possible
//

const CIRCUMFIX_COMMENT_SUFFIX_RX = / (?:\*[/)]|--%?>)$/
const NEWLINE_RX = /\r\n?|\n/
const TAG_DELIMITER_RX = /[,;]/
const TAG_DIRECTIVE_RX = /\b(?:tag|(end))::(\S+)\[\]$/

function getTags (attrs) {
  if (attrs['$key?']('tag')) {
    const tag = attrs['$[]']('tag')
    if (tag && tag !== '!') {
      return tag.charAt() === '!' ? { [tag.substr(1)]: false } : { [tag]: true }
    }
  } else if (attrs['$key?']('tags')) {
    const tags = attrs['$[]']('tags')
    if (tags) {
      let result = {}
      let any = false
      tags.split(TAG_DELIMITER_RX).forEach((tag) => {
        if (tag && tag !== '!') {
          any = true
          if (tag.charAt() === '!') {
            result[tag.substr(1)] = false
          } else {
            result[tag] = true
          }
        }
      })
      if (any) return result
    }
  }
}

function applyTagFiltering (contents, tags) {
  let selecting, selectingDefault, wildcard
  if ('**' in tags) {
    if ('*' in tags) {
      selectingDefault = selecting = tags['**']
      wildcard = tags['*']
      delete tags['*']
    } else {
      selectingDefault = selecting = wildcard = tags['**']
    }
    delete tags['**']
  } else {
    selectingDefault = selecting = !Object.values(tags).includes(true)
    if ('*' in tags) {
      wildcard = tags['*']
      delete tags['*']
    }
  }

  const lines = []
  const tagStack = []
  const usedTags = []
  let activeTag
  let lineNum = 0
  let startLineNum
  contents.split(NEWLINE_RX).forEach((line) => {
    lineNum += 1
    let m
    let l = line
    if (
      (l.endsWith('[]') ||
        (~l.indexOf('[] ') &&
          (m = l.match(CIRCUMFIX_COMMENT_SUFFIX_RX)) &&
          (l = l.substr(0, m.index)).endsWith('[]'))) &&
      (m = l.match(TAG_DIRECTIVE_RX))
    ) {
      const thisTag = m[2]
      if (m[1]) {
        if (thisTag === activeTag) {
          tagStack.shift()
          ;[activeTag, selecting] = tagStack.length ? tagStack[0] : [undefined, selectingDefault]
        } else if (thisTag in tags) {
          const idx = tagStack.findIndex(([name]) => name === thisTag)
          if (~idx) {
            tagStack.splice(idx, 1)
            //console.warn(`line ${lineNum}: mismatched end tag in include: expected ${activeTag}, found ${thisTag}`)
          }
          //} else {
          //  //console.warn(`line ${lineNum}: unexpected end tag in include: ${thisTag}`)
          //}
        }
      } else if (thisTag in tags) {
        usedTags.push(thisTag)
        tagStack.unshift([(activeTag = thisTag), (selecting = tags[thisTag])])
      } else if (wildcard !== undefined) {
        selecting = activeTag && !selecting ? false : wildcard
        tagStack.unshift([(activeTag = thisTag), selecting])
      }
    } else if (selecting) {
      if (!startLineNum) startLineNum = lineNum
      lines.push(line)
    }
  })
  // Q: use _.difference(Object.keys(tags), usedTags)?
  //const missingTags = Object.keys(tags).filter((e) => !usedTags.includes(e))
  //if (missingTags.length) {
  //  console.warn(`tag${missingTags.length > 1 ? 's' : ''} '${missingTags.join(',')}' not found in include`)
  //}
  return [lines, startLineNum || 1]
}

