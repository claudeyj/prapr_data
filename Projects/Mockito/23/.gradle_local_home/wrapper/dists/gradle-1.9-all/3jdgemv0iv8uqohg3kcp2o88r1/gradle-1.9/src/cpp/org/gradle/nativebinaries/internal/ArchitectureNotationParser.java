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

import org.gradle.api.internal.notations.NotationParserBuilder;
import org.gradle.api.internal.notations.api.NotationParser;
import org.gradle.api.internal.notations.api.UnsupportedNotationException;
import org.gradle.api.internal.notations.parsers.TypedNotationParser;

import java.util.Arrays;
import java.util.List;

public class ArchitectureNotationParser {

    private static final List<String> X86_ALIASES = Arrays.asList("x86", "i386", "ia-32");
    private static final List<String> X86_64_ALIASES = Arrays.asList("x86_64", "amd64", "x64", "x86-64");
    private static final List<String> ITANIUM_ALIASES = Arrays.asList("ia-64");
    private static final List<String> PPC_32_ALIASES = Arrays.asList("ppc");
    private static final List<String> PPC_64_ALIASES = Arrays.asList("ppc64");
    private static final List<String> SPARC_32_ALIASES = Arrays.asList("sparc", "sparc32", "sparc-v7", "sparc-v8");
    private static final List<String> SPARC_64_ALIASES = Arrays.asList("sparc64", "ultrasparc", "sparc-v9");
    private static final List<String> ARM_ALIASES = Arrays.asList("arm");

    public static NotationParser<ArchitectureInternal> parser() {
        return new NotationParserBuilder<ArchitectureInternal>()
                .resultingType(ArchitectureInternal.class)
                .parser(new Parser())
                .toComposite();
    }

    private static final class Parser extends TypedNotationParser<String, ArchitectureInternal> {
        private Parser() {
            super(String.class);
        }

        @Override
        protected ArchitectureInternal parseType(String notation) {
            if (X86_ALIASES.contains(notation.toLowerCase())) {
                return new DefaultArchitecture(notation, ArchitectureInternal.InstructionSet.X86, 32);
            }
            if (X86_64_ALIASES.contains(notation.toLowerCase())) {
                return new DefaultArchitecture(notation, ArchitectureInternal.InstructionSet.X86, 64);
            }
            if (ITANIUM_ALIASES.contains(notation.toLowerCase())) {
                return new DefaultArchitecture(notation, ArchitectureInternal.InstructionSet.ITANIUM, 64);
            }
            if (PPC_32_ALIASES.contains(notation.toLowerCase())) {
                return new DefaultArchitecture(notation, ArchitectureInternal.InstructionSet.PPC, 32);
            }
            if (PPC_64_ALIASES.contains(notation.toLowerCase())) {
                return new DefaultArchitecture(notation, ArchitectureInternal.InstructionSet.PPC, 64);
            }
            if (SPARC_32_ALIASES.contains(notation.toLowerCase())) {
                return new DefaultArchitecture(notation, ArchitectureInternal.InstructionSet.SPARC, 32);
            }
            if (SPARC_64_ALIASES.contains(notation.toLowerCase())) {
                return new DefaultArchitecture(notation, ArchitectureInternal.InstructionSet.SPARC, 64);
            }
            if (ARM_ALIASES.contains(notation.toLowerCase())) {
                return new DefaultArchitecture(notation, ArchitectureInternal.InstructionSet.ARM, 32);
            }
            throw new UnsupportedNotationException(notation);
        }
    }

}
