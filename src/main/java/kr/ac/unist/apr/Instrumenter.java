package kr.ac.unist.apr;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;

import kr.ac.unist.apr.utils.InsnNodeUtils;
import kr.ac.unist.apr.utils.Path;
import kr.ac.unist.apr.visitor.MethodInstrumenter;

/**
 * Main class of instrumentation.
 * <p>
 *  This class uses ASM to instrument target program.
 *  Branch IDs are deterministic and unique.
 *  It is guaranteed that the branch IDs are always same.
 * 
 *  This instrumenter ignores patched bytecodes.
 * </p>
 * @author Youngjae Kim
 */
public class Instrumenter {
    private String targetPath;

    private Map<String,ClassReader> targetNodes=new HashMap<>();
    private Map<String,ClassReader> originalNodes=new HashMap<>();
    public static Map<Integer,String> hashStrings=new HashMap<>();

    public static final int MAX_PREV_INSNS=10;
    private static int prevId=0;

    /**
     * Default constructor.
     * <p>
     *  This constructor prepares ASM to instrument.
     * </p>
     * @param targetSourcePath class path of target program
     * @param originalSourcePath class path of original source
     * @throws IOException if file not found or I/O errors
     */
    public Instrumenter(String targetSourcePath,
                    String originalSourcePath) throws IOException {
        this.targetPath=targetSourcePath;

        // generate ClassReader for original source
        Main.LOGGER.log(Level.INFO, "Parse Instructions for original source...");
        List<String> allOriginalSources=Path.getAllSources(new File(originalSourcePath));
        for (String source:allOriginalSources){
            if (source.contains("kr/ac/unist/apr")) continue;

            ClassReader reader=new ClassReader(new FileInputStream(source));
            originalNodes.put(Path.removeSrcPath(source, originalSourcePath),reader);
        }

        Main.LOGGER.log(Level.INFO, "Parse Instructions for target source...");
        // generate ClassWriter for patched source
        List<String> allSources=Path.getAllSources(new File(targetSourcePath));
        for (String source:allSources){
            if (source.contains("kr/ac/unist/apr")) continue;

            ClassReader reader=new ClassReader(new FileInputStream(source));
            targetNodes.put(Path.removeSrcPath(source, targetSourcePath),reader);
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
     * @throws FileNotFoundException
     */
    public void instrument() throws IOException{
        // Visit original source visitor and get IDs
        // TODO: Cache/load this result with file
        Main.LOGGER.log(Level.INFO, "Instrument class file...");
        for (Map.Entry<String,ClassReader> originalCtxt:originalNodes.entrySet()){
            ClassNode classNode=new ClassNode();
            originalCtxt.getValue().accept(classNode, 0);

            Map<MethodNode,Map<Integer,Integer>> methodIds=new HashMap<>();
            for (MethodNode methodInfo:classNode.methods){
                methodIds.put(methodInfo,
                        computeBranchIds(methodInfo.instructions, originalCtxt.getKey(), methodInfo.name, methodInfo.desc));
            }

            ClassReader targetReader=targetNodes.get(originalCtxt.getKey());
            ClassNode node=new ClassNode();
            targetReader.accept(node,0);

            // Instrument every methods
            for (MethodNode methodInfo:node.methods) {
                // Instrument every labels
                MethodNode sourceMethod=InsnNodeUtils.findSameMethod(methodInfo, methodIds.keySet());
                if (sourceMethod!=null){
                    MethodInstrumenter instrumenter=new MethodInstrumenter(Opcodes.ASM9,originalCtxt.getKey(), methodInfo.access,
                                            methodInfo.name, methodInfo.desc, methodInfo.signature,
                                            methodInfo.exceptions.toArray(new String[0]),
                                            methodIds.get(sourceMethod));
                    methodInfo.accept(instrumenter);

                    Map<LabelNode,InsnList> newInsns=instrumenter.getNewInsns();
                    for (Map.Entry<LabelNode,InsnList> entry:newInsns.entrySet()){
                        int index=methodInfo.instructions.indexOf(entry.getKey());
                        methodInfo.instructions.insert(methodInfo.instructions.get(index+1), entry.getValue());
                    }
                    methodInfo.check(Opcodes.ASM9);
                }
            }
            
            // Save instrumented file
            ClassWriter writer2=new ClassWriter(ClassWriter.COMPUTE_MAXS);
            classNode.accept(writer2);
            
            node.check(Opcodes.ASM9);
            ClassWriter writer=new ClassWriter(ClassWriter.COMPUTE_MAXS);
            node.accept(writer);

            System.out.println("Orig: "+writer2.toByteArray().length+", Patched: "+writer.toByteArray().length);
            byte[] newClass=writer.toByteArray();
            if (targetPath.endsWith(".class")){
                FileOutputStream fos=new FileOutputStream(targetPath);
                fos.write(newClass);
                fos.close();
            }
            else{
                FileOutputStream fos=new FileOutputStream(targetPath+"/"+originalCtxt.getKey());
                fos.write(newClass);
                fos.close();
            }
        }
    }

    /**
     * Compute branch IDs.
     * @param instructions instructions of method
     * @param className class name
     * @param methodName method name
     * @param methodDesc method descriptor
     */
    public Map<Integer,Integer> computeBranchIds(InsnList instructions, String className, String methodName, String methodDesc) {
        Map<Integer,Integer> ids=new HashMap<>();

        for (int i=0;i<instructions.size();i++) {
            AbstractInsnNode insn=instructions.get(i);
            
            // Compute branch ID
            if (insn.getType()==AbstractInsnNode.JUMP_INSN) {
                JumpInsnNode jumpInsn=(JumpInsnNode)insn;
                LabelNode curLabel=jumpInsn.label;
                int labelIndex=instructions.indexOf(curLabel);

                Deque<AbstractInsnNode> prevInsns=new ArrayDeque<>();
                for (int j=labelIndex-1;j>=0 && j>=Instrumenter.MAX_PREV_INSNS;j--) {
                    prevInsns.add(instructions.get(j));
                }

                String hashSource=className+"::"+methodName+"::"+methodDesc+"::";
                for (AbstractInsnNode prevInsn:prevInsns) {
                    String nodeString=InsnNodeUtils.convertNodeToString(prevInsn);
                    if (nodeString.length()>0)
                        hashSource+=nodeString+";";
                }
                int hashed=hashSource.hashCode();
                hashStrings.put(hashed, hashSource);

                if (ids.containsKey(hashed))
                    Main.LOGGER.warning("Duplicated ID: "+hashed);
                ids.put(hashed,prevId++);
            }
        }

        return ids;
    }

}
