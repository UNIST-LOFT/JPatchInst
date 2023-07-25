package kr.ac.unist.apr.visitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.visitor.TreeVisitor;
import com.github.javaparser.utils.Pair;
import com.github.javaparser.ast.body.MethodDeclaration;

import kr.ac.unist.apr.Main;
import kr.ac.unist.apr.Branch;

/**
 * This visitor is used to record the file, method, location info for each branch ID.
 * <p>
 *  This visitor returns branchID and file,method,line belong to that branchID.
 * 
 *  The ID of each node should be {@link List} because conditional expression has two branches and switch statement has multiple branches.
 *  If the node is conditional expression, the size of the list is 2.
 *  If the node is {@link SwitchEntry}, the size of the list is 1.
 * </p>
 * @author Truong Huy Trung (trungtruong1)
 */

public class BranchInfoVisitor extends TreeVisitor {
    private static long currentId=0;
    private static String fileName;
    private static String methodName;
    private List<Branch> branches;

    /**
     * Default constructor.
     */
    public BranchInfoVisitor(String filename) {
        super();
        fileName=filename;
        branches=new ArrayList<>();
    }


    /**
     * Visit the given node.
     * <p>
     *  Override this method if you want your own visitor.
     * </p>
     */

    @Override
    public void process(Node node) {    

        if (node instanceof MethodDeclaration){
            MethodDeclaration methodDeclaration = (MethodDeclaration) node;
            methodName = methodDeclaration.getName().getIdentifier();
        }
        if (node instanceof IfStmt) {
            IfStmt ifStmt=(IfStmt) node;

            Branch thenStmt = new Branch();
            thenStmt.setId(Long.toString(currentId++));
            thenStmt.setFileName(fileName);            
            thenStmt.setMethodName(methodName);
            String lineRangeThen=ifStmt.getThenStmt().getRange().toString().replaceAll(".*line (\\d+).*line (\\d+).*", "$1-$2");
            thenStmt.setLineRange(lineRangeThen);
            this.branches.add(thenStmt);

            // Else branch
            if (ifStmt.hasElseBranch()){
                Branch elseStmt = new Branch();
                elseStmt.setId(Long.toString(currentId++));
                elseStmt.setFileName(fileName);            
                elseStmt.setMethodName(methodName);
                String lineRangeElse=ifStmt.getElseStmt().get().getRange().toString().replaceAll(".*line (\\d+).*line (\\d+).*", "$1-$2");
                elseStmt.setLineRange(lineRangeElse);
                this.branches.add(elseStmt);
            }
            
        }
        if (node instanceof ForStmt || node instanceof WhileStmt || node instanceof DoStmt || node instanceof ForEachStmt){
            Branch stmt = new Branch();
            stmt.setId(Long.toString(currentId++));
            stmt.setFileName(fileName);            
            stmt.setMethodName(methodName);
            String lineRange = node.getRange().toString().replaceAll(".*line (\\d+).*line (\\d+).*", "$1-$2");
            stmt.setLineRange(lineRange);
            this.branches.add(stmt);
        }
        else if (node instanceof SwitchEntry) {
            Branch switchBranch = new Branch();
            switchBranch.setId(Long.toString(currentId++));
            switchBranch.setFileName(fileName);            
            switchBranch.setMethodName(methodName);
            String lineRange = node.getRange().toString().replaceAll(".*line (\\d+).*line (\\d+).*", "$1-$2");
            switchBranch.setLineRange(lineRange);
            this.branches.add(switchBranch);
        }
    }
}
