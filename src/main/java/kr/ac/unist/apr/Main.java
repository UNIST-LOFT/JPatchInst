package kr.ac.unist.apr;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class Main {
    public static final Logger LOGGER=Logger.getGlobal();
    public static void main(String[] args) {
        if (args.length<2) {
            System.out.println("Usage: java -jar JPatchInst.jar <original_source_path> <target_source_path>");
            System.exit(1);
        }

        Options options=new Options();
        options.addOption("i", "branch-id", true, "Branch ID to instrument. Seperated in comma(,). Default is all.");
        options.addOption("t", "time-output-file", true, "Output file path for each time to instrument file");

        CommandLineParser parser=new DefaultParser();
        CommandLine cmd=null;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        String branchIds=cmd.getOptionValue("i", "");
        ArrayList<Integer> branchIdList=new ArrayList<>();
        if (!branchIds.equals("")){
            String[] parsedBranchIds=branchIds.split(",");
            for (String branchId : parsedBranchIds) {
                if (branchId.equals("")) {
                    continue;
                }
                branchIdList.add(Integer.parseInt(branchId));
            }
        }

        String timeOutputFile=cmd.getOptionValue("t", "");

        String[] parsedArgs=cmd.getArgs();

        // Convert Windows path separators (\\) to single backslash
        String originalSourcePath=parsedArgs[0].replace("\\\\", "\\");
        String targetSourcePath=parsedArgs[1].replace("\\\\", "\\");
        LOGGER.log(Level.INFO, "Original Source Path: "+originalSourcePath);
        LOGGER.log(Level.INFO, "Target Source Path: "+targetSourcePath);

        try {
            LOGGER.log(Level.INFO, "Start instrumenting...");
            Instrumenter instrumenter=new Instrumenter(targetSourcePath,originalSourcePath,branchIdList);
            instrumenter.instrument(timeOutputFile);
            LOGGER.log(Level.INFO, "Instrumenting finished.");
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
