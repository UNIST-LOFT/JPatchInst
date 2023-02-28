package kr.ac.unist.apr;

import java.io.PrintStream;
import java.lang.reflect.Method;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

public class InstructionMethodAdapter extends InstructionAdapter implements Opcodes {
  private final String className;
  private final String methodName;
  private final String descriptor;
  private final String superName;

  public InstructionMethodAdapter(MethodVisitor mv, String className,
      String methodName, String descriptor, String superName) {
    super(ASM8, mv);
    this.className = className;
    this.methodName = methodName;
    this.descriptor = descriptor;
    this.superName = superName;
  }

  @Override
  public void visitJumpInsn(int opcode, Label label){
    addBranchExecuted();
    super.visitJumpInsn(opcode, label);
  }

  @Override
  public void visitCode(){
    // try {
    //   super.mv.visitMethodInsn(INVOKESTATIC, GlobalStates.globalClass, GlobalStates.globalSaveMethod,
    //             Type.getMethodDescriptor(GlobalStates.class.getMethod(GlobalStates.globalSaveMethod)), false);
    //   super.visitEnd();
    // } catch (NoSuchMethodException | SecurityException e) {
    //   e.printStackTrace();
    // }
    addBranchExecuted();
    visitEnd();
  }

  private void addBranchExecuted(){
    // try {
      // Add print statement
      // super.mv.visitLdcInsn(GlobalStates.currentBranchId);
      // super.mv.visitMethodInsn(INVOKEVIRTUAL, "java.io.PrintStream", "println", Type.getMethodDescriptor(PrintStream.class.getMethod("println", long.class)),false);

      // Load current ID of branch
      // super.mv.visitLdcInsn(Long.valueOf(GlobalStates.currentBranchId));
      lconst(GlobalStates.currentBranchId);
      // super.mv.visitInsn(Opcodes.ICONST_1);
      GlobalStates.currentBranchId++;

      // Add call for GlobalStates.setBranchInfo(id)
      // super.mv.visitMethodInsn(INVOKESTATIC, GlobalStates.globalClass, GlobalStates.globalMethod, 
      //         Type.getMethodDescriptor(GlobalStates.class.getMethod(GlobalStates.globalMethod, long.class)), false);
      try {
        invokestatic(GlobalStates.globalClass, GlobalStates.globalMethod, 
                Type.getMethodDescriptor(GlobalStates.class.getMethod(GlobalStates.globalMethod, long.class)), false);
      } catch (NoSuchMethodException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (SecurityException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    // } catch (NoSuchMethodException e) {
    //   e.printStackTrace();
    // } catch (SecurityException e) {
    //   e.printStackTrace();
    // }
  }
}
