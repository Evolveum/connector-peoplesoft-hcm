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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectorException;

public class XMLParser {

	private static final Log LOGGER = Log.getLog(XMLParser.class);

	public Map<String, String> parseXmlToMap(String xml) {
		Map<String, String> assigmentMap = new HashMap<String, String>();

		if (xml != null && !xml.isEmpty()) {
			try {

				// xml = URLEncoder.encode(xml, "UTF-8");
				XMLInputFactory factory = XMLInputFactory.newInstance();
				String value = "";
				String startName = "";
				InputStream stream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
				boolean isRootElement = true;

				XMLEventReader eventReader = factory.createXMLEventReader(stream);
				while (eventReader.hasNext()) {

					XMLEvent event = eventReader.nextEvent();

					Integer code = event.getEventType();

					if (code == XMLStreamConstants.START_ELEMENT) {

						StartElement startElement = event.asStartElement();
						startName = startElement.getName().getLocalPart();

						if (!isRootElement) {
							assigmentMap.put(startName, null);
						} else {
							isRootElement = false;
						}
					} else if (code == XMLStreamConstants.CHARACTERS) {

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

						}
					} else if (code == XMLStreamConstants.END_ELEMENT) {

						EndElement endElement = event.asEndElement();
						String endName = endElement.getName().getLocalPart();

						if (endName.equals(startName)) {
							if (value != null) {

								assigmentMap.put(endName, value);

								value = null;
							}
						} else {
							LOGGER.info("No value between xml tags, tag name : {0}", endName);
						}

					} else if (code == XMLStreamConstants.END_DOCUMENT) {
						isRootElement = true;
					}
				}

			} catch (XMLStreamException e) {

				StringBuilder error = new StringBuilder("Xml stream exception wile parsing xml string")
						.append(e.getLocalizedMessage());
				throw new ConnectorException(error.toString());
			}
		}

		return assigmentMap;
	}

}
