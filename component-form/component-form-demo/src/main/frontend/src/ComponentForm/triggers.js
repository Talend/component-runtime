import jsonpath from 'jsonpath';
import { COMPONENT_ACTION_URL } from '../constants';

export default function onDefaultTrigger(event, { trigger, schema, properties }) {
	const payload = {};
	for(const param of trigger.parameters) {
		payload[param.key] = jsonpath.query(properties, `$.${param.path}`, 1)[0];
	}
	return fetch(
		`${COMPONENT_ACTION_URL}?action=${trigger.action}&family=${trigger.family}&type=${trigger.type}`,
		{
			method: 'post',
			headers: { 'Content-Type': 'application/json' },
			body: JSON.stringify(payload),
		}
	).then(resp => {
		if(resp.ok) {
			return {};
		}
		return resp.json()
			.then(body => {
				const errorMessage = body && body.error ?
					body.error :
					`${resp.status}: ${resp.statusText}`;
				return { errors: {
					[schema.key]: errorMessage,
				} };
			});
	});
}