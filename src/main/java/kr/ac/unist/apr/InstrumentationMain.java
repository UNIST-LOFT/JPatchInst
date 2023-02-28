package kr.ac.unist.apr;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import soot.Pack;
import soot.PackManager;
import soot.Scene;
import soot.Transform;
import soot.options.Options;

public class InstrumentationMain {
    public static void main(String[] args) {
        String classPath = args[0];
        String targetPath=args[1];
        System.out.println("ClassPath: " + classPath);
        System.out.println("TargetPath: " + targetPath);
        instrumentWithSoot(classPath,targetPath);
    }

    private static void instrumentWithSoot(String classPath,String targetPath) {
        String classPathSeparator = ":";
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            classPathSeparator = ";";
        }

        /*Set the soot-classpath to include the helper class and class to analyze*/
        Options.v().set_prepend_classpath(true);
        Options.v().set_soot_classpath(Scene.v().defaultClassPath() + classPathSeparator + classPath + classPathSeparator+targetPath);
        ArrayList<String> dirs=new ArrayList<>();
        dirs.add(targetPath);

        // Options.v().set_src_prec(Options.src_prec_class);
        Options.v().set_app(true);
        // Options.v().set_whole_program(true);
        Options.v().set_allow_phantom_refs(true);

        // we set the soot output dir to target/classes so that the instrumented class can override the class file
        Options.v().set_output_dir(targetPath);
        Options.v().set_unfriendly_mode(true);
        List<String> excluded=new ArrayList<>();
        excluded.add("java.*");
        excluded.add("sun.*");
        excluded.add("javax.*");
        excluded.add("com.sun.*");
        excluded.add("kr.ac.unist.apr.*");
        Options.v().set_exclude(excluded);

        List<String> dynamicDirs=new ArrayList<>();
        List<File> libPath=getAllLibraries("/usr/lib/jvm/java-8-openjdk-amd64/jre");
        libPath.addAll(getAllLibraries("/usr/lib/jvm/java-8-openjdk-amd64/lib"));
        libPath.addAll(getAllLibraries("/root/project/APR-instrumenter/build/classes"));
        for (File file : libPath) {
            dynamicDirs.add(file.getAbsolutePath());
        }
        Options.v().set_dynamic_dir(dynamicDirs);

        // List<String> dynamicPkgs=new ArrayList<>();
        // dynamicPkgs.add("/root/project/APR-instrumenter/build/classes/kr/ac/unist/apr/GlobalStates.class");
        // Options.v().set_dynamic_class(dynamicPkgs);

        // retain line numbers
        Options.v().set_keep_line_number(true);
        // retain the original variable names
        Options.v().setPhaseOption("jb", "use-original-names:true");

        /* add a phase to transformer pack by call Pack.add */
        Pack jtp = PackManager.v().getPack("jtp");

        Instrumentation instrumenter = new Instrumentation();
        jtp.add(new Transform("jtp.instrumenter", instrumenter));

        List<String> dirNames=new ArrayList<>();
        dirNames.add(targetPath);
        Options.v().set_process_dir(dirNames);

        List<File> classFiles = getClassFiles(targetPath);
        List<String> classNames=new ArrayList<>();
        for (File sootClass : classFiles) {
            String initPath=sootClass.getAbsolutePath();
            int index=initPath.indexOf(targetPath);
            initPath=initPath.substring(index+targetPath.length());
            if (initPath.startsWith("/")){
                initPath=initPath.substring(1);
            }
            String classUnderTest = initPath.replace('/', '.').replace(".class", "");
            if (classUnderTest.startsWith("kr.ac.unist.apr.GlobalStates")) continue;
            System.out.println("Instrumenting " + classUnderTest);
            classNames.add(classUnderTest);
        }

        try{
            soot.Main.main(classNames.toArray(new String[0]));
            // soot.Main.main(new String[]{});
        } catch(RuntimeException e){
            System.out.println("Error: " + e.getMessage());
        }

        // String classUnderTest = targetPath;
        // System.out.println("Instrumenting " + classUnderTest);
        // // pass arguments to soot
        // soot.Main.main(new String[]{classUnderTest});
        // System.out.println("Instrumented " + classUnderTest);
    }

    private static List<File> getClassFiles(String path){
        File[] files=new File(path).listFiles();
        List<File> initResult= Arrays.asList(files);
        List<File> result=new ArrayList<>();

        for (File file : initResult) {
            if (file.isDirectory()){
                result.addAll(getClassFiles(file.getAbsolutePath()));
            }
            else if (file.getAbsolutePath().endsWith(".class")){
                result.add(file);
            }
        }

        return result;
    }

    private static List<File> getAllLibraries(String path){
        File[] files=new File(path).listFiles();
        List<File> initResult= Arrays.asList(files);
        List<File> result=new ArrayList<>();

        for (File file : initResult) {
            if (file.isDirectory()){
                result.addAll(getAllLibraries(file.getAbsolutePath()));
            }
            else if (file.getAbsolutePath().endsWith(".jar")){
                result.add(file);
            }
        }

        return result;
    }

}
