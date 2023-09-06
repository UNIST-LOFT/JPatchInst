package kr.ac.unist.apr.visitor;

import java.util.ArrayDeque;
import java.util.Deque;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import kr.ac.unist.apr.Instrumenter;
import kr.ac.unist.apr.utils.InsnNodeUtils;

/**
 * Method visitor for instrumentation.
 * @author YoungJae Kim (FreddyYJ)
 */
public class MethodInstrumenter extends MethodNode {
    private String className;

    /**
     * Default constructor.
     * @param api ASM API version
     * @param className class name
     * @param access access modifier
     * @param name method name
     * @param descriptor method descriptor
     * @param signature method signature
     * @param exceptions method exceptions
     */
    public MethodInstrumenter(int api,String className, int access, String name, String descriptor, String signature,
            String[] exceptions) {
        super(api, access, name, descriptor, signature, exceptions);
        this.className=className;
    }

    /**
     * It instruments the entry of branches.
     */
    @Override
    public void visitLabel(Label label) {
        super.visitLabel(label);      
        String hashSource=className+"::"+super.name+"::"+super.desc+"::";

        // Get k previous instructions
        Deque<AbstractInsnNode> prevInsns=new ArrayDeque<>();
        for (int i=instructions.size()-1;i>=0 || i>=Instrumenter.MAX_PREV_INSNS;i--) {
            prevInsns.add(instructions.get(i));
        }

        for (AbstractInsnNode prevInsn:prevInsns) {
            hashSource+=InsnNodeUtils.convertNodeToString(prevInsn)+";";
        }
        int hashed=hashSource.hashCode();

        if (Instrumenter.ids.containsKey(hashed)){
            int branchId=Instrumenter.ids.get(hashed);
            // TODO: Change to actual instrumentation (now it is just print)
            instructions.add(new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/System", "out",
                                                    "Ljava/io/PrintStream;"));
            instructions.add(new LdcInsnNode("Branch ID:"+branchId));
            instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println",
                                        "(Ljava/lang/String;)V",false));
        }
    }
}
