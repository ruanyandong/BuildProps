package com.dong.buildprops;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.UnknownTaskException;
import org.gradle.api.plugins.Convention;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.plugins.PluginManager;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.compile.GroovyCompile;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.plugins.ide.idea.model.IdeaModel;
import org.gradle.plugins.ide.idea.model.IdeaModule;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Gradle plugin for {@code Build.java} source code generating.
 */
public class BuildPropsPlugin implements Plugin<Project> {

    private static final String PLUGIN_ID = "buildprops";

    private static final String GENERATED_SOURCE_ROOT = "generated" + File.separator + "source" + File.separator + PLUGIN_ID;

    @Override
    public void apply(final Project project) {
        final TaskContainer tasks = project.getTasks();
        final PluginManager plugins = project.getPluginManager();

        if (!plugins.hasPlugin("idea")) {
            plugins.apply("idea");
        }

        final Convention convention = project.getConvention();
        final JavaPluginConvention javaPlugin = convention.getPlugin(JavaPluginConvention.class);
        final SourceSetContainer sourceSets = javaPlugin.getSourceSets();
        final BuildPropsGenerator buildProps = tasks.create("buildProps", BuildPropsGenerator.class, it -> it.getOutputs().upToDateWhen(spec -> false));

        project.afterEvaluate(p -> {
            configureIdeaModule(project, sourceSets);

            sourceSets.stream().filter(it -> SourceSet.MAIN_SOURCE_SET_NAME.equals(it.getName())).forEach(sourceSet -> {
                try {
                    final JavaCompile compileJava = (JavaCompile) tasks.getByName(sourceSet.getCompileTaskName("java")).dependsOn(buildProps);
                    compileJava.setSource(project.files(compileJava.getSource(), buildProps.getOutput()));
                } catch (final UnknownTaskException ignore) {
                }

                try {
                    final GroovyCompile compileGroovy = (GroovyCompile) tasks.getByName(sourceSet.getCompileTaskName("groovy")).dependsOn(buildProps);
                    compileGroovy.setSource(project.files(compileGroovy.getSource(), buildProps.getOutput()));
                } catch (final UnknownTaskException ignore) {
                }
            });
        });
    }

    static File getGeneratedSourcesDir(final Project project, final SourceSet sourceSet) {
        return new File(project.getBuildDir(), GENERATED_SOURCE_ROOT + File.separator + sourceSet.getName());
    }

    private static void configureIdeaModule(final Project project, final SourceSetContainer sourceSets) {
        final SourceSet mainSourceSet = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        final SourceSet testSourceSet = sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME);
        final File mainGeneratedSourcesDir = getGeneratedSourcesDir(project, mainSourceSet);
        final File testGeneratedSourcesDir = getGeneratedSourcesDir(project, testSourceSet);
        final IdeaModule ideaModule = project.getExtensions().getByType(IdeaModel.class).getModule();
        ideaModule.setExcludeDirs(getIdeaExcludeDirs(project, getGeneratedSourceDirs(project, sourceSets), ideaModule));
        ideaModule.setSourceDirs(project.files(ideaModule.getSourceDirs(), mainGeneratedSourcesDir).getFiles());
        ideaModule.setTestSourceDirs(project.files(ideaModule.getTestSourceDirs(), testGeneratedSourcesDir).getFiles());
        ideaModule.setGeneratedSourceDirs(project.files(ideaModule.getGeneratedSourceDirs(), mainGeneratedSourcesDir, testGeneratedSourcesDir).getFiles());
    }

    private static Set<File> getGeneratedSourceDirs(final Project project, final SourceSetContainer sourceSets) {
        final Set<File> excludes = new LinkedHashSet<>();
        sourceSets.forEach(sourceSet -> {
            for (File f = getGeneratedSourcesDir(project, sourceSet); f != null && !f.equals(project.getProjectDir()); f = f.getParentFile()) {
                excludes.add(f);
            }
        });
        return excludes;
    }

    private static Set<File> getIdeaExcludeDirs(final Project project, final Set<File> excludes, final IdeaModule ideaModule) {
        final Set<File> excludeDirs = new LinkedHashSet<>(ideaModule.getExcludeDirs());
        if (excludes.contains(project.getBuildDir()) && excludeDirs.contains(project.getBuildDir())) {
            excludeDirs.remove(project.getBuildDir());

            try {
                Files.list(project.getBuildDir().toPath()).map(Path::toFile).filter(File::isDirectory).forEach(excludeDirs::add);
            } catch (final IOException e) {
            }
        }

        excludeDirs.removeAll(excludes);
        return excludeDirs;
    }

}

