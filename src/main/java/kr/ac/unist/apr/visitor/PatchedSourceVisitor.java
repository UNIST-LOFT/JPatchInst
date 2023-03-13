package kr.ac.unist.apr.visitor;

import java.util.ArrayList;
import java.util.List;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.visitor.TreeVisitor;
import com.github.javaparser.utils.Pair;

/**
 * This visitor finds which nodes should be instrumented and their branch IDs for a patched file.
 * <p>
 *  This visitor is only for a patched files.
 *  To instrument not patched files, use {@link PatchedSourceVisitor}.
 * 
 *  Before run this visitor, run {@link OriginalSourceVisitor} to get the mapping between nodes and branch IDs.
 *  
 *  This visitor instruments IfStmt, SwitchEntry, ForStmt, ForEachStmt, WhileStmt, and DoStmt.
 *  For IfStmt, it instruments the then and else statements. If the else exist, it has two IDs and otherwise one ID.
 *  The other statements have only one ID for their body.
 * </p>
 * @author Youngjae Kim (FreddyYJ)
 */
public class PatchedSourceVisitor extends TreeVisitor {
    private List<Node> nodes;
    private List<List<Long>> ids;
    
    private List<Node> resultNodes;
    private List<List<Long>> resultIds;

    /**
     * Constructor with node-branchID mapping from {@link OriginalSourceVisitor}.
     * @param nodeToId node-branchID mapping from {@link OriginalSourceVisitor}.
     */
    public PatchedSourceVisitor(Pair<List<Node>,List<List<Long>>> nodeToId) {
        super();
        nodes=nodeToId.a;
        ids=nodeToId.b;
        resultNodes=new ArrayList<>();
        resultIds=new ArrayList<>();
    }
    
    /**
     * Get branch IDs for the given node.
     * <p>
     *  Since the original source and target source is not same, we return null if the node is not found in the mapping.
     * 
     *  The size of return value should be 2 if the node is a conditional statement.
     *  The size of return value should be 1 if the node is a case statement.
     * </p>
     * @param node node to find branch IDs.
     * @return branch IDs for the given node or null if node is not found.
     */
    private List<Long> getIds(Node node) {
        for (int i=0;i<nodes.size();i++) {
            Node n=nodes.get(i);
            boolean isEqual=n.equals(node);
            if (isEqual){
                Node curNode=n;
                Node curSrcNode=node;
                boolean isSame=true;
                while (curNode.getParentNode().isPresent() && curSrcNode.getParentNode().isPresent()) {
                    Node parentNode=curNode.getParentNode().get();
                    Node parentSrcNode=curSrcNode.getParentNode().get();

                    if (parentNode.getClass().getName().equals(parentSrcNode.getClass().getName())){
                        if (parentNode instanceof BlockStmt){
                            BlockStmt parentBlock=(BlockStmt)parentNode;
                            BlockStmt parentSrcBlock=(BlockStmt)parentSrcNode;
                            if (parentBlock.getStatements().indexOf(curNode)!=parentSrcBlock.getStatements().indexOf(curSrcNode)){
                                isSame=false;
                                break;
                            }
                        }
                        else if (parentNode instanceof MethodDeclaration){
                            MethodDeclaration parentMethod=(MethodDeclaration)parentNode;
                            MethodDeclaration parentSrcMethod=(MethodDeclaration)parentSrcNode;
                            if (!parentMethod.getNameAsString().equals(parentSrcMethod.getNameAsString())){
                                isSame=false;
                                break;
                            }
                        }
                    }
                    else{
                        isSame=false;
                        break;
                    }

                    curNode=parentNode;
                    curSrcNode=parentSrcNode;
                }

                if (isSame){
                    return ids.get(i);
                }
            }
        }
        return null;
    }

    /**
     * Get the nodes and branch IDs to instrument.
     * <p>
     *  Return value is a pair of node list and branch IDs for each node.
     *  Instrument each nodes with the branch IDs.
     * </p>
     * @return the nodes and branch IDs to instrument.
     */
    public Pair<List<Node>,List<List<Long>>> getResult() {
        return new Pair<>(resultNodes,resultIds);
    }

    /**
     * Visit the given node.
     * <p>
     *  Override this method if you want your own visitor.
     * </p>
     */
    @Override
    public void process(Node node) {
        if (node instanceof IfStmt || node instanceof ForStmt || node instanceof WhileStmt || node instanceof DoStmt
                    || node instanceof ForEachStmt) {  
            // Conditional statements
            resultNodes.add(node);
            resultIds.add(getIds(node));
        }
        else if (node instanceof SwitchEntry) {
            // Switch case/default
            SwitchEntry switchCase=(SwitchEntry)node;
            resultNodes.add(switchCase);
            resultIds.add(getIds(switchCase));
        }
    }
}
