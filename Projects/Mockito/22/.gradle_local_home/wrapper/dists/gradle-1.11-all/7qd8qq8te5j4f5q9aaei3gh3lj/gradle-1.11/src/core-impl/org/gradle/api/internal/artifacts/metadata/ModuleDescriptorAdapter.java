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

package org.gradle.api.internal.artifacts.metadata;

import org.apache.ivy.core.module.descriptor.*;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.internal.artifacts.DefaultModuleVersionIdentifier;
import org.gradle.api.internal.artifacts.component.DefaultModuleComponentIdentifier;
import org.gradle.util.CollectionUtils;

import java.util.*;

public class ModuleDescriptorAdapter implements MutableModuleVersionMetaData {
    private static final List<String> DEFAULT_STATUS_SCHEME = Arrays.asList("integration", "milestone", "release");

    private final ModuleVersionIdentifier moduleVersionIdentifier;
    private final ModuleDescriptor moduleDescriptor;
    private final ComponentIdentifier componentIdentifier;
    private boolean changing;
    private boolean metaDataOnly;
    private String status;
    private List<String> statusScheme = DEFAULT_STATUS_SCHEME;
    private List<DependencyMetaData> dependencies;
    private Map<String, DefaultConfigurationMetaData> configurations = new HashMap<String, DefaultConfigurationMetaData>();
    private Set<ModuleVersionArtifactMetaData> artifacts;

    public ModuleDescriptorAdapter(ModuleDescriptor moduleDescriptor) {
        this(DefaultModuleVersionIdentifier.newId(moduleDescriptor.getModuleRevisionId()), moduleDescriptor);
    }

    public ModuleDescriptorAdapter(ModuleVersionIdentifier identifier, ModuleDescriptor moduleDescriptor) {
        this(identifier, moduleDescriptor, DefaultModuleComponentIdentifier.newId(identifier));
    }

    public ModuleDescriptorAdapter(ModuleVersionIdentifier moduleVersionIdentifier, ModuleDescriptor moduleDescriptor, ComponentIdentifier componentIdentifier) {
        this.moduleVersionIdentifier = moduleVersionIdentifier;
        this.moduleDescriptor = moduleDescriptor;
        this.componentIdentifier = componentIdentifier;
        status = moduleDescriptor.getStatus();
    }

    public MutableModuleVersionMetaData copy() {
        // TODO:ADAM - need to make a copy of the descriptor (it's effectively immutable at this point so it's not a problem yet)
        ModuleDescriptorAdapter copy = new ModuleDescriptorAdapter(moduleVersionIdentifier, moduleDescriptor);
        copy.dependencies = dependencies;
        copy.changing = changing;
        copy.metaDataOnly = metaDataOnly;
        copy.status = status;
        copy.statusScheme = statusScheme;
        return copy;
    }

    @Override
    public String toString() {
        return moduleVersionIdentifier.toString();
    }

    public ModuleVersionIdentifier getId() {
        return moduleVersionIdentifier;
    }

    public ModuleDescriptor getDescriptor() {
        return moduleDescriptor;
    }

    public boolean isChanging() {
        return changing;
    }

    public boolean isMetaDataOnly() {
        return metaDataOnly;
    }

    public String getStatus() {
        return status;
    }

    public List<String> getStatusScheme() {
        return statusScheme;
    }

    public ComponentIdentifier getComponentId() {
        return componentIdentifier;
    }

    public void setChanging(boolean changing) {
        this.changing = changing;
    }

    public void setMetaDataOnly(boolean metaDataOnly) {
        this.metaDataOnly = metaDataOnly;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setStatusScheme(List<String> statusScheme) {
        this.statusScheme = statusScheme;
    }

    public List<DependencyMetaData> getDependencies() {
        if (dependencies == null) {
            dependencies = new ArrayList<DependencyMetaData>();
            for (final DependencyDescriptor dependencyDescriptor : moduleDescriptor.getDependencies()) {
                dependencies.add(new DefaultDependencyMetaData(dependencyDescriptor));
            }
        }
        return dependencies;
    }

    public void setDependencies(Iterable<? extends DependencyMetaData> dependencies) {
        this.dependencies = CollectionUtils.toList(dependencies);
        for (DefaultConfigurationMetaData configuration : configurations.values()) {
            configuration.dependencies = null;
        }
    }

    public DefaultConfigurationMetaData getConfiguration(final String name) {
        DefaultConfigurationMetaData configuration = configurations.get(name);
        if (configuration == null) {
            Configuration descriptor = moduleDescriptor.getConfiguration(name);
            if (descriptor == null) {
                return null;
            }
            Set<String> hierarchy = new LinkedHashSet<String>();
            hierarchy.add(name);
            for (String parent : descriptor.getExtends()) {
                hierarchy.addAll(getConfiguration(parent).hierarchy);
            }
            configuration = new DefaultConfigurationMetaData(name, descriptor, hierarchy);
            configurations.put(name, configuration);
        }
        return configuration;
    }

    public Set<ModuleVersionArtifactMetaData> getArtifacts() {
        if (artifacts == null) {
            artifacts = new LinkedHashSet<ModuleVersionArtifactMetaData>();
            for (Artifact artifact : moduleDescriptor.getAllArtifacts()) {
                artifacts.add(new DefaultModuleVersionArtifactMetaData(moduleVersionIdentifier, artifact));
            }
        }
        return artifacts;
    }

    protected Set<ModuleVersionArtifactMetaData> getArtifactsForConfiguration(ConfigurationMetaData configurationMetaData) {
        Map<Artifact, ModuleVersionArtifactMetaData> ivyArtifacts = new HashMap<Artifact, ModuleVersionArtifactMetaData>();
        for (ModuleVersionArtifactMetaData artifact : getArtifacts()) {
            ivyArtifacts.put(artifact.getArtifact(), artifact);
        }
        Set<ModuleVersionArtifactMetaData> artifacts = new LinkedHashSet<ModuleVersionArtifactMetaData>();
        for (String ancestor : configurationMetaData.getHierarchy()) {
            for (Artifact artifact : moduleDescriptor.getArtifacts(ancestor)) {
                artifacts.add(ivyArtifacts.get(artifact));
            }
        }
        return artifacts;
    }

    private class DefaultConfigurationMetaData implements ConfigurationMetaData {
        private final String name;
        private final Configuration descriptor;
        private final Set<String> hierarchy;
        private List<DependencyMetaData> dependencies;
        private Set<ModuleVersionArtifactMetaData> artifacts;
        private LinkedHashSet<ExcludeRule> excludeRules;

        private DefaultConfigurationMetaData(String name, Configuration descriptor, Set<String> hierarchy) {
            this.name = name;
            this.descriptor = descriptor;
            this.hierarchy = hierarchy;
        }

        @Override
        public String toString() {
            return String.format("%s:%s", moduleVersionIdentifier, name);
        }

        public ModuleVersionMetaData getModuleVersion() {
            return ModuleDescriptorAdapter.this;
        }

        public String getName() {
            return name;
        }

        public Set<String> getHierarchy() {
            return hierarchy;
        }

        public boolean isTransitive() {
            return descriptor.isTransitive();
        }

        public List<DependencyMetaData> getDependencies() {
            if (dependencies == null) {
                dependencies = new ArrayList<DependencyMetaData>();
                for (DependencyMetaData dependency : ModuleDescriptorAdapter.this.getDependencies()) {
                    if (include(dependency)) {
                        dependencies.add(dependency);
                    }
                }
            }
            return dependencies;
        }

        private boolean include(DependencyMetaData dependency) {
            String[] moduleConfigurations = dependency.getDescriptor().getModuleConfigurations();
            for (int i = 0; i < moduleConfigurations.length; i++) {
                String moduleConfiguration = moduleConfigurations[i];
                if (moduleConfiguration.equals("%") || hierarchy.contains(moduleConfiguration)) {
                    return true;
                }
                if (moduleConfiguration.equals("*")) {
                    boolean include = true;
                    for (int j = i + 1; j < moduleConfigurations.length && moduleConfigurations[j].startsWith("!"); j++) {
                        if (moduleConfigurations[j].substring(1).equals(getName())) {
                            include = false;
                            break;
                        }
                    }
                    if (include) {
                        return true;
                    }
                }
            }
            return false;
        }

        public Set<ExcludeRule> getExcludeRules() {
            if (excludeRules == null) {
                excludeRules = new LinkedHashSet<ExcludeRule>();
                for (ExcludeRule excludeRule : moduleDescriptor.getAllExcludeRules()) {
                    for (String config : excludeRule.getConfigurations()) {
                        if (hierarchy.contains(config)) {
                            excludeRules.add(excludeRule);
                            break;
                        }
                    }
                }
            }
            return excludeRules;
        }

        public Set<ModuleVersionArtifactMetaData> getArtifacts() {
            if (artifacts == null) {
                artifacts = getArtifactsForConfiguration(this);
            }
            return artifacts;
        }
    }
}
