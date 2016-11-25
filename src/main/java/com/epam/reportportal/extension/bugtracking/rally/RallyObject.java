/*
 * Copyright 2016 EPAM Systems
 * 
 * 
 * This file is part of EPAM Report Portal.
 * https://github.com/reportportal/service-rally
 * 
 * Report Portal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Report Portal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Report Portal.  If not, see <http://www.gnu.org/licenses/>.
 */ 
package com.epam.reportportal.extension.bugtracking.rally;

import com.google.gson.annotations.SerializedName;

/**
 * @author Dzmitry_Kavalets
 */
class RallyObject {

	@SerializedName("_rallyAPIMajor")
	private String rallyApiMajor;

	@SerializedName("_rallyAPIMinor")
	private String rallyApiMinor;

	@SerializedName("_ref")
	private String ref;

	@SerializedName("_refObjectUUID")
	private String refObjectUUID;

	@SerializedName("_objectVersion")
	private String objectVersion;

	@SerializedName("_refObjectName")
	private String refObjectName;

	@SerializedName("ObjectID")
	private long objectId;

	@SerializedName("_type")
	private String type;

	public String getRallyApiMajor() {
		return rallyApiMajor;
	}

	public void setRallyApiMajor(String rallyApiMajor) {
		this.rallyApiMajor = rallyApiMajor;
	}

	public String getRallyApiMinor() {
		return rallyApiMinor;
	}

	public void setRallyApiMinor(String rallyApiMinor) {
		this.rallyApiMinor = rallyApiMinor;
	}

	public String getRef() {
		return ref;
	}

	public void setRef(String ref) {
		this.ref = ref;
	}

	public String getRefObjectUUID() {
		return refObjectUUID;
	}

	public void setRefObjectUUID(String refObjectUUID) {
		this.refObjectUUID = refObjectUUID;
	}

	public String getObjectVersion() {
		return objectVersion;
	}

	public void setObjectVersion(String objectVersion) {
		this.objectVersion = objectVersion;
	}

	public String getRefObjectName() {
		return refObjectName;
	}

	public void setRefObjectName(String refObjectName) {
		this.refObjectName = refObjectName;
	}

	public long getObjectId() {
		return objectId;
	}

	public void setObjectId(long objectId) {
		this.objectId = objectId;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
}