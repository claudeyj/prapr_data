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

package org.gradle.nativebinaries.internal;

import org.gradle.api.InvalidUserDataException;
import org.gradle.nativebinaries.Library;
import org.gradle.nativebinaries.LibraryResolver;
import org.gradle.nativebinaries.NativeBinary;
import org.gradle.nativebinaries.NativeDependencySet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class NativeDependencyResolver {
    public Collection<NativeDependencySet> resolve(NativeBinary target, Collection<?> libs) {
        List<NativeDependencySet> result = new ArrayList<NativeDependencySet>();
        for (Object lib : libs) {
            result.add(resolve(target, lib));
            resolve(target, lib);
        }
        return result;
    }

    private NativeDependencySet resolve(NativeBinary target, Object lib) {
        if (lib instanceof NativeDependencySet) {
            return (NativeDependencySet) lib;
        }
        if (lib instanceof Library) {
            return resolve(target, ((Library) lib).getShared());
        }
        if (lib instanceof ContextualLibraryResolver) {
            return ((ContextualLibraryResolver) lib)
                    .withFlavor(target.getFlavor())
                    .withToolChain(target.getToolChain())
                    .withPlatform(target.getTargetPlatform())
                    .withBuildType(target.getBuildType())
                    .resolve();
        }
        if (lib instanceof LibraryResolver) {
            return ((LibraryResolver) lib).resolve();
        }

        throw new InvalidUserDataException("Not a valid type for a library dependency: " + lib);
    }
}
