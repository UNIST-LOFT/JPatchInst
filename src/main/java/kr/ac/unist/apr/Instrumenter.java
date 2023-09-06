package kr.ac.unist.apr;

import java.io.File;
import java.io.FileInputStream;
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
    private Map<String,ClassReader> targetNodes=new HashMap<>();
    private Map<String,ClassReader> originalNodes=new HashMap<>();
    private Map<Integer,String> hashStrings=new HashMap<>();

    public static final int MAX_PREV_INSNS=10;
    private static int prevId=0;
    public static Map<Integer,Integer> ids=new HashMap<>();

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
     */
    public void instrument(){
        // Visit original source visitor and get IDs
        // TODO: Cache/load this result with file
        Main.LOGGER.log(Level.INFO, "Instrument class file...");
        for (Map.Entry<String,ClassReader> originalCtxt:originalNodes.entrySet()){
            ClassNode classNode=new ClassNode();
            originalCtxt.getValue().accept(classNode, 0);

            for (MethodNode methodInfo:classNode.methods){
                computeBranchIds(methodInfo.instructions, originalCtxt.getKey(), methodInfo.name, methodInfo.desc);
            }

            ClassReader targetReader=targetNodes.get(originalCtxt.getKey());
            ClassWriter writer=new ClassWriter(targetReader,0);
            ClassNode node=new ClassNode();
            targetReader.accept(node,0);
            node.accept(writer);

            // Instrument every methods
            for (MethodNode methodInfo:node.methods) {
                // Instrument every labels
                MethodInstrumenter instrumenter=new MethodInstrumenter(Opcodes.ASM9,originalCtxt.getKey(), methodInfo.access,
                                        methodInfo.name, methodInfo.desc, methodInfo.signature,
                                        methodInfo.exceptions.toArray(new String[0]));
                methodInfo.accept(instrumenter);
                methodInfo.check(Opcodes.ASM9);
            }
            
            // Save instrumented file
            // FileWriter writer = new FileWriter(targetSourcePath+"/"+targetCtxt.getKey());
            // writer.write(targetNode.toString());
            // writer.close();

        }
    }

    /**
     * Compute branch IDs.
     * @param instructions instructions of method
     * @param className class name
     * @param methodName method name
     * @param methodDesc method descriptor
     */
    public void computeBranchIds(InsnList instructions, String className, String methodName, String methodDesc) {
        Deque<AbstractInsnNode> prevInsns=new ArrayDeque<>();

        for (int i=0;i<instructions.size();i++) {
            AbstractInsnNode insn=instructions.get(i);
            
            // Compute branch ID
            if (insn.getType()==AbstractInsnNode.JUMP_INSN) {
                String hashSource=className+"::"+methodName+"::"+methodDesc+"::";
                for (AbstractInsnNode prevInsn:prevInsns) {
                    hashSource+=InsnNodeUtils.convertNodeToString(prevInsn)+";";
                }
                int hashed=hashSource.hashCode();
                hashStrings.put(hashed, hashSource);

                if (ids.containsKey(hashed))
                    Main.LOGGER.warning("Duplicated ID: "+hashed);
                ids.put(hashed,prevId++);
            }

            // Update queue
            prevInsns.add(insn);
            if (prevInsns.size()>MAX_PREV_INSNS)
                prevInsns.remove();
        }
    }

}
