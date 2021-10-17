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

package org.gradle.invocation;

import org.gradle.api.internal.initialization.ScriptCompileScope;
import org.gradle.initialization.ClassLoaderRegistry;
import org.gradle.internal.classloader.CachingClassLoader;
import org.gradle.internal.classloader.MultiParentClassLoader;

public class DefaultBuildClassLoaderRegistry implements BuildClassLoaderRegistry, ScriptCompileScope {
    private final MultiParentClassLoader rootClassLoader;
    private final CachingClassLoader scriptClassLoader;

    public DefaultBuildClassLoaderRegistry(ClassLoaderRegistry registry) {
        rootClassLoader = new MultiParentClassLoader(registry.getGradleApiClassLoader());
        scriptClassLoader = new CachingClassLoader(rootClassLoader);
    }

    public void addRootClassLoader(ClassLoader classLoader) {
        rootClassLoader.addParent(classLoader);
    }

    public ScriptCompileScope getRootCompileScope() {
        return this;
    }

    public ClassLoader getScriptCompileClassLoader() {
        return scriptClassLoader;
    }
}
