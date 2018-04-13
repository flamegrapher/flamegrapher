package flamegrapher.model;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ProcessesTest {

    @Test
    public void processFromString() {
        String output = "89585 /Users/lgomes/.vscode/extensions/redhat.java-0.17.0/server/plugins/org.eclipse.equinox.launcher_1.4.0.v20161219-1356.jar -configuration /Users/lgomes/.vscode/extensions/redhat.java-0.17.0/server/config_mac -data /Users/lgomes/Library/Application Support/Code/User/workspaceStorage/a6ad3ec3747972b8b4052f02e032d34e/redhat.java/jdt_ws\n"
                + "58949 \n" + "9045 flamegrapher.MainVerticle\n"
                + "31534 org.netbeans.Main --cachedir /Users/lgomes/Library/Caches/VisualVM/8u131 --userdir /Users/lgomes/Library/Application Support/VisualVM/8u131 --branding visualvm\n"
                + "44255 /Users/lgomes/.vscode/extensions/redhat.java-0.17.0/server/plugins/org.eclipse.equinox.launcher_1.4.0.v20161219-1356.jar -configuration /Users/lgomes/.vscode/extensions/redhat.java-0.17.0/server/config_mac -data /Users/lgomes/Library/Application Support/Code/User/workspaceStorage/5ecf9f491a4557d830646f5fec674095/redhat.java/jdt_ws\n"
                + "9055 sun.tools.jcmd.JCmd";
        
        Processes p = Processes.fromString(output);
        assertThat(p.items().size(), equalTo(6));
        assertTrue(p.items().containsKey("58949"));
    }
    
    @Test
    public void itemFromRecordingString() {
        String output = "73191:\n" + 
                "Recording: recording=2 name=\"Recording 2\" (running)";
        
        Item i = Item.fromStatus(output);
        assertThat(i.getPid(), equalTo("73191"));
        assertThat(i.getRecordingNumber(), equalTo("2"));
    }
    
    @Test
    public void itemFromNotRecordingString() {
        String output = "73191:\n" + 
                "No available recordings.";
        
        Item i = Item.fromStatus(output);
        assertThat(i.getPid(), equalTo("73191"));
        assertThat(i.getRecordingNumber(), equalTo("N/A"));
    }
    
    @Test
    public void itemFromNotEnabledString() {
        String output = "90407:\n" + 
                "Java Flight Recorder not enabled.\n" + 
                "\n" + 
                "Use VM.unlock_commercial_features to enable.";
        
        Item i = Item.fromStatus(output);
        assertThat(i.getPid(), equalTo("90407"));
        assertThat(i.getRecordingNumber(), equalTo("N/A"));
    }
    
    @Test
    public void checkStateFriendlyName() {
        assertThat(State.NOT_RECORDING.toString(), equalTo("Not recording"));
    }
    
    @Test
    public void startStatus() {
        String output = "73191:\n" + 
                "Stopped recording 4.\n" + 
                "NCELRND0380:flamegrapher lgomes$ jcmd 73191 JFR.start\n" + 
                "73191:\n" + 
                "Started recording 5. No limit (duration/maxsize/maxage) in use.\n" + 
                "\n" + 
                "Use JFR.dump recording=5 filename=FILEPATH to copy recording data to file.";
        Item i = Item.fromStart(output);
        assertThat(i.getPid(), equalTo("73191"));
        assertThat(i.getState(), equalTo(State.RECORDING));
        assertThat(i.getRecordingNumber(), equalTo("5"));        
    }
    

}
