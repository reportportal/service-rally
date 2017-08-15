/*
 * Copyright 2017 EPAM Systems
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
import com.google.common.collect.ImmutableMap;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;

/**
 * @author Andrei Varabyeu
 */
public class BugTemplateTest {

	@Test
	public void testTemplate() {
		TemplateEngine templateEngine = new RallyServiceApp().getTemplateEngine();

		RallyStrategy.LogEntry log1 = new RallyStrategy.LogEntry();
		log1.setMessage("log 1 message");
		log1.setBinaryDataId("binary data id");

		Map<String, ?> params = ImmutableMap.<String, Object>builder().put("description", "demo description")
				.put("comments", "demo comments").put("logs", Arrays.asList(log1)).build();

		String result = templateEngine.merge("bug_template.ftl", params);

		Assert.assertThat("Description not found", result, CoreMatchers.containsString("demo description"));
		Assert.assertThat("Comments not found", result, CoreMatchers.containsString("demo comments"));
		Assert.assertThat("Log message not found", result, CoreMatchers.containsString("log 1 message"));
		Assert.assertThat("Binary Data ID not found", result, CoreMatchers.containsString("binary data id"));

	}
}
