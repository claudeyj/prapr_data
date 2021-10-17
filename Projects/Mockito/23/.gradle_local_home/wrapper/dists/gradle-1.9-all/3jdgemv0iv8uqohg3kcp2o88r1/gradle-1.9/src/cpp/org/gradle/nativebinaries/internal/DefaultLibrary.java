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

package org.gradle.nativebinaries.internal;

import org.gradle.api.Action;
import org.gradle.api.DomainObjectSet;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.internal.file.DefaultSourceDirectorySet;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.language.HeaderExportingSourceSet;
import org.gradle.nativebinaries.*;

import java.util.ArrayList;
import java.util.List;

public class DefaultLibrary extends DefaultNativeComponent implements Library {
    private final DefaultSourceDirectorySet headers;

    public DefaultLibrary(String name, Instantiator instantiator, FileResolver fileResolver) {
        super(name, instantiator);
        this.headers = new DefaultSourceDirectorySet("headers", String.format("Exported headers for native library '%s'", name), fileResolver);
        initExportedHeaderTracking();
    }

    @Override
    public String toString() {
        return String.format("library '%s'", getName());
    }

    public SourceDirectorySet getHeaders() {
        return headers;
    }

    public ContextualLibraryResolver getShared() {
        return new DefaultLibraryResolver(this).withType(SharedLibraryBinary.class);
    }

    public ContextualLibraryResolver getStatic() {
        return new DefaultLibraryResolver(this).withType(StaticLibraryBinary.class);
    }

    private void initExportedHeaderTracking() {
        // TODO - headers.srcDirs() should allow a Callable<SourceDirectorySet> for lazy calculation
        final DomainObjectSet<HeaderExportingSourceSet> headerExportingSourceSets = getSource().withType(HeaderExportingSourceSet.class);
        headerExportingSourceSets.all(new Action<HeaderExportingSourceSet>() {
            public void execute(HeaderExportingSourceSet headerExportingSourceSet) {
                updateHeaderDirs(headerExportingSourceSets, headers);
            }
        });
        headerExportingSourceSets.whenObjectRemoved(new Action<HeaderExportingSourceSet>() {
            public void execute(HeaderExportingSourceSet headerExportingSourceSet) {
                updateHeaderDirs(headerExportingSourceSets, headers);
            }
        });
    }

    private void updateHeaderDirs(DomainObjectSet<HeaderExportingSourceSet> sourceSets, DefaultSourceDirectorySet headers) {
        List<SourceDirectorySet> headerDirs = new ArrayList<SourceDirectorySet>();
        for (HeaderExportingSourceSet sourceSet : sourceSets) {
            headerDirs.add(sourceSet.getExportedHeaders());
        }
        headers.setSrcDirs(headerDirs);
    }
}