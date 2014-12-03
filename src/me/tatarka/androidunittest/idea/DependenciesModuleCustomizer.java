package me.tatarka.androidunittest.idea;

import com.android.builder.model.AndroidLibrary;
import com.android.builder.model.Dependencies;
import com.android.builder.model.JavaArtifact;
import com.android.builder.model.JavaLibrary;
import com.android.tools.idea.gradle.dependency.LibraryDependency;
import com.android.tools.idea.gradle.dependency.ModuleDependency;
import com.android.tools.idea.gradle.facet.AndroidGradleFacet;
import com.android.tools.idea.gradle.messages.Message;
import com.google.common.base.Objects;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.roots.DependencyScope;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleOrderEntry;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.impl.libraries.ProjectLibraryTable;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

import static com.android.tools.idea.gradle.messages.CommonMessageGroupNames.FAILED_TO_SET_UP_DEPENDENCIES;

/**
 * Created by evan on 6/6/14.
 */
public class DependenciesModuleCustomizer extends AbstractDependenciesModuleCustomizer<IdeaAndroidUnitTest> {
    private static final Logger LOG = Logger.getInstance(AbstractDependenciesModuleCustomizer.class);

    @Override
    protected void setUpDependencies(@NotNull ModifiableRootModel rootModel, @NotNull IdeaAndroidUnitTest androidUnitTest, @NotNull List<Message> errorsFound) {
        JavaArtifact selectedTestJavaArtifact = androidUnitTest.getSelectedTestJavaArtifact();
        if (selectedTestJavaArtifact == null) {
            return;
        }

        Dependencies dependencies = selectedTestJavaArtifact.getDependencies();
        for (JavaLibrary library : dependencies.getJavaLibraries()) {
            updateDependency(rootModel, library.getJarFile());
        }
        for (AndroidLibrary library : getLibraries(dependencies)) {
            updateDependency(rootModel, library.getFolder());
        }
        for (String project : dependencies.getProjects()) {
            updateDependency(rootModel, project, errorsFound);
        }

        updateDependenciesWithJavadocSources(rootModel, androidUnitTest);
    }

    private void updateDependency(@NotNull ModifiableRootModel model, @NotNull File library) {
        LibraryDependency libraryDependency = new LibraryDependency(library, DependencyScope.TEST);
        updateDependency(model, libraryDependency);
    }


    private void updateDependency(@NotNull ModifiableRootModel model, @NotNull LibraryDependency dependency) {
        Collection<String> binaryPaths = dependency.getPaths(LibraryDependency.PathType.BINARY);
        setUpLibraryDependency(model, dependency.getName(), dependency.getScope(), binaryPaths, getDependencyOrder(dependency));
    }

    private void updateDependenciesWithJavadocSources(@NotNull ModifiableRootModel model, @NotNull IdeaAndroidUnitTest androidUnitTest) {
        LibraryTable libraryTable = ProjectLibraryTable.getInstance(model.getProject());

        for (Library library : libraryTable.getLibraries()) {
            Collection<String> sourcesPaths = androidUnitTest.getSourcesPaths(library.getName());
            Collection<String> javadocPaths = androidUnitTest.getJavadocPaths(library.getName());

            updateLibrarySourcesIfAbsent(library, sourcesPaths, OrderRootType.SOURCES);
            updateLibrarySourcesIfAbsent(library, javadocPaths, OrderRootType.DOCUMENTATION);
        }
    }

    private static DependencyOrder getDependencyOrder(LibraryDependency dependency) {
        return getDependencyOrder(dependency.getName());
    }

    private static DependencyOrder getDependencyOrder(String dependencyName) {
        return overridesAndroidDependency(dependencyName) ? DependencyOrder.TOP : DependencyOrder.BOTTOM;
    }

    private static boolean overridesAndroidDependency(String name) {
        return name.startsWith("junit-");
    }

    private void updateDependency(@NotNull ModifiableRootModel model,
                                  @Nullable String project,
                                  @NotNull List<Message> errorsFound) {
        if (project == null || project.isEmpty()) return;
        ModuleDependency dependency = new ModuleDependency(project, DependencyScope.TEST);

        ModuleManager moduleManager = ModuleManager.getInstance(model.getProject());
        Module moduleDependency = null;
        for (Module module : moduleManager.getModules()) {
            AndroidGradleFacet androidGradleFacet = AndroidGradleFacet.getInstance(module);
            if (androidGradleFacet != null) {
                String gradlePath = androidGradleFacet.getConfiguration().GRADLE_PROJECT_PATH;
                if (Objects.equal(gradlePath, dependency.getGradlePath())) {
                    moduleDependency = module;
                    break;
                }
            }
        }
        if (moduleDependency != null) {
            ModuleOrderEntry orderEntry = model.addModuleOrderEntry(moduleDependency);
            orderEntry.setExported(true);
            return;
        }

        LibraryDependency backup = dependency.getBackupDependency();
        boolean hasLibraryBackup = backup != null;
        String msg = String.format("Unable to find module with Gradle path '%1$s'.", dependency.getGradlePath());

        Message.Type type = Message.Type.ERROR;
        if (hasLibraryBackup) {
            msg += String.format(" Linking to library '%1$s' instead.", backup.getName());
            type = Message.Type.WARNING;
        }

        LOG.info(msg);

        errorsFound.add(new Message(FAILED_TO_SET_UP_DEPENDENCIES, type, msg));

        // fall back to library dependency, if available.
        if (hasLibraryBackup) {
            updateDependency(model, backup);
        }
    }

    /**
     * The android plugin changed {@link com.android.builder.model.Dependencies#getLibraries()} from
     * returning a {@link java.util.List} to returning a {@link java.util.Collection}. Therefore, to
     * be compatible, we will link against the latest version and fall back to reflection.
     */
    private static Collection<AndroidLibrary> getLibraries(Dependencies dependencies) {
        try {
            return dependencies.getLibraries();
        } catch (NoSuchMethodError e) {
            try {
                Method method = dependencies.getClass().getMethod("getLibraries");
                return (List<AndroidLibrary>) method.invoke(dependencies);
            } catch (NoSuchMethodException e1) {
                throw new RuntimeException(e1);
            } catch (InvocationTargetException e1) {
                throw new RuntimeException(e1);
            } catch (IllegalAccessException e1) {
                throw new RuntimeException(e1);
            }
        }
    }
}
