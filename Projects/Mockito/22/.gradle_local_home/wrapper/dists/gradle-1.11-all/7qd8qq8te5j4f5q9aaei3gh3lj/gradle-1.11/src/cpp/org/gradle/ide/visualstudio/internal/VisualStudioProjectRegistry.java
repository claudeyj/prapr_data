/*
 * Copyright 2013 the original author or authors.
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

package org.gradle.ide.visualstudio.internal;

import org.gradle.api.internal.DefaultNamedDomainObjectSet;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.nativebinaries.*;

public class VisualStudioProjectRegistry extends DefaultNamedDomainObjectSet<DefaultVisualStudioProject> {
    private final FileResolver fileResolver;
    private final VisualStudioProjectResolver projectResolver;
    private final VisualStudioProjectMapper projectMapper;

    public VisualStudioProjectRegistry(FileResolver fileResolver, VisualStudioProjectResolver projectResolver,
                                       VisualStudioProjectMapper projectMapper, Instantiator instantiator) {
        super(DefaultVisualStudioProject.class, instantiator);
        this.fileResolver = fileResolver;
        this.projectResolver = projectResolver;
        this.projectMapper = projectMapper;
    }

    public VisualStudioProjectConfiguration getProjectConfiguration(ProjectNativeBinary nativeBinary) {
        String projectName = projectName(nativeBinary);
        return getByName(projectName).getConfiguration(nativeBinary);
    }

    public void addProjectConfiguration(ProjectNativeBinary nativeBinary) {
        VisualStudioProjectMapper.ProjectConfigurationNames names = projectMapper.mapToConfiguration(nativeBinary);
        DefaultVisualStudioProject project = getOrCreateProject(nativeBinary, names.project);
        VisualStudioProjectConfiguration configuration = createVisualStudioProjectConfiguration(nativeBinary, project, names.configuration, names.platform);
        project.addConfiguration(nativeBinary, configuration);
    }

    private VisualStudioProjectConfiguration createVisualStudioProjectConfiguration(ProjectNativeBinary nativeBinary, DefaultVisualStudioProject project, String configuration, String platform) {
        return getInstantiator().newInstance(
                VisualStudioProjectConfiguration.class, project, configuration, platform, nativeBinary, configurationType(nativeBinary));
    }

    private DefaultVisualStudioProject getOrCreateProject(ProjectNativeBinary nativeBinary, String projectName) {
        DefaultVisualStudioProject vsProject = findByName(projectName);
        if (vsProject == null) {
            vsProject = getInstantiator().newInstance(DefaultVisualStudioProject.class, projectName, nativeBinary.getComponent(), fileResolver, projectResolver, getInstantiator());
            vsProject.source(nativeBinary.getSource());
            add(vsProject);
        }
        return vsProject;
    }

    private String projectName(ProjectNativeBinary nativeBinary) {
        return projectMapper.mapToConfiguration(nativeBinary).project;
    }

    private static String configurationType(NativeBinary nativeBinary) {
        return nativeBinary instanceof StaticLibraryBinary ? "StaticLibrary"
                : nativeBinary instanceof SharedLibraryBinary ? "DynamicLibrary"
                : "Application";
    }
}

