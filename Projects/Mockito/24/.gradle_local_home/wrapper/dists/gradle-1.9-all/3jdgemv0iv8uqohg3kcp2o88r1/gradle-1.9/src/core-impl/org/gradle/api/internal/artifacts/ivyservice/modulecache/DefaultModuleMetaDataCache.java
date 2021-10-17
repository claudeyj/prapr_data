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
package org.gradle.api.internal.artifacts.ivyservice.modulecache;

import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.internal.artifacts.ModuleVersionIdentifierSerializer;
import org.gradle.api.internal.artifacts.ivyservice.CacheLockingManager;
import org.gradle.api.internal.artifacts.ivyservice.DependencyToModuleVersionResolver;
import org.gradle.api.internal.artifacts.ivyservice.IvyXmlModuleDescriptorWriter;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.ModuleSource;
import org.gradle.api.internal.artifacts.metadata.ModuleVersionMetaData;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.ModuleVersionRepository;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.parser.IvyXmlModuleDescriptorParser;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.strategy.ResolverStrategy;
import org.gradle.cache.PersistentIndexedCache;
import org.gradle.internal.resource.local.LocallyAvailableResource;
import org.gradle.messaging.serialize.*;
import org.gradle.util.BuildCommencedTimeProvider;
import org.gradle.internal.hash.HashValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;

public class DefaultModuleMetaDataCache implements ModuleMetaDataCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultModuleMetaDataCache.class);

    private final BuildCommencedTimeProvider timeProvider;
    private final CacheLockingManager cacheLockingManager;

    private final ModuleDescriptorStore moduleDescriptorStore;
    private PersistentIndexedCache<RevisionKey, ModuleDescriptorCacheEntry> cache;

    public DefaultModuleMetaDataCache(BuildCommencedTimeProvider timeProvider, CacheLockingManager cacheLockingManager, ResolverStrategy resolverStrategy) {
        this.timeProvider = timeProvider;
        this.cacheLockingManager = cacheLockingManager;

        moduleDescriptorStore = new ModuleDescriptorStore(cacheLockingManager.createMetaDataStore(), new IvyXmlModuleDescriptorWriter(), new IvyXmlModuleDescriptorParser(resolverStrategy));
    }

    private PersistentIndexedCache<RevisionKey, ModuleDescriptorCacheEntry> getCache() {
        if (cache == null) {
            cache = initCache();
        }
        return cache;
    }

    private PersistentIndexedCache<RevisionKey, ModuleDescriptorCacheEntry> initCache() {
        return cacheLockingManager.createCache("module-metadata.bin", new RevisionKeySerializer(), new ModuleDescriptorCacheEntrySerializer());
    }

    public CachedMetaData getCachedModuleDescriptor(ModuleVersionRepository repository, ModuleVersionIdentifier moduleVersionIdentifier, DependencyToModuleVersionResolver resolver) {
        ModuleDescriptorCacheEntry moduleDescriptorCacheEntry = getCache().get(createKey(repository, moduleVersionIdentifier));
        if (moduleDescriptorCacheEntry == null) {
            return null;
        }
        if (moduleDescriptorCacheEntry.isMissing) {
            return new DefaultCachedMetaData(moduleDescriptorCacheEntry, null, timeProvider);
        }
        ModuleDescriptor descriptor = moduleDescriptorStore.getModuleDescriptor(repository, moduleVersionIdentifier);
        if (descriptor == null) {
            // Descriptor file has been manually deleted - ignore the entry
            return null;
        }
        return new DefaultCachedMetaData(moduleDescriptorCacheEntry, descriptor, timeProvider);
    }

    public CachedMetaData cacheMissing(ModuleVersionRepository repository, ModuleVersionIdentifier id) {
        return cacheModuleDescriptor(repository, id, null, null, false);
    }

    public CachedMetaData cacheMetaData(ModuleVersionRepository repository, ModuleVersionMetaData metaData, ModuleSource moduleSource) {
        return cacheModuleDescriptor(repository, metaData.getId(), metaData.getDescriptor(), moduleSource, metaData.isChanging());
    }

    public CachedMetaData cacheModuleDescriptor(ModuleVersionRepository repository, ModuleVersionIdentifier moduleVersionIdentifier, ModuleDescriptor moduleDescriptor, ModuleSource moduleSource, boolean isChanging) {
        ModuleDescriptorCacheEntry entry;
        if (moduleDescriptor == null) {
            LOGGER.debug("Recording absence of module descriptor in cache: {} [changing = {}]", moduleVersionIdentifier, isChanging);
            entry = createMissingEntry(isChanging);
            getCache().put(createKey(repository, moduleVersionIdentifier), entry);
        } else {
            LOGGER.debug("Recording module descriptor in cache: {} [changing = {}]", moduleDescriptor.getModuleRevisionId(), isChanging);
            LocallyAvailableResource resource = moduleDescriptorStore.putModuleDescriptor(repository, moduleDescriptor);
            entry = createEntry(isChanging, resource.getSha1(), moduleSource);
            getCache().put(createKey(repository, moduleVersionIdentifier), entry);
        }
        return new DefaultCachedMetaData(entry, null, timeProvider);
    }

    private RevisionKey createKey(ModuleVersionRepository repository, ModuleVersionIdentifier moduleVersionIdentifier) {
        return new RevisionKey(repository.getId(), moduleVersionIdentifier);
    }

    private ModuleDescriptorCacheEntry createMissingEntry(boolean changing) {
        return new ModuleDescriptorCacheEntry(changing, true, timeProvider.getCurrentTime(), BigInteger.ZERO, null);
    }

    private ModuleDescriptorCacheEntry createEntry(boolean changing, HashValue moduleDescriptorHash, ModuleSource moduleSource) {
        return new ModuleDescriptorCacheEntry(changing, false, timeProvider.getCurrentTime(), moduleDescriptorHash.asBigInteger(), moduleSource);
    }

    private static class RevisionKey {
        private final String repositoryId;
        private final ModuleVersionIdentifier moduleVersionIdentifier;

        private RevisionKey(String repositoryId, ModuleVersionIdentifier moduleVersionIdentifier) {
            this.repositoryId = repositoryId;
            this.moduleVersionIdentifier = moduleVersionIdentifier;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || !(o instanceof RevisionKey)) {
                return false;
            }
            RevisionKey other = (RevisionKey) o;
            return repositoryId.equals(other.repositoryId) && moduleVersionIdentifier.equals(other.moduleVersionIdentifier);
        }

        @Override
        public int hashCode() {
            return repositoryId.hashCode() ^ moduleVersionIdentifier.hashCode();
        }
    }

    private static class RevisionKeySerializer implements Serializer<RevisionKey> {
        private final ModuleVersionIdentifierSerializer identifierSerializer = new ModuleVersionIdentifierSerializer();

        public void write(Encoder encoder, RevisionKey value) throws Exception {
            encoder.writeString(value.repositoryId);
            identifierSerializer.write(encoder, value.moduleVersionIdentifier);
        }

        public RevisionKey read(Decoder decoder) throws Exception {
            String resolverId = decoder.readString();
            ModuleVersionIdentifier identifier = identifierSerializer.read(decoder);
            return new RevisionKey(resolverId, identifier);
        }
    }

    private static class ModuleDescriptorCacheEntrySerializer implements Serializer<ModuleDescriptorCacheEntry> {
        private final DefaultSerializer<ModuleSource> moduleSourceSerializer = new DefaultSerializer<ModuleSource>(ModuleSource.class.getClassLoader());

        public void write(Encoder encoder, ModuleDescriptorCacheEntry value) throws Exception {
            encoder.writeBoolean(value.isMissing);
            encoder.writeBoolean(value.isChanging);
            encoder.writeLong(value.createTimestamp);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            OutputStreamBackedEncoder sourceEncoder = new OutputStreamBackedEncoder(outputStream);
            moduleSourceSerializer.write(sourceEncoder, value.moduleSource);
            sourceEncoder.flush();
            byte[] serializedModuleSource = outputStream.toByteArray();
            encoder.writeBinary(serializedModuleSource);
            byte[] hash = value.moduleDescriptorHash.toByteArray();
            encoder.writeBinary(hash);
        }

        public ModuleDescriptorCacheEntry read(Decoder decoder) throws Exception {
            boolean isMissing = decoder.readBoolean();
            boolean isChanging = decoder.readBoolean();
            long createTimestamp = decoder.readLong();
            byte[] serializedModuleSource = decoder.readBinary();
            ModuleSource moduleSource = moduleSourceSerializer.read(new InputStreamBackedDecoder(new ByteArrayInputStream(serializedModuleSource)));
            byte[] encodedHash = decoder.readBinary();
            BigInteger hash = new BigInteger(encodedHash);
            return new ModuleDescriptorCacheEntry(isChanging, isMissing, createTimestamp, hash, moduleSource);
        }
    }
}
