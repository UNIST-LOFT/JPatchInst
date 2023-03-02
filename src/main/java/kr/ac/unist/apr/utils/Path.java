package kr.ac.unist.apr.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Path {
    public static List<String> getAllSources(File rootDirectory) {
        List<String> sources = new ArrayList<>();
        File[] files = rootDirectory.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                sources.addAll(getAllSources(rootDirectory));
            } else {
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
    // TODO: Classpath (e.g. target/dependency) and Sourcepath (e.g. src/main/java)
    
}
