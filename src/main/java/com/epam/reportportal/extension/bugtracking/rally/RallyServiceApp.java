/*
 * Copyright 2016 EPAM Systems
 * 
 * 
 * This file is part of EPAM Report Portal.
 * https://github.com/epam/ReportPortal
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
/*
 * This file is part of Report Portal.
 *
 * Report Portal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Report Portal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Report Portal.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.epam.reportportal.extension.bugtracking.rally;

import com.epam.reportportal.extension.bugtracking.BugTrackingApp;
import com.epam.reportportal.extension.bugtracking.ExternalSystemStrategy;
import com.epam.reportportal.commons.template.VelocityTemplateEngine;
import com.rallydev.rest.RallyRestApi;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.ui.velocity.VelocityEngineFactoryBean;

import java.net.URI;

/**
 * Entry point for Rally integration service
 *
 * @author Andrei Varabyeu
 */
public class RallyServiceApp extends BugTrackingApp {

	@Override
	public ExternalSystemStrategy externalSystemStrategy() {
		return new RallyStrategy() {
			@Override
			public RallyRestApi getRallyRestApi(URI server, String apiKey) {
				return new RallyRestApi(server, apiKey);
			}
		};

	}

	@Bean
	public VelocityEngineFactoryBean getVelocityEngineFactory() {
		VelocityEngineFactoryBean velocityEngineFactory = new VelocityEngineFactoryBean();
		velocityEngineFactory.setResourceLoaderPath("classpath:/");
		velocityEngineFactory.setPreferFileSystemAccess(false);
		return velocityEngineFactory;
	}

	@Bean
	public VelocityTemplateEngine getVelocityTemplateEngine() {
		return new VelocityTemplateEngine(getVelocityEngineFactory().getObject());
	}

	public static void main(String[] args) {
		SpringApplication.run(RallyServiceApp.class);
	}
}