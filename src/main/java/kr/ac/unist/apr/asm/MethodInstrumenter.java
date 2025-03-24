package kr.ac.unist.apr.asm;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.Label;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;

import kr.ac.unist.apr.Instrumenter;
import kr.ac.unist.apr.utils.InsnNodeUtils;

/**
 * Method visitor for instrumentation.
 * @author YoungJae Kim (FreddyYJ)
 */
public class MethodInstrumenter extends MethodNode {
    private String className;
    private Map<Integer,Integer> ids;

    private int currentLine;

    private Map<LabelNode,InsnList> newInsns=new HashMap<>();

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
            String[] exceptions,Map<Integer,Integer> ids) {
        super(api, access, name, descriptor, signature, exceptions);
        this.className=className;
        this.ids=ids;
    }

    /**
     * It instruments the entry of branches.
     */
    @Override
    public void visitLabel(Label label) {
        super.visitLabel(label);
        
        if (currentLine==0) return; // Between methods/fields

        String hashSource=className+"::"+super.name+"::"+super.desc+"::";

        // Get k previous instructions
        Deque<AbstractInsnNode> prevInsns=new ArrayDeque<>();
        for (int i=instructions.size()-1;i>=0 && i>=Instrumenter.MAX_PREV_INSNS;i--) {
            prevInsns.add(instructions.get(i));
        }

        for (AbstractInsnNode prevInsn:prevInsns) {
            String nodeString=InsnNodeUtils.convertNodeToString(prevInsn);
            if (nodeString.length()>0)
                hashSource+=nodeString+";";
        }
        int hashed=hashSource.hashCode();

        if (ids.containsKey(hashed)){
            int branchId=ids.get(hashed);
            InsnList newInsns=Instruction.insertNewInstructions(branchId);
            this.newInsns.put((LabelNode)instructions.getLast(), newInsns);
            Instrumenter.totalInstrumented++;
        }
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        super.visitLineNumber(line, start);
        currentLine=line;
    }

    public Map<LabelNode,InsnList> getNewInsns() {
        return newInsns;
    }
}
