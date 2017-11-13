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
import { Typeahead } from '@talend/react-components';
import Input from '../Component/Input';

import theme from './CategorySelector.scss';

function escapeRegexCharacters(str) {
  return str.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

// split the config in 2: the "main" category (value) which has suggestions and the "specific" one which is
// for this component family
export default class CategorySelector extends React.Component {
  constructor(props) {
    super(props);
    this.onBlur = this.onBlur.bind(this);
    this.onChange = this.onChange.bind(this);
    this.onFocus = this.onFocus.bind(this);
    this.onKeyDown = this.onKeyDown.bind(this);
    this.onSelect = this.onSelect.bind(this);

    this.theme = {
      container: [
        theme.container,
        'tf-datalist-container'
      ].join(' '),
      itemsContainer: theme['items-container'],
      itemsList: theme.items,
    };

    this.state = {
      previousValue: props.initialValue,
      value: props.initialValue,
      specific: {
        value: 'ComponentCategory'
      },
      allSuggestions: [
        {
          title: 'Business',
          description: 'Category about business related components'
        },
        {
          title: 'Database',
          description: 'Category about RDBMS or SQL databases'
        },
        {
          title: 'Big Data',
          description: 'Category about other databases'
        },
        {
          title: 'Business Intelligence',
          description: 'Category about business intelligence components'
        },
        {
          title: 'Cloud',
          description: 'Category about cloud solutions'
        },
        {
          title: 'Internet',
          description: 'Category about internet connectivity'
        },
        {
          title: 'Misc',
          description: 'Default category'
        }
      ]
    };
  }

  onBlur(event) {
    this.resetSuggestions();
    const { value, previousValue } = this.state;

    if (value !== previousValue) {
      this.updateValue(event, value);
    }
  }

  onChange(event, { value }) {
    this.updateSuggestions(value);
    this.updateValue(event, (value.title || value) + (!!this.specific.value ? '/' + this.specific.value : ''));
  }

  onFocus() {
    this.updateSuggestions(this.state.value);
  }

  onKeyDown(event, { focusedItemIndex, newFocusedItemIndex }) {
    switch (event.which) {
      case keycode.codes.esc:
        event.preventDefault();
        this.resetValue();
        break;
      case keycode.codes.enter:
        if (!this.state.suggestions) {
          break;
        }
        event.preventDefault();
        if (Number.isInteger(focusedItemIndex)) {
          // suggestions are displayed and an item has the focus : we select it
          this.onSelect(event, { itemIndex: focusedItemIndex });
        } else if (this.state.value !== this.state.previousValue) {
          // there is no focused item and the current value is not persisted
          // we persist it
          this.updateValue(event, this.state.value);
        }
        this.resetSuggestions();
        break;
      case keycode.codes.down:
        event.preventDefault();
        if (!this.state.suggestions) {
          // display all suggestions when they are not displayed
          this.updateSuggestions();
        }
        this.setState({ focusedItemIndex: newFocusedItemIndex });
        break;
      case keycode.codes.up:
        event.preventDefault();
        this.setState({ focusedItemIndex: newFocusedItemIndex });
        break;
      default:
        break;
    }
  }

  onSelect(event, { itemIndex }) {
    const newValue = this.state.suggestions[itemIndex];
    this.updateValue(event, newValue.title);
  }

  updateValue(event, value, persist) {
    const previousValue = persist ? value.title : this.state.previousValue;
    this.setState({ value, previousValue });
    this.props.onChange({ value });
  }

  resetValue() {
    this.setState({
      suggestions: undefined,
      value: this.state.previousValue,
    });
  }

  updateSuggestions(value) {
    let suggestions = this.state.allSuggestions;
    if (value) {
      const escapedValue = escapeRegexCharacters(value.trim());
      const regex = new RegExp(escapedValue, 'i');
      suggestions = suggestions.filter(itemValue => regex.test(itemValue.title) || regex.test(itemValue.description));
    }

    this.setState({ suggestions });
  }

  resetSuggestions() {
    this.setState({
      suggestions: undefined,
      focusedItemIndex: undefined,
    });
  }

  render() {
    return (
      <div className={theme.CategorySelector}>
        <div className={theme['tf-datalist']}>
          <Typeahead
            focusedItemIndex={this.state.focusedItemIndex}
            items={this.state.suggestions}
            onBlur={this.onBlur}
            onChange={this.onChange}
            onFocus={this.onFocus}
            onKeyDown={this.onKeyDown}
            onSelect={this.onSelect}
            theme={this.theme}
            value={this.state.value}
            icon={{name: "talend-search", title: "Toggle search input", bsStyle: "link"}}
            placeholder="Select or create a category..."
            multiSection={false}
            autoFocus={false}
          />
          <span className={theme.categorySeparator}>/</span>
          <Input className="form-control" type="text" placeholder="Enter the component family specific category..."
                 aggregate={this.state.specific} accessor="value"/>
        </div>
      </div>
    );
  }
}
