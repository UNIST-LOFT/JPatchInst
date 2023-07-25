package kr.ac.unist.apr;

import java.io.FileWriter;
import java.io.IOException;

/**
 * Handler for instrumented codes.
 * <p>
 *  Instrumented code will execute this class.
 * 
 *  Before run the intrumented code, add this class to the CLASSPATH or 
 *       copy this file to source directory of the target project.
 * 
 *  This class counts the executed number of each branch and saves to file.
 * 
 *  To use this class, you have to set the following environment variables:
 *  <ul>
 *    <li>GREYBOX_BRANCH=1 to count the execution of each branches</li>
 *    <li>GREYBOX_RESULT=filename to save the result</li>
 *  </ul>
 * 
 *  Note that we do not implement checkers to check these variables are defined.
 * 
 *  In result file, each line has two columns which are seperated by comma(,).
 *  First column is the branch ID and second column is the number of execution of each branch.
 * 
 *  This class use Shutdown Hook to save the result, to reduce the overhead.
 *  It the program is terminated by external signal, the result may not be saved.
 * </p>
 * @author Youngjae Kim
 * @see OriginalSourceVisitor
 * @see TargetSourceVisitor
 * @see PatchedSourceVisitor
 */
public class GlobalStates {
  // In this class, we minimalize the dependencies language features.
  
  /** Full name of this class */
  public static final String STATE_CLASS_NAME="kr.ac.unist.apr.GlobalStates";
  public static final String STATE_INIT="initialize";
  
  public static final String STATE_IS_INITIALIZED="isInitialized";
  public static final String STATE_PREV_ID="previousId";
  public static final String STATE_BRANCH_COUNT="branchCount";

  public static final String STATE_ENV_RECORD="GREYBOX_BRANCH";
  
  /**
   * The executed branch ID in previous. Used for calculate current ID.
   */
  public static int previousId=0;
  public static int[] branchCount=new int[200000]; // Make enough size of array to reduce the overhead.
  
  public static boolean isInitialized=false;

  public static FileWriter resultFile=null;

  public static void initialize() {
    if (System.getenv("GREYBOX_BRANCH").equals("1")) {
      Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
        public void run() {
          try {
            resultFile=new FileWriter(System.getenv("GREYBOX_RESULT"));
            for (int i=0;i<branchCount.length;i++) {
              if (branchCount[i]>0) {
                resultFile.write(i+":"+branchCount[i]+"\n");
              }
            }
            resultFile.close();
          } catch (IOException e) {
            FileWriter fw;
            try {
              fw=new FileWriter("/tmp/greybox.err");
              fw.write(e.getMessage());
              fw.close();
            } catch (IOException e1) {
              System.err.println("Cannot open error file: /tmp/greybox.err");
              e1.printStackTrace();
            }
          }
        }
      }));
    }
    isInitialized=true;
  }
}
