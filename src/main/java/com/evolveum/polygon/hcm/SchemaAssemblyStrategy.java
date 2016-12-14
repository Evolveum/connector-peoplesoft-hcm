package com.evolveum.polygon.hcm;

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
			
			if("Assignment_Status_Type".equals(attributeName)){
				builder.addAttributeInfo(OperationalAttributeInfos.ENABLE);
				
			} else if ("assignments_record".equals(attributeName)){
				AttributeInfoBuilder infoBuilder = new AttributeInfoBuilder(attributeName);
				infoBuilder.setUpdateable(true);
				infoBuilder.setCreateable(true);
				infoBuilder.setReadable(true);
				infoBuilder.setType(String.class);
				infoBuilder.setMultiValued(true);
				builder.addAttributeInfo(infoBuilder.build());
			}else{
			AttributeInfoBuilder infoBuilder = new AttributeInfoBuilder(attributeName);

			infoBuilder.setUpdateable(true);
			infoBuilder.setCreateable(true);
			infoBuilder.setReadable(true);

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
				dictionary.add("50"); 
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
			Map<String, Object> schemaAttributeMap, ResultsHandler handler, String uidAttributeName, String primariId) {

		return attributeMap;

	}

	public String setIterations(String value) {
		this.iterations = value;

		return this.iterations;
	}

}
