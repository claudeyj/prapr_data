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

package org.gradle.nativebinaries.toolchain.internal.clang;

import org.gradle.api.internal.file.FileResolver;
import org.gradle.internal.os.OperatingSystem;
import org.gradle.nativebinaries.toolchain.Clang;
import org.gradle.nativebinaries.toolchain.internal.gcc.ToolRegistry;
import org.gradle.nativebinaries.toolchain.internal.ToolType;
import org.gradle.nativebinaries.toolchain.internal.gcc.AbstractGccCompatibleToolChain;
import org.gradle.process.internal.ExecActionFactory;

public class ClangToolChain extends AbstractGccCompatibleToolChain implements Clang {
    public static final String DEFAULT_NAME = "clang";

    public ClangToolChain(String name, OperatingSystem operatingSystem, FileResolver fileResolver, ExecActionFactory execActionFactory) {
        super(name, operatingSystem, fileResolver, execActionFactory, new ToolRegistry(operatingSystem));

        tools.setExeName(ToolType.CPP_COMPILER, "clang++");
        tools.setExeName(ToolType.C_COMPILER, "clang");
        tools.setExeName(ToolType.ASSEMBLER, "as");
        tools.setExeName(ToolType.LINKER, "clang++");
        tools.setExeName(ToolType.STATIC_LIB_ARCHIVER, "ar");
    }

    @Override
    protected String getTypeName() {
        return "Clang";
    }

}
