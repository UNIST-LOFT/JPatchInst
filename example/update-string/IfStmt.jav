/**
 * String literal is updated in line 11 ("a>1" to "Some new string").
 * @author Youngjae Kim (FreddyYJ)
 */
public class IfStmt {
    public void testIfStmt() {
        int a=5;
        String output="";

        if (a>1) {
            output="Some new string";
        }
        else{
            output="a<=1";
        }

        System.out.println(output);
    }
}
