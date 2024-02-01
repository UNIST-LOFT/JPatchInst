package kr.ac.unist.apr.utils;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

/**
 * Utility class related to ASM.
 */
public class InsnNodeUtils {
    /**
     * Converts AbstractInsnNode to string to generate a hash.
     * @param node AbstractInsnNode
     * @return string representation of AbstractInsnNode
     */
    public static String convertNodeToString(AbstractInsnNode node) {
        switch (node.getType()){
            case AbstractInsnNode.FIELD_INSN:
                FieldInsnNode fieldInsnNode=(FieldInsnNode)node;
                return "FieldInsn:"+node.getOpcode()+","+fieldInsnNode.owner+"."+fieldInsnNode.name+":"+fieldInsnNode.desc;
            case AbstractInsnNode.FRAME:
                FrameNode frameNode=(FrameNode)node;
                return "Frame:"+node.getOpcode()+","+frameNode.type;
            case AbstractInsnNode.IINC_INSN:
                IincInsnNode iincInsnNode=(IincInsnNode)node;
                return "IincInsn:"+node.getOpcode()+","+iincInsnNode.incr;
            case AbstractInsnNode.INSN:
                return "Insn:"+node.getOpcode();
            case AbstractInsnNode.INT_INSN:
                IntInsnNode intInsnNode=(IntInsnNode)node;
                return "IntInsn:"+node.getOpcode()+","+intInsnNode.operand;
            case AbstractInsnNode.INVOKE_DYNAMIC_INSN:
                InvokeDynamicInsnNode invokeDynamicInsnNode=(InvokeDynamicInsnNode)node;
                return "InvokeDynamicInsn:"+node.getOpcode()+","+invokeDynamicInsnNode.name+":"+invokeDynamicInsnNode.desc;
            case AbstractInsnNode.JUMP_INSN:
                return "JumpInsn:"+node.getOpcode();
            case AbstractInsnNode.LABEL:
                return "";
            case AbstractInsnNode.LDC_INSN:
                return "LdcInsn:"+node.getOpcode();
            case AbstractInsnNode.LINE:
                return "";
            case AbstractInsnNode.LOOKUPSWITCH_INSN:
                return "LookupSwitchInsn:"+node.getOpcode();
            case AbstractInsnNode.METHOD_INSN:
                MethodInsnNode methodInsnNode=(MethodInsnNode)node;
                return "MethodInsn:"+node.getOpcode()+","+methodInsnNode.owner+"."+methodInsnNode.name+":"+methodInsnNode.desc;
            case AbstractInsnNode.MULTIANEWARRAY_INSN:
                MultiANewArrayInsnNode multiANewArrayInsnNode=(MultiANewArrayInsnNode)node;
                return "MultiANewArrayInsn:"+node.getOpcode()+","+multiANewArrayInsnNode.desc+","+multiANewArrayInsnNode.dims;
            case AbstractInsnNode.TABLESWITCH_INSN:
                TableSwitchInsnNode tableSwitchInsnNode=(TableSwitchInsnNode)node;
                return "TableSwitchInsn:"+node.getOpcode()+","+tableSwitchInsnNode.min+","+tableSwitchInsnNode.max;
            case AbstractInsnNode.TYPE_INSN:
                TypeInsnNode typeInsnNode=(TypeInsnNode)node;
                return "TypeInsn:"+node.getOpcode()+","+typeInsnNode.desc;
            case AbstractInsnNode.VAR_INSN:
                return "VarInsn:"+node.getOpcode();
            default:
                throw new RuntimeException("Node type not found: "+node.getType());
        }
    }

    public static boolean compareMethodNode(MethodNode a,MethodNode b) {
        return a.name.equals(b.name) && a.desc.equals(b.desc);
    }

    public static MethodNode findSameMethod(MethodNode source,Iterable<MethodNode> target) {
        for (MethodNode node:target) {
            if (compareMethodNode(source,node)) {
                return node;
            }
        }

        return null;
    }
}
