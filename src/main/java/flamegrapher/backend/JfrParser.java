package flamegrapher.backend;

import static com.oracle.jmc.flightrecorder.JfrAttributes.EVENT_STACKTRACE;

import java.io.File;
import java.io.IOException;
import java.util.Stack;

import com.oracle.jmc.common.IMCFrame;
import com.oracle.jmc.common.IMCMethod;
import com.oracle.jmc.common.IMCStackTrace;
import com.oracle.jmc.common.IMemberAccessor;
import com.oracle.jmc.common.item.IItem;
import com.oracle.jmc.common.item.IItemCollection;
import com.oracle.jmc.common.item.ItemFilters;
import com.oracle.jmc.common.unit.IQuantity;
import com.oracle.jmc.flightrecorder.CouldNotLoadRecordingException;
import com.oracle.jmc.flightrecorder.JfrAttributes;
import com.oracle.jmc.flightrecorder.JfrLoaderToolkit;

import flamegrapher.backend.JsonOutputWriter.StackFrame;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class JfrParser {

    private static final Logger logger = LoggerFactory.getLogger(JfrParser.class);

    public StackFrame toJson(File jfr, String... eventTypes) throws IOException, CouldNotLoadRecordingException {
        IItemCollection filtered = JfrLoaderToolkit.loadEvents(jfr)
                                                   .apply(ItemFilters.type(eventTypes));

        JsonOutputWriter writer = new JsonOutputWriter();
        filtered.forEach(events -> {
            IMemberAccessor<IMCStackTrace, IItem> accessor = events.getType()
                                                                   .getAccessor(EVENT_STACKTRACE.getKey());
            IMemberAccessor<IQuantity, IItem> startTime = events.getType()
                                                                .getAccessor(JfrAttributes.START_TIME.getKey());

            IMemberAccessor<IQuantity, IItem> endTime = events.getType()
                                                              .getAccessor(JfrAttributes.END_TIME.getKey());

            if (startTime == null && endTime == null) {
                return;
            }

            if (endTime == null) {
                endTime = startTime;
            }
            if (startTime == null) {
                // When the event has only end time, it means that it's only an
                // occurence, thus we set startTime = endTime.
                startTime = endTime;
            }
            for (IItem item : events) {
                Stack<String> stack = new Stack<>();
                IMCStackTrace stackTrace = accessor.getMember(item);
                if (startTime.getMember(item) == null || endTime.getMember(item) == null
                        || stackTrace.getFrames() == null) {
                    continue;
                }
                long start = startTime.getMember(item)
                                      .longValue();
                long end = endTime.getMember(item)
                                  .longValue();
                stackTrace.getFrames()
                          .forEach(frame -> {
                              stack.push(getFrameName(frame));
                          });
                long duration = end - start;
                writer.processEvent(start, end, duration, stack, 1L);
            }
        });

        return writer.getStackFrame();
    }

    private String getFrameName(IMCFrame frame) {
        // TODO: Make it a configuration parameter
        boolean ignoreLineNumbers = false;

        StringBuilder methodBuilder = new StringBuilder();
        IMCMethod method = frame.getMethod();
        methodBuilder.append(method.getType()
                                   .getFullName())
                     .append("#")
                     .append(method.getMethodName());

        if (!ignoreLineNumbers) {
            methodBuilder.append(":");
            methodBuilder.append(frame.getFrameLineNumber());
        }
        return methodBuilder.toString();
    }

}
