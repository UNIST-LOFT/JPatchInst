/**
 * If-return statement is inserted in line 10.
 * @author Youngjae Kim (FreddyYJ)
 */
public class IfStmt {
    public void testIfStmt() {
        int a=5;
        String output="";

        if (output==null) {
            return;
        }
        if (a>1) {
            output="a>1";
        }
        else{
            output="a<=1";
        }

        System.out.println(output);
    }
}
