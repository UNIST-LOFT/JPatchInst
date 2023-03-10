package kr.ac.unist.apr.visitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.visitor.TreeVisitor;

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
    private static long currentId;
    private Map<Node,List<Long>> nodeToId;

    /**
     * Default constructor.
     */
    public OriginalSourceVisitor() {
        super();
        nodeToId = new HashMap<>();
        currentId=0;
    }

    /**
     * Get the results of this visitor.
     * @return branch IDs of each node.
     * @see TargetSourceVisitor#TargetSourceVisitor(Map)
     * @see PatchedSourceVisitor#PatchedSourceVisitor(Map)
     */
    public Map<Node,List<Long>> getNodeToId(){
        return nodeToId;
    }

    /**
     * Add branch IDs for the given conditional expression.
     * @param node conditional expression
     */
    private void addConditionalId(Node node) {
        List<Long> ids=new ArrayList<>();
        ids.add(currentId++);
        ids.add(currentId++);
        nodeToId.put(node,ids);
    }

    @Override
    public void process(Node node) {
        if (node instanceof IfStmt || node instanceof ForStmt || node instanceof WhileStmt || node instanceof DoStmt)
            addConditionalId(node);
        else if (node instanceof SwitchEntry) {
            List<Long> ids=new ArrayList<>();
            ids.add(currentId++);
            nodeToId.put(node,ids);
        }
    }
}
