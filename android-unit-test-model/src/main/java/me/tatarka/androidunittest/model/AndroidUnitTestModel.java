package me.tatarka.androidunittest.model;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

public class AndroidUnitTestModel implements AndroidUnitTest, Serializable {
    private final String RPackageName;
    private final List<Variant> variants;

    public AndroidUnitTestModel(String RPackageName, List<Variant> variants) {
        this.RPackageName = RPackageName;
        this.variants = variants;
    }

    @Override
    public String getRPackageName() {
        return RPackageName;
    }

    @Override
    public Collection<Variant> getVariants() {
        return variants;
    }
}
