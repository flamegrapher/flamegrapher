package flamegrapher.model;

import static flamegrapher.model.JVMType.fromString;
import static java.lang.Integer.parseInt;
import static java.util.regex.Pattern.compile;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JVM {

    private static final Pattern VM_VERSION = compile("^(?<pid>[0-9]*):(?<type>[a-zA-Z\\s]*).*JDK (?<majorVersion>[0-9]*)\\.", Pattern.DOTALL);

    private final int pid, majorVersion;

    private final JVMType type;

    private String fullOutput;

    public JVM(int pid, int majorVersion, JVMType type) {
        this.pid = pid;
        this.majorVersion = majorVersion;
        this.type = type;
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

    public JVMType getType() {
        return type;
    }
    
    public int getPid() {
        return pid;
    }

    public static JVM fromVMVersion(String output) {
        Matcher matcher = VM_VERSION.matcher(output);
        JVM vm = null;
        if (matcher.find()) {
            int pid = parseInt(matcher.group("pid"));
            int version = parseInt(matcher.group("majorVersion"));
            JVMType type = fromString(matcher.group("type").trim());
            vm = new JVM(pid, version, type);
            vm.setFullOutput(output);
        }
        return vm;
    }
}
