const { until } = require('selenium-webdriver');
const fs = require('fs');

async function waitForElement(driver, selector, timeout = 5000) {
    const element = await driver.wait(
        until.elementLocated({ css: selector }), 
        timeout
    );
    return element;
}

async function waitForElementToBeVisible(driver, element, timeout = 5000) {
    await driver.wait(
        until.elementIsVisible(element),
        timeout
    );
}

async function takeScreenshot(driver, name) {
    const screenshot = await driver.takeScreenshot();
    fs.writeFileSync(`screenshots/${name}.png`, screenshot, 'base64');
}

module.exports = {
    waitForElement,
    waitForElementToBeVisible,
    takeScreenshot
}; 