package kr.ac.unist.apr.visitor;

import java.util.List;
import java.util.Map;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.visitor.TreeVisitor;

public class TargetSourceVisitor extends TreeVisitor {
    private Map<Node,List<Long>> nodeToId;

    public TargetSourceVisitor(Map<Node,List<Long>> nodeToId) {
        super();
        this.nodeToId=nodeToId;
    }

    @Override
    public void process(Node node) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'process'");
    }
    
}
