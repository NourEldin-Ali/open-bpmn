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
import org.eclipse.glsp.graph.GModelElement;
import org.eclipse.glsp.graph.GNode;
import org.eclipse.glsp.graph.builder.impl.GLayoutOptions;
import org.eclipse.glsp.graph.util.GraphUtil;
import org.eclipse.glsp.server.features.directediting.ApplyLabelEditOperation;
import org.eclipse.glsp.server.operations.AbstractOperationHandler;
import org.eclipse.glsp.server.operations.Operation;
import org.openbpmn.bpmn.elements.TextAnnotation;
import org.openbpmn.bpmn.elements.core.BPMNElementNode;
import org.openbpmn.bpmn.exceptions.BPMNMissingElementException;
import org.openbpmn.glsp.bpmn.LabelGNode;
import org.openbpmn.glsp.bpmn.TaskGNode;
import org.openbpmn.glsp.bpmn.TextAnnotationGNode;
import org.openbpmn.glsp.model.BPMNGModelState;
import org.openbpmn.glsp.utils.BPMNGraphUtil;

import com.google.inject.Inject;

/**
 * This BPMNApplyEditLabelOperationHandler is used to apply label changes on
 * Tasks, TextAnnotations and BPMNLabels. The method updates also the GNode and
 * reset the mode state.
 * <p>
 * For TextAnnotation the the handler updates the BPMN 'text' property, for all
 * other BPMNNodeElements it updates the 'name' property.
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

    @Override
    public void executeOperation(final ApplyLabelEditOperation operation) {
        GNode gNodeElement = null;

        // resolve the GNode element
        Optional<GNode> _gNodeElement = modelState.getIndex().findElementByClass(operation.getLabelId(), GNode.class);
        if (_gNodeElement != null && _gNodeElement.isPresent()) {
            gNodeElement = _gNodeElement.get();
        }

        // resolve the corresponding BPMN Element
        BPMNElementNode bpmnElement = resolveBPMNElement(operation);
        if (bpmnElement != null) {

            // For TextAnnotation we need to updat the 'text' argument on the gNode
            if (bpmnElement instanceof TextAnnotation) {
                ((TextAnnotation) bpmnElement).setText(operation.getText());
                // update gNode
                if (gNodeElement != null) {
                    // update gNode
                    gNodeElement.getArgs().put("text", operation.getText());
                }

            } else {
                // For all other elements the text is stored in the 'name' attribute.
                bpmnElement.setName(operation.getText());
                // update gNode
                if (gNodeElement != null) {
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
                            if (bpmnElement.getLabel() != null) {
                                bpmnElement.getLabel().getBounds().setDimension(newLabelSize.getWidth(),
                                        newLabelSize.getHeight());
                            }
                        } catch (BPMNMissingElementException e) {
                            e.printStackTrace();
                        }

                    }
                }
            }
            // we do NOT reset the model
        } else {
            logger.warn("Unable to resolve the corresponding BPMN element Node for " + operation.getLabelId());
        }
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
    private BPMNElementNode resolveBPMNElement(final ApplyLabelEditOperation operation) {
        BPMNElementNode result = null;
        String labelId = operation.getLabelId();
        String elementID = null;

        Optional<GNode> _gNodeElement = modelState.getIndex().findElementByClass(labelId, GNode.class);

        if (_gNodeElement != null && _gNodeElement.isPresent()) {
            GNode gNodeElement = _gNodeElement.get();
            String type = gNodeElement.getType();
            GModelElement parent = gNodeElement.getParent();
            if ("bpmn-text-node".equals(type) && (parent instanceof LabelGNode || parent instanceof TaskGNode
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
            result = (BPMNElementNode) modelState.getBpmnModel().findElementById(elementID);
        }

        return result;

    }
}
