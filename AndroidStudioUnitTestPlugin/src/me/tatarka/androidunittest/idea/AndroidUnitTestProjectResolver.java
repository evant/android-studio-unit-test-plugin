package me.tatarka.androidunittest.idea;

import com.android.builder.model.AndroidProject;
import com.android.builder.model.Variant;
import com.android.tools.idea.gradle.util.GradleBuilds;
import com.android.tools.idea.startup.AndroidStudioSpecificInitializer;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants;
import com.intellij.openapi.externalSystem.util.Order;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.containers.ContainerUtil;
import org.gradle.tooling.model.gradle.GradleScript;
import org.gradle.tooling.model.idea.IdeaModule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension;

import java.io.File;
import java.util.*;

/**
 * Created by evan on 6/4/14.
 */
@Order(ExternalSystemConstants.UNORDERED)
public class AndroidUnitTestProjectResolver extends AbstractProjectResolverExtension {
    @Override
    public void populateModuleExtraModels(@NotNull IdeaModule gradleModule, @NotNull DataNode<ModuleData> ideModule) {
        GradleScript buildScript = null;
        try {
            buildScript = gradleModule.getGradleProject().getBuildScript();
        } catch (UnsupportedOperationException ignore) {}

        nextResolver.populateModuleExtraModels(gradleModule, ideModule);

        if (buildScript == null || !inAndroidGradleProject(gradleModule)) {
            return;
        }

        File moduleFilePath = new File(FileUtil.toSystemDependentName(ideModule.getData().getModuleFilePath()));
        File moduleRootDirPath = moduleFilePath.getParentFile();

        AndroidProject androidProject = resolverCtx.getExtraProject(gradleModule, AndroidProject.class);

        if (androidProject != null) {
            Variant selectedVariant = getVariantToSelect(androidProject);
            IdeaAndroidUnitTest ideaAndroidUnitTest =  new IdeaAndroidUnitTest(gradleModule.getName(), moduleRootDirPath, androidProject, selectedVariant.getName());
            ideModule.createChild(AndroidUnitTestKeys.IDEA_ANDROID_UNIT_TEST, ideaAndroidUnitTest);
        }
    }

    @NotNull
    private static Variant getVariantToSelect(@NotNull AndroidProject androidProject) {
        Collection<Variant> variants = androidProject.getVariants();
        if (variants.size() == 1) {
            Variant variant = ContainerUtil.getFirstItem(variants);
            assert variant != null;
            return variant;
        }
        // look for "debug" variant. This is just a little convenience for the user that has not created any additional flavors/build types.
        // trying to match something else may add more complexity for little gain.
        for (Variant variant : variants) {
            if ("debug".equals(variant.getName())) {
                return variant;
            }
        }
        List<Variant> sortedVariants = Lists.newArrayList(variants);
        Collections.sort(sortedVariants, new Comparator<Variant>() {
            @Override
            public int compare(Variant o1, Variant o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        return sortedVariants.get(0);
    }

    @Override
    @NotNull
    public Set<Class> getExtraProjectModelClasses() {
        return Sets.<Class>newHashSet(AndroidProject.class);
    }

    @Override
    public void populateModuleDependencies(@NotNull IdeaModule gradleModule, @NotNull DataNode<ModuleData> ideModule, @NotNull DataNode<ProjectData> ideProject) {
        super.populateModuleDependencies(gradleModule, ideModule, ideProject);
    }

    // Indicates it is an "Android" project if at least one module has an AndroidProject.
    private boolean inAndroidGradleProject(@NotNull IdeaModule gradleModule) {
        if (!resolverCtx.findModulesWithModel(AndroidProject.class).isEmpty()) {
            return true;
        }
        if (GradleBuilds.BUILD_SRC_FOLDER_NAME.equals(gradleModule.getGradleProject().getName()) && AndroidStudioSpecificInitializer.isAndroidStudio()) {
            // For now, we will "buildSrc" to be considered part of an Android project. We need changes in IDEA to make this distinction better.
            // Currently, when processing "buildSrc" we don't have access to the rest of modules in the project, making it impossible to tell
            // if the project has at least one Android module.
            return true;
        }
        return false;
    }
}
