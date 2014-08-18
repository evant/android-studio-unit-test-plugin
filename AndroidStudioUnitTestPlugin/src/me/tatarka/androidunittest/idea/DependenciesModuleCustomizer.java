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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
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

        if (selectedTestJavaArtifact == null) return;

        Dependencies dependencies = selectedTestJavaArtifact.getDependencies();
        for (JavaLibrary library : dependencies.getJavaLibraries()) {
            updateDependency(rootModel, library.getJarFile());
        }
        for (AndroidLibrary library : dependencies.getLibraries()) {
            updateDependency(rootModel, library.getFolder());
        }
        for (String project : dependencies.getProjects()) {
            updateDependency(rootModel, project, errorsFound);
        }
    }

    private void updateDependency(@NotNull ModifiableRootModel model, @NotNull File library) {
        updateDependency(model, new LibraryDependency(library, DependencyScope.TEST));
    }

    private void updateDependency(@NotNull ModifiableRootModel model, @NotNull LibraryDependency dependency) {
        Collection<String> binaryPaths = dependency.getPaths(LibraryDependency.PathType.BINARY);

        DependencyOrder order = overridesAndroidDependency(dependency.getName()) ? DependencyOrder.TOP : DependencyOrder.BOTTOM;
        setUpLibraryDependency(model, dependency.getName(), dependency.getScope(), binaryPaths, order);
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
}
