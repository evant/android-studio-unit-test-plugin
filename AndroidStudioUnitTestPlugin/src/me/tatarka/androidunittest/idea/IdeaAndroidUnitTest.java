package me.tatarka.androidunittest.idea;

import com.android.builder.model.AndroidProject;
import com.google.common.collect.Maps;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import me.tatarka.androidunittest.model.AndroidUnitTest;
import me.tatarka.androidunittest.model.Variant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Created by evan on 6/5/14.
 */
public class IdeaAndroidUnitTest implements Serializable {
    private static WeakHashMap<AndroidProject, IdeaAndroidUnitTest> map = new WeakHashMap<AndroidProject, IdeaAndroidUnitTest>();

    @NotNull private final String myModuleName;
    @NotNull private final VirtualFile myRootDir;
    @NotNull private final AndroidUnitTest myDelegate;
    @NotNull private final AndroidProject myAndroidDelegate;
    @Nullable private String mySelectedVariantName;

    @NotNull private Map<String, Variant> myVariantsByName = Maps.newHashMap();
    @NotNull private Map<String, com.android.builder.model.Variant> myAndroidVariantsByName = Maps.newHashMap();

    public IdeaAndroidUnitTest(@NotNull String moduleName, @NotNull File rootDir, @NotNull AndroidUnitTest delegate, @NotNull AndroidProject androidDelegate, @NotNull String selectedVariantName) {
        myModuleName = moduleName;
        VirtualFile found = VfsUtil.findFileByIoFile(rootDir, true);
        assert found != null;
        myRootDir = found;
        myDelegate = delegate;
        myAndroidDelegate = androidDelegate;

        populateVariantsByName();

        setSelectedVariantName(selectedVariantName);
        map.put(androidDelegate, this);
    }

    private void populateVariantsByName() {
        for (Variant variant : myDelegate.getVariants()) {
            myVariantsByName.put(variant.getName(), variant);
        }
        for (com.android.builder.model.Variant variant : myAndroidDelegate.getVariants()) {
            myAndroidVariantsByName.put(variant.getName(), variant);
        }
    }

    /** * Updates the name of the selected build variant. If the given name does not belong to an existing variant, this method will pick up
     * the first variant, in alphabetical order.
     *
     * @param name the new name.
     */
    public void setSelectedVariantName(@NotNull String name) {
        Collection<String> variantNames = getVariantNames();
        String newVariantName = null;
        if (variantNames.contains(name)) {
            newVariantName = name;
        }
        mySelectedVariantName = newVariantName;
    }

    /**
     * @return the selected build variant.
     */
    @Nullable
    public Variant getSelectedVariant() {
        return myVariantsByName.get(mySelectedVariantName);
    }

    @Nullable
    public com.android.builder.model.Variant getSelectedAndroidVariant() {
        return myAndroidVariantsByName.get(mySelectedVariantName);
    }

    @NotNull
    public Collection<String> getVariantNames() {
        return myVariantsByName.keySet();
    }

    @NotNull
    public String getModuleName() { return myModuleName; }

    @NotNull
    public AndroidUnitTest getDelegate() { return myDelegate; }

    @NotNull
    public AndroidProject getAndroidDelegate() { return myAndroidDelegate; }

    @NotNull
    public VirtualFile getRootDir() { return myRootDir; }

    @Nullable
    public static IdeaAndroidUnitTest getFromAndroidProject(AndroidProject androidProject) {
        return map.get(androidProject);
    }
}
