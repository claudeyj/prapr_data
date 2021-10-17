/*
 * Copyright 2012 the original author or authors.
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

package org.gradle.api.internal.artifacts.ivyservice.resolveengine.result;

import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.result.*;
import org.gradle.api.internal.artifacts.result.DefaultResolutionResult;
import org.gradle.api.internal.artifacts.result.DefaultResolvedModuleVersionResult;
import org.gradle.internal.Factory;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class DefaultResolutionResultBuilder implements ResolutionResultBuilder {

    private DefaultResolvedModuleVersionResult rootModule;

    private Map<ModuleVersionIdentifier, DefaultResolvedModuleVersionResult> modules
            = new LinkedHashMap<ModuleVersionIdentifier, DefaultResolvedModuleVersionResult>();

    CachingDependencyResultFactory dependencyResultFactory = new CachingDependencyResultFactory();

    public DefaultResolutionResultBuilder start(ModuleVersionIdentifier root) {
        rootModule = createOrGet(root, VersionSelectionReasons.ROOT);
        return this;
    }

    public ResolutionResult complete() {
        return new DefaultResolutionResult(new RootFactory(rootModule));
    }

    public void resolvedModuleVersion(ModuleVersionSelection moduleVersion) {
        createOrGet(moduleVersion.getSelectedId(), moduleVersion.getSelectionReason());
    }

    public void resolvedConfiguration(ModuleVersionIdentifier id, Collection<? extends InternalDependencyResult> dependencies) {
        for (InternalDependencyResult d : dependencies) {
            DefaultResolvedModuleVersionResult from = modules.get(id);
            DependencyResult dependency;
            if (d.getFailure() != null) {
                dependency = dependencyResultFactory.createUnresolvedDependency(d.getRequested(), from, d.getReason(), d.getFailure());
            } else {
                DefaultResolvedModuleVersionResult selected = modules.get(d.getSelected().getSelectedId());
                dependency = dependencyResultFactory.createResolvedDependency(d.getRequested(), from, selected);
                selected.addDependent((ResolvedDependencyResult) dependency);
            }
            from.addDependency(dependency);
        }
    }

    private DefaultResolvedModuleVersionResult createOrGet(ModuleVersionIdentifier id, ModuleVersionSelectionReason selectionReason) {
        if (!modules.containsKey(id)) {
            modules.put(id, new DefaultResolvedModuleVersionResult(id, selectionReason));
        }
        return modules.get(id);
    }

    private static class RootFactory implements Factory<ResolvedModuleVersionResult> {
        private DefaultResolvedModuleVersionResult rootModule;

        public RootFactory(DefaultResolvedModuleVersionResult rootModule) {
            this.rootModule = rootModule;
        }

        public ResolvedModuleVersionResult create() {
            return rootModule;
        }
    }
}