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

import org.identityconnectors.common.StringUtil;
/**
 * @author Matus
 */
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.ConfigurationProperty;
import org.identityconnectors.framework.spi.StatefulConfiguration;

public class HcmConnectorConfiguration extends AbstractConfiguration implements StatefulConfiguration {

	private String file;
	private String iterations;
	private String uid;
	private String name;

	@ConfigurationProperty(order = 1, displayMessageKey = "file.display", helpMessageKey = "file.help", required = true, confidential = false)
	public String getFilePath() {
		return file;
	}

	public void setFilePath(String filePath) {
		file = filePath;
	}

	@ConfigurationProperty(order = 2, displayMessageKey = "iterations.display", helpMessageKey = "iterations.help", required = true, confidential = false)
	public String getIterations() {
		return iterations;
	}

	public void setIterations(String iterationNo) {
		iterations = iterationNo;
	}

	@ConfigurationProperty(order = 3, displayMessageKey = "uid.display", helpMessageKey = "uid.help", required = true, confidential = false)
	public String getUidAttribute() {
		return uid;
	}

	public void setUidAttribute(String uidAttribute) {
		uid = uidAttribute;
	}

	@ConfigurationProperty(order = 4, displayMessageKey = "name.display", helpMessageKey = "name.help", required = true, confidential = false)
	public String getPrimaryId() {
		return name;
	}

	public void setPrimaryId(String primaryId) {
		name = primaryId;
	}

	@Override
	public void release() {
		this.file = null;
		this.iterations = null;
		this.uid = null;
		this.name = null;
	}

	@Override
	public void validate() {
		if (StringUtil.isBlank(file)) {

			throw new IllegalArgumentException("File path cannot be empty.");

		}
		if (StringUtil.isBlank(iterations)) {

			throw new IllegalArgumentException("Parser iterations cannot be empty.");
		}
		if (StringUtil.isBlank(uid)) {

			throw new IllegalArgumentException("Unique identifier name cannot be empty.");
		}

		if (StringUtil.isBlank(name)) {

			throw new IllegalArgumentException("Name attribute cannot be empty.");
		}

	}

}
