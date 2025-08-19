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
    private final Scanner scanner = new Scanner(System.in);
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
        // 現在の命令がnullでないか、または次の命令が存在するかを確認
        return !(this.current == null || this.current.getNext() == null)
                || (this.first != null && this.currentInstructionIndex == 0);
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
        // 現在の命令がnullの場合は、最初の命令に移動
        if (this.current == null)
        {
            this.current = this.first;
            this.currentInstructionIndex = 0;
        }

        // 次の命令が存在するまでスキップ
        while (!(this.current == null || (this.current = forward ? this.current.getNext() : this.current.getPrevious()) == null))
        {
            if (this.current.getOpcode() == -1)  // ラベルやフレームノードの場合
            {
                if (this.current instanceof LabelNode labelNode)
                    this.cacheLabel(labelNode);
            }
            else
            {
                this.currentInstructionIndex += forward ? 1 : -1;  // インデックスを更新
                break;  // 有効な命令に到達したらループを抜ける
            }
        }
    }

    public void stepForward()
    {
        if (this.current == null && this.currentInstructionIndex != 0)
            throw new VMPanic("Cannot step forward when current is null and instruction index is not zero.");

        this.skipToNextInstruction(true);
    }

    public void stepBackward()
    {
        if (this.current == null)
            return;

        this.skipToNextInstruction(false);
    }

    public void setCurrent(int instructionIndex)
    {
        if (instructionIndex < 0)
            throw new VMPanic("Instruction index cannot be negative.");

        // 現在と同じインデックスなら何もしない
        if (this.currentInstructionIndex == instructionIndex)
            return;

        if (this.current == null && this.first == null)
            throw new VMPanic("No instructions available to set.");

        if (this.current == null)
            this.current = this.first;

        if (instructionIndex < this.currentInstructionIndex)
            while (this.current != null && this.currentInstructionIndex > instructionIndex)
                this.stepBackward();
        else  // 次のインデックスに進む
            while (this.current != null && this.currentInstructionIndex < instructionIndex)
                this.stepForward();
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
