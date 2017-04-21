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

package com.evolveum.polygon.test.hcm;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.ConnectorIOException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.SearchResult;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.common.objects.filter.FilterBuilder;
import org.identityconnectors.framework.spi.SearchResultsHandler;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.evolveum.polygon.connector.hcm.HcmConnector;
import com.evolveum.polygon.connector.hcm.HcmConnectorConfiguration;

public class TestSuiteHcm {
	private static final Log LOGGER = Log.getLog(TestSuiteHcm.class);
	private String propertyFilePath = "../hcm-connector/testProperties/test.properties";
	private HcmConnector connector;
	private HcmConnectorConfiguration configuration;

	private static String INCONSISTENFILE = "filepath_xmlinconsistent";
	private static String CONSISTENFILE = "filepath_xmlconsistent";
	private static String FILENOTFOUND = "filepath_filenotfound";
	private static String UID = "uid";
	private static String SECONDARYID = "secondaryid";
	private static String SECONDARYIDMISSING = "secondaryid_missing";

	private static String ITERATIONS = "iterationcount";

	private static ArrayList<ConnectorObject> results = new ArrayList<>();

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

	@DataProvider(name = "CONFIGTESTPROVIDER")
	public Object[][] configurationResourcesProvider() {
		return new Object[][] { { true, true } };
	}

	@Test(expectedExceptions = ConnectorException.class, priority = 1, dataProvider = "CONFIGTESTPROVIDER")
	private void xmlDataInconsistent(Boolean v1, Boolean v2) {

		LOGGER.info("## The evaluated test: ConnectorException if xml data inconsistent");
		configuration = new HcmConnectorConfiguration();

		configuration.setFilePath(getProperty(INCONSISTENFILE));
		configuration.setIterations(getProperty(ITERATIONS));
		configuration.setPrimaryId(getProperty(SECONDARYID));
		configuration.setUidAttribute(getProperty(UID));

		connector = new HcmConnector();
		connector.init(configuration);
		connector.schema();
	}

	@Test(expectedExceptions = ConnectorIOException.class, dependsOnMethods = {
			"xmlDataInconsistent" }, priority = 1, dataProvider = "CONFIGTESTPROVIDER")
	private void resourceFileNotFound(Boolean v1, Boolean v2) {

		LOGGER.info("## The evaluated test: ConnectorException if file not found");
		configuration = new HcmConnectorConfiguration();

		configuration.setFilePath(getProperty(FILENOTFOUND));
		configuration.setIterations(getProperty(ITERATIONS));
		configuration.setPrimaryId(getProperty(SECONDARYID));
		configuration.setUidAttribute(getProperty(UID));

		connector = new HcmConnector();
		connector.init(configuration);
		connector.schema();
	}

	@Test(dependsOnMethods = { "resourceFileNotFound" }, priority = 1, dataProvider = "CONFIGTESTPROVIDER")
	private void initializationWithCorrectResource(Boolean v1, Boolean v2) {

		LOGGER.info("## The evaluated test: initialization of connector via correct configuration ");
		configuration = new HcmConnectorConfiguration();

		configuration.setFilePath(getProperty(CONSISTENFILE));
		configuration.setIterations(getProperty(ITERATIONS));
		configuration.setPrimaryId(getProperty(SECONDARYID));
		configuration.setUidAttribute(getProperty(UID));

		connector = new HcmConnector();
		connector.init(configuration);
		connector.schema();

		Assert.assertTrue(true);
	}

	@Test(expectedExceptions = ConnectorException.class, dependsOnMethods = {
			"initializationWithCorrectResource" }, priority = 3, dataProvider = "CONFIGTESTPROVIDER")
	private void secondaryAttributeValueMissing(Boolean v1, Boolean v2) {

		LOGGER.info("## The evaluated test: secondary attribute value missing ");
		configuration.setPrimaryId(getProperty(SECONDARYIDMISSING));

		connector.init(configuration);
		connector.schema();

		connector.executeQuery(ObjectClass.ACCOUNT, null, null, null);
	}

	@Test(dependsOnMethods = { "initializationWithCorrectResource" }, priority = 2, dataProvider = "CONFIGTESTPROVIDER")
	private void listAllEntries(Boolean v1, Boolean v2) {

		results = new ArrayList<>();

		LOGGER.info("## The evaluated test: lit all entries ");

		connector.executeQuery(ObjectClass.ACCOUNT, null, handler, null);

		Assert.assertFalse(results.isEmpty());

	}

	@Test(dependsOnMethods = { "initializationWithCorrectResource" }, priority = 2, dataProvider = "CONFIGTESTPROVIDER")
	private void egualsFilterTest(Boolean v1, Boolean v2) {

		String attrName = getProperty("filter_equals_attr");
		String attrValue = getProperty("filter_equals_value");
		results = new ArrayList<>();

		LOGGER.info("## The evaluated test: lit all entries ");

		Filter filter = FilterBuilder.equalTo(AttributeBuilder.build(attrName, attrValue));
		connector.executeQuery(ObjectClass.ACCOUNT, filter, handler, null);

		Assert.assertFalse(results.isEmpty());

	}

	@Test(dependsOnMethods = { "initializationWithCorrectResource" }, priority = 2, dataProvider = "CONFIGTESTPROVIDER")
	private void containsFilterTest(Boolean v1, Boolean v2) {

		String attrName = getProperty("filter_contains_attr");
		String attrValue = getProperty("filter_contains_value");
		results = new ArrayList<>();

		LOGGER.info("## The evaluated test: lit all entries ");

		Filter filter = FilterBuilder.contains(AttributeBuilder.build(attrName, attrValue));
		connector.executeQuery(ObjectClass.ACCOUNT, filter, handler, null);

		Assert.assertFalse(results.isEmpty());

	}

	@Test(dependsOnMethods = { "initializationWithCorrectResource" }, priority = 2, dataProvider = "CONFIGTESTPROVIDER")
	private void notFilterTest(Boolean v1, Boolean v2) {

		String conAttrName = getProperty("filter_contains_attr");
		String conAttrValue = getProperty("filter_contains_value");
		results = new ArrayList<>();

		LOGGER.info("## The evaluated test: lit all entries ");

		Filter containsFilter = FilterBuilder.contains(AttributeBuilder.build(conAttrName, conAttrValue));
		Filter notFilter = FilterBuilder.not(containsFilter);

		connector.executeQuery(ObjectClass.ACCOUNT, notFilter, handler, null);

		Assert.assertFalse(results.isEmpty());

	}

	@Test(dependsOnMethods = { "initializationWithCorrectResource" }, priority = 2, dataProvider = "CONFIGTESTPROVIDER")
	private void andFilterTest(Boolean v1, Boolean v2) {

		String conAttrName = getProperty("filter_contains_attr");
		String conAttrValue = getProperty("filter_contains_value");
		String eqAttrName = getProperty("filter_equals_attr");
		String eqAttrValue = getProperty("filter_equals_value");
		results = new ArrayList<>();

		LOGGER.info("## The evaluated test: lit all entries ");

		Filter containsFilter = FilterBuilder.contains(AttributeBuilder.build(conAttrName, conAttrValue));
		Filter equalsFilter = FilterBuilder.contains(AttributeBuilder.build(conAttrName, conAttrValue));
		Filter notFilter = FilterBuilder.and(containsFilter, equalsFilter);

		connector.executeQuery(ObjectClass.ACCOUNT, notFilter, handler, null);

		Assert.assertFalse(results.isEmpty());

	}

	@Test(dependsOnMethods = { "initializationWithCorrectResource" }, priority = 2, dataProvider = "CONFIGTESTPROVIDER")
	private void queryOptionsAttributesToGetTest(Boolean v1, Boolean v2) {

		Boolean doesNotContain = false;
		// TODO

		String option = getProperty("query_options_option");
		String value = getProperty("query_options_value");

		List<String> attributesToContain = new ArrayList<String>();
		attributesToContain.add(value);
		attributesToContain.add("__UID__");
		attributesToContain.add("__NAME__");

		Map<String, Object> operationOptions = new HashMap<String, Object>();

		String[] attrsToGet = { value };

		operationOptions.put(option, attrsToGet);

		OperationOptions options = new OperationOptions(operationOptions);

		results = new ArrayList<>();

		// LOGGER.info("## The evaluated test: lit all entries ");

		connector.executeQuery(ObjectClass.ACCOUNT, null, handler, options);

		if (!results.isEmpty()) {

			ConnectorObject evaluatedResult = results.get(0);
			Set<Attribute> attrs = (Set<Attribute>) evaluatedResult.getAttributes();

			for (Attribute attr : attrs) {
				if (!attributesToContain.contains(attr.getName())) {
					doesNotContain = true;
				} else {
					attributesToContain.remove(attr.getName());
				}
			}

		}

		Assert.assertFalse(!attributesToContain.isEmpty() || doesNotContain);

	}

	private String getProperty(String propertyName) {
		String property = null;

		Properties properties = new Properties();

		try {
			FileInputStream inputStream = new FileInputStream(propertyFilePath);

			properties.load(inputStream);

			property = properties.getProperty(propertyName);
			LOGGER.info("The property which is fetched: {0} and its value: {1}", propertyName, property);

		} catch (FileNotFoundException e) {
			LOGGER.error("Error while reading the property file. {0}", e.getLocalizedMessage());

			e.printStackTrace();
		} catch (IOException e) {
			LOGGER.error("Error while reading the property file. {0}", e.getLocalizedMessage());
			e.printStackTrace();
		}

		return property;
	}

}
