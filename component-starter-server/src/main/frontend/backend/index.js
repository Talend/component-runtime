const configuration = require('./configuration.json');

function setup(app) {
    app.get('/api/project/configuration', function (req, res) {
      res.json(configuration);
    });
}

module.exports = setup;