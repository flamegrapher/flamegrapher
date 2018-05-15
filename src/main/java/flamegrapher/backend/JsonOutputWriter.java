/*
* Copyright 2016 M. Isuru Tharanga Chrishantha Perera
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package flamegrapher.backend;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import io.vertx.core.json.Json;

/**
 * This is a simplified version of the JsonOutputWriter found on
 * https://github.com/chrishantha/jfr-flame-graph Create JSON output to be used
 * with d3-flame-graph. https://github.com/spiermar/d3-flame-graph
 * <p>
 * This is similar to https://github.com/spiermar/node-stack-convert
 * </p>
 */
public class JsonOutputWriter {

    /**
     * The bottom of the stack must be "root"
     */
    private static final String ROOT = "root";

    /**
     * The data model for json
     */
    private StackFrame profile = new StackFrame(ROOT);

    public static class StackFrame {

        String name;
        Long value = 0L;
        List<StackFrame> children = null;
        transient Map<String, StackFrame> childrenMap = new HashMap<>();

        public StackFrame(String name) {
            this.name = name;
        }

        public StackFrame addFrame(String frameName, Long size) {
            if (children == null) {
                children = new ArrayList<>();
            }
            StackFrame frame = childrenMap.get(frameName);
            if (frame == null) {
                frame = new StackFrame(frameName);
                childrenMap.put(frameName, frame);
                children.add(frame);
            }
            frame.value += size;
            return frame;
        }

        public String getName() {
            return name;
        }

        public Long getValue() {
            return value;
        }

        public List<StackFrame> getChildren() {
            return children;
        }
    }

    public void processEvent(Stack<String> stack, Long size) {
        StackFrame frame = profile;
        frame.value += size;

        while (!stack.empty()) {
            frame = frame.addFrame(stack.pop(), size);
        }
    }

    public StackFrame getStackFrame() {
        return this.profile;
    }

    public void writeOutput(BufferedWriter bufferedWriter) throws IOException {
        String json = Json.encode(this.profile);
        bufferedWriter.write(json);
    }
}
