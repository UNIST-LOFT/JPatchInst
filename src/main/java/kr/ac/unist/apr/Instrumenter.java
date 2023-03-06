package kr.ac.unist.apr;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.gumtreediff.gen.javaparser.JavaParserGenerator;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;
import com.github.javaparser.ast.Node;

import kr.ac.unist.apr.gumtree.MyRootsClassifier;
import kr.ac.unist.apr.utils.Path;
import kr.ac.unist.apr.visitor.OriginalSourceVisitor;

/**
 * Main class of instrumentation.
 * <p>
 *  This class will create AST for original source of patched file and whole target program (except test classes)
 *  and instrument the whole target program.
 * 
 *  If the patch insert/modify new branch(es), new branch(es) will not be instrumented.
 *  If the patch removes branch(es), the IDs of removed branch(es) will not be used.
 * </p>
 * @author Youngjae Kim
 */
public class Instrumenter {
    private String originalFilePath;
    private String patchedFilePath;
    private TreeContext originalRootNode;
    private TreeContext patchedRootNode;
    private List<TreeContext> targetNodes; // this list do not contain patched source
    private List<TreeContext> originalNodes; // this list do not contain patched source
    private Map<String,Map<Node,List<Long>>> originalNodeToId;

    /**
     * Default constructor.
     * <p>
     *  Original file path and patched file patch are required to compare between ASTs of them.
     *  Class path and source path are required to generate ASTs of target program.
     *  Every paths should be absolute.
     * 
     *  Note: This constructor will consume some time, to generate every ASTs for target program.
     * </p>
     * @param originalFilePath path of original source file of patched file
     * @param patchedFilePath path of patched file
     * @param targetSourcePath path of target program source
     * @param originalSourcePath path of original source of target program
     * @param classPaths class path of target program
     * @param sourcePaths source path of target program
     * @throws IOException if file not found or I/O errors
     */
    public Instrumenter(String originalFilePath,
                    String patchedFilePath,
                    String targetSourcePath,
                    String originalSourcePath,
                    String[] classPaths,
                    String[] sourcePaths) throws IOException {
        this.originalFilePath=originalFilePath;
        this.patchedFilePath=patchedFilePath;

        // generate AST for original source
        List<String> allOriginalSources=Path.getAllSources(new File(originalSourcePath));
        for (String source:allOriginalSources){
            JavaParserGenerator parser = new JavaParserGenerator();
            FileReader fReader = new FileReader(source);
            BufferedReader bReader = new BufferedReader(fReader);
            TreeContext sourceCtxt = parser.generate(bReader);

            fReader.close();
            bReader.close();
            if (source.equals(originalFilePath))
                originalRootNode = sourceCtxt;
            else
                originalNodes.add(sourceCtxt);
        }

        // generate AST for patched source
        List<String> allSources=Path.getAllSources(new File(targetSourcePath));
        for (String source:allSources){
            JavaParserGenerator parser = new JavaParserGenerator();
            FileReader fReader = new FileReader(source);
            BufferedReader bReader = new BufferedReader(fReader);
            TreeContext targetCtxt = parser.generate(bReader);

            fReader.close();
            bReader.close();
            if (source.equals(patchedFilePath))
                patchedRootNode = targetCtxt;
            else
                targetNodes.add(targetCtxt);
        }
    }
    
    public void instrument() throws UnsupportedOperationException, IOException{
        // Instrument patched file
        Matcher matcher = Matchers.getInstance().getMatcher(originalRootNode.getRoot(), patchedRootNode.getRoot());
        matcher.match();
        MyRootsClassifier classifier = new MyRootsClassifier(originalRootNode,patchedRootNode,matcher);
        Set<ITree> dstAddTrees=classifier.getDstAddTrees();
        Set<ITree> srcDelTrees=classifier.getSrcDelTrees();
        Set<ITree> srcUpdTrees=classifier.getSrcUpdTrees();
        Set<ITree> dstUpdTrees=classifier.getDstUpdTrees();
        Set<ITree> srcMvTrees=classifier.getSrcMvTrees();
        Set<ITree> dstMvTrees=classifier.getDstMvTrees();

        // Visit original source visitor
        OriginalSourceVisitor originalSourceVisitor=new OriginalSourceVisitor();
        originalSourceVisitor.visitPreOrder(originalRootNode.getRoot().getJParserNode());
        originalNodeToId.put(originalFilePath, originalSourceVisitor.getNodeToId());
    }
}
