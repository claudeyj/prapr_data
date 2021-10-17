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
package org.gradle.nativebinaries;

import org.gradle.api.Incubating;
import org.gradle.api.file.SourceDirectorySet;

/**
 * The logical representation of an library native component.
 */
@Incubating
public interface Library extends NativeComponent {
    /**
     * The headers exported by this library.
     */
    SourceDirectorySet getHeaders();

    /**
     * Converts this library to a native dependency that uses the shared library variant. This is the default.
     */
    LibraryResolver getShared();

    /**
     * Converts this library to a native dependency that uses the static library variant.
     */
    LibraryResolver getStatic();
}