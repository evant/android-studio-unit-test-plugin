package me.tatarka.androidunittest.model;

import org.gradle.tooling.model.Model;

import java.util.Collection;

/**
 * A model that provides information to the AndroidUnitTestPlugin so that it can update the ide
 * state.
 */
public interface AndroidUnitTest extends Model {
    /**
     * The package name where R is generated, this is the same as your main application maifest
     *
     * @return the R package name
     */
    String getRPackageName();

    /**
     * A collection of all variants for the android project
     *
     * @return the variants
     */
    Collection<Variant> getVariants();
}
