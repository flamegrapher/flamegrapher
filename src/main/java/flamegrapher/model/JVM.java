package flamegrapher.model;

import static java.util.regex.Pattern.compile;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JVM {

    private static final Pattern VM_VERSION = compile("JDK (?<majorVersion>[0-9])\\.",
            Pattern.DOTALL);

    private final int majorVersion;

    private String fullOutput;
    
    public JVM(int majorVersion) {
        this.majorVersion = majorVersion;
    }

    public String getFullOutput() {
        return fullOutput;
    }

    public void setFullOutput(String fullOutput) {
        this.fullOutput = fullOutput;
    }

    public int getMajorVersion() {
        return majorVersion;
    }

    public static JVM fromVMVersion(String output) {
        Matcher matcher = VM_VERSION.matcher(output);
        JVM vm = null;
        if (matcher.find()) {
            int version = Integer.parseInt(matcher.group("majorVersion"));
            vm = new JVM(version);
            vm.setFullOutput(output);
        }
        return vm;
    }
}
