package com.evolveum.polygon.hcm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ResultsHandler;

public class ObjectBuilderStrategy extends DocumentProcessing implements HandlingStrategy {

	private Map<String, Map<String, Object>> buffer = new HashMap<String, Map<String, Object>>();
	private static final Log LOGGER = Log.getLog(ObjectBuilderStrategy.class);

	public ConnectorObject buildConnectorObject(Map<String, Object> attributes, String uidAttributeName,
			String primariId) {

		ConnectorObjectBuilder cob = new ConnectorObjectBuilder();
		cob.setUid((String) attributes.get(uidAttributeName));
		cob.setName((String) attributes.get(primariId));
		cob.setObjectClass(ObjectClass.ACCOUNT);

		for (String attributeName : attributes.keySet()) {
			if (!attributeName.equals("Assignment_Status_Type")) {
				Object attribute = attributes.get(attributeName);
				if (!(attribute instanceof ArrayList<?>)) {
					if (!uidAttributeName.equals(attributeName) || !primariId.equals(attributeName)) {
						cob.addAttribute(attributeName, attribute);
					}
				} else {

					Collection<Object> col = new ArrayList<Object>();
					for (String st : (List<String>) attribute) {

						col.add(st);
					}
					cob.addAttribute(attributeName, col);
				}
			} else {

				Boolean isActive = false;
				if (attributes.get(attributeName).equals("ACTIVE")) {
					isActive = true;
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

		if (!attributeMap.isEmpty()) {

			String uid = (String) attributeMap.get(uidAttributeName);

			if (uid == null || uid.isEmpty()) {

				StringBuilder errorBuilder = new StringBuilder(
						"The UID value of an record is missing, please make sure all the record contain a value for the following attribute: ")
								.append(uidAttributeName);

				throw new ConnectorException(errorBuilder.toString());

			}

			if (!buffer.containsKey(uid)) {

				buffer.put(uid, attributeMap);
			}

			attributeMap = new HashMap<String, Object>();
		}

		return attributeMap;

	}

	public void handleBufferedData(String uidAttributeName, String primariIdName, ResultsHandler handler) {

		Map<String, Object> evaluatedMap;
		Map<String, Object> finalMap;
		String uidValue = "";
		String primIdValue = "";

		if (!buffer.isEmpty()) {

			for (String employeeUid : buffer.keySet()) {
				evaluatedMap = buffer.get(employeeUid);

				uidValue = "";
				primIdValue = "";
				finalMap = new HashMap<String, Object>();

				if (evaluatedMap.containsKey(uidAttributeName)) {
					uidValue = (String) evaluatedMap.get(uidAttributeName);
					finalMap.put(uidAttributeName, uidValue);
				} else {

					StringBuilder errorBuilder = new StringBuilder(
							"UID attribute value missing from a record please make sure all the record contain the value for the following attribute: ")
									.append(uidAttributeName);

					throw new ConnectorException(errorBuilder.toString());

				}
				if (evaluatedMap.containsKey(primariIdName)) {
					primIdValue = (String) evaluatedMap.get(primariIdName);

					finalMap.put(primariIdName, primIdValue);

				} else {
					StringBuilder errorBuilder;
					if (uidValue != null) {
						errorBuilder = new StringBuilder("Name attribute missing from the record with the UID: ")
								.append(uidValue)
								.append(" .Please make sure that all the attributes have the following attribute: ")
								.append(primariIdName);
						LOGGER.error(
								"Name attribute value missing from an record, the uid: {0} .Please make sure that all the attributes have the following attribute: {1}",
								uidValue, primariIdName);
					} else {
						errorBuilder = new StringBuilder(
								"Name attribute value missing from an record .Please make sure that all the attributes have the following attribute: ")
										.append(primariIdName);
						LOGGER.error(
								"Name attribute value missing from an record .Please make sure that all the attributes have the following attribute: {0}",
								primariIdName);
					}
					throw new ConnectorException(errorBuilder.toString());
				}

				ConnectorObject connectorObject = buildConnectorObject(finalMap, uidAttributeName, primariIdName);
				handler.handle(connectorObject);
			}
		}

	}

}
