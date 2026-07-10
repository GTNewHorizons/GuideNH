import java.io.File;
import java.nio.file.*;
import java.util.*;
import org.openjdk.jmc.common.item.*;
import org.openjdk.jmc.flightrecorder.*;

/**
 * Parse a JFR recording using JMC's open-source parser library.
 * Usage: java -cp "jmc-plugins-dir/*" JfrParse <file.jfr>
 */
public class JfrParse {
    public static void main(String[] args) throws Exception {
        File file = new File(args[0]);
        System.out.println("Opening: " + file.getAbsolutePath());

        IItemCollection items = JfrLoaderToolkit.loadFile(file);
        Set<String> eventTypes = new LinkedHashSet<>();
        long totalEvents = 0;
        long samples = 0, clientSamples = 0;
        Map<String, Long> topMethods = new LinkedHashMap<>();

        for (IItemIterable segment : items) {
            for (IItem item : segment) {
                totalEvents++;
                String type = item.getType().getIdentifier();
                eventTypes.add(type);

                if (!type.contains("ExecutionSample")) continue;
                samples++;

                String thread = String.valueOf(
                    IItemAccessor.getAccessor(item, "eventThread").getMember("threadName"));
                if (!thread.contains("Client thread")) continue;
                clientSamples++;

                Object st = IItemAccessor.getAccessor(item, "stackTrace");
                if (st != null) {
                    String frame = st.toString();
                    if (frame.length() > 200) frame = frame.substring(0, 200);
                    topMethods.merge(frame, 1L, Long::sum);
                }
            }
        }

        System.out.println("\n=== SUMMARY ===");
        System.out.println("Total events: " + totalEvents);
        System.out.println("Event types (" + eventTypes.size() + "): " + eventTypes);
        System.out.println("ExecutionSample total: " + samples + ", Client thread: " + clientSamples);

        System.out.println("\n=== TOP Client Thread Stack Traces ===");
        topMethods.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(30)
            .forEach(e -> System.out.println(e.getValue() + "\t" + e.getKey()));
    }
}
