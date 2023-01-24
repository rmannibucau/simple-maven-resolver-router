package com.github.rmannibucau.maven.resolver;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.filter.RemoteRepositoryFilter;

import java.util.Objects;
import java.util.function.BiFunction;

public class SimpleRouter implements RemoteRepositoryFilter {
    private final BiFunction<String, String, String> config;
    private final String defaultRepo;

    public SimpleRouter(final BiFunction<String, String, String> configLookup) {
        this.config = configLookup;
        this.defaultRepo = config.apply("defaultRepoId", "central");
    }

    @Override
    public Result acceptArtifact(final RemoteRepository remoteRepository, final Artifact artifact) {
        return accept(remoteRepository.getId(), artifact.getGroupId());
    }

    @Override
    public Result acceptMetadata(final RemoteRepository remoteRepository, final Metadata metadata) {
        return accept(remoteRepository.getId(), metadata.getGroupId());
    }

    private Result accept(final String repoId, final String groupId) {
        return test(repoId, groupId) ?
                new ResultImpl(true, "groupId='" + groupId + "' uses repo='" + repoId + "'") :
                new ResultImpl(false, "groupId='" + groupId + "' does not use repo='" + repoId + "'");
    }

    private boolean test(final String repoId, final String groupId) {
        final var value = config.apply("groups." + groupId, null);
        if (value != null) {
            return Objects.equals(repoId, value);
        }

        final var split = groupId.lastIndexOf('.');
        if (split > 0) {
            return test(repoId, groupId.substring(0, split));
        }

        return Objects.equals(defaultRepo, repoId);
    }

    private static class ResultImpl implements Result {
        private final boolean accepted;
        private final String reasoning;

        private ResultImpl(final boolean accepted, final String reasoning) {
            this.accepted = accepted;
            this.reasoning = reasoning;
        }

        @Override
        public boolean isAccepted() {
            return accepted;
        }

        @Override
        public String reasoning() {
            return reasoning;
        }
    }
}
