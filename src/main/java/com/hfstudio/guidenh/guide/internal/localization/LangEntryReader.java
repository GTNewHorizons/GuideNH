package com.hfstudio.guidenh.guide.internal.localization;

import java.io.IOException;
import java.io.Reader;

class LangEntryReader {

    private final Reader reader;
    private final char[] chars = new char[8192];
    private final StringBuilder text = new StringBuilder(128);

    private int position;
    private int limit;

    private String key;
    private boolean valuePending;

    LangEntryReader(Reader reader) {
        this.reader = reader;
    }

    public boolean next() throws IOException {
        if (valuePending) {
            skipValue();
        }

        key = null;

        lines: while (ensureAvailable()) {
            text.setLength(0);

            if (chars[position] == '\uFEFF') {
                position++;
                if (!ensureAvailable()) {
                    return false;
                }
            }

            if (chars[position] == '#') {
                skipToNextLine();
                continue;
            }

            while (true) {
                if (!ensureAvailable()) {
                    return false;
                }

                int start = position;

                while (position < limit) {
                    char c = chars[position];

                    if (c == '=') {
                        int length = position - start;

                        if (text.isEmpty() && length == 0) {
                            position++;
                            skipToNextLine();
                            continue lines;
                        }

                        if (text.isEmpty()) {
                            key = new String(chars, start, length);
                        } else {
                            text.append(chars, start, length);
                            key = text.toString();
                        }

                        position++;
                        valuePending = true;
                        return true;
                    }

                    if (c == '\r' || c == '\n') {
                        position++;
                        consumeLineEnd(c);
                        continue lines;
                    }

                    position++;
                }

                // Only needed when a key happens to cross a reader-buffer boundary
                text.append(chars, start, position - start);
            }
        }

        return false;
    }

    public String key() {
        if (key == null) {
            throw new IllegalStateException("No current language entry");
        }
        return key;
    }

    public String readValue() throws IOException {
        if (!valuePending) {
            throw new IllegalStateException("Current language entry has no unread value");
        }

        text.setLength(0);

        while (ensureAvailable()) {
            int start = position;

            while (position < limit) {
                char c = chars[position];
                if (c == '\r' || c == '\n') {
                    break;
                }
                position++;
            }

            if (position > start) {
                text.append(chars, start, position - start);
            }

            if (position < limit) {
                int lineEnd = chars[position++];
                consumeLineEnd(lineEnd);

                valuePending = false;
                return text.toString();
            }
        }

        valuePending = false;
        return text.toString();
    }

    private void skipValue() throws IOException {
        skipToNextLine();
        valuePending = false;
    }

    private void skipToNextLine() throws IOException {
        while (ensureAvailable()) {
            while (position < limit) {
                char c = chars[position++];

                if (c == '\n') {
                    return;
                }

                if (c == '\r') {
                    consumeLineEnd(c);
                    return;
                }
            }
        }
    }

    private void consumeLineEnd(int c) throws IOException {
        if (c != '\r') {
            return;
        }

        if (ensureAvailable() && chars[position] == '\n') {
            position++;
        }
    }

    private boolean ensureAvailable() throws IOException {
        if (position < limit) {
            return true;
        }

        limit = reader.read(chars);
        position = 0;
        return limit != -1;
    }
}
