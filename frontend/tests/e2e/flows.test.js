import { setupDriver, teardown } from './setup.js';
import { LoginPage } from './pages/LoginPage.js';
import { ProjectPage } from './pages/ProjectPage.js';
import { ETLDesignerPage } from './pages/ETLDesignerPage.js';
import { writeFileSync } from 'fs';
import { Buffer } from 'buffer';

// Set timeout for entire test suite
//jest.setTimeout(30000);

describe('ETL Flow Tests', () => {
    let driver;
    let loginPage;
    let projectPage;
    let designerPage;
    const testProjectName = 'Test Project ' + Date.now();

    beforeAll(async () => {
        try {
            console.log('Setting up test environment...');
            driver = await setupDriver();
            loginPage = new LoginPage(driver);
            projectPage = new ProjectPage(driver);
            designerPage = new ETLDesignerPage(driver);
            console.log('Test environment setup complete');
        } catch (error) {
            console.error('Setup failed:', error);
            throw error;
        }
    }, 30000);

    afterAll(async () => {
        try {
            console.log('Tearing down test environment...');
            await teardown(driver);
            console.log('Teardown complete');
        } catch (error) {
            console.error('Teardown failed:', error);
            throw error;
        }
    }, 30000);

    beforeEach(async () => {
        try {
            console.log('Starting login process...');
            await loginPage.navigate();
            await loginPage.login('admin', 'admin');
            console.log('Login successful');
            
            // Add a small delay after login
            await driver.sleep(2000);
            
            // Get and log current URL
            const currentUrl = await driver.getCurrentUrl();
            console.log('Current URL after login:', currentUrl);
            
            // Get and log page source
            const pageSource = await driver.getPageSource();
            console.log('Page source excerpt:', pageSource.substring(0, 500));
            
        } catch (error) {
            console.error('Login process failed:', error);
            throw error;
        }
    }, 30000);  // Increased timeout for login

    test('should create project and ETL flow', async () => {
        try {
            console.log('Starting project creation test...');
            
            // Navigate to projects page
            console.log('Navigating to projects page...');
            await projectPage.navigate();
            
            // Add a small delay after navigation
            await driver.sleep(2000);
            
            // Create project
            console.log('Creating project:', testProjectName);
            await projectPage.createProject(testProjectName);
            
            // Add a small delay after project creation
            await driver.sleep(2000);
            
            // Open project
            console.log('Opening project:', testProjectName);
            await projectPage.openProject(testProjectName);
            
            // Rest of the test...
            console.log('Creating sheet...');
            await designerPage.createNewSheet('Test Sheet');
            
            const sheetTitle = await driver.findElement({ css: '.sheet-title' });
            expect(await sheetTitle.getText()).toBe('Test Sheet');
            
            // Add components and create flow...
            console.log('Test completed successfully');
            
        } catch (error) {
            console.error('Test failed:', error);
            
            // Capture screenshot on failure using ES modules syntax
            try {
                const screenshot = await driver.takeScreenshot();
                writeFileSync('error-screenshot.png', Buffer.from(screenshot, 'base64'));
                console.log('Screenshot saved as error-screenshot.png');
            } catch (screenshotError) {
                console.error('Failed to save screenshot:', screenshotError);
            }
            
            throw error;
        }
    }, 60000);
}); 