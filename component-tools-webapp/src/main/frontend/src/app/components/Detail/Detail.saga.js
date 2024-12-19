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
import { takeLatest, put } from "redux-saga/effects";
import { ComponentForm } from "@talend/react-containers";
import { SUBMIT_COMPONENT } from "../../store/constants";

function* doSubmit({ properties, componentId }) {
	if (componentId === "detail-form") {
		yield put({ type: SUBMIT_COMPONENT, properties });
	}
}

export default function* start() {
	yield takeLatest(ComponentForm.ON_SUBMIT, doSubmit);
}
