package kr.ac.unist.apr;

import java.io.IOException;

public class Main {
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

        try {
            Instrumenter instrumenter=new Instrumenter(originalFilePath,patchedFilePath,targetSourcePath,originalSourcePath,classPath);
            instrumenter.instrument();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
