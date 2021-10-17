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

package org.gradle.nativebinaries.toolchain.internal.msvcpp;

import org.gradle.api.internal.tasks.compile.ArgWriter;
import org.gradle.api.internal.tasks.compile.Compiler;
import org.gradle.api.tasks.WorkResult;
import org.gradle.nativebinaries.language.cpp.internal.CppCompileSpec;
import org.gradle.nativebinaries.toolchain.internal.OptionsFileArgsTransformer;
import org.gradle.nativebinaries.toolchain.internal.CommandLineTool;

class CppCompiler implements Compiler<CppCompileSpec> {

    private final CommandLineTool<CppCompileSpec> commandLineTool;

    CppCompiler(CommandLineTool<CppCompileSpec> commandLineTool) {
        this.commandLineTool = commandLineTool.withArguments(
                new OptionsFileArgsTransformer<CppCompileSpec>(
                        ArgWriter.windowsStyleFactory(),
                        new CppCompilerArgsTransformer()));
    }

    public WorkResult execute(CppCompileSpec spec) {
        return commandLineTool.inWorkDirectory(spec.getObjectFileDir()).execute(spec);
    }

    private static class CppCompilerArgsTransformer extends VisualCppCompilerArgsTransformer<CppCompileSpec> {
        protected String getLanguageOption() {
            return "/TP";
        }
    }
}
