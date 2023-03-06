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
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'process'");
    }
    
    public static enum DiffType {
        INSERT, DELETE, UPDATE, MOVE
    }
}
