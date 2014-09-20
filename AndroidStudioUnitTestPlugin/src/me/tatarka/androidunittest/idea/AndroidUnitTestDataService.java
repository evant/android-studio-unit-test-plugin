package me.tatarka.androidunittest.idea;

import com.android.tools.idea.gradle.GradleSyncState;
import com.android.tools.idea.gradle.customizer.ModuleCustomizer;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.service.project.manage.ProjectDataService;
import com.intellij.openapi.externalSystem.util.DisposeAwareProjectChange;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created by evan on 6/4/14.
 */
public class AndroidUnitTestDataService implements ProjectDataService<IdeaAndroidUnitTest, Void> {
    private static final Logger LOG = Logger.getInstance(AndroidUnitTestDataService.class);
    private final List<ModuleCustomizer<IdeaAndroidUnitTest>> myCustomizers;

    public AndroidUnitTestDataService() {
        myCustomizers = ImmutableList.of(
                new ContentRootModuleCustomizer(),
                new DependenciesModuleCustomizer(),
                new RunConfigurationModuleCustomizer()
        );
    }

    @NotNull
    @Override
    public Key<IdeaAndroidUnitTest> getTargetDataKey() {
        return AndroidUnitTestKeys.IDEA_ANDROID_UNIT_TEST;
    }

    @Override
    public void importData(@NotNull Collection<DataNode<IdeaAndroidUnitTest>> toImport, @NotNull Project project, boolean synchronous) {
        if (!toImport.isEmpty()) {
            try {
                doImport(toImport, project, synchronous);
            } catch (RuntimeException e) {
                LOG.info(String.format("Failed to set up Android modules in project '%1$s'", project.getName()), e);
                if (e.getMessage() != null) {
                    GradleSyncState.getInstance(project).syncFailed(e.getMessage());
                } else {
                    GradleSyncState.getInstance(project).syncFailed("Unknown Error");
                }
            }
        }
    }

    @Override
    public void removeData(@NotNull Collection<? extends Void> voids, @NotNull Project project, boolean b) {

    }

    private void doImport(@NotNull final Collection<DataNode<IdeaAndroidUnitTest>> toImport, @NotNull final Project project, boolean synchronous) {
        ExternalSystemApiUtil.executeProjectChangeAction(synchronous, new DisposeAwareProjectChange(project) {
            @Override
            public void execute() {
                Map<String, IdeaAndroidUnitTest> androidProjectsByModuleName = indexByModuleName(toImport);
                ModuleManager moduleManager = ModuleManager.getInstance(project);
                for (Module module : moduleManager.getModules()) {
                    IdeaAndroidUnitTest androidProject = androidProjectsByModuleName.get(module.getName());
                    customizeModule(module, project, androidProject);
                }
            }
        });
    }

    private void customizeModule(@NotNull Module module, @NotNull Project project, @Nullable IdeaAndroidUnitTest androidProject) {
        if (androidProject == null) return;
        for (ModuleCustomizer<IdeaAndroidUnitTest> customizer : myCustomizers) {
            customizer.customizeModule(module, project, androidProject);
        }
    }

    @NotNull
    private static Map<String, IdeaAndroidUnitTest> indexByModuleName(@NotNull Collection<DataNode<IdeaAndroidUnitTest>> dataNodes) {
        Map<String, IdeaAndroidUnitTest> index = Maps.newHashMap();
        for (DataNode<IdeaAndroidUnitTest> d : dataNodes) {
            IdeaAndroidUnitTest androidProject = d.getData();
            index.put(androidProject.getModuleName(), androidProject);
        }
        return index;
    }
}
