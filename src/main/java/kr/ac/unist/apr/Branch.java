package kr.ac.unist.apr;

/**
 * Class for storing branch info.
 * @author Truong Huy Trung
*/

public class Branch {
  private String id;
  private String fileName;
  private String methodName;
  private String lineRange;

  public void setId(String id) {
      this.id = id;
  }

  public void setFileName(String fileName) {
      this.fileName = fileName;
  }

  public void setMethodName(String methodName) {
      this.methodName = methodName;
  }

  public void setLineRange(String lineRange) {
      this.lineRange = lineRange;
  }

  public String getId() {
      return id;
  }

  public String getFileName() {
      return fileName;
  }

  public String getMethodName() {
      return methodName;
  }

  public String getLineRange() {
      return lineRange;
  }
}