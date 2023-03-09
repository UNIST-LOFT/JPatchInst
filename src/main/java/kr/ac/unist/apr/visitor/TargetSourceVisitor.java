package kr.ac.unist.apr.visitor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.visitor.TreeVisitor;

import kr.ac.unist.apr.GlobalStates;

/**
 * This visitor is used to instrument the target source code except the patched file.
 * @author Youngjae Kim (FreddyYJ)
 */
public class TargetSourceVisitor extends TreeVisitor {
    private Map<Node,List<Long>> nodeToId;
    private Set<Node> computed;

    /**
     * Constructor with node-branchID mapping from {@link OriginalSourceVisitor}.
     * @param nodeToId node-branchID mapping from {@link OriginalSourceVisitor}.
     */
    public TargetSourceVisitor(Map<Node,List<Long>> nodeToId) {
        super();
        this.nodeToId=nodeToId;
        computed=new HashSet<>();
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
        for (Node n:nodeToId.keySet()) {
            boolean isEqual=n.equals(node);
            if (isEqual){
                computed.add(n);
                return nodeToId.get(n);
            }
        }
        throw new RuntimeException("Cannot find node in nodeToId");
    }

    /**
     * Generate a wrapper method call expression for the given condition.
     * @param condition condition to be wrapped
     * @param parentNode parent node of the condition
     * @return wrapper method call expression for the given condition.
     * @see GlobalStates#wrapConditionExpr
     */
    private MethodCallExpr genConditionWrapper(Expression condition,Node parentNode) {
        List<Long> ids=getIds(parentNode);
        if (ids.size()!=2)
            throw new RuntimeException("Size of IDs for condition expr should be 2, but: "+ids.size());

        NameExpr classAccess=new NameExpr(GlobalStates.STATE_CLASS_NAME);
        String methodName=GlobalStates.STATE_COND_METHOD;
        List<Expression> args=Arrays.asList(condition, new LongLiteralExpr(Long.toString(ids.get(0))), new LongLiteralExpr(Long.toString(ids.get(1))));

        return new MethodCallExpr(classAccess, methodName, new NodeList<>(args));
    }

    /**
     * Instrument the given statement.
     * @param stmt {@link IfStmt}, {@link ForStmt}, {@link WhileStmt} or {@link DoStmt}.
     */
    private void instrumentCondition(Statement stmt) {
        if (stmt instanceof IfStmt) {
            // if statement
            IfStmt ifStmt=(IfStmt)stmt;
            Expression condition=ifStmt.getCondition();
            
            MethodCallExpr wrapper=genConditionWrapper(condition,ifStmt);
            ifStmt.setCondition(wrapper);
        }
        else if (stmt instanceof ForStmt) {
            // for statement
            ForStmt forStmt=(ForStmt)stmt;
            Optional<Expression> condition=forStmt.getCompare();

            // We assume that the condition is not false literal
            if (condition.isPresent() && !(condition.get() instanceof BooleanLiteralExpr)){
                MethodCallExpr wrapper=genConditionWrapper(condition.get(),forStmt);
                forStmt.setCompare(wrapper);
            }
        }
        else if (stmt instanceof WhileStmt) {
            // while statement
            WhileStmt whileStmt=(WhileStmt)stmt;
            Expression condition=whileStmt.getCondition();

            // We assume that the condition is not false literal
            if (!(condition instanceof BooleanLiteralExpr)){
                MethodCallExpr wrapper=genConditionWrapper(condition,whileStmt);
                whileStmt.setCondition(wrapper);
            }
        }
        else if (stmt instanceof DoStmt) {
            // do-while statement
            DoStmt doStmt=(DoStmt)stmt;
            Expression condition=doStmt.getCondition();

            // We assume that the condition is not false literal
            if (!(condition instanceof BooleanLiteralExpr)){
                MethodCallExpr wrapper=genConditionWrapper(condition,doStmt);
                doStmt.setCondition(wrapper);
            }
        }
        // TODO: Handle for-each statement
    }

    /**
     * Instrument the given {@link SwitchEntry}.
     * @param switchCase {@link SwitchEntry} to be instrumented.
     */
    private void instrumentSwitchCase(SwitchEntry switchCase) {
        List<Long> ids=getIds(switchCase);
        if (ids.size()!=1)
            throw new RuntimeException("Size of IDs for switch case should be 1, but: "+ids.size());

        NameExpr classAccess=new NameExpr(GlobalStates.STATE_CLASS_NAME);
        String methodName=GlobalStates.STATE_BRANCH_METHOD;
        List<Expression> args=Arrays.asList(new LongLiteralExpr(Long.toString(ids.get(0))));

        MethodCallExpr newMethod=new MethodCallExpr(classAccess, methodName, new NodeList<>(args));
        ExpressionStmt newStmt=new ExpressionStmt(newMethod);

        // Add new method call to case/default statement
        NodeList<Statement> stmts=switchCase.getStatements();
        stmts.add(0, newStmt);
        switchCase.setStatements(stmts);
    }

    @Override
    public void process(Node node) {
        if (node instanceof IfStmt || node instanceof ForStmt || node instanceof WhileStmt || node instanceof DoStmt
                    || node instanceof ForEachStmt) {  
            // Conditional statements
            Statement stmt=(Statement)node;
            instrumentCondition(stmt);
        }
        else if (node instanceof SwitchEntry) {
            // Switch case/default
            SwitchEntry switchCase=(SwitchEntry)node;
            instrumentSwitchCase(switchCase);
        }
    }
}
