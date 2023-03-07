package kr.ac.unist.apr.visitor;

import java.util.List;
import java.util.Map;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.visitor.TreeVisitor;

public class PatchedSourceVisitor extends TreeVisitor {
    private Map<Node,List<Long>> nodeToId;
    private Map<DiffType,List<Node>> diffNodes;
    
    public PatchedSourceVisitor(Map<Node,List<Long>> nodeToId,Map<DiffType,List<Node>> diffNodes) {
        super();
        this.nodeToId=nodeToId;
        this.diffNodes=diffNodes;
    }
    
    @Override
    public void process(Node node) {
        // TODO Implements instrumentation with handling patch
        // Tip: NoCommentEqualsVisitor to check if the node is same
        // Tip: CompilationUnit.getImports() to check if this AST is already instrumented
    }
    
    public static enum DiffType {
        INSERT, DELETE, UPDATE, MOVE
    }
}
