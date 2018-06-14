
import { call, put, select, take, takeEvery } from 'redux-saga/effects';
import cmf from '@talend/react-cmf';
import Component from './ComponentForm.component';

function* fecthDefinition({ definitionURL, componentId }) {
	const { data, response } = yield call(cmf.sagas.http.get, definitionURL);
	if (!response.ok) {
		return;
	}
	yield put(Component.setStateAction(data, componentId));
}

function* onDidMount({ componentId = 'demo', definitionURL }) {
	const state = yield select();
	if (!Component.getState(state, componentId).get('jsonSchema')) {
		yield fecthDefinition({ definitionURL, componentId });
	}
}

function* handle(props) {
	yield call(onDidMount, props);
	yield takeEvery(Component.DEFINITION_URL_CHANGED, fecthDefinition)
	yield take('DO_NOT_QUIT');
}

export default {
	'ComponentForm#default': handle,
};
