package com.dong.buildprops;

import org.apache.commons.io.FileUtils;
import org.gradle.api.Project;
import org.gradle.api.internal.AbstractTask;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import static com.dong.buildprops.BuildPropsPlugin.getGeneratedSourcesDir;

/**
 * The task for {@code Build.java} source file generating.
 */
public class BuildPropsGenerator extends AbstractTask {

    @TaskAction
    public void run() throws IOException {
        final Project project = getProject();
        final Object group = project.getGroup();
        final String pkg = String.valueOf(group);
        final String path = pkg.replace(".", File.separator) + File.separator + "Build.java";
        try (final PrintWriter printer = new PrintWriter(FileUtils.openOutputStream(new File(getOutput(), path)))) {
            printer.println("/**");
            printer.println(" * DO NOT MODIFY! This file is generated automatically.");
            printer.println(" */");
            printer.printf ("package %s;", pkg).println();
            printer.println();
            printer.println("public interface Build {");
            printer.println();
            printer.printf ("    String GROUP = \"%s\";", group).println();
            printer.printf ("    String ARTIFACT = \"%s\";", project.getName()).println();
            printer.printf ("    String VERSION = \"%s\";", project.getVersion()).println();
            printer.printf ("    String REVISION = \"%s\";", VersionControlSystem.getInstance(project).getRevision()).println();
            printer.println();
            printer.println("}");
        }
    }

    @OutputDirectory
    public File getOutput() {
        return getGeneratedSourcesDir(getProject(), getProject().getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME));
    }

}

