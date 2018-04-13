package flamegrapher.model;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class JVMTest {

    @Test
    public void version8() {
        String output = "73191:\n" + 
                "Java HotSpot(TM) 64-Bit Server VM version 25.131-b11\n" + 
                "JDK 8.0_131";
        JVM vm = JVM.fromVMVersion(output);
        assertThat(vm.getMajorVersion(), equalTo(8));
        
    }
    
    @Test
    public void version9() {
        String output = "14785:\n" + 
                "Java HotSpot(TM) 64-Bit Server VM version 9+181\n" + 
                "JDK 9.0.0";
        JVM vm = JVM.fromVMVersion(output);
        assertThat(vm.getMajorVersion(), equalTo(9));
    }

}
