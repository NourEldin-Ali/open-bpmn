package it.unibz.deltabpmn.bpmn.parsers;

import it.unibz.deltabpmn.datalogic.BulkCondition;
import it.unibz.deltabpmn.dataschema.core.DataSchema;
import it.unibz.deltabpmn.dataschema.elements.Attribute;
import it.unibz.deltabpmn.dataschema.elements.Constant;
import it.unibz.deltabpmn.dataschema.elements.Sort;
import it.unibz.deltabpmn.exception.InvalidInputException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BulkConditionParser {

    //ToDO: add NULL management!
    public static BulkCondition parseAndUpdate(BulkCondition bulkCondition, String condition, DataSchema dataSchema) throws InvalidInputException {
        //BulkCondition result = new BulkCondition(dataSchema.getRepositoryRelationAssociations().get(relationName), dataSchema);
        String[] values = condition.split("AND");
        for (String expression : values) {
            if (expression.contains("!=")) {
                String[] operands = expression.split("!=");
                Attribute attr = getAttribute(operands[0].trim(), dataSchema);
                bulkCondition.addNotEqualsCondition(attr, getConstant(operands[1].trim(), attr.getSort(), dataSchema));
            } else if (expression.contains("=")) {
                String[] operands = expression.split("=");
                Attribute attr = getAttribute(operands[0].trim(), dataSchema);
                //if it's an attribute, then don't do anything, just fetch it from list of attributes
                //if it's a case variable, we assume that it has been already generated by looking into the vars declaration
                bulkCondition.addEqualsCondition(attr, getConstant(operands[1], attr.getSort(), dataSchema));
            } else if (expression.contains(">")) {
                String[] operands = expression.split(">");
                Attribute attr = getAttribute(operands[0].trim(), dataSchema);
                bulkCondition.addGreaterThanCondition(attr, getConstant(operands[1].trim(), attr.getSort(), dataSchema));
            } else if (expression.contains("<")) {
                String[] operands = expression.split("<");
                Attribute attr = getAttribute(operands[0].trim(), dataSchema);
                bulkCondition.addLessThanCondition(attr, getConstant(operands[1].trim(), attr.getSort(), dataSchema));
            } else if (expression.contains("IN")) {
                String[] operands = expression.split("IN");
                //List<Attribute> attributes = new ArrayList<>();
                String[] attributeNames = operands[0].substring(operands[0].indexOf("(") + 1, operands[0].indexOf(")")).trim().split(",");
                List<Attribute> attributes = new ArrayList<>();
                Arrays.stream(attributeNames).forEach(c -> attributes.add(dataSchema.getAllAttributes().get(c)));
                bulkCondition.addInRelationCondition(dataSchema.getCatalogRelationAssociations().get(operands[1].trim()), attributes.toArray(new Attribute[attributes.size()]));
            } else if (expression.contains("NOT IN")) {
                String[] operands = expression.split("NOT IN");
                //currently not supported
                //ToDo: add support :)
            }
        }
        return bulkCondition;
    }


    private static Attribute getAttribute(String el, DataSchema dataSchema) {
        String[] signature = el.split("\\.");
        Attribute attr = dataSchema.getAllAttributes().get(signature[1]);
        return attr;
    }

    private static Constant getConstant(String el, Sort attrSort, DataSchema dataSchema) {
        Constant val = (Constant) TermProcessor.processTerm(el.trim(), dataSchema);
        if (val == null)
            val = dataSchema.newConstant(el, attrSort);
        return val;
    }
}