package com.dong.buildprops;

import com.google.common.collect.ImmutableMap;
import org.codehaus.groovy.runtime.ProcessGroovyMethods;
import org.gradle.api.Project;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.Objects;

/**
 * An abstraction of VersionControlSystem
 */
public abstract class VersionControlSystem {

    private static final Map<String, VersionControlSystem> SUPPORTED_VCS = ImmutableMap.of("git", new Git(), "svn", new Svn());

    private static final VersionControlSystem DEFAULT = new VersionControlSystem() {
        @Override
        public String getRevision() {
            return "";
        }
    };

    /**
     * Returns an instance of {@link VersionControlSystem}
     *
     * @param project an instance of {@link Project}
     * @return an instance of {@link VersionControlSystem}
     */
    public static VersionControlSystem getInstance(final Project project) {
        try {
            System.out.println("project.getRootDir().toPath() "+project.getRootDir().toPath());
            return Files.list(project.getRootDir().toPath())
                    .map(it -> it.toFile().getName())
                    .filter(it-> it.startsWith("."))
                    .map(it -> SUPPORTED_VCS.get(it.substring(1)))
                    .filter(Objects::nonNull)
                    .findAny().orElse(DEFAULT);
        } catch (final IOException e) {
            return DEFAULT;
        }
    }

    /**
     * Returns the revision of current project
     */
    public abstract String getRevision();

    private static final class Git extends VersionControlSystem {

        private Git() {
        }

        @Override
        public String getRevision() {
            try {
                return ProcessGroovyMethods.getText(ProcessGroovyMethods.execute("git rev-parse HEAD")).trim();
            } catch (IOException e) {
                return "";
            }
        }

    }

    private static final class Svn extends VersionControlSystem {

        private Svn() {
        }

        @Override
        public String getRevision() {
            try {
                return ProcessGroovyMethods.getText(ProcessGroovyMethods.execute("svnversion")).trim();
            } catch (IOException e) {
                return "";
            }
        }

    }

}

