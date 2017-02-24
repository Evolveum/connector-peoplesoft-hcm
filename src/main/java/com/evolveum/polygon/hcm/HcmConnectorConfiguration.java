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

import org.identityconnectors.common.StringUtil;
/**
 * @author Matus
 */
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.ConfigurationProperty;
import org.identityconnectors.framework.spi.StatefulConfiguration;

public class HcmConnectorConfiguration extends AbstractConfiguration implements StatefulConfiguration {

	private String FILE;
	private String ITERATIONS;
	private String UIDATTRIBUTE;
	private String PRIMARIID;

	@ConfigurationProperty(order = 1, displayMessageKey = "file.display", groupMessageKey = "basic.group", helpMessageKey = "file.help", required = true, confidential = false)
	public String getFilePath() {
		return FILE;
	}

	public void setFilePath(String file) {
		FILE = file;
	}

	@ConfigurationProperty(order = 2, displayMessageKey = "iterations.display", groupMessageKey = "basic.group", helpMessageKey = "iterations.help", required = true, confidential = false)
	public String getIterations() {
		return ITERATIONS;
	}

	public void setIterations(String iterations) {
		ITERATIONS = iterations;
	}

	@ConfigurationProperty(order = 3, displayMessageKey = "uid.display", groupMessageKey = "basic.group", helpMessageKey = "uid.help", required = true, confidential = false)
	public String getUidAttribute() {
		return UIDATTRIBUTE;
	}

	public void setUidAttribute(String uidAttribute) {
		UIDATTRIBUTE = uidAttribute;
	}

	@ConfigurationProperty(order = 4, displayMessageKey = "name.display", groupMessageKey = "basic.group", helpMessageKey = "name.help", required = true, confidential = false)
	public String getPrimaryId() {
		return PRIMARIID;
	}

	public void setPrimaryId(String primaryId) {
		PRIMARIID = primaryId;
	}

	@Override
	public void release() {
		this.FILE = null;
		this.ITERATIONS = null;
		this.UIDATTRIBUTE = null;
		this.PRIMARIID = null;
	}

	@Override
	public void validate() {
		if (StringUtil.isBlank(FILE)) {

			throw new IllegalArgumentException("File path cannot be empty.");

		}
		if (StringUtil.isBlank(ITERATIONS)) {

			throw new IllegalArgumentException("Parser iterations cannot be empty.");
		}
		if (StringUtil.isBlank(UIDATTRIBUTE)) {

			throw new IllegalArgumentException("Unique identifier name cannot be empty.");
		}

		if (StringUtil.isBlank(PRIMARIID)) {

			throw new IllegalArgumentException("Name attribute cannot be empty.");
		}

	}

}
