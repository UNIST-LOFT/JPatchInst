package kr.ac.unist.apr;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;

import com.github.gumtreediff.actions.Diff;
import com.github.gumtreediff.actions.OnlyRootsClassifier;
import com.github.gumtreediff.gen.TreeGenerator;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;

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
    private ASTNode originalRootNode;
    private ASTNode patchedRootNode;
    private List<ASTNode> targetNodes; // this list do not contain patched source
    private List<ASTNode> originalNodes; // this list do not contain patched source
    private Map<String,Map<ASTNode,List<Long>>> originalNodeToId;
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
        Map<String, String> options = new HashMap<>();
        options.put("org.eclipse.jdt.core.compiler.source", "1.8");
        options.put("org.eclipse.jdt.core.problem.enablePreviewFeatures", "disabled");
        List<String> allOriginalSources=Path.getAllSources(new File(originalSourcePath));
        for (String source:allOriginalSources){
            ASTParser parser= ASTParser.newParser(new AST(options).apiLevel());
            parser.setEnvironment(classPaths, sourcePaths, null, true);
            FileReader originalReader=new FileReader(source);
            BufferedReader originalBufferedReader=new BufferedReader(originalReader);
            String originalSource="";
            String line;
            while ((line=originalBufferedReader.readLine())!=null) {
                originalSource=originalSource+line+"\n";
            }
            originalBufferedReader.close();
            originalReader.close();
            parser.setSource(originalSource.toCharArray());
            ASTNode rootNode=parser.createAST(null);
            if (source.equals(originalFilePath))
                originalRootNode = rootNode;
            else
                originalNodes.add(rootNode);

            // Visit original source visitor
            OriginalSourceVisitor originalSourceVisitor=new OriginalSourceVisitor();
            rootNode.accept(originalSourceVisitor);
            originalNodeToId.put(source, originalSourceVisitor.getNodeToId());
        }

        // generate AST for patched source
        List<String> allSources=Path.getAllSources(new File(targetSourcePath));
        for (String source:allSources){
            ASTParser parser= ASTParser.newParser(new AST(options).apiLevel());
            parser.setEnvironment(classPaths, sourcePaths, null, true);
            FileReader patchedReader=new FileReader(source);
            BufferedReader patchedBufferedReader=new BufferedReader(patchedReader);
            String patchedSource="";
            String line;
            while ((line=patchedBufferedReader.readLine())!=null) {
                patchedSource=patchedSource+line+"\n";
            }
            patchedBufferedReader.close();
            patchedReader.close();
            parser.setSource(patchedSource.toCharArray());
            ASTNode rootNode=parser.createAST(null);
            if(source.equals(patchedFilePath)){
                patchedRootNode = rootNode;
                OnlyRootsClassifier classifier = new OnlyRootsClassifier(Diff.compute(originalFilePath, patchedFilePath));
                Set<Tree> updatedTrees=classifier.getUpdatedDsts();
            }
            else
                targetNodes.add(rootNode);
        }
    }

    // TODO: Instrument target program
    // TODO: Compare original AST and patched AST and instrument
}
