import React from 'react';
import PropTypes from 'prop-types';
import Toggle from '@talend/react-components/lib/Toggle';
import theme from './EmbeddableToggle.scss';
import Help from '../Help';
import Schema from '../Schema';

class EmbeddableToggle extends React.Component {
	static propTypes = {
		connection: PropTypes.object,
	};
	constructor(props) {
		super(props);
		this.state = {
			checked: !!props.connection.generic,
			structure: props.connection.structure,
		};
		this.onChange = this.onChange.bind(this);
	}

	componentWillReceiveProps(nextProps) {
		if (this.props !== nextProps) {
			this.setState({
				checked: !!nextProps.connection.generic,
				structure: nextProps.connection.structure,
			});
		}
	}

	onChange() {
		this.setState(() => {
			this.props.connection.generic = !this.props.connection.generic;
			return { checked: this.props.connection.generic };
		});
	}

	render() {
		return (
			<schema-configuration>
				<div className={theme['form-row']}>
					<p className={theme.title}>
						Generic
						<Help
							title="Generic"
							i18nKey="processor_generic"
							content={
								<span>
									<p>
										Is this branch type generic, i.e. is the data strongly typed or can use a
										dynamic schema.
									</p>
									<p>
										Using a dynamic schema will allow you to read data from a structure you do not
										know at development time.
									</p>
								</span>
							}
						/>
					</p>
					<Toggle checked={this.state.checked} onChange={this.onChange} />
				</div>
				{!this.state.checked && (
					<div className={theme['form-row']}>
						<p className={theme.title}>Structure</p>
						<Schema schema={this.state.structure} readOnly name="root" />
					</div>
				)}
			</schema-configuration>
		);
	}
}

export default EmbeddableToggle;
