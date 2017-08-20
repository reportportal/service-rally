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

import com.epam.reportportal.commons.template.TemplateEngine;
import com.epam.reportportal.extension.bugtracking.ExternalSystemStrategy;
import com.epam.ta.reportportal.database.BinaryData;
import com.epam.ta.reportportal.database.DataStorage;
import com.epam.ta.reportportal.database.dao.LogRepository;
import com.epam.ta.reportportal.database.dao.TestItemRepository;
import com.epam.ta.reportportal.database.entity.ExternalSystem;
import com.epam.ta.reportportal.database.entity.Log;
import com.epam.ta.reportportal.database.entity.item.TestItem;
import com.epam.ta.reportportal.exception.ReportPortalException;
import com.epam.ta.reportportal.ws.model.externalsystem.AllowedValue;
import com.epam.ta.reportportal.ws.model.externalsystem.PostFormField;
import com.epam.ta.reportportal.ws.model.externalsystem.PostTicketRQ;
import com.epam.ta.reportportal.ws.model.externalsystem.Ticket;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.rallydev.rest.RallyRestApi;
import com.rallydev.rest.request.CreateRequest;
import com.rallydev.rest.request.QueryRequest;
import com.rallydev.rest.request.UpdateRequest;
import com.rallydev.rest.response.CreateResponse;
import com.rallydev.rest.response.QueryResponse;
import com.rallydev.rest.response.Response;
import com.rallydev.rest.response.UpdateResponse;
import com.rallydev.rest.util.Fetch;
import com.rallydev.rest.util.QueryFilter;
import com.rallydev.rest.util.Ref;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.epam.reportportal.extension.bugtracking.rally.RallyConstants.*;

/**
 * @author Dzmitry_Kavalets
 */
public abstract class RallyStrategy implements ExternalSystemStrategy {

	private static final Logger LOGGER = LoggerFactory.getLogger(RallyStrategy.class);
	private static final String BUG_TEMPLATE_PATH = "bug_template.ftl";

	@Autowired
	private LogRepository logRepository;

	@Autowired
	private DataStorage dataStorage;

	@Autowired
	private TestItemRepository testItemRepository;

	@Autowired
	private TemplateEngine templateEngine;

	private Gson gson;

	public abstract RallyRestApi getRallyRestApi(URI server, String apiKey);

	public RallyStrategy() {
		gson = new Gson();
	}

	@Override
	public boolean checkConnection(ExternalSystem externalSystem) {
		try (RallyRestApi rallyRestApi = getRallyRestApi(new URI(externalSystem.getUrl()), externalSystem.getAccessKey())) {
			QueryRequest defectRequest = new QueryRequest(DEFECT);
			defectRequest.setPageSize(1);
			defectRequest.setLimit(1);
			return rallyRestApi.query(defectRequest).wasSuccessful();
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			return false;
		}
	}

	@Override
	public Optional<Ticket> getTicket(String id, ExternalSystem externalSystem) {
		Ticket ticket;
		try (RallyRestApi restApi = getRallyRestApi(new URI(externalSystem.getUrl()), externalSystem.getAccessKey())) {
			Optional<Defect> defectOptional = findDefect(restApi, id);
			if (!defectOptional.isPresent()) {
				return Optional.empty();
			}
			ticket = buildTicket(defectOptional.get(), externalSystem);
		} catch (Exception e) {
			LOGGER.error("Unable to load ticket: " + e.getMessage(), e);
			throw new ReportPortalException("Unable to load ticket: " + e.getMessage(), e);
		}
		return Optional.of(ticket);
	}

	@Override
	public Ticket submitTicket(PostTicketRQ ticketRQ, ExternalSystem externalSystem) {
		try (RallyRestApi restApi = getRallyRestApi(new URI(externalSystem.getUrl()), ticketRQ.getToken())) {
			List<LogEntry> itemLogs = loadTestItemLogs(ticketRQ);
			Defect newDefect = postDefect(restApi, ticketRQ, externalSystem);
			String description = newDefect.getDescription();
			Map<String, String> attachments = new HashMap<>();
			for (LogEntry logEntry : itemLogs) {
				if (logEntry.getBinaryDataId() != null)
					attachments.put(logEntry.getBinaryDataId(),
							String.valueOf(postImage(newDefect.getRef(), logEntry, restApi).getObjectId()));
			}
			for (Map.Entry<String, String> binaryDataEntry : attachments.entrySet()) {
				description = description.replace(binaryDataEntry.getKey(),
						"/slm/attachment/" + binaryDataEntry.getValue() + "/" + binaryDataEntry.getKey());
			}
			updateDescription(description, newDefect, restApi);
			return buildTicket(newDefect, externalSystem);
		} catch (Exception e) {
			LOGGER.error("Unable to submit ticket: " + e.getMessage(), e);
			throw new ReportPortalException("Unable to submit ticket: " + e.getMessage(), e);
		}
	}

	@Override
	public List<String> getIssueTypes(ExternalSystem system) {
		return Collections.singletonList(DEFECT);
	}

	@Override
	public List<PostFormField> getTicketFields(String issueType, ExternalSystem externalSystem) {

		try (RallyRestApi restApi = getRallyRestApi(new URI(externalSystem.getUrl()), externalSystem.getAccessKey())) {
			ArrayList<PostFormField> fields = new ArrayList<>();
			List<AttributeDefinition> attributeDefinitions = findDefectAttributeDefinitions(restApi);
			for (AttributeDefinition attributeDefinition : attributeDefinitions) {
				if (!attributeDefinition.isReadOnly()) {
					PostFormField postFormField = new PostFormField();
					// load predefined values
					if (attributeDefinition.getAllowedValue().getCount() > 0) {
						List<AllowedValue> definedValues = new ArrayList<>();
						List<AllowedAttributeValue> allowedAttributeValues = findAllowedAttributeValues(restApi, attributeDefinition);
						for (AllowedAttributeValue allowedAttributeValue : allowedAttributeValues) {
							AllowedValue allowedValue = new AllowedValue();
							if (allowedAttributeValue.getStringValue() != null && !allowedAttributeValue.getStringValue().isEmpty()) {
								allowedValue.setValueName(allowedAttributeValue.getStringValue());
								if (!"null".equals(allowedAttributeValue.getRef())) {
									allowedValue.setValueId(Ref.getRelativeRef(allowedAttributeValue.getRef()));
								}
								definedValues.add(allowedValue);
							}
						}
						postFormField.setDefinedValues(definedValues);
					}
					postFormField.setId(attributeDefinition.getElementName());
					postFormField.setFieldName(attributeDefinition.getName());
					postFormField.setIsRequired(attributeDefinition.isRequired());
					postFormField.setFieldType(attributeDefinition.getType());
					fields.add(postFormField);
				}
			}
			return fields;
		} catch (IOException | URISyntaxException e) {
			throw new ReportPortalException("Unable to load ticket fields: " + e.getMessage(), e);
		}
	}

	private List<AllowedAttributeValue> findAllowedAttributeValues(RallyRestApi restApi, AttributeDefinition attributeDefinition)
			throws IOException {
		QueryRequest allowedValuesRequest = new QueryRequest((JsonObject) gson.toJsonTree(attributeDefinition.getAllowedValue()));
		allowedValuesRequest.setFetch(new Fetch(STRING_VALUE));
		QueryResponse allowedValuesResponse = restApi.query(allowedValuesRequest);
		return gson.fromJson(allowedValuesResponse.getResults(), new TypeToken<List<AllowedAttributeValue>>() {
		}.getType());
	}

	private List<AttributeDefinition> findDefectAttributeDefinitions(RallyRestApi restApi) throws IOException {
		QueryRequest typeDefRequest = new QueryRequest(TYPE_DEFINITION);
		typeDefRequest.setFetch(new Fetch(OBJECT_ID, ATTRIBUTES));
		typeDefRequest.setQueryFilter(new QueryFilter(NAME, "=", DEFECT));
		QueryResponse typeDefQueryResponse = restApi.query(typeDefRequest);
		JsonObject typeDefJsonObject = typeDefQueryResponse.getResults().get(0).getAsJsonObject();
		QueryRequest attributeRequest = new QueryRequest(
				(JsonObject) gson.toJsonTree(gson.fromJson(typeDefJsonObject, TypeDefinition.class).getAttributeDefinition()));
		attributeRequest.setFetch(new Fetch(ALLOWED_VALUES, ELEMENT_NAME, NAME, REQUIRED, TYPE, OBJECT_ID, READ_ONLY));
		QueryResponse attributesQueryResponse = restApi.query(attributeRequest);
		return gson.fromJson(attributesQueryResponse.getResults(), new TypeToken<List<AttributeDefinition>>() {
		}.getType());
	}

	private Optional<Defect> findDefect(RallyRestApi restApi, String ticketId) throws IOException {
		QueryRequest defectRequest = new QueryRequest(DEFECT);
		defectRequest.setQueryFilter(new QueryFilter(FORMATTED_ID, "=", ticketId));
		QueryResponse query = restApi.query(defectRequest);
		if (!query.wasSuccessful())
			return Optional.empty();
		List<Defect> defects = gson.fromJson(query.getResults(), new TypeToken<List<Defect>>() {
		}.getType());
		return defects.stream().findAny();
	}

	private List<LogEntry> loadTestItemLogs(final PostTicketRQ ticketRQ) {
		List<Log> logs = ticketRQ.getBackLinks().size() == 1
				? logRepository.findByTestItemRef(ticketRQ.getTestItemId(),
						ticketRQ.getNumberOfLogs() == 0 ? 50 : ticketRQ.getNumberOfLogs(), ticketRQ.getIsIncludeScreenshots())
				: new ArrayList<>();
		return logs.stream().map(log -> {
			final LogEntry logEntry = new LogEntry();
			logEntry.setLogId(log.getId());
			logEntry.setBinaryDataId(log.getBinaryContent() == null ? null : log.getBinaryContent().getBinaryDataId());
			logEntry.setMessage(ticketRQ.getIsIncludeLogs() ? log.getLogMsg() : null);
			return logEntry;
		}).collect(Collectors.toList());
	}

	private String createDescription(PostTicketRQ ticketRQ, List<LogEntry> itemLogs) {
		TestItem testItem = testItemRepository.findOne(ticketRQ.getTestItemId());
		HashMap<Object, Object> templateData = new HashMap<>();
		if (ticketRQ.getIsIncludeComments()) {
			templateData.put("comments", testItem.getIssue().getIssueDescription());
		}
		if (ticketRQ.getBackLinks() != null) {
			templateData.put("backLinks", ticketRQ.getBackLinks());
		}
		if (itemLogs != null && (ticketRQ.getIsIncludeLogs() || ticketRQ.getIsIncludeScreenshots())) {
			templateData.put("logs", itemLogs);
		}
		return templateEngine.merge(BUG_TEMPLATE_PATH, templateData);
	}

	private Defect postDefect(RallyRestApi restApi, PostTicketRQ ticketRQ, ExternalSystem externalSystem) throws IOException {
		JsonObject newDefect = new JsonObject();
		List<PostFormField> fields = ticketRQ.getFields();
		List<PostFormField> savedFields = externalSystem.getFields();
		for (PostFormField field : fields) {
			// skip empty fields
			if (!field.getValue().isEmpty()) {
				String value = field.getValue().get(0);
				for (PostFormField savedField : savedFields) {
					if (savedField.getFieldName().equalsIgnoreCase(field.getFieldName())) {
						List<AllowedValue> definedValues = savedField.getDefinedValues();
						if (definedValues != null)
							for (AllowedValue definedValue : definedValues) {
								if (definedValue.getValueName().equals(field.getValue().get(0)) && definedValue.getValueId() != null) {
									value = definedValue.getValueId();
								}
							}
					}
				}
				newDefect.addProperty(field.getId(), value);
			}
		}
		List<LogEntry> itemLogs = loadTestItemLogs(ticketRQ);
		String description = createDescription(ticketRQ, itemLogs);
		newDefect.addProperty(DESCRIPTION,
				newDefect.get(DESCRIPTION) != null ? (newDefect.get(DESCRIPTION).getAsString() + "<br>" + description) : description);
		CreateRequest createRequest = new CreateRequest(DEFECT, newDefect);
		try {
			CreateResponse createResponse = restApi.create(createRequest);
			checkResponse(createResponse);
			return gson.fromJson(createResponse.getObject(), Defect.class);
		}  catch (Exception e){
			LOGGER.error("Errored request: {}", gson.toJson(createRequest));
			throw e;
		}
	}

	private void checkResponse(Response response) {
		if (response.getErrors().length > 0) {
			throw new ReportPortalException(
					"Error during interacting with Rally: " + Stream.of(response.getErrors()).collect(Collectors.joining(" ")));
		}
	}

	private RallyObject postImage(String itemRef, LogEntry log, RallyRestApi restApi) throws IOException {
		String binaryId = log.getBinaryDataId();
		BinaryData binaryData = dataStorage.fetchData(binaryId);
		byte[] bytes = ByteStreams.toByteArray(binaryData.getInputStream());
		JsonObject attach = new JsonObject();
		attach.addProperty(CONTENT, Base64.encodeBase64String(bytes));
		CreateResponse attachmentContentResponse = restApi.create(new CreateRequest(ATTACHMENT_CONTENT, attach));
		JsonObject attachment = new JsonObject();
		attachment.addProperty(ARTIFACT, itemRef);
		attachment.addProperty(CONTENT, attachmentContentResponse.getObject().get(REF).getAsString());
		attachment.addProperty(NAME, binaryId);
		attachment.addProperty(DESCRIPTION, binaryId);
		attachment.addProperty(CONTENT_TYPE, binaryData.getContentType());
		attachment.addProperty(SIZE, bytes.length);
		CreateRequest attachmentCreateRequest = new CreateRequest(ATTACHMENT, attachment);
		CreateResponse attachmentResponse = restApi.create(attachmentCreateRequest);
		checkResponse(attachmentResponse);
		return gson.fromJson(attachmentResponse.getObject(), RallyObject.class);
	}

	private Defect updateDescription(String description, Defect defect, RallyRestApi restApi) throws IOException {
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty(DESCRIPTION, description);
		UpdateRequest updateRequest = new UpdateRequest(defect.getRef(), jsonObject);
		UpdateResponse update = restApi.update(updateRequest);
		checkResponse(update);
		return gson.fromJson(update.getObject(), Defect.class);
	}

	private Ticket buildTicket(Defect defect, ExternalSystem externalSystem) {
		Ticket ticket = new Ticket();
		String link = externalSystem.getUrl() + "/#/" + Ref.getOidFromRef(defect.getProject().getRef()) + "/detail/defect/" + defect
				.getObjectId();
		ticket.setId(defect.getFormattedId());
		ticket.setSummary(defect.getName());
		ticket.setTicketUrl(link);
		ticket.setStatus(defect.getState());
		return ticket;
	}

	public static class LogEntry {
		private String logId;
		private String binaryDataId;
		private String message;

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

		public String getLogId() {
			return logId;
		}

		public void setLogId(String logId) {
			this.logId = logId;
		}

		public String getBinaryDataId() {
			return binaryDataId;
		}

		public void setBinaryDataId(String binaryDataId) {
			this.binaryDataId = binaryDataId;
		}

		public boolean isBinaryDataExists() {
			return binaryDataId != null;
		}

		@Override
		public String toString() {
			return "LogEntry{" + "logId='" + logId + '\'' + ", binaryDataId='" + binaryDataId + '\'' + ", message='" + message + '\'' + '}';
		}
	}

}
