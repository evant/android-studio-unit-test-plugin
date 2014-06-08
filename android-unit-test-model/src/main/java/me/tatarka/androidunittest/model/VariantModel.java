package me.tatarka.androidunittest.model;

import java.io.File;
import java.io.Serializable;
import java.util.Collection;
import java.util.Set;

public class VariantModel implements Variant, Serializable {
    private final String name;
    private final File manifest;
    private final Set<File> sourceDirectories;
    private final File resourcesDirectory;
    private final File assetsDirectory;
    private final Set<File> javaDependencies;
    private final Set<String> projectDependencies;
    private final File compileDestinationDir;

    VariantModel(String name, File manifest, Set<File> sourceDirectories, File resourcesDirectory, File assetsDirectory, Set<File> javaDependencies, Set<String> projectDependencies, File compileDestinationDir) {
        this.name = name;
        this.manifest = manifest;
        this.sourceDirectories = sourceDirectories;
        this.resourcesDirectory = resourcesDirectory;
        this.assetsDirectory = assetsDirectory;
        this.javaDependencies = javaDependencies;
        this.projectDependencies = projectDependencies;
        this.compileDestinationDir = compileDestinationDir;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public File getManifest() {
        return manifest;
    }

    @Override
    public Collection<File> getSourceDirectories() {
        return sourceDirectories;
    }

    @Override
    public Collection<File> getJavaDependencies() {
        return javaDependencies;
    }

    @Override
    public Collection<String> getProjectDependencies() {
        return projectDependencies;
    }

    @Override
    public File getResourcesDirectory() {
        return resourcesDirectory;
    }

    @Override
    public File getAssetsDirectory() {
        return assetsDirectory;
    }

    @Override
    public File getCompileDestinationDirectory() {
        return compileDestinationDir;
    }
}
