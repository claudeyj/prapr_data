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
package org.gradle.api.internal.artifacts.ivyservice.ivyresolve.parser;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.descriptor.*;
import org.apache.ivy.core.module.descriptor.Configuration.Visibility;
import org.apache.ivy.core.module.id.ArtifactId;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.plugins.matcher.ExactPatternMatcher;
import org.apache.ivy.plugins.matcher.PatternMatcher;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorParser;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.parser.data.PomDependencyMgt;
import org.gradle.api.internal.externalresource.ExternalResource;
import org.gradle.util.DeprecationLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.Map.Entry;


/**
 * This a straight copy of org.apache.ivy.plugins.parser.m2.PomModuleDescriptorBuilder, with minor changes: 1) Do not create artifact for empty classifier. (Previously did so for all non-null
 * classifiers)
 */
public class GradlePomModuleDescriptorBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(GradlePomModuleDescriptorBuilder.class);

    public static final Configuration[] MAVEN2_CONFIGURATIONS = new Configuration[]{
            new Configuration("default", Visibility.PUBLIC,
                    "runtime dependencies and master artifact can be used with this conf",
                    new String[]{"runtime", "master"}, true, null),
            new Configuration("master", Visibility.PUBLIC,
                    "contains only the artifact published by this module itself, "
                            + "with no transitive dependencies",
                    new String[0], true, null),
            new Configuration("compile", Visibility.PUBLIC,
                    "this is the default scope, used if none is specified. "
                            + "Compile dependencies are available in all classpaths.",
                    new String[0], true, null),
            new Configuration("provided", Visibility.PUBLIC,
                    "this is much like compile, but indicates you expect the JDK or a container "
                            + "to provide it. "
                            + "It is only available on the compilation classpath, and is not transitive.",
                    new String[0], true, null),
            new Configuration("runtime", Visibility.PUBLIC,
                    "this scope indicates that the dependency is not required for compilation, "
                            + "but is for execution. It is in the runtime and test classpaths, "
                            + "but not the compile classpath.",
                    new String[]{"compile"}, true, null),
            new Configuration("test", Visibility.PRIVATE,
                    "this scope indicates that the dependency is not required for normal use of "
                            + "the application, and is only available for the test compilation and "
                            + "execution phases.",
                    new String[]{"runtime"}, true, null),
            new Configuration("system", Visibility.PUBLIC,
                    "this scope is similar to provided except that you have to provide the JAR "
                            + "which contains it explicitly. The artifact is always available and is not "
                            + "looked up in a repository.",
                    new String[0], true, null),
            new Configuration("sources", Visibility.PUBLIC,
                    "this configuration contains the source artifact of this module, if any.",
                    new String[0], true, null),
            new Configuration("javadoc", Visibility.PUBLIC,
                    "this configuration contains the javadoc artifact of this module, if any.",
                    new String[0], true, null),
            new Configuration("optional", Visibility.PUBLIC,
                    "contains all optional dependencies", new String[0], true, null)
    };

    static final Map<String, ConfMapper> MAVEN2_CONF_MAPPING = new HashMap<String, ConfMapper>();

    private static final String DEPENDENCY_MANAGEMENT = "m:dependency.management";
    private static final String EXTRA_INFO_DELIMITER = "__";
    private static final Collection<String> JAR_PACKAGINGS = Arrays.asList("ejb", "bundle", "maven-plugin", "eclipse-plugin");
    private final Map<String, String> extraInfo = new HashMap<String, String>();

    static interface ConfMapper {
        public void addMappingConfs(DefaultDependencyDescriptor dd, boolean isOptional);
    }

    static {
        MAVEN2_CONF_MAPPING.put("compile", new ConfMapper() {
            public void addMappingConfs(DefaultDependencyDescriptor dd, boolean isOptional) {
                if (isOptional) {
                    dd.addDependencyConfiguration("optional", "compile(*)");
                    //dd.addDependencyConfiguration("optional", "provided(*)");
                    dd.addDependencyConfiguration("optional", "master(*)");

                } else {
                    dd.addDependencyConfiguration("compile", "compile(*)");
                    //dd.addDependencyConfiguration("compile", "provided(*)");
                    dd.addDependencyConfiguration("compile", "master(*)");
                    dd.addDependencyConfiguration("runtime", "runtime(*)");
                }
            }
        });
        MAVEN2_CONF_MAPPING.put("provided", new ConfMapper() {
            public void addMappingConfs(DefaultDependencyDescriptor dd, boolean isOptional) {
                if (isOptional) {
                    dd.addDependencyConfiguration("optional", "compile(*)");
                    dd.addDependencyConfiguration("optional", "provided(*)");
                    dd.addDependencyConfiguration("optional", "runtime(*)");
                    dd.addDependencyConfiguration("optional", "master(*)");
                } else {
                    dd.addDependencyConfiguration("provided", "compile(*)");
                    dd.addDependencyConfiguration("provided", "provided(*)");
                    dd.addDependencyConfiguration("provided", "runtime(*)");
                    dd.addDependencyConfiguration("provided", "master(*)");
                }
            }
        });
        MAVEN2_CONF_MAPPING.put("runtime", new ConfMapper() {
            public void addMappingConfs(DefaultDependencyDescriptor dd, boolean isOptional) {
                if (isOptional) {
                    dd.addDependencyConfiguration("optional", "compile(*)");
                    dd.addDependencyConfiguration("optional", "provided(*)");
                    dd.addDependencyConfiguration("optional", "master(*)");

                } else {
                    dd.addDependencyConfiguration("runtime", "compile(*)");
                    dd.addDependencyConfiguration("runtime", "runtime(*)");
                    dd.addDependencyConfiguration("runtime", "master(*)");
                }
            }
        });
        MAVEN2_CONF_MAPPING.put("test", new ConfMapper() {
            public void addMappingConfs(DefaultDependencyDescriptor dd, boolean isOptional) {
                //optional doesn't make sense in the test scope
                dd.addDependencyConfiguration("test", "runtime(*)");
                dd.addDependencyConfiguration("test", "master(*)");
            }
        });
        MAVEN2_CONF_MAPPING.put("system", new ConfMapper() {
            public void addMappingConfs(DefaultDependencyDescriptor dd, boolean isOptional) {
                //optional doesn't make sense in the system scope
                dd.addDependencyConfiguration("system", "master(*)");
            }
        });
    }


    private final DefaultModuleDescriptor ivyModuleDescriptor;

    private ModuleRevisionId mrid;

    private DescriptorParseContext parserSettings;

    public GradlePomModuleDescriptorBuilder(ExternalResource res, DescriptorParseContext ivySettings) {
        ivyModuleDescriptor = new DefaultModuleDescriptor(XmlModuleDescriptorParser.getInstance(), null);
        ivyModuleDescriptor.setResolvedPublicationDate(new Date(res.getLastModified()));
        for (Configuration maven2Configuration : MAVEN2_CONFIGURATIONS) {
            ivyModuleDescriptor.addConfiguration(maven2Configuration);
        }
        ivyModuleDescriptor.setMappingOverride(true);
        ivyModuleDescriptor.addExtraAttributeNamespace("m", Ivy.getIvyHomeURL() + "maven");
        parserSettings = ivySettings;
    }

    public DefaultModuleDescriptor getModuleDescriptor() {
        return ivyModuleDescriptor;
    }

    public void setModuleRevId(ModuleRevisionId mrid, String group, String module, String version) {
        this.mrid = mrid;
        ivyModuleDescriptor.setModuleRevisionId(ModuleRevisionId.newInstance(group, module, version));

        if ((version == null) || version.endsWith("SNAPSHOT")) {
            ivyModuleDescriptor.setStatus("integration");
        } else {
            ivyModuleDescriptor.setStatus("release");
        }
    }

    public void setHomePage(String homePage) {
        ivyModuleDescriptor.setHomePage(homePage);
    }

    public void setDescription(String description) {
        ivyModuleDescriptor.setDescription(description);
    }

    public void setLicenses(Iterable<License> licenses) {
        for (License license : licenses) {
            ivyModuleDescriptor.addLicense(license);
        }
    }

    public void addMainArtifact(String artifactId, String packaging) {
        if ("pom".equals(packaging)) {
            DefaultArtifact artifact = new DefaultArtifact(mrid, new Date(), artifactId, "jar", "jar");
            if (parserSettings.artifactExists(artifact)) {
                ivyModuleDescriptor.addArtifact("master", artifact);
            }

            return;
        }

        if (!isKnownJarPackaging(packaging)) {
            DefaultArtifact artifact = new DefaultArtifact(mrid, new Date(), artifactId, packaging, packaging);

            if (parserSettings.artifactExists(artifact)) {
                ivyModuleDescriptor.addArtifact("master", artifact);

                DeprecationLogger.nagUserOfDeprecated("Relying on packaging to define the extension of the main artifact");
                return;
            }
        }

        ivyModuleDescriptor.addArtifact("master", new DefaultArtifact(mrid, new Date(), artifactId, packaging, "jar"));
    }

    private boolean isKnownJarPackaging(String packaging) {
        return "jar".equals(packaging) || JAR_PACKAGINGS.contains(packaging);
    }

    public void addDependency(PomReader.PomDependencyData dep) {
        String scope = dep.getScope();
        if ((scope != null) && (scope.length() > 0) && !MAVEN2_CONF_MAPPING.containsKey(scope)) {
            // unknown scope, defaulting to 'compile'
            scope = "compile";
        }

        String version = dep.getVersion();
        version = (version == null || version.length() == 0) ? getDefaultVersion(dep) : version;
        ModuleRevisionId moduleRevId = ModuleRevisionId.newInstance(dep.getGroupId(), dep.getArtifactId(), version);

        // Some POMs depend on themselves, don't add this dependency: Ivy doesn't allow this!
        // Example: http://repo2.maven.org/maven2/net/jini/jsk-platform/2.1/jsk-platform-2.1.pom
        ModuleRevisionId mRevId = ivyModuleDescriptor.getModuleRevisionId();
        if ((mRevId != null) && mRevId.getModuleId().equals(moduleRevId.getModuleId())) {
            return;
        }

        DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(ivyModuleDescriptor, moduleRevId, true, false, true);
        scope = (scope == null || scope.length() == 0) ? getDefaultScope(dep) : scope;
        ConfMapper mapping = MAVEN2_CONF_MAPPING.get(scope);
        mapping.addMappingConfs(dd, dep.isOptional());
        Map<String, String> extraAtt = new HashMap<String, String>();
        boolean hasClassifier = dep.getClassifier() != null && dep.getClassifier().length() > 0;
        boolean hasNonJarType = dep.getType() != null && !"jar".equals(dep.getType());
        if (hasClassifier || hasNonJarType) {
            String type = "jar";
            if (dep.getType() != null) {
                type = dep.getType();
            }
            String ext = type;

            // if type is 'test-jar', the extension is 'jar' and the classifier is 'tests'
            // Cfr. http://maven.apache.org/guides/mini/guide-attached-tests.html
            if ("test-jar".equals(type)) {
                ext = "jar";
                extraAtt.put("m:classifier", "tests");
            } else if (JAR_PACKAGINGS.contains(type)) {
                ext = "jar";
            }

            // we deal with classifiers by setting an extra attribute and forcing the
            // dependency to assume such an artifact is published
            if (dep.getClassifier() != null) {
                extraAtt.put("m:classifier", dep.getClassifier());
            }
            DefaultDependencyArtifactDescriptor depArtifact = new DefaultDependencyArtifactDescriptor(dd, dd.getDependencyId().getName(), type, ext, null, extraAtt);
            // here we have to assume a type and ext for the artifact, so this is a limitation
            // compared to how m2 behave with classifiers
            String optionalizedScope = dep.isOptional() ? "optional" : scope;
            dd.addDependencyArtifact(optionalizedScope, depArtifact);
        }

        // experimentation shows the following, excluded modules are
        // inherited from parent POMs if either of the following is true:
        // the <exclusions> element is missing or the <exclusions> element
        // is present, but empty.
        List /*<ModuleId>*/ excluded = dep.getExcludedModules();
        if (excluded.isEmpty()) {
            excluded = getDependencyMgtExclusions(dep.getGroupId(), dep.getArtifactId());
        }
        for (Object anExcluded : excluded) {
            ModuleId excludedModule = (ModuleId) anExcluded;
            String[] confs = dd.getModuleConfigurations();
            for (String conf : confs) {
                dd.addExcludeRule(conf, new DefaultExcludeRule(new ArtifactId(
                        excludedModule, PatternMatcher.ANY_EXPRESSION,
                        PatternMatcher.ANY_EXPRESSION,
                        PatternMatcher.ANY_EXPRESSION),
                        ExactPatternMatcher.INSTANCE, null));
            }
        }

        ivyModuleDescriptor.addDependency(dd);
    }

    public void addDependency(DependencyDescriptor descriptor) {
        // Some POMs depend on themselves through their parent POM, don't add this dependency
        // since Ivy doesn't allow this!
        // Example: http://repo2.maven.org/maven2/com/atomikos/atomikos-util/3.6.4/atomikos-util-3.6.4.pom
        ModuleId dependencyId = descriptor.getDependencyId();
        ModuleRevisionId mRevId = ivyModuleDescriptor.getModuleRevisionId();
        if ((mRevId != null) && mRevId.getModuleId().equals(dependencyId)) {
            return;
        }

        ivyModuleDescriptor.addDependency(descriptor);
    }


    public void addDependencyMgt(PomDependencyMgt dep) {
        String key = getDependencyMgtExtraInfoKeyForVersion(dep);
        addExtraInfo(key, dep.getVersion());
        if (dep.getScope() != null) {
            String scopeKey = getDependencyMgtExtraInfoKeyForScope(dep.getGroupId(), dep.getArtifactId());
            addExtraInfo(scopeKey, dep.getScope());
        }
        if (!dep.getExcludedModules().isEmpty()) {
            final String exclusionPrefix = getDependencyMgtExtraInfoPrefixForExclusion(dep.getGroupId(), dep.getArtifactId());
            int index = 0;
            for (Object o : dep.getExcludedModules()) {
                final ModuleId excludedModule = (ModuleId) o;
                addExtraInfo(exclusionPrefix + index,
                        excludedModule.getOrganisation() + EXTRA_INFO_DELIMITER + excludedModule.getName());
                index += 1;
            }
        }
    }

    public void addPlugin(PomDependencyMgt plugin) {
        String pluginValue = plugin.getGroupId() + EXTRA_INFO_DELIMITER + plugin.getArtifactId()
                + EXTRA_INFO_DELIMITER + plugin.getVersion();
        String pluginExtraInfo = extraInfo.get("m:maven.plugins");
        if (pluginExtraInfo == null) {
            pluginExtraInfo = pluginValue;
        } else {
            pluginExtraInfo = pluginExtraInfo + "|" + pluginValue;
        }
        addExtraInfo("m:maven.plugins", pluginExtraInfo);
    }

    public List<PomDependencyMgt> getPlugins(ModuleDescriptor md) {
        List<PomDependencyMgt> result = new ArrayList<PomDependencyMgt>();
        String plugins = (String) md.getExtraInfo().get("m:maven.plugins");
        if (plugins == null) {
            return new ArrayList<PomDependencyMgt>();
        }
        String[] pluginsArray = plugins.split("\\|");
        for (String plugin : pluginsArray) {
            String[] parts = plugin.split(EXTRA_INFO_DELIMITER);
            result.add(new PomPluginElement(parts[0], parts[1], parts[2]));
        }

        return result;
    }

    private static class PomPluginElement implements PomDependencyMgt {
        private String groupId;
        private String artifactId;
        private String version;

        public PomPluginElement(String groupId, String artifactId, String version) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
        }

        public String getGroupId() {
            return groupId;
        }

        public String getArtifactId() {
            return artifactId;
        }

        public String getVersion() {
            return version;
        }

        public String getScope() {
            return null;
        }

        public String getType() {
            return null;
        }

        public String getClassifier() {
            return null;
        }

        public List /*<ModuleId>*/ getExcludedModules() {
            return Collections.EMPTY_LIST; // probably not used?
        }
    }

    private String getDefaultVersion(PomReader.PomDependencyData dep) {
        String key = getDependencyMgtExtraInfoKeyForVersion(dep);
        return extraInfo.get(key);
    }

    private String getDefaultScope(PomReader.PomDependencyData dep) {
        String key = getDependencyMgtExtraInfoKeyForScope(dep.getGroupId(), dep.getArtifactId());
        String result = extraInfo.get(key);
        if ((result == null) || !MAVEN2_CONF_MAPPING.containsKey(result)) {
            result = "compile";
        }
        return result;
    }

    private String getDependencyMgtExtraInfoKeyForVersion(PomDependencyMgt dep) {
        StringBuilder key = new StringBuilder();
        key.append(DEPENDENCY_MANAGEMENT).append(EXTRA_INFO_DELIMITER).append(dep.getGroupId()).append(EXTRA_INFO_DELIMITER).append(dep.getArtifactId());

        if(dep.getType() != null) {
            key.append(EXTRA_INFO_DELIMITER).append(dep.getType());
        }

        if(dep.getClassifier() != null) {
            key.append(EXTRA_INFO_DELIMITER).append(dep.getClassifier());
        }

        key.append(EXTRA_INFO_DELIMITER).append("version");
        return key.toString();
    }

    private String getDependencyMgtExtraInfoKeyForScope(String groupId, String artifaceId) {
        return DEPENDENCY_MANAGEMENT + EXTRA_INFO_DELIMITER + groupId
                + EXTRA_INFO_DELIMITER + artifaceId + EXTRA_INFO_DELIMITER + "scope";
    }

    private String getDependencyMgtExtraInfoPrefixForExclusion(
            String groupId, String artifaceId) {
        return DEPENDENCY_MANAGEMENT + EXTRA_INFO_DELIMITER + groupId
                + EXTRA_INFO_DELIMITER + artifaceId + EXTRA_INFO_DELIMITER + "exclusion_";
    }

    private List<ModuleId> getDependencyMgtExclusions(String groupId, String artifactId) {
        String exclusionPrefix = getDependencyMgtExtraInfoPrefixForExclusion(groupId, artifactId);
        List<ModuleId> exclusionIds = new LinkedList<ModuleId>();
        Map<String, String> extras = extraInfo;
        for (Entry<String, String> entry : extras.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(exclusionPrefix)) {
                String fullExclusion = entry.getValue();
                String[] exclusionParts = fullExclusion.split(EXTRA_INFO_DELIMITER);
                if (exclusionParts.length != 2) {
                    LOGGER.error("Wrong number of parts for dependency management extra info exclusion: expect 2, found {}: {}",
                            exclusionParts.length, fullExclusion);
                    continue;
                }
                exclusionIds.add(ModuleId.newInstance(exclusionParts[0], exclusionParts[1]));
            }
        }

        return exclusionIds;
    }

    private void addExtraInfo(String key, String value) {
        if (!extraInfo.containsKey(key)) {
            extraInfo.put(key, value);
        }
    }
}