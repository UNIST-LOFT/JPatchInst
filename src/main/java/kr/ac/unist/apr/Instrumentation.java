package kr.ac.unist.apr;

import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import soot.Body;
import soot.BodyTransformer;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.GotoStmt;
import soot.jimple.IfStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.Jimple;
import soot.jimple.LongConstant;
import soot.jimple.NegExpr;
import soot.jimple.Stmt;
import soot.jimple.SwitchStmt;
import soot.util.Chain;

public class Instrumentation extends BodyTransformer{
    static final SootClass GLOBAL_CLASS=Scene.v().loadClassAndSupport("kr.ac.unist.apr.GlobalStates");
    static final SootMethod BRANCH_METHOD=GLOBAL_CLASS.getMethod("void setBranchInfo(long)");
    static final SootMethod SAVE_METHOD=GLOBAL_CLASS.getMethod("void saveBranchInfo()");

    static final SootClass SYSTEM_CLASS=Scene.v().loadClassAndSupport("java.lang.System");
    static final SootMethod ENV_VAR_METHOD=SYSTEM_CLASS.getMethod("java.lang.String getenv(java.lang.String)");
    static final SootClass STRING_CLASS=Scene.v().loadClassAndSupport("java.lang.String");
    static final SootMethod STRING_EQUALS_METHOD=STRING_CLASS.getMethod("boolean equals(java.lang.Object)");

    @Override
    protected void internalTransform(Body arg0, String arg1, Map<String, String> arg2) {
        Chain<Unit> units=arg0.getUnits();
        
        Iterator<?> iter=units.snapshotIterator();
        while (iter.hasNext()){
            Stmt stmt=(Stmt)iter.next();

            if (stmt instanceof IfStmt){
                // If statement
                IfStmt ifStmt=(IfStmt)stmt;

                // Then branch
                InvokeExpr expr=Jimple.v().newStaticInvokeExpr(BRANCH_METHOD.makeRef(), LongConstant.v(genId()));
                Stmt invokeStmt=Jimple.v().newInvokeStmt(expr);
                units.insertAfter(invokeStmt, ifStmt);

                addSave(invokeStmt, units);

                // Else branch
                expr=Jimple.v().newStaticInvokeExpr(BRANCH_METHOD.makeRef(), LongConstant.v(genId()));
                invokeStmt=Jimple.v().newInvokeStmt(expr);

                units.insertBefore(invokeStmt, ifStmt.getTarget());

                addSave(invokeStmt, units);
            }
            else if (stmt instanceof GotoStmt){
                // Goto statement
                GotoStmt gotoStmt=(GotoStmt)stmt;

                InvokeExpr expr=Jimple.v().newStaticInvokeExpr(BRANCH_METHOD.makeRef(), LongConstant.v(genId()));
                Stmt invokeStmt=Jimple.v().newInvokeStmt(expr);

                units.insertBefore(invokeStmt, gotoStmt.getTarget());

                addSave(invokeStmt, units);
            }
            else if (stmt instanceof SwitchStmt){
                // Switch-case statement
                SwitchStmt switchStmt=(SwitchStmt)stmt;

                // Instrument cases
                for (Unit target:switchStmt.getTargets()){
                    InvokeExpr expr=Jimple.v().newStaticInvokeExpr(BRANCH_METHOD.makeRef(), LongConstant.v(genId()));
                    Stmt invokeStmt=Jimple.v().newInvokeStmt(expr);

                    units.insertBefore(invokeStmt, target);

                    addSave(invokeStmt, units);
                }

                // Instrument default case
                InvokeExpr expr=Jimple.v().newStaticInvokeExpr(BRANCH_METHOD.makeRef(), LongConstant.v(genId()));
                Stmt invokeStmt=Jimple.v().newInvokeStmt(expr);

                units.insertBefore(invokeStmt, switchStmt.getDefaultTarget());

                addSave(invokeStmt, units);
            }

        }
    }

    private void addSave(Stmt stmt,Chain<Unit> units) {
        // Instrument to save to file
        InvokeExpr expr=Jimple.v().newStaticInvokeExpr(SAVE_METHOD.makeRef());
        Stmt invokeStmt=Jimple.v().newInvokeStmt(expr);
        units.insertAfter(invokeStmt,stmt);
    }
    
    private long genId(){
        Random rand=new Random();
        long id=rand.nextLong();
        while (GlobalStates.usedIds.contains(id)){
            id=rand.nextLong();
        }
        GlobalStates.usedIds.add(id);

        return id;
    }
}
