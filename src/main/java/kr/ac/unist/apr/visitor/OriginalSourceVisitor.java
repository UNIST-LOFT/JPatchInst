package kr.ac.unist.apr.visitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.WhileStatement;

/**
 * Visitor to get each branch ID of original source.
 * <p>
 *  This visitor do not modify the AST.
 *  To run the visitor, use {@link ASTNode#accept(ASTVisitor)}. {@link ASTNode} should be {@link org.eclipse.jdt.core.dom.CompilationUnit}.
 *  To get the result, use {@link #getNodeToId()}.
 * 
 *  The result of this visitor is {@link Map}<ASTNode, List<Long>>.
 *  The key is one of the following ASTNode: {@link IfStatement}, {@link ForStatement}, {@link WhileStatement}, {@link SwitchCase}.
 *  The value is {@link List} of the branch IDs.
 *  If the key is {@link IfStatement}, {@link ForStatement} or {@link WhileStatement}, the value is a list of two IDs; first ID is then branch and second ID is else branch.
 *  If the key is {@link SwitchCase}, the value is a list of multiple ID; for each cases/default.
 * </p>
 * @author Youngjae Kim
 */
public class OriginalSourceVisitor extends ASTVisitor {
    private static long currentId;
    private Map<ASTNode,List<Long>> nodeToId;

    /**
     * Default constructor.
     */
    public OriginalSourceVisitor() {
        super();
        nodeToId = new HashMap<>();
        currentId=0;
    }

    @Override
    public boolean visit(IfStatement ifStmt) {
        List<Long> ids=new ArrayList<>();
        ids.add(currentId++);
        ids.add(currentId++);
        nodeToId.put(ifStmt,ids);
        
        return true;
    }

    @Override
    public boolean visit(ForStatement forStmt){
        List<Long> ids=new ArrayList<>();
        ids.add(currentId++);
        ids.add(currentId++);
        nodeToId.put(forStmt,ids);
        
        return true;
    }

    @Override
    public boolean visit(WhileStatement whileStmt) {
        List<Long> ids=new ArrayList<>();
        ids.add(currentId++);
        ids.add(currentId++);
        nodeToId.put(whileStmt,ids);
        
        return true;
    }

    @Override
    public boolean visit(SwitchCase caseStmt) {
        List<Long> ids=new ArrayList<>();
        ids.add(currentId++);
        nodeToId.put(caseStmt,ids);
        
        return true;
    }

    /**
     * Get the branch IDs of each node.
     * @return branch IDs of each node.
     */
    public Map<ASTNode,List<Long>> getNodeToId(){
        return nodeToId;
    }
}
