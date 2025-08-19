package tokyo.peya.langjal.vm.engine;

import lombok.Getter;
import org.antlr.v4.codegen.model.chunk.ThisRulePropertyRef_ctx;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Label;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.VMInterpreter;
import tokyo.peya.langjal.vm.exceptions.VMPanic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class BytecodeInterpreter implements VMInterpreter
{
    private final JalVM vm;
    private final List<AbstractInsnNode> instructions;
    private final Map<Integer, Integer> labelToInstructionIndexMap;

    private int currentInstructionIndex = -1; // -1: 未開始

    public BytecodeInterpreter(@NotNull JalVM vm, @NotNull MethodNode method)
    {
        this.vm = vm;
        this.instructions = new ArrayList<>();
        this.labelToInstructionIndexMap = new HashMap<>();

        int index = 0;
        for (AbstractInsnNode insn : method.instructions)
        {
            // ラベル → 次の実際命令の index をマッピング
            if (insn instanceof LabelNode ln)
                this.labelToInstructionIndexMap.put(ln.getLabel().hashCode(), index);
            else if (insn.getOpcode() != -1)
            {
                this.instructions.add(insn); // 実際の命令だけ追加
                index++;
            }
            // フレームや LineNumberNode は無視
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
        Integer idx = this.labelToInstructionIndexMap.get(label.hashCode());
        if (idx == null)
            throw new VMPanic("Label not found: " + label);
        return idx -1; // -1 するのは、LabelNode の index が実際の命令の index より 1 つ大きいから
    }
}
