package kr.ac.unist.apr.visitor;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.visitor.TreeVisitor;

public class DumpVisitor extends TreeVisitor {
    private StringBuilder builder=new StringBuilder();
    private int indent=0;

    private void addIndent() {
        for (int i=0;i<indent;i++)
            builder.append("    ");
    }

    @Override
    public void process(Node node) {
        builder.append(node.getClass().getSimpleName());
        if (node instanceof NodeWithSimpleName) {
            NodeWithSimpleName<?> nodeWithSimpleName=(NodeWithSimpleName<?>)node;
            builder.append("  ");
            builder.append(nodeWithSimpleName.getNameAsString());
        }
        builder.append("\n");
    }

    public String getDump() {
        return builder.toString();
    }
    
}
