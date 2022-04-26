/**
 */
package org.openbpmn.di.impl;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EClass;

import org.eclipse.emf.ecore.util.EObjectResolvingEList;

import org.openbpmn.di.DiPackage;
import org.openbpmn.di.Label;
import org.openbpmn.di.LabeledShape;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Labeled Shape</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link org.openbpmn.di.impl.LabeledShapeImpl#getOwnedLabel <em>Owned Label</em>}</li>
 * </ul>
 *
 * @generated
 */
public abstract class LabeledShapeImpl extends ShapeImpl implements LabeledShape {
    /**
     * The cached value of the '{@link #getOwnedLabel() <em>Owned Label</em>}' reference list.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see #getOwnedLabel()
     * @generated
     * @ordered
     */
    protected EList<Label> ownedLabel;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    protected LabeledShapeImpl() {
        super();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    @Override
    protected EClass eStaticClass() {
        return DiPackage.Literals.LABELED_SHAPE;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public EList<Label> getOwnedLabel() {
        if (ownedLabel == null) {
            ownedLabel = new EObjectResolvingEList<Label>(Label.class, this, DiPackage.LABELED_SHAPE__OWNED_LABEL);
        }
        return ownedLabel;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    @Override
    public Object eGet(int featureID, boolean resolve, boolean coreType) {
        switch (featureID) {
            case DiPackage.LABELED_SHAPE__OWNED_LABEL:
                return getOwnedLabel();
        }
        return super.eGet(featureID, resolve, coreType);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    @Override
    public boolean eIsSet(int featureID) {
        switch (featureID) {
            case DiPackage.LABELED_SHAPE__OWNED_LABEL:
                return ownedLabel != null && !ownedLabel.isEmpty();
        }
        return super.eIsSet(featureID);
    }

} //LabeledShapeImpl