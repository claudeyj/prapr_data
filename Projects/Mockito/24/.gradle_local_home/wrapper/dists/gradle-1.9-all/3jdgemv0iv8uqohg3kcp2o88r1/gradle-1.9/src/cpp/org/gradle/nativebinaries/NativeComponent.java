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

import org.gradle.api.Action;
import org.gradle.api.DomainObjectSet;
import org.gradle.api.Incubating;
import org.gradle.api.Named;
import org.gradle.language.base.LanguageSourceSet;

/**
 * Represents a logical software component, which may be built in a number of variant binaries.
 */
@Incubating
public interface NativeComponent extends Named {

    /**
     * The source sets that are used to build this component.
     */
    DomainObjectSet<LanguageSourceSet> getSource();

    /**
     * Adds one or more {@link LanguageSourceSet}s that are used to compile this binary.
     * <p/>
     * This method accepts the following types:
     *
     * <ul>
     *     <li>A {@link org.gradle.language.base.FunctionalSourceSet}</li>
     *     <li>A {@link LanguageSourceSet}</li>
     *     <li>A Collection of {@link LanguageSourceSet}s</li>
     * </ul>
     */
    void source(Object source);

    /**
     * The binaries that are built for this component. You can use this to configure the binaries for this component.
     */
    DomainObjectSet<NativeBinary> getBinaries();

    /**
     * The name that is used to construct the output file names when building this component.
     */
    String getBaseName();

    /**
     * Sets the name that is used to construct the output file names when building this component.
     */
    void setBaseName(String baseName);

    /**
     * The set of flavors defined for this component. All components automatically have a default flavor named "default".
     */
    FlavorContainer getFlavors();

    /**
     * Configure the flavors for this component.
     */
    void flavors(Action<? super FlavorContainer> config);

}
