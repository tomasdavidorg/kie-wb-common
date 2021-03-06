/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
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

package org.kie.workbench.common.screens.archetype.mgmt.backend.maven;

import java.util.Properties;

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.appformer.maven.integration.embedder.MavenEmbedder;
import org.appformer.maven.integration.embedder.MavenEmbedderException;
import org.appformer.maven.integration.embedder.MavenProjectLoader;
import org.appformer.maven.integration.embedder.MavenRequest;

import static org.kie.soup.commons.validation.PortablePreconditions.checkNotNull;

public abstract class AbstractMavenCommand {

    protected final String baseDirectory;
    private final int loggingLevel;

    protected AbstractMavenCommand(final String baseDirectory) {
        this(baseDirectory,
             MavenExecutionRequest.LOGGING_LEVEL_ERROR);
    }

    private AbstractMavenCommand(final String baseDirectory,
                                 final int loggingLevel) {
        this.baseDirectory = checkNotNull("baseDirectory", baseDirectory);
        this.loggingLevel = loggingLevel;
    }

    public abstract MavenRequest buildMavenRequest();

    public abstract Properties buildUserProperties();

    public MavenExecutionResult execute() throws MavenEmbedderException {
        final MavenEmbedder mavenEmbedder = createMavenEmbedder();
        final MavenRequest mavenRequest = prepareExecution();

        try {
            return mavenEmbedder.execute(mavenRequest);
        } finally {
            mavenEmbedder.dispose();
        }
    }

    MavenEmbedder createMavenEmbedder() throws MavenEmbedderException {
        return new MavenEmbedder(MavenProjectLoader.createMavenRequest(false));
    }

    MavenRequest prepareExecution() {
        final MavenRequest mavenRequest = buildMavenRequest();

        mavenRequest.setBaseDirectory(baseDirectory);
        mavenRequest.setLoggingLevel(loggingLevel);
        mavenRequest.setUserProperties(buildUserProperties());

        return mavenRequest;
    }
}
