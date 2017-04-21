/*
 * Copyright (c) 2016 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.polygon.connector.hcm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConfigurationException;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ResultsHandler;

public class ObjectBuilderStrategy extends DocumentProcessing implements HandlingStrategy {

	private Map<String, Map<String, Object>> buffer = new HashMap<String, Map<String, Object>>();
	private static final Log LOGGER = Log.getLog(ObjectBuilderStrategy.class);

	public ConnectorObject buildConnectorObject(Map<String, Object> attributes, String uidAttributeName,
			String primaryId) {

		ConnectorObjectBuilder cob = new ConnectorObjectBuilder();
		cob.setUid((String) attributes.get(uidAttributeName));
		cob.setName((String) attributes.get(primaryId));
		cob.setObjectClass(ObjectClass.ACCOUNT);

		for (String attributeName : attributes.keySet()) {

			if (!attributeName.equals("Assignment_Status_Type")) {
				Object attribute = attributes.get(attributeName);
				if (!(attribute instanceof ArrayList<?>)) {
					if (!uidAttributeName.equals(attributeName) && !primaryId.equals(attributeName)) {

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
			Map<String, Object> schemaAttributeMap, ResultsHandler handler, String uidAttributeName, String primaryId) {

		if (!attributeMap.isEmpty()) {
			attributeMap = injectAttributes(attributeMap, schemaAttributeMap);

			String uid = (String) attributeMap.get(uidAttributeName);

			// LOGGER.ok("The processed uid: {0}", uid);

			if (!buffer.containsKey(uid)) {

				buffer.put(uid, attributeMap);

			} else {
				StringBuilder idBuilder = null;
				int order = 0;
				String originalUid = uid;

				while (buffer.containsKey(uid)) {
					idBuilder = new StringBuilder(originalUid).append(DOT).append(order);
					uid = idBuilder.toString();
					order++;
				}
				buffer.put(uid, attributeMap);

				//
				// if (attributeMap.containsKey(ASSIGMENTID)){
				// StringBuilder idBuilder = new StringBuilder(uid);
				// String assigment = attributeMap.get(ASSIGMENTID);
				// idBuilder.append(DOT).append(assigment);
				// buffer.put(idBuilder.toString(), attributeMap);
				//
				// }else {
				// LOGGER.error("No assigment number defined for the user: {0}",
				// uid);
				// }
				//
			}

			attributeMap = new HashMap<String, Object>();
		}

		return attributeMap;

	}

	public void handleBufferedData(String uidAttributeName, String primaryId, ResultsHandler handler) {
		LOGGER.ok("The buffered data are being processed");

		HandlingStrategy strategy = new ObjectBuilderStrategy();

		ConnectorObject connectorObject;
		int length = 0;
		Map<String, Object> evaluatedMap;
		Map<String, Object> employeeObject = new HashMap<>();
		Map<String, Map<String, Object>> entries = new HashMap<String, Map<String, Object>>();

		String record = "";

		if (!buffer.isEmpty()) {

			for (String employeeUid : buffer.keySet()) {
				evaluatedMap = buffer.get(employeeUid);

				String[] splitId = employeeUid.split("\\.");
				String uid;
				Boolean noAssignment = false;

				length = splitId.length;

				if (length > 1) {
					uid = splitId[0];
				} else {
					uid = employeeUid;
				}

				if (evaluatedMap.containsKey(ASSIGNMENTTAG)) {

					record = (String) evaluatedMap.get(ASSIGNMENTTAG);
					noAssignment = false;
				} else {

					noAssignment = true;
				}

				if (evaluatedMap.containsKey(uidAttributeName)) {

					String evalUid = (String) evaluatedMap.get(primaryId);

					if (evalUid == null || evalUid.isEmpty()) {
						StringBuilder errorBuilder = new StringBuilder(
								"UID attribute value missing from a record please make sure all the record contain the value for the following attribute: ")
										.append(uidAttributeName);

						throw new ConfigurationException(errorBuilder.toString());
					}
				}
				if (evaluatedMap.containsKey(primaryId)) {

					String evalPid = (String) evaluatedMap.get(primaryId);
					if (evalPid == null || evalPid.isEmpty()) {
						StringBuilder errorBuilder = new StringBuilder(
								"Name attribute value missing from an record .Please make sure that all the attributes have the following attribute: ")
										.append(primaryId);
						throw new ConfigurationException(errorBuilder.toString());
					}
				}

				if (record.isEmpty()) {
					// LOGGER.info("Empty assignment record present in the
					// account with the id {0}", employeeUid);

				}

				if (!entries.containsKey(uid)) {
					employeeObject = new HashMap<String, Object>();
					employeeObject.putAll(evaluatedMap);

					if (!noAssignment) {
						List<String> recordList = new ArrayList<String>();
						recordList.add(record);
						employeeObject.put(ASSIGNMENTTAG, recordList);
					}
					entries.put(uid, employeeObject);

				} else {
					if (!noAssignment) {
						employeeObject = entries.get(uid);
						List<String> processedAssignments = (List<String>) employeeObject.get(ASSIGNMENTTAG);
						processedAssignments.add(record);
						employeeObject.put(ASSIGNMENTTAG, processedAssignments);
						entries.put(uid, employeeObject);
					}
				}

				record = "";

			}

			if (!entries.isEmpty()) {
				for (String entrieId : entries.keySet()) {
					employeeObject = entries.get(entrieId);
					connectorObject = ((ObjectBuilderStrategy) strategy).buildConnectorObject(employeeObject,
							uidAttributeName, primaryId);
					handler.handle(connectorObject);
				}
			}

		}

	}

}
