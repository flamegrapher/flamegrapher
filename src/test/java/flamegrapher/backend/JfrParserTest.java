package flamegrapher.backend;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;

import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;

import org.junit.Test;

import flamegrapher.backend.JsonOutputWriter.StackFrame;

public class JfrParserTest {
    @Test
    public void jdk8() throws IOException, CouldNotLoadRecordingException {

        StackFrame s = parse("58027.1.jfr", JdkTypeIDs.EXECUTION_SAMPLE);
        assertNotNull(s);
        assertThat(s.getChildren()
                    .size(),
                equalTo(8));
    }

    @Test
    public void jdk10() throws IOException, CouldNotLoadRecordingException {
        StackFrame s = parse("78460.1.jfr", JdkTypeIDs.EXECUTION_SAMPLE, JavaFlightRecorder.NATIVE_METHOD_SAMPLE);
        assertNotNull(s);
        assertNotNull(s.getChildren());
        assertThat(s.getChildren()
                    .size(),
                equalTo(3));
    }

    @Test
    public void jdk10Locks() throws IOException, CouldNotLoadRecordingException {
        StackFrame s = parse("7856.1.jfr", JdkTypeIDs.MONITOR_ENTER);
        assertNotNull(s);
        assertNotNull(s.getChildren());
        assertThat(s.getChildren()
                    .size(),
                equalTo(4));
    }

    @Test
    public void jdk8Allocations() throws IOException, CouldNotLoadRecordingException {
        StackFrame s = parse("7789.1.jfr", JdkTypeIDs.ALLOC_INSIDE_TLAB, JdkTypeIDs.ALLOC_OUTSIDE_TLAB);
        assertNotNull(s);
        assertNotNull(s.getChildren());
        assertThat(s.getChildren()
                    .size(),
                equalTo(6));
        assertThat(s.getValue(), equalTo(917496L));
    }

    private StackFrame parse(String filename, String... eventTypes) throws IOException, CouldNotLoadRecordingException {
        File jfr = getFile(filename);
        JfrParser parse = new JfrParser();
        StackFrame s = parse.toJson(jfr, eventTypes);
        return s;
    }

    private File getFile(String filename) {
        ClassLoader classLoader = getClass().getClassLoader();
        String pathname = classLoader.getResource(filename)
                                     .getFile();
        File jfr = new File(pathname);
        return jfr;
    }
}
