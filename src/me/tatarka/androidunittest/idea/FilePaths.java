package me.tatarka.androidunittest.idea;

import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public final class FilePaths {
    private FilePaths() {
    }

    public static boolean isPathInContentEntry(@NotNull File path, @NotNull ContentEntry contentEntry) {
        VirtualFile rootFile = contentEntry.getFile();
        if (rootFile == null) {
            return false;
        }
        File rootFilePath = VfsUtilCore.virtualToIoFile(rootFile);
        return FileUtil.isAncestor(rootFilePath, path, false);
    }

    /**
     * Converts the given path to an URL. The underlying implementation "cheats": it doesn't encode spaces and it just adds the "file"
     * protocol at the beginning of this path. We use this method when creating URLs for file paths that will be included in a module's
     * content root, because converting a URL back to a path expects the path to be constructed the way this method does. To obtain a real
     * URL from a file path, use {@link com.android.utils.SdkUtils#fileToUrl(java.io.File)}.
     *
     * @param path the given path.
     * @return the created URL.
     */
    @NotNull
    public static String pathToIdeaUrl(@NotNull File path) {
        return VfsUtilCore.pathToUrl(FileUtil.toSystemIndependentName(path.getPath()));
    }
}
