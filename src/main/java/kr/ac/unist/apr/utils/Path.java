package kr.ac.unist.apr.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class related to file/path.
 * @author Youngjae Kim (FreddyYJ)
 */
public class Path {
    /**
     * Get all source files in the given directory.
     * @param rootDirectory root directory to search for source files (e.g. src/main/java for Maven project).
     * @return list of source files in absolute path.
     */
    public static List<String> getAllSources(File rootDirectory) {
        List<String> sources = new ArrayList<>();
        File[] files = rootDirectory.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                sources.addAll(getAllSources(file));
            } else {
                if (file.getName().endsWith(".java"))
                    sources.add(file.getAbsolutePath());
            }
        }

        // Remove classes from this instrumenter
        for (String source : new ArrayList<>(sources)) {
            if (source.contains("kr/ac/unist/apr"))
                sources.remove(source);
        }
        return sources;
    }

    public static String removeSrcPath(String absPath, String srcPath) {
        int length=srcPath.length();
        if (srcPath.endsWith("/")) length--;
        String relPath=absPath.substring(length);
        if (relPath.startsWith("/")) return relPath.substring(1);
        else return relPath;
    }
}
