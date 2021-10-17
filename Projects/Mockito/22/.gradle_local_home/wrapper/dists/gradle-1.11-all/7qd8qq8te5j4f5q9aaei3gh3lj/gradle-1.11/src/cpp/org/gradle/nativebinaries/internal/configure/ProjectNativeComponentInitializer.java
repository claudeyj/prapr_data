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

package org.gradle.nativebinaries.internal.configure;

import org.gradle.api.Action;
import org.gradle.language.base.internal.BinaryNamingSchemeBuilder;
import org.gradle.nativebinaries.BuildType;
import org.gradle.nativebinaries.Flavor;
import org.gradle.nativebinaries.ProjectNativeComponent;
import org.gradle.nativebinaries.internal.ProjectNativeComponentInternal;
import org.gradle.nativebinaries.platform.Platform;
import org.gradle.nativebinaries.toolchain.ToolChain;
import org.gradle.nativebinaries.toolchain.internal.ToolChainRegistryInternal;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public class ProjectNativeComponentInitializer implements Action<ProjectNativeComponent> {
    private final NativeBinariesFactory factory;
    private final ToolChainRegistryInternal toolChainRegistry;
    private final Set<Platform> allPlatforms = new LinkedHashSet<Platform>();
    private final Set<BuildType> allBuildTypes = new LinkedHashSet<BuildType>();
    private final Set<Flavor> allFlavors = new LinkedHashSet<Flavor>();
    private final BinaryNamingSchemeBuilder namingSchemeBuilder;

    public ProjectNativeComponentInitializer(NativeBinariesFactory factory, BinaryNamingSchemeBuilder namingSchemeBuilder, ToolChainRegistryInternal toolChainRegistry,
                                             Collection<? extends Platform> allPlatforms, Collection<? extends BuildType> allBuildTypes, Collection<? extends Flavor> allFlavors) {
        this.factory = factory;
        this.namingSchemeBuilder = namingSchemeBuilder;
        this.toolChainRegistry = toolChainRegistry;
        this.allPlatforms.addAll(allPlatforms);
        this.allBuildTypes.addAll(allBuildTypes);
        this.allFlavors.addAll(allFlavors);
    }

    public void execute(ProjectNativeComponent projectNativeComponent) {
        ProjectNativeComponentInternal component = (ProjectNativeComponentInternal) projectNativeComponent;
        for (Platform platform : component.choosePlatforms(allPlatforms)) {
            ToolChain toolChain = toolChainRegistry.getForPlatform(platform);
            for (BuildType buildType : component.chooseBuildTypes(allBuildTypes)) {
                for (Flavor flavor : component.chooseFlavors(allFlavors)) {
                    BinaryNamingSchemeBuilder namingScheme = initializeNamingScheme(component, platform, buildType, flavor);
                    factory.createNativeBinaries(component, namingScheme, toolChain, platform, buildType, flavor);
                }
            }
        }
    }

    private BinaryNamingSchemeBuilder initializeNamingScheme(ProjectNativeComponentInternal component, Platform platform, BuildType buildType, Flavor flavor) {
        BinaryNamingSchemeBuilder namingScheme = namingSchemeBuilder.withComponentName(component.getName());
        if (usePlatformDimension(component)) {
            namingScheme = namingScheme.withVariantDimension(platform.getName());
        }
        if (useBuildTypeDimension(component)) {
            namingScheme = namingScheme.withVariantDimension(buildType.getName());
        }
        if (useFlavorDimension(component)) {
            namingScheme = namingScheme.withVariantDimension(flavor.getName());
        }
        return namingScheme;
    }

    private boolean usePlatformDimension(ProjectNativeComponentInternal component) {
        return component.choosePlatforms(allPlatforms).size() > 1;
    }

    private boolean useBuildTypeDimension(ProjectNativeComponentInternal component) {
        return component.chooseBuildTypes(allBuildTypes).size() > 1;
    }

    private boolean useFlavorDimension(ProjectNativeComponentInternal component) {
        return component.chooseFlavors(allFlavors).size() > 1;
    }

}
