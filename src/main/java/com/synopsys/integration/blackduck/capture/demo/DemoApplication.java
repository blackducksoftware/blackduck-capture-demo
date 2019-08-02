/**
 * demo
 * <p>
 * Copyright (c) 2019 Synopsys, Inc.
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.synopsys.integration.blackduck.capture.demo;

import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionView;
import com.synopsys.integration.blackduck.api.generated.view.ProjectView;
import com.synopsys.integration.blackduck.api.generated.view.VersionBomComponentView;
import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfig;
import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfigBuilder;
import com.synopsys.integration.blackduck.service.BlackDuckServicesFactory;
import com.synopsys.integration.blackduck.service.ProjectBomService;
import com.synopsys.integration.blackduck.service.ProjectService;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.LogLevel;
import com.synopsys.integration.log.PrintStreamIntLogger;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

import java.util.List;
import java.util.stream.Collectors;

@SpringBootApplication
public class DemoApplication implements ApplicationRunner {
    /**
     * The environment variables that make this work are:
     * BLACKDUCK_URL and either BLACKDUCK_API_TOKEN or BLACKDUCK_USERNAME and BLACKDUCK_PASSWORD
     */
    public static void main(final String[] args) {
        SpringApplicationBuilder builder = new SpringApplicationBuilder(DemoApplication.class);
        builder.logStartupInfo(false);
        builder.run(args);
    }

    @Override
    public void run(final ApplicationArguments applicationArguments) throws IntegrationException {
        // configure these values for your project/version
        String projectName = "";
        String projectVersionName = "";

        IntLogger logger = new PrintStreamIntLogger(System.out, LogLevel.INFO);
        BlackDuckServerConfigBuilder blackDuckServerConfigBuilder = BlackDuckServerConfig.newBuilder();
        blackDuckServerConfigBuilder.setLogger(logger);
        blackDuckServerConfigBuilder.setTrustCert(true);
        blackDuckServerConfigBuilder.setProperties(System.getenv().entrySet());

        BlackDuckServerConfig blackDuckServerConfig = blackDuckServerConfigBuilder.build();
        BlackDuckServicesFactory blackDuckServicesFactory = blackDuckServerConfig.createBlackDuckServicesFactory(logger);
        ProjectService projectService = blackDuckServicesFactory.createProjectService();
        ProjectBomService projectBomService = blackDuckServicesFactory.createProjectBomService();

        // print out all the project names
        projectService.getAllProjects().stream().map(ProjectView::getName).forEach(System.out::println);

        // print out all the version names for a given project name
        ProjectView projectView = projectService.getProjectByName(projectName).orElseThrow(() -> new IntegrationException(String.format("Could not find a project named %s.", projectName)));
        projectService.getAllProjectVersions(projectView).stream().map(ProjectVersionView::getVersionName).forEach(System.out::println);

        // print out all the components for a project/version (The BOM, or Bill Of Materials)
        ProjectVersionView projectVersionView = projectService.getProjectVersion(projectView, projectVersionName).orElseThrow(() -> new IntegrationException(String.format("Could not find a version named %s in the %s project.", projectVersionName, projectName)));
        List<VersionBomComponentView> bomComponents = projectBomService.getComponentsForProjectVersion(projectVersionView);
        bomComponents.stream().map(component -> String.format("%s (%s)", component.getComponentName(), component.getComponentVersionName())).forEach(System.out::println);

        // print just the components with complex licenses
        List<VersionBomComponentView> bomComponentsWithMoreThanOneLicense = bomComponents
                .stream()
                .filter(component -> null != component.getLicenses() && component.getLicenses().size() > 1)
                .collect(Collectors.toList());

        bomComponentsWithMoreThanOneLicense
                .stream()
                .map(component -> String.format("%s (%s)", component.getComponentName(), component.getComponentVersionName()))
                .forEach(System.out::println);

        System.out.println(String.format("Total size of the BOM: %d", bomComponents.size()));
        System.out.println(String.format("Number of components with multiple licenses: %d", bomComponentsWithMoreThanOneLicense.size()));
    }

}
