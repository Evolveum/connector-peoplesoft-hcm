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

import java.util.List;
import java.util.Map;

import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.filter.Filter;

public interface HandlingStrategy {
	public static final String FIRSTFLAG = "parser";
	public static String ID = "Extract_Employee_Person_ID";
	public static final String DOT = ".";
	public static final String DELIMITER = "\\_";
	public static final String ASSIGNMENTTAG = "Assignment";

	public Map<String, Object> parseXMLData(HcmConnectorConfiguration conf, ResultsHandler handler,
			Map<String, Object> schemaAttributeMap, Filter query);

	public String processMultiValuedAttributes(Map<String, String> multiValuedAttributeBuffer);

	public List<String> populateDictionary(String flag);

	public Map<String, Object> handleEmployeeData(Map<String, Object> attributeMap,
			Map<String, Object> schemaAttributeMap, ResultsHandler handler, String uidAttributeName, String primaryId);

	public Map<String, Object> injectAttributes(Map<String, Object> attributeMap,
			Map<String, Object> schemaAttributeMap);

	public Boolean checkFilter(String endName, String value, Filter filter, String uidAttributeName);

	public void handleBufferedData(String uidAttributeName, String primaryId, ResultsHandler handler);
	
	public void evaluateOptions(OperationOptions options);
}
