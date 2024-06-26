package org.openbpmn.bpmn.elements;

import java.io.DataOutput;
import java.util.Set;

import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.elements.core.BPMNElementNode;
import org.openbpmn.bpmn.exceptions.BPMNModelException;
import org.w3c.dom.Element;

/**
 * An Data object is work within an Activity.  * 
 * @author Ali Nour Eldin
 *
 */
public class DataObjectAttributeExtension extends BPMNElementNode {

    public final static double DEFAULT_WIDTH = 110.0;
    public final static double DEFAULT_HEIGHT = 25.0;
    @SuppressWarnings("unused")
	private DataInputObjectExtension dataInputObject=null;
    
    @SuppressWarnings("unused")
	private DataOutputObjectExtension dataOutputObject=null;
    
    protected DataObjectAttributeExtension(BPMNModel model, Element node, String type, BPMNProcess bpmnProcess,DataInputObjectExtension dataObject) throws BPMNModelException {
        super(model, node, type,bpmnProcess);
        this.dataInputObject = dataObject;
    }

    protected DataObjectAttributeExtension(BPMNModel model, Element node, String type, BPMNProcess bpmnProcess,DataOutputObjectExtension dataObject) throws BPMNModelException {
        super(model, node, type,bpmnProcess);
        this.dataOutputObject = dataObject;
    }
    
    public Set<DataObjectAttributeExtension> getAttributesObject() {
    if ( dataInputObject!=null) {
    	return dataInputObject.getDataAttributes();
    }
    if ( dataOutputObject!=null) {
    	return dataOutputObject.getDataAttributes();
    }
    return null;
    }
    
    public BPMNElementNode getDataParent() {
    	if ( dataInputObject!=null) {
        	return dataInputObject;
        }
        if ( dataOutputObject!=null) {
        	return dataOutputObject;
        }
        return null;
        }
    
    @Override
    public double getDefaultWidth() {
        return DEFAULT_WIDTH;
    }


    @Override
    public double getDefaultHeight() {
        return DEFAULT_HEIGHT;
    }
    
    public Activity getActivity() {
    	if(dataInputObject != null) {
    		return dataInputObject.activity;
    	}else if(dataOutputObject != null) {
    		return dataOutputObject.activity;
    	}
    	return null;
    }
}
