const { defineConfig } = require("cypress");

module.exports = defineConfig({
  e2e: {
    setupNodeEvents(on, config) {
      // implement node event listeners here
    },
    baseUrl: 'https://reverse-proxy:8444', // PM System via HTTPS Proxy
    chromeWebSecurity: false, // Allow self-signed certs and cross-origin
    supportFile: 'cypress/support/e2e.js', // Enable support file
    video: false,
    screenshotOnRunFailure: false
  },
});