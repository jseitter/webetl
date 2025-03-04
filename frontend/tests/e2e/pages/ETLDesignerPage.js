import { until } from 'selenium-webdriver';

export class ETLDesignerPage {
    constructor(driver) {
        this.driver = driver;
    }

    async createNewSheet(name) {
        const newButton = await this.driver.findElement({ css: 'button[aria-label="Create New Sheet"]' });
        await newButton.click();

        const nameInput = await this.driver.findElement({ css: 'input[name="sheetName"]' });
        await nameInput.sendKeys(name);

        const saveButton = await this.driver.findElement({ css: 'button[type="submit"]' });
        await saveButton.click();
    }

    async addComponent(type, position) {
        const sidebar = await this.driver.findElement({ css: '.component-sidebar' });
        const component = await sidebar.findElement({ css: `[data-component="${type}"]` });
        
        const canvas = await this.driver.findElement({ css: '.react-flow' });
        const actions = this.driver.actions();
        
        await actions
            .dragAndDrop(component, canvas)
            .move({ x: position.x, y: position.y })
            .perform();
    }

    async connectComponents(sourceId, targetId) {
        const sourceHandle = await this.driver.findElement({ 
            css: `[data-nodeid="${sourceId}"] .source-handle` 
        });
        const targetHandle = await this.driver.findElement({ 
            css: `[data-nodeid="${targetId}"] .target-handle` 
        });

        const actions = this.driver.actions();
        await actions
            .move({ origin: sourceHandle })
            .press()
            .move({ origin: targetHandle })
            .release()
            .perform();
    }
} 