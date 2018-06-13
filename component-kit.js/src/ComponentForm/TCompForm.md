# TCompForm

# AddForm

```javascript
<AddForm
    definitionURL

    triggerUrl
/>
```


Dump:
open AddDatastoreForm
render TCompForm
it get datastores.json to get definition
from /componentproxy/api/v1/configurations/form/initial/datastore

fill name
select component
it POST /componentproxy/api/v1/actions/execute?action=builtin%3A%3Aroot%3A%3AreloadFromId&family=builtin%3A%3Afamily&type=reload
payload: {"id":"U2VydmljZU5vdyNkYXRhc3RvcmUjYmFzaWNBdXRo"}

response: trigger-type.json


fill API_URL
it trigger POST calls to /api/v1/actions/execute?action=urlValidation&family=ServiceNow&type=validation
payload: { arg0:"twtrw"}
response: {"comment":"no protocol: twtrw","status":"KO"}
and comment becomes the error message

