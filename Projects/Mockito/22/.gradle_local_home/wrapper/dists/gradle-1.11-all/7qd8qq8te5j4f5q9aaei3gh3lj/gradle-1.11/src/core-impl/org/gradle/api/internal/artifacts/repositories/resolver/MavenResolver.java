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
package org.gradle.api.internal.artifacts.repositories.resolver;

import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DefaultArtifact;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.id.ArtifactRevisionId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.plugins.matcher.PatternMatcher;
import org.gradle.api.internal.artifacts.ModuleMetadataProcessor;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.BuildableModuleVersionMetaDataResolveResult;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.ModuleSource;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.parser.GradlePomModuleDescriptorParser;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.strategy.LatestStrategy;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.strategy.ResolverStrategy;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.strategy.VersionMatcher;
import org.gradle.api.internal.artifacts.repositories.transport.RepositoryTransport;
import org.gradle.api.internal.externalresource.local.LocallyAvailableResourceFinder;
import org.gradle.api.internal.resource.ResourceNotFoundException;
import org.gradle.api.resources.ResourceException;
import org.gradle.util.DeprecationLogger;
import org.gradle.util.WrapUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.*;

public class MavenResolver extends ExternalResourceResolver implements PatternBasedResolver {
    private static final Logger LOGGER = LoggerFactory.getLogger(MavenResolver.class);
    private final RepositoryTransport transport;
    private final String root;
    private final List<String> artifactRoots = new ArrayList<String>();
    private String pattern = MavenPattern.M2_PATTERN;
    private boolean usepoms = true;
    private boolean useMavenMetadata = true;
    private final MavenMetadataLoader mavenMetaDataLoader;

    public MavenResolver(String name, URI rootUri, RepositoryTransport transport,
                         LocallyAvailableResourceFinder<ArtifactRevisionId> locallyAvailableResourceFinder,
                         ModuleMetadataProcessor metadataProcessor, VersionMatcher versionMatcher,
                         LatestStrategy latestStrategy, ResolverStrategy resolverStrategy) {
        super(name, transport.getRepository(),
                new ChainedVersionLister(new MavenVersionLister(transport.getRepository()), new ResourceVersionLister(transport.getRepository())),
                locallyAvailableResourceFinder, new GradlePomModuleDescriptorParser(), metadataProcessor, resolverStrategy, versionMatcher, latestStrategy);
        transport.configureCacheManager(this);

        this.mavenMetaDataLoader = new MavenMetadataLoader(transport.getRepository());
        this.transport = transport;
        this.root = transport.convertToPath(rootUri);

        super.setM2compatible(true);

        // SNAPSHOT revisions are changing revisions
        setChangingMatcher(PatternMatcher.REGEXP);
        setChangingPattern(".*-SNAPSHOT");

        updatePatterns();
    }

    protected void getDependency(DependencyDescriptor dd, BuildableModuleVersionMetaDataResolveResult result) {
        if (isSnapshotVersion(dd)) {
            getSnapshotDependency(dd, result);
        } else {
            super.getDependency(dd, result);
        }
    }

    private void getSnapshotDependency(DependencyDescriptor dd, BuildableModuleVersionMetaDataResolveResult result) {
        final ModuleRevisionId dependencyRevisionId = dd.getDependencyRevisionId();
        final String uniqueSnapshotVersion = findUniqueSnapshotVersion(dependencyRevisionId);
        if (uniqueSnapshotVersion != null) {
            DependencyDescriptor enrichedDependencyDescriptor = enrichDependencyDescriptorWithSnapshotVersionInfo(dd, dependencyRevisionId, uniqueSnapshotVersion);
            findStaticDependency(enrichedDependencyDescriptor, result);
            if (result.getState() == BuildableModuleVersionMetaDataResolveResult.State.Resolved) {
                result.setModuleSource(new TimestampedModuleSource(uniqueSnapshotVersion));
            }
        } else {
            findStaticDependency(dd, result);
        }
    }

    private DependencyDescriptor enrichDependencyDescriptorWithSnapshotVersionInfo(DependencyDescriptor dd, ModuleRevisionId dependencyRevisionId, String uniqueSnapshotVersion) {
        Map<String, String> extraAttributes = new HashMap<String, String>(1);
        extraAttributes.put("timestamp", uniqueSnapshotVersion);
        final ModuleRevisionId newModuleRevisionId = ModuleRevisionId.newInstance(dependencyRevisionId.getOrganisation(), dependencyRevisionId.getName(), dependencyRevisionId.getRevision(), extraAttributes);
        return dd.clone(newModuleRevisionId);
    }

    private boolean isSnapshotVersion(DependencyDescriptor dd) {
        return dd.getDependencyRevisionId().getRevision().endsWith("SNAPSHOT");
    }

    protected File download(Artifact artifact, ModuleSource moduleSource) throws IOException {
        if (moduleSource instanceof TimestampedModuleSource) {
            TimestampedModuleSource timestampedModuleSource = (TimestampedModuleSource) moduleSource;
            String timestampedVersion = timestampedModuleSource.getTimestampedVersion();
            return downloadTimestampedVersion(artifact, timestampedVersion);
        } else {
            return download(artifact);
        }
    }

    private File downloadTimestampedVersion(Artifact artifact, String timestampedVersion) throws IOException {
        final ModuleRevisionId artifactModuleRevisionId = artifact.getModuleRevisionId();
        final ModuleRevisionId moduleRevisionId = ModuleRevisionId.newInstance(artifactModuleRevisionId.getOrganisation(),
                artifactModuleRevisionId.getName(),
                artifactModuleRevisionId.getRevision(),
                WrapUtil.toMap("timestamp", timestampedVersion));
        final Artifact artifactWithResolvedModuleRevisionId = DefaultArtifact.cloneWithAnotherMrid(artifact, moduleRevisionId);
        return download(artifactWithResolvedModuleRevisionId);
    }

    public void addArtifactLocation(URI baseUri, String pattern) {
        if (pattern != null && pattern.length() > 0) {
            throw new IllegalArgumentException("Maven Resolver only supports a single pattern. It cannot be provided on a per-location basis.");
        }
        artifactRoots.add(transport.convertToPath(baseUri));

        updatePatterns();
    }

    public void addDescriptorLocation(URI baseUri, String pattern) {
        throw new UnsupportedOperationException("Cannot have multiple descriptor urls for MavenResolver");
    }

    private String getWholePattern() {
        return root + pattern;
    }

    private void updatePatterns() {
        if (isUsepoms()) {
            setIvyPatterns(Collections.singletonList(getWholePattern()));
        } else {
            setIvyPatterns(Collections.<String>emptyList());
        }

        List<String> artifactPatterns = new ArrayList<String>();
        artifactPatterns.add(getWholePattern());
        for (String artifactRoot : artifactRoots) {
            artifactPatterns.add(artifactRoot + pattern);
        }
        setArtifactPatterns(artifactPatterns);
    }

    @Override
    protected Artifact getMetaDataArtifactFor(ModuleRevisionId moduleRevisionId) {
        if (isUsepoms()) {
            ArtifactRevisionId artifactRevisionId = ArtifactRevisionId.newInstance(moduleRevisionId, moduleRevisionId.getName(), "pom", "pom", moduleRevisionId.getExtraAttributes());
            return new DefaultArtifact(artifactRevisionId, null, null, true);
        }

        return null;
    }

    private String findUniqueSnapshotVersion(ModuleRevisionId moduleRevisionId) {
        Artifact pomArtifact = DefaultArtifact.newPomArtifact(moduleRevisionId, new Date());
        String metadataLocation = toResourcePattern(getWholePattern()).toModuleVersionPath(pomArtifact) + "/maven-metadata.xml";
        MavenMetadata mavenMetadata = parseMavenMetadata(metadataLocation);

        if (mavenMetadata.timestamp != null) {
            // we have found a timestamp, so this is a snapshot unique version
            String rev = moduleRevisionId.getRevision();
            rev = rev.substring(0, rev.length() - "SNAPSHOT".length());
            rev = rev + mavenMetadata.timestamp + "-" + mavenMetadata.buildNumber;
            return rev;
        }
        return null;
    }

    private MavenMetadata parseMavenMetadata(String metadataLocation) {
        if (shouldUseMavenMetadata(pattern)) {
            try {
                return mavenMetaDataLoader.load(metadataLocation);
            } catch (ResourceNotFoundException e) {
                return new MavenMetadata();
            } catch (ResourceException e) {
                LOGGER.warn("impossible to access Maven metadata file, ignored.", e);
            }
        }
        return new MavenMetadata();
    }

    // A bunch of configuration properties that we don't (yet) support in our model via the DSL. Users can still tweak these on the resolver using mavenRepo().
    public boolean isUsepoms() {
        return usepoms;
    }

    public void setUsepoms(boolean usepoms) {
        this.usepoms = usepoms;
        updatePatterns();
    }

    public boolean isUseMavenMetadata() {
        return useMavenMetadata;
    }

    @Deprecated
    public void setUseMavenMetadata(boolean useMavenMetadata) {
        DeprecationLogger.nagUserOfDiscontinuedMethod("MavenResolver.setUseMavenMetadata(boolean)");
        this.useMavenMetadata = useMavenMetadata;
        if (useMavenMetadata) {
            this.versionLister = new ChainedVersionLister(
                    new MavenVersionLister(getRepository()),
                    new ResourceVersionLister(getRepository()));
        } else {
            this.versionLister = new ResourceVersionLister(getRepository());
        }
    }

    private boolean shouldUseMavenMetadata(String pattern) {
        return isUseMavenMetadata() && pattern.endsWith(MavenPattern.M2_PATTERN);
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        if (pattern == null) {
            throw new NullPointerException("pattern must not be null");
        }
        this.pattern = pattern;
        updatePatterns();
    }

    public String getRoot() {
        return root;
    }

    public void setRoot(String root) {
        throw new UnsupportedOperationException("Cannot configure root on mavenRepo. Use 'url' property instead.");
    }

    @Override
    public void setM2compatible(boolean compatible) {
        if (!compatible) {
            throw new IllegalArgumentException("Cannot set m2compatible = false on mavenRepo.");
        }
    }

    protected static class TimestampedModuleSource implements ModuleSource {
        public String getTimestampedVersion() {
            return timestampedVersion;
        }

        private final String timestampedVersion;

        public TimestampedModuleSource(String uniqueSnapshotVersion) {
            this.timestampedVersion = uniqueSnapshotVersion;
        }
    }
}
