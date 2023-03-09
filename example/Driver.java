import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Set;

import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;

import kr.ac.unist.apr.gumtree.MyRootsClassifier;

import com.github.gumtreediff.actions.RootsClassifier;
import com.github.gumtreediff.gen.javaparser.JavaParserGenerator;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.matchers.Matcher;

/**
 * This is a driver class for examples.
 * <p>
 * Usage: java -cp ../build/libs/gumtree-all.jar:. Driver <source path> <target path>
 * Example: java -cp ../build/libs/gumtree-all.jar:. Driver IfStmt.java insert/IfStmt.java
 * </p>
 * @author Youngjae Kim (FreddyYJ)
 */
public class Driver {
    private TreeContext ctxt1;
    private TreeContext ctxt2;
    public static void main(String[] args) {
        String path1=args[0];
        String path2=args[1];

        try{
            // Generate source AST
            JavaParserGenerator gen=new JavaParserGenerator();
            FileReader fReader1=new FileReader(path1);
            BufferedReader bReader1=new BufferedReader(fReader1);
            TreeContext ctxt1=gen.generate(bReader1);
            fReader1.close();
            bReader1.close();

            // Generate target AST
            gen=new JavaParserGenerator();
            FileReader fReader2=new FileReader(path2);
            BufferedReader bReader2=new BufferedReader(fReader2);
            TreeContext ctxt2=gen.generate(bReader2);
            fReader2.close();
            bReader2.close();

            // Get difference and print
            Driver driver=new Driver(ctxt1,ctxt2);
            driver.getDiff();
        } catch (IOException e){
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Default constructor with source and target {@link TreeContext}}.
     * @param ctxt1 source {@link TreeContext}
     * @param ctxt2 target {@link TreeContext}
     */
    public Driver(TreeContext ctxt1, TreeContext ctxt2) {
        this.ctxt1=ctxt1;
        this.ctxt2=ctxt2;
    }

    /**
     * Print the tree with Node from JavaParser.
     * @param tree GumTree {@link ITree} to print
     */
    public void printTree(ITree tree) {
        System.out.println(tree.getJParserNode().toString());
    }

    /**
     * Print the tree with GumTree.
     * @param tree GumTree {@link ITree} to print
     */
    public void printGumTree(ITree tree) {
        System.out.println(tree.toTreeString());
    }

    /**
     * Get and print the difference between source and target.
     */
    public void getDiff() {
        // Create matcher to compute differences
        Matcher matcher=Matchers.getInstance().getMatcher(ctxt1.getRoot(), ctxt2.getRoot());
        matcher.match();
        // Classifier to classify differences
        MyRootsClassifier classifier=new MyRootsClassifier(ctxt1,ctxt2,matcher);

        // Added in target
        Set<ITree> dstAddTrees=classifier.getDstAddTrees();
        System.out.println("Added in destination: ");
        for (ITree tree: dstAddTrees){
            printTree(tree);
            System.out.println();
        }
        System.out.println("---------------------");

        // Deleted from source
        Set<ITree> srcDelTrees=classifier.getSrcDelTrees();
        System.out.println("Removed from source: ");
        for (ITree tree: srcDelTrees){
            printTree(tree);
            System.out.println();
        }
        System.out.println("---------------------");

        // Updated from source
        Set<ITree> srcUpdTrees=classifier.getSrcUpdTrees();
        System.out.println("Updated from source: ");
        for (ITree tree: srcUpdTrees){
            printTree(tree);
            System.out.println();
        }
        System.out.println("---------------------");

        // Updated in target
        Set<ITree> dstUpdTrees=classifier.getDstUpdTrees();
        System.out.println("Updated in destination: ");
        for (ITree tree: dstUpdTrees){
            printTree(tree);
            System.out.println();
        }
        System.out.println("---------------------");

        // Moved from source
        Set<ITree> srcMvTrees=classifier.getSrcMvTrees();
        System.out.println("Moved from source: ");
        for (ITree tree: srcMvTrees){
            printTree(tree);
            System.out.println();
        }
        System.out.println("---------------------");

        // Moved in target
        Set<ITree> dstMvTrees=classifier.getDstMvTrees();
        System.out.println("Moved in destination: ");
        for (ITree tree: dstMvTrees){
            printTree(tree);
            System.out.println();
        }
        System.out.println("---------------------");
    }
}
