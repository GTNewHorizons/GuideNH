package com.hfstudio.guidenh.guide.scene.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.message.ParameterizedMessage;
import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.config.ModConfig;
import com.hfstudio.guidenh.libs.unist.UnistNode;
import com.hfstudio.guidenh.libs.unist.UnistPosition;

import cpw.mods.fml.common.FMLLog;

public class GuideDebugLog {

    private static final ThreadLocal<LogContext> CONTEXT = new ThreadLocal<>();
    private static final ContextScope NOOP_CONTEXT_SCOPE = new ContextScope(null, false);
    private static volatile @Nullable DiagnosticCollector activeDiagnostics;

    protected GuideDebugLog() {}

    public static ContextScope pushContext(String language, String sourceLanguage, String sourcePath) {
        LogContext previous = CONTEXT.get();
        CONTEXT.set(new LogContext(language, sourceLanguage, sourcePath, null, activeDiagnostics));
        return new ContextScope(previous, true);
    }

    public static DiagnosticScope pushDiagnostics() {
        synchronized (GuideDebugLog.class) {
            if (activeDiagnostics != null) {
                throw new IllegalStateException("Another site export is already collecting diagnostics");
            }
            DiagnosticCollector current = new DiagnosticCollector();
            activeDiagnostics = current;
            return new DiagnosticScope(current);
        }
    }

    public static Runnable withCurrentContext(Runnable action) {
        LogContext captured = CONTEXT.get();
        return () -> {
            LogContext previous = CONTEXT.get();
            if (captured == null) {
                CONTEXT.remove();
            } else {
                CONTEXT.set(captured);
            }
            try {
                action.run();
            } finally {
                if (previous == null) {
                    CONTEXT.remove();
                } else {
                    CONTEXT.set(previous);
                }
            }
        };
    }

    public static ContextScope pushNode(@Nullable UnistNode node) {
        return pushPosition(node != null ? node.position() : null);
    }

    public static ContextScope pushPosition(@Nullable UnistPosition position) {
        LogContext current = CONTEXT.get();
        if (current == null || position == null) {
            return NOOP_CONTEXT_SCOPE;
        }
        CONTEXT.set(current.withPosition(position));
        return new ContextScope(current, true);
    }

    public static String position(@Nullable UnistNode node) {
        return position(node != null ? node.position() : null);
    }

    public static String position(@Nullable UnistPosition position) {
        if (position == null || position.start() == null) {
            return "unknown";
        }
        return position.start()
            .line() + ":"
            + position.start()
                .column();
    }

    public static boolean isEnabled() {
        return ModConfig.debug.enableDebugMode;
    }

    public static boolean isDebugEnabled() {
        return isEnabled();
    }

    public static void run(@Nullable Runnable action) {
        if (!isEnabled() || action == null) {
            return;
        }
        action.run();
    }

    public static void runOnce(@Nullable Set<String> onceKeys, @Nullable String key, @Nullable Runnable action) {
        if (!isEnabled() || onceKeys == null || key == null || key.isEmpty() || action == null) {
            return;
        }
        if (onceKeys.add(key)) {
            action.run();
        }
    }

    public static void error(@Nullable CharSequence message, Object... args) {
        if (message == null || message.length() <= 0) {
            return;
        }
        record(DiagnosticLevel.ERROR, message.toString(), args);
        FMLLog.getLogger()
            .error(withContext(message.toString()), args);
    }

    public static void warn(boolean enabled, @Nullable CharSequence message, Object... args) {
        if (!enabled) {
            return;
        }
        warnAlways(message, args);
    }

    public static void warn(@Nullable CharSequence message, Object... args) {
        if (!isEnabled()) {
            return;
        }
        warnAlways(message, args);
    }

    public static void warnAlways(@Nullable CharSequence message, Object... args) {
        if (message == null || message.length() <= 0) {
            return;
        }
        record(DiagnosticLevel.WARNING, message.toString(), args);
        FMLLog.getLogger()
            .warn(withContext(message.toString()), args);
    }

    public static void info(boolean enabled, @Nullable CharSequence message, Object... args) {
        if (!enabled) {
            return;
        }
        infoAlways(message, args);
    }

    public static void info(@Nullable CharSequence message, Object... args) {
        if (!isEnabled()) {
            return;
        }
        infoAlways(message, args);
    }

    public static void infoAlways(@Nullable CharSequence message, Object... args) {
        if (message == null || message.length() <= 0) {
            return;
        }
        FMLLog.getLogger()
            .info(withContext(message.toString()), args);
    }

    public static void debug(boolean enabled, @Nullable CharSequence message, Object... args) {
        if (!enabled) {
            return;
        }
        debugAlways(message, args);
    }

    public static void debug(@Nullable CharSequence message, Object... args) {
        if (!isEnabled()) {
            return;
        }
        debugAlways(message, args);
    }

    public static void debugAlways(@Nullable CharSequence message, Object... args) {
        if (message == null || message.length() <= 0) {
            return;
        }
        FMLLog.getLogger()
            .debug(withContext(message.toString()), args);
    }

    private static String withContext(String message) {
        LogContext context = CONTEXT.get();
        return context == null ? message : context.prefix() + message;
    }

    private static void record(DiagnosticLevel level, String message, Object... args) {
        LogContext context = CONTEXT.get();
        DiagnosticCollector collector = context != null ? context.collector() : activeDiagnostics;
        if (collector == null || activeDiagnostics != collector
            || (context == null && Thread.currentThread() != collector.owner)) {
            return;
        }
        ParameterizedMessage formatted = new ParameterizedMessage(message, args);
        String rendered = formatted.getFormattedMessage();
        if (formatted.getThrowable() != null) {
            rendered += " (cause: " + formatted.getThrowable() + ")";
        }
        collector.add(level, withContext(rendered));
    }

    private enum DiagnosticLevel {
        WARNING,
        ERROR
    }

    private static class DiagnosticCollector {

        private final Thread owner = Thread.currentThread();
        private final List<Diagnostic> entries = new ArrayList<>();
        private int warningCount;
        private int errorCount;

        private synchronized void add(DiagnosticLevel level, String message) {
            entries.add(new Diagnostic(level, message));
            if (level == DiagnosticLevel.ERROR) {
                errorCount++;
            } else {
                warningCount++;
            }
        }

        private synchronized List<Diagnostic> snapshot() {
            return List.copyOf(entries);
        }

        private synchronized DiagnosticSummary summary() {
            return new DiagnosticSummary(List.copyOf(entries), warningCount, errorCount);
        }

        private synchronized int warningCount() {
            return warningCount;
        }

        private synchronized int errorCount() {
            return errorCount;
        }
    }

    private record Diagnostic(DiagnosticLevel level, String message) {}

    private record DiagnosticSummary(List<Diagnostic> entries, int warnings, int errors) {}

    public static class DiagnosticScope implements AutoCloseable {

        private final DiagnosticCollector collector;
        private boolean closed;

        private DiagnosticScope(DiagnosticCollector collector) {
            this.collector = collector;
        }

        public int warningCount() {
            return collector.warningCount();
        }

        public int errorCount() {
            return collector.errorCount();
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            synchronized (GuideDebugLog.class) {
                if (activeDiagnostics == collector) {
                    activeDiagnostics = null;
                }
            }
            DiagnosticSummary summary = collector.summary();
            int warnings = summary.warnings();
            int errors = summary.errors();
            FMLLog.getLogger()
                .info("[GuideNH] [GuideSiteExportTask] Export diagnostics: warnings={}, errors={}", warnings, errors);
            int warningIndex = 0;
            int errorIndex = 0;
            for (Diagnostic entry : summary.entries()) {
                if (entry.level() == DiagnosticLevel.ERROR) {
                    FMLLog.getLogger()
                        .error(
                            "[GuideNH] [GuideSiteExportTask] Export error {}/{}: {}",
                            ++errorIndex,
                            errors,
                            entry.message());
                } else {
                    FMLLog.getLogger()
                        .warn(
                            "[GuideNH] [GuideSiteExportTask] Export warning {}/{}: {}",
                            ++warningIndex,
                            warnings,
                            entry.message());
                }
            }
        }
    }

    public static class ContextScope implements AutoCloseable {

        private final LogContext previous;
        private final boolean changed;

        private ContextScope(@Nullable LogContext previous, boolean changed) {
            this.previous = previous;
            this.changed = changed;
        }

        @Override
        public void close() {
            if (!changed) {
                return;
            }
            if (previous == null) {
                CONTEXT.remove();
            } else {
                CONTEXT.set(previous);
            }
        }
    }

    private record LogContext(String language, String sourceLanguage, String sourcePath,
        @Nullable UnistPosition position, @Nullable DiagnosticCollector collector) {

        private LogContext withPosition(@Nullable UnistPosition nextPosition) {
            return new LogContext(language, sourceLanguage, sourcePath, nextPosition, collector);
        }

        private String prefix() {
            StringBuilder result = new StringBuilder("[ExportSite context: language=").append(language)
                .append(", sourceLanguage=")
                .append(sourceLanguage == null || sourceLanguage.isEmpty() ? "unknown" : sourceLanguage);
            result.append(", source=")
                .append(sourcePath);
            if (position != null && position.start() != null) {
                result.append(", line=")
                    .append(
                        position.start()
                            .line())
                    .append(", column=")
                    .append(
                        position.start()
                            .column());
            } else {
                result.append(", line=unknown, column=unknown");
            }
            return result.append("] ")
                .toString();
        }
    }

}
