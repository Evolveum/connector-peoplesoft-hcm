package com.evolveum.polygon.hcm;

import java.net.ConnectException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.SchemaBuilder;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.ContainsFilter;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.Connector;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.framework.spi.operations.DeleteOp;
import org.identityconnectors.framework.spi.operations.SchemaOp;
import org.identityconnectors.framework.spi.operations.SearchOp;
import org.identityconnectors.framework.spi.operations.TestOp;

@ConnectorClass(displayNameKey = "HcmConnector.connector.display", configurationClass = HcmConnectorConfiguration.class)
public class HcmConnector implements Connector, SchemaOp, SearchOp<Filter>, TestOp, DeleteOp {

	private HcmConnectorConfiguration configuration;
	private Schema schema = null;

	private static final Log LOGGER = Log.getLog(HcmConnector.class);

	@Override
	public void test() {
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

		LOGGER.info("The fiter query: {0}", query);
		
		HandlingStrategy strategy = new SchemaAssemblyStrategy();
		((SchemaAssemblyStrategy) strategy).setIterations(configuration.getIterations());
		Map<String, Object> schemaMap;
		try {
			schemaMap = strategy.parseXMLData(configuration, handler, null, null);

			if (query == null) {
				strategy = new ObjectBuilderStrategy();
				strategy.parseXMLData(configuration, handler, schemaMap, query);

			} else if (query instanceof EqualsFilter || query instanceof ContainsFilter) {
				strategy = new FilterQueryStrategy();
				strategy.parseXMLData(configuration, handler, schemaMap, query);

			} else {

				LOGGER.warn("Unsupported action");

			}

		} catch (ConnectException e) {

			StringBuilder errorBuilder = new StringBuilder("An exception occurred while processing the query: ")
					.append(e.getLocalizedMessage());

			new ConnectException(errorBuilder.toString());
		}

	}

	@Override
	public Schema schema() {
		if (schema == null) {

			Map<String, Object> attributeMap = new HashMap<String, Object>();
			SchemaBuilder schemaBuilder = new SchemaBuilder(HcmConnector.class);
			HandlingStrategy strategy = new SchemaAssemblyStrategy();

			((SchemaAssemblyStrategy) strategy).setIterations(configuration.getIterations());
			try {
				attributeMap = strategy.parseXMLData(configuration, null, null, null);

				ObjectClassInfo oclassInfo = ((SchemaAssemblyStrategy) strategy).buildSchema(attributeMap);

				schemaBuilder.defineObjectClass(oclassInfo);

				this.schema = schemaBuilder.build();

				LOGGER.info("The schema: {0}", this.schema);

			} catch (ConnectException e) {

				StringBuilder errorBuilder = new StringBuilder("An exception occurred while processing the query: ")
						.append(e.getLocalizedMessage());

				new ConnectException(errorBuilder.toString());
			}
		}
		return this.schema;
	}

	@Override
	public Configuration getConfiguration() {
		LOGGER.info("Fetch configuration");
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

	@Override
	public void delete(ObjectClass objectClass, Uid uid, OperationOptions options) {
		LOGGER.warn("Operation not supported");

	}

}
