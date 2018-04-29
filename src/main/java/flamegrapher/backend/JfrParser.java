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

public class JfrParser {

    public StackFrame toJson(File jfr, String eventType) throws IOException, CouldNotLoadRecordingException {
        IItemCollection filtered = JfrLoaderToolkit.loadEvents(jfr)
                                                   .apply(ItemFilters.type(eventType));
        JsonOutputWriter writer = new JsonOutputWriter();
        filtered.forEach(events -> {

            // IMCStackTrace stack =
            IMemberAccessor<IMCStackTrace, IItem> accessor = events.getType()
                                                                   .getAccessor(EVENT_STACKTRACE.getKey());
            IMemberAccessor<IQuantity, IItem> startTime = events.getType()
                                                                .getAccessor(JfrAttributes.START_TIME.getKey());

            IMemberAccessor<IQuantity, IItem> endTime = events.getType()
                                                              .getAccessor(JfrAttributes.END_TIME.getKey());
            System.out.println(events);

            if (endTime == null || startTime == null) {
                return;
            }
            for (IItem item : events) {
                Stack<String> stack = new Stack<>();
                IMCStackTrace stackTrace = accessor.getMember(item);
                if (startTime.getMember(item) == null || endTime.getMember(item) == null) {
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
        boolean ignoreLineNumbers = false;

        StringBuilder methodBuilder = new StringBuilder();
        IMCMethod method = frame.getMethod();
        methodBuilder.append(method.getType().getFullName())
                     .append("#")
                     .append(method.getMethodName());

        if (!ignoreLineNumbers) {
            methodBuilder.append(":");
            methodBuilder.append(frame.getFrameLineNumber());
        }
        return methodBuilder.toString();
    }

}
