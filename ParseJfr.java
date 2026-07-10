import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import jdk.jfr.*;
import jdk.jfr.consumer.*;

public class ParseJfr {
    public static void main(String[] args) throws Exception {
        Path path = Paths.get(args[0]);
        System.out.println("Reading: " + path);
        try (RecordingFile rf = new RecordingFile(path)) {
            int count = 0;
            while (rf.hasMoreEvents()) {
                RecordedEvent event = rf.readEvent();
                String thread = event.getThread() != null ? event.getThread().getJavaName() : "null";
                if (count < 10) {
                    System.out.println("[" + thread + "] " + event.getEventType().getName()
                        + " @ " + event.getStartTime());
                }
                if (++count > 100) break;
            }
            System.out.println("Total events scanned: " + count);
            System.out.println("Available event types: " + rf.getEventTypes().stream()
                .map(t -> t.getName() + "(" + t.getId() + ")")
                .toList());
        } catch (Exception e) {
            System.out.println("FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
