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
import { ActionButton } from '@talend/react-components/lib/Actions';
import { withTranslation } from 'react-i18next';
import DatastoreContext from '../../DatastoreContext';
import DatasetContext from '../../DatasetContext';
import ComponentsContext from '../../ComponentsContext';
import Help from '../Help';

function IsUsedWarning() {
	return (
		<Help
			title="Delete dataset"
			i18nKey="delete_dataset_warning_is_used"
			icon="talend-warning"
			content={<p>You can't delete this dataset because it's already used.</p>}
		/>
	);
}

function IsLastWarning() {
	return (
		<Help
			title="Delete dataset"
			i18nKey="delete_dataset_warning_is_last"
			icon="talend-warning"
			content={<p>You can't delete this dataset because it's the last one.</p>}
		/>
	);
}

function DoDelete(props) {
	return (
		<ActionButton
			label={props.t('DELETE_DATASET_LABEL', { defaultValue: 'Delete this dataset' })}
			onClick={props.onClick}
			icon="talend-trash"
			hideLabel
			bsStyle="link"
			className="btn-icon-only"
		/>
	);
}
DoDelete.propTypes = {
	t: PropTypes.func,
	onClick: PropTypes.func,
};

const DoDeleteTrans = withTranslation('Help')(DoDelete);

export default class DatasetDelete extends React.Component {
	static propTypes = {
		item: PropTypes.shape({
			$id: PropTypes.string,
		}),
	};

	constructor(props) {
		super(props);
		this.onService = this.onService.bind(this);
		this.onDelete = this.onDelete.bind(this);
	}

	onService(datastore, dataset, components) {
		this.datastore = datastore;
		this.dataset = dataset;
		this.components = components;
	}

	onDelete() {
		return (event) => {
			event.preventDefault();
			this.dataset.delete(this.props.item);
		};
	}

	isUsed() {
		// is their a sources or processors
		return (
			this.components.components.filter(
				(component) =>
					component.source.configurationStructure.entries.filter(
						(entry) => entry.reference === this.props.item.$id,
					).length > 0,
			).length > 0
		);
	}

	render() {
		return (
			<DatastoreContext.Consumer>
				{(datastore) => (
					<DatasetContext.Consumer>
						{(dataset) => (
							<ComponentsContext.Consumer>
								{(components) => {
									this.onService(datastore, dataset, components);
									if (this.isUsed()) {
										return <IsUsedWarning />;
									}
									if (dataset.datasets.length === 1) {
										return <IsLastWarning />;
									}
									return <DoDeleteTrans onClick={this.onDelete()} />;
								}}
							</ComponentsContext.Consumer>
						)}
					</DatasetContext.Consumer>
				)}
			</DatastoreContext.Consumer>
		);
	}
}
