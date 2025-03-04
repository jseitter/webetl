import { until } from 'selenium-webdriver';

export class LoginPage {
    constructor(driver) {
        this.driver = driver;
    }

    async navigate() {
        await this.driver.get('http://localhost:5173');
        // Wait for the login form to be visible
        await this.driver.wait(
            until.elementLocated({ css: '.login-form' }), 
            10000,
            'Login form not found'
        );
    }

    async login(username, password) {
        console.log('Starting login process...');
        
        // Wait for elements to be present and visible
        const usernameInput = await this.driver.wait(
            until.elementLocated({ css: 'input[type="text"]' }), 
            10000,
            'Username input not found'
        );
        const passwordInput = await this.driver.wait(
            until.elementLocated({ css: 'input[type="password"]' }), 
            10000,
            'Password input not found'
        );
        const submitButton = await this.driver.wait(
            until.elementLocated({ css: 'button[type="submit"]' }), 
            10000,
            'Submit button not found'
        );

        console.log('Found all login form elements');

        await usernameInput.clear();
        await usernameInput.sendKeys(username);
        await passwordInput.clear();
        await passwordInput.sendKeys(password);
        
        console.log('Filled in credentials');
        
        await submitButton.click();
        console.log('Clicked submit button');

        // Wait for navigation to complete - look for project list specifically
        try {
            await this.driver.wait(
                until.elementLocated({ css: '.project-list' }), 
                10000,
                'Project list not found after login'
            );
            console.log('Successfully navigated to project list');
        } catch (error) {
            console.error('Navigation error:', error);
            
            // Get current URL and page source for debugging
            const url = await this.driver.getCurrentUrl();
            const source = await this.driver.getPageSource();
            console.log('Current URL:', url);
            console.log('Page source:', source.substring(0, 500) + '...'); // First 500 chars
            
            throw error;
        }
    }
} 