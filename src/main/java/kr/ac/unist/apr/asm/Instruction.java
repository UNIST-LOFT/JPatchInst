package kr.ac.unist.apr.asm;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

import kr.ac.unist.apr.GlobalStates;

public class Instruction {
    public static InsnList insertNewInstructions(int branchId) {
        InsnList newInstructions=new InsnList();

        // if (System.getenv("GREYBOX_BRANCH").equals("1"))
        newInstructions.add(new LdcInsnNode(GlobalStates.STATE_ENV_RECORD));
        newInstructions.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "java/lang/System",
            "getenv",
            "(Ljava/lang/String;)Ljava/lang/String;",
            false
        ));
        newInstructions.add(new LdcInsnNode("1"));
        newInstructions.add(new MethodInsnNode(
            Opcodes.INVOKEVIRTUAL,
            "java/lang/String",
            "equals",
            "(Ljava/lang/Object;)Z",
            false
        ));
        LabelNode exit=new LabelNode(new Label()); // Exit label
        newInstructions.add(new JumpInsnNode(Opcodes.IFEQ,exit));

        // if (!GlobalStates.isInitialized)
        newInstructions.add(new FieldInsnNode(Opcodes.GETSTATIC, GlobalStates.STATE_CLASS_NAME.replace('.', '/'),
            GlobalStates.STATE_IS_INITIALIZED, "Z"));
        newInstructions.add(new JumpInsnNode(Opcodes.IFNE, exit));
        
        // GlobalStates.initialize();
        newInstructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, GlobalStates.STATE_CLASS_NAME.replace('.', '/'),
            GlobalStates.STATE_INIT, "()V", false));

        // GlobalStates.curId = GlobalStates.previousId ^ branchId;
        newInstructions.add(exit);
        // newInstructions.add(new FrameNode(Opcodes.F_APPEND, 1, new Object[] {Opcodes.INTEGER}, 0, null));
        // newInstructions.add(new FieldInsnNode(Opcodes.GETSTATIC, GlobalStates.STATE_CLASS_NAME.replace('.', '/'),
        //     GlobalStates.STATE_PREV_ID, "I"));
        newInstructions.add(new IntInsnNode(Opcodes.SIPUSH,branchId));
        // newInstructions.add(new InsnNode(Opcodes.IXOR));
        newInstructions.add(new FieldInsnNode(Opcodes.PUTSTATIC, GlobalStates.STATE_CLASS_NAME.replace('.', '/'), "curId", "I"));

        // GlobalStates.branchCount[GlobalStates.curId]++;
        newInstructions.add(new FieldInsnNode(Opcodes.GETSTATIC, GlobalStates.STATE_CLASS_NAME.replace('.', '/'), GlobalStates.STATE_BRANCH_COUNT, "[I"));
        newInstructions.add(new FieldInsnNode(Opcodes.GETSTATIC, GlobalStates.STATE_CLASS_NAME.replace('.', '/'), "curId", "I"));
        newInstructions.add(new InsnNode(Opcodes.DUP2));
        newInstructions.add(new InsnNode(Opcodes.IALOAD));
        newInstructions.add(new InsnNode(Opcodes.ICONST_1));
        newInstructions.add(new InsnNode(Opcodes.IADD));
        newInstructions.add(new InsnNode(Opcodes.IASTORE));

        // GlobalStates.previousId = GlobalStates.curId >> 1;
        newInstructions.add(new FieldInsnNode(Opcodes.GETSTATIC, GlobalStates.STATE_CLASS_NAME.replace('.', '/'), "curId", "I"));
        newInstructions.add(new InsnNode(Opcodes.ICONST_1));
        newInstructions.add(new InsnNode(Opcodes.ISHR));
        newInstructions.add(new FieldInsnNode(Opcodes.PUTSTATIC, GlobalStates.STATE_CLASS_NAME.replace('.', '/'), GlobalStates.STATE_PREV_ID, "I"));

        return newInstructions;
    }
}
