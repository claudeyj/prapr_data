/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.nativebinaries.language.cpp.plugins

import org.gradle.api.Incubating
import org.gradle.api.Project
import org.gradle.api.Plugin

import org.gradle.api.internal.artifacts.publish.DefaultPublishArtifact
import org.gradle.api.internal.plugins.DefaultArtifactPublicationSet
import org.gradle.configuration.project.ProjectConfigurationActionContainer

import javax.inject.Inject

/**
 * A convention-based plugin that automatically adds a single C++ source set named "main" and wires it into a {@link org.gradle.nativebinaries.Executable} named "main".
 */
@Incubating
class CppExeConventionPlugin implements Plugin<Project> {
    private final ProjectConfigurationActionContainer configureActions

    @Inject
    CppExeConventionPlugin(ProjectConfigurationActionContainer configureActions) {
        this.configureActions = configureActions
    }

    void apply(Project project) {
        project.plugins.apply(CppPlugin)
        
        project.with {
            executables {
                main {
                    baseName = project.name
                }
            }
        }

        configureActions.add {
            project.with {
                def exeArtifact = new DefaultPublishArtifact(
                        archivesBaseName, // name
                        "exe", // ext
                        "exe", // type
                        null, // classifier
                        null, // date

                        // needs to be more general and not peer into the spec
                        binaries.mainExecutable.outputFile,
                        binaries.mainExecutable
                )

                extensions.getByType(DefaultArtifactPublicationSet).addCandidate(exeArtifact)
            }
        }
    }

}