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

	@ConfigurationProperty(order = 1, displayMessageKey = "File path", groupMessageKey = "basic.group", helpMessageKey = "Please provide the path to the file.", required = true, confidential = false)
	public String getFilePath() {
		return FILE;
	}

	public void setFilePath(String file) {
		FILE = file;
	}

	@ConfigurationProperty(order = 2, displayMessageKey = "Parser iterations", groupMessageKey = "basic.group", helpMessageKey = "Please provide a number value (e.g. 50). The higher the value the more precise the schema.", required = true, confidential = false)
	public String getIterations() {
		return ITERATIONS;
	}

	public void setIterations(String iterations) {
		ITERATIONS = iterations;
	}

	@ConfigurationProperty(order = 3, displayMessageKey = "Unique identifier name", groupMessageKey = "basic.group", helpMessageKey = "Please provide the unique identifier attribute name (e.g. UID).", required = true, confidential = false)
	public String getUidAttribute() {
		return UIDATTRIBUTE;
	}

	public void setUidAttribute(String uidAttribute) {
		UIDATTRIBUTE = uidAttribute;
	}

	@ConfigurationProperty(order = 4, displayMessageKey = "Name attribute", groupMessageKey = "basic.group", helpMessageKey = "Please provide the attribute representing the name of an Employee.", required = true, confidential = false)
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
