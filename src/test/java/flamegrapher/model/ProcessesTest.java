package flamegrapher.model;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ProcessesTest {

    @Test
    public void fromString() {
        String output = "89585 /Users/lgomes/.vscode/extensions/redhat.java-0.17.0/server/plugins/org.eclipse.equinox.launcher_1.4.0.v20161219-1356.jar -configuration /Users/lgomes/.vscode/extensions/redhat.java-0.17.0/server/config_mac -data /Users/lgomes/Library/Application Support/Code/User/workspaceStorage/a6ad3ec3747972b8b4052f02e032d34e/redhat.java/jdt_ws\n"
                + "58949 \n" + "9045 flamegrapher.MainVerticle\n"
                + "31534 org.netbeans.Main --cachedir /Users/lgomes/Library/Caches/VisualVM/8u131 --userdir /Users/lgomes/Library/Application Support/VisualVM/8u131 --branding visualvm\n"
                + "44255 /Users/lgomes/.vscode/extensions/redhat.java-0.17.0/server/plugins/org.eclipse.equinox.launcher_1.4.0.v20161219-1356.jar -configuration /Users/lgomes/.vscode/extensions/redhat.java-0.17.0/server/config_mac -data /Users/lgomes/Library/Application Support/Code/User/workspaceStorage/5ecf9f491a4557d830646f5fec674095/redhat.java/jdt_ws\n"
                + "9055 sun.tools.jcmd.JCmd";
        
        Processes p = Processes.fromString(output);
        assertThat(p.pidName.size(), equalTo(6));
        assertTrue(p.pidName.containsKey("58949"));
    }

}
