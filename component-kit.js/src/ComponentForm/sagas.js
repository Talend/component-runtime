
import { call, put, select, take, takeEvery } from 'redux-saga/effects';
import cmf from '@talend/react-cmf';
import Component from './ComponentForm.component';

function* fecthDefinition(url, componentId) {
	const { data, response } = yield call(cmf.sagas.http.get, url);
	if (!response.ok) {
		return;
	}
	yield put(Component.setStateAction(data, componentId));
}

function* onDidMount({ componentId = 'demo', definitionURL }) {
	const state = yield select();
	if (!Component.getState(state, componentId).get('jsonSchema')) {
		yield fecthDefinition(definitionURL, componentId);
	}
}

function* onTrigger(action) {
	if (action.jsonSchema || action.uiSchema) {
		yield put(Component.setStateAction(action, action.event.componentId));
	}
}

function* handle(props) {
	yield call(onDidMount, props);
	yield takeEvery('TCOMP_FORM_ON_TRIGGER', onTrigger);
	yield take('DO_NOT_QUIT');
}

export default {
	'ComponentForm#default': handle,
};
