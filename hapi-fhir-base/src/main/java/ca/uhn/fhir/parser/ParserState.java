package ca.uhn.fhir.parser;

import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import ca.uhn.fhir.context.BaseRuntimeChildDefinition;
import ca.uhn.fhir.context.BaseRuntimeElementCompositeDefinition;
import ca.uhn.fhir.context.BaseRuntimeElementDefinition;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.RuntimeChildDeclaredExtensionDefinition;
import ca.uhn.fhir.context.RuntimePrimitiveDatatypeDefinition;
import ca.uhn.fhir.context.RuntimePrimitiveDatatypeNarrativeDefinition;
import ca.uhn.fhir.context.RuntimeResourceBlockDefinition;
import ca.uhn.fhir.context.RuntimeResourceDefinition;
import ca.uhn.fhir.context.RuntimeResourceReferenceDefinition;
import ca.uhn.fhir.model.api.Bundle;
import ca.uhn.fhir.model.api.ICompositeDatatype;
import ca.uhn.fhir.model.api.ICompositeElement;
import ca.uhn.fhir.model.api.IElement;
import ca.uhn.fhir.model.api.IPrimitiveDatatype;
import ca.uhn.fhir.model.api.IResourceBlock;
import ca.uhn.fhir.model.api.ISupportsUndeclaredExtensions;
import ca.uhn.fhir.model.api.ResourceReference;
import ca.uhn.fhir.model.api.UndeclaredExtension;
import ca.uhn.fhir.model.primitive.StringDt;
import ca.uhn.fhir.model.primitive.XhtmlDt;

class ParserState {

	private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(ParserState.class);

	private FhirContext myContext;

	private Object myObject;

	private BaseState myState;

	private ParserState(FhirContext theContext) {
		myContext = theContext;
	}

	public void attributeValue(Attribute theAttribute, String theValue) throws DataFormatException {
		myState.attributeValue(theAttribute, theValue);
	}

	public void endingElement(EndElement theElem) throws DataFormatException {
		myState.endingElement(theElem);
	}

	public void enteringNewElement(StartElement theElement, String theName) throws DataFormatException {
		myState.enteringNewElement(theElement, theName);
	}

	public void enteringNewElementExtension(StartElement theElem, String theUrlAttr) {
		myState.enteringNewElementExtension(theElem, theUrlAttr);
	}

	public Object getObject() {
		return myObject;
	}

	public boolean isComplete() {
		return myObject != null;
	}

	public void otherEvent(XMLEvent theEvent) throws DataFormatException {
		myState.otherEvent(theEvent);
	}

	private void pop() {
		myState = myState.myStack;
		myState.wereBack();
	}

	private void push(BaseState theState) {
		theState.setStack(myState);
		myState = theState;
	}

	public static ParserState getPreResourceInstance(FhirContext theContext) throws DataFormatException {
		ParserState retVal = new ParserState(theContext);
		retVal.push(retVal.new PreResourceState());
		return retVal;
	}

	private class AtomPrimitiveState extends BaseState{

		private IPrimitiveDatatype<?> myPrimitive;
		private String myData;

		public AtomPrimitiveState(IPrimitiveDatatype<?> thePrimitive) {
			myPrimitive = thePrimitive;
		}

		@Override
		public void attributeValue(Attribute theAttribute, String theValue) throws DataFormatException {
			// ignore
		}

		@Override
		public void endingElement(EndElement theElem) throws DataFormatException {
			myPrimitive.setValueAsString(myData);
			pop();
		}

		@Override
		public void enteringNewElement(StartElement theElement, String theLocalPart) throws DataFormatException {
			throw new DataFormatException("Unexpected nested element in atom tag ");
		}

		@Override
		protected IElement getCurrentElement() {
			return null;
		}

		@Override
		public void otherEvent(XMLEvent theEvent) throws DataFormatException {
			if (theEvent.isCharacters()) {
				String data = theEvent.asCharacters().getData();
				if (myData == null) {
					myData = data;
				}else {
					// this shouldn't generally happen so it's ok that it's inefficient
					myData = myData + data; 
				}
			}
		}
		
	}
	
	private class AtomState extends BaseState {

		private Bundle myInstance;

		public AtomState(Bundle theInstance) {
			myInstance = theInstance;
		}

		@Override
		public void attributeValue(Attribute theAttribute, String theValue) throws DataFormatException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void endingElement(EndElement theElem) throws DataFormatException {
			pop();
		}

		@Override
		public void enteringNewElement(StartElement theElement, String theLocalPart) throws DataFormatException {
			if (theLocalPart.equals("title")) {
				push(new AtomPrimitiveState(myInstance.getTitle()));
			}
		}

		@Override
		protected IElement getCurrentElement() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void otherEvent(XMLEvent theEvent) throws DataFormatException {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	private class PreAtomState extends BaseState {

		private Bundle myInstance;

		@Override
		public void attributeValue(Attribute theAttribute, String theValue) throws DataFormatException {
			// ignore
		}

		@Override
		public void endingElement(EndElement theElem) throws DataFormatException {
			// ignore
		}

		@Override
		public void enteringNewElement(StartElement theElement, String theLocalPart) throws DataFormatException {
			if (!"feed".equals(theLocalPart)) {
				throw new DataFormatException("Expecting outer element called 'feed', found: "+theLocalPart);
			}
			
			myInstance = new Bundle();
			push(new AtomState(myInstance));
			
		}

		@Override
		protected IElement getCurrentElement() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void wereBack() {
			myObject = myInstance;
		}

		@Override
		public void otherEvent(XMLEvent theEvent) throws DataFormatException {
			// ignore
		}
		
	}
	
	private abstract class BaseState {

		private BaseState myStack;

		public abstract void attributeValue(Attribute theAttribute, String theValue) throws DataFormatException;

		public abstract void endingElement(EndElement theElem) throws DataFormatException;

		public abstract void enteringNewElement(StartElement theElement, String theLocalPart) throws DataFormatException;

		/**
		 * Default implementation just handles undeclared extensions
		 */
		public void enteringNewElementExtension(@SuppressWarnings("unused") StartElement theElement, String theUrlAttr) {
			if (getCurrentElement() instanceof ISupportsUndeclaredExtensions) {
				UndeclaredExtension newExtension = new UndeclaredExtension();
				newExtension.setUrl(theUrlAttr);
				// TODO: fail if we don't support undeclared extensions
				((ISupportsUndeclaredExtensions) getCurrentElement()).getUndeclaredExtensions().add(newExtension);
				ExtensionState newState = new ExtensionState(newExtension);
				push(newState);
			}
		}

		protected abstract IElement getCurrentElement();

		public abstract void otherEvent(XMLEvent theEvent) throws DataFormatException;

		public void setStack(BaseState theState) {
			myStack = theState;
		}

		public void wereBack() {
			// allow an implementor to override
		}

	}

	private class DeclaredExtensionState extends BaseState {

		private IElement myChildInstance;
		private RuntimeChildDeclaredExtensionDefinition myDefinition;
		private IElement myParentInstance;

		public DeclaredExtensionState(RuntimeChildDeclaredExtensionDefinition theDefinition, IElement theParentInstance) {
			myDefinition = theDefinition;
			myParentInstance = theParentInstance;
		}

		@Override
		public void attributeValue(Attribute theAttribute, String theValue) throws DataFormatException {
			throw new DataFormatException("'value' attribute is invalid in 'extension' element");
		}

		@Override
		public void endingElement(EndElement theElem) throws DataFormatException {
			pop();
		}

		@Override
		public void enteringNewElement(StartElement theElement, String theLocalPart) throws DataFormatException {
			BaseRuntimeElementDefinition<?> target = myDefinition.getChildByName(theLocalPart);
			if (target == null) {
				throw new DataFormatException("Unknown extension element name: " + theLocalPart);
			}

			switch (target.getChildType()) {
			case COMPOSITE_DATATYPE: {
				BaseRuntimeElementCompositeDefinition<?> compositeTarget = (BaseRuntimeElementCompositeDefinition<?>) target;
				ICompositeDatatype newChildInstance = (ICompositeDatatype) compositeTarget.newInstance();
				myDefinition.getMutator().addValue(myParentInstance, newChildInstance);
				ElementCompositeState newState = new ElementCompositeState(compositeTarget, newChildInstance);
				push(newState);
				return;
			}
			case PRIMITIVE_DATATYPE: {
				RuntimePrimitiveDatatypeDefinition primitiveTarget = (RuntimePrimitiveDatatypeDefinition) target;
				IPrimitiveDatatype<?> newChildInstance = primitiveTarget.newInstance();
				myDefinition.getMutator().addValue(myParentInstance, newChildInstance);
				PrimitiveState newState = new PrimitiveState(newChildInstance);
				push(newState);
				return;
			}
			case RESOURCE_REF: {
				RuntimeResourceReferenceDefinition resourceRefTarget = (RuntimeResourceReferenceDefinition) target;
				ResourceReference newChildInstance = new ResourceReference();
				myDefinition.getMutator().addValue(myParentInstance, newChildInstance);
				ResourceReferenceState newState = new ResourceReferenceState(resourceRefTarget, newChildInstance);
				push(newState);
				return;
			}
			case PRIMITIVE_XHTML:
			case RESOURCE:
			case RESOURCE_BLOCK:
			case UNDECL_EXT:
			case EXTENSION_DECLARED:
			default:
				break;
			}
		}

		@Override
		public void enteringNewElementExtension(StartElement theElement, String theUrlAttr) {
			RuntimeChildDeclaredExtensionDefinition declaredExtension = myDefinition.getChildExtensionForUrl(theUrlAttr);
			if (declaredExtension != null) {
				if (myChildInstance == null) {
					myChildInstance = myDefinition.newInstance();
					myDefinition.getMutator().addValue(myParentInstance, myChildInstance);
				}
				BaseState newState = new DeclaredExtensionState(declaredExtension, myChildInstance);
				push(newState);
			} else {
				super.enteringNewElementExtension(theElement, theUrlAttr);
			}
		}

		@Override
		protected IElement getCurrentElement() {
			return myParentInstance;
		}

		@Override
		public void otherEvent(XMLEvent theEvent) throws DataFormatException {
			// ignore
		}

	}

	private class ElementCompositeState extends BaseState {

		private BaseRuntimeElementCompositeDefinition<?> myDefinition;
		private ICompositeElement myInstance;

		public ElementCompositeState(BaseRuntimeElementCompositeDefinition<?> theDef, ICompositeElement theInstance) {
			myDefinition = theDef;
			myInstance = theInstance;
		}

		@Override
		public void attributeValue(Attribute theAttribute, String theValue) {
			ourLog.debug("Ignoring attribute value: {}", theValue);
		}

		@Override
		public void endingElement(EndElement theElem) {
			pop();
			if (myState == null) {
				myObject = myInstance;
			}
		}

		@Override
		public void enteringNewElement(StartElement theElement, String theChildName) throws DataFormatException {
			BaseRuntimeChildDefinition child = myDefinition.getChildByNameOrThrowDataFormatException(theChildName);
			BaseRuntimeElementDefinition<?> target = child.getChildByName(theChildName);
			if (target == null) {
				throw new DataFormatException("Found unexpected element '" + theChildName + "' in parent element '" + myDefinition.getName() + "'. Valid names are: " + child.getValidChildNames());
			}

			switch (target.getChildType()) {
			case COMPOSITE_DATATYPE: {
				BaseRuntimeElementCompositeDefinition<?> compositeTarget = (BaseRuntimeElementCompositeDefinition<?>) target;
				ICompositeDatatype newChildInstance = (ICompositeDatatype) compositeTarget.newInstance();
				child.getMutator().addValue(myInstance, newChildInstance);
				ElementCompositeState newState = new ElementCompositeState(compositeTarget, newChildInstance);
				push(newState);
				return;
			}
			case PRIMITIVE_DATATYPE: {
				RuntimePrimitiveDatatypeDefinition primitiveTarget = (RuntimePrimitiveDatatypeDefinition) target;
				IPrimitiveDatatype<?> newChildInstance = primitiveTarget.newInstance();
				child.getMutator().addValue(myInstance, newChildInstance);
				PrimitiveState newState = new PrimitiveState(newChildInstance);
				push(newState);
				return;
			}
			case RESOURCE_REF: {
				RuntimeResourceReferenceDefinition resourceRefTarget = (RuntimeResourceReferenceDefinition) target;
				ResourceReference newChildInstance = new ResourceReference();
				child.getMutator().addValue(myInstance, newChildInstance);
				ResourceReferenceState newState = new ResourceReferenceState(resourceRefTarget, newChildInstance);
				push(newState);
				return;
			}
			case RESOURCE_BLOCK: {
				RuntimeResourceBlockDefinition blockTarget = (RuntimeResourceBlockDefinition) target;
				IResourceBlock newBlockInstance = blockTarget.newInstance();
				child.getMutator().addValue(myInstance, newBlockInstance);
				ElementCompositeState newState = new ElementCompositeState(blockTarget, newBlockInstance);
				push(newState);
				return;
			}
			case PRIMITIVE_XHTML: {
				RuntimePrimitiveDatatypeNarrativeDefinition xhtmlTarget = (RuntimePrimitiveDatatypeNarrativeDefinition) target;
				XhtmlDt newDt = xhtmlTarget.newInstance();
				child.getMutator().addValue(myInstance, newDt);
				XhtmlState state = new XhtmlState(newDt, theElement);
				push(state);
				return;
			}
			case UNDECL_EXT:
			case RESOURCE: {
				// Throw an exception because this shouldn't happen here
				break;
			}
			}

			throw new DataFormatException("Illegal resource position: " + target.getChildType());
		}

		@Override
		public void enteringNewElementExtension(StartElement theElement, String theUrlAttr) {
			RuntimeChildDeclaredExtensionDefinition declaredExtension = myDefinition.getDeclaredExtension(theUrlAttr);
			if (declaredExtension != null) {
				BaseState newState = new DeclaredExtensionState(declaredExtension, myInstance);
				push(newState);
			} else {
				super.enteringNewElementExtension(theElement, theUrlAttr);
			}
		}

		@Override
		protected IElement getCurrentElement() {
			return myInstance;
		}

		@Override
		public void otherEvent(XMLEvent theEvent) {
			// ignore
		}

	}

	private class ExtensionState extends BaseState {

		private UndeclaredExtension myExtension;

		public ExtensionState(UndeclaredExtension theExtension) {
			myExtension = theExtension;
		}

		@Override
		public void attributeValue(Attribute theAttribute, String theValue) throws DataFormatException {
			throw new DataFormatException("'value' attribute is invalid in 'extension' element");
		}

		@Override
		public void endingElement(EndElement theElem) throws DataFormatException {
			if (myExtension.getValue() != null && myExtension.getUndeclaredExtensions().size() > 0) {
				throw new DataFormatException("Extension must not have both a value and other contained extensions");
			}
			pop();
		}

		@Override
		public void enteringNewElement(StartElement theElement, String theLocalPart) throws DataFormatException {
			BaseRuntimeElementDefinition<?> target = myContext.getRuntimeChildUndeclaredExtensionDefinition().getChildByName(theLocalPart);
			if (target == null) {
				throw new DataFormatException("Unknown extension element name: " + theLocalPart);
			}

			switch (target.getChildType()) {
			case COMPOSITE_DATATYPE: {
				BaseRuntimeElementCompositeDefinition<?> compositeTarget = (BaseRuntimeElementCompositeDefinition<?>) target;
				ICompositeDatatype newChildInstance = (ICompositeDatatype) compositeTarget.newInstance();
				myExtension.setValue(newChildInstance);
				ElementCompositeState newState = new ElementCompositeState(compositeTarget, newChildInstance);
				push(newState);
				return;
			}
			case PRIMITIVE_DATATYPE: {
				RuntimePrimitiveDatatypeDefinition primitiveTarget = (RuntimePrimitiveDatatypeDefinition) target;
				IPrimitiveDatatype<?> newChildInstance = primitiveTarget.newInstance();
				myExtension.setValue(newChildInstance);
				PrimitiveState newState = new PrimitiveState(newChildInstance);
				push(newState);
				return;
			}
			case RESOURCE_REF: {
				RuntimeResourceReferenceDefinition resourceRefTarget = (RuntimeResourceReferenceDefinition) target;
				ResourceReference newChildInstance = new ResourceReference();
				myExtension.setValue(newChildInstance);
				ResourceReferenceState newState = new ResourceReferenceState(resourceRefTarget, newChildInstance);
				push(newState);
				return;
			}
			case PRIMITIVE_XHTML:
			case RESOURCE:
			case RESOURCE_BLOCK:
			case UNDECL_EXT:
				break;
			}
		}

		@Override
		protected IElement getCurrentElement() {
			return myExtension;
		}

		@Override
		public void otherEvent(XMLEvent theEvent) throws DataFormatException {
			// ignore
		}

	}

	private class PreResourceState extends BaseState {

		private ICompositeElement myInstance;

		@Override
		public void attributeValue(Attribute theAttribute, String theValue) throws DataFormatException {
			// ignore
		}

		@Override
		public void endingElement(EndElement theElem) throws DataFormatException {
			// ignore
		}

		@Override
		public void enteringNewElement(StartElement theElement, String theLocalPart) throws DataFormatException {
			BaseRuntimeElementDefinition<?> definition = myContext.getNameToResourceDefinition().get(theLocalPart);
			if (!(definition instanceof RuntimeResourceDefinition)) {
				throw new DataFormatException("Element '" + theLocalPart + "' is not a resource, expected a resource at this position");
			}

			RuntimeResourceDefinition def = (RuntimeResourceDefinition) definition;
			myInstance = def.newInstance();

			push(new ElementCompositeState(def, myInstance));
		}

		@Override
		protected IElement getCurrentElement() {
			return myInstance;
		}

		@Override
		public void otherEvent(XMLEvent theEvent) throws DataFormatException {
			// ignore
		}

		@Override
		public void wereBack() {
			myObject = myInstance;
		}

	}

	private class PrimitiveState extends BaseState {
		private IPrimitiveDatatype<?> myInstance;

		public PrimitiveState(IPrimitiveDatatype<?> theInstance) {
			super();
			myInstance = theInstance;
		}

		@Override
		public void attributeValue(Attribute theAttribute, String theValue) throws DataFormatException {
			myInstance.setValueAsString(theValue);
		}

		@Override
		public void endingElement(EndElement theElem) {
			pop();
		}

		@Override
		public void enteringNewElement(StartElement theElement, String theLocalPart) throws DataFormatException {
			throw new Error("?? can this happen?"); // TODO: can this happen?
		}

		@Override
		protected IElement getCurrentElement() {
			return myInstance;
		}

		@Override
		public void otherEvent(XMLEvent theEvent) {
			// ignore
		}

	}

	private class ResourceReferenceState extends BaseState {

		private RuntimeResourceReferenceDefinition myDefinition;
		private ResourceReference myInstance;
		private ResourceReferenceSubState mySubState;

		public ResourceReferenceState(RuntimeResourceReferenceDefinition theDefinition, ResourceReference theInstance) {
			myDefinition = theDefinition;
			myInstance = theInstance;
			mySubState = ResourceReferenceSubState.INITIAL;
		}

		@Override
		public void attributeValue(Attribute theAttribute, String theValue) throws DataFormatException {
			switch (mySubState) {
			case DISPLAY:
				myInstance.setDisplay(theValue);
				break;
			case INITIAL:
				throw new DataFormatException("Unexpected attribute: " + theValue);
			case REFERENCE:
				myInstance.setReference(theValue);
				break;
			}
		}

		@Override
		public void endingElement(EndElement theElement) {
			switch (mySubState) {
			case INITIAL:
				pop();
				break;
			case DISPLAY:
			case REFERENCE:
				mySubState = ResourceReferenceSubState.INITIAL;
			}
		}

		@Override
		public void enteringNewElement(StartElement theElem, String theLocalPart) throws DataFormatException {
			switch (mySubState) {
			case INITIAL:
				if ("display".equals(theLocalPart)) {
					mySubState = ResourceReferenceSubState.DISPLAY;
					break;
				} else if ("reference".equals(theLocalPart)) {
					mySubState = ResourceReferenceSubState.REFERENCE;
					break;
				}
				//$FALL-THROUGH$
			case DISPLAY:
			case REFERENCE:
				throw new DataFormatException("Unexpected element: " + theLocalPart);
			}
		}

		@Override
		protected IElement getCurrentElement() {
			return myInstance;
		}

		@Override
		public void otherEvent(XMLEvent theEvent) {
			// ignore
		}

	}

	private enum ResourceReferenceSubState {
		DISPLAY, INITIAL, REFERENCE
	}

	private class XhtmlState extends BaseState {
		private int myDepth;
		private XhtmlDt myDt;
		private List<XMLEvent> myEvents = new ArrayList<XMLEvent>();

		private XhtmlState(XhtmlDt theXhtmlDt, StartElement theXhtmlStartElement) throws DataFormatException {
			myDepth = 1;
			myDt = theXhtmlDt;
			myEvents.add(theXhtmlStartElement);
		}

		@Override
		public void attributeValue(Attribute theAttr, String theValue) throws DataFormatException {
			myEvents.add(theAttr);
		}

		@Override
		public void endingElement(EndElement theElement) throws DataFormatException {
			myEvents.add(theElement);

			myDepth--;
			if (myDepth == 0) {
				myDt.setValue(myEvents);
				pop();
			}
		}

		@Override
		public void enteringNewElement(StartElement theElem, String theLocalPart) throws DataFormatException {
			myDepth++;
			myEvents.add(theElem);
		}

		@Override
		protected IElement getCurrentElement() {
			return myDt;
		}

		@Override
		public void otherEvent(XMLEvent theEvent) throws DataFormatException {
			myEvents.add(theEvent);
		}
	}

}