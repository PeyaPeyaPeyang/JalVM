package tokyo.peya.langjal.vm;

import tokyo.peya.langjal.vm.references.ClassReference;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main
{
    public static void main(String[] args)
    {
        VMConfiguration config = parseArgs(args);

        JalVM vm = new JalVM(config);
        vm.executeMain();
    }
    public static VMConfiguration parseArgs(String[] args)
    {
        List<Path> classPaths = new ArrayList<>();
        boolean enableAssertions = false;

        String mainClass = null;
        List<String> mainArgs = new ArrayList<>();

        for (int i = 0; i < args.length; i++)
        {
            String arg = args[i];

            // -ea or -enableassertions or -ea:...
            if (arg.equals("-ea") || arg.equalsIgnoreCase("-enableassertions") || arg.startsWith("-ea:"))
            {
                enableAssertions = true;
                continue;
            }

            // -version
            if (arg.equals("-version"))
            {
                System.out.println("jalvm version \"1.0.0\"");
                System.out.println("LangJAL VM - J(al)VM (build 1.0.0");
                System.out.println("LangJAL Implementation (build 1.2.1");
                System.exit(0);
            }

            // -cp <value> or -classpath <value>
            if (arg.equals("-cp") || arg.equalsIgnoreCase("-classpath"))
            {
                if (i + 1 >= args.length)
                    throw new IllegalArgumentException("Missing argument for " + arg);

                String paths = args[++i]; // consume next as cp value
                addClasspathEntries(paths, classPaths);
                continue;
            }

            // -cp=... or -classpath=... or -cp:...
            if (arg.startsWith("-cp=") || arg.startsWith("-classpath=")
                    || arg.startsWith("-cp:") || arg.startsWith("-classpath:"))
            {
                int equal = arg.indexOf('=');

                String val;
                if (equal >= 0)
                    val = arg.substring(equal + 1);
                else
                {
                    int colon = arg.indexOf(':');
                    val = (colon >= 0) ? arg.substring(colon + 1) : "";
                }
                addClasspathEntries(val, classPaths);
                continue;
            }

            // Main 以降の最初の引数を main class として扱う
            if (!arg.startsWith("-"))
            {
                mainClass = arg;
                // 以降の引数を mainArgs として扱う
                if (i + 1 < args.length)
                    mainArgs.addAll(Arrays.asList(args).subList(i + 1, args.length));
                break;
            }

            // その他のオプションは無視する。
        }

        if (mainClass == null)
            throw new IllegalArgumentException("No main class specified");

        return VMConfiguration.builder(ClassReference.of(mainClass))
                              .classPaths(classPaths)
                              .enableAssertions(enableAssertions)
                              .mainArgs(mainArgs)
                              .build();
    }

    private static void addClasspathEntries(String raw, List<? super Path> dest)
    {
        if (raw == null || raw.isEmpty())
        {
            return;
        }
        String[] parts = raw.split(File.pathSeparator);
        Arrays.stream(parts)
              .map(String::trim)
              .filter(s -> !s.isEmpty())
              .map(Paths::get)
              .forEach(dest::add);
    }
}
