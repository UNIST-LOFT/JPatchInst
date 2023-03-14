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
  /** wrapper method name of switch-case */
  public static final String STATE_BRANCH_METHOD="wrapBranch";
  /** method name to save the info */
  public static final String STATE_SAVE_INFO="saveBranchInfo";
  
  /**
   * The executed branch ID in previous. Used for calculate current ID.
   */
  private static long previousId=0;

  // NOTE: We use array instead of Map<> because some benchmarks do not supports them (e.g. Chart)
  /**
   * The IDs of the branches.
   */
  private static long[] branchIds=new long[1000000];
  /**
   * The counter of the branches.
   */
  private static long[] branchCounters=new long[1000000];
  /**
   * The total number of the branches.
   */
  private static int totalBranches=0;
  
  /**
   * We do not want to add shutdown hook more than once.
   */
  private static boolean isShutdownHookSet=false;

  /**
   * Wrapper for each branch.
   * <p>
   *  This method is called by instrumented code.
   * 
   *  This method counts the executed number of each branch.
   * </p>
   */
  public static void wrapBranch(long id){
    if (System.getenv("GREYBOX_BRANCH").equals("1")) {
      try{
        long currentKey=id^previousId;

        boolean isFound=false;
        for (int i=0; i<totalBranches; i++) {
          if (branchIds[i]==currentKey) {
            branchCounters[i]++;
            isFound=true;
            break;
          }
        }
        if (!isFound) {
          branchIds[totalBranches]=currentKey;
          branchCounters[totalBranches]=1;
          totalBranches++;
        }
    
        previousId=id>>1;
      }
      catch (Exception e) {
        FileWriter fw;
        try {
          fw=new FileWriter("/tmp/greybox.err");
          fw.write(e.getMessage());
          fw.close();
        } catch (IOException e1) {
          
        }

      }

      if (!isShutdownHookSet) {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
          public void run() {
            try {
              String outputFile=System.getenv("GREYBOX_RESULT");
              FileWriter fw=new FileWriter(outputFile,true);
              for (int i=0;i<totalBranches;i++) {
                fw.write(Long.toString(branchIds[i])+","+Long.toString(branchCounters[i])+"\n");
              }
              fw.close();
            } catch (IOException e) {
              FileWriter fw;
              try {
                fw=new FileWriter("/tmp/greybox.err");
                fw.write(e.getMessage());
                fw.close();
              } catch (IOException e1) {
                
              }
            }
          }
        }));
        isShutdownHookSet=true;
      }
    }
  }
}
