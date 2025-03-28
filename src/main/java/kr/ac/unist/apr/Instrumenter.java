package kr.ac.unist.apr;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;
import java.util.logging.Level;

import kr.ac.unist.apr.asm.Instruction;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import kr.ac.unist.apr.utils.InsnNodeUtils;
import kr.ac.unist.apr.utils.Path;
import kr.ac.unist.apr.asm.InstrumentClassWriter;
import kr.ac.unist.apr.asm.MethodInstrumenter;

/**
 * Main class of instrumentation.
 * <p>
 * This class uses ASM to instrument target program.
 * Branch IDs are deterministic and unique.
 * It is guaranteed that the branch IDs are always same.
 * <p>
 * This instrumenter ignores patched bytecodes.
 * </p>
 *
 * @author Youngjae Kim
 */
public class Instrumenter {
    private String targetPath;

    private Map<String, ClassReader> targetNodes = new HashMap<>();
    private Map<String, ClassReader> originalNodes = new HashMap<>();
    public static Map<Integer, String> hashStrings = new HashMap<>();

    public static final int MAX_PREV_INSNS = 10;
    private static int prevId = 0;
    private List<Integer> branchIds;

    private InsnList getFieldChangeInstructions(String className, boolean isStatic) {
        InsnList instructions = new InsnList();
        instructions.add(isStatic ? new InsnNode(Opcodes.ACONST_NULL) : new VarInsnNode(Opcodes.ALOAD, 0));
        instructions.add(new LdcInsnNode(className.replace(File.separatorChar, '.')));
        instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, GlobalStates.STATE_CLASS_NAME.replace('.', '/'),
                GlobalStates.STATE_FIELD_LOG_METHOD_NAME, "(Ljava/lang/Object;Ljava/lang/String;)V", false));

        return instructions;
    }

    /**
     * Default constructor.
     * <p>
     * This constructor prepares ASM to instrument.
     * </p>
     *
     * @param targetSourcePath   class path of target program
     * @param originalSourcePath class path of original source
     * @throws IOException if file not found or I/O errors
     */
    public Instrumenter(String targetSourcePath,
            String originalSourcePath, List<Integer> branchIds) throws IOException {
        this.targetPath = targetSourcePath;
        this.branchIds = branchIds;

        // generate ClassReader for original source
        Main.LOGGER.log(Level.INFO, "Parse Instructions for original source...");
        List<String> allOriginalSources = Path.getAllSources(new File(originalSourcePath));
        for (String source : allOriginalSources) {
            if (source.contains("kr/ac/unist/apr"))
                continue;

            ClassReader reader = new ClassReader(new FileInputStream(source));
            originalNodes.put(Path.removeSrcPath(source, originalSourcePath), reader);
        }

        Main.LOGGER.log(Level.INFO, "Parse Instructions for target source...");
        // generate ClassWriter for patched source
        List<String> allSources = Path.getAllSources(new File(targetSourcePath));
        for (String source : allSources) {
            if (source.contains("kr/ac/unist/apr"))
                continue;

            ClassReader reader = new ClassReader(new FileInputStream(source));
            targetNodes.put(Path.removeSrcPath(source, targetSourcePath), reader);
        }
    }

    public Instrumenter(String targetSourcePath, String originalSourcePath) throws IOException {
        this(targetSourcePath, originalSourcePath, new ArrayList<>());
    }

    /**
     * Instrument target program with handling patch.
     * <p>
     * This method instruments branches with unique IDs.
     * It also overwrites target sources.
     * Backup the original target sources before using this method.
     * <p>
     * This method guarantees that the branch IDs are always same.
     * </p>
     *
     * @throws FileNotFoundException
     */
    public void instrument(String timeFileOutput) throws IOException {
        // Visit original source visitor and get IDs
        Main.LOGGER.log(Level.INFO, "Instrument class file...");
        Map<String, Double> timeMap = new HashMap<>();
        for (Map.Entry<String, ClassReader> originalCtxt : originalNodes.entrySet()) {
            // Target class file
            ClassReader targetReader = targetNodes.get(originalCtxt.getKey());
            ClassNode node = new ClassNode();
            if (targetReader == null) {
                // Target class file not exist if patch removes whole 'public class'
                Main.LOGGER.info("Class file " + originalCtxt.getKey() + " not found in target. Skip it.");
                continue;
            }
            targetReader.accept(node, 0);

            // Skip if already instrumented
            boolean skip = false;
            for (FieldNode field : node.fields) {
                if (field.name.equals("greyboxInstrumented")) {
                    Main.LOGGER.log(Level.FINE, "Skip instrumenting " + originalCtxt.getKey());
                    skip = true;
                    break;
                }
            }
            if (skip)
                continue;

            long start = Calendar.getInstance().getTimeInMillis();
            // Source class file
            ClassNode classNode = new ClassNode();
            originalCtxt.getValue().accept(classNode, 0);

            Map<MethodNode, Map<Integer, Integer>> methodIds = new HashMap<>();
            for (MethodNode methodInfo : classNode.methods) {
                methodIds.put(methodInfo,
                        computeBranchIds(methodInfo.instructions, originalCtxt.getKey(), methodInfo.name,
                                methodInfo.desc));
            }

            // Instrument every methods
            for (MethodNode methodInfo : node.methods) {
                // Instrument every labels
                MethodNode sourceMethod = InsnNodeUtils.findSameMethod(methodInfo, methodIds.keySet());
                if (sourceMethod != null) {
                    MethodInstrumenter instrumenter = new MethodInstrumenter(Opcodes.ASM9, originalCtxt.getKey(),
                            methodInfo.access,
                            methodInfo.name, methodInfo.desc, methodInfo.signature,
                            methodInfo.exceptions.toArray(new String[0]),
                            methodIds.get(sourceMethod));
                    methodInfo.accept(instrumenter);

                    Map<LabelNode, InsnList> newInsns = instrumenter.getNewInsns();
                    for (Map.Entry<LabelNode, InsnList> entry : newInsns.entrySet()) {
                        int index = methodInfo.instructions.indexOf(entry.getKey());
                        if (index + 1 < methodInfo.instructions.size()) // Check label is method end
                            methodInfo.instructions.insert(methodInfo.instructions.get(index + 1), entry.getValue());
                    }

                    // skip logging field change if the class is an interface or the method is an
                    // abstract method
                    if ((node.access & Opcodes.ACC_INTERFACE) == 0 && (methodInfo.access & Opcodes.ACC_ABSTRACT) == 0) {
                        // add initialize instructions on method enter
                        methodInfo.instructions.insert(Instruction.getInitInstructions());

                        // Log field changes on method return/throw
                        if (!methodInfo.name.equals("<init>")) {
                            ListIterator<AbstractInsnNode> iterator = methodInfo.instructions.iterator();
                            while (iterator.hasNext()) {
                                AbstractInsnNode insn = iterator.next();
                                int opcode = insn.getOpcode();
                                if ((opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN)
                                        || opcode == Opcodes.ATHROW) {
                                    InsnList instructions = getFieldChangeInstructions(originalCtxt.getKey(),
                                            (methodInfo.access & Opcodes.ACC_STATIC) != 0);

                                    methodInfo.instructions.insertBefore(insn, instructions);
                                    totalInstrumented++;
                                }
                            }
                        }

                        // log field changes on method exit
                        if (!methodInfo.name.equals("<init>")) {
                            InsnList instructions = getFieldChangeInstructions(originalCtxt.getKey(),
                                    (methodInfo.access & Opcodes.ACC_STATIC) != 0);

                            methodInfo.instructions.add(instructions);
                            totalInstrumented++;
                        }
                    }

                    methodInfo.check(Opcodes.ASM9);
                }
            }

            // Create class writer for original to get the size
            ClassWriter writer2 = new InstrumentClassWriter(targetPath,
                    ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            classNode.accept(writer2);

            // Save instrumented file
            // Add dummy field to check instrumented
            node.fields.add(new FieldNode(Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL + Opcodes.ACC_STATIC,
                    "greyboxInstrumented", "I", null, Integer.valueOf(0)));
            node.check(Opcodes.ASM9);
            ClassWriter writer = new InstrumentClassWriter(targetPath,
                    ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            node.accept(writer);

            Main.LOGGER.info("Instrumenting " + originalCtxt.getKey() + " - Orig: " + writer2.toByteArray().length
                    + ", Patched: " + writer.toByteArray().length);
            byte[] newClass = writer.toByteArray();
            if (targetPath.endsWith(".class")) {
                FileOutputStream fos = new FileOutputStream(targetPath);
                fos.write(newClass);
                fos.close();
            } else {
                FileOutputStream fos = new FileOutputStream(targetPath + "/" + originalCtxt.getKey());
                fos.write(newClass);
                fos.close();
            }

            double totalTime = (Calendar.getInstance().getTimeInMillis() - start) / 1000.0; // Seconds
            if (timeMap.containsKey(classNode.sourceFile)) {
                timeMap.put(classNode.sourceFile, timeMap.get(classNode.sourceFile) + totalTime);
            } else {
                timeMap.put(classNode.sourceFile, totalTime);
            }
        }
        Main.LOGGER.log(Level.INFO, "Total instrumented: " + totalInstrumented);
        Main.LOGGER.log(Level.INFO, "Final prev id: " + prevId);

        if (!timeFileOutput.equals("")) {
            FileWriter writer = new FileWriter(timeFileOutput);
            for (Map.Entry<String, Double> entry : timeMap.entrySet()) {
                writer.write(entry.getKey() + "," + entry.getValue() + "\n");
            }
            writer.close();
        }
    }

    public static int totalInstrumented = 0;

    /**
     * Compute branch IDs.
     *
     * @param instructions instructions of method
     * @param className    class name
     * @param methodName   method name
     * @param methodDesc   method descriptor
     */
    public Map<Integer, Integer> computeBranchIds(InsnList instructions, String className, String methodName,
            String methodDesc) {
        Map<Integer, Integer> ids = new HashMap<>();

        for (int i = 0; i < instructions.size(); i++) {
            AbstractInsnNode insn = instructions.get(i);

            // Compute branch ID
            if (insn.getType() == AbstractInsnNode.JUMP_INSN) {
                if (branchIds.size() == 0 || branchIds.contains(prevId)) {
                    JumpInsnNode jumpInsn = (JumpInsnNode) insn;
                    LabelNode curLabel = jumpInsn.label;
                    int labelIndex = instructions.indexOf(curLabel);

                    Deque<AbstractInsnNode> prevInsns = new ArrayDeque<>();
                    for (int j = labelIndex - 1; j >= 0 && j >= Instrumenter.MAX_PREV_INSNS; j--) {
                        prevInsns.add(instructions.get(j));
                    }

                    String hashSource = className + "::" + methodName + "::" + methodDesc + "::";
                    for (AbstractInsnNode prevInsn : prevInsns) {
                        String nodeString = InsnNodeUtils.convertNodeToString(prevInsn);
                        if (nodeString.length() > 0)
                            hashSource += nodeString + ";";
                    }
                    int hashed = hashSource.hashCode();
                    hashStrings.put(hashed, hashSource);

                    if (ids.containsKey(hashed))
                        Main.LOGGER.finer("Duplicated ID: " + hashed);
                    ids.put(hashed, prevId++);
                } else {
                    prevId++;
                }
            }
        }

        return ids;
    }

}
