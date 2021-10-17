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

import org.apache.ivy.core.module.descriptor.*;
import org.apache.ivy.core.module.descriptor.Configuration.Visibility;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.parser.PomReader.PomDependencyData;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.parser.PomReader.PomPluginElement;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.parser.data.MavenDependencyKey;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.parser.data.PomDependencyMgt;
import org.gradle.api.internal.artifacts.metadata.ModuleDescriptorAdapter;
import org.gradle.api.internal.artifacts.metadata.MutableModuleVersionMetaData;
import org.gradle.api.internal.externalresource.LocallyAvailableExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;

/**
 * This a straight copy of org.apache.ivy.plugins.parser.m2.PomModuleDescriptorParser, with one change: we do NOT attempt to retrieve source and javadoc artifacts when parsing the POM. This cuts the
 * number of remote call in half to resolve a module.
 */
public final class GradlePomModuleDescriptorParser extends AbstractModuleDescriptorParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(GradlePomModuleDescriptorParser.class);
    private static final String DEPENDENCY_IMPORT_SCOPE = "import";

    @Override
    protected String getTypeName() {
        return "POM";
    }

    public String toString() {
        return "gradle pom parser";
    }

    protected MutableModuleVersionMetaData doParseDescriptor(DescriptorParseContext parserSettings, LocallyAvailableExternalResource resource, boolean validate) throws IOException, ParseException, SAXException {
        GradlePomModuleDescriptorBuilder mdBuilder = new GradlePomModuleDescriptorBuilder(resource, parserSettings);

        PomReader pomReader = new PomReader(resource);

        doParsePom(parserSettings, mdBuilder, pomReader);

        String artifactId = pomReader.getArtifactId();
        if (pomReader.getRelocation() == null) {
            mdBuilder.addMainArtifact(artifactId, pomReader.getPackaging());
        }

        DefaultModuleDescriptor moduleDescriptor = mdBuilder.getModuleDescriptor();
        ModuleDescriptorAdapter adapter = new ModuleDescriptorAdapter(moduleDescriptor);
        if ("pom".equals(pomReader.getPackaging())) {
            adapter.setMetaDataOnly(true);
        }
        return adapter;
    }

    private void doParsePom(DescriptorParseContext parserSettings, GradlePomModuleDescriptorBuilder mdBuilder, PomReader pomReader) throws IOException, SAXException {
        PomReader parentDescr = null;
        if (pomReader.hasParent()) {
            //Is there any other parent properties?

            ModuleRevisionId parentModRevID = ModuleRevisionId.newInstance(
                    pomReader.getParentGroupId(),
                    pomReader.getParentArtifactId(),
                    pomReader.getParentVersion());
            parentDescr = parseOtherPom(parserSettings, parentModRevID);
            Map<String, String> parentPomProps = parentDescr.getProperties();
            for (Map.Entry<String, String> entry : parentPomProps.entrySet()) {
                pomReader.setProperty(entry.getKey(), entry.getValue());
            }
        }
        pomReader.resolveGAV();

        String groupId = pomReader.getGroupId();
        String artifactId = pomReader.getArtifactId();
        String version = pomReader.getVersion();
        mdBuilder.setModuleRevId(parserSettings.getCurrentRevisionId(), groupId, artifactId, version);

        mdBuilder.setHomePage(pomReader.getHomePage());
        mdBuilder.setDescription(pomReader.getDescription());
        mdBuilder.setLicenses(pomReader.getLicenses());

        ModuleRevisionId relocation = pomReader.getRelocation();

        if (relocation != null) {
            if (groupId != null && artifactId != null
                    && artifactId.equals(relocation.getName())
                    && groupId.equals(relocation.getOrganisation())) {
                LOGGER.error("POM relocation to an other version number is not fully supported in Gradle : {} relocated to {}.",
                        mdBuilder.getModuleDescriptor().getModuleRevisionId(), relocation);
                LOGGER.warn("Please update your dependency to directly use the correct version '{}'.", relocation);
                LOGGER.warn("Resolution will only pick dependencies of the relocated element.  Artifacts and other metadata will be ignored.");
                PomReader relocatedModule = parseOtherPom(parserSettings, relocation);

                Collection<PomDependencyData> pomDependencyDataList = relocatedModule.getDependencies().values();
                for(PomDependencyData pomDependencyData : pomDependencyDataList) {
                    mdBuilder.addDependency(pomDependencyData);
                }

            } else {
                LOGGER.info(mdBuilder.getModuleDescriptor().getModuleRevisionId()
                        + " is relocated to " + relocation
                        + ". Please update your dependencies.");
                LOGGER.debug("Relocated module will be considered as a dependency");
                DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(mdBuilder.getModuleDescriptor(), relocation, true, false, true);
                /* Map all public dependencies */
                Configuration[] m2Confs = GradlePomModuleDescriptorBuilder.MAVEN2_CONFIGURATIONS;
                for (Configuration m2Conf : m2Confs) {
                    if (Visibility.PUBLIC.equals(m2Conf.getVisibility())) {
                        dd.addDependencyConfiguration(m2Conf.getName(), m2Conf.getName());
                    }
                }
                mdBuilder.addDependency(dd);
            }
        } else {
            if (parentDescr != null) {
                // add plugins from parent
                List<PomPluginElement> plugins = parentDescr.getPlugins();
                for(PomPluginElement plugin : plugins) {
                    mdBuilder.addPlugin(plugin);
                }

                pomReader.addInheritedDependencyMgts(parentDescr.getDependencyMgt());
                pomReader.addInheritedDependencies(parentDescr.getDependencies());
            }

            overrideDependencyMgtsWithImported(parserSettings, pomReader);
            addDependencyMgtsToBuilder(mdBuilder, pomReader.getDependencyMgt().values());

            for (PomDependencyData dependency : pomReader.getDependencies().values()) {
                mdBuilder.addDependency(dependency);
            }

            for (Object o : pomReader.getPlugins()) {
                PomPluginElement plugin = (PomPluginElement) o;
                mdBuilder.addPlugin(plugin);
            }
        }
    }

    /**
     * Overrides existing dependency management information with imported ones if existing.
     *
     * @param parseContext Parse context
     * @param pomReader POM reader
     * @throws IOException
     * @throws SAXException
     */
    private void overrideDependencyMgtsWithImported(DescriptorParseContext parseContext, PomReader pomReader) throws IOException, SAXException {
        Map<MavenDependencyKey, PomDependencyMgt> importedDependencyMgts = parseImportedDependencyMgts(parseContext, pomReader.getPomDependencyMgt().values());
        pomReader.addInheritedDependencyMgts(importedDependencyMgts);
    }

    /**
     * Parses imported dependency management information.
     *
     * @param parseContext Parse context
     * @param currentDependencyMgts Current dependency management information
     * @return Imported dependency management information
     * @throws IOException
     * @throws SAXException
     */
    private Map<MavenDependencyKey, PomDependencyMgt> parseImportedDependencyMgts(DescriptorParseContext parseContext, Collection<PomDependencyMgt> currentDependencyMgts) throws IOException, SAXException {
        Map<MavenDependencyKey, PomDependencyMgt> importedDependencyMgts = new LinkedHashMap<MavenDependencyKey, PomDependencyMgt>();

        for(PomDependencyMgt currentDependencyMgt : currentDependencyMgts) {
            if(isDependencyImportScoped(currentDependencyMgt)) {
                PomReader importDescr = parseImportedPom(parseContext, currentDependencyMgt);
                importedDependencyMgts.putAll(importDescr.getDependencyMgt());
            }
        }

        return importedDependencyMgts;
    }

    /**
     * Adds dependency management information to builder. Excludes elements with scope "import".
     *
     * @param mdBuilder Module descriptor builder
     * @param dependencyMgts Dependency management information
     */
    private void addDependencyMgtsToBuilder(GradlePomModuleDescriptorBuilder mdBuilder, Collection<PomDependencyMgt> dependencyMgts) {
        for(PomDependencyMgt dependencyMgt : dependencyMgts) {
            if(!isDependencyImportScoped(dependencyMgt)) {
                mdBuilder.addDependencyMgt(dependencyMgt);
            }
        }
    }

    /**
     * Checks if dependency has scope "import".
     *
     * @param dependencyMgt Dependency management element
     * @return Flag
     */
    private boolean isDependencyImportScoped(PomDependencyMgt dependencyMgt) {
        return DEPENDENCY_IMPORT_SCOPE.equals(dependencyMgt.getScope());
    }

    /**
     * Parses imported POM.
     *
     * @param parseContext Parse context
     * @param pomDependencyMgt Dependency management information
     * @return POM reader
     * @throws IOException
     * @throws SAXException
     */
    private PomReader parseImportedPom(DescriptorParseContext parseContext, PomDependencyMgt pomDependencyMgt) throws IOException, SAXException {
        ModuleRevisionId importModRevID = ModuleRevisionId.newInstance(pomDependencyMgt.getGroupId(), pomDependencyMgt.getArtifactId(), pomDependencyMgt.getVersion());
        return parseOtherPom(parseContext, importModRevID);
    }

    /**
     * Parses other POM.
     *
     * @param parseContext Parse context
     * @param parentModRevID Parent module revision ID
     * @return POM reader
     * @throws IOException
     * @throws SAXException
     */
    private PomReader parseOtherPom(DescriptorParseContext parseContext, ModuleRevisionId parentModRevID) throws IOException, SAXException {
        Artifact pomArtifact = DefaultArtifact.newPomArtifact(parentModRevID, new Date());
        LocallyAvailableExternalResource localResource = parseContext.getArtifact(pomArtifact);
        GradlePomModuleDescriptorBuilder mdBuilder = new GradlePomModuleDescriptorBuilder(localResource, parseContext);
        PomReader pomReader = new PomReader(localResource);
        doParsePom(parseContext, mdBuilder, pomReader);
        return pomReader;
    }
}
