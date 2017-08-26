package me.tatarka.androidunittest.idea;

import com.android.builder.model.JavaArtifact;
import com.android.builder.model.SourceProvider;
import com.android.builder.model.Variant;
import com.android.tools.idea.gradle.IdeaAndroidProject;
import com.android.tools.idea.gradle.customizer.ModuleCustomizer;
import com.android.tools.idea.gradle.run.MakeBeforeRunTask;
import com.android.tools.idea.gradle.run.MakeBeforeRunTaskProvider;
import com.android.tools.idea.startup.AndroidStudioSpecificInitializer;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Collections2;
import com.google.common.collect.Maps;
import com.intellij.compiler.options.CompileStepBeforeRun;
import com.intellij.execution.*;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.execution.configurations.ModuleBasedConfiguration;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.junit.JUnitConfiguration;
import com.intellij.execution.junit.JUnitConfigurationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import me.tatarka.androidunittest.idea.util.DefaultManifestParser;
import me.tatarka.androidunittest.idea.util.ManifestParser;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.android.run.AndroidRunConfiguration;
import org.jetbrains.android.run.AndroidRunConfigurationType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created by evan on 6/7/14.
 */
public class RunConfigurationModuleCustomizer implements ModuleCustomizer<IdeaAndroidUnitTest> {
    private static final Logger LOG = Logger.getInstance(AbstractContentRootModuleCustomizer.class);

    public static void setupRunManagerListener(Project project) {
        RunManagerEx runManager = RunManagerEx.getInstanceEx(project);
        runManager.addRunManagerListener(new RunManagerAdapter() {
            @Override
            public void runConfigurationAdded(@NotNull RunnerAndConfigurationSettings settings) {
                RunConfiguration config = settings.getConfiguration();
                if (config instanceof ModuleBasedConfiguration) {
                    ModuleBasedConfiguration moduleConfig = (ModuleBasedConfiguration) config;
                    Collection<Module> validModules = moduleConfig.getValidModules();
                    for (Module module : validModules) {
                        if (isRelevantRunConfig(module, moduleConfig, JUnitConfiguration.class)) {
                            configureJUnitConfig(module, (JUnitConfiguration) moduleConfig);
                            break;
                        }
                    }
                }
            }
        });
    }

    @Override
    public void customizeModule(@NotNull Module module, @NotNull Project project, @Nullable IdeaAndroidUnitTest androidUnitTest) {
        if (androidUnitTest == null) {
            return;
        }
        final JavaArtifact selectedTestJavaArtifact = androidUnitTest.getSelectedTestJavaArtifact(module);
        if (selectedTestJavaArtifact == null) {
            return;
        }
        Variant androidVariant = androidUnitTest.getSelectedAndroidVariant(module);

        String RPackageName = findRPackageName(androidUnitTest);

        String vmParameters = buildVmParameters(module, RPackageName, selectedTestJavaArtifact, androidVariant);

        RunManagerEx runManager = RunManagerEx.getInstanceEx(project);
        JUnitConfigurationType junitConfigType = ConfigurationTypeUtil.findConfigurationType(JUnitConfigurationType.class);
        List<RunConfiguration> configs = runManager.getConfigurationsList(junitConfigType);

        for (RunConfiguration config : configs) {
            if (isRelevantRunConfig(module, config, JUnitConfiguration.class)) {
                JUnitConfiguration jconfig = (JUnitConfiguration) config;
                jconfig.setVMParameters(vmParameters);
                setupMakeTask(project, module, androidVariant, runManager, jconfig);
            }
        }
    }

    private static void configureJUnitConfig(Module module, JUnitConfiguration jconfig) {
        AndroidFacet androidFacet = AndroidFacet.getInstance(module);
        if (androidFacet == null) {
            return;
        }

        IdeaAndroidProject ideaAndroidProject = androidFacet.getIdeaAndroidProject();
        if (ideaAndroidProject == null) {
            return;
        }

        IdeaAndroidUnitTest androidUnitTest = IdeaAndroidUnitTest.getFromAndroidProject(ideaAndroidProject.getDelegate());
        if (androidUnitTest == null) {
            return;
        }

        final JavaArtifact selectedTestJavaArtifact = androidUnitTest.getSelectedTestJavaArtifact(module);
        if (selectedTestJavaArtifact == null) {
            return;
        }
        Variant androidVariant = androidUnitTest.getSelectedAndroidVariant(module);

        String RPackageName = findRPackageName(androidUnitTest);

        String vmParameters = buildVmParameters(module, RPackageName, selectedTestJavaArtifact, androidVariant);

        jconfig.setVMParameters(vmParameters);
        jconfig.setWorkingDirectory(FilePaths.moduleRootPath(module).getPath());
        setupMakeTask(module.getProject(), module, androidVariant, RunManagerEx.getInstanceEx(module.getProject()), jconfig);
    }

    private static void setupMakeTask(@NotNull Project project, @NotNull Module module, @NotNull Variant androidVariant, @NotNull RunManagerEx runManager, @NotNull JUnitConfiguration jconfig) {
        if (AndroidStudioSpecificInitializer.isAndroidStudio()) {
            setupAndroidStudioMakeTask(project, module, androidVariant, runManager, jconfig);
        } else {
//            setupIntellijMakeTask(project, module, androidVariant, runManager, jconfig);
        }
    }

    private static void setupAndroidStudioMakeTask(@NotNull Project project, @NotNull Module module, @NotNull Variant androidVariant, @NotNull RunManagerEx runManager, @NotNull JUnitConfiguration jconfig) {
        MakeBeforeRunTask makeBeforeRunTask = null;
        List<BeforeRunTask> beforeRunTasks = runManager.getBeforeRunTasks(jconfig);

        for (BeforeRunTask task : beforeRunTasks) {
            if (task instanceof MakeBeforeRunTask) {
                makeBeforeRunTask = (MakeBeforeRunTask) task;
                break;
            }
        }

        if (makeBeforeRunTask == null) {
            BeforeRunTaskProvider<MakeBeforeRunTask> makeBeforeRunProvider = BeforeRunTaskProvider.getProvider(project, MakeBeforeRunTaskProvider.ID);
            AndroidRunConfigurationType androidRunConfigType = ConfigurationTypeUtil.findConfigurationType(AndroidRunConfigurationType.class);
            List<RunConfiguration> configs = runManager.getConfigurationsList(androidRunConfigType);

            AndroidRunConfiguration androidRunConfig = null;
            for (RunConfiguration config : configs) {
                if (config instanceof AndroidRunConfiguration) {
                    // Any android run configuration will do, just need one to create the task.
                    androidRunConfig = (AndroidRunConfiguration) config;
                    break;
                }
            }

            if (androidRunConfig != null) {
                makeBeforeRunTask = makeBeforeRunProvider.createTask(androidRunConfig);
            }
            replaceStandardMakeTask(beforeRunTasks, makeBeforeRunTask);
        }

        if (makeBeforeRunTask != null) {
            makeBeforeRunTask.setEnabled(true);
            makeBeforeRunTask.setGoal(getTestBuildName(androidVariant));
            runManager.setBeforeRunTasks(jconfig, beforeRunTasks, false);
        }
    }

    /** TODO: we can do something similar in intellij by using and external gradle tasks but then you
     *  get a "ClassNotFoundException" in android studio. Is there a way to get around that?
     *
     *  Alternativly, the Gradle-Aware Make task should work in intellii, but the android plugin
     *  explicitly disables it. Would duplicating the task be worth it?
     *
     *  For now, do neither of these things as steering people to use the gradle test runner my be a
     *  better option.
     */
//    private static void setupIntellijMakeTask(@NotNull Project project, @NotNull Module module, @NotNull Variant androidVariant, @NotNull RunManagerEx runManager, @NotNull JUnitConfiguration jconfig) {
//        ExternalSystemBeforeRunTask gradleBeforeRunTask = null;
//        List<BeforeRunTask> beforeRunTasks = runManager.getBeforeRunTasks(jconfig);
//
//        for (BeforeRunTask task : beforeRunTasks) {
//            if (task.getProviderId() == GradleBeforeRunTaskProvider.ID) {
//                gradleBeforeRunTask = (ExternalSystemBeforeRunTask) task;
//                break;
//            }
//        }
//
//        if (gradleBeforeRunTask == null) {
//            BeforeRunTaskProvider<ExternalSystemBeforeRunTask> gradleBeforeRunProvider = GradleBeforeRunTaskProvider.getProvider(project, GradleBeforeRunTaskProvider.ID);
//            if (gradleBeforeRunProvider != null) {
//                gradleBeforeRunTask = gradleBeforeRunProvider.createTask(jconfig);
//                replaceStandardMakeTask(beforeRunTasks, gradleBeforeRunTask);
//            }
//        }
//
//        if (gradleBeforeRunTask != null) {
//            gradleBeforeRunTask.setEnabled(true);
//            gradleBeforeRunTask.getTaskExecutionSettings().setTaskNames(Collections.singletonList(getTestBuildName(androidVariant)));
//            gradleBeforeRunTask.getTaskExecutionSettings().setExternalProjectPath(FilePaths.moduleRootPath(module).getPath());
//            runManager.setBeforeRunTasks(jconfig, beforeRunTasks, false);
//        }
//    }
    
    private static void replaceStandardMakeTask(List<BeforeRunTask> tasks, BeforeRunTask newTask) {
        for (int i = tasks.size() - 1; i >= 0; i--) {
            BeforeRunTask task = tasks.get(i);
            if (task.getProviderId() == CompileStepBeforeRun.ID && task instanceof CompileStepBeforeRun.MakeBeforeRunTask) {
                tasks.remove(i);
                break;
            }
        }
        tasks.add(newTask);
    }

    private static String getTestBuildName(Variant variant) {
        return "test" + StringUtils.capitalize(variant.getName()) + "Classes";
    }

    private static String findRPackageName(IdeaAndroidUnitTest androidUnitTest) {
        String packageName = androidUnitTest.getAndroidDelegate().getDefaultConfig().getProductFlavor().getApplicationId();
        if (packageName == null) {
            File manifestFile = androidUnitTest.getAndroidDelegate().getDefaultConfig().getSourceProvider().getManifestFile();
            ManifestParser parser = new DefaultManifestParser();
            packageName = parser.getPackage(manifestFile);
        }
        return packageName;
    }

    private static boolean isRelevantRunConfig(Module module, RunConfiguration config, Class<? extends ModuleBasedConfiguration> runConfigClass) {
        if (!config.getClass().isAssignableFrom(runConfigClass)) return false;
        for (Module m : ((ModuleBasedConfiguration) config).getModules()) {
            if (m == module) return true;
        }
        return false;
    }

    private static String buildVmParameters(Module module, String RPackageName, JavaArtifact testJavaArtifact, Variant androidVariant) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> prop : getRobolectricProperties(RPackageName, testJavaArtifact).entrySet()) {
            builder.append("-D").append(prop.getKey()).append("=\"").append(prop.getValue()).append("\" ");
        }
        return builder.toString();
    }

    private static Map<String, String> getRobolectricProperties(String RPackageName, JavaArtifact testJavaArtifact) {
        SourceProvider sourceProvider = testJavaArtifact.getVariantSourceProvider();
        String manifestFile = sourceProvider.getManifestFile().getAbsolutePath();
        String resourcesDirs = fileCollectionToPath(sourceProvider.getResDirectories());
        String assetsDir = fileCollectionToPath(sourceProvider.getAssetsDirectories());

        Map<String, String> props = Maps.newHashMap();
        props.put("android.manifest", manifestFile);
        props.put("android.resources", resourcesDirs);
        props.put("android.assets", assetsDir);
        props.put("android.package", RPackageName);
        return props;
    }

    private static String fileCollectionToPath(Collection<File> files) {
        return Joiner.on(File.pathSeparatorChar).join(Collections2.transform(files, new Function<File, String>() {
            @javax.annotation.Nullable
            @Override
            public String apply(@Nullable File file) {
                return file.getAbsolutePath();
            }
        }));
    }
}
