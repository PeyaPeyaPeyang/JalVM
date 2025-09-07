package tokyo.peya.langjal.vm.values;

import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.AnnotationNode;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.VMComponent;
import tokyo.peya.langjal.vm.engine.members.VMField;
import tokyo.peya.langjal.vm.panics.VMPanic;
import tokyo.peya.langjal.vm.references.ClassReference;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class VMAnnotation extends VMObject
{
    private final JalVM vm;
    private final VMClass type;

    private final AnnotationNode anno;
    @Getter(AccessLevel.NONE)
    private final Map<String, Object> rawValues;
    @Getter(AccessLevel.NONE)
    private final Map<String, VMValue> valuesCache;

    public VMAnnotation(@NotNull VMComponent component, @NotNull AnnotationNode anno)
    {
        super(component.getClassLoader().findClass(
                ClassReference.of(anno.desc.substring(1, anno.desc.length() - 1))
        ), null);

        this.vm = component.getVM();
        this.type = component.getClassLoader().findClass(ClassReference.of(anno.desc.substring(1, anno.desc.length() - 1)));
        this.anno = anno;
        this.rawValues = new HashMap<>();
        this.valuesCache = new HashMap<>();

        if (anno.values != null)
        {
            List<?> values = anno.values;
            for (int i = 0; i < values.size(); i += 2)
            {
                String key = (String) values.get(i);
                this.rawValues.put(key, values.get(i + 1));
            }
        }

        this.forceInitialise(component.getClassLoader());
    }

    public static List<VMAnnotation> of(@NotNull VMComponent component, @Nullable List<? extends AnnotationNode> annotations)
    {
        if (annotations == null)
            return Collections.emptyList();

        return annotations.stream()
                .map(anno -> new VMAnnotation(component, anno))
                .toList();
    }


    private static VMValue createValue(@NotNull VMComponent com, @NotNull Object value)
    {
        if (!(value instanceof String[] strings))
            return VMValue.fromJavaObject(com, value);

        // enum の場合は -> [enum type, enum value] の形で来る
        if (strings.length != 2)
            throw new VMPanic("Expected enum representation to be of length 2, but got " + strings.length);

        String enumType = strings[0];
        String enumValue = strings[1];

        String enumClass = enumType.substring(1, enumType.length() - 1);  // Ltokyo/peya/langjal/SomeEnum; -> tokyo/peya/langjal/SomeEnum
        VMClass clazz = com.getClassLoader().findClass(ClassReference.of(enumClass));

        VMField field = clazz.findField(enumValue);
        return clazz.getStaticFieldValue(field);
    }

    @NotNull
    public VMValue getValue(@NotNull String key)
    {
        VMValue value = this.getValueSafe(key);
        if (value == null)
            throw new VMPanic("No value found for annotation element: " + key);
        return value;
    }

    @Nullable
    public VMValue getValueSafe(@NotNull String key)
    {
        if (this.valuesCache.containsKey(key))
            return this.valuesCache.get(key);

        if (this.anno.values == null)
            return null;

        Object raw = this.rawValues.get(key);
        if (raw == null)
            return null;

        VMValue value = createValue(this.vm, raw);
        this.valuesCache.put(key, value);
        return value;
    }
}
