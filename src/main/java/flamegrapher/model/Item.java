package flamegrapher.model;

import static java.util.regex.Pattern.compile;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class Item {

    private static final Logger logger = LoggerFactory.getLogger(Item.class);
    /** Matcher for process status w.r.t. JFR */
    private static final Pattern STATUS = compile(
            "(?<pid>[0-9]*):.*(?:[Rr]ecording[= ](?<recordingId>[0-9]*):? name|No available recordings|Java Flight Recorder not enabled)",
            Pattern.DOTALL);

    /** Matcher for recording start output */
    private static final Pattern START = compile("(?<pid>[0-9]*):.*Started recording (?<recordingId>[0-9]*)",
            Pattern.DOTALL);

    private final String pid;

    private final String name;

    @JsonProperty("recording")
    private String recordingNumber;

    private State state;

    public static final String NOT_APPLICABLE = "N/A";

    public Item(String pid, String name) {
        this.pid = pid;
        this.name = name;
    }

    public Item(String pid) {
        this(pid, "UNKNOWN");
    }

    public String getRecordingNumber() {
        return recordingNumber;
    }

    public void setRecordingNumber(String recordingNumber) {
        this.recordingNumber = recordingNumber;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public String getPid() {
        return pid;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return String.format(pid + "(" + name + ") " + state);
    }

    /**
     * <pre>
     *   8683:
     *   Recording: recording=1 name="Recording 1" (running)
     * </pre>
     * 
     * or
     * 
     * <pre>
     *   73191:
     *   No available recordings.
     * </pre>
     * 
     * or
     * 
     * <pre>
     *  90407:
     *  Java Flight Recorder not enabled.
     *
     *  Use VM.unlock_commercial_features to enable.
     * </pre>
     * 
     * @param status
     *            the output of the "jcmd [PID] JFR.check" command
     * @return an Item instance
     */
    public static Item fromStatus(String status) {

        Matcher matcher = STATUS.matcher(status);
        Item i = null;
        while (matcher.find()) {
            String pid = matcher.group("pid");
            i = new Item(pid);
            String recordingId = matcher.group("recordingId");
            if (recordingId != null) {
                i.setState(State.RECORDING);
                i.setRecordingNumber(recordingId);
            } else {
                i.setState(State.NOT_RECORDING);
                i.setRecordingNumber(Item.NOT_APPLICABLE);
            }
        }
        if (i == null) {
            logger.error("Didn't match status " + status);
        }
        return i;
    }

    public static Item fromStart(String output) {
        Matcher matcher = START.matcher(output);
        Item i = null;
        while (matcher.find()) {
            String pid = matcher.group("pid");
            String recordingId = matcher.group("recordingId");
            i = new Item(pid);
            i.setState(State.RECORDING);
            i.setRecordingNumber(recordingId);
        }
        return i;
    }

}
