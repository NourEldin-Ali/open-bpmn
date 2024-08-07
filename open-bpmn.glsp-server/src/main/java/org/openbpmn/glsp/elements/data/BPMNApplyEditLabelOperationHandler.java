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
package org.openbpmn.glsp.elements.data;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.glsp.graph.GDimension;
import org.eclipse.glsp.graph.GEdge;
import org.eclipse.glsp.graph.GModelElement;
import org.eclipse.glsp.graph.GNode;
import org.eclipse.glsp.graph.builder.impl.GLayoutOptions;
import org.eclipse.glsp.graph.util.GraphUtil;
import org.eclipse.glsp.server.features.directediting.ApplyLabelEditOperation;
import org.eclipse.glsp.server.operations.AbstractOperationHandler;
import org.eclipse.glsp.server.operations.Operation;
import org.openbpmn.bpmn.BPMNTypes;
import org.openbpmn.bpmn.elements.Activity;
import org.openbpmn.bpmn.elements.TextAnnotation;
import org.openbpmn.bpmn.elements.core.BPMNElement;
import org.openbpmn.bpmn.elements.core.BPMNElementNode;
import org.openbpmn.bpmn.elements.core.BPMNLabel;
import org.openbpmn.bpmn.exceptions.BPMNMissingElementException;
import org.openbpmn.glsp.bpmn.LabelGNode;
import org.openbpmn.glsp.bpmn.TaskGNode;
import org.openbpmn.glsp.bpmn.TextAnnotationGNode;
import org.openbpmn.glsp.model.BPMNGModelState;
import org.openbpmn.glsp.utils.BPMNGraphUtil;
import org.openbpmn.glsp.utils.ModelTypes;

import com.google.inject.Inject;

/**
 * This BPMNApplyEditLabelOperationHandler is used to apply label changes on
 * BPMNElementNodes or BPMNEdges. The method updates also the GNode/GEdge and
 * reset the mode state.
 * <p>
 * For TextAnnotation the the handler updates the BPMN 'text' property, for all
 * other BPMNElements it updates the 'name' property.
 * <p>
 * (See also:
 * https://www.eclipse.org/glsp/documentation/rendering/#default-views)
 * <p>
 * Using the {@link AbstractOperationHandler} avoids the need for casting of the
 * operation object. It implementation handles exactly one {@link Operation}
 * type.
 *
 * @see: https://github.com/eclipse-glsp/glsp/blob/master/PROTOCOL.md
 *
 * @author rsoika
 *
 */
public class BPMNApplyEditLabelOperationHandler extends AbstractOperationHandler<ApplyLabelEditOperation> {

    private static Logger logger = LogManager.getLogger(BPMNApplyEditLabelOperationHandler.class);

    @Inject
    protected BPMNGModelState modelState;

    /**
     * This method updates the corresponding BPMN Element form a given Label. This
     * object can either be a Node or an Edge
     */
    @Override
    public void executeOperation(final ApplyLabelEditOperation operation) {

        // Do we have an GNode?
        Optional<GNode> _gNodeElement = modelState.getIndex().findElementByClass(operation.getLabelId(), GNode.class);
        GNode gNodeElement = _gNodeElement.orElse(null);
        if (gNodeElement != null) {
            BPMNElementNode bpmnElementNode = resolveBPMNElementNode(gNodeElement);
            if (bpmnElementNode != null) {
                // For TextAnnotation we need to update the 'text' argument on the gNode
                if (bpmnElementNode instanceof TextAnnotation) {
                    ((TextAnnotation) bpmnElementNode).setText(operation.getText());
                    // update gNode
                    gNodeElement.getArgs().put("text", operation.getText());

                } else {
                    // For all other elements the text is stored in the 'name' attribute.
                    bpmnElementNode.setName(operation.getText());
                    // update gNode

                    // update gNode
                    String text = operation.getText();
                    gNodeElement.getArgs().put("text", text);

                    GModelElement parent = gNodeElement.getParent();
                    if (parent != null && parent instanceof LabelGNode) {
                        int FONT_SIZE = 14;
                        LabelGNode label = (LabelGNode) parent;
                        // resize based on the lines....
                        int estimatedLines = estimateLineCount(operation.getText(), FONT_SIZE, 100);
                        GDimension newLabelSize = GraphUtil.dimension(100, FONT_SIZE * estimatedLines);
                        label.getLayoutOptions().put(GLayoutOptions.KEY_PREF_WIDTH, newLabelSize.getWidth());
                        label.getLayoutOptions().put(GLayoutOptions.KEY_PREF_HEIGHT, newLabelSize.getHeight());
                        // calling the size method does not have an effect.
                        // see:
                        // https://github.com/eclipse-glsp/glsp/discussions/741#discussioncomment-3688606
                        label.setSize(newLabelSize);
                        // Update the BPMNLabel bounds...
                        try {
                            BPMNLabel bpmnLabel = bpmnElementNode.getLabel();
                            if (bpmnLabel != null) {
                                bpmnLabel.getBounds().setDimension(newLabelSize.getWidth(), newLabelSize.getHeight());
                            }
                        } catch (BPMNMissingElementException e) {
                            e.printStackTrace();
                        }

                    }
                }
            } else {
                System.out.println(gNodeElement.getId());
                Activity act = null;
                String elementId = "";
                if (gNodeElement.getId().contains(BPMNTypes.DATA_PROCESSING_EXTENSION)) {
                    elementId = gNodeElement.getId().split("_bpmnlabel_bpmntext")[0];

                } else {
                    elementId = gNodeElement.getId().split("_name")[0];

                }
                act = (Activity) modelState.getBpmnModel().findElementExtensionNodeById(elementId);
                bpmnElementNode = (BPMNElementNode) act.findElementById(elementId);
                String text = operation.getText();
                if (text.contains(":")) {
                    bpmnElementNode.setAttribute("type", text.split(":")[1]);
                    text = text.split(":")[0];

                }
                bpmnElementNode.setName(text);
                // update gNode

                gNodeElement.getArgs().put("text", text);

                GModelElement parent = gNodeElement.getParent();
                if (parent != null && parent instanceof LabelGNode) {
                    int FONT_SIZE = 14;
                    LabelGNode label = (LabelGNode) parent;
                    // resize based on the lines....
                    int estimatedLines = estimateLineCount(operation.getText(), FONT_SIZE, 100);
                    GDimension newLabelSize = GraphUtil.dimension(100, FONT_SIZE * estimatedLines);
                    label.getLayoutOptions().put(GLayoutOptions.KEY_PREF_WIDTH, newLabelSize.getWidth());
                    label.getLayoutOptions().put(GLayoutOptions.KEY_PREF_HEIGHT, newLabelSize.getHeight());
                    // calling the size method does not have an effect.
                    // see:
                    // https://github.com/eclipse-glsp/glsp/discussions/741#discussioncomment-3688606
                    label.setSize(newLabelSize);
                    // Update the BPMNLabel bounds...
                    try {
                        BPMNLabel bpmnLabel = bpmnElementNode.getLabel();
                        if (bpmnLabel != null) {
                            bpmnLabel.getBounds().setDimension(newLabelSize.getWidth(), newLabelSize.getHeight());
                        }
                    } catch (BPMNMissingElementException e) {
                        e.printStackTrace();
                    }

                }
            }
        } else {
            // it was not a GNode - so test if we have a Edge...
            Optional<GEdge> _gEdgeElement = modelState.getIndex().findElementByClass(operation.getLabelId(),
                    GEdge.class);
            GEdge gEdgeElement = _gEdgeElement.orElse(null);
            if (gEdgeElement != null) {
                gEdgeElement.getArgs().put("text", operation.getText());
                BPMNElement bpmnElement = modelState.getBpmnModel().findElementById(gEdgeElement.getId());
                bpmnElement.setName(operation.getText());
            } else {
                logger.warn("Unable to resolve the corresponding BPMN element Node for " + operation.getLabelId());
            }
        }

        // we do need to reset the model because of the properties panels
        modelState.reset();

    }

    /**
     * Helper method to estimate the number of lines a text will need in a panel
     * with a given width
     *
     * @param text
     * @param fontSize
     * @param width
     * @return estimated number of lines
     */
    private int estimateLineCount(final String text, final int fontSize, final int width) {
        int result = 0;
        // first split the text by hard line breaks...
        String[] paragraphs = text.split("\n");

        // Estimate the number of characters per line based on the font size and the
        // given width
        double charactersPerLine = width / (fontSize * 0.5);

        // Estimate the number of lines per paragraph based on the number of characters
        for (String paragraph : paragraphs) {
            int lineCount = (int) Math.ceil(paragraph.length() / charactersPerLine);
            result += lineCount;
        }
        return result;
    }

    /**
     * This helper method resolves the corresponding BPMN Element form a
     * ApplyLabelEditOperation by resolving the LabelId to the correct element.
     *
     * @param operation
     * @return
     */
    private BPMNElementNode resolveBPMNElementNode(final GNode gNodeElement) {
        BPMNElementNode result = null;
        String elementID = null;
        if (gNodeElement != null) {
            String type = gNodeElement.getType();
            GModelElement parent = gNodeElement.getParent();
            if (ModelTypes.BPMN_TEXT_NODE.equals(type) && (parent instanceof LabelGNode || parent instanceof TaskGNode
                    || parent instanceof TextAnnotationGNode)) {
                // it is a TextAnnotation...
                elementID = parent.getId();
            } else {
                elementID = gNodeElement.getId();
            }
            // find element
            if (BPMNGraphUtil.isBPMNLabelID(elementID)) {
                elementID = BPMNGraphUtil.resolveFlowElementIDfromLabelID(elementID);
            }

        }
        // find the BPMNElement....
        result = (BPMNElementNode) modelState.getBpmnModel().findElementById(elementID);
        return result;

    }
}
