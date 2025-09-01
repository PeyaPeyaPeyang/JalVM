package tokyo.peya.langjal.vm.engine;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Label;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import tokyo.peya.langjal.vm.panics.VMPanic;
import tokyo.peya.langjal.vm.references.ClassReference;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class BytecodeInterpreter implements VMInterpreter
{
    private final MethodNode method;
    private final List<AbstractInsnNode> instructions;
    private final LinkedHashMap<Integer, Label> labelToInstructionIndexMap;
    private final LinkedHashMap<Label, Integer> labelToLineNumberMap;

    private final List<ExceptionHandlerDirective> exceptionHandlers;

    private int currentInstructionIndex = -1; // -1: 未開始

    public BytecodeInterpreter(@NotNull VMComponent com, @NotNull MethodNode method)
    {
        this.method = method;

        this.instructions = new ArrayList<>();
        this.labelToInstructionIndexMap = new LinkedHashMap<>();
        this.labelToLineNumberMap = new LinkedHashMap<>();
        this.exceptionHandlers = new LinkedList<>();

        int index = 0;
        for (AbstractInsnNode insn : method.instructions)
        {
            // ラベル → 次の実際命令の index をマッピング
            if (insn instanceof LabelNode ln)
                this.labelToInstructionIndexMap.put(index, ln.getLabel());
            else if (insn instanceof LineNumberNode line)
                this.labelToLineNumberMap.put(line.start.getLabel(), line.line);
            else if (insn.getOpcode() != -1)
            {
                this.instructions.add(insn); // 実際の命令だけ追加
                index++;
            }
            // フレームや LineNumberNode は無視
        }

        // 例外ハンドラを読み込んでおく
        for (TryCatchBlockNode tcb : method.tryCatchBlocks)
        {
            int startIdx = this.getLabelInstructionIndex(tcb.start.getLabel());
            int endIdx = this.getLabelInstructionIndex(tcb.end.getLabel());
            int handlerIdx = this.getLabelInstructionIndex(tcb.handler.getLabel());
            VMClass exceptionType = tcb.type == null
                    ? null
                    : com.getClassLoader().findClass(ClassReference.of(tcb.type));
            this.exceptionHandlers.add(new ExceptionHandlerDirective(
                    startIdx,
                    endIdx,
                    handlerIdx,
                    exceptionType
            ));
        }
    }

    @Override
    public boolean hasNextInstruction()
    {
        return this.currentInstructionIndex + 1 < this.instructions.size();
    }

    @Override
    public AbstractInsnNode feedNextInstruction()
    {
        if (!hasNextInstruction())
            throw new VMPanic("No next instruction available.");

        this.currentInstructionIndex++;
        return this.instructions.get(this.currentInstructionIndex);
    }

    @Override
    public AbstractInsnNode getCurrentInstruction()
    {
        if (this.currentInstructionIndex < 0 || this.currentInstructionIndex >= this.instructions.size())
            throw new VMPanic("No current instruction available.");
        return this.instructions.get(this.currentInstructionIndex);
    }

    public void stepForward()
    {
        if (this.instructions.isEmpty())
            throw new VMPanic("No instructions available.");
        else if (!hasNextInstruction())
            throw new VMPanic("No next instruction available.");
        else
            this.currentInstructionIndex++;
    }

    public void stepBackward()
    {
        if (this.currentInstructionIndex < 0)
            throw new VMPanic("Already at the first instruction.");
        this.currentInstructionIndex--;
    }

    public void setCurrent(int instructionIndex)
    {
        if (instructionIndex >= this.instructions.size())
            throw new VMPanic("Invalid instruction index: " + instructionIndex);

        this.currentInstructionIndex = instructionIndex;
    }

    @Override
    public int getCurrentInstructionIndex()
    {
        return this.currentInstructionIndex;
    }


    @Override
    public int getLabelInstructionIndex(@NotNull Label label)
    {
        int idx = this.labelToInstructionIndexMap.entrySet().stream()
                                                 .filter(e -> e.getValue().equals(label))
                                                 .map(Map.Entry::getKey)
                                                 .findFirst()
                                                 .orElseThrow(() -> new VMPanic("Label not found in method instructions: " + label));

        return idx - 1; // -1 するのは、LabelNode の index が実際の命令の index より 1 つ大きいから
    }

    @Override
    @Nullable
    public Label getInstructionLabel(int instructionIndex)
    {
        Integer current = null;
        for (Map.Entry<Integer, Label> entry : this.labelToInstructionIndexMap.entrySet())
        {
            if (entry.getKey() > instructionIndex)
                break;
            current = entry.getKey();
        }

        if (current == null)
            return null;
        return this.labelToInstructionIndexMap.get(current);
    }

    @Override
    public int getLineNumberOf(int instructionIndex)
    {
        Label label = getInstructionLabel(instructionIndex);
        if (label == null)
            return -1;

        return this.labelToLineNumberMap.getOrDefault(label, -1);
    }

    @Override
    public @Nullable ExceptionHandlerDirective getExceptionHandlerFor(int instructionIndex,
                                                                      @NotNull VMClass exceptionClass)
    {
        for (ExceptionHandlerDirective ehd : this.exceptionHandlers)
            if (ehd.startInstructionIndex() <= instructionIndex && instructionIndex < ehd.endInstructionIndex())
            {
                VMClass exceptionType = ehd.exceptionType();
                if (exceptionType == null || exceptionType.isAssignableFrom(exceptionClass))
                    return ehd;
            }

        return null;
    }
}
