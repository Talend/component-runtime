This is a TCK connector to test and validate the checkpoint input feature.

## Sample configuration

### Sample configuration `configuration.json`
```json
{
  "configuration": {
    "dataset": {
      "maxRecords": 100
    }
  }
}
```

### Sample checkpoint configuration `checkpoint.json`
```json
{
  "$checkpoint": {
    "sinceId": 10,
    "status": "running",
    "__version": 2
  }
}
```


## Testing migration

Provide a `checkpoint.json` file with the following content:

```json
{
  "$checkpoint": {
    "since_id": 10,
    "status": "running",
    "__version": 1
  }
}
```
