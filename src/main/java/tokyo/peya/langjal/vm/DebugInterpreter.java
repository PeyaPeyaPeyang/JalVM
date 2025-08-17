package tokyo.peya.langjal.vm;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.threads.VMThread;

import java.util.Scanner;

public class DebugInterpreter implements VMInterpreter
{
    private final JalVM vm;
    private final VMThread engine;
    private final VMFrame frame;

    private final Scanner scanner;

    private boolean isRunning = true;

    public DebugInterpreter(@NotNull JalVM vm, @NotNull VMThread engine, @NotNull VMFrame frame)
    {
        this.vm = vm;
        this.engine = engine;
        this.frame = frame;

        this.scanner = new Scanner(System.in);
    }

    @Override
    public boolean hasNextInstruction()
    {
        return this.isRunning;
    }

    @Override
    public AbstractInsnNode feedNextInstruction()
    {
        if (!this.isRunning)
        {
            throw new IllegalStateException("Interpreter is not running.");
        }

        String input = this.okNext();
        String[] parts = input.split(" ");
        if (parts.length == 0)
            return null;

        String insn = parts[0];
        switch (insn)
        {
            case "exit" ->
            {
                this.isRunning = false;
                out("Bye!");
                return null;
            }
            case "show" ->
            {
                out("Current frame: %s", this.frame);
                out("Current thread: %s", this.engine.getName());
                out("Current method: %s", this.frame.getMethod().getMethodNode().name);
                out("Stack: %s", this.frame.getStack());
                out("Locals: %s", this.frame.getLocals());
                return null;
            }
            case "sipush" ->
            {
                if (!checkArgs(parts, 2))
                {
                    return null;
                }
                int value = asInt(parts[1]);
                return new IntInsnNode(EOpcodes.SIPUSH, value);
            }

            // <editor-fold desc="Stack">
            case "pop" ->
            {
                return new InsnNode(EOpcodes.POP);
            }
            case "pop2" ->
            {
                return new InsnNode(EOpcodes.POP2);
            }
            case "dup" ->
            {
                return new InsnNode(EOpcodes.DUP);
            }
            case "dup2" ->
            {
                return new InsnNode(EOpcodes.DUP2);
            }
            case "dup_x1" ->
            {
                return new InsnNode(EOpcodes.DUP_X1);
            }
            case "dup_x2" ->
            {
                return new InsnNode(EOpcodes.DUP_X2);
            }
            case "dup2_x1" ->
            {
                return new InsnNode(EOpcodes.DUP2_X1);
            }
            case "dup2_x2" ->
            {
                return new InsnNode(EOpcodes.DUP2_X2);
            }
            case "swap" ->
            {
                return new InsnNode(EOpcodes.SWAP);
            }
            // </editor-fold>

            // <editor-fold desc="Math">
            case "iadd" ->
            {
                return new InsnNode(EOpcodes.IADD);
            }
            case "ladd" ->
            {
                return new InsnNode(EOpcodes.LADD);
            }
            case "fadd" ->
            {
                return new InsnNode(EOpcodes.FADD);
            }
            case "dadd" ->
            {
                return new InsnNode(EOpcodes.DADD);
            }
            case "isub" ->
            {
                return new InsnNode(EOpcodes.ISUB);
            }
            case "lsub" ->
            {
                return new InsnNode(EOpcodes.LSUB);
            }
            case "fsub" ->
            {
                return new InsnNode(EOpcodes.FSUB);
            }
            case "dsub" ->
            {
                return new InsnNode(EOpcodes.DSUB);
            }
            case "imul" ->
            {
                return new InsnNode(EOpcodes.IMUL);
            }
            case "lmul" ->
            {
                return new InsnNode(EOpcodes.LMUL);
            }
            case "fmul" ->
            {
                return new InsnNode(EOpcodes.FMUL);
            }
            case "dmul" ->
            {
                return new InsnNode(EOpcodes.DMUL);
            }
            case "idiv" ->
            {
                return new InsnNode(EOpcodes.IDIV);
            }
            case "ldiv" ->
            {
                return new InsnNode(EOpcodes.LDIV);
            }
            case "fdiv" ->
            {
                return new InsnNode(EOpcodes.FDIV);
            }
            case "ddiv" ->
            {
                return new InsnNode(EOpcodes.DDIV);
            }
            case "irem" ->
            {
                return new InsnNode(EOpcodes.IREM);
            }
            case "lrem" ->
            {
                return new InsnNode(EOpcodes.LREM);
            }
            case "frem" ->
            {
                return new InsnNode(EOpcodes.FREM);
            }
            case "drem" ->
            {
                return new InsnNode(EOpcodes.DREM);
            }
            case "ineg" ->
            {
                return new InsnNode(EOpcodes.INEG);
            }
            case "lneg" ->
            {
                return new InsnNode(EOpcodes.LNEG);
            }
            case "fneg" ->
            {
                return new InsnNode(EOpcodes.FNEG);
            }
            case "dneg" ->
            {
                return new InsnNode(EOpcodes.DNEG);
            }
            case "ishl" ->
            {
                return new InsnNode(EOpcodes.ISHL);
            }
            case "lshl" ->
            {
                return new InsnNode(EOpcodes.LSHL);
            }
            case "ishr" ->
            {
                return new InsnNode(EOpcodes.ISHR);
            }
            case "lshr" ->
            {
                return new InsnNode(EOpcodes.LSHR);
            }
            case "iushr" ->
            {
                return new InsnNode(EOpcodes.IUSHR);
            }
            case "lushr" ->
            {
                return new InsnNode(EOpcodes.LUSHR);
            }
            case "iand" ->
            {
                return new InsnNode(EOpcodes.IAND);
            }
            case "land" ->
            {
                return new InsnNode(EOpcodes.LAND);
            }
            case "ior" ->
            {
                return new InsnNode(EOpcodes.IOR);
            }
            case "lor" ->
            {
                return new InsnNode(EOpcodes.LOR);
            }
            case "ixor" ->
            {
                return new InsnNode(EOpcodes.IXOR);
            }
            case "lxor" ->
            {
                return new InsnNode(EOpcodes.LXOR);
            }
            case "iinc" ->
            {
                if (!checkArgs(parts, 3, 2))
                {
                    return null;
                }
                int index = asInt(parts[1]);
                int increment = asInt(parts[2]);
                return new IincInsnNode(index, increment);
            }
            // </editor-fold>

            default ->
            {
                out("Unknown instruction: ", input);
                return null;
            }
        }
    }

    private boolean checkArgs(String[] parts, int expected, int minExpected)
    {
        if (parts.length < minExpected)
        {
            out("Usage: %s", parts[0]);
            return false;
        }
        if (parts.length > expected)
        {
            out("Too many arguments for %s", parts[0]);
            return false;
        }
        return true;
    }

    private boolean checkArgs(String[] parts, int expected)
    {
        return checkArgs(parts, expected, expected);
    }

    private int asInt(@NotNull String value)
    {
        try
        {
            return Integer.parseInt(value);
        }
        catch (NumberFormatException e)
        {
            out("Invalid integer: %s", value);
            return 0; // or throw an exception
        }
    }

    private String okNext()
    {
        System.out.print(this.frame.getThread().getName() + " OK >");
        return this.scanner.nextLine();
    }

    private static void out(String message, Object... args)
    {
        System.out.printf((message) + "%n", args);
    }
}
