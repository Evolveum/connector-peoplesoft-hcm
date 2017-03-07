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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.filter.AttributeFilter;
import org.identityconnectors.framework.common.objects.filter.CompositeFilter;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.common.objects.filter.NotFilter;

public class FilterQueryStrategy extends DocumentProcessing implements HandlingStrategy {

	private static final Log LOGGER = Log.getLog(FilterQueryStrategy.class);

	private Map<String, Map<String, Object>> buffer = new HashMap<String, Map<String, Object>>();

	@Override
	public Boolean checkFilter(String endName, String value, Filter filter, String uidAttributeName) {

		if (filter instanceof NotFilter) { // not filter consistent only with
											// AttributeFilter as parameter
			Filter evaluatedFilter = ((NotFilter) filter).getFilter();
			if (evaluatedFilter instanceof AttributeFilter) {

				String attributeName = ((AttributeFilter) evaluatedFilter).getName();

				if (evaluatedFilter instanceof CompositeFilter) {

					return false;
				} else if (!(attributeName.equals("__UID__") && uidAttributeName.equals(endName))
						&& !attributeName.equals(endName)) {

					return true;
				}
			}

		}

		if (filter != null) {

			StringBuilder heplerVariableBuilder = new StringBuilder(endName).append(".").append(value).append(".")
					.append(uidAttributeName);

			Boolean outcome = filter.accept(new FilterHandler(), heplerVariableBuilder.toString());

			return outcome;
		} else {

			return false;
		}

	}

	public Map<String, Object> handleEmployeeData(Map<String, Object> attributeMap,
			Map<String, Object> schemaAttributeMap, ResultsHandler handler, String uidAttributeName, String primariId) {

		if (!attributeMap.isEmpty()) {
			attributeMap = injectAttributes(attributeMap, schemaAttributeMap);

			String uid = (String) attributeMap.get(uidAttributeName);

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
		LOGGER.ok("Processing trough buffered data");
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

				if (evaluatedMap.containsKey(ASSIGNMENTTAG)) {

					record = (String) evaluatedMap.get(ASSIGNMENTTAG);

				}

				if (record.isEmpty()) {
					// LOGGER.ok("Empty assignment record present in the account
					// with the id {0}", employeeUid);

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
						List<String> processedAssigments = (List<String>) employeeObject.get(ASSIGNMENTTAG);
						processedAssigments.add(record);
						employeeObject.put(ASSIGNMENTTAG, processedAssigments);
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
