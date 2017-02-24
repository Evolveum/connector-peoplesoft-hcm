package com.evolveum.polygon.hcm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.SchemaBuilder;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.Connector;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.framework.spi.operations.SchemaOp;
import org.identityconnectors.framework.spi.operations.SearchOp;
import org.identityconnectors.framework.spi.operations.TestOp;

@ConnectorClass(displayNameKey = "HcmConnector.connector.display", configurationClass = HcmConnectorConfiguration.class)
public class HcmConnector implements Connector, SchemaOp, SearchOp<Filter>, TestOp {

	private HcmConnectorConfiguration configuration;
	private Schema schema = null;

	private static final Log LOGGER = Log.getLog(HcmConnector.class);

	@Override
	public void test() {
		HandlingStrategy strategy = new SchemaAssemblyStrategy();
		((SchemaAssemblyStrategy) strategy).setIterations(configuration.getIterations());
		Map<String, Object> schemaMap;

		schemaMap = strategy.parseXMLData(configuration, null, null, null);

		if (schemaMap.isEmpty()) {
			throw new ConnectorException(
					"No schema information was returned by the parser, please check if the configuration and the hcm resource is valid");
		}

	}

	@Override
	public FilterTranslator<Filter> createFilterTranslator(ObjectClass objectClass, OperationOptions options) {
		return new FilterTranslator<Filter>() {
			@Override
			public List<Filter> translate(Filter filter) {
				return CollectionUtil.newList(filter);
			}
		};
	}

	@Override
	public void executeQuery(ObjectClass objectClass, Filter query, ResultsHandler handler, OperationOptions options) {

		LOGGER.info("The filter query: {0}", query);

		HandlingStrategy strategy = new SchemaAssemblyStrategy();
		((SchemaAssemblyStrategy) strategy).setIterations(configuration.getIterations());
		Map<String, Object> schemaMap;

		schemaMap = strategy.parseXMLData(configuration, handler, null, null);

		if (query == null) {
			strategy = new ObjectBuilderStrategy();

			strategy.parseXMLData(configuration, handler, schemaMap, query);

		} else {
			strategy = new FilterQueryStrategy();
			strategy.parseXMLData(configuration, handler, schemaMap, query);
		}

	}

	@Override
	public Schema schema() {
		if (schema == null) {

			Map<String, Object> attributeMap = new HashMap<String, Object>();
			SchemaBuilder schemaBuilder = new SchemaBuilder(HcmConnector.class);
			HandlingStrategy strategy = new SchemaAssemblyStrategy();

			((SchemaAssemblyStrategy) strategy).setIterations(configuration.getIterations());

			attributeMap = strategy.parseXMLData(configuration, null, null, null);

			ObjectClassInfo oclassInfo = ((SchemaAssemblyStrategy) strategy).buildSchema(attributeMap);

			schemaBuilder.defineObjectClass(oclassInfo);

			this.schema = schemaBuilder.build();

			// LOGGER.info("The schema: {0}", this.schema);
		}
		return this.schema;
	}

	@Override
	public Configuration getConfiguration() {
		return this.configuration;
	}

	@Override
	public void init(Configuration configuration) {
		this.configuration = (HcmConnectorConfiguration) configuration;
		this.configuration.validate();
	}

	@Override
	public void dispose() {
		this.configuration = null;

	}

}
