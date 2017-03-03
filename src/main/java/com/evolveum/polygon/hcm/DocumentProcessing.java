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

package com.evolveum.polygon.hcm;

import java.io.FileNotFoundException;
import java.io.FileReader;
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
import org.identityconnectors.framework.common.exceptions.ConnectorIOException;
import org.identityconnectors.framework.common.objects.OperationOptions;
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

	public static final String OPTION_ATTRIBUTES_TO_GET_ = "ATTRS_TO_GET";

	private static Boolean elementIsEmployeeData = false;
	private static Boolean elementIsMultiValued = false;
	// private static Boolean assigmentIsActive = false;

	private Map<String, Object> attributeMap = new HashMap<String, Object>();

	static Map<String, String> multiValuedAttributeBuffer = new HashMap<String, String>();
	private static List<String> multiValuedAttributesList = new ArrayList<String>();

	private List<String> attrsToGet = new ArrayList<>();

	public Map<String, Object> parseXMLData(HcmConnectorConfiguration conf, ResultsHandler handler,
			Map<String, Object> schemaAttributeMap, Filter query) {

		XMLInputFactory factory = XMLInputFactory.newInstance();
		try {

			String uidAttributeName = conf.getUidAttribute();
			String primariId = conf.getPrimaryId();
			String startName = "";
			String value = null;

			StringBuilder assignmentXMLBuilder = null;

			List<String> builderList = new ArrayList<String>();

			Integer nOfIterations = 0;
			Boolean isSubjectToQuery = false;
			Boolean isAssigment = false;
			Boolean evaluateAttr = true;
			Boolean specificAttributeQuery = false;

			XMLEventReader eventReader = factory.createXMLEventReader(new FileReader(conf.getFilePath()));
			List<String> dictionary = populateDictionary(FIRSTFLAG);

			if (!attrsToGet.isEmpty()) {

				attrsToGet.add(uidAttributeName);
				attrsToGet.add(primariId);
				specificAttributeQuery = true;
				evaluateAttr = false;
				LOGGER.ok("The uid and primary id were added to the queried attribute list");

				schemaAttributeMap = modifySchemaAttributeMap(schemaAttributeMap);
			}

			while (eventReader.hasNext()) {

				XMLEvent event = eventReader.nextEvent();

				Integer code = event.getEventType();

				if (code == XMLStreamConstants.START_ELEMENT) {

					StartElement startElement = event.asStartElement();
					startName = startElement.getName().getLocalPart();

					if (!evaluateAttr && attrsToGet.contains(startName)) {

						evaluateAttr = true;
					}

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
					} else if (evaluateAttr) {

						if (!isAssigment) {
							if (!ASSIGNMENTTAG.equals(startName)) {

							} else {
								assignmentXMLBuilder = new StringBuilder();
								isAssigment = true;
							}
						} else {

							builderList = processAssignment(startName, null, START, builderList);
						}

						if (multiValuedAttributesList.contains(startName)) {

							elementIsMultiValued = true;
						}

					}

				} else if (elementIsEmployeeData) {

					if (code == XMLStreamConstants.CHARACTERS && evaluateAttr) {

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
							// value = StringEscapeUtils.escapeXml10(value);
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

						if (endName.equals(EMPLOYEES)) {

							attributeMap = handleEmployeeData(attributeMap, schemaAttributeMap, handler,
									uidAttributeName, primariId);

							elementIsEmployeeData = false;

						} else if (evaluateAttr) {

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
										builderList = processAssignment(endName, value, VALUE, builderList);

										builderList = processAssignment(endName, null, END, builderList);
									}
									// LOGGER.info("Attribute name: {0} and the
									// Attribute value: {1}", endName, value);
									value = null;
								}
							} else {
								if (endName.equals(ASSIGNMENTTAG)) {

									builderList = processAssignment(endName, null, CLOSE, builderList);

									// if (assigmentIsActive) {

									for (String records : builderList) {
										assignmentXMLBuilder.append(records);

									}

									attributeMap.put(ASSIGNMENTTAG, assignmentXMLBuilder.toString());
									// } else {
									// }

									builderList = new ArrayList<String>();
									// assigmentIsActive = false;
									isAssigment = false;

								} else if (multiValuedAttributesList.contains(endName)) {
									processMultiValuedAttributes(multiValuedAttributeBuffer);
								}
							}

						}
						if (specificAttributeQuery && evaluateAttr) {
							evaluateAttr = false;
						}
					}
				} else if (code == XMLStreamConstants.END_DOCUMENT) {
					handleBufferedData(uidAttributeName, primariId, handler);
				}
			}

		} catch (FileNotFoundException e) {
			StringBuilder errorBuilder = new StringBuilder("File not found at the specified path.")
					.append(e.getLocalizedMessage());
			LOGGER.error("File not found at the specified path: {0}", e);
			throw new ConnectorIOException(errorBuilder.toString());
		} catch (XMLStreamException e) {

			LOGGER.error("Unexpected processing error while parsing the .xml document : {0}", e);

			StringBuilder errorBuilder = new StringBuilder(
					"Unexpected processing error while parsing the .xml document. ").append(e.getLocalizedMessage());

			throw new ConnectorIOException(errorBuilder.toString());
		}
		return attributeMap;

	}

	private Map<String, Object> modifySchemaAttributeMap(Map<String, Object> schemaAttributeMap) {
		schemaAttributeMap.clear();

		for (String attribute : attrsToGet) {
			schemaAttributeMap.put(attribute, "");
		}
		return schemaAttributeMap;
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
		LOGGER.ok("The dictionary flag which is about to be applied: {0} ", flag);
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

	public List<String> processAssignment(String attributeName, String attributeValue, String elementType,
			List<String> builderList) {

		if (START.equals(elementType)) {
			builderList.add(OABRACET);
			builderList.add(attributeName);
			builderList.add(SLASH);
			builderList.add(CABRACET);
		} else if (END.equals(elementType)) {
			builderList.remove(builderList.size() - 3);

			builderList.add(OABRACET);
			builderList.add(SLASH);
			builderList.add(attributeName);
			builderList.add(CABRACET);

		} else if (VALUE.equals(elementType)) {

			// if ("Assignment_Status_Type".equals(attributeName)) {
			//
			// if ("ACTIVE".equals(attributeValue)) {
			//
			// assigmentIsActive = true;
			// }
			//
			// }
			builderList.add(attributeValue);

		} else if (CLOSE.equals(elementType)) {
			StringBuilder buildEndTag = new StringBuilder(OABRACET).append(ASSIGNMENTTAG).append(CABRACET);

			builderList.add(0, buildEndTag.toString());
			buildEndTag.insert(1, SLASH);
			builderList.add(buildEndTag.toString());
		}
		return builderList;

	}

	@Override
	public void evaluateOptions(OperationOptions options) {
		LOGGER.ok("Evaluating options");

		if (options != null) {
			Map<String, Object> returnedOptions = options.getOptions();
			for (String optionName : returnedOptions.keySet()) {

				if (OPTION_ATTRIBUTES_TO_GET_.equals(optionName)) {

					String[] atg = (String[]) returnedOptions.get(optionName);

					StringBuilder queriedAttributes = new StringBuilder();

					for (int i = 0; i < atg.length; i++) {
						attrsToGet.add(atg[i]);
						queriedAttributes.append(atg[i]);
					}

					LOGGER.ok("The queried attributes: {0}", attrsToGet.toString());

				}
			}

		}

	}

}
