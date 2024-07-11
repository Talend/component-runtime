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
import '@talend/bootstrap-theme/dist/bootstrap.css';
import React from 'react';
import ReactDOM from 'react-dom';
import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import App from './components/App';
import helpTexts from './locales/en/Help.json';

// import './intellij.css';
import './index.scss';

function logKeys(lang, ns, key, value) {
	// eslint-disable-next-line no-console
	console.log(`${key}: "${value.replace(/"/g, '\\"')}"`);
}

// eslint-disable-next-line import/no-named-as-default-member
i18n.use(initReactI18next).init({
	lng: 'en',
	resources: {
		en: {
			Help: helpTexts,
			'tui-components': {
				HEADERBAR_GO_PORTAL: 'Go to Portal',
				NO_RESULT_FOUND: 'No result.',
				TYPEAHEAD_SEARCHING: 'Searching for matches...',
				TYPEAHEAD_LOADING: 'Loading...',
			},
		},
	},
	/*
  to capture all keys:
  1. activate next line
  2. update Help.js to drop the overlay trigger and wrap the popover in I18n
  3. launch the app and browse pages (don't forget configuration tabs)
  3. update the json content from the captured key/value pairs
  */
	saveMissing: true,
	missingKeyHandler: logKeys,
});
i18n.addResourceBundle('en', 'Help', helpTexts);

ReactDOM.render(<App />, document.getElementById('app'));
