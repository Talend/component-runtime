import React from 'react';
import ReactDOM from 'react-dom';
import 'whatwg-fetch'
import '@talend/bootstrap-theme/dist/bootstrap.css';
import './index.css';
import App from './App';
import registerServiceWorker from './registerServiceWorker';

ReactDOM.render(<App />, document.getElementById('root'));
registerServiceWorker();
