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
import java.util.logging.Level;

import com.github.gumtreediff.gen.javaparser.JavaParserGenerator;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.utils.Pair;

import kr.ac.unist.apr.gumtree.MyRootsClassifier;
import kr.ac.unist.apr.utils.Path;
import kr.ac.unist.apr.visitor.OriginalSourceVisitor;
import kr.ac.unist.apr.visitor.PatchedSourceVisitor;
import kr.ac.unist.apr.visitor.TargetSourceVisitor;

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
     * @throws IOException if file not found or I/O errors
     */
    public Instrumenter(String originalFilePath,
                    String patchedFilePath,
                    String targetSourcePath,
                    String originalSourcePath,
                    String[] classPaths) throws IOException {
        this.originalFilePath=originalFilePath;
        this.patchedFilePath=patchedFilePath;

        Main.LOGGER.log(Level.INFO, "Generate AST for original source...");
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

        Main.LOGGER.log(Level.INFO, "Generate AST for target source...");
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
        Main.LOGGER.log(Level.INFO, "Gen IDs from original ASTs...");
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
        Main.LOGGER.log(Level.INFO, "Instrument target program except patched file...");
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
        if (dstAddSet.size()!=0 && srcMvSet.size()!=0){
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

        Main.LOGGER.log(Level.INFO, "Instrument patched file...");
        PatchedSourceVisitor patchedSourceVisitor=new PatchedSourceVisitor(originalNodeToId.get(originalFilePath));
        patchedSourceVisitor.visitPreOrder(patchedRootNode.getRoot().getJParserNode());
        
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
        writer.write(patchedRootNode.getRoot().getJParserNode().toString());
        writer.close();
    }

    static class ModifiedNode {
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

    private List<Node> convertNodes(Set<ITree> trees){
        List<Node> result=new ArrayList<>();
        for (ITree tree:trees){
            result.add(tree.getJParserNode());
        }

        return result;
    }

    protected Pair<ModifiedNode,ModifiedNode> revertMove(Node movedFromOriginal,Node movedToPatch) {
        if (movedFromOriginal.getParentNode().get() instanceof BlockStmt &&
                        movedToPatch.getParentNode().get() instanceof BlockStmt){
            // A node is moved from a block to another block.
            BlockStmt blockFrom=(BlockStmt)movedFromOriginal.getParentNode().get();
            BlockStmt blockTo=(BlockStmt)movedToPatch.getParentNode().get();

            int indexFrom=blockFrom.getStatements().indexOf(movedFromOriginal);
            int indexTo=blockTo.getStatements().indexOf(movedToPatch);

            blockFrom.getStatements().remove(movedFromOriginal);
            blockTo.getStatements().remove(movedToPatch);

            ModifiedNode beforeNode,afterNode;
            if (indexFrom==0){
                beforeNode=new ModifiedNode(movedFromOriginal,indexFrom,blockFrom,null);
            }
            else{
                beforeNode=new ModifiedNode(movedFromOriginal,indexFrom,blockFrom,blockFrom.getStatements().get(indexFrom-1));
            }

            if (indexTo==0){
                afterNode=new ModifiedNode(movedToPatch,indexTo,blockTo,null);
            }
            else{
                afterNode=new ModifiedNode(movedToPatch,indexTo,blockTo,blockTo.getStatements().get(indexTo-1));
            }

            return new Pair<Instrumenter.ModifiedNode,Instrumenter.ModifiedNode>(afterNode, beforeNode);
        }
        else{
            throw new RuntimeException("RevertMoveVisitor can only handle statement that moved in BlockStmt.");
        }
    }

    protected ModifiedNode revertRemoval(Node removedNode) {
        if (removedNode instanceof MethodDeclaration){
            // Removal method declaration
            if (!(removedNode.getParentNode().get() instanceof TypeDeclaration)){
                throw new RuntimeException("RevertRemoveVisitor can only handle MethodDecl that removed in Class/Interface Dec.");
            }

            TypeDeclaration typeDecl=(TypeDeclaration)removedNode.getParentNode().get();
            int index=typeDecl.getMembers().indexOf(removedNode);
            typeDecl.getMembers().remove(removedNode);
            ModifiedNode modifiedNode;
            if (index==0){
                modifiedNode=new ModifiedNode(removedNode,index,typeDecl,null);
            }
            else{
                modifiedNode=new ModifiedNode(removedNode,index,typeDecl,typeDecl.getMembers().get(index-1));
            }

            return modifiedNode;
        }
        else{
            // Removal statement
            if (!(removedNode instanceof Statement)){
                throw new RuntimeException("Inserted node should be Statement.");
            }
            if (!(removedNode.getParentNode().get() instanceof BlockStmt)) {
                throw new RuntimeException("RevertInsertionVisitor can only handle statement that inserted in BlockStmt.");
            }

            BlockStmt block=(BlockStmt)removedNode.getParentNode().get();
            int index=block.getStatements().indexOf(removedNode);
            block.getStatements().remove(removedNode);
            ModifiedNode modifiedNode;
            if (index==0){
                modifiedNode=new ModifiedNode(removedNode,index,block,null);
            }
            else{
                modifiedNode=new ModifiedNode(removedNode,index,block,block.getStatements().get(index-1));
            }
            return modifiedNode;
        }
    }

    protected ModifiedNode revertInsertion(Node insertedNode) {
        if (!(insertedNode instanceof Statement)){
            throw new RuntimeException("Inserted node should be Statement.");
        }
        if (!(insertedNode.getParentNode().get() instanceof BlockStmt)) {
            throw new RuntimeException("RevertInsertionVisitor can only handle statement that inserted in BlockStmt.");
        }

        BlockStmt block=(BlockStmt)insertedNode.getParentNode().get();
        int index=block.getStatements().indexOf(insertedNode);
        block.getStatements().remove(insertedNode);
        ModifiedNode modifiedNode;
        if (index==0){
            modifiedNode=new ModifiedNode(insertedNode,index,block,null);
        }
        else{
            modifiedNode=new ModifiedNode(insertedNode,index,block,block.getStatements().get(index-1));
        }
        return modifiedNode;
    }

    protected Pair<ModifiedNode,ModifiedNode> revertUpdate(Node beforeNode,Node afteNode) {
        if (beforeNode instanceof VariableDeclarator){
            // Update field or variable declaration
            if (!(beforeNode.getParentNode().get() instanceof FieldDeclaration || beforeNode.getParentNode().get() instanceof VariableDeclarationExpr) ||
                            !(afteNode.getParentNode().get() instanceof TypeDeclaration || afteNode.getParentNode().get() instanceof VariableDeclarationExpr)){
                throw new RuntimeException("Can only handle update field or variable declaration.");
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
                throw new RuntimeException("RevertUpdateVisitor can only handle VariableDeclarator that updated in FieldDecl or VarDecl.");
            }
        }
        else if (beforeNode instanceof Statement){
            // Update normal statement
            Node curBefore=beforeNode.getParentNode().get();
            Node curAfter=afteNode.getParentNode().get();
            while (!(curBefore.getParentNode().get() instanceof BlockStmt)){
                curBefore=curBefore.getParentNode().get();
                curAfter=curAfter.getParentNode().get();
            }
            assert curAfter.getParentNode().get() instanceof BlockStmt;

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
        else {
            throw new RuntimeException("RevertUpdateVisitor can only handle VariableDeclarator or Statement.");
        }
    }

    protected void rollbackMove(Pair<ModifiedNode,ModifiedNode> movedInfos) {
        // A node is moved from a block to another block.
        ModifiedNode beforeMoved=movedInfos.a;
        ModifiedNode afterMoved=movedInfos.b;
        BlockStmt blockFrom=(BlockStmt)beforeMoved.parent;
        BlockStmt blockTo=(BlockStmt)afterMoved.parent;

        if (beforeMoved.index==0){
            blockFrom.getStatements().addFirst((Statement)beforeMoved.node);
        }
        else{
            blockFrom.getStatements().addAfter((Statement)beforeMoved.node, (Statement)beforeMoved.beforeNode);
        }
        
        if (afterMoved.index==0)
            blockTo.getStatements().addFirst((Statement)afterMoved.node);
        else
            blockTo.getStatements().addAfter((Statement)afterMoved.node, (Statement)afterMoved.beforeNode);
    }

    protected void rollbackRemoval(ModifiedNode removedInfo) {
        if (removedInfo.node instanceof MethodDeclaration){
            // Removal method declaration
            if (!(removedInfo.parent instanceof TypeDeclaration)){
                throw new RuntimeException("RevertRemoveVisitor can only handle MethodDecl that removed in Class/Interface Dec.");
            }

            TypeDeclaration typeDecl=(TypeDeclaration)removedInfo.parent;
            if (removedInfo.index==0)
                typeDecl.getMembers().addFirst(removedInfo.node);
            else
                typeDecl.getMembers().addAfter(removedInfo.node, removedInfo.beforeNode);
        }
        else{
            // Removal statement
            if (!(removedInfo.node instanceof Statement)){
                throw new RuntimeException("Inserted node should be Statement.");
            }
            if (!(removedInfo.parent instanceof BlockStmt)) {
                throw new RuntimeException("RevertInsertionVisitor can only handle statement that inserted in BlockStmt.");
            }

            BlockStmt block=(BlockStmt)removedInfo.parent;
            int index=removedInfo.index;
            
            if (index==0)
                block.getStatements().addFirst((Statement)removedInfo.node);
            else
                block.getStatements().addAfter((Statement)removedInfo.node, (Statement)removedInfo.beforeNode);
        }
    }

    protected void rollbackInsertion(ModifiedNode insertedInfo) {
        if (!(insertedInfo.node instanceof Statement)){
            throw new RuntimeException("Inserted node should be Statement.");
        }
        if (!(insertedInfo.parent instanceof BlockStmt)) {
            throw new RuntimeException("RevertInsertionVisitor can only handle statement that inserted in BlockStmt.");
        }

        BlockStmt block=(BlockStmt)insertedInfo.parent;
        int index=insertedInfo.index;
        
        if (index==0)
            block.getStatements().addFirst((Statement) insertedInfo.node);
        else
            block.getStatements().addAfter((Statement) insertedInfo.node, (Statement) insertedInfo.beforeNode);
    }

    protected void rollbackUpdate(Pair<ModifiedNode,ModifiedNode> updatedInfos) {
        ModifiedNode beforeUpdated=updatedInfos.a;
        ModifiedNode afterUpdated=updatedInfos.b;
        if (beforeUpdated.node instanceof VariableDeclarator){
            // Update field or variable declaration
            if (!(beforeUpdated.parent instanceof FieldDeclaration || beforeUpdated.parent instanceof VariableDeclarationExpr) ||
                            !(afterUpdated.parent instanceof TypeDeclaration || afterUpdated.parent instanceof VariableDeclarationExpr)){
                throw new RuntimeException("Can only handle update field or variable declaration.");
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
                throw new RuntimeException("RevertUpdateVisitor can only handle VariableDeclarator that updated in FieldDecl or VarDecl.");
            }
        }
        else if (beforeUpdated.beforeNode instanceof Statement){
            // Update normal statement
            BlockStmt blockAfter=(BlockStmt)afterUpdated.parent;
            int indexBefore=beforeUpdated.index;
            int indexAfter=afterUpdated.index;
            assert indexBefore==indexAfter;
            blockAfter.getStatements().set(indexAfter, (Statement)afterUpdated.beforeNode);
        }
        else {
            throw new RuntimeException("RevertUpdateVisitor can only handle VariableDeclarator or Statement.");
        }
    }


}
