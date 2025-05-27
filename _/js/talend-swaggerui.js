$(document).ready(function () {
  window.ui = SwaggerUIBundle({
    spec: window.talend.swaggerUi,
    dom_id: '#swagger-ui',
    deepLinking: true,
    presets: [
      SwaggerUIBundle.presets.apis,
      SwaggerUIStandalonePreset.slice(1)
    ],
    plugins: [
      SwaggerUIBundle.plugins.DownloadUrl
    ],
    layout: 'StandaloneLayout'
  });
});
