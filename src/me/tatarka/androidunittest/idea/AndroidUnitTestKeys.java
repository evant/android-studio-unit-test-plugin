package me.tatarka.androidunittest.idea;

import com.android.tools.idea.gradle.AndroidProjectKeys;
import com.intellij.openapi.externalSystem.model.Key;

/**
 * Created by evan on 6/5/14.
 */
public class AndroidUnitTestKeys {
    public static final Key<IdeaAndroidUnitTest> IDEA_ANDROID_UNIT_TEST = Key.create(IdeaAndroidUnitTest.class, AndroidProjectKeys.IDE_ANDROID_PROJECT.getProcessingWeight() + 10);
}
