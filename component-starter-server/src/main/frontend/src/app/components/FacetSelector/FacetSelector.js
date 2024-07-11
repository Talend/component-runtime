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
import Badge from '@talend/react-components/lib/Badge';
import Typeahead from '@talend/react-components/lib/Typeahead';
import Help from '../Help';

import theme from './FacetSelector.module.scss';

function escapeRegexCharacters(str) {
	return str.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

// highly inspired from MultiSelectTag but in a way it integrates with this app layout
// note: the map/filter etc would be worth using underscore,
// for now we assume the suggestions are not numerous enough to require it

function getSelected(props, typeaheadConfig) {
	if (props.selected) {
		return typeaheadConfig
			.map((cat) => cat.suggestions.filter((s) => props.selected.indexOf(s.title) >= 0))
			.reduce((a, i) => a.concat(i), []);
	}
	return [];
}

export default class FacetSelector extends React.Component {
	static propTypes = {
		facets: PropTypes.object,
		selected: PropTypes.array,
	};
	constructor(props) {
		super(props);

		const typeaheadConfig = Object.keys(this.props.facets).map((category) => {
			const facets = this.props.facets[category];
			return {
				// icon: {name: "talend-filter", title: "icon"},
				title: category,
				suggestions: facets.map((suggestion) => ({
					title: suggestion.name,
					description: suggestion.description,
					// category: item,
				})),
			};
		});

		this.state = {
			facetTypeahead: typeaheadConfig,
			selected: getSelected(props, typeaheadConfig),
		};

		this.onChange = this.onChange.bind(this);
		this.onFocus = this.onFocus.bind(this);
		this.onKeyDown = this.onKeyDown.bind(this);
		this.onRemoveTag = this.onRemoveTag.bind(this);
		this.onAddTag = this.onAddTag.bind(this);
		this.resetSuggestions = this.resetSuggestions.bind(this);
	}

	onKeyDown(event, { focusedItemIndex, newFocusedItemIndex }) {
		switch (event.key) {
			case 'Enter':
				event.preventDefault();
				if (Number.isInteger(focusedItemIndex)) {
					this.onAddTag(event, { itemIndex: focusedItemIndex });
				}
				break;
			case 'ArrowDown':
			case 'Down':
			case 'ArrowUp':
			case 'Up':
				event.preventDefault();
				this.setState({ focusedItemIndex: newFocusedItemIndex });
				break;
			case 'Backspace':
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
		this.setState((state) =>
			state.selected.push(this.state.suggestions[sectionIndex].suggestions[itemIndex]),
		);
		this.updateSuggestions();
	}

	onRemoveTag(itemIndex) {
		this.props.selected.splice(itemIndex, 1);
		this.setState((state) => state.selected.splice(itemIndex, 1));
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
			let suggestions = this.state.facetTypeahead;
			if (value) {
				const escapedValue = escapeRegexCharacters(value.trim());

				// todo: use bloodhood engine
				const regex = new RegExp(escapedValue, 'i');

				suggestions = this.state.facetTypeahead
					.map((category) => ({
						icon: category.icon,
						title: category.title,
						suggestions: category.suggestions.filter((item) => regex.test(item.title)),
					}))
					.filter((category) => category.suggestions.length > 0);
			}
			return {
				focusedItemIndex: suggestions.length ? 0 : undefined,
				suggestions,
				value: !!value && value.length === 0 ? undefined : value,
			};
		});
	}

	render() {
		return (
			<div className={`form-group ${theme.wrapper}`}>
				<div>
					<label htmlFor="facets">Facets</label>
					<Typeahead
						id="facets"
						icon={{ name: 'talend-search', title: 'Toggle search input', bsStyle: 'link' }}
						placeholder="Select a facet to add to your project"
						// multiSection={false}
						// autoFocus={false}
						value={this.state.value}
						items={this.state.suggestions}
						onBlur={this.resetSuggestions}
						onChange={this.onChange}
						onFocus={this.onFocus}
						onKeyDown={this.onKeyDown}
						onSelect={this.onAddTag}
						theme={theme}
						// tabindex="-1"
					/>
					<div>
						<Help
							title="Facets"
							i18nKey="facets_values"
							content={
								<span>
									<p>
										Selecting a facet allows you to adds some features to your generated project.
									</p>
									<p>
										A common use case is to activate the testing facet to have skeletons for tests.
									</p>
								</span>
							}
						/>
					</div>
				</div>

				<div className={theme.badges}>
					{this.state.selected.map((item, index) => (
						<Badge
							label={item.title}
							category={item.category}
							key={index}
							onDelete={() => this.onRemoveTag(index)}
						/>
					))}
				</div>
			</div>
		);
	}
}
