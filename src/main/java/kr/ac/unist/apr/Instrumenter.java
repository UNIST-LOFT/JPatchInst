package kr.ac.unist.apr;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
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
import kr.ac.unist.apr.visitor.PatchedSourceVisitor;
import kr.ac.unist.apr.visitor.TargetSourceVisitor;
import kr.ac.unist.apr.visitor.PatchedSourceVisitor.DiffType;

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
    private Map<String,TreeContext> targetNodes=new HashMap<>(); // this list do not contain patched source
    private Map<String,TreeContext> originalNodes=new HashMap<>(); // this list do not contain patched source
    private Map<String,Map<Node,List<Long>>> originalNodeToId=new HashMap<>();

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
                originalNodes.put(source,sourceCtxt);
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
                targetNodes.put(source,targetCtxt);
        }
    }
    
    public void instrument() throws UnsupportedOperationException, IOException{
        // Visit original source visitor
        // TODO: Cache/load this result with file
        OriginalSourceVisitor originalSourceVisitor=new OriginalSourceVisitor();
        originalSourceVisitor.visitPreOrder(originalRootNode.getRoot().getJParserNode());
        originalNodeToId.put(originalFilePath, originalSourceVisitor.getNodeToId());

        for (Map.Entry<String,TreeContext> originalCtxt:originalNodes.entrySet()){
            originalSourceVisitor=new OriginalSourceVisitor();
            originalSourceVisitor.visitPreOrder(originalCtxt.getValue().getRoot().getJParserNode());
            originalNodeToId.put(originalCtxt.getKey(), originalSourceVisitor.getNodeToId());
        }

        // Instrument target program without patched file
        // TODO: Skip already instrumented file
        for (Map.Entry<String,TreeContext> targetCtxt:targetNodes.entrySet()){
            TargetSourceVisitor instrumenterVisitor=new TargetSourceVisitor(originalNodeToId.get(targetCtxt.getKey()));
            Node targetNode=targetCtxt.getValue().getRoot().getJParserNode();
            instrumenterVisitor.visitPreOrder(targetNode);
            
            // Save instrumented file
            FileWriter writer = new FileWriter(targetCtxt.getKey());
            writer.write(targetNode.toString());
            writer.close();
        }

        // Get differences between original source and patched source
        Matcher matcher = Matchers.getInstance().getMatcher(originalRootNode.getRoot(), patchedRootNode.getRoot());
        matcher.match();
        MyRootsClassifier classifier = new MyRootsClassifier(originalRootNode,patchedRootNode,matcher);
        Set<ITree> dstAddTrees=classifier.getDstAddTrees();
        Set<ITree> srcDelTrees=classifier.getSrcDelTrees();
        Set<ITree> srcUpdTrees=classifier.getSrcUpdTrees();
        Set<ITree> dstUpdTrees=classifier.getDstUpdTrees();
        Set<ITree> srcMvTrees=classifier.getSrcMvTrees();
        Set<ITree> dstMvTrees=classifier.getDstMvTrees();
        dstAddTrees=removeDuplicateNode(dstAddTrees);
        srcDelTrees=removeDuplicateNode(srcDelTrees);
        srcUpdTrees=removeDuplicateNode(srcUpdTrees);
        dstUpdTrees=removeDuplicateNode(dstUpdTrees);
        srcMvTrees=removeDuplicateNode(srcMvTrees);
        dstMvTrees=removeDuplicateNode(dstMvTrees);

        Map<DiffType,List<Node>> diffMap=new HashMap<>();
        ArrayList<Node> dstAddList=new ArrayList<>();
        for (ITree tree:dstAddTrees)
            dstAddList.add(tree.getJParserNode());
        diffMap.put(DiffType.INSERT, dstAddList);

        ArrayList<Node> srcDelList=new ArrayList<>();
        for (ITree tree:srcDelTrees)
            srcDelList.add(tree.getJParserNode());
        diffMap.put(DiffType.DELETE, srcDelList);

        ArrayList<Node> srcUpdList=new ArrayList<>();
        for (ITree tree:srcUpdTrees)
            srcUpdList.add(tree.getJParserNode());
        diffMap.put(DiffType.UPDATE_ORIG, srcUpdList);

        ArrayList<Node> dstUpdList=new ArrayList<>();
        for (ITree tree:dstUpdTrees)
            dstUpdList.add(tree.getJParserNode());
        diffMap.put(DiffType.UPDATE_PATCH, dstUpdList);

        ArrayList<Node> srcMvList=new ArrayList<>();
        for (ITree tree:srcMvTrees)
            srcMvList.add(tree.getJParserNode());
        diffMap.put(DiffType.MOVE_ORIG, srcMvList);

        ArrayList<Node> dstMvList=new ArrayList<>();
        for (ITree tree:dstMvTrees)
            dstMvList.add(tree.getJParserNode());
        diffMap.put(DiffType.MOVE_PATCH, dstMvList);

        // Instrument patched file
        PatchedSourceVisitor patchVisitor=new PatchedSourceVisitor(originalNodeToId.get(patchedFilePath),diffMap);
        patchVisitor.visitPreOrder(patchedRootNode.getRoot().getJParserNode());
    }

    private Set<ITree> removeDuplicateNode(Set<ITree> trees){
        Set<ITree> result=trees;
        for (ITree tree:trees){
            for (ITree target:trees){
                if (target.getJParserNode().isAncestorOf(tree.getJParserNode()) && !target.equals(tree))
                    result.remove(tree);
            }
        }
        return result;
    }
}
