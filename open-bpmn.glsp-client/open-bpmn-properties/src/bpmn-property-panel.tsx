/********************************************************************************
 * Copyright (c) 2022 Imixs Software Solutions GmbH and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the Eclipse
 * Public License v. 2.0 are satisfied: GNU General Public License, version 2
 * with the GNU Classpath Exception which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 ********************************************************************************/
import {
    EditorContextService, EnableToolPaletteAction,
    GLSPActionDispatcher, RequestAction, ResponseAction, ServerMessageAction, hasArguments, hasStringProp
} from '@eclipse-glsp/client';
import { Action, RequestContextActions, SetContextActions } from '@eclipse-glsp/protocol';
import {
    AbstractUIExtension,
    EnableDefaultToolsAction,
    EnableToolsAction,
    IActionHandler,
    ICommand,
    SModelElement,
    SModelRoot,
    SetUIExtensionVisibilityAction,
    TYPES
} from 'sprotty';
import { SelectionListener, SelectionService } from '@eclipse-glsp/client/lib/features/select/selection-service';
import { JsonForms } from '@jsonforms/react';
import { vanillaCells, vanillaRenderers } from '@jsonforms/vanilla-renderers';
import { inject, injectable, postConstruct } from 'inversify';
import * as React from 'react';
import { createRoot } from 'react-dom/client';
import { codiconCSSClasses } from 'sprotty/lib/utils/codicon';

import { SelectItemComboRendererEntry, SelectItemRendererEntry } from './SelectItemControl';
import { TextFileEditorRendererEntry } from './TextFileEditorControl';
import { isBoundaryEvent } from '@open-bpmn/open-bpmn-model/lib/model';
// eslint-disable-next-line @typescript-eslint/explicit-function-return-type
@injectable()
export class BPMNPropertyPanel extends AbstractUIExtension implements SelectionListener, IActionHandler {

    static readonly ID = 'bpmn-property-panel';

    @inject(TYPES.IActionDispatcher) protected actionDispatcher: GLSPActionDispatcher;
    @inject(EditorContextService)
    protected readonly editorContext: EditorContextService;

    @inject(SelectionService)
    protected selectionService: SelectionService;

    protected bodyDiv: HTMLElement;
    protected headerDiv: HTMLElement;
    modelRootId: string;
    modelRoot: Readonly<SModelRoot>;
    selectedElementId: string;
    initForm: boolean;
    lastCategory: string;
    isResizing: boolean;
    panelToggle: boolean;
    currentY: number;
    headerTitle: HTMLElement;
    protected panelContainer: any;

    @postConstruct()
    postConstruct(): void {
        this.selectionService.register(this);
    }

    id(): string {
        return BPMNPropertyPanel.ID;
    }
    containerClass(): string {
        return BPMNPropertyPanel.ID;
    }

    /*
     * Initialize the elements of property panel
     */
    protected initializeContents(_containerElement: HTMLElement): void {
        this.createHeader();
        this.createBody();
    }

    /**
     * Updates the containerElement under the given context before it becomes visible.
     * We override this method to preselect the root element as a default selection.
     *
     * @param _containerElement
     * @param root
     */
    protected override onBeforeShow(_containerElement: HTMLElement, root: Readonly<SModelRoot>): void {
        this.modelRootId = root.id;
        // preselect the root element showing the diagram properties
        this.selectionChanged(root, []);
    }

    /*
     * This method creates a header bar that can be grabbed with the mouse
     * to resize the height of the property panel.
     */
    protected createHeader(): void {
        const headerCompartment = document.createElement('div');
        headerCompartment.classList.add('bpmn-property-header');
        headerCompartment.append(this.createHeaderTitle());
        headerCompartment.appendChild(this.createHeaderTools());
        this.containerElement.appendChild(headerCompartment);
        this.headerDiv = headerCompartment;

        // eslint-disable-next-line arrow-parens
        this.headerDiv.addEventListener('mousedown', (e) => {
            this.isResizing = true;
            this.currentY = e.clientY;
        });
        // eslint-disable-next-line arrow-parens
        this.headerDiv.addEventListener('mouseup', (e) => {
            this.isResizing = false;
        });
        // eslint-disable-next-line arrow-parens
        window.addEventListener('mousemove', (e) => {
            if (!this.isResizing) {
                return;
            }
            const parent = this.containerElement.parentElement;
            let _newheight = this.containerElement.offsetHeight - (e.clientY - this.currentY);
            // fix minheigt
            if (_newheight < 28) {
                _newheight = 28;
                this.isResizing = false;
            }
            // fix maxheight
            if (parent && _newheight > parent.offsetHeight - 28) {
                _newheight = parent.offsetHeight - 28;
                this.isResizing = false;
            }
            this.containerElement.style.height = `${_newheight}px`;
            this.currentY = e.clientY;

            // if the mouse is no longer within the diagram plane, we stop the resizing
            if (parent && !parent.matches(':hover')) {
                this.isResizing = false;
            }
        });
    }

    protected createBody(): void {
        const bodyDiv = document.createElement('div');
        bodyDiv.classList.add('bpmn-properties-body');
        this.containerElement.appendChild(bodyDiv);
        this.bodyDiv = bodyDiv;
    }

    protected createHeaderTitle(): HTMLElement {
        const header = document.createElement('div');
        header.classList.add('header-icon');
        header.appendChild(createIcon('extensions'));
        this.headerTitle = document.createElement('span');
        header.appendChild(this.headerTitle);
        this.headerTitle.textContent = 'BPMN Properties';
        return header;
    }

    /*
     * This method creates the header tool buttons to control the size of the panel
     * Icons can be found here:
     * https://microsoft.github.io/vscode-codicons/dist/codicon.html
     */
    private createHeaderTools(): HTMLElement {
        const headerTools = document.createElement('div');
        headerTools.classList.add('header-tools');

        const hideToolButton = createIcon('chrome-minimize');
        hideToolButton.title = 'Hide Property Panel';
        hideToolButton.onclick = this.onClickResizePanel(hideToolButton, 'TOOL_COMMAND_HIDE');
        headerTools.appendChild(hideToolButton);

        const maximizeToolButton = createIcon('chevron-up');
        maximizeToolButton.title = 'Maximize Property Panel';
        maximizeToolButton.onclick = this.onClickResizePanel(maximizeToolButton, 'TOOL_COMMAND_MAXIMIZE');
        headerTools.appendChild(maximizeToolButton);

        return headerTools;
    }

    /**
     * Helper method to resize the property panel
     *
     * @param button
     * @param toolId
     * @returns
     */
    protected onClickResizePanel(button: HTMLElement, toolId?: string) {
        return (_ev: MouseEvent) => {
            if (!this.editorContext.isReadonly) {
                const action = toolId ? EnableToolsAction.create([toolId]) : EnableDefaultToolsAction.create();
                this.actionDispatcher.dispatch(action);
                if (toolId === 'TOOL_COMMAND_MAXIMIZE') {
                    this.containerElement.style.height = '50%';
                }
                if (toolId === 'TOOL_COMMAND_HIDE') {
                    this.containerElement.style.height = '28px';
                }
                // restore the default actions in the diagram panel (like move elements)
                this.actionDispatcher.dispatch(EnableDefaultToolsAction.create());
            }
        };
    }

    /*
     * We react on different event actions.
     *
     * The EnableToolPaletteAction is used to make the property panel visible
     * If we already have the bodyDiv defined, than we skip this event.
     * See Discussion: https://githubs.com/eclipse-glsp/glsp/discussions/910
     *
     * The BPMNPropertyPanelToggleAction is used to show/hide the
     * propertyPanel. This action is called by context menus.
     *
     * The BPMNPropertyPanelUpdateAction is send by the server during an update
     * of the data state and indicates that the property panel need to be refreshed.
     */
    handle(action: Action): ICommand | Action | void {
        // check enable state
        if (!this.bodyDiv && action.kind === EnableToolPaletteAction.KIND) {
            const requestAction = RequestContextActions.create({
                // contextId: ToolPalette.ID,
                contextId: BPMNPropertyPanel.ID,
                editorContext: {
                    selectedElementIds: []
                }
            });
            this.actionDispatcher.requestUntil(requestAction).then(response => {
                if (SetContextActions.is(response)) {
                    this.actionDispatcher.dispatch(
                        SetUIExtensionVisibilityAction.create({
                            extensionId: BPMNPropertyPanel.ID,
                            visible: !this.editorContext.isReadonly
                        })
                    );
                }
            });
        }

        // Toggle the property panel. Action is triggered by context menu
        if (action.kind === BPMNPropertyPanelToggleAction.KIND) {
            if (!this.panelToggle) {
                this.containerElement.style.height = '50%';
            } else {
                this.containerElement.style.height = '28px';
            }
            this.panelToggle = !this.panelToggle;
        }

        // Update content of the property panel. Action is triggered by the server
        if (action.kind === BPMNPropertyPanelUpdateAction.KIND) {
            this.updatePropertyPanel(this.selectedElementId);
        }
    }

    /*
     * This method reacts on the selection of a BPMN element. It can handel different
     * situations of a selection and updates the property panel input fields.
     *
     * The method also computes the last selected category. This is used in the setState method
     * to restore the last category if the element type has not changed.
     */
    selectionChanged(root: Readonly<SModelRoot>, selectedElements: string[]): void {
        this.modelRoot = root;
        // return if we do not yet have a body DIV.
        if (!this.bodyDiv) {
            return;
        }
        if (selectedElements && selectedElements.length > 0) {
            // first we need to verify if a Symbol/BPMNLabel combination was selected.
            // In this case we are only interested in the BPMNFlowElement and not in the label
            if (selectedElements.length > 1) {
                const filteredArr = selectedElements.filter(val => !val.endsWith('_bpmnlabel'));
                selectedElements = filteredArr;
            }
            // Next verify if we have task/Boundary selection
            // In this case we are only interested in the Task  and not in the Event
            if (selectedElements.length === 2) {
                const element_test = root.index.getById(selectedElements[1]);
                if (element_test && isBoundaryEvent(element_test)) {
                    // remove Boundary event from selection list
                    selectedElements.pop();
                }
            }
        }

        // if no element is selected we default to the root element (diagram plane)
        let _id;
        if (!selectedElements || selectedElements.length === 0) {
            _id = root.id;
        } else if (selectedElements.length === 1) {
            // we  have exactly one element selected.
            _id = selectedElements[0];
        }

        // avoid message loop...
        if (!_id || _id === this.selectedElementId || _id === 'EMPTY') {
            // skip this event!
            return;
        }

        // now if we have an element we show teh panel..
        if (_id) {
            // did we have a change?
            // set new selectionId
            this.selectedElementId = _id;
            // because the jsonForms send a onchange event after init we mark this state here
            this.initForm = true;
            // init the react container only once....
            if (!this.panelContainer) {
                this.panelContainer = createRoot(this.bodyDiv);
            }
            // finally update the panel
            this.updatePropertyPanel(_id);
        } else {
            // no single element selected!
            this.selectedElementId = '';
            if (this.bodyDiv && this.panelContainer) {
                this.headerTitle.textContent = 'BPMN Properties';
                // multi selection - we can not show a property panel
                this.panelContainer.render(<React.Fragment>Please select a single element </React.Fragment>);
            }
        }
    }
    /**
     * This helper method is responsible to refresh teh property panel.
     * The method loads the element from the root model context and updates
     * the JsonForms schemata.
     *
     * @param _elementID
     */
    updatePropertyPanel(_elementID: string): void {
        // return if we do not yet have a body DIV.
        if (!this.bodyDiv) {
            return;
        }
        // compute the current category....
        this.updateLastCategory();

        let element: SModelElement | undefined;
        // first we load the element from the model root
        if (this.modelRoot.id === _elementID) {
            element = this.modelRoot;
            this.headerTitle.textContent = 'Default Process';
        } else {
            // load element from index#
            element = this.modelRoot.index.getById(_elementID);
            if (element) {
                this.headerTitle.textContent = element.type;
            }
        }

        // update the property panel if we have a valid element
        if (element) {
            // BPMN Node selected, collect JSONForms schemata....
            let bpmnPropertiesData;
            let bpmnPropertiesSchema;
            let bpmnPropertiesUISchema;
            if (hasArguments(element)) {
                // parse the jsonForms schema details
                bpmnPropertiesData = JSON.parse(element.args.JSONFormsData + '');
                bpmnPropertiesSchema = JSON.parse(element.args.JSONFormsSchema + '');
                bpmnPropertiesUISchema = JSON.parse(element.args.JSONFormsUISchema + '');
            }
            // Build a generic JSONForms Property panel if we have at least an UISchema
            if (bpmnPropertiesUISchema) {
                // list of renderers declared outside the App component
                const bpmnRenderers = [
                    ...vanillaRenderers,
                    // optional register custom renderers...
                    SelectItemRendererEntry,
                    SelectItemComboRendererEntry,
                    TextFileEditorRendererEntry
                ];

                // render JSONForm // vanillaRenderers
                // we also set the key to the current elementID to reinitialize the form panel
                if (hasKeyValue(bpmnPropertiesUISchema, 'scope', '#/properties/export') &&
                    hasKeyValue(bpmnPropertiesUISchema, 'scope', '#/properties/tomcmt')) {
                    this.panelContainer.render(
                        <div>
                            <JsonForms
                                data={bpmnPropertiesData}
                                schema={bpmnPropertiesSchema}
                                uischema={bpmnPropertiesUISchema}
                                cells={vanillaCells}
                                renderers={bpmnRenderers}
                                onChange={({ errors, data }) => this.setState({ data })}
                                key={this.selectedElementId}
                            />
                            <input type="button" className="favorite styled" value="Export to Bonita" onClick={() => {
                                console.log('export to bonita has started');
                                // eslint-disable-next-line max-len
                                this.actionDispatcher.dispatch(ServerMessageAction.create('Start Converting DF-BPMN to Bonita proc', { severity: 'INFO', timeout: 5000 }));
                                this.actionDispatcher.request(
                                    MyCustomAction.create({ elementId: this.selectedElementId, additionalInformation: 'toBonita' })
                                );
                            }
                            } />

                            <input type="button" className="favorite styled" value="Export to MCMT" onClick={() => {
                                console.log('export to bonita has started');
                                // eslint-disable-next-line max-len
                                this.actionDispatcher.dispatch(ServerMessageAction.create('Start Converting DF-BPMN to MCMT', { severity: 'INFO', timeout: 5000 }));
                                this.actionDispatcher.request(
                                    MyCustomAction.create({ elementId: this.selectedElementId, additionalInformation: 'toMCMT' })
                                );
                            }
                            } />
                        </div>);
                } else if (hasKeyValue(bpmnPropertiesUISchema, 'scope', '#/properties/behavior') &&
                    hasKeyValue(bpmnPropertiesUISchema, 'scope', '#/properties/code')) {
                    this.panelContainer.render(
                        <div>
                            <JsonForms
                                data={bpmnPropertiesData}
                                schema={bpmnPropertiesSchema}
                                uischema={bpmnPropertiesUISchema}
                                cells={vanillaCells}
                                renderers={bpmnRenderers}
                                onChange={({ errors, data }) => this.setState({ data })}
                                key={this.selectedElementId}
                            />
                            {/* this button for generate gherkin syntax */}
                            <input type="button" className="favorite styled" value="Generate Behavior" onClick={async () => {
                                console.log('generate gherkin has been started');

                                // eslint-disable-next-line max-len
                                this.actionDispatcher.dispatch(ServerMessageAction.create('Start Converting Text to Gherkin', { severity: 'INFO', timeout: 5000 }));
                                this.actionDispatcher.request(
                                    MyCustomAction.create({ elementId: this.selectedElementId, additionalInformation: 'gherkin' })
                                );
                            }
                            } />
                            {/* this button for generate groovy script code */}
                            <input type="button" className="favorite styled" value="Generate Code" onClick={async () => {
                                console.log('generate groovy has been started');

                                // eslint-disable-next-line max-len
                                this.actionDispatcher.dispatch(ServerMessageAction.create('Start Converting Gherkin to Groovy', { severity: 'INFO', timeout: 5000 }));
                                this.actionDispatcher.request(
                                    MyCustomAction.create({ elementId: this.selectedElementId, additionalInformation: 'groovy' })
                                );
                            }
                            } />
                            {/* this button for generate groovy script code */}
                            <input type="button" className="favorite styled" value="Unit Test Generator" onClick={async () => {
                                console.log('generate unit test has been started');

                                // eslint-disable-next-line max-len
                                this.actionDispatcher.dispatch(ServerMessageAction.create('Start Generating unit-test for Groovy', { severity: 'INFO', timeout: 5000 }));
                                this.actionDispatcher.request(
                                    MyCustomAction.create({ elementId: this.selectedElementId, additionalInformation: 'unittest' })
                                );
                            }
                            } />
                        </div>);
                } else {
                    this.panelContainer.render(
                        <JsonForms
                            data={bpmnPropertiesData}
                            schema={bpmnPropertiesSchema}
                            uischema={bpmnPropertiesUISchema}
                            cells={vanillaCells}
                            renderers={bpmnRenderers}
                            onChange={({ errors, data }) => this.setState({ data })}
                            key={this.selectedElementId}
                        />);
                }
            } else {
                // we have no UISchema!
                this.selectedElementId = '';
                this.headerTitle.textContent = 'BPMN Properties';
            }
        } else {
            // we have no UISchema!
            this.selectedElementId = '';
            this.headerTitle.textContent = 'BPMN Properties';
        }

    }
    // clickExport(_newData: any): void {
    //     console.log('click export');
    //     const newJsonData = JSON.stringify(_newData.data);
    //     const action = new BPMNApplyPropertiesUpdateOperation(this.selectedElementId, newJsonData, 'Bonita Integration');
    //     this.actionDispatcher.dispatch(action);
    // }
    /*
     * This method is responsible to send the new data in a
     * ApplyEditOperation Action to the server....
     */
    setState(_newData: any): void {
        if (this.initForm) {
            // try to pre-select the last category
            const listCategories = this.bodyDiv.querySelectorAll('ul.category-subcategories li');
            for (let i = 0; i < listCategories.length; i++) {
                // Check if the LI element's text content matches our lastCategory
                if (listCategories[i].textContent === this.lastCategory) {
                    // Simulate the click event on the LI element
                    const event = new MouseEvent('click', {
                        view: window,
                        bubbles: true,
                        cancelable: true
                    });
                    listCategories[i].dispatchEvent(event);
                    break;
                }
            }

            // we do not dispatch the first onChange event
            // see https://jsonforms.io/docs/integrations/react/#onchange
            this.initForm = false;
            return;
        }
        // figure out the active category
        this.updateLastCategory();
        const newJsonData = JSON.stringify(_newData.data);
        const action = new BPMNApplyPropertiesUpdateOperation(this.selectedElementId, newJsonData, this.lastCategory);
        this.actionDispatcher.dispatch(action);
    }

    /**
     * Computes the current selected category
     */
    protected updateLastCategory(): void {
        const selectedListItem = this.bodyDiv.querySelector('ul.category-subcategories li.selected');
        if (selectedListItem && selectedListItem.textContent) {
            this.lastCategory = selectedListItem.textContent;
        }
    }
}

@injectable()
export class MyCustomResponseActionHandler implements IActionHandler {
    @inject(TYPES.IActionDispatcher) protected actionDispatcher: GLSPActionDispatcher;

    handle(action: MyCustomResponseAction): void | Action {
        // implement your custom logic to handle the action
        // Optionally issue a response action

        if (action.responceCode === '200') {
            this.actionDispatcher.dispatch(ServerMessageAction.create('Converting has been done', { severity: 'INFO', timeout: 5000 }));
        }
        else if (action.responceCode === '400') {
            this.actionDispatcher.dispatch(ServerMessageAction.create('Error in converting', { severity: 'ERROR', timeout: 5000 }));
        } else if (action.responceCode === '401') {
            this.actionDispatcher.dispatch(ServerMessageAction.create('Error in converting: No text to convert to Gherkin',
                { severity: 'ERROR', timeout: 5000 }));
        } else if (action.responceCode === '402') {
            this.actionDispatcher.dispatch(ServerMessageAction.create('Error in converting: No gherkin to convert to groovy script',
                { severity: 'ERROR', timeout: 5000 }));
        } else if (action.responceCode === '403') {
            this.actionDispatcher.dispatch(ServerMessageAction.create('Error in generating unit-test: No groovy script',
                { severity: 'ERROR', timeout: 5000 }));
        } else if (action.responceCode === '203') {
            this.actionDispatcher.dispatch(ServerMessageAction.create('Generating unit-test success',
                { severity: 'INFO', timeout: 5000 }));
            window.open(action.additionalInformation);
            console.log(action.additionalInformation);
        }else if (action.responceCode === '204') {
            this.actionDispatcher.dispatch(ServerMessageAction.create('The conversion of DF-BPMN to MCMT was successful',
                { severity: 'INFO', timeout: 5000 }));
        } else if (action.responceCode === '404') {
            this.actionDispatcher.dispatch(ServerMessageAction.create('Error in converting DF-BPMN to MCMT: check the logs',
                { severity: 'ERROR', timeout: 5000 }));
        }
        console.log('Recieved response from server');
    }
}

/**
 * This action is send after a property change to the backend providing the ID and the new value
 */
export class BPMNApplyPropertiesUpdateOperation implements Action {
    static readonly KIND = 'applyBPMNPropertiesUpdate';
    readonly kind = BPMNApplyPropertiesUpdateOperation.KIND;
    constructor(readonly id: string, readonly jsonData: string, readonly category: string) { }
}

export function createIcon(codiconId: string): HTMLElement {
    const icon = document.createElement('i');
    icon.classList.add(...codiconCSSClasses(codiconId));
    return icon;
}

export interface BPMNPropertyPanelToggleAction extends Action {
    kind: typeof BPMNPropertyPanelToggleAction.KIND;
}

export namespace BPMNPropertyPanelToggleAction {
    export const KIND = 'propertyPanelToggle';

    export function is(object: any): object is BPMNPropertyPanelToggleAction {
        return Action.hasKind(object, KIND);
    }

    export function create(): BPMNPropertyPanelToggleAction {
        return { kind: KIND };
    }
}

export interface BPMNPropertyPanelUpdateAction extends Action {
    kind: typeof BPMNPropertyPanelUpdateAction.KIND;
    // selectedElementID: string;
    // category: string;
}

export namespace BPMNPropertyPanelUpdateAction {
    export const KIND = 'propertyPanelUpdate';
    export function is(object: any): object is BPMNPropertyPanelUpdateAction {
        return Action.hasKind(object, KIND);
    }
    export function create(): BPMNPropertyPanelUpdateAction {
        return { kind: KIND };
    }
}

export function hasKeyValue(obj: any, keyToFind: string, valueToFind: string): boolean {
    if (obj instanceof Array) {
        for (const i in obj) {
            if (hasKeyValue(obj[i], keyToFind, valueToFind)) {
                return true;
            }
        }
    } else {
        for (const prop in obj) {
            if (obj[prop]) {
                if (prop === keyToFind && obj[prop] === valueToFind) {
                    return true;
                }
                if (obj[prop] instanceof Object || obj[prop] instanceof Array) {
                    if (hasKeyValue(obj[prop], keyToFind, valueToFind)) {
                        return true;
                    }
                }
            }
        }
    }
    return false;
}

export interface MyCustomAction extends RequestAction<MyCustomResponseAction> {
    kind: typeof MyCustomAction.KIND;
    additionalInformation: string;
}

export namespace MyCustomAction {
    export const KIND = 'myCustomKind';
    export function is(object: any): object is MyCustomAction {
        return (RequestAction.hasKind(object, KIND) &&
            hasStringProp(object, 'additionalInformation') &&
            hasStringProp(object, 'elementId'));
    }
    export function create(options: { elementId: string, additionalInformation: string, requestId?: string }): MyCustomAction {
        return {
            kind: KIND,
            requestId: '',
            ...options
        };
    }
}

export interface MyCustomResponseAction extends ResponseAction {
    kind: typeof MyCustomResponseAction.KIND;
    responceCode: string;
    additionalInformation: string;
}

export namespace MyCustomResponseAction {
    export const KIND = 'myCustomResponse';

    export function is(object: any): object is MyCustomResponseAction {
        return Action.hasKind(object, KIND) &&
            hasStringProp(object, 'responceCode') &&
            hasStringProp(object, 'additionalInformation');
    }

    export function create(options: { responceCode: string, additionalInformation: string, responseId?: string }): MyCustomResponseAction {
        return {
            kind: KIND,
            responseId: '',
            ...options
        };
    }
}
