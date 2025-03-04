import { Builder } from 'selenium-webdriver';
import chrome from 'selenium-webdriver/chrome';

export async function setupDriver() {
    const options = new chrome.Options();
    // Add options for CI environment if needed
    // options.addArguments('--headless');
    
    const driver = await new Builder()
        .forBrowser('chrome')
        .setChromeOptions(options)
        .build();
        
    await driver.manage().window().setRect({ width: 1920, height: 1080 });
    return driver;
}

export async function teardown(driver) {
    if (driver) {
        await driver.quit();
    }
} 