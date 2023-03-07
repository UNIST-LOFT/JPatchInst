package kr.ac.unist.apr;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Handler for instrumented codes.
 * <p>
 *  Instrumented code will execute this class.
 *  This class should be included in the ClassPath and do not instrument this class.
 * </p>
 * @author Youngjae Kim
 */
public class GlobalStates {
  public static final String STATE_CLASS_NAME="kr.ac.unist.apr.GlobalStates";
  public static final String STATE_COND_METHOD="wrapConditionExpr";
  public static final String STATE_BRANCH_METHOD="setBranchInfo";
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
   * Wrapper for condition expression (IfStmt, ForStmt, WhileStmt).
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
   * Add/increment branch counter.
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
