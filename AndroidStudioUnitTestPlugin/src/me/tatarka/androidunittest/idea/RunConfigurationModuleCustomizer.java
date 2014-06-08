package me.tatarka.androidunittest.idea;

import com.android.tools.idea.gradle.customizer.ModuleCustomizer;
import com.google.common.collect.Maps;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.junit.JUnitConfiguration;
import com.intellij.execution.junit.JUnitConfigurationType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import me.tatarka.androidunittest.model.Variant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Created by evan on 6/7/14.
 */
public class RunConfigurationModuleCustomizer implements ModuleCustomizer<IdeaAndroidUnitTest> {
    @Override
    public void customizeModule(@NotNull Module module, @NotNull Project project, @Nullable IdeaAndroidUnitTest androidUnitTest) {
        if (androidUnitTest == null) return;
        Variant variant = androidUnitTest.getSelectedVariant();
        if (variant == null) return;
        String RPackageName = androidUnitTest.getDelegate().getRPackageName();

        String vmParameters = buildVmParameters(RPackageName, variant);

        RunManager runManager = RunManager.getInstance(project);
        runManager.getConfigurationFactories();
        JUnitConfigurationType junitConfigType = ConfigurationTypeUtil.findConfigurationType(JUnitConfigurationType.class);
        List<RunConfiguration> configs = runManager.getConfigurationsList(junitConfigType);

        for (RunConfiguration config : configs) {
            if (config instanceof JUnitConfiguration) {
                JUnitConfiguration jconfig = (JUnitConfiguration) config;
                jconfig.setVMParameters(vmParameters);
            }
        }

        for (ConfigurationFactory factory : junitConfigType.getConfigurationFactories()) {
            RunnerAndConfigurationSettings settings = runManager.getConfigurationTemplate(factory);
            RunConfiguration config = settings.getConfiguration();
            if (config instanceof JUnitConfiguration) {
                JUnitConfiguration jconfig = (JUnitConfiguration) config;
                jconfig.setVMParameters(vmParameters);
            }
        }
    }

    private static String buildVmParameters(String RPackageName, Variant variant) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> prop : getRobolectricProperties(RPackageName, variant).entrySet()) {
            builder.append("-D").append(prop.getKey()).append("=\"").append(prop.getValue()).append("\" ");
        }
        return builder.toString();
    }

    private static Map<String, String> getRobolectricProperties(String RPackageName, Variant variant) {
        String manifestFile = variant.getManifest().getAbsolutePath();
        String resourcesDirs = variant.getResourcesDirectory().getAbsolutePath();
        String assetsDir = variant.getAssetsDirectory().getAbsolutePath();

        Map<String, String> props = Maps.newHashMap();
        props.put("android.manifest", manifestFile);
        props.put("android.resources", resourcesDirs);
        props.put("android.assets", assetsDir);
        props.put("android.package", RPackageName);
        return props;
    }
}
