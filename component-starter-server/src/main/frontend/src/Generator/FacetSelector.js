/**
 *  Copyright (C) 2006-2017 Talend Inc. - www.talend.com
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
import React from 'react';
import keycode from 'keycode';
import { Typeahead, Badge } from '@talend/react-components';
import Help from '../Component/Help';

import theme from './FacetSelector.scss';

function escapeRegexCharacters(str) {
  return str.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

// highly inspired from MultiSelectTag but in a way it integrates with this app layout
// note: the map/filter etc would be worth using underscore, for now we assume the suggestions are not numerous enough to require it
export default class FacetSelector extends React.Component {
  constructor(props) {
    super(props);

    const typeaheadConfig = Object.keys(this.props.facets)
      .map(item => {
          const facets = this.props.facets[item];
          return {
            // icon: {name: "talend-filter", title: "icon"},
            title: item,
            suggestions: facets.map(f => {
              return {
                  title: f.name,
                  description: f.description,
                  category: item
              };
            })
          };
      });

    this.theme = {
      container: theme.typeahead,
      itemsContainer: theme['items-container'],
      itemsList: theme.items,
    };

    this.state = {
      facetTypeahead: typeaheadConfig,
      selected: !props.selected ? [] : typeaheadConfig
        .map(cat => cat.suggestions.filter(s => props.selected.indexOf(s.title) >= 0))
        .reduce((a, i) => a.concat(i), [])
    };

    this.onChange = this.onChange.bind(this);
    this.onFocus = this.onFocus.bind(this);
    this.onKeyDown = this.onKeyDown.bind(this);
    this.onRemoveTag = this.onRemoveTag.bind(this);
    this.onAddTag = this.onAddTag.bind(this);
    this.resetSuggestions = this.resetSuggestions.bind(this);
  }

  onKeyDown(event, { focusedItemIndex, newFocusedItemIndex }) {
    switch (event.which) {
      case keycode.codes.enter:
        event.preventDefault();
        if (Number.isInteger(focusedItemIndex)) {
          this.onAddTag(event, { itemIndex: focusedItemIndex });
        }
        break;
      case keycode.codes.down:
      case keycode.codes.up:
        event.preventDefault();
        this.setState({ focusedItemIndex: newFocusedItemIndex });
        break;
      case keycode.codes.backspace:
        if (!this.state.value && this.props.selected.length > 0) {
          this.onRemoveTag(this.props.selected.length - 1);
        }
        break;
      default:
        break;
    }
  }

  onChange(event, { value }) {
    this.updateSuggestions(value);
  }

  onFocus() {
    this.updateSuggestions('');
  }

  onAddTag(event, { sectionIndex, itemIndex }) {
    this.props.selected.push(this.state.suggestions[sectionIndex].suggestions[itemIndex].title);
    this.setState(state => state.selected.push(this.state.suggestions[sectionIndex].suggestions[itemIndex]));
    this.updateSuggestions();
  }

  onRemoveTag(itemIndex) {
    this.props.selected.splice(itemIndex, 1);
    this.setState(state => state.selected.splice(itemIndex, 1));
  }

  resetSuggestions() {
    this.setState({
      suggestions: undefined,
      focusedItemIndex: undefined,
    });
  }

  updateSuggestions(value) {
    this.setState(() => {
      // remove selected items
      let suggestions = Object.keys(this.state.facetTypeahead)
        .map(category => {
          const cat = this.state.facetTypeahead[category];
          return {
            icon: cat.icon,
            title: cat.title,
            suggestions: cat.suggestions.filter(item => {
              return this.props.selected.indexOf(item.title) < 0;
            })
          }
        }).filter(category => category.suggestions.length > 0);

      if (!!value) {
        const escapedValue = escapeRegexCharacters(value.trim());

        // todo: use bloodhood engine
        const regex = new RegExp(escapedValue, 'i');

        suggestions = Object.keys(this.state.facetTypeahead)
          .map(category => {
            const cat = this.state.facetTypeahead[category];
            return {
              icon: cat.icon,
              title: cat.title,
              suggestions: cat.suggestions.filter(item => regex.test(item.title))
            }
          }).filter(category => category.suggestions.length > 0);
      }

      return {
        focusedItemIndex: suggestions.length ? 0 : undefined,
        suggestions,
        value: !!value && value.length === 0 ? undefined : value
      };
    });
  }

  render() {
    if(!this.props.facets) {
      return (<div>Loading ...</div>);
    }

    return (
        <div className={theme.wrapper}>
          <div>
            <Typeahead
              icon={{name: "talend-search", title: "Toggle search input", bsStyle: "link"}}
              placeholder="Select a facet to add to your project"
              multiSection="false"
              autoFocus={false}
              value={this.state.value}
              items={this.state.suggestions}
              onBlur={this.resetSuggestions}
              onChange={this.onChange}
              onFocus={this.onFocus}
              onKeyDown={this.onKeyDown}
              onSelect={this.onAddTag}
              theme={this.theme}
              tabindex="-1"
            />
            <div>
              <Help title="Facets" content={
                <span>
                  <p>Selecting a facet allows you to adds some features to your generated project.</p>
                  <p>A common use case is to activate the testing facet to have skeletons for tests.</p>
                </span>
              } />
            </div>
          </div>

          <div className={theme.badges}>{
              this.state.selected.map((item, index) => {
                return <Badge label={item.title} category={item.category} key={index} onDelete={event => this.onRemoveTag(index)} />;
              })
          }</div>
        </div>
    );
  }
}
