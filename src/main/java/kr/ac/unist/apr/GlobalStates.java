package kr.ac.unist.apr;

import java.io.FileWriter;
import java.io.IOException;

/**
 * Handler for instrumented codes.
 * <p>
 *  Instrumented code will execute this class.
 * 
 *  Before run the intrumented code, add this class to the CLASSPATH.
 * 
 *  This class counts the executed number of each branch and saves to file.
 * </p>
 * @author Youngjae Kim
 * @see OriginalSourceVisitor
 * @see TargetSourceVisitor
 * @see PatchedSourceVisitor
 */
public class GlobalStates {
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

  /**
   * Wrapper for SwitchEntry (i.e. case and default).
   * <p>
   *  Insert this method at the first at the body of the SwitchEntry.
   * 
   *  This method records branch counter if GREYBOX_BRANCH environment variable is set to 1.
   *  If GREYBOX_BRANCH is set to 1, GREYBOX_RESULT environment variable should be set to the path of the result file.
   * </p>
   * @param id ID of the branch
   * @see GlobalStates#wrapConditionExpr(boolean, long, long)
   */
  public static void wrapBranch(long id){
    if (System.getenv("GREYBOX_BRANCH").equals("1")) {
      long currentKey=id^previousId;
      previousId=id>>1;

      try {
        String outputFile=System.getenv("GREYBOX_RESULT");
        FileWriter fw=new FileWriter(outputFile,true);
        fw.write(Long.toString(currentKey)+"\n");
        fw.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}
