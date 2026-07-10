import java.io.*;
import java.util.*;
import com.oracle.jrockit.jfr.parser.*;

/**
 * Extract thread stack samples from a JFR 0.9 recording file.
 * Uses the JDK 8 JRE's built-in jfr.jar (com.oracle.jrockit.jfr.parser.JFRParser).
 * Usage: java -cp jfr.jar;. JfrDump <file.jfr> [output.txt]
 */
public class JfrDump {
    public static void main(String[] args) throws Exception {
        String input = args[0];
        PrintStream out = args.length > 1 ? new PrintStream(new File(args[1])) : System.out;

        try (FileInputStream fis = new FileInputStream(input)) {
            JFRParser parser = JFRParser.parse(fis);
            var chunks = parser.chunks();
            out.println("Chunks: " + chunks.size());
            int eventCount = 0;
            for (var chunk : chunks) {
                for (var event : chunk.events()) {
                    if (eventCount++ > 1000) break;
                    out.println(event.getEventType().getName()
                        + " thread=" + event.getThread()
                        + " stack=" + (event.getStackTrace() != null ? event.getStackTrace().getFrames().length : 0));
                }
            }
            out.println("Total events: " + eventCount);
        }
    }
}
