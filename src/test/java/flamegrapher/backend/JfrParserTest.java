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
    public void jdk8_mac() throws IOException, CouldNotLoadRecordingException {
        File jfr = getFile("58027.1.jfr");
        JfrParser parse = new JfrParser();
        StackFrame s = parse.toJson(jfr, JdkTypeIDs.EXECUTION_SAMPLE);
        assertNotNull(s);
        assertThat(s.getChildren()
                    .size(),
                equalTo(8));

    }

    @Test
    public void jdk10_mac() throws IOException, CouldNotLoadRecordingException {
        File jfr = getFile("78460.1.jfr");
        JfrParser parse = new JfrParser();
        StackFrame s = parse.toJson(jfr, JdkTypeIDs.EXECUTION_SAMPLE, "com.oracle.jdk.NativeMethodSample");
        assertNotNull(s);
        assertNotNull(s.getChildren());
        assertThat(s.getChildren()
                    .size(),
                equalTo(3));
    }

    private File getFile(String filename) {
        ClassLoader classLoader = getClass().getClassLoader();
        String pathname = classLoader.getResource(filename)
                                     .getFile();
        File jfr = new File(pathname);
        return jfr;
    }

}
