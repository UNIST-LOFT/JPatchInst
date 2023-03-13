package kr.ac.unist.apr.visitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.visitor.TreeVisitor;
import com.github.javaparser.utils.Pair;

/**
 * This visitor is used to find the branch IDs of each node.
 * <p>
 *  This visitor returns node-branchID mapping.
 * 
 *  The ID of each node should be {@link List} because conditional expression has two branches and switch statement has multiple branches.
 *  If the node is conditional expression, the size of the list is 2.
 *  If the node is {@link SwitchEntry}, the size of the list is 1.
 * </p>
 * @author Youngjae Kim (FreddyYJ)
 */
public class OriginalSourceVisitor extends TreeVisitor {
    private static long currentId=0;
    private List<Node> nodes;
    private List<List<Long>> ids;

    /**
     * Default constructor.
     */
    public OriginalSourceVisitor() {
        super();
        nodes=new ArrayList<>();
        ids=new ArrayList<>();
    }

    /**
     * Get the results of this visitor.
     * @return branch IDs of each node.
     * @see TargetSourceVisitor#TargetSourceVisitor(Map)
     * @see PatchedSourceVisitor#PatchedSourceVisitor(Map)
     */
    public Pair<List<Node>,List<List<Long>>> getNodeToId(){
        return new Pair<List<Node>,List<List<Long>>>(nodes, ids);
    }

    /**
     * Visit the given node.
     * <p>
     *  Override this method if you want your own visitor.
     * </p>
     */

    @Override
    public void process(Node node) {
        if (node instanceof IfStmt) {
            IfStmt ifStmt=(IfStmt) node;

            // Then branch
            List<Long> ids=new ArrayList<>();
            ids.add(currentId++);

            // Else branch
            if (ifStmt.hasElseBranch())
                ids.add(currentId++);
            
            nodes.add(node);
            this.ids.add(ids);
        }
        if (node instanceof ForStmt || node instanceof WhileStmt || node instanceof DoStmt || node instanceof ForEachStmt){
            List<Long> ids=new ArrayList<>();
            ids.add(currentId++);
            nodes.add(node);
            this.ids.add(ids);
        }
        else if (node instanceof SwitchEntry) {
            List<Long> ids=new ArrayList<>();
            ids.add(currentId++);
            nodes.add(node);
            this.ids.add(ids);
        }
    }
}
