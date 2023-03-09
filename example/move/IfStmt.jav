/**
 * println is moved to line 9.
 * @author Youngjae Kim (FreddyYJ)
 */
public class IfStmt {
    public void testIfStmt() {
        int a=5;
        String output="";
        System.out.println(output);

        if (a>1) {
            output="a>1";
        }
        else{
            output="a<=1";
        }
    }
}
