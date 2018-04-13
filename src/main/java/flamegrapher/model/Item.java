package flamegrapher.model;

import static java.util.regex.Pattern.compile;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Item {

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

    /** Matcher for process status w.r.t. JFR */
    private static final Pattern STATUS = compile(
            "(?<pid>[0-9]*):.*(recording=(?<recordingId>[0-9]*) name|No available recordings|Java Flight Recorder not enabled)", Pattern.DOTALL);

    /**
     * @param status
     *            a String of the following format:
     * 
     *            <pre>
     *               8683:
     *               Recording: recording=1 name="Recording 1" (running)
     *            </pre>
     *            or
     *            <pre>
     *               73191:
     *               No available recordings.
     *            </pre>
     *            or 
     *            <pre>
     *               90407:
     *               Java Flight Recorder not enabled.
     *
     *               Use VM.unlock_commercial_features to enable.
     *            </pre>
     * 
     * @return an Item instance
     */
    public static Item fromStatus(String status) {

        Matcher matcher = STATUS.matcher(status);
        Item i = null;
        while (matcher.find()) {
            String pid = matcher.group("pid");
            i = new Item(pid, "UNKNOWN");
            String recordingId = matcher.group("recordingId");
            if (recordingId != null) {
                i.setState(State.RECORDING);
                i.setRecordingNumber(recordingId);
            } else {
                i.setState(State.NOT_RECORDING);
                i.setRecordingNumber(Item.NOT_APPLICABLE);
            }
        }
        return i;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((pid == null) ? 0 : pid.hashCode());
        result = prime * result + ((recordingNumber == null) ? 0 : recordingNumber.hashCode());
        result = prime * result + ((state == null) ? 0 : state.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Item other = (Item) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (pid == null) {
            if (other.pid != null)
                return false;
        } else if (!pid.equals(other.pid))
            return false;
        if (recordingNumber == null) {
            if (other.recordingNumber != null)
                return false;
        } else if (!recordingNumber.equals(other.recordingNumber))
            return false;
        if (state != other.state)
            return false;
        return true;
    }
    
    

}
