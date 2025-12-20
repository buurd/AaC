const { defineConfig } = require("cypress");

module.exports = defineConfig({
  e2e: {
    setupNodeEvents(on, config) {
      // implement node event listeners here
    },
    baseUrl: 'http://pm-system:8001', // We will name the container 'pm-system'
    supportFile: false,
    video: false,
    screenshotOnRunFailure: false
  },
});