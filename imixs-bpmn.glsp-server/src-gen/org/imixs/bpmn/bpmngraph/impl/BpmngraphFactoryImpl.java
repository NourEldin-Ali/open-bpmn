/**
 */
package org.imixs.bpmn.bpmngraph.impl;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;

import org.eclipse.emf.ecore.impl.EFactoryImpl;

import org.eclipse.emf.ecore.plugin.EcorePlugin;

import org.imixs.bpmn.bpmngraph.*;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model <b>Factory</b>.
 * <!-- end-user-doc -->
 * @generated
 */
public class BpmngraphFactoryImpl extends EFactoryImpl implements BpmngraphFactory {
   /**
    * Creates the default factory implementation.
    * <!-- begin-user-doc -->
    * <!-- end-user-doc -->
    * @generated
    */
   public static BpmngraphFactory init() {
      try {
         BpmngraphFactory theBpmngraphFactory = (BpmngraphFactory)EPackage.Registry.INSTANCE.getEFactory(BpmngraphPackage.eNS_URI);
         if (theBpmngraphFactory != null) {
            return theBpmngraphFactory;
         }
      }
      catch (Exception exception) {
         EcorePlugin.INSTANCE.log(exception);
      }
      return new BpmngraphFactoryImpl();
   }

   /**
    * Creates an instance of the factory.
    * <!-- begin-user-doc -->
    * <!-- end-user-doc -->
    * @generated
    */
   public BpmngraphFactoryImpl() {
      super();
   }

   /**
    * <!-- begin-user-doc -->
    * <!-- end-user-doc -->
    * @generated
    */
   @Override
   public EObject create(EClass eClass) {
      switch (eClass.getClassifierID()) {
         case BpmngraphPackage.ACTIVITY_NODE: return createActivityNode();
         case BpmngraphPackage.ICON: return createIcon();
         case BpmngraphPackage.WEIGHTED_EDGE: return createWeightedEdge();
         default:
            throw new IllegalArgumentException("The class '" + eClass.getName() + "' is not a valid classifier");
      }
   }

   /**
    * <!-- begin-user-doc -->
    * <!-- end-user-doc -->
    * @generated
    */
   public ActivityNode createActivityNode() {
      ActivityNodeImpl activityNode = new ActivityNodeImpl();
      return activityNode;
   }

   /**
    * <!-- begin-user-doc -->
    * <!-- end-user-doc -->
    * @generated
    */
   public Icon createIcon() {
      IconImpl icon = new IconImpl();
      return icon;
   }

   /**
    * <!-- begin-user-doc -->
    * <!-- end-user-doc -->
    * @generated
    */
   public WeightedEdge createWeightedEdge() {
      WeightedEdgeImpl weightedEdge = new WeightedEdgeImpl();
      return weightedEdge;
   }

   /**
    * <!-- begin-user-doc -->
    * <!-- end-user-doc -->
    * @generated
    */
   public BpmngraphPackage getBpmngraphPackage() {
      return (BpmngraphPackage)getEPackage();
   }

   /**
    * <!-- begin-user-doc -->
    * <!-- end-user-doc -->
    * @deprecated
    * @generated
    */
   @Deprecated
   public static BpmngraphPackage getPackage() {
      return BpmngraphPackage.eINSTANCE;
   }

} //BpmngraphFactoryImpl