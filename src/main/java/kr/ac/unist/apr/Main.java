package kr.ac.unist.apr;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    public static final Logger LOGGER=Logger.getGlobal();
    public static void main(String[] args) {
        if (args.length!=3 && args.length!=5) {
            System.out.println("Usage: java -jar JPatchInst.jar <original_source_path> <target_source_path> <class_path> [original_file] [patched_file]");
            System.exit(1);
        }
        
        String originalSourcePath=args[0];
        String targetSourcePath=args[1];
        String[] classPath=args[2].split(":");
        LOGGER.log(Level.INFO, "Original Source Path: "+originalSourcePath);
        LOGGER.log(Level.INFO, "Target Source Path: "+targetSourcePath);
        LOGGER.log(Level.INFO, "Class Path: "+args[2]);

        String originalFilePath=null;
        String patchedFilePath=null;
        if (args.length==5){
            originalFilePath=args[3];
            patchedFilePath=args[4];
            LOGGER.log(Level.INFO, "Original File Path: "+originalFilePath);
            LOGGER.log(Level.INFO, "Patched File Path: "+patchedFilePath);
        }

        try {
            LOGGER.log(Level.INFO, "Start instrumenting...");
            Instrumenter instrumenter=new Instrumenter(originalFilePath,patchedFilePath,targetSourcePath,originalSourcePath,classPath);
            instrumenter.instrument();
            LOGGER.log(Level.INFO, "Instrumenting finished.");
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
