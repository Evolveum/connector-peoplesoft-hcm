package com.evolveum.polygon.hcm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ResultsHandler;

public class ObjectBuilderStrategy extends DocumentProcessing implements HandlingStrategy {

	public static String DISPLAYNAME = "Person_Display_Name";

	public ConnectorObject buildConnectorObject(Map<String, Object> attributes, String uidAttributeName, String primariId) {

		ConnectorObjectBuilder cob = new ConnectorObjectBuilder();
		cob.setUid((String) attributes.get(uidAttributeName));
		cob.setName((String) attributes.get(primariId));
		cob.setObjectClass(ObjectClass.ACCOUNT);

		for (String attributeName : attributes.keySet()) {

			
			if(!attributeName.equals("Assignment_Status_Type")){
				Object attribute = attributes.get(attributeName);
				if(! (attribute instanceof ArrayList<?>)){
			cob.addAttribute(attributeName, attribute);
				}else{
					
					Collection<Object> col = new ArrayList<Object>();
			for (String st: (List<String>) attribute){

				col.add(st);
			}
					cob.addAttribute(attributeName, col);
				}
			}else{
				
			Boolean isActive= false;
			if(attributes.get(attributeName).equals("ACTIVE")){
				isActive =true;
			}
			
				cob.addAttribute("__ENABLE__", isActive);
			}
		}

		ConnectorObject finalConnectorObject = cob.build();

		return finalConnectorObject;
	}

	@Override
	public Map<String, Object> handleEmployeeData(Map<String, Object> attributeMap,
			Map<String, Object> schemaAttributeMap, ResultsHandler handler, String uidAttributeName, String primariId) {

		attributeMap = injectAttributes(attributeMap, schemaAttributeMap);

		ConnectorObject connectorObject = buildConnectorObject(attributeMap, uidAttributeName, primariId);
		handler.handle(connectorObject);

		attributeMap = new HashMap<String, Object>();
		return attributeMap;

	}

	public Map<String, Object> injectAttributes(Map<String, Object> attributeMap,
			Map<String, Object> schemaAttributeMap) {

		Map<String, Object> assembledMap = new HashMap<String, Object>();

		for (String schemaAttributeName : schemaAttributeMap.keySet()) {

			if (!attributeMap.containsKey(schemaAttributeName)) {

				assembledMap.put(schemaAttributeName, "");

			} else {
				assembledMap.put(schemaAttributeName, (String) attributeMap.get(schemaAttributeName));
			}

		}
		return assembledMap;
	}

}
