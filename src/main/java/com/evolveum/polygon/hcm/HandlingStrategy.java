package com.evolveum.polygon.hcm;

import java.net.ConnectException;
import java.util.List;
import java.util.Map;

import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.filter.Filter;

public interface HandlingStrategy {
	public static final String FIRSTFLAG = "parser";
	public static String ID = "Extract_Employee_Person_ID";
	public static final String DOT = ".";
	public static final String DELIMITER = "\\_";

	public Map<String, Object> parseXMLData(HcmConnectorConfiguration conf, ResultsHandler handler,
			Map<String, Object> schemaAttributeMap, Filter query) throws ConnectException;

	public String processMultiValuedAttributes(Map<String, String> multiValuedAttributeBuffer);

	public List<String> populateDictionary(String flag);

	public Map<String, Object> handleEmployeeData(Map<String, Object> attributeMap,
			Map<String, Object> schemaAttributeMap, ResultsHandler handler, String uidAttributeName, String primariId);

	public Map<String, Object> injectAttributes(Map<String, Object> attributeMap,
			Map<String, Object> schemaAttributeMap);

	public Boolean checkFilter(String endName, String value, Filter filter, String uidAttributeName);
	
	public void handleBufferedData(String uidAttributeName, String primariId, ResultsHandler handler);
}
