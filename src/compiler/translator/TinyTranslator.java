package compiler.translator;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

import com.sun.org.apache.bcel.internal.generic.PUSH;
import compiler.IR;
import compiler.MicroErrorMessages;
import compiler.MicroRuntimeException;
import compiler.SymbolMap;
import compiler.element.Element;
import compiler.element.Register;
import compiler.element.Temporary;

public class TinyTranslator {

    private static final int STACK = 5;
    private static final Map<IR.Opcode, String> dict;

    static {
        Map<IR.Opcode, String> _dict = new HashMap<>();
        _dict.put(IR.Opcode.ADDI, "addi");
        _dict.put(IR.Opcode.ADDF, "addr");
        _dict.put(IR.Opcode.SUBI, "subi");
        _dict.put(IR.Opcode.SUBF, "subr");
        _dict.put(IR.Opcode.MULTI, "muli");
        _dict.put(IR.Opcode.MULTF, "mulr");
        _dict.put(IR.Opcode.DIVI, "divi");
        _dict.put(IR.Opcode.DIVF, "divr");
        _dict.put(IR.Opcode.GT, "jgt");
        _dict.put(IR.Opcode.GE, "jge");
        _dict.put(IR.Opcode.LT, "jlt");
        _dict.put(IR.Opcode.LE, "jle");
        _dict.put(IR.Opcode.NE, "jne");
        _dict.put(IR.Opcode.EQ, "jeq");
        _dict.put(IR.Opcode.JUMP, "jmp");
        _dict.put(IR.Opcode.LABEL, "label");
        _dict.put(IR.Opcode.JSR, "jsr");
        _dict.put(IR.Opcode.PUSH, "push");
        _dict.put(IR.Opcode.POP, "pop");
        _dict.put(IR.Opcode.RETURN, "ret");
        _dict.put(IR.Opcode.LINK, "link");
        _dict.put(IR.Opcode.READI, "sys readi");
        _dict.put(IR.Opcode.READF, "sys readr");
        _dict.put(IR.Opcode.WRITEI, "sys writei");
        _dict.put(IR.Opcode.WRITEF, "sys writer");
        _dict.put(IR.Opcode.WRITES, "sys writes");

        dict = Collections.unmodifiableMap(_dict);
    }

    private enum Type {
        GENERIC, CALC, COMP, JSR, RET, STORE
    }

    private static EnumSet<IR.Opcode> CalcSet = EnumSet.of(
            IR.Opcode.ADDI, IR.Opcode.ADDF, IR.Opcode.SUBI, IR.Opcode.SUBF,
            IR.Opcode.MULTI, IR.Opcode.MULTF, IR.Opcode.DIVI, IR.Opcode.DIVF
    );

    private static EnumSet<IR.Opcode> CompSet = EnumSet.of(
            IR.Opcode.GT, IR.Opcode.GE, IR.Opcode.LT, IR.Opcode.LE, IR.Opcode.NE, IR.Opcode.EQ
    );

    private static EnumSet<IR.Opcode> StoreSet = EnumSet.of(
            IR.Opcode.STOREI, IR.Opcode.STOREF
    );

    private static EnumSet<IR.Opcode> GenericSet = EnumSet.of(
            IR.Opcode.JUMP, IR.Opcode.LABEL, IR.Opcode.PUSH, IR.Opcode.POP, IR.Opcode.LINK,
            IR.Opcode.READI, IR.Opcode.READF, IR.Opcode.WRITEI, IR.Opcode.WRITEF, IR.Opcode.WRITES
    );

    private static EnumSet<IR.Opcode> IgnoreRASet = EnumSet.of(
            IR.Opcode.LINK, IR.Opcode.LABEL, IR.Opcode.JSR, IR.Opcode.JUMP, IR.Opcode.WRITES
    );

    //private int register;
    private Map<String, Integer> map;

    public TinyTranslator() {
        //this.register = -1;
        this.map = new HashMap<>();
    }

    public void printTinyFromIR(SymbolMap globalSymbolMap, IR ir) {
        System.out.println(";tiny code");

        IR tinyIR = transformIRtoTinyIR(ir, globalSymbolMap);
        System.out.println(tinyIR);

        globalSymbolMap.values().stream()
                .map(e -> e.isString() ?
                        String.format("str %s %s", e.getName(), e.getValue()) :
                        String.format("var %s", e.getName()))
                .forEach(System.out::println);

        // Init Main
        System.out.println("push");
        pushReg();
        System.out.println("jsr main");
        System.out.println("sys halt");

        tinyIR.forEach(n -> {
            String op1 = resolveOp(n.getOp1());
            String op2 = resolveOp(n.getOp2());
            String focus = resolveOp(n.getFocus());
            String command = dict.get(n.getOpcode());

            switch(getType(n.getOpcode())) {
                case GENERIC:
                    if (focus == null)
                        System.out.format("%s\n", command);
                    else
                        System.out.format("%s %s\n", command, focus);
                    break;
                case CALC:
                    System.out.format("%s %s %s\n", command, op2, focus);
                    break;
                case COMP:
                    String comp = resolveComp(n.getOp1(), n.getOp2());
                    /*
                    if (!n.getOp2().isTemporary()) {
                        System.out.format("move %s r%s\n", op2, ++register);
                        op2 = "r" + register;
                    }
                    */
                    System.out.format("%s %s %s\n", comp, op1, op2);
                    System.out.format("%s %s\n", command, focus);
                    break;
                case JSR:
                    pushReg();
                    System.out.format("%s %s\n", command, focus);
                    popReg();
                    break;
                case RET:
                    System.out.println("unlnk");
                    System.out.println("ret");
                    break;
                case STORE:
                    /*
                    if (!n.getOp1().isTemporary() && !n.getFocus().isTemporary()) {
                        System.out.format("move %s r%s\n", op1, ++register);
                        op1 = "r" + register;
                    }
                    */
                    if (!(op1.equals(focus)))
                        System.out.format("move %s %s\n", op1, focus);
                    break;
                default:
                    throw new MicroRuntimeException(MicroErrorMessages.UnknownTinyType);
            }
        });

        System.out.println("end");
    }

    private IR transformIRtoTinyIR(IR ir, SymbolMap globalSymbolMap) {
        IR tinyIR = new IR(globalSymbolMap);
        RegisterFile rf = new RegisterFile(4);

        int localCount = 0;
        for (IR.Node n : ir) {
            Register rx = null, ry, rz;
            Element tOp1 = n.getOp1(), tOp2 = n.getOp2(), tFocus = n.getFocus();
            System.out.println(n);
            if (n.getOpcode() == IR.Opcode.LINK)
                localCount = n.getFocus().getCtxVal();

            if (!IgnoreRASet.contains(n.getOpcode())) {
                if (tOp1 != null && !tOp1.isConstant()) {
                    tOp1 = rx = rf.ensure(tOp1, n, tinyIR, localCount);
                    if (!n.getOut().contains(n.getOp1()))
                        rf.free(rx, tinyIR, n.getOut(), localCount);
                }

                if (tOp2 != null && !tOp2.isConstant()) {
                    tOp2 = ry = rf.ensure(tOp2, n, tinyIR, localCount);
                    if (!n.getOut().contains(n.getOp2()))
                        rf.free(ry, tinyIR, n.getOut(), localCount);
                }

                if (CalcSet.contains(n.getOpcode())) {
                    tFocus = rf.transfer(rx, tFocus, tinyIR, n.getOut(), localCount);
                    rx.setDirty(true);
                } else if (tFocus != null && n.getOpcode() == IR.Opcode.PUSH) {
                    tFocus = rf.get(tFocus);
                } else if (tFocus != null && tFocus.isReturn()) {
                    tFocus = tFocus.getTinyElement(localCount);
                } else if (tFocus != null && !CompSet.contains(n.getOpcode())) {
                    tFocus = rz = rf.allocate(n.getFocus(), n, tinyIR, localCount);
                    rz.setDirty(true);
                }
            }

            IR.Node newNode = new IR.Node(n.getOpcode(), tOp1, tOp2, tFocus);
            tinyIR.add(newNode);

            if (n.isLeader() || n.isReturn())
                rf.flush(tinyIR, localCount);

            System.out.println();
        }

        return tinyIR;
    }

    private Type getType(IR.Opcode opcode) {
        if (CalcSet.contains(opcode)) return Type.CALC;
        if (CompSet.contains(opcode)) return Type.COMP;
        if (StoreSet.contains(opcode)) return Type.STORE;
        if (GenericSet.contains(opcode)) return Type.GENERIC;
        if (opcode == IR.Opcode.JSR) return Type.JSR;
        if (opcode == IR.Opcode.RETURN) return Type.RET;

        throw new MicroRuntimeException(MicroErrorMessages.UnknownIRNodeType);
    }

    private String resolveComp(Element op1, Element op2) {
        if ((op1 != null && op1.isFloat()) || (op2 != null && op2.isFloat()))
            return "cmpr";
        else
            return "cmpi";
    }

    private String resolveOp(Element op) {
        if (op == null)
            return null;

        switch (op.getCtx()) {
            case CONSTANT:
                return op.getValue();
            case FLOCAL:
                return "$" + Integer.toString(-op.getCtxVal());
            case FPARAM:
                return "$" + Integer.toString(STACK + op.getCtxVal());
            case RETURN:
                return "$" + Integer.toString(STACK + op.getCtxVal() + 1);
            default:
                return op.getRef();
        }
    }

    private void pushReg() {
        IntStream.rangeClosed(0, 3).mapToObj(i -> "push r" + i).forEach(System.out::println);
    }

    private void popReg() {
        IntStream.rangeClosed(0, 3)
                .map(i -> 3 - i).mapToObj(i -> "pop r" + i).forEach(System.out::println);
    }
}
