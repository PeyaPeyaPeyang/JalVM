package tokyo.peya.langjal.vm.values.metaobjects;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ModuleNode;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.members.VMMethod;
import tokyo.peya.langjal.vm.engine.threading.VMThread;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMInteger;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMValue;
import tokyo.peya.langjal.vm.values.metaobjects.reflection.VMMethodObject;

import java.util.SortedMap;

@Getter
public class VMStackTraceElementObject extends VMObject
{
    private final JalVM vm;
    private final String classLoaderName;
    private final String moduleName;
    private final String moduleVersion;
    private final String declaringClass;
    private final String methodName;
    private final String fileName;
    private final int lineNumber;

    public VMStackTraceElementObject(@NotNull JalVM vm,
                                     @NotNull VMMethod method,
                                     int lineNumber)
    {
        super(vm.getClassLoader().findClass(ClassReference.of("java/lang/StackTraceElement")));
        this.vm = vm;
        this.classLoaderName = null;
        VMClass clazz = method.getClazz();
        ModuleNode mod = clazz.getClazz().module;
        if (mod == null)
        {
            this.moduleName = null;
            this.moduleVersion = null;
        }
        else
        {
            this.moduleName = mod.name;
            this.moduleVersion = mod.version;
        }

        this.declaringClass = clazz.getReference().getFullQualifiedDotName();
        this.methodName = method.getName();
        this.fileName = clazz.getClazz().sourceFile;
        this.lineNumber = lineNumber;

        this.setField("classLoaderName", VMStringObject.createString(vm, this.classLoaderName));
        this.setField("moduleName", VMStringObject.createString(vm, this.moduleName));
        this.setField("moduleVersion", VMStringObject.createString(vm, this.moduleVersion));
        this.setField("declaringClass", VMStringObject.createString(vm, this.declaringClass));
        this.setField("methodName", VMStringObject.createString(vm, this.methodName));
        this.setField("fileName", VMStringObject.createString(vm, this.fileName));
        this.setField("lineNumber", new VMInteger(vm, this.lineNumber));

        this.forceInitialise(vm.getClassLoader());
    }

    public VMStackTraceElementObject(@NotNull JalVM vm,
                                     @NotNull StackTraceElement original)
    {
        super(vm.getClassLoader().findClass(ClassReference.of("java/lang/StackTraceElement")));

        this.vm = vm;
        this.classLoaderName = original.getClassLoaderName();
        this.moduleName = original.getModuleName();
        this.moduleVersion = original.getModuleVersion();
        this.declaringClass = original.getClassName();
        this.methodName = original.getMethodName();
        this.fileName = original.getFileName();
        this.lineNumber = original.getLineNumber();

        this.setField("classLoaderName", VMStringObject.createString(vm, this.classLoaderName));
        this.setField("moduleName", VMStringObject.createString(vm, this.moduleName));
        this.setField("moduleVersion", VMStringObject.createString(vm, this.moduleVersion));
        this.setField("declaringClass", VMStringObject.createString(vm, this.declaringClass));
        this.setField("methodName", VMStringObject.createString(vm, this.methodName));
        this.setField("fileName", VMStringObject.createString(vm, this.fileName));
        this.setField("lineNumber", new VMInteger(vm, this.lineNumber));
        this.forceInitialise(vm.getClassLoader());
    }
}
