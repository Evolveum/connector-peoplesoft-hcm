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
import java.util.List;
import java.util.Map;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.identityconnectors.framework.common.objects.OperationalAttributeInfos;
import org.identityconnectors.framework.common.objects.ResultsHandler;

public class SchemaAssemblyStrategy extends DocumentProcessing implements HandlingStrategy {

	private static final Log LOGGER = Log.getLog(SchemaAssemblyStrategy.class);
	private String iterations = "";

	public ObjectClassInfo buildSchema(Map<String, Object> attributeMap) {
		ObjectClassInfoBuilder builder = new ObjectClassInfoBuilder();

		builder.addAttributeInfo(Name.INFO);
		builder.setType(ObjectClass.ACCOUNT_NAME);

		for (String attributeName : attributeMap.keySet()) {

			if ("Assignment_Status_Type".equals(attributeName)) {
				builder.addAttributeInfo(OperationalAttributeInfos.ENABLE);

			} else if ("Assignment".equals(attributeName)) {
				AttributeInfoBuilder infoBuilder = new AttributeInfoBuilder(attributeName);
				infoBuilder.setUpdateable(true);
				infoBuilder.setCreateable(true);
				infoBuilder.setReadable(true);
				infoBuilder.setType(String.class);
				infoBuilder.setMultiValued(true);
				infoBuilder.setReturnedByDefault(false);
				builder.addAttributeInfo(infoBuilder.build());
			} else {
				AttributeInfoBuilder infoBuilder = new AttributeInfoBuilder(attributeName);

				infoBuilder.setUpdateable(true);
				infoBuilder.setCreateable(true);
				infoBuilder.setReadable(true);
				infoBuilder.setReturnedByDefault(false);
				infoBuilder.setType(String.class);
				builder.addAttributeInfo(infoBuilder.build());
			}
		}
		
		return builder.build();

	}

	@Override
	public List<String> populateDictionary(String flag) {

		List<String> dictionary = new ArrayList<String>();

		if (FIRSTFLAG.equals(flag)) {

			if (iterations.isEmpty()) {
				dictionary.add("1000");
			} else {
				dictionary.add(iterations);

			}
		} else {
			LOGGER.warn("No such flag defined: {0}", flag);
		}
		return dictionary;

	}

	@Override
	public Map<String, Object> handleEmployeeData(Map<String, Object> attributeMap,
			Map<String, Object> schemaAttributeMap, ResultsHandler handler, String uidAttributeName, String primaryId) {

		return attributeMap;

	}

	public String setIterations(String value) {
		this.iterations = value;

		return this.iterations;
	}

}
