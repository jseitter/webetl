package io.webetl.websocket;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import io.webetl.runtime.RuntimeMetrics;
import java.util.Set;
import java.util.HashSet;

@ServerEndpoint("/ws/metrics/{sheetId}")
public class MetricsWebSocket {
    private static final Set<Session> sessions = new HashSet<>();

    @OnOpen
    public void onOpen(Session session) {
        sessions.add(session);
    }
    @OnMessage
    public void onMessage(Session session, String message) {
        // Handle client messages
    }

    public void sendMetrics(String sheetId, RuntimeMetrics metrics) {
        // Send metrics updates to connected clients
    }
} 