package flamegrapher.model;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.Test;

public class ProcessesTest {

    @Test
    public void processFromString() {
        String []output = new String[] {
                "89585 /Users/lgomes/.vscode/extensions/redhat.java-0.17.0/server/plugins/org.eclipse.equinox.launcher_1.4.0.v20161219-1356.jar -configuration /Users/lgomes/.vscode/extensions/redhat.java-0.17.0/server/config_mac -data /Users/lgomes/Library/Application Support/Code/User/workspaceStorage/a6ad3ec3747972b8b4052f02e032d34e/redhat.java/jdt_ws",
                "58949 ", "9045 flamegrapher.MainVerticle",
                "31534 org.netbeans.Main --cachedir /Users/lgomes/Library/Caches/VisualVM/8u131 --userdir /Users/lgomes/Library/Application Support/VisualVM/8u131 --branding visualvm",
                "44255 /Users/lgomes/.vscode/extensions/redhat.java-0.17.0/server/plugins/org.eclipse.equinox.launcher_1.4.0.v20161219-1356.jar -configuration /Users/lgomes/.vscode/extensions/redhat.java-0.17.0/server/config_mac -data /Users/lgomes/Library/Application Support/Code/User/workspaceStorage/5ecf9f491a4557d830646f5fec674095/redhat.java/jdt_ws",
                "9055 sun.tools.jcmd.JCmd"
                };
        
        Processes p = Processes.fromString(arrayToString(output));
        assertThat(p.items().size(), equalTo(6));
        assertTrue(p.items().containsKey("58949"));
    }

    private String arrayToString(String[] output) {
        return Arrays.asList(output).stream().collect(Collectors.joining(System.lineSeparator()));
    }
    
    @Test
    public void itemFromRecordingString() {
        String []output = new String[] {
                "73191:", 
                "Recording: recording=2 name=\"Recording 2\" (running)"
        };
        
        Item i = Item.fromStatus(arrayToString(output));
        assertThat(i.getPid(), equalTo("73191"));
        assertThat(i.getRecordingNumber(), equalTo("2"));
    }
    
    @Test
    public void itemFromNotRecordingString() {
        String []output = new String[] {
                "73191:", 
                "No available recordings."
        };
        
        Item i = Item.fromStatus(arrayToString(output));
        assertThat(i.getPid(), equalTo("73191"));
        assertThat(i.getRecordingNumber(), equalTo("N/A"));
    }
    
    @Test
    public void itemFromNotEnabledString() {
        String []output = new String[] {
                "90407:", 
                "Java Flight Recorder not enabled.",
                "",
                "Use VM.unlock_commercial_features to enable."
        };
        
        Item i = Item.fromStatus(arrayToString(output));
        assertThat(i.getPid(), equalTo("90407"));
        assertThat(i.getRecordingNumber(), equalTo("N/A"));
    }
    
    @Test
    public void checkStateFriendlyName() {
        assertThat(State.NOT_RECORDING.toString(), equalTo("Not recording"));
    }
    
    @Test
    public void startStatus() {
        String []output = new String[] {
                "73191:",
                "Started recording 5. No limit (duration/maxsize/maxage) in use.", 
                "",
                "Use JFR.dump recording=5 filename=FILEPATH to copy recording data to file."
        };
        Item i = Item.fromStart(arrayToString(output));
        assertThat(i.getPid(), equalTo("73191"));
        assertThat(i.getState(), equalTo(State.RECORDING));
        assertThat(i.getRecordingNumber(), equalTo("5"));        
    }
    

}
