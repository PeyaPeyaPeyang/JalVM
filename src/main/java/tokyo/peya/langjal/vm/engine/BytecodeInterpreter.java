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

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class BytecodeInterpreter implements VMInterpreter
{
    private final JalVM vm;

    private final AbstractInsnNode first;
    private final Map<Integer, Integer> labelToInstructionIndexMap;

    private AbstractInsnNode current;
    @Getter
    private int currentInstructionIndex;

    public BytecodeInterpreter(@NotNull JalVM vm, @NotNull MethodNode method)
    {
        this.vm = vm;
        this.labelToInstructionIndexMap = new HashMap<>();

        this.first = method.instructions.getFirst();
        this.current = null;
    }

    @Override
    public boolean hasNextInstruction()
    {
        if (this.first == null) return false;
        if (this.current == null) return true; // まだ開始してない → 最初の命令はある
        AbstractInsnNode next = this.current.getNext();
        while (next != null && next.getOpcode() == -1) // ラベルや
        {
            if (next instanceof LabelNode ln)
                cacheLabel(ln);
            next = next.getNext();
        }
        return next != null && next.getOpcode() != -1; // 次の命令が存在するか
    }

    private void cacheLabel(@NotNull LabelNode label)
    {
        // ラベルノードのオフセットを取得し、インデックスを記録する
        int labelID = label.getLabel().hashCode();
        if (!this.labelToInstructionIndexMap.containsKey(labelID))
            this.labelToInstructionIndexMap.put(labelID, this.currentInstructionIndex);
    }


    private void skipToNextInstruction(boolean forward)
    {
        AbstractInsnNode next = this.current;
        while (true)
        {
            next = next == null ? this.first: (forward ? next.getNext() : next.getPrevious());

            if (next == null)  // 終端
            {
                this.current = null;
                if (!forward)
                    this.currentInstructionIndex = 0;  // 戻るときは0に設定

                return;
            }

            if (next.getOpcode() == -1)
            {
                if (next instanceof LabelNode ln)
                    cacheLabel(ln);
                continue; // ラベルやフレームは飛ばす
            }

            // 有効命令にヒット
            this.current = next;
            this.currentInstructionIndex += (forward ? 1 : -1);
            return;
        }
    }

    public void stepForward()
    {
        if (this.first == null)
            throw new VMPanic("No instructions available.");

        if (this.current == null)
        {
            // 初回だけ特別扱い
            this.current = this.first;
            while (this.current != null && this.current.getOpcode() == -1)
            {
                if (this.current instanceof LabelNode ln)
                    cacheLabel(ln);
                this.current = this.current.getNext();
            }
            this.currentInstructionIndex = 0;
            return;
        }
        skipToNextInstruction(true);
    }

    public void stepBackward()
    {
        if (this.current == null) return;
        skipToNextInstruction(false);
    }

    public void setCurrent(int instructionIndex)
    {
        if (instructionIndex < 0)
            throw new VMPanic("Instruction index cannot be negative.");
        if (this.first == null)
            throw new VMPanic("No instructions available to set.");

        // 現在未設定なら先頭にジャンプ
        if (this.current == null)
            this.currentInstructionIndex = -1;

        while (this.currentInstructionIndex < instructionIndex)
            stepForward();
        while (this.currentInstructionIndex > instructionIndex)
            stepBackward();
    }
    @Override
    public AbstractInsnNode feedNextInstruction()
    {
        if (!this.hasNextInstruction())
            throw new VMPanic("No next instruction available.");

        this.stepForward();
        return this.current;
    }

    @Override
    public int getLabelInstructionIndex(@NotNull Label node)
    {
        int labelID = node.hashCode();
        if (this.labelToInstructionIndexMap.containsKey(labelID))
            return this.labelToInstructionIndexMap.get(labelID);

        // キャッシュにない場合は，最初から探索
        AbstractInsnNode current = this.first;
        int index = 0;
        while (current != null)
        {
            if (current instanceof LabelNode labelNode && labelNode.getLabel().hashCode() == labelID)
            {
                this.labelToInstructionIndexMap.put(labelID, index);
                return index;
            }

            current = current.getNext();
            if (current.getOpcode() != -1) // -1はラベルやフレーム
                index++;
        }

        throw new VMPanic("Label with ID " + labelID + " not found in the method instructions.");
    }
}
