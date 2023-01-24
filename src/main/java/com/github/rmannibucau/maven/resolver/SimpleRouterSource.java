package com.github.rmannibucau.maven.resolver;

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.filter.RemoteRepositoryFilter;
import org.eclipse.aether.spi.connector.filter.RemoteRepositoryFilterSource;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.function.BiFunction;

@Singleton
@Named("com.github.rmannibucau.maven.resolver.SimpleRouterSource")
public class SimpleRouterSource implements RemoteRepositoryFilterSource {
    private static final String KEY = SimpleRouterSource.class.getName() + ".instance";

    private static final RemoteRepositoryFilter DISABLED = new RemoteRepositoryFilter() {
        @Override
        public Result acceptArtifact(final RemoteRepository remoteRepository, final Artifact artifact) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Result acceptMetadata(final RemoteRepository remoteRepository, final Metadata metadata) {
            throw new UnsupportedOperationException();
        }
    };

    @Override
    public RemoteRepositoryFilter getRemoteRepositoryFilter(final RepositorySystemSession repositorySystemSession) {
        final var cached = (RemoteRepositoryFilter) repositorySystemSession.getData().get(KEY);
        if (cached != null && cached != DISABLED) {
            return cached;
        }

        final var configProperties = repositorySystemSession.getConfigProperties();
        final var systemProperties = repositorySystemSession.getSystemProperties();
        final var allProps = new HashMap<>(configProperties);
        allProps.putAll(systemProperties);

        final BiFunction<String, String, String> configLookup = (key, fallback) -> {
            final var confKey = "rmannibucau.maven-resolver.simple-router." + key;
            final var value = allProps.getOrDefault(confKey, fallback);
            return value != null ? value.toString() : null;
        };

        if (!Boolean.parseBoolean(configLookup.apply("enabled", "true"))) {
            return null;
        }
        return (RemoteRepositoryFilter) repositorySystemSession.getData().computeIfAbsent(KEY, () -> new SimpleRouter(configLookup));
    }
}
