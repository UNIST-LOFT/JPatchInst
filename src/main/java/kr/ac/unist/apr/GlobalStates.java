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
  public static final String STATE_INIT="initialize";
  
  public static final String STATE_IS_INITIALIZED="isInitialized";
  public static final String STATE_PREV_ID="previousId";
  public static final String STATE_RESULT_FILE="resultFile";

  public static final String STATE_ENV_RECORD="GREYBOX_BRANCH";
  public static final String STATE_ENV_RESULT_FILE="GREYBOX_RESULT";
  
  /**
   * The executed branch ID in previous. Used for calculate current ID.
   */
  public static long previousId=0;
  
  /**
   * We do not want to add shutdown hook more than once.
   */
  private static boolean isShutdownHookSet=false;
  public static boolean isInitialized=false;

  public static FileWriter resultFile=null;

  public static void initialize() {
    if (System.getenv("GREYBOX_BRANCH").equals("1")) {
      if (resultFile==null) {
        try {
          resultFile=new FileWriter(System.getenv("GREYBOX_RESULT"));
        } catch (IOException e) {
          System.err.println("Cannot open result file: "+System.getenv("GREYBOX_RESULT"));
          e.printStackTrace();
        }
      }

      Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
        public void run() {
          try {
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

  /**
   * Wrapper for each branch.
   * <p>
   *  This method is called by instrumented code.
   * 
   *  This method counts the executed number of each branch.
   * </p>
   * @deprecated Use {@link #previousId} and {@link #resultFile} directly to reduce method call.
   */
  public static void wrapBranch(long id){
    if (System.getenv("GREYBOX_BRANCH").equals("1")) {
      try{
        if (!isShutdownHookSet) {
          resultFile=new FileWriter(System.getenv("GREYBOX_RESULT"));
        }
        
        long currentKey=id^previousId;

        resultFile.write(Long.toString(currentKey)+"\n");
        resultFile.flush();
    
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
              resultFile.close();
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
