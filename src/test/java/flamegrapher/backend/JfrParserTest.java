package flamegrapher.backend;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import com.oracle.jmc.flightrecorder.CouldNotLoadRecordingException;
import com.oracle.jmc.flightrecorder.jdk.JdkTypeIDs;

import flamegrapher.backend.JsonOutputWriter.StackFrame;

public class JfrParserTest {
    @Test
    public void test() throws IOException, CouldNotLoadRecordingException {
        ClassLoader classLoader = getClass().getClassLoader();
        String pathname = classLoader.getResource("58027.1.jfr").getFile();
        File jfr = new File(pathname);
        JfrParser parse = new JfrParser();
        StackFrame s = parse.toJson(jfr, JdkTypeIDs.EXECUTION_SAMPLE);
        assertNotNull(s);
        assertThat(s.getChildren().size(), equalTo(8));
        
    }
}
