package com.evolveum.polygon.hcm;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.lang3.StringEscapeUtils;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.filter.Filter;


public class DocumentProcessing implements HandlingStrategy {

	private static final Log LOGGER = Log.getLog(DocumentProcessing.class);

	private static final String EMPLOYEES = "Employees";

	private static final String TYPE = "Type";

	private static final String OABRACET = "<";
	private static final String CABRACET = ">";
	private static final String SLASH = "/";
	private static final String START = "startElement";
	private static final String END = "endElement";
	private static final String VALUE = "value";
	private static final String CLOSE = "close";
	private static Boolean elementIsEmployeeData = false;
	private static Boolean elementIsMultiValued = false;

	private static Map<String, Object> attributeMap = new HashMap<String, Object>();

	static Map<String, String> multiValuedAttributeBuffer = new HashMap<String, String>();
	static Map<String, String> specialCharacters = new HashMap<String, String>();
	private static List<String> multiValuedAttributesList = new ArrayList<String>();

	static {
		
		specialCharacters.put("'", "&apos;");
		specialCharacters.put("\"", "&quot;");
		specialCharacters.put(">", "&gt;");
		specialCharacters.put("&", "&amp;");
		specialCharacters.put("<", "&apos;");
		
		multiValuedAttributesList.add("e-mail_address_record");
		multiValuedAttributesList.add("phones_record");
	}

	public Map<String, Object> parseXMLData(HcmConnectorConfiguration conf, ResultsHandler handler,
			Map<String, Object> schemaAttributeMap, Filter query) throws ConnectException {

		XMLInputFactory factory = XMLInputFactory.newInstance();
		try {

			String uidAttributeName = conf.getUidAttribute();
			String primariId = conf.getPrimaryId();
			String startName = "";
			String value = null;

			StringBuilder assigmentXMLBuilder = null;
			
			List<String> builderList= new ArrayList<String>();

			Integer nOfIterations = 0;
			Boolean isSubjectToQuery = false;
			Boolean isAssigment = false;

			XMLEventReader eventReader = factory.createXMLEventReader(new FileReader(conf.getFilePath()));
			List<String> dictionary = populateDictionary(FIRSTFLAG);

			while (eventReader.hasNext()) {

				XMLEvent event = eventReader.nextEvent();

				Integer code = event.getEventType();

				if (code == XMLStreamConstants.START_ELEMENT) {

					StartElement startElement = event.asStartElement();
					startName = startElement.getName().getLocalPart();

					if (!elementIsEmployeeData) {

						if (startName.equals(EMPLOYEES)) {
							if (dictionary.contains(nOfIterations.toString())) {
								break;
							} else {
								startName = "";
								elementIsEmployeeData = true;
								nOfIterations++;
							}
						}
					} else {

						if (!isAssigment) {
							if (!"assignments_record".equals(startName)) {

							} else {
								assigmentXMLBuilder = new StringBuilder();
								isAssigment = true;
							}
						} else {

							processAssigment(startName, null, START,builderList, assigmentXMLBuilder);
						}

						if (multiValuedAttributesList.contains(startName)) {

							elementIsMultiValued = true;
						}

					}

				} else if (elementIsEmployeeData) {

					if (code == XMLStreamConstants.CHARACTERS) {

						Characters characters = event.asCharacters();

						if (!characters.isWhiteSpace()) {

							StringBuilder valueBuilder;
							if (value != null) {
								valueBuilder = new StringBuilder(value).append("")
										.append(characters.getData().toString());
							} else {
								valueBuilder = new StringBuilder(characters.getData().toString());
							}
							value = valueBuilder.toString();
							//value = StringEscapeUtils.escapeXml10(value);
							// LOGGER.info("The attribute value for: {0} is
							// {1}", startName, value);
						}
					} else if (code == XMLStreamConstants.END_ELEMENT) {

						EndElement endElement = event.asEndElement();
						String endName = endElement.getName().getLocalPart();

						isSubjectToQuery = checkFilter(endName, value, query, uidAttributeName);

						if (!isSubjectToQuery) {
							attributeMap.clear();
							elementIsEmployeeData = false;
							value = null;

							endName = EMPLOYEES;
						}

						if (endName.equals(startName)) {
							if (value != null) {

								if (!isAssigment) {
									if (!elementIsMultiValued) {
										attributeMap.put(startName, value);
									} else {

										multiValuedAttributeBuffer.put(startName, value);
									}
								} else {
								
									value = StringEscapeUtils.escapeXml10(value);
									processAssigment(null, value, VALUE,builderList, assigmentXMLBuilder);

									processAssigment(endName, null, END,builderList, assigmentXMLBuilder);
								}
								// LOGGER.info("Attribute name: {0} and the
								// Attribute value: {1}", endName, value);
								value = null;
							}
						} else {
							if (endName.equals(EMPLOYEES)) {

								attributeMap = handleEmployeeData(attributeMap, schemaAttributeMap, handler,
										uidAttributeName, primariId);
								elementIsEmployeeData = false;

							} else if (endName.equals("assignments_record")) {
								
								processAssigment(endName, null, CLOSE,builderList, assigmentXMLBuilder);
								
								for(String records : builderList){
									assigmentXMLBuilder.append(records);
									
								}
								
								attributeMap.put("assignments_record", assigmentXMLBuilder.toString());
								builderList = new ArrayList<String>();
								isAssigment = false;

							} else if (multiValuedAttributesList.contains(endName)) {
								processMultiValuedAttributes(multiValuedAttributeBuffer);
							}
						}

					}
				} else if (code == XMLStreamConstants.END_DOCUMENT) {
					handleBufferedData(uidAttributeName, primariId, handler);
				}
			}

		} catch (FileNotFoundException e) {
			StringBuilder errorBuilder = new StringBuilder("File not found at the specified path.")
					.append(e.getLocalizedMessage());
			LOGGER.error("File not found at the specified paths: {0}", e);
			throw new ConnectException(errorBuilder.toString());
		} catch (XMLStreamException e) {

			LOGGER.error("Unexpected processing error while parsing the .xml document : {0}", e);

			StringBuilder errorBuilder = new StringBuilder(
					"Unexpected processing error while parsing the .xml document.").append(e.getLocalizedMessage());

			throw new ConnectException(errorBuilder.toString());
		}
		return attributeMap;

	}

	public String processMultiValuedAttributes(Map<String, String> multiValuedAttributeBuffer) {
		Map<String, String> renamedAttributes = new HashMap<String, String>();
		List<String> unchangedAttributes = new ArrayList<String>();
		Boolean typeValueWasSet = false;
		String typeValue = "";

		for (String attributeName : multiValuedAttributeBuffer.keySet()) {

			String[] nameParts = attributeName.split(DELIMITER);
			String lastPart = nameParts[nameParts.length - 1];
			if (TYPE.equals(lastPart)) {

				typeValue = multiValuedAttributeBuffer.get(attributeName);
				typeValueWasSet = true;
			} else {

				if (typeValueWasSet) {
					StringBuilder buildAttributeName = new StringBuilder(attributeName).append(DOT).append(typeValue);
					renamedAttributes.put(buildAttributeName.toString(), multiValuedAttributeBuffer.get(attributeName));
				} else {
					unchangedAttributes.add(attributeName);
				}

			}
		}

		if (!unchangedAttributes.isEmpty()) {
			for (String attributeName : unchangedAttributes) {
				StringBuilder buildAttributeName = new StringBuilder(attributeName).append(DOT).append(typeValue);
				renamedAttributes.put(buildAttributeName.toString(), multiValuedAttributeBuffer.get(attributeName));
			}

		}

		multiValuedAttributeBuffer.clear();
		attributeMap.putAll(renamedAttributes);
		elementIsMultiValued = false;
		return "";
	}

	@Override
	public List<String> populateDictionary(String flag) {

		List<String> dictionary = new ArrayList<String>();

		if (FIRSTFLAG.equals(flag)) {
			dictionary.add("");
		} else {

			LOGGER.warn("No such flag defined: {0}", flag);
		}
		return dictionary;

	}

	public Map<String, Object> handleEmployeeData(Map<String, Object> attributeMap,
			Map<String, Object> schemaAttributeMap, ResultsHandler handler, String uidAttributeName, String primariId) {

		attributeMap = new HashMap<String, Object>();
		return attributeMap;

	}

	@Override
	public Boolean checkFilter(String endName, String value, Filter filter, String uidAttributeName) {

		return true;

	}

	@Override
	public Map<String, Object> injectAttributes(Map<String, Object> attributeMap,
			Map<String, Object> schemaAttributeMap) {

		Map<String, Object> assembledMap = new HashMap<String, Object>();

		for (String schemaAttributeName : schemaAttributeMap.keySet()) {

			if (!attributeMap.containsKey(schemaAttributeName)) {

				assembledMap.put(schemaAttributeName, "");

			} else {
				assembledMap.put(schemaAttributeName, attributeMap.get(schemaAttributeName));
			}

		}
		return assembledMap;

	}

	public void handleBufferedData(String uidAttributeName, String primariId, ResultsHandler handler) {

	}

	public List<String> processAssigment(String attributeName, String attributeValue, String elementType,List<String> builderList,
			StringBuilder assigmentXMLBuilder) {

		if (START.equals(elementType)) {
			builderList.add(OABRACET);
			builderList.add(attributeName);
			builderList.add(SLASH);
			builderList.add(CABRACET);	
		} else if (END.equals(elementType)) {
		builderList.remove(builderList.size()-3);
		
		builderList.add(OABRACET);
		builderList.add(SLASH);
		builderList.add(attributeName);
		builderList.add(CABRACET);

		} else if (VALUE.equals(elementType)){
			
			builderList.add(attributeValue);
		
		} else if (CLOSE.equals(elementType)){
			builderList.add(0, "<assignments_record>");
			builderList.add("</assignments_record>");
		}
		return builderList;

	}

}
