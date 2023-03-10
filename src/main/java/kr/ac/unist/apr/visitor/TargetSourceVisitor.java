package kr.ac.unist.apr.visitor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.visitor.TreeVisitor;
import com.github.javaparser.utils.Pair;

import kr.ac.unist.apr.GlobalStates;

/**
 * This visitor is used to instrument the target source code except the patched file.
 * @author Youngjae Kim (FreddyYJ)
 */
public class TargetSourceVisitor extends TreeVisitor {
    private List<Node> nodes;
    private List<List<Long>> ids;

    private List<Node> resultNodes;
    private List<List<Long>> resultIds;

    /**
     * Constructor with node-branchID mapping from {@link OriginalSourceVisitor}.
     * @param nodeToId node-branchID mapping from {@link OriginalSourceVisitor}.
     */
    public TargetSourceVisitor(Pair<List<Node>,List<List<Long>>> nodeToId) {
        super();
        this.nodes=nodeToId.a;
        this.ids=nodeToId.b;
        this.resultNodes=new ArrayList<>();
        this.resultIds=new ArrayList<>();
    }

    /**
     * Get branch IDs for the given node.
     * <p>
     *  Since the original source and target source is exactly same, we throw an exception if the node is not found in the mapping.
     * 
     *  The size of return value should be 2 if the node is a conditional statement.
     *  The size of return value should be 1 if the node is a case statement.
     * </p>
     * @param node node to find branch IDs.
     * @return branch IDs for the given node.
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
        throw new RuntimeException("Cannot find node in nodeToId");
    }

    public Pair<List<Node>,List<List<Long>>> getResult() {
        return new Pair<>(resultNodes,resultIds);
    }

    @Override
    public void process(Node node) {
        if (node instanceof IfStmt || node instanceof ForStmt || node instanceof WhileStmt || node instanceof DoStmt
                    || node instanceof ForEachStmt) {  
            // Conditional statements
            Statement stmt=(Statement)node;
            resultNodes.add(stmt);
            resultIds.add(getIds(stmt));
        }
        else if (node instanceof SwitchEntry) {
            // Switch case/default
            SwitchEntry switchCase=(SwitchEntry)node;
            resultNodes.add(switchCase);
            resultIds.add(getIds(switchCase));
        }
    }
}
