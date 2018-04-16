package flamegrapher.model;

public enum JVMType {

    HOTSPOT, OPEN_JDK, UNKNOWN;

    public static JVMType fromString(String t) {
       
        if (t.startsWith("Java HotSpot")) {
            return HOTSPOT;
        }

        if (t.startsWith("OpenJDK")) {
            return JVMType.OPEN_JDK;
        }

        return UNKNOWN;
    }
}
