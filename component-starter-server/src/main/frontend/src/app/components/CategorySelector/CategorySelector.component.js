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
import React from 'react';
import PropTypes from 'prop-types';
import Typeahead from '@talend/react-components/lib/Typeahead';

import theme from './CategorySelector.module.scss';

function escapeRegexCharacters(str) {
	return str.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

export default class CategorySelector extends React.Component {
	static propTypes = {
		initialValue: PropTypes.string,
		onChange: PropTypes.func,
	};
	constructor(props) {
		super(props);
		this.onBlur = this.onBlur.bind(this);
		this.onChange = this.onChange.bind(this);
		this.onFocus = this.onFocus.bind(this);
		this.onKeyDown = this.onKeyDown.bind(this);
		this.onSelect = this.onSelect.bind(this);

		this.theme = {
			container: [theme.container, 'tf-datalist-container'].join(' '),
			itemsContainer: theme['items-container'],
			itemsList: theme.items,
		};

		this.state = {
			previousValue: props.initialValue,
			value: props.initialValue,
			allSuggestions: [
				{
					title: 'Business',
					description: 'Category about business related components',
				},
				{
					title: 'Databases',
					description: 'Category about RDBMS or SQL databases',
				},
				{
					title: 'Big Data',
					description: 'Category about other databases',
				},
				{
					title: 'Business Intelligence',
					description: 'Category about business intelligence components',
				},
				{
					title: 'Cloud',
					description: 'Category about cloud solutions',
				},
				{
					title: 'Internet',
					description: 'Category about internet connectivity',
				},
				{
					title: 'Misc',
					description: 'Default category',
				},
			],
		};
	}

	onBlur() {
		this.resetSuggestions();
		if (this.hasChanged()) {
			this.updateValue(this.state.value);
		}
	}

	onChange(event, { value }) {
		this.updateSuggestions(value);
		this.updateValue(value.title || (!!value && value.length > 0 ? value : ''));
	}

	onFocus() {
		this.updateSuggestions(this.state.value);
	}

	onKeyDown(event, { focusedItemIndex, newFocusedItemIndex }) {
		switch (event.key) {
			case 'Escape':
				event.preventDefault();
				this.resetValue();
				break;
			case 'Enter':
				if (!this.state.suggestions) {
					break;
				}
				event.preventDefault();
				if (Number.isInteger(focusedItemIndex)) {
					// suggestions are displayed and an item has the focus : we select it
					this.onSelect(event, { itemIndex: focusedItemIndex });
				} else if (this.hasChanged()) {
					// there is no focused item and the current value is not persisted
					// we persist it
					this.updateValue(this.state.value);
				}
				this.resetSuggestions();
				break;
			case 'ArrowDown':
			case 'Down':
				event.preventDefault();
				if (!this.state.suggestions) {
					// display all suggestions when they are not displayed
					this.updateSuggestions();
				}
				this.setState({ focusedItemIndex: newFocusedItemIndex });
				break;
			case 'ArrowUp':
			case 'Up':
				event.preventDefault();
				this.setState({ focusedItemIndex: newFocusedItemIndex });
				break;
			default:
				break;
		}
	}

	onSelect(event, { itemIndex }) {
		const newValue = this.state.suggestions[itemIndex];
		this.updateValue(newValue.title);
	}

	hasChanged() {
		return this.state.value !== this.state.previousValue;
	}

	updateValue(value) {
		const previousValue = value;
		const aggregate = value.title || (!!value && value.length > 0 ? value : 'Misc');
		this.props.onChange({ value: aggregate });
		this.setState({ value, previousValue });
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
			suggestions = suggestions.filter(
				(itemValue) => regex.test(itemValue.title) || regex.test(itemValue.description),
			);
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
						icon={{ name: 'talend-search', title: 'Toggle search input', bsStyle: 'link' }}
						placeholder="Select or create a category..."
						multiSection={false}
						autoFocus={false}
					/>
				</div>
			</div>
		);
	}
}
