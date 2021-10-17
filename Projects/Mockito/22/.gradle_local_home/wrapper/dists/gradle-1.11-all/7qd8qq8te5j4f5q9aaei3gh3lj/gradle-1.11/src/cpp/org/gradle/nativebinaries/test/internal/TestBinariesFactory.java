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
package org.gradle.nativebinaries.test.internal;

import org.gradle.internal.reflect.Instantiator;
import org.gradle.language.base.internal.BinaryNamingScheme;
import org.gradle.language.base.internal.BinaryNamingSchemeBuilder;
import org.gradle.nativebinaries.BuildType;
import org.gradle.nativebinaries.Flavor;
import org.gradle.nativebinaries.ProjectNativeBinary;
import org.gradle.nativebinaries.internal.ProjectNativeBinaryInternal;
import org.gradle.nativebinaries.internal.ProjectNativeComponentInternal;
import org.gradle.nativebinaries.internal.configure.NativeBinariesFactory;
import org.gradle.nativebinaries.internal.resolve.NativeDependencyResolver;
import org.gradle.nativebinaries.platform.Platform;
import org.gradle.nativebinaries.test.TestSuiteExecutableBinary;
import org.gradle.nativebinaries.toolchain.ToolChain;
import org.gradle.nativebinaries.toolchain.internal.ToolChainInternal;

import java.io.File;

class TestBinariesFactory implements NativeBinariesFactory {
    private final Instantiator instantiator;
    private final NativeDependencyResolver resolver;
    private final File binariesOutputDir;

    TestBinariesFactory(Instantiator instantiator, NativeDependencyResolver resolver, File binariesOutputDir) {
        this.instantiator = instantiator;
        this.resolver = resolver;
        this.binariesOutputDir = binariesOutputDir;
    }

    public void createNativeBinaries(ProjectNativeComponentInternal component, BinaryNamingSchemeBuilder namingSchemeBuilder, ToolChain toolChain, Platform platform, BuildType buildType, Flavor flavor) {
        BinaryNamingScheme namingScheme = namingSchemeBuilder.withTypeString("CUnitExe").build();
        ProjectNativeBinary nativeBinary = instantiator.newInstance(DefaultTestSuiteExecutableBinary.class, component, flavor, toolChain, platform, buildType, namingScheme, resolver);
        setupDefaults(nativeBinary);
        component.getBinaries().add(nativeBinary);
    }

    private void setupDefaults(ProjectNativeBinary nativeBinary) {
        BinaryNamingScheme namingScheme = ((ProjectNativeBinaryInternal) nativeBinary).getNamingScheme();
        File binaryOutputDir = new File(binariesOutputDir, namingScheme.getOutputDirectoryBase());
        String baseName = nativeBinary.getComponent().getBaseName();

        ToolChainInternal tc = (ToolChainInternal) nativeBinary.getToolChain();
        ((TestSuiteExecutableBinary) nativeBinary).setExecutableFile(new File(binaryOutputDir, tc.getExecutableName(baseName)));
    }
}
