package com.evolveum.polygon.connector.hcm;
/**
 * 
 * @author Matus
 *
 */

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.SearchResult;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.AndFilter;
import org.identityconnectors.framework.common.objects.filter.ContainsFilter;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.FilterBuilder;
import org.identityconnectors.framework.common.objects.filter.NotFilter;
import org.identityconnectors.framework.spi.SearchResultsHandler;

public class Main {

	private static final Log LOGGER = Log.getLog(Main.class);

	private static final ArrayList<ConnectorObject> results = new ArrayList<>();

	//private static final Uid TEST_UID = new Uid(""); // the value
																// of
																// "
																// attribute

	public static void main(String[] args) throws IOException {
//		HcmConnector conn = new HcmConnector();
//		conn.init(buildConfig());
//		conn.schema();
		
//	EqualsFilter uidEqualsFilterTest = (EqualsFilter) FilterBuilder.equalTo(TEST_UID);
//		
//		 conn.executeQuery(ObjectClass.ACCOUNT, null, handler, null);
//
	
//
//		 ContainsFilter containsFilterTest = (ContainsFilter) FilterBuilder .contains(AttributeBuilder.build("", ""));
//
//		 ContainsFilter containsFilterTestTwo = (ContainsFilter) FilterBuilder .contains(AttributeBuilder.build("", ""));
//		 
		// ContainsFilter containsAllValuesFilterTest = (ContainsFilter) FilterBuilder .containsAllValues(AttributeBuilder.build("", ""));
		 
		 
//		 EqualsFilter eqTest = (EqualsFilter) FilterBuilder .equalTo(AttributeBuilder.build("", ""));
//		 AndFilter andFilter = (AndFilter) FilterBuilder.and(containsFilterTest, eqTest);
//
//		 NotFilter notTest= (NotFilter) FilterBuilder.not(andFilter); // TODO check filter
//		 
		
		 
		// conn.executeQuery(ObjectClass.ACCOUNT, notTest, handler, null);
		 
		for (ConnectorObject object : results) {

			LOGGER.info("the result: {0}", object.toString());

		}	
		LOGGER.info("the number of results: {0}", results.size());
	}

	public static SearchResultsHandler handler = new SearchResultsHandler() {

		@Override
		public boolean handle(ConnectorObject connectorObject) {
			results.add(connectorObject);
			return true;
		}

		@Override
		public void handleResult(SearchResult result) {
			LOGGER.info("Im handling {0}", result.getRemainingPagedResults());
		}
	};

	public static HcmConnectorConfiguration buildConfig() {
		HcmConnectorConfiguration config = new HcmConnectorConfiguration();

		 config.setFilePath("");
		 config.setIterations("1000000");
		 config.setUidAttribute("");
		 config.setPrimaryId("");
		
		 return config;

	}
	
	public static OperationOptions getOptions() {

		Map<String, Object> operationOptions = new HashMap<String, Object>();

		
		String[] attrsToGet= {"Person_Last_Name"};
		
		operationOptions.put("ATTRS_TO_GET", attrsToGet);

	OperationOptions options = new OperationOptions(operationOptions);

		return options;
	}
	
	static String readFile(String path, Charset encoding) 
			  throws IOException 
			{
			  byte[] encoded = Files.readAllBytes(Paths.get(path));
			  return new String(encoded, encoding);
			}

}
