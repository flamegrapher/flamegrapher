package flamegrapher.model;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class JVMTest {

    @Test
    public void version8_Hotspot() {
        String output = "73191:\n" + 
                "Java HotSpot(TM) 64-Bit Server VM version 25.131-b11\n" + 
                "JDK 8.0_131";
        JVM vm = JVM.fromVMVersion(output);
        assertThat(vm.getMajorVersion(), equalTo(8));
        assertThat(vm.getType(), equalTo(JVMType.HOTSPOT));
    }
    
    @Test
    public void version9_Hotspot() {
        String output = "14785:\n" + 
                "Java HotSpot(TM) 64-Bit Server VM version 9+181\n" + 
                "JDK 9.0.0";
        JVM vm = JVM.fromVMVersion(output);
        assertThat(vm.getMajorVersion(), equalTo(9));
        assertThat(vm.getType(), equalTo(JVMType.HOTSPOT));
    }
    
    @Test
    public void version10_Hotspot() {
        String output = "87957:\n" + 
                "Java HotSpot(TM) 64-Bit Server VM version 10+46\n" + 
                "JDK 10.0.0";
        
        JVM vm = JVM.fromVMVersion(output);
        assertThat(vm.getMajorVersion(), equalTo(10));
        assertThat(vm.getType(), equalTo(JVMType.HOTSPOT));
    }

    @Test
    public void version8_OpenJDK() {

        String output = "49371:\n" + 
                "OpenJDK 64-Bit Server VM version 25.152-b12\n" + 
                "JDK 8.0_152\n" + 
                "";
        JVM vm = JVM.fromVMVersion(output);
        assertThat(vm.getMajorVersion(), equalTo(8));
        assertThat(vm.getType(), equalTo(JVMType.OPEN_JDK));        
    }

}
