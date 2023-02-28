package kr.ac.unist.apr;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GlobalStates {
  static final String globalClass="kr/ac/unist/apr/GlobalStates";
  static final String globalMethod="setBranchInfo";
  static final String globalSaveMethod="saveBranchInfo";
  static List<Long> usedIds=new ArrayList<>();
  static long currentBranchId=0;
  static long previousId=0;

  public static Map<Long,Long> branchInfos=new HashMap<>();

  public static void setBranchInfo(long id){
    long currentKey=id^previousId;
    
    if (branchInfos.containsKey(currentKey)){
      branchInfos.put(currentKey,branchInfos.get(currentKey)+1);
    } else {
      branchInfos.put(currentKey,1L);
    }

    previousId=id>>1;
  }
  
  public static void saveBranchInfo() {
    try {
      FileWriter fw=new FileWriter("/tmp/branchInfo.txt");
      for (Map.Entry<Long, Long> entry : branchInfos.entrySet()) {
        fw.write(entry.getKey()+","+entry.getValue()+"\n");
      }
      fw.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
