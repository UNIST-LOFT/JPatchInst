package kr.ac.unist.apr.visitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.visitor.TreeVisitor;

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
     * Get the branch IDs of each node.
     * @return branch IDs of each node.
     */
    public Map<Node,List<Long>> getNodeToId(){
        return nodeToId;
    }

    private void addConditionalId(Node node) {
        List<Long> ids=new ArrayList<>();
        ids.add(currentId++);
        ids.add(currentId++);
        nodeToId.put(node,ids);
    }

    @Override
    public void process(Node node) {
        if (node instanceof IfStmt || node instanceof ForStmt || node instanceof WhileStmt)
            addConditionalId(node);
        else if (node instanceof SwitchEntry) {
            List<Long> ids=new ArrayList<>();
            ids.add(currentId++);
            nodeToId.put(node,ids);
        }
    }
}
