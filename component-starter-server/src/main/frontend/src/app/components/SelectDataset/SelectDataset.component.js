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
 */import React from 'react';
import PropTypes from 'prop-types';
import theme from './SelectDataset.module.scss';

function getOptionProps(selectProps, dataset, index) {
	const props = {
		key: index,
		value: dataset.index
	}
	if (selectProps.current === dataset.id) {
		props.selected = true;
	}
	return props;
}

function SelectDataset(props) {
	return (
		<div className="form-group">
			<label htmlFor="select-dataset">Select a dataset</label>
			<select className="form-control" id="select-dataset" onChange={props.onChange} value={props.value}>
				<option value="">--Please choose one--</option>
				<option value="create-new">Create a new dataset</option>
				{props.datasets.map((dataset, index) => (
					<option {...getOptionProps(props, dataset, index)}>{dataset.name}</option>
				))}
			</select>
		</div>
	);
}

SelectDataset.displayName = 'SelectDataset';
SelectDataset.propTypes = {
	onChange: PropTypes.func,
	dataset: PropTypes.arrayOf(PropTypes.shape({
		id: PropTypes.string,
		name: PropTypes.string,
	})),
};

export default SelectDataset;
