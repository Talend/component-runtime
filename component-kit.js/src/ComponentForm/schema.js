import invariant from 'invariant';
import { migrate } from '@talend/react-forms/lib/UIForm/merge';

export function customizer(objValue, srcValue) {
	if (Array.isArray(objValue)) {
		return objValue.concat(srcValue.filter(value => !objValue.includes(value)));
	}
	return undefined;
}

export function getSafeSchema(schema) {
	invariant(schema, 'schema is required');
	invariant(schema.jsonSchema, 'schema.jsonSchema is required in a TCompForm');
	invariant(schema.uiSchema, 'schema.uiSchema is required in a TCompForm');

	if (!Array.isArray(schema.uiSchema)) {
		return migrate(schema.jsonSchema, schema.uiSchema);
	}
	return schema;
}
