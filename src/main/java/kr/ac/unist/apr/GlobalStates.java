package kr.ac.unist.apr;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
  /** wrapper method name of conditional expression */
  public static final String STATE_COND_METHOD="wrapConditionExpr";
  /** wrapper method name of switch-case */
  public static final String STATE_BRANCH_METHOD="wrapSwitchEntry";
  /** method name to save the info */
  public static final String STATE_SAVE_INFO="saveBranchInfo";
  
  /**
   * The executed branch ID in previous. Used for calculate current ID.
   */
  private static long previousId=0;

  /**
   * Counter for each branches. 
   */
  private static Map<Long,Long> branchInfos=new HashMap<>();

  /**
   * Wrapper for condition expression (IfStmt, ForStmt, WhileStmt, DoStmt).
   * <p>
   *  Wrap the condition expression of IfStmt, ForStmt, WhileStmt with this method.
   *  This method returns exactly same result of the condition expression.
   * 
   *  This method records branch counter if GREYBOX_BRANCH environment variable is set to 1.
   *  If GREYBOX_BRANCH is set to 1, GREYBOX_RESULT environment variable should be set to the path of the result file.
   * </p>
   * @param condition Original condition
   * @param id1 ID for then branch
   * @param id2 ID for else branch
   * @return Original condition
   */
  public static boolean wrapConditionExpr(boolean condition,long id1,long id2) {
    if (System.getenv("GREYBOX_BRANCH").equals("1")) {
      if (condition){
        setBranchInfo(id1);
      }
      else {
        setBranchInfo(id2);
      }

      saveBranchInfo();
    }

    return condition;
  }

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
  public static void wrapSwitchEntry(long id){
    if (System.getenv("GREYBOX_BRANCH").equals("1")) {
      setBranchInfo(id);
      saveBranchInfo();
    }
  }

  /**
   * Increment branch counter.
   * @param id static ID
   */
  private static void setBranchInfo(long id){
    long currentKey=id^previousId;
    
    if (branchInfos.containsKey(currentKey)){
      branchInfos.put(currentKey,branchInfos.get(currentKey)+1);
    } else {
      branchInfos.put(currentKey,1L);
    }

    previousId=id>>1;
  }
  
  /**
   * Save branch counter to the file.
   * <p>
   *  The file path is set by GREYBOX_RESULT environment variable.
   *  
   *  Note that this method does not check GREYBOX_BRANCH environment variable.
   * </p>
   */
  private static void saveBranchInfo() {
    try {
      String outputFile=System.getenv("GREYBOX_RESULT");
      FileWriter fw=new FileWriter(outputFile);
      for (Map.Entry<Long, Long> entry : branchInfos.entrySet()) {
        fw.write(entry.getKey()+","+entry.getValue()+"\n");
      }
      fw.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
