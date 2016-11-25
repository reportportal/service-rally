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

import com.epam.ta.reportportal.database.entity.ExternalSystem;
import com.epam.ta.reportportal.ws.model.externalsystem.PostFormField;
import com.epam.ta.reportportal.ws.model.externalsystem.Ticket;
import com.rallydev.rest.RallyRestApi;
import com.rallydev.rest.request.CreateRequest;
import com.rallydev.rest.request.QueryRequest;
import com.rallydev.rest.request.UpdateRequest;
import com.rallydev.rest.response.CreateResponse;
import com.rallydev.rest.response.QueryResponse;
import com.rallydev.rest.response.UpdateResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RallyStrategyTest {

	private RallyStrategy rallyStrategy = rallyMock();
	private ExternalSystem externalSystem;

	@Before
	public void init() throws Throwable {
		externalSystem = new ExternalSystem();
		externalSystem.setUrl("https://rp.epam.com");
	}

	@Test
	public void connectionTest() {
		final boolean result = rallyStrategy.checkConnection(externalSystem);
		Assert.assertTrue(result);
	}

	@Test
	public void getTicketTest() {
		final Optional<Ticket> de1 = rallyStrategy.getTicket("DE1", externalSystem);
		Assert.assertTrue(de1.isPresent());
	}

	@Test
	public void getTicketFields() {
		final List<PostFormField> ticketFields = rallyStrategy.getTicketFields("", externalSystem);
		Assert.assertNotNull(ticketFields);
		Assert.assertFalse(ticketFields.isEmpty());
	}

	public RallyStrategy rallyMock() {
		return new RallyStrategy() {
			@Override
			public RallyRestApi getRallyRestApi(URI server, String apiKey) {
				final RallyRestApi rallyRestApi = mock(RallyRestApi.class);
				try {
					when(rallyRestApi.query(argThat(new ArgumentMatcher<QueryRequest>() {
						@Override
						public boolean matches(Object argument) {
							if (argument == null)
								return false;
							QueryRequest rq = (QueryRequest) argument;
							final String url = "/typedefinition.js?start=1&pagesize=200&fetch=ObjectID%2CAttributes&order=ObjectID&query=%28Name+%3D+Defect%29";
							return url.equals(rq.toUrl());
						}
					}))).thenReturn(new QueryResponse(
							new String(Files.readAllBytes(new File("src/test/resources/rally/TypeDefinition.json").toPath()))));
					when(rallyRestApi.query(argThat(new ArgumentMatcher<QueryRequest>() {
						@Override
						public boolean matches(Object argument) {
							if (argument == null)
								return false;
							QueryRequest rq = (QueryRequest) argument;
							String url = "/TypeDefinition/36321741237/Attributes?start=1&pagesize=200&fetch=AllowedValues%2CElementName%2CName%2CRequired%2CType%2CObjectID%2CReadOnly&order=ObjectID";
							return url.equals(rq.toUrl());

						}
					}))).thenReturn(new QueryResponse(
							new String(Files.readAllBytes(new File("src/test/resources/rally/AttributeDefinition.json").toPath()))));
					when(rallyRestApi.query(argThat(new ArgumentMatcher<QueryRequest>() {
						@Override
						public boolean matches(Object argument) {
							if (argument == null)
								return false;
							QueryRequest rq = (QueryRequest) argument;
							String url = "/AttributeDefinition/-12537/AllowedValues?start=1&pagesize=200&fetch=StringValue&order=ObjectID";
							return url.equals(rq.toUrl());
						}
					}))).thenReturn(new QueryResponse(
							new String(Files.readAllBytes(new File("src/test/resources/rally/AllowedValue.json").toPath()))));
					when(rallyRestApi.query(argThat(new ArgumentMatcher<QueryRequest>() {
						@Override
						public boolean matches(Object argument) {
							if (argument == null)
								return false;
							QueryRequest rq = (QueryRequest) argument;
							String url = "/defect.js?start=1&pagesize=1&fetch=true&order=ObjectID";
							return url.equals(rq.toUrl());
						}
					}))).thenReturn(
							new QueryResponse(new String(Files.readAllBytes(new File("src/test/resources/rally/Defect.json").toPath()))));
					when(rallyRestApi.query(argThat(new ArgumentMatcher<QueryRequest>() {
						@Override
						public boolean matches(Object argument) {
							if (argument == null)
								return false;
							QueryRequest rq = (QueryRequest) argument;
							String url = "/defect.js?start=1&pagesize=200&fetch=true&order=ObjectID&query=%28FormattedID+%3D+DE1%29";
							return url.equals(rq.toUrl());
						}
					}))).thenReturn(
							new QueryResponse(new String(Files.readAllBytes(new File("src/test/resources/rally/Defect.json").toPath()))));
					when(rallyRestApi.query(argThat(new ArgumentMatcher<QueryRequest>() {
						@Override
						public boolean matches(Object argument) {
							if (argument == null)
								return false;
							QueryRequest rq = (QueryRequest) argument;
							String url = "/defect.js?start=1&pagesize=200&fetch=true&order=ObjectID&query=%28Name+contains+test%29";
							return url.equals(rq.toUrl());
						}
					}))).thenReturn(
							new QueryResponse(new String(Files.readAllBytes(new File("src/test/resources/rally/Defect.json").toPath()))));
					when(rallyRestApi.create(argThat(new ArgumentMatcher<CreateRequest>() {
						@Override
						public boolean matches(Object argument) {
							if (argument == null)
								return false;
							CreateRequest rq = (CreateRequest) argument;
							String url = "/defect/create.js?fetch=true";
							return url.equals(rq.toUrl());
						}
					}))).thenReturn(new CreateResponse(
							new String(Files.readAllBytes(new File("src/test/resources/rally/CreateTicket.json").toPath()))));
					when(rallyRestApi.update(argThat(new ArgumentMatcher<UpdateRequest>() {
						@Override
						public boolean matches(Object argument) {
							if (argument == null)
								return false;
							UpdateRequest rq = (UpdateRequest) argument;
							String url = "/defect/51333816331.js?fetch=true";
							return url.equals(rq.toUrl());
						}
					}))).thenReturn(new UpdateResponse(
							new String(Files.readAllBytes(new File("src/test/resources/rally/UpdateTicket.json").toPath()))));
				} catch (IOException e) {

				}
				return rallyRestApi;
			}
		};
	}
}