package flamegrapher.backend;

import java.io.File;
import java.io.IOException;

import com.oracle.jmc.common.item.IItemCollection;
import com.oracle.jmc.common.item.ItemFilters;
import com.oracle.jmc.flightrecorder.CouldNotLoadRecordingException;
import com.oracle.jmc.flightrecorder.JfrLoaderToolkit;

public class JfrParser {
        
    public String toJson(File jfr, String eventType) throws IOException, CouldNotLoadRecordingException {
        IItemCollection events = JfrLoaderToolkit.loadEvents(jfr).apply(ItemFilters.type(eventType));
        events.forEach(event -> {
            //event.getType()
            // Will have to debug to understand how to use this :(
            // Process all stacks
        });
        
        // Choose to dump to JSON or SVG
        
        return null;
    }

}
