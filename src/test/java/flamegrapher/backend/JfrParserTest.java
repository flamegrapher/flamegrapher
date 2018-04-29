package flamegrapher.backend;

import java.io.File;
import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;

import com.oracle.jmc.flightrecorder.CouldNotLoadRecordingException;
import com.oracle.jmc.flightrecorder.jdk.JdkTypeIDs;

import flamegrapher.backend.JsonOutputWriter.StackFrame;

public class JfrParserTest {
    @Ignore
    @Test
    public void test() throws IOException, CouldNotLoadRecordingException {
        File jfr = new File("/tmp/flamegrapher/64625.2.jfr");
        JfrParser parse = new JfrParser();
        StackFrame s = parse.toJson(jfr, JdkTypeIDs.EXECUTION_SAMPLE);
        System.out.println(s);
    }
}
