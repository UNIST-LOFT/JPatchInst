package kr.ac.unist.apr;

public class Main {
    public static void main(String[] args) {
        if (args.length!=3) {
            System.out.println("Usage: java -jar apr.jar <project> <patchedFilePath> <originalFilePath>");
            System.exit(1);
        }
        
        String project=args[0];
        String patchedFilePath=args[1];
        String originalFilePath=args[2];
    }
}
