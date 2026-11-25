package com.hfstudio.guidenh.bridge.transport;

import java.io.FilterInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class BridgeSocketInputStream extends FilterInputStream {

    private final Socket socket;
    private final long authenticationDeadline;
    private boolean authenticated;
    private int timeoutMillis;

    public BridgeSocketInputStream(Socket socket, int authenticationTimeoutMillis) throws IOException {
        super(socket.getInputStream());
        this.socket = socket;
        authenticationDeadline = System.nanoTime() + authenticationTimeoutMillis * 1_000_000L;
    }

    public boolean authenticate() {
        if (authenticated) return true;
        if (System.nanoTime() >= authenticationDeadline) return false;
        authenticated = true;
        return true;
    }

    private void prepareRead() throws IOException {
        long remaining = authenticationDeadline - System.nanoTime();
        if (!authenticated && remaining <= 0) throw new SocketTimeoutException("Bridge authentication timed out");
        int timeout = authenticated ? 30_000
            : (int) Math.max(1L, Math.min(30_000L, (remaining + 999_999L) / 1_000_000L));
        if (timeout != timeoutMillis) {
            socket.setSoTimeout(timeout);
            timeoutMillis = timeout;
        }
    }

    @Override
    public int read() throws IOException {
        prepareRead();
        return in.read();
    }

    @Override
    public int read(byte[] bytes, int offset, int length) throws IOException {
        prepareRead();
        return in.read(bytes, offset, length);
    }
}
