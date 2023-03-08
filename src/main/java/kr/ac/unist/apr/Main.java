package kr.ac.unist.apr;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    public static final Logger LOGGER=Logger.getGlobal();
    public static void main(String[] args) {
        if (args.length!=3) {
            System.out.println("Usage: java -jar apr.jar <project> <patchedFilePath> <originalFilePath>");
            System.exit(1);
        }
        
        String originalFilePath=args[0];
        String patchedFilePath=args[1];
        String originalSourcePath=args[2];
        String targetSourcePath=args[3];
        String[] classPath=args[4].split(":");
        LOGGER.log(Level.INFO, "Original File Path: "+originalFilePath);
        LOGGER.log(Level.INFO, "Patched File Path: "+patchedFilePath);
        LOGGER.log(Level.INFO, "Original Source Path: "+originalSourcePath);
        LOGGER.log(Level.INFO, "Target Source Path: "+targetSourcePath);
        LOGGER.log(Level.INFO, "Class Path: "+args[4]);

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
