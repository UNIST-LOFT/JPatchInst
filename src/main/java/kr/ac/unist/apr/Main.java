package kr.ac.unist.apr;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    public static final Logger LOGGER=Logger.getGlobal();
    public static void main(String[] args) {
        if (args.length!=2) {
            System.out.println("Usage: java -jar JPatchInst.jar <original_source_path> <target_source_path>");
            System.exit(1);
        }
        
        String originalSourcePath=args[0];
        String targetSourcePath=args[1];
        LOGGER.log(Level.INFO, "Original Source Path: "+originalSourcePath);
        LOGGER.log(Level.INFO, "Target Source Path: "+targetSourcePath);

        try {
            LOGGER.log(Level.INFO, "Start instrumenting...");
            Instrumenter instrumenter=new Instrumenter(targetSourcePath,originalSourcePath);
            instrumenter.instrument();
            LOGGER.log(Level.INFO, "Instrumenting finished.");
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
