package flamegrapher.model;

import static org.apache.commons.lang.StringUtils.capitalize;
import static org.apache.commons.lang.StringUtils.lowerCase;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

@JsonSerialize(using=ToStringSerializer.class)
public enum State {
    RECORDING, NOT_RECORDING;
    
    
    @Override
    public String toString() {
        return capitalize(lowerCase((super.toString().replace("_", " "))));
    }
}
