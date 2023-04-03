package kr.ac.unist.apr;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import com.github.gumtreediff.gen.javaparser.JavaParserGenerator;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.utils.Pair;

import kr.ac.unist.apr.gumtree.MyRootsClassifier;
import kr.ac.unist.apr.utils.Path;
import kr.ac.unist.apr.visitor.OriginalSourceVisitor;
import kr.ac.unist.apr.visitor.PatchedSourceVisitor;
import kr.ac.unist.apr.visitor.TargetSourceVisitor;

/**
 * Main class of instrumentation.
 * <p>
 *  This class will create AST for original program and target program (except test classes)
 *  and instrument the whole target program with patch.
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
    private Map<String,Pair<List<Node>,List<List<Long>>>> nodeToId=new HashMap<>();

    private String originalSourcePath;
    private String targetSourcePath;

    /**
     * Default constructor.
     * <p>
     *  Generate ASTs for original program and target program (except test classes).
     * 
     *  Original file path and patched file patch are required to compare between ASTs of them.
     *  Class paths are required to generate ASTs of target program.
     *  Every paths should be absolute.
     * 
     *  Note: This constructor will consume some time, to generate every ASTs for target program.
     * 
     *  originalFilePath and patchedFilePath should end with .java.
     *  targetSourcePath and originalSourcePath should be directory.
     * </p>
     * @param originalFilePath path of original source file of patched file
     * @param patchedFilePath path of patched file
     * @param targetSourcePath path of target program source
     * @param originalSourcePath path of original source of target program
     * @param classPaths class path of target program
     * @throws IOException if file not found or I/O errors
     */
    public Instrumenter(String originalFilePath,
                    String patchedFilePath,
                    String targetSourcePath,
                    String originalSourcePath,
                    String[] classPaths) throws IOException {
        if (originalFilePath!=null && patchedFilePath!=null) {
            this.originalFilePath=new File(originalFilePath).getAbsolutePath();
            this.patchedFilePath=new File(patchedFilePath).getAbsolutePath();
        }
        else {
            this.originalFilePath=null;
            this.patchedFilePath=null;
        }
        this.originalSourcePath=new File(originalSourcePath).getAbsolutePath();
        this.targetSourcePath=new File(targetSourcePath).getAbsolutePath();

        Main.LOGGER.log(Level.INFO, "Generate AST for original source...");
        // generate AST for original source
        List<String> allOriginalSources=Path.getAllSources(new File(originalSourcePath));
        for (String source:allOriginalSources){
            if (source.contains("kr/ac/unist/apr")) continue;
            JavaParserGenerator parser = new JavaParserGenerator();
            FileReader fReader = new FileReader(source);
            BufferedReader bReader = new BufferedReader(fReader);
            TreeContext sourceCtxt = parser.generate(bReader);

            fReader.close();
            bReader.close();
            if (originalFilePath!=null){
                File sourcFile=new File(originalFilePath);
                String sourcePath=sourcFile.getAbsolutePath();
                originalNodes.put(source,sourceCtxt);
                if (source.equals(sourcePath))
                    originalRootNode = sourceCtxt;
            }
            else
                originalNodes.put(source,sourceCtxt);
        }

        Main.LOGGER.log(Level.INFO, "Generate AST for target source...");
        // generate AST for patched source
        List<String> allSources=Path.getAllSources(new File(targetSourcePath));
        for (String source:allSources){
            if (source.contains("kr/ac/unist/apr")) continue;
            JavaParserGenerator parser = new JavaParserGenerator();
            FileReader fReader = new FileReader(source);
            BufferedReader bReader = new BufferedReader(fReader);
            if (bReader.readLine().equals("// __INSTRUMENTED__")) {
                bReader.close();
                fReader.close();
                continue;
            }
            fReader.close();
            bReader.close();
            bReader=new BufferedReader(new FileReader(source));
            
            TreeContext targetCtxt = parser.generate(bReader);

            fReader.close();
            bReader.close();
            if (patchedFilePath!=null){
                File sourcFile=new File(patchedFilePath);
                String sourcePath=sourcFile.getAbsolutePath();
                if (source.equals(sourcePath))
                    patchedRootNode = targetCtxt;
                else
                    targetNodes.put(source,targetCtxt);
            }
            else
                targetNodes.put(source,targetCtxt);
        }
    }
    
    /**
     * Instrument target program with handling patch.
     * <p>
     *  This method instruments branches with unique IDs.
     *  It also overwrites target sources.
     *  Backup the original target sources before using this method.
     * 
     *  This method guarantees that the branch IDs are always same.
     * </p>
     * @throws UnsupportedOperationException thrown by GumTreeJP
     * @throws IOException thrown at saving instrumented source
     */
    public void instrument() throws UnsupportedOperationException, IOException{
        // Visit original source visitor and get IDs
        // TODO: Cache/load this result with file
        Main.LOGGER.log(Level.INFO, "Gen IDs from original ASTs...");
        OriginalSourceVisitor originalSourceVisitor;
        for (Map.Entry<String,TreeContext> originalCtxt:originalNodes.entrySet()){
            originalSourceVisitor=new OriginalSourceVisitor();
            originalSourceVisitor.visitPreOrder(originalCtxt.getValue().getRoot().getJParserNode());
            nodeToId.put(originalCtxt.getKey(), originalSourceVisitor.getNodeToId());
        }

        // Instrument target program without patched file
        Main.LOGGER.log(Level.INFO, "Instrument target program except patched file...");
        for (Map.Entry<String,TreeContext> targetCtxt:targetNodes.entrySet()){
            TargetSourceVisitor instrumenterVisitor=new TargetSourceVisitor(nodeToId.get(targetCtxt.getKey().
                        replace(targetSourcePath, originalSourcePath)));
            Node targetNode=targetCtxt.getValue().getRoot().getJParserNode();
            instrumenterVisitor.visitPreOrder(targetNode);
            Pair<List<Node>,List<List<Long>>> finalIds=instrumenterVisitor.getResult();
            for (int i=0;i<finalIds.a.size();i++){
                Node node=finalIds.a.get(i);
                List<Long> ids=finalIds.b.get(i);
                if (node instanceof SwitchEntry)
                    instrumentSwitchCase((SwitchEntry) node, ids);
                else
                    instrumentCondition((Statement) node, ids);
            }
            
            // Save instrumented file
            FileWriter writer = new FileWriter(targetCtxt.getKey());
            writer.write("// __INSTRUMENTED__\n");
            writer.write(targetNode.toString());
            writer.close();
        }

        // Finish instrumentation if there is no patched file
        if (patchedFilePath==null) return;

        // Get differences between original source and patched source
        Main.LOGGER.log(Level.INFO, "Run GumTreeJP...");
        Matcher matcher = Matchers.getInstance().getMatcher(originalRootNode.getRoot(), patchedRootNode.getRoot());
        matcher.match();
        MyRootsClassifier classifier = new MyRootsClassifier(originalRootNode,patchedRootNode,matcher);
        Set<ITree> dstAddTrees=classifier.getDstAddTrees();
        Set<ITree> srcDelTrees=classifier.getSrcDelTrees();
        Set<ITree> srcUpdTrees=classifier.getSrcUpdTrees();
        Set<ITree> dstUpdTrees=classifier.getDstUpdTrees();
        Set<ITree> srcMvTrees=classifier.getSrcMvTrees();
        Set<ITree> dstMvTrees=classifier.getDstMvTrees();
        List<Node> dstAddSet=convertNodes(dstAddTrees);
        List<Node> srcDelSet=convertNodes(srcDelTrees);
        List<Node> srcUpdSet=convertNodes(srcUpdTrees);
        List<Node> dstUpdSet=convertNodes(dstUpdTrees);
        List<Node> srcMvSet=convertNodes(srcMvTrees);
        List<Node> dstMvSet=convertNodes(dstMvTrees);

        assert dstAddSet.size()<=1 && srcDelSet.size()<=1 && srcUpdSet.size()<=1 && dstUpdSet.size()<=1 && srcMvSet.size()<=1 && dstMvSet.size()<=1;

        Main.LOGGER.log(Level.INFO, "GumTreeJP finished, parsing results...");
        if (dstAddSet.size()!=0 && srcDelSet.size()!=0 && srcMvSet.size()!=0) {
            if (dstAddSet.get(0) instanceof BlockStmt && srcDelSet.get(0) instanceof BlockStmt){
                // Convert terrible insertion+delection to another operation
                BlockStmt srcBlock=(BlockStmt) srcDelSet.get(0);
                BlockStmt dstBlock=(BlockStmt) dstAddSet.get(0);

                if (srcBlock.getStatements().size()<dstBlock.getStatements().size()){
                    // Insertion
                    boolean found=false;
                    for (int i=0;i<srcBlock.getStatements().size();i++) {
                        Statement srcStmt=srcBlock.getStatement(i);
                        Statement dstStmt=dstBlock.getStatement(i);
                        if (!srcStmt.equals(dstStmt)){
                            srcDelSet.clear();
                            dstAddSet.clear();
                            srcMvSet.clear();
                            dstMvSet.clear();

                            dstAddSet.add(dstStmt);
                            found=true;
                            break;
                        }
                    }

                    if (!found) {
                        // Last statement is inserted
                        srcDelSet.clear();
                        dstAddSet.clear();
                        srcMvSet.clear();
                        dstMvSet.clear();

                        dstAddSet.add(dstBlock.getStatement(dstBlock.getStatements().size()-1));
                    }
                }
                else if (srcBlock.getStatements().size()>dstBlock.getStatements().size()) {
                    // Removed
                    boolean found=false;
                    for (int i=0;i<dstBlock.getStatements().size();i++) {
                        Statement srcStmt=srcBlock.getStatement(i);
                        Statement dstStmt=dstBlock.getStatement(i);
                        if (!srcStmt.equals(dstStmt)){
                            srcDelSet.clear();
                            dstAddSet.clear();
                            srcMvSet.clear();
                            dstMvSet.clear();

                            srcDelSet.add(srcBlock);
                            found=true;
                            break;
                        }
                    }

                    if (!found) {
                        // Last statement is inserted
                        srcDelSet.clear();
                        dstAddSet.clear();
                        srcMvSet.clear();
                        dstMvSet.clear();

                        srcDelSet.add(srcBlock.getStatement(srcBlock.getStatements().size()-1));
                    }
                }
                else{
                    // Updated to another statement
                    for (int i=0;i<srcBlock.getStatements().size();i++) {
                        Statement srcStmt=srcBlock.getStatement(i);
                        Statement dstStmt=dstBlock.getStatement(i);
                        if (!srcStmt.equals(dstStmt)){
                            srcDelSet.clear();
                            dstAddSet.clear();
                            srcMvSet.clear();
                            dstMvSet.clear();

                            srcUpdSet.add(srcStmt);
                            dstUpdSet.add(dstStmt);
                            break;
                        }
                    }
                }
            }
        }
        else if (dstAddSet.size()!=0 && srcMvSet.size()!=0){
            // Convert insertion+move to update
            for (Node node:new ArrayList<>(dstAddSet)){
                dstAddSet.clear();
                Node origNode=srcMvSet.get(0);
                srcMvSet.clear();
                dstMvSet.clear();

                srcUpdSet.add(origNode);
                dstUpdSet.add(node);
            }
        }
        else if (srcDelSet.size()!=0 && dstMvSet.size()!=0){
            // Convert deletion+move to update
            for (Node node:new ArrayList<>(srcDelSet)){
                srcDelSet.clear();
                Node origNode=dstMvSet.get(0);
                srcMvSet.clear();
                dstMvSet.clear();

                srcUpdSet.add(node);
                dstUpdSet.add(origNode);
            }
        }

        // Revert changes in patched AST
        Main.LOGGER.log(Level.INFO, "Reverts original and patched ASTs to instrument...");
        ModifiedNode modifiedNode=null;
        Pair<ModifiedNode,ModifiedNode> updatedNode=null;
        if (dstAddSet.size()>0){
            assert dstAddSet.size()==1;
            Main.LOGGER.log(Level.INFO, "Insertion found!");
            modifiedNode=revertInsertion(dstAddSet.get(0));
        }
        else if (srcDelSet.size()>0){
            assert srcDelSet.size()==1;
            Main.LOGGER.log(Level.INFO, "Deletion found!");
            modifiedNode=revertRemoval(srcDelSet.get(0));
        }
        else if (srcUpdSet.size()>0){
            assert srcUpdSet.size()==1;
            Main.LOGGER.log(Level.INFO, "Update found!");
            updatedNode=revertUpdate(srcUpdSet.get(0),dstUpdSet.get(0));
        }
        else if (srcMvSet.size()>0){
            assert srcMvSet.size()==1;
            Main.LOGGER.log(Level.INFO, "Move found!");
            updatedNode=revertMove(srcMvSet.get(0),dstMvSet.get(0));
        }

        // Instrument patched source
        Main.LOGGER.log(Level.INFO, "Instrument patched file...");
        PatchedSourceVisitor patchedSourceVisitor=new PatchedSourceVisitor(nodeToId.get(originalFilePath));
        patchedSourceVisitor.visitPreOrder(patchedRootNode.getRoot().getJParserNode());
        
        Pair<List<Node>,List<List<Long>>> finalIds=patchedSourceVisitor.getResult();
        for (int i=0;i<finalIds.a.size();i++){
            Node node=finalIds.a.get(i);
            List<Long> ids=finalIds.b.get(i);
            if (node instanceof SwitchEntry)
                instrumentSwitchCase((SwitchEntry) node, ids);
            else
                instrumentCondition((Statement) node, ids);
        }

        // Rollback changes in patched AST
        Main.LOGGER.log(Level.INFO, "Roll back reverted ASTs...");
        if (dstAddSet.size()>0){
            assert dstAddSet.size()==1;
            rollbackInsertion(modifiedNode);
        }
        else if (srcDelSet.size()>0){
            assert srcDelSet.size()==1;
            rollbackRemoval(modifiedNode);
        }
        else if (srcUpdSet.size()>0){
            assert srcUpdSet.size()==1;
            rollbackUpdate(updatedNode);
        }
        else if (srcMvSet.size()>0){
            assert srcMvSet.size()==1;
            rollbackMove(updatedNode);
        }

        Main.LOGGER.log(Level.INFO, "Save patched file...");
        // Save patched file
        FileWriter writer = new FileWriter(patchedFilePath);
        writer.write("// __INSTRUMENTED__\n");
        writer.write(patchedRootNode.getRoot().getJParserNode().toString());
        writer.close();
    }

    /**
     * Information class of a patched node.
     * <p>
     *  For insertion and removal, this class represents the inserted/removed node.
     *  For update and move, this class represents the updated/moved node from original and updated/moved node to patch.
     * </p>
     * @see modifiedStmt
     * @see Instrumenter#revertInsertion
     * @see Instrumenter#revertRemoval
     * @see Instrumenter#revertUpdate
     * @see Instrumenter#revertMove
     * @see Instrumenter#rollbackInsertion
     * @see Instrumenter#rollbackRemoval
     * @see Instrumenter#rollbackUpdate
     * @see Instrumenter#rollbackMove
     */
    static class ModifiedNode {
        /**
         * The inserted/removed/updated/moved node.
         */
        Node node;
        int index;
        Node parent;
        Node beforeNode;
        public ModifiedNode(Node node, int index, Node parent,Node beforeNode) {
            this.node = node;
            this.index = index;
            this.parent = parent;
            this.beforeNode=beforeNode;
        }
    }

    /**
     * Information class if patch modified a special statement.
     * <p>
     *  For update, if a patch modifies a children of {@link IfStmt}, {@link WhileStmt}, {@link ForStmt} or {@link SwitchEntry}, this class represents which child node is modified.
     * </p>
     * @see Instrumenter#revertUpdate
     * @see Instrumenter#rollbackUpdate
     */
    static class ModifiedStmt extends ModifiedNode {
        Class<? extends Node> parentClass;
        Method nodeGetter;
        Method nodeSetter;
        /**
         * Constructor for variable number of children (e.g. {@link SwitchEntry#getStatements()}).
         * @param node modified node
         * @param index index of modified node in children
         * @param parent parent node ({@link IfStmt}, {@link WhileStmt}, {@link ForStmt} or {@link SwitchEntry})
         * @param nodeGetter getter method of children (e.g. {@link SwitchEntry#getStatements()}
         * @param nodeSetter setter method of children (e.g. {@link SwitchEntry#setStatements(NodeList)})
         */
        public ModifiedStmt(Node node,int index, Node parent,Method nodeGetter,Method nodeSetter) {
            super(node, index, parent,null);
            this.parentClass = parent.getClass();
            this.nodeGetter = nodeGetter;
            this.nodeSetter = nodeSetter;
        }
        /**
         * Constructor for single child (e.g. {@link IfStmt#getThenStmt()}).
         * @param node modified node
         * @param parent parent node ({@link IfStmt}, {@link WhileStmt}, {@link ForStmt} or {@link SwitchEntry})
         * @param nodeGetter getter method of children (e.g. {@link IfStmt#getThenStmt()}
         * @param nodeSetter setter method of children (e.g. {@link IfStmt#setThenStmt(Statement)})
         */
        public ModifiedStmt(Node node, Node parent,Method nodeGetter,Method nodeSetter) {
            this(node, 0, parent, nodeGetter, nodeSetter);
        }
    }

    /**
     * Convert {@link ITree}s into {@link Statement}s.
     * @param trees {@link ITree}s to be converted
     * @return {@link Statement}s
     */
    private List<Node> convertNodes(Set<ITree> trees){
        List<Node> result=new ArrayList<>();
        for (ITree tree:trees){
            result.add(tree.getJParserNode());
        }

        List<Node> finalResult=new ArrayList<>();
        for (Node node:result){
            Node curNode=node;
            // while (!(curNode instanceof Statement || curNode instanceof MethodDeclaration || curNode instanceof ConstructorDeclaration ||
            //                 curNode instanceof FieldDeclaration || curNode instanceof VariableDeclarationExpr))
            //     curNode=curNode.getParentNode().get();
            if (!(curNode instanceof LineComment))
                finalResult.add(curNode);
        }

        return finalResult;
    }

    /**
     * Revert block-to-block move.
     * <p>
     *  Revert move in {@link BlockStmt}.
     * 
     *  Revert move by move the modified node to original location.
     *  This will return 2 {@link ModifiedNode}s, first one represents the change in original AST
     *   and second one represents the change in patch AST.
     * 
     *  Call {@link Instrumenter#rollbackMove} after the instrumentation.
     * </p>
     * @param movedFromOriginal moved node in original AST
     * @param movedToPatch moved node in patch AST
     * @return pair of {@link ModifiedNode}s
     * @see Instrumenter#rollbackMove
     */
    protected Pair<ModifiedNode,ModifiedNode> revertMove(Node movedFromOriginal,Node movedToPatch) {
        Node curNodeFromOrig=movedFromOriginal;
        Node curNodeToPatch=movedToPatch;
        while (!(curNodeFromOrig.getParentNode().get() instanceof BlockStmt)) {
            curNodeFromOrig=curNodeFromOrig.getParentNode().get();
        }
        while (!(curNodeToPatch.getParentNode().get() instanceof BlockStmt)) {
            curNodeToPatch=curNodeToPatch.getParentNode().get();
        }
        if (curNodeFromOrig.getParentNode().get() instanceof BlockStmt &&
                        movedToPatch.getParentNode().get() instanceof BlockStmt){
            // A node is moved from a block to another block.
            BlockStmt blockFrom=(BlockStmt)curNodeFromOrig.getParentNode().get();
            BlockStmt blockTo=(BlockStmt)curNodeToPatch.getParentNode().get();

            int indexFrom=blockFrom.getStatements().indexOf(curNodeFromOrig);
            int indexTo=blockTo.getStatements().indexOf(curNodeToPatch);

            blockTo.getStatements().remove(curNodeToPatch);
            blockTo.getStatements().add(indexFrom, (Statement) curNodeToPatch);

            ModifiedNode beforeNode,afterNode;
            if (indexFrom==0){
                beforeNode=new ModifiedNode(curNodeFromOrig,indexFrom,blockFrom,null);
            }
            else{
                beforeNode=new ModifiedNode(curNodeFromOrig,indexFrom,blockFrom,blockFrom.getStatements().get(indexFrom-1));
            }

            if (indexTo==0){
                afterNode=new ModifiedNode(curNodeToPatch,indexTo,blockTo,null);
            }
            else{
                afterNode=new ModifiedNode(curNodeToPatch,indexTo,blockTo,blockTo.getStatements().get(indexTo-1));
            }

            return new Pair<Instrumenter.ModifiedNode,Instrumenter.ModifiedNode>(beforeNode, afterNode);
        }
        else{
            throw new RuntimeException("Move operation only supports moving a statement in a block.");
        }
    }

    /**
     * Revert removal.
     * <p>
     *  Revert {@link Statement} removal in {@link BlockStmt} or {@link MethodDeclaration} removal in {@link TypeDeclaraion}.
     * 
     *  This method removes the removed node from the original AST.
     *  This returns a {@link ModifiedNode} represents the removed node in original AST.
     * 
     *  Call {@link Instrumenter#rollbackRemoval} after the instrumentation.
     * 
     *  Note: `removedNode` should be in the original AST and not in the patch AST.
     * </p>
     * @param removedNode removed node, in original AST
     * @return information of removed node
     * @see Instrumenter#rollbackRemoval
     */
    protected ModifiedNode revertRemoval(Node removedNode) {
        Node curNode=removedNode;
        while (!(curNode instanceof MethodDeclaration || curNode instanceof ConstructorDeclaration ||
                        curNode instanceof FieldDeclaration || curNode instanceof VariableDeclarationExpr || curNode.getParentNode().get() instanceof BlockStmt))
            curNode=curNode.getParentNode().get();

        if (curNode instanceof MethodDeclaration || curNode instanceof ConstructorDeclaration){
            // Removal method declaration
            if (!(removedNode.getParentNode().get() instanceof TypeDeclaration)){
                throw new RuntimeException("Removing method decl only supports for Class/Interface/Enum.");
            }

            ModifiedNode modifiedNode=null;
            if (curNode.getParentNode().get() instanceof ClassOrInterfaceDeclaration){
                ClassOrInterfaceDeclaration typeDecl=(ClassOrInterfaceDeclaration)curNode.getParentNode().get();
                int index=typeDecl.getMembers().indexOf(curNode);
                typeDecl.getMembers().remove(curNode);
                if (index==0){
                    modifiedNode=new ModifiedNode(curNode,index,typeDecl,null);
                }
                else{
                    modifiedNode=new ModifiedNode(curNode,index,typeDecl,typeDecl.getMembers().get(index-1));
                }
            }
            else if (curNode.getParentNode().get() instanceof EnumDeclaration) {
                EnumDeclaration typeDecl=(EnumDeclaration)curNode.getParentNode().get();
                int index=typeDecl.getMembers().indexOf(curNode);
                typeDecl.getMembers().remove(curNode);
                if (index==0){
                    modifiedNode=new ModifiedNode(curNode,index,typeDecl,null);
                }
                else{
                    modifiedNode=new ModifiedNode(curNode,index,typeDecl,typeDecl.getMembers().get(index-1));
                }
            }
            else {
                // Ignore AnnotationDeclaration: no method decl, ignore RecordDeclaration: only Java 14+ supported
                throw new RuntimeException("Removing method decl only supports for Class/Interface/Enum.");
            }

            return modifiedNode;
        }
        else{
            // Removal statement
            if (!(curNode instanceof Statement)){
                throw new RuntimeException("Removed node should be Statement.");
            }
            if (!(curNode.getParentNode().get() instanceof BlockStmt)) {
                throw new RuntimeException("Removing statement only supports in the BlockStmt.");
            }

            BlockStmt block=(BlockStmt)curNode.getParentNode().get();
            int index=block.getStatements().indexOf(curNode);
            block.getStatements().remove(curNode);
            ModifiedNode modifiedNode;
            if (index==0){
                modifiedNode=new ModifiedNode(curNode,index,block,null);
            }
            else{
                modifiedNode=new ModifiedNode(curNode,index,block,block.getStatements().get(index-1));
            }
            return modifiedNode;
        }
    }

    /**
     * Revert insertion.
     * <p>
     *  Revert {@link Statement} insertion in {@link BlockStmt}.
     *  
     *  This method removes the inserted node from the patch AST.
     *  This returns a {@link ModifiedNode} represents the inserted node in patch AST.
     * 
     *  Call {@link Instrumenter#rollbackInsertion} after the instrumentation.
     * 
     *  Note: `insertedNode` should be in the patch AST and not in the original AST.
     * </p>
     * @param insertedNode inserted node, in patch AST
     * @return information of inserted node
     * @see Instrumenter#rollbackInsertion
     */
    protected ModifiedNode revertInsertion(Node insertedNode) {
        Node curNode=insertedNode;
        while (!(curNode.getParentNode().get() instanceof BlockStmt)){
            curNode=curNode.getParentNode().get();
        }

        BlockStmt block=(BlockStmt)curNode.getParentNode().get();
        int index=block.getStatements().indexOf(curNode);
        block.getStatements().remove(curNode);
        ModifiedNode modifiedNode;
        if (index==0){
            modifiedNode=new ModifiedNode(curNode,index,block,null);
        }
        else{
            modifiedNode=new ModifiedNode(curNode,index,block,block.getStatements().get(index-1));
        }
        return modifiedNode;
    }

    /**
     * Revert update.
     * <p>
     *  Revert update {@link Statement}, initializer of {@link FieldDeclaration} or initializer of {@link VariableDeclaraionExpr}.
     * 
     *  If the arguments are {@link FieldDeclaration} or {@link VariableDeclaraionExpr}, this method will remove the initializer of the arguments.
     *  If the arguments are {@link Statement}, this method will replace the statement with the original statement.
     * 
     *  This method returns 2 {@link ModifiedNode} represents and updated nodes in original and patched AST.
     * 
     *  Call {@link Instrumenter#rollbackUpdate} after the instrumentation.
     * 
     *  Note: `beforeNode` and `afterNode` should be in the original and patch AST respectively.
     * </p>
     * @param beforeNode original node, in original AST
     * @param afterNode updated node, in patch AST
     * @return information of original and updated node
     * @see Instrumenter#rollbackUpdate
     */
    protected Pair<ModifiedNode,ModifiedNode> revertUpdate(Node beforeNode,Node afteNode) {
        if (beforeNode instanceof VariableDeclarator){
            // Update field or variable declaration
            if (!(beforeNode.getParentNode().get() instanceof FieldDeclaration || beforeNode.getParentNode().get() instanceof VariableDeclarationExpr) ||
                            !(afteNode.getParentNode().get() instanceof TypeDeclaration || afteNode.getParentNode().get() instanceof VariableDeclarationExpr)){
                throw new RuntimeException("Updating variable decl only supports field/local variable.");
            }

            if (beforeNode.getParentNode().get() instanceof FieldDeclaration){
                VariableDeclarator fieldBefore=(VariableDeclarator)beforeNode;
                VariableDeclarator fieldAfter=(VariableDeclarator)afteNode;
                VariableDeclarator fieldBeforeClone=fieldBefore.clone();
                VariableDeclarator fieldAfterClone=fieldAfter.clone();
                fieldBeforeClone.removeInitializer();
                fieldAfterClone.removeInitializer();

                FieldDeclaration fieldDeclBefore=(FieldDeclaration)beforeNode.getParentNode().get();
                FieldDeclaration fieldDeclAfter=(FieldDeclaration)afteNode.getParentNode().get();
                int indexBefore=fieldDeclBefore.getVariables().indexOf(fieldBefore);
                int indexAfter=fieldDeclAfter.getVariables().indexOf(fieldAfter);
                fieldDeclBefore.getVariables().set(indexBefore, fieldBeforeClone);
                fieldDeclAfter.getVariables().set(indexAfter, fieldAfterClone);

                ModifiedNode beforeModified=new ModifiedNode(fieldBeforeClone,indexBefore,fieldDeclBefore,fieldBefore);
                ModifiedNode afterModified=new ModifiedNode(fieldAfterClone,indexAfter,fieldDeclAfter,fieldAfter);
                return new Pair<ModifiedNode,ModifiedNode>(beforeModified, afterModified);
            }
            else if (beforeNode.getParentNode().get() instanceof VariableDeclarationExpr){
                VariableDeclarator varBefore=(VariableDeclarator)beforeNode;
                VariableDeclarator varAfter=(VariableDeclarator)afteNode;
                VariableDeclarator varBeforeClone=varBefore.clone();
                VariableDeclarator varAfterClone=varAfter.clone();
                varBeforeClone.removeInitializer();
                varAfterClone.removeInitializer();

                VariableDeclarationExpr varDeclBefore=(VariableDeclarationExpr)beforeNode.getParentNode().get();
                VariableDeclarationExpr varDeclAfter=(VariableDeclarationExpr)afteNode.getParentNode().get();
                int indexBefore=varDeclBefore.getVariables().indexOf(varBefore);
                int indexAfter=varDeclAfter.getVariables().indexOf(varAfter);
                varDeclBefore.getVariables().set(indexBefore, varBeforeClone);
                varDeclAfter.getVariables().set(indexAfter, varAfterClone);

                ModifiedNode beforeModified=new ModifiedNode(varBeforeClone,indexBefore,varDeclBefore,varBefore);
                ModifiedNode afterModified=new ModifiedNode(varAfterClone,indexAfter,varDeclAfter,varAfter);
                return new Pair<ModifiedNode,ModifiedNode>(beforeModified, afterModified);
            }
            else{
                throw new RuntimeException("Updating variable decl only supports field/local variable.");
            }
        }
        else if (beforeNode instanceof Statement || beforeNode instanceof Expression){
            // Update normal statement
            Node curBefore=beforeNode;
            Node curAfter=afteNode;
            while (!(curBefore.getParentNode().get() instanceof BlockStmt) &&
                    !(curBefore.getParentNode().get() instanceof IfStmt) &&
                    !(curBefore.getParentNode().get() instanceof WhileStmt) &&
                    !(curBefore.getParentNode().get() instanceof ForStmt) &&
                    !(curBefore.getParentNode().get() instanceof DoStmt) &&
                    !(curBefore.getParentNode().get() instanceof SwitchEntry)){
                curBefore=curBefore.getParentNode().get();
                curAfter=curAfter.getParentNode().get();
            }
            assert curAfter.getParentNode().get().getClass().getName().equals(curBefore.getParentNode().get().getClass().getName());

            if (curBefore.getParentNode().get() instanceof BlockStmt){
                // Handle block statement
                BlockStmt blockBefore=(BlockStmt)curBefore.getParentNode().get();
                BlockStmt blockAfter=(BlockStmt)curAfter.getParentNode().get();
                int indexBefore=blockBefore.getStatements().indexOf(curBefore);
                int indexAfter=blockAfter.getStatements().indexOf(curAfter);
                assert indexBefore==indexAfter;
                blockAfter.getStatements().set(indexAfter, (Statement)curBefore.clone());

                ModifiedNode beforeModified=new ModifiedNode(curBefore,indexBefore,blockBefore,blockBefore.getStatements().get(indexBefore));
                ModifiedNode afterModified=new ModifiedNode(curAfter,indexAfter,blockAfter,blockAfter.getStatements().get(indexAfter));
                return new Pair<ModifiedNode,ModifiedNode>(beforeModified, afterModified);
            }
            else if (curBefore.getParentNode().get() instanceof IfStmt) {
                // Handle if statement
                IfStmt ifBefore=(IfStmt)curBefore.getParentNode().get();
                IfStmt ifAfter=(IfStmt)curAfter.getParentNode().get();

                Method methodGetter=null;
                Method methodSetter=null;
                if (ifBefore.getThenStmt().equals(curBefore)){
                    try {
                        methodGetter=ifBefore.getClass().getMethod("getThenStmt");
                        methodSetter=ifBefore.getClass().getMethod("setThenStmt",Statement.class);

                        ifAfter.setThenStmt((Statement) curBefore);
                    } catch (NoSuchMethodException | SecurityException e) {
                        e.printStackTrace();
                        System.exit(1);
                    }
                }
                else if (ifBefore.getElseStmt().isPresent() && ifBefore.getElseStmt().get().equals(curBefore)){
                    try {
                        methodGetter=ifBefore.getClass().getMethod("getElseStmt");
                        methodSetter=ifBefore.getClass().getMethod("setElseStmt",Statement.class);

                        ifAfter.setElseStmt((Statement) curBefore);
                    } catch (NoSuchMethodException | SecurityException e) {
                        e.printStackTrace();
                        System.exit(1);
                    }
                }
                else if (ifBefore.getCondition().equals(curBefore)){
                    try {
                        methodGetter=ifBefore.getClass().getMethod("getCondition");
                        methodSetter=ifBefore.getClass().getMethod("setCondition",Expression.class);

                        ifAfter.setCondition((Expression) curBefore);
                    } catch (NoSuchMethodException | SecurityException e) {
                        e.printStackTrace();
                        System.exit(1);
                    }
                }
                else{
                    throw new RuntimeException("Updating if stmt only supports condition/then/else.");
                }

                ModifiedNode beforeModified=new ModifiedStmt(curBefore,ifBefore,methodGetter,methodSetter);
                ModifiedNode afterModified=new ModifiedStmt(curAfter,ifAfter,methodGetter,methodSetter);
                return new Pair<ModifiedNode,ModifiedNode>(beforeModified, afterModified);
            }
            else if (curBefore.getParentNode().get() instanceof WhileStmt) {
                // Handle while statement
                WhileStmt whileBefore=(WhileStmt)curBefore.getParentNode().get();
                WhileStmt whileAfter=(WhileStmt)curAfter.getParentNode().get();

                Method methodGetter=null;
                Method methodSetter=null;
                if (whileBefore.getBody().equals(curBefore)){
                    try {
                        methodGetter=whileBefore.getClass().getMethod("getBody");
                        methodSetter=whileBefore.getClass().getMethod("setBody",Statement.class);

                        whileAfter.setBody((Statement) curBefore);
                    } catch (NoSuchMethodException | SecurityException e) {
                        e.printStackTrace();
                        System.exit(1);
                    }
                }
                else if (whileBefore.getCondition().equals(curBefore)){
                    try {
                        methodGetter=whileBefore.getClass().getMethod("getCondition");
                        methodSetter=whileBefore.getClass().getMethod("setCondition",Expression.class);

                        whileAfter.setCondition((Expression) curBefore);
                    } catch (NoSuchMethodException | SecurityException e) {
                        e.printStackTrace();
                        System.exit(1);
                    }
                }
                else{
                    throw new RuntimeException("Updating while stmt only supports condition/body.");
                }

                ModifiedNode beforeModified=new ModifiedStmt(curBefore,whileBefore,methodGetter,methodSetter);
                ModifiedNode afterModified=new ModifiedStmt(curAfter,whileAfter,methodGetter,methodSetter);
                return new Pair<ModifiedNode,ModifiedNode>(beforeModified, afterModified);
            }
            else if (curBefore.getParentNode().get() instanceof ForStmt) {
                // Handle for statement
                ForStmt forBefore=(ForStmt)curBefore.getParentNode().get();
                ForStmt forAfter=(ForStmt)curAfter.getParentNode().get();

                Method methodGetter=null;
                Method methodSetter=null;
                int indexBefore=0;
                int indexAfter=0;
                if (forBefore.getBody().equals(curBefore)){
                    try {
                        methodGetter=forBefore.getClass().getMethod("getBody");
                        methodSetter=forBefore.getClass().getMethod("setBody",Statement.class);

                        forAfter.setBody((Statement) curBefore);
                    } catch (NoSuchMethodException | SecurityException e) {
                        e.printStackTrace();
                        System.exit(1);
                    }
                }
                else if (forBefore.getInitialization().contains(curBefore)){
                    try {
                        NodeList<Expression> beforeInit=forBefore.getInitialization();
                        NodeList<Expression> afterInit=forAfter.getInitialization();

                        methodGetter=forBefore.getClass().getMethod("getInitialization");
                        methodSetter=forBefore.getClass().getMethod("setInitialization",beforeInit.getClass());
                        indexBefore=beforeInit.indexOf(curBefore);
                        indexAfter=afterInit.indexOf(curAfter);

                        afterInit.set(indexAfter, (Expression) curBefore);
                        forAfter.setInitialization(afterInit);
                    } catch (NoSuchMethodException | SecurityException e) {
                        e.printStackTrace();
                        System.exit(1);
                    }
                }
                else if (forBefore.getUpdate().contains(curBefore)){
                    try {
                        NodeList<Expression> beforeUpdate=forBefore.getUpdate();
                        NodeList<Expression> afterUpdate=forAfter.getUpdate();

                        methodGetter=forBefore.getClass().getMethod("getUpdate");
                        methodSetter=forBefore.getClass().getMethod("setUpdate",beforeUpdate.getClass());
                        indexBefore=beforeUpdate.indexOf(curBefore);
                        indexAfter=afterUpdate.indexOf(curAfter);

                        afterUpdate.set(indexAfter, (Expression) curBefore);
                        forAfter.setUpdate(afterUpdate);
                    } catch (NoSuchMethodException | SecurityException e) {
                        e.printStackTrace();
                        System.exit(1);
                    }
                }
                else if (forBefore.getCompare().isPresent() && forBefore.getCompare().get().equals(curBefore)){
                    try {
                        methodGetter=forBefore.getClass().getMethod("getCompare");
                        methodSetter=forBefore.getClass().getMethod("setCompare",Expression.class);

                        forAfter.setCompare((Expression) curBefore);
                    } catch (NoSuchMethodException | SecurityException e) {
                        e.printStackTrace();
                        System.exit(1);
                    }
                }
                else{
                    throw new RuntimeException("Updating for stmt only supports initialization/compare/update/body.");
                }

                ModifiedNode beforeModified=new ModifiedStmt(curBefore,indexBefore,forBefore,methodGetter,methodSetter);
                ModifiedNode afterModified=new ModifiedStmt(curAfter,indexAfter,forAfter,methodGetter,methodSetter);
                return new Pair<ModifiedNode,ModifiedNode>(beforeModified, afterModified);
            }
            else if (curBefore.getParentNode().get() instanceof DoStmt) {
                // Handle while statement
                DoStmt whileBefore=(DoStmt)curBefore.getParentNode().get();
                DoStmt whileAfter=(DoStmt)curAfter.getParentNode().get();

                Method methodGetter=null;
                Method methodSetter=null;
                if (whileBefore.getBody().equals(curBefore)){
                    try {
                        methodGetter=whileBefore.getClass().getMethod("getBody");
                        methodSetter=whileBefore.getClass().getMethod("setBody",Statement.class);

                        whileAfter.setBody((Statement) curBefore);
                    } catch (NoSuchMethodException | SecurityException e) {
                        e.printStackTrace();
                        System.exit(1);
                    }
                }
                else if (whileBefore.getCondition().equals(curBefore)){
                    try {
                        methodGetter=whileBefore.getClass().getMethod("getCondition");
                        methodSetter=whileBefore.getClass().getMethod("setCondition",Expression.class);

                        whileAfter.setCondition((Expression) curBefore);
                    } catch (NoSuchMethodException | SecurityException e) {
                        e.printStackTrace();
                        System.exit(1);
                    }
                }
                else{
                    throw new RuntimeException("Updating do-while stmt only supports condition/body.");
                }

                ModifiedNode beforeModified=new ModifiedStmt(curBefore,whileBefore,methodGetter,methodSetter);
                ModifiedNode afterModified=new ModifiedStmt(curAfter,whileAfter,methodGetter,methodSetter);
                return new Pair<ModifiedNode,ModifiedNode>(beforeModified, afterModified);
            }
            else if (curBefore.getParentNode().get() instanceof ForEachStmt) {
                // Handle while statement
                ForEachStmt whileBefore=(ForEachStmt)curBefore.getParentNode().get();
                ForEachStmt whileAfter=(ForEachStmt)curAfter.getParentNode().get();

                Method methodGetter=null;
                Method methodSetter=null;
                if (whileBefore.getBody().equals(curBefore)){
                    try {
                        methodGetter=whileBefore.getClass().getMethod("getBody");
                        methodSetter=whileBefore.getClass().getMethod("setBody",Statement.class);

                        whileAfter.setBody((Statement) curBefore);
                    } catch (NoSuchMethodException | SecurityException e) {
                        e.printStackTrace();
                        System.exit(1);
                    }
                }
                else{
                    throw new RuntimeException("Updating for-each stmt only supports body.");
                }

                ModifiedNode beforeModified=new ModifiedStmt(curBefore,whileBefore,methodGetter,methodSetter);
                ModifiedNode afterModified=new ModifiedStmt(curAfter,whileAfter,methodGetter,methodSetter);
                return new Pair<ModifiedNode,ModifiedNode>(beforeModified, afterModified);
            }
            else if (curBefore.getParentNode().get() instanceof SwitchEntry) {
                // Handle while statement
                SwitchEntry caseBefore=(SwitchEntry)curBefore.getParentNode().get();
                SwitchEntry caseAfter=(SwitchEntry)curAfter.getParentNode().get();

                Method methodGetter=null;
                Method methodSetter=null;
                int indexBefore=0;
                int indexAfter=0;
                if (caseBefore.getStatements().contains(curBefore)){
                    try {
                        NodeList<Statement> beforeUpdate=caseBefore.getStatements();
                        NodeList<Statement> afterUpdate=caseAfter.getStatements();

                        methodGetter=caseBefore.getClass().getMethod("getStatements");
                        methodSetter=caseBefore.getClass().getMethod("setStatements",beforeUpdate.getClass());
                        indexBefore=beforeUpdate.indexOf(curBefore);
                        indexAfter=afterUpdate.indexOf(curAfter);

                        afterUpdate.set(indexAfter, (Statement) curBefore);
                        caseAfter.setStatements(afterUpdate);
                    } catch (NoSuchMethodException | SecurityException e) {
                        e.printStackTrace();
                        System.exit(1);
                    }
                }
                else{
                    throw new RuntimeException("Updating switch entry only supports statements.");
                }

                ModifiedNode beforeModified=new ModifiedStmt(curBefore,indexBefore,caseBefore,methodGetter,methodSetter);
                ModifiedNode afterModified=new ModifiedStmt(curAfter,indexAfter,caseAfter,methodGetter,methodSetter);
                return new Pair<ModifiedNode,ModifiedNode>(beforeModified, afterModified);
            }
        }
        throw new RuntimeException("Updating node only supports statements: "+beforeNode.getClass().getName());
    }

    /**
     * Rollback move of a node from a location to another location in a {@link BlockStmt}.
     * <p>
     *  This method return back the node to its original location.
     * </p>
     * @param movedInfos information of the moved node
     * @see Instrumenter#revertMove
     */
    protected void rollbackMove(Pair<ModifiedNode,ModifiedNode> movedInfos) {
        // A node is moved from a block to another block.
        ModifiedNode beforeMoved=movedInfos.a;
        ModifiedNode afterMoved=movedInfos.b;
        BlockStmt blockFrom=(BlockStmt)beforeMoved.parent;
        BlockStmt blockTo=(BlockStmt)afterMoved.parent;

        if (blockFrom.getStatements().size()==blockTo.getStatements().size()){
            blockTo.getStatements().remove(afterMoved.node);
            blockTo.getStatements().add(afterMoved.index, (Statement)afterMoved.node);
        }
        else{
            blockTo.getStatements().remove(afterMoved.node);
            blockTo.getStatements().add(afterMoved.index+1, (Statement)afterMoved.node);
        }
        
    }

    /**
     * Rollback removal patch in {@link BlockStmt} or {@link TypeDeclaration}.
     * <p>
     *  This method return back the removed node in original AST.
     * </p>
     * @param removedInfo information of the removed node in the patched AST
     * @see Instrumenter#revertRemoval
     */
    protected void rollbackRemoval(ModifiedNode removedInfo) {
        if (removedInfo.node instanceof MethodDeclaration){
            // Removal method declaration
            if (!(removedInfo.parent instanceof TypeDeclaration)){
                throw new RuntimeException("Removing method only supports Class/Interface/Enum.");
            }

            if (removedInfo.parent instanceof ClassOrInterfaceDeclaration) {
                ClassOrInterfaceDeclaration typeDecl=(ClassOrInterfaceDeclaration)removedInfo.parent;
                if (removedInfo.index==0)
                    typeDecl.getMembers().addFirst((MethodDeclaration) removedInfo.node);
                else
                    typeDecl.getMembers().addAfter((MethodDeclaration) removedInfo.node, (BodyDeclaration<?>) removedInfo.beforeNode);
            }
            else if (removedInfo.parent instanceof EnumDeclaration) {
                EnumDeclaration typeDecl=(EnumDeclaration)removedInfo.parent;
                if (removedInfo.index==0)
                    typeDecl.getMembers().addFirst((MethodDeclaration) removedInfo.node);
                else
                    typeDecl.getMembers().addAfter((MethodDeclaration) removedInfo.node, (BodyDeclaration<?>) removedInfo.beforeNode);
            }
            else {
                // Ignore AnnotationDeclaration: no method decl, ignore RecordDeclaration: only Java 14+ supported
                throw new RuntimeException("Removing method only supports Class/Interface/Enum.");
            }
        }
        else if (removedInfo.node instanceof ConstructorDeclaration){
            // Removal method declaration
            if (!(removedInfo.parent instanceof TypeDeclaration)){
                throw new RuntimeException("Removing method only supports Class/Interface/Enum.");
            }

            if (removedInfo.parent instanceof ClassOrInterfaceDeclaration) {
                ClassOrInterfaceDeclaration typeDecl=(ClassOrInterfaceDeclaration)removedInfo.parent;
                if (removedInfo.index==0)
                    typeDecl.getMembers().addFirst((ConstructorDeclaration) removedInfo.node);
                else
                    typeDecl.getMembers().addAfter((ConstructorDeclaration) removedInfo.node, (BodyDeclaration<?>) removedInfo.beforeNode);
            }
            else if (removedInfo.parent instanceof EnumDeclaration) {
                EnumDeclaration typeDecl=(EnumDeclaration)removedInfo.parent;
                if (removedInfo.index==0)
                    typeDecl.getMembers().addFirst((ConstructorDeclaration) removedInfo.node);
                else
                    typeDecl.getMembers().addAfter((ConstructorDeclaration) removedInfo.node, (BodyDeclaration<?>) removedInfo.beforeNode);
            }
            else {
                // Ignore AnnotationDeclaration: no method decl, ignore RecordDeclaration: only Java 14+ supported
                throw new RuntimeException("Removing method only supports Class/Interface/Enum.");
            }
        }
        else{
            // Removal statement
            if (!(removedInfo.node instanceof Statement)){
                throw new RuntimeException("Removed node should be Statement.");
            }
            if (!(removedInfo.parent instanceof BlockStmt)) {
                throw new RuntimeException("Removing statement only supports BlockStmt.");
            }

            BlockStmt block=(BlockStmt)removedInfo.parent;
            int index=removedInfo.index;
            
            if (block.getStatements().size()>0 && block.getStatements().get(0).toString().startsWith("kr.ac.unist.apr")){
                if (index==0)
                    block.getStatements().add(1,(Statement)removedInfo.node);
                else
                    block.getStatements().addAfter((Statement)removedInfo.node, (Statement)removedInfo.beforeNode);    
            }
            else{
                if (index==0)
                    block.getStatements().addFirst((Statement)removedInfo.node);
                else
                    block.getStatements().addAfter((Statement)removedInfo.node, (Statement)removedInfo.beforeNode);
            }
        }
    }

    /**
     * Rollback insertion patch in {@link BlockStmt}.
     * <p>
     *  This method return back the inserted node in patched AST.
     * </p>
     * @param insertedInfo information of the inserted node in the patched AST
     * @see Instrumenter#revertInsertion
     */
    protected void rollbackInsertion(ModifiedNode insertedInfo) {
        if (!(insertedInfo.node instanceof Statement)){
            throw new RuntimeException("Inserted node should be Statement.");
        }
        if (!(insertedInfo.parent instanceof BlockStmt)) {
            throw new RuntimeException("Inserting statement only supports BlockStmt.");
        }

        BlockStmt block=(BlockStmt)insertedInfo.parent;
        int index=insertedInfo.index;
        
        if (block.getStatements().size()>0 && block.getStatements().get(0).toString().startsWith("kr.ac.unist.apr")){
            if (index==0)
                block.getStatements().add(1,(Statement) insertedInfo.node);
            else
                block.getStatements().addAfter((Statement) insertedInfo.node, (Statement) insertedInfo.beforeNode);
        }
        else{
            if (index==0)
                block.getStatements().addFirst((Statement) insertedInfo.node);
            else
                block.getStatements().addAfter((Statement) insertedInfo.node, (Statement) insertedInfo.beforeNode);
        }
    }

    /**
     * Rollback update patch in {@link Statement}, {@link FieldDeclaration} or {@link VariableDeclarationExpr}.
     * <p>
     *  This method return back the initializer if {@link FieldDeclaration} or {@link VariableDeclarationExpr}.
     *  This method return back the child node if {@link Statement}.
     * </p>
     * @param updatedInfos information of the updated node
     * @see Instrumenter#revertUpdate
     */
    protected void rollbackUpdate(Pair<ModifiedNode,ModifiedNode> updatedInfos) {
        ModifiedNode beforeUpdated=updatedInfos.a;
        ModifiedNode afterUpdated=updatedInfos.b;
        if (beforeUpdated.node instanceof VariableDeclarator){
            // Update field or variable declaration
            if (!(beforeUpdated.parent instanceof FieldDeclaration || beforeUpdated.parent instanceof VariableDeclarationExpr) ||
                            !(afterUpdated.parent instanceof TypeDeclaration || afterUpdated.parent instanceof VariableDeclarationExpr)){
                throw new RuntimeException("Updating field or variable only supports FieldDeclaration or VariableDeclarationExpr.");
            }

            if (beforeUpdated.parent instanceof FieldDeclaration){
                FieldDeclaration fieldDeclBefore=(FieldDeclaration)beforeUpdated.parent;
                FieldDeclaration fieldDeclAfter=(FieldDeclaration)afterUpdated.parent;
                int indexBefore=beforeUpdated.index;
                int indexAfter=afterUpdated.index;
                fieldDeclBefore.getVariables().set(indexBefore, (VariableDeclarator) beforeUpdated.beforeNode);
                fieldDeclAfter.getVariables().set(indexAfter, (VariableDeclarator) afterUpdated.beforeNode);
                
            }
            else if (beforeUpdated.parent instanceof VariableDeclarationExpr){
                VariableDeclarationExpr varDeclBefore=(VariableDeclarationExpr)beforeUpdated.parent;
                VariableDeclarationExpr varDeclAfter=(VariableDeclarationExpr)afterUpdated.parent;
                int indexBefore=beforeUpdated.index;
                int indexAfter=afterUpdated.index;
                varDeclBefore.getVariables().set(indexBefore, (VariableDeclarator) beforeUpdated.beforeNode);
                varDeclAfter.getVariables().set(indexAfter, (VariableDeclarator) afterUpdated.beforeNode);
            }
            else{
                throw new RuntimeException("Updating field or variable only supports FieldDeclaration or VariableDeclarationExpr.");
            }
        }
        else if (beforeUpdated.node instanceof Statement || beforeUpdated.node instanceof Expression){
            if (beforeUpdated.parent instanceof BlockStmt){
                // Update normal statement
                BlockStmt blockBefore=(BlockStmt)beforeUpdated.parent;
                BlockStmt blockAfter=(BlockStmt)afterUpdated.parent;
                int indexBefore=beforeUpdated.index;
                int indexAfter=afterUpdated.index;
                assert indexBefore==indexAfter;
                if (blockBefore.getStatements().size()==blockAfter.getStatements().size())
                    blockAfter.getStatements().set(indexAfter, (Statement)afterUpdated.node);
                else
                    blockAfter.getStatements().set(indexAfter+1, (Statement)afterUpdated.node);
            }
            else if (beforeUpdated.parent instanceof IfStmt) {
                ModifiedStmt beforeModified=(ModifiedStmt)beforeUpdated;
                ModifiedStmt afterModified=(ModifiedStmt)afterUpdated;

                IfStmt ifStmt=(IfStmt)afterModified.parent;
                if (beforeModified.nodeGetter.getName().equals("getThenStmt")) {
                    BlockStmt body=(BlockStmt)ifStmt.getThenStmt();
                    body.getStatements().set(1, (Statement) afterModified.node);
                }
                else if (beforeModified.nodeGetter.getName().equals("getElseStmt")) {
                    BlockStmt body=(BlockStmt)ifStmt.getElseStmt().get();
                    body.getStatements().set(1, (Statement) afterModified.node);
                }
                else if (beforeModified.nodeGetter.getName().equals("getCondition")) {
                    ifStmt.setCondition((Expression) afterModified.node);
                }
                else {
                    throw new RuntimeException("Updating if stmt only supports condition/then/else.");
                }
            }
            else if (beforeUpdated.parent instanceof WhileStmt) {
                ModifiedStmt beforeModified=(ModifiedStmt)beforeUpdated;
                ModifiedStmt afterModified=(ModifiedStmt)afterUpdated;

                WhileStmt whileStmt=(WhileStmt)afterModified.parent;
                if (beforeModified.nodeGetter.getName().equals("getBody")) {
                    BlockStmt body=(BlockStmt)whileStmt.getBody();
                    body.getStatements().set(1, (Statement) afterModified.node);
                }
                else if (beforeModified.nodeGetter.getName().equals("getCondition")) {
                    whileStmt.setCondition((Expression) afterModified.node);
                }
                else {
                    throw new RuntimeException("Updating while stmt only supports condition/then.");
                }
            }
            else if (beforeUpdated.parent instanceof ForStmt) {
                ModifiedStmt beforeModified=(ModifiedStmt)beforeUpdated;
                ModifiedStmt afterModified=(ModifiedStmt)afterUpdated;

                ForStmt forStmt=(ForStmt)afterModified.parent;
                if (beforeModified.nodeGetter.getName().equals("getBody")) {
                    BlockStmt body=(BlockStmt)forStmt.getBody();
                    body.getStatements().set(1, (Statement) afterModified.node);
                }
                else if (beforeModified.nodeGetter.getName().equals("getCompare")) {
                    forStmt.setCompare((Expression) afterModified.node);
                }
                else if (beforeModified.nodeGetter.getName().equals("getInitialization")) {
                    NodeList<Expression> initList=forStmt.getInitialization();
                    initList.set(afterModified.index, (Expression) afterModified.node);
                    forStmt.setInitialization(initList);
                }
                else if (beforeModified.nodeGetter.getName().equals("getUpdate")) {
                    NodeList<Expression> updateList=forStmt.getUpdate();
                    updateList.set(afterModified.index, (Expression) afterModified.node);
                    forStmt.setUpdate(updateList);
                }
                else {
                    throw new RuntimeException("Updating for stmt only supports init/compare/update/body.");
                }
            }
            else if (beforeUpdated.parent instanceof DoStmt) {
                ModifiedStmt beforeModified=(ModifiedStmt)beforeUpdated;
                ModifiedStmt afterModified=(ModifiedStmt)afterUpdated;

                DoStmt whileStmt=(DoStmt)afterModified.parent;
                if (beforeModified.nodeGetter.getName().equals("getBody")) {
                    BlockStmt body=(BlockStmt)whileStmt.getBody();
                    body.getStatements().set(1, (Statement) afterModified.node);
                }
                else if (beforeModified.nodeGetter.getName().equals("getCondition")) {
                    whileStmt.setCondition((Expression) afterModified.node);
                }
                else {
                    throw new RuntimeException("Updating do-while stmt only supports condition/then.");
                }
            }
            else if (beforeUpdated.parent instanceof ForEachStmt) {
                ModifiedStmt beforeModified=(ModifiedStmt)beforeUpdated;
                ModifiedStmt afterModified=(ModifiedStmt)afterUpdated;

                ForEachStmt whileStmt=(ForEachStmt)afterModified.parent;
                if (beforeModified.nodeGetter.getName().equals("getBody")) {
                    BlockStmt body=(BlockStmt)whileStmt.getBody();
                    body.getStatements().set(1, (Statement) afterModified.node);
                }
                else {
                    throw new RuntimeException("Updating for-each stmt only supports body.");
                }
            }
            else if (beforeUpdated.parent instanceof SwitchEntry) {
                ModifiedStmt beforeModified=(ModifiedStmt)beforeUpdated;
                ModifiedStmt afterModified=(ModifiedStmt)afterUpdated;

                SwitchEntry switchEntry=(SwitchEntry)afterModified.parent;
                if (beforeModified.nodeGetter.getName().equals("getStatements")) {
                    NodeList<Statement> stmtList=switchEntry.getStatements();
                    stmtList.set(afterModified.index+1, (Statement) afterModified.node);
                    switchEntry.setStatements(stmtList);
                }
                else {
                    throw new RuntimeException("Updating switch entry only supports body.");
                }
            }
            else {
                throw new RuntimeException("Updating node only supports statements: "+beforeUpdated.parent.getClass().getName());
            }
        }
    }


    /**
     * Generate a wrapper method call expression for the given condition.
     * @param condition condition to be wrapped
     * @param parentNode parent node of the condition
     * @return wrapper method call expression for the given condition.
     * @see GlobalStates#wrapConditionExpr
     */
    private MethodCallExpr genWrapper(long id) {
        NameExpr classAccess=new NameExpr(GlobalStates.STATE_CLASS_NAME);
        String methodName=GlobalStates.STATE_BRANCH_METHOD;
        List<Expression> args=Arrays.asList(new LongLiteralExpr(Long.toString(id)));

        MethodCallExpr wrapperCaller= new MethodCallExpr(classAccess, methodName, new NodeList<>(args));
        return wrapperCaller;
    }

    /**
     * Instrument the given statement.
     * @param stmt {@link IfStmt}, {@link ForStmt}, {@link WhileStmt} or {@link DoStmt}.
     */
    private void instrumentCondition(Statement stmt,List<Long> ids) {
        if (stmt instanceof IfStmt) {
            // if statement
            IfStmt ifStmt=(IfStmt)stmt;
            
            // Then branch
            Statement thenStmt=ifStmt.getThenStmt();
            if (thenStmt instanceof BlockStmt){
                BlockStmt thenBlock=(BlockStmt)thenStmt;
                thenBlock.getStatements().addFirst(new ExpressionStmt(genWrapper(ids.get(0))));
            }
            else {
                NodeList<Statement> thenStmts=new NodeList<>();
                thenStmts.add(new ExpressionStmt(genWrapper(ids.get(0))));
                thenStmts.add(thenStmt);
                BlockStmt thenBlock=new BlockStmt(thenStmts);
                ifStmt.setThenStmt(thenBlock);
            }

            // Else branch
            if (ifStmt.hasElseBranch()){
                Statement elseStmt=ifStmt.getElseStmt().get();
                if (elseStmt instanceof BlockStmt){
                    BlockStmt elseBlock=(BlockStmt)elseStmt;
                    elseBlock.getStatements().addFirst(new ExpressionStmt(genWrapper(ids.get(1))));
                }
                else {
                    NodeList<Statement> elseStmts=new NodeList<>();
                    elseStmts.add(new ExpressionStmt(genWrapper(ids.get(1))));
                    elseStmts.add(elseStmt);
                    BlockStmt elseBlock=new BlockStmt(elseStmts);
                    ifStmt.setElseStmt(elseBlock);
                }
            }
        }
        else if (stmt instanceof ForStmt) {
            // for statement
            ForStmt forStmt=(ForStmt)stmt;

            // Instrument
            Statement body=forStmt.getBody();
            if (body instanceof BlockStmt){
                BlockStmt bodyBlock=(BlockStmt)body;
                bodyBlock.getStatements().addFirst(new ExpressionStmt(genWrapper(ids.get(0))));
            }
            else {
                NodeList<Statement> bodyStmts=new NodeList<>();
                bodyStmts.add(new ExpressionStmt(genWrapper(ids.get(0))));
                bodyStmts.add(body);
                BlockStmt bodyBlock=new BlockStmt(bodyStmts);
                forStmt.setBody(bodyBlock);
            }
        }
        else if (stmt instanceof WhileStmt) {
            // while statement
            WhileStmt whileStmt=(WhileStmt)stmt;

            // Instrument
            Statement body=whileStmt.getBody();
            if (body instanceof BlockStmt){
                BlockStmt bodyBlock=(BlockStmt)body;
                bodyBlock.getStatements().addFirst(new ExpressionStmt(genWrapper(ids.get(0))));
            }
            else {
                NodeList<Statement> bodyStmts=new NodeList<>();
                bodyStmts.add(new ExpressionStmt(genWrapper(ids.get(0))));
                bodyStmts.add(body);
                BlockStmt bodyBlock=new BlockStmt(bodyStmts);
                whileStmt.setBody(bodyBlock);
            }
        }
        else if (stmt instanceof DoStmt) {
            // do-while statement
            DoStmt doStmt=(DoStmt)stmt;

            // Instrument
            Statement body=doStmt.getBody();
            if (body instanceof BlockStmt){
                BlockStmt bodyBlock=(BlockStmt)body;
                bodyBlock.getStatements().addFirst(new ExpressionStmt(genWrapper(ids.get(0))));
            }
            else {
                NodeList<Statement> bodyStmts=new NodeList<>();
                bodyStmts.add(new ExpressionStmt(genWrapper(ids.get(0))));
                bodyStmts.add(body);
                BlockStmt bodyBlock=new BlockStmt(bodyStmts);
                doStmt.setBody(bodyBlock);
            }
        }
        else if (stmt instanceof ForEachStmt) {
            ForEachStmt forEachStmt=(ForEachStmt)stmt;

            // Instrument
            Statement body=forEachStmt.getBody();
            if (body instanceof BlockStmt){
                BlockStmt bodyBlock=(BlockStmt)body;
                bodyBlock.getStatements().addFirst(new ExpressionStmt(genWrapper(ids.get(0))));
            }
            else {
                NodeList<Statement> bodyStmts=new NodeList<>();
                bodyStmts.add(new ExpressionStmt(genWrapper(ids.get(0))));
                bodyStmts.add(body);
                BlockStmt bodyBlock=new BlockStmt(bodyStmts);
                forEachStmt.setBody(bodyBlock);
            }
        }
    }

    /**
     * Instrument the given {@link SwitchEntry}.
     * @param switchCase {@link SwitchEntry} to be instrumented.
     */
    private void instrumentSwitchCase(SwitchEntry switchCase, List<Long> ids) {
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
}
