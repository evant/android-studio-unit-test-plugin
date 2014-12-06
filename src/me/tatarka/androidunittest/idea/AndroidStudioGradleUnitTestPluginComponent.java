package me.tatarka.androidunittest.idea;

import com.android.tools.idea.gradle.IdeaAndroidProject;
import com.android.tools.idea.gradle.customizer.ModuleCustomizer;
import com.android.tools.idea.gradle.util.Projects;
import com.android.tools.idea.gradle.variant.view.BuildVariantView;
import com.google.common.collect.ImmutableList;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by evan on 6/6/14.
 */
public class AndroidStudioGradleUnitTestPluginComponent implements ProjectComponent {
    private static final Logger LOGGER = Logger.getInstance(AndroidStudioGradleUnitTestPluginComponent.class);

    private final Project myProject;
    private final List<ModuleCustomizer<IdeaAndroidUnitTest>> myCustomizers;

    public AndroidStudioGradleUnitTestPluginComponent(Project project) {
        myProject = project;
        myCustomizers = ImmutableList.of(
                new ContentRootModuleCustomizer(),
                new DependenciesModuleCustomizer(),
                new RunConfigurationModuleCustomizer()
        );
    }

    @Override
    public void initComponent() {
        // TODO: insert component initialization logic here
    }

    @Override
    public void disposeComponent() {
        // TODO: insert component disposal logic here
    }

    @Override
    @NotNull
    public String getComponentName() {
        return "AndroidStudioGradleUnitTestPluginComponent";
    }

    @Override
    public void projectOpened() {
        if (Projects.isGradleProject(myProject)) {
            BuildVariantView.getInstance(myProject).addListener(new BuildVariantView.BuildVariantSelectionChangeListener() {
                @Override
                public void buildVariantSelected(@NotNull final List<AndroidFacet> androidFacets) {
                    ApplicationManager.getApplication().runWriteAction(new Runnable() {
                        @Override
                        public void run() {
                            for (AndroidFacet facet : androidFacets) {
                                updateModule(facet);
                            }
                        }
                    });
                }
            });

            final List<Pair<Module, IdeaAndroidUnitTest>> androidUnitTests = new ArrayList<Pair<Module, IdeaAndroidUnitTest>>();

            Module[] modules = ModuleManager.getInstance(myProject).getModules();
            for (Module module : modules) {
                AndroidFacet androidFacet = AndroidFacet.getInstance(module);
                if (androidFacet != null) {
                    IdeaAndroidProject ideaAndroidProject = androidFacet.getIdeaAndroidProject();
                    if (ideaAndroidProject != null) {
                        String moduleFilePath = module.getModuleFilePath(); // System dependent absolute path.
                        File moduleFile = new File(moduleFilePath);
                        assert moduleFile.getParent() != null : moduleFile.getPath();
                        File moduleRootDirPath = moduleFile.getParentFile();

                        androidUnitTests.add(Pair.create(module, new IdeaAndroidUnitTest(module.getName(), moduleRootDirPath, ideaAndroidProject.getDelegate())));
                    }
                }
            }

            if (!androidUnitTests.isEmpty()) {
                ApplicationManager.getApplication().runWriteAction(new Runnable() {
                    @Override
                    public void run() {
                        for (Pair<Module, IdeaAndroidUnitTest> pair : androidUnitTests) {
                            for (ModuleCustomizer<IdeaAndroidUnitTest> customizer : myCustomizers) {
                                customizer.customizeModule(pair.first, myProject, pair.second);
                            }
                        }
                    }
                });
            }
        }
    }

    private void updateModule(AndroidFacet facet) {
        IdeaAndroidProject androidProject = facet.getIdeaAndroidProject();
        if (androidProject == null) return;
        IdeaAndroidUnitTest androidUnitTest = IdeaAndroidUnitTest.getFromAndroidProject(androidProject.getDelegate());
        if (androidUnitTest == null) return;

        for (ModuleCustomizer<IdeaAndroidUnitTest> customizer : myCustomizers) {
            customizer.customizeModule(facet.getModule(), myProject, androidUnitTest);
        }
    }

    @Override
    public void projectClosed() {
    }
}
