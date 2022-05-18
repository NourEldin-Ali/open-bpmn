/********************************************************************************
 * Copyright (c) 2019-2021 EclipseSource and others.
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
    configureDefaultModelElements,
    configureModelElement,
    ConsoleLogger,
    createClientContainer,
    DeleteElementContextMenuItemProvider,
    editLabelFeature,
    GridSnapper,
    LogLevel,
    overrideViewerOptions,
    RevealNamedElementActionProvider,
    RoundedCornerNodeView,
    RectangularNodeView,
    SCompartment,
    SCompartmentView,
    SLabel,
    SLabelView,configureView,
    StructureCompartmentView,configureCommand,
    TYPES
} from '@eclipse-glsp/client';
// import { DefaultTypes } from '@eclipse-glsp/protocol';
import 'balloon-css/balloon.min.css';
import { Container, ContainerModule } from 'inversify';
import 'sprotty/css/edit-label.css';
import '../css/diagram.css';
import {
	GatewayNode,
	PoolNode,
	Icon,
	TaskNode,
	EventNode,
	SequenceFlow,
	BPMNElementAnchor,
	BPMNPolylineElementAnchor,
	BPMNPort
} from '@open-bpmn/open-bpmn-model';
import { IconView, GatewayNodeView, EventNodeView } from './bpmn-element-views';
import { BPMNSequenceFlowView } from './bpmn-routing-views';
import { HelperLineListener,DrawHelperLinesCommand,RemoveHelperLinesCommand,HelperLineView } from './bpmn-helperlines';

import {bpmnPropertyModule} from '@open-bpmn/open-bpmn-properties';

const bpmnDiagramModule = new ContainerModule((bind, unbind, isBound, rebind) => {
    const context = { bind, unbind, isBound, rebind };

    rebind(TYPES.ILogger).to(ConsoleLogger).inSingletonScope();
    rebind(TYPES.LogLevel).toConstantValue(LogLevel.warn);
    bind(TYPES.ISnapper).to(GridSnapper);
    bind(TYPES.ICommandPaletteActionProvider).to(RevealNamedElementActionProvider);
    bind(TYPES.IContextMenuItemProvider).to(DeleteElementContextMenuItemProvider);

	// bpmn helper lines
    bind(TYPES.MouseListener).to(HelperLineListener);
    configureCommand({ bind, isBound }, DrawHelperLinesCommand);
    configureCommand({ bind, isBound }, RemoveHelperLinesCommand);
    configureView({ bind, isBound }, 'helpline', HelperLineView);

    // bind the BPMN AnchorComputer
    bind(TYPES.IAnchorComputer).to(BPMNElementAnchor).inSingletonScope();
    bind(TYPES.IAnchorComputer).to(BPMNPolylineElementAnchor).inSingletonScope();

    // bind(TYPES.IAnchorComputer).to(BPMNSequenceFlowAnchor).inSingletonScope();
    // bind(TYPES.IAnchorComputer).to(BPMNEventElementAnchor).inSingletonScope();

    configureDefaultModelElements(context);
    configureModelElement(context, 'task', TaskNode, RoundedCornerNodeView);
    configureModelElement(context, 'manualTask', TaskNode, RoundedCornerNodeView);
    configureModelElement(context, 'userTask', TaskNode, RoundedCornerNodeView);
    configureModelElement(context, 'sendTask', TaskNode, RoundedCornerNodeView);
    configureModelElement(context, 'serviceTask', TaskNode, RoundedCornerNodeView);
    configureModelElement(context, 'scriptTask', TaskNode, RoundedCornerNodeView);

    configureModelElement(context, 'startEvent', EventNode, EventNodeView);
    configureModelElement(context, 'endEvent', EventNode, EventNodeView);
    configureModelElement(context, 'catchEvent', EventNode, EventNodeView);
    configureModelElement(context, 'throwEvent', EventNode, EventNodeView);
	configureModelElement(context, 'event:port', BPMNPort, RectangularNodeView);

    configureModelElement(context, 'exclusiveGateway', GatewayNode, GatewayNodeView);
    configureModelElement(context, 'inclusiveGateway', GatewayNode, GatewayNodeView);
    configureModelElement(context, 'parallelGateway', GatewayNode, GatewayNodeView);
    configureModelElement(context, 'complexGateway', GatewayNode, GatewayNodeView);

    configureModelElement(context, 'label:heading', SLabel, SLabelView, { enable: [editLabelFeature] });
    configureModelElement(context, 'comp:comp', SCompartment, SCompartmentView);
    configureModelElement(context, 'comp:header', SCompartment, SCompartmentView);
    configureModelElement(context, 'icon', Icon, IconView);

    configureModelElement(context, 'pool', PoolNode, RoundedCornerNodeView);
    configureModelElement(context, 'struct', SCompartment, StructureCompartmentView);

    // configureModelElement(context, 'edge:sequenceflow', SequenceFlow, BPMNSequenceFlowView);
    configureModelElement(context, 'sequenceFlow', SequenceFlow, BPMNSequenceFlowView);
    
    

});

export default function createContainer(widgetId: string): Container {
    // Create the createClientContainer with the diagramModule and the BPMN bpmnPropertyModule...
    const container = createClientContainer(bpmnDiagramModule, bpmnPropertyModule);
    overrideViewerOptions(container, {
        baseDiv: widgetId,
        hiddenDiv: widgetId + '_hidden'
    });
    return container;
}
