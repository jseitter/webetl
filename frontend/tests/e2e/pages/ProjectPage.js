import { until } from 'selenium-webdriver';

export class ProjectPage {
    constructor(driver) {
        this.driver = driver;
    }

    async navigate() {
        console.log('Navigating to projects page...');
        await this.driver.get('http://localhost:5173/projects');
        await this.driver.wait(
            until.elementLocated({ css: '.project-list' }), 
            10000,
            'Project list not found'
        );
        console.log('Successfully loaded projects page');
    }

    async createProject(name) {
        console.log('Starting project creation:', name);
        
        // Wait for and click the new project button
        const createButton = await this.driver.wait(
            until.elementLocated({ css: '[data-testid="create-project-button"]' }), 
            10000,
            'Create project button not found'
        );
        await this.driver.wait(
            until.elementIsVisible(createButton),
            10000,
            'Create project button not visible'
        );
        console.log('Found create project button');
        await createButton.click();
        console.log('Clicked create project button');

        // Wait for dialog to appear
        const dialog = await this.driver.wait(
            until.elementLocated({ css: '.MuiDialog-root' }), 
            10000,
            'Project dialog not found'
        );
        console.log('Project dialog opened');

        // Find and fill in the project name
        const nameInput = await dialog.findElement({ css: 'input[name="projectName"]' });
        await nameInput.clear();
        await nameInput.sendKeys(name);
        console.log('Entered project name');

        // Find and click the save button
        const saveButton = await dialog.findElement({ css: 'button[type="submit"]' });
        await saveButton.click();
        console.log('Clicked save button');

        // Wait for the new project to appear in the list
        await this.driver.wait(
            until.elementLocated({ css: `[data-testid="project-item-${name}"]` }), 
            10000,
            'New project not found in list'
        );
        console.log('Project created successfully');
    }

    async openProject(name) {
        console.log('Opening project:', name);
        
        // Wait for and click the project link
        const projectLink = await this.driver.wait(
            until.elementLocated({ css: `[data-testid="project-item-${name}"] a` }), 
            10000,
            'Project link not found'
        );
        await this.driver.wait(
            until.elementIsVisible(projectLink),
            10000,
            'Project link not visible'
        );
        console.log('Found project link');
        
        await projectLink.click();
        console.log('Clicked project link');

        // Wait for ETL designer to load
        await this.driver.wait(
            until.elementLocated({ css: '.etl-designer' }), 
            10000,
            'ETL designer not loaded'
        );
        console.log('Successfully navigated to ETL designer');
    }
} 