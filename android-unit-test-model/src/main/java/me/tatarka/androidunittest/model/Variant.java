package me.tatarka.androidunittest.model;

import org.gradle.tooling.model.Model;

import java.io.File;
import java.util.Collection;

/**
 * Information about a single android build variant. A model that provides information to the
 * AndroidUnitTestPlugin so that it can update the ide state.
 */
public interface Variant extends Model {
    /**
     * The name of the variant.
     *
     * @return the variant name
     */
    String getName();

    /**
     * The merged manifest for the variant.
     *
     * @return the manifest
     */
    File getManifest();

    /**
     * The test source derectories for the variant.
     *
     * @return the source directoreis
     */
    Collection<File> getSourceDirectories();

    /**
     * The test java gradle dependencies for the variant.
     *
     * @return the java dependencies
     */
    Collection<File> getJavaDependencies();

    /**
     * The test module depdencencics for the variant.
     *
     * @return the projecect dependencies
     */
    Collection<String> getProjectDependencies();

    /**
     * The test output build directory for the variant.
     *
     * @return the compile destination directory
     */
    File getCompileDestinationDirectory();

    /**
     * The merged resources directory for the variant.
     *
     * @return the resources directory
     */
    File getResourcesDirectory();

    /**
     * The merged assets directory for the variant.
     *
     * @return the assets directory
     */
    File getAssetsDirectory();
}
