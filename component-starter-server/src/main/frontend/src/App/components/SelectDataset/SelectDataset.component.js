import React from 'react';
import PropTypes from 'prop-types';
import theme from './SelectDataset.scss';

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
