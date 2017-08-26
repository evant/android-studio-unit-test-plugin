package me.tatarka.androidunittest.idea.util;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.io.FileWrapper;
import com.android.io.StreamException;
import com.android.utils.XmlUtils;
import com.android.xml.AndroidManifest;
import com.android.xml.AndroidXPathFactory;
import org.xml.sax.InputSource;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;

/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class DefaultManifestParser implements ManifestParser {

    @Nullable
    @Override
    public String getPackage(@NonNull File manifestFile) {
        return getStringValue(manifestFile, "/manifest/@package");
    }

    @Nullable
    @Override
    public String getVersionName(@NonNull File manifestFile) {
        return getStringValue(manifestFile, "/manifest/@android:versionName");
    }

    @Override
    public int getVersionCode(@NonNull File manifestFile) {
        try {
            String value = getStringValue(manifestFile, "/manifest/@android:versionCode");
            if (value != null) {
                return Integer.valueOf(value);
            }
        } catch (NumberFormatException ignored) {
            // return -1 below.
        }

        return -1;
    }

    @Override
    public Object getMinSdkVersion(@NonNull File manifestFile) {
        try {
            return AndroidManifest.getMinSdkVersion(new FileWrapper(manifestFile));
        } catch (XPathExpressionException e) {
            // won't happen.
        } catch (StreamException e) {
            throw new RuntimeException(e);
        }

        return 1;
    }

    @Override
    public Object getTargetSdkVersion(@NonNull File manifestFile) {
        try {
            return AndroidManifest.getTargetSdkVersion(new FileWrapper(manifestFile));
        } catch (XPathExpressionException e) {
            // won't happen.
        } catch (StreamException e) {
            throw new RuntimeException(e);
        }

        return -1;
    }

    private static String getStringValue(@NonNull File file, @NonNull String xPath) {
        XPath xpath = AndroidXPathFactory.newXPath();

        try {
            InputSource source = new InputSource(XmlUtils.getUtfReader(file));
            return xpath.evaluate(xPath, source);
        } catch (XPathExpressionException e) {
            // won't happen.
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return null;
    }
}
