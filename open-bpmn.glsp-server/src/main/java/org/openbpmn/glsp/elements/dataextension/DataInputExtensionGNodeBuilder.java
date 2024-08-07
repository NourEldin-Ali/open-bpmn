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
package org.openbpmn.glsp.elements.dataextension;

import java.util.logging.Logger;

import org.eclipse.glsp.graph.builder.AbstractGNodeBuilder;
import org.eclipse.glsp.graph.builder.impl.GArguments;
import org.eclipse.glsp.graph.builder.impl.GLayoutOptions;
import org.eclipse.glsp.graph.util.GConstants;
import org.eclipse.glsp.graph.util.GraphUtil;
import org.openbpmn.bpmn.elements.DataInputObjectExtension;
import org.openbpmn.bpmn.elements.core.BPMNBounds;
import org.openbpmn.bpmn.exceptions.BPMNMissingElementException;
import org.openbpmn.glsp.bpmn.BpmnFactory;
import org.openbpmn.glsp.bpmn.DataObjectExtensionGNode;
import org.openbpmn.glsp.bpmn.IconGCompartment;
import org.openbpmn.glsp.elements.IconGCompartmentBuilder;
import org.openbpmn.glsp.model.BPMNGModelFactory;
import org.openbpmn.glsp.utils.BPMNGraphUtil;

/**
 * BPMN 2.0 Task Element.
 * <p>
 * The method builds a GNode from a BPMNTask element. The builder is called from
 * the method createGModelFromProcess of the BPMNGModelFactory.
 *
 * @author Ali Nour Eldin
 *
 */
public class DataInputExtensionGNodeBuilder
        extends AbstractGNodeBuilder<DataObjectExtensionGNode, DataInputExtensionGNodeBuilder> {

    private static Logger logger = Logger.getLogger(BPMNGModelFactory.class.getName());
    private static final String V_GRAB = "vGrab";
    private static final String H_GRAB = "hGrab";
    private final String name;
    private final String type;

    public DataInputExtensionGNodeBuilder(final DataInputObjectExtension data) {
        super(data.getType());
        this.name = data.getName();
        this.id = data.getId();
        this.type = data.getAttribute("type");

        try {
            BPMNBounds bpmnBounds = data.getBounds();
            this.size = GraphUtil.dimension(bpmnBounds.getDimension().getWidth(),
                    bpmnBounds.getDimension().getHeight());
        } catch (BPMNMissingElementException e) {
            // should not happen
            logger.severe("BPMN-DataInputExtension does not support a BPMNBounds object!");
        }
        // set Layout options
        this.addCssClass(type);
        this.addCssClass("dataObjectExtension");
        if (data.getAttribute("isMultiple").contentEquals("true")) {
            this.addCssClass("isMuliple");
        }
        this.addArguments(GArguments.cornerRadius(0));

        if (data.getElementNode().getParentNode().getAttributes().getNamedItem("expand").getNodeValue()
                .contentEquals("false")) {
            this.addCssClass("hideElement");
        }
    }

    @Override
    protected DataObjectExtensionGNode instantiate() {
        return BpmnFactory.eINSTANCE.createDataObjectExtensionGNode();
    }

    @Override
    protected DataInputExtensionGNodeBuilder self() {
        return this;
    }

    @Override
    public void setProperties(final DataObjectExtensionGNode node) {
        super.setProperties(node);
        node.setName(name);

        node.setLayout(GConstants.Layout.VBOX);
//        node.getLayoutOptions().put(GLayoutOptions.KEY_H_ALIGN, GConstants.HAlign.CENTER);
//        node.getLayoutOptions().put(GLayoutOptions.KEY_V_ALIGN, GConstants.VAlign.CENTER);
        // Set min width/height
        node.getLayoutOptions().put(GLayoutOptions.KEY_MIN_WIDTH, DataInputObjectExtension.DEFAULT_WIDTH);
        node.getLayoutOptions().put(GLayoutOptions.KEY_MIN_HEIGHT, DataInputObjectExtension.DEFAULT_HEIGHT);

        node.getLayoutOptions().put(H_GRAB, true);
        node.getLayoutOptions().put(V_GRAB, true);

//        node.getLayoutOptions().put(GLayoutOptions.KEY_PREF_WIDTH, size.getWidth());
//        node.getLayoutOptions().put(GLayoutOptions.KEY_PREF_HEIGHT, size.getHeight());

        node.getLayoutOptions().put(GLayoutOptions.KEY_V_GAP, 1);

        IconGCompartment taskIcon = new IconGCompartmentBuilder(). //
                id(node.getId() + "_icon"). //
                layoutOptions(new GLayoutOptions().hAlign(GConstants.HAlign.LEFT)). //
                build();

        node.getChildren().add(taskIcon);

        IconGCompartment taskIcon2 = new IconGCompartmentBuilder(). //
                id(node.getId() + "_icon2"). //
                layoutOptions(new GLayoutOptions().hAlign(GConstants.HAlign.CENTER)). //
                build();
        node.getChildren().add(taskIcon2);
        // node.getChildren().add(BPMNGraphUtil.createCompartmentHeader(node));

//        node.getChildren().add(BPMNGraphUtil.createMultiLineTextNode(id + "_name", name + ":" + type));
        node.getChildren().add(BPMNGraphUtil.createMultiLineTextNode(id + "_name", name));

    }

}
