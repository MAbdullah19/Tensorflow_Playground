package com.playground.api;

import com.playground.exceptions.SessionNotFoundException;

import java.security.SecureRandom;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * In-memory store for active {@link Session}s keyed by id.
 *
 * <p>Each session has its own lock so the HTTP layer can safely process
 * concurrent requests (different sessions stay independent, while requests
 * inside the same session are serialised).
 */
public final class SessionManager {

    private static final long IDLE_TIMEOUT_MS = 30L * 60L * 1000L; // 30 min
    private static final int  MAX_SESSIONS    = 256;

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final Map<String, Long> lastSeen = new ConcurrentHashMap<>();
    private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    private final SecureRandom secureRandom = new SecureRandom();

    public Session create() {
        evictIfNeeded();
        String id = newId();
        long seed = secureRandom.nextLong();
        Session session = new Session(id, seed);
        sessions.put(id, session);
        lastSeen.put(id, System.currentTimeMillis());
        locks.put(id, new ReentrantLock());
        return session;
    }

    public Session get(String id) {
        Session s = sessions.get(id);
        if (s != null) lastSeen.put(id, System.currentTimeMillis());
        return s;
    }

    /** Like {@link #get(String)} but throws if the session does not exist. */
    public Session require(String id) {
        Session s = get(id);
        if (s == null) throw new SessionNotFoundException(id);
        return s;
    }

    public ReentrantLock lockFor(String id) {
        return locks.get(id);
    }

    public ReentrantLock requireLockFor(String id) {
        ReentrantLock lock = locks.get(id);
        if (lock == null) throw new SessionNotFoundException(id);
        return lock;
    }

    public boolean delete(String id) {
        boolean removed = sessions.remove(id) != null;
        lastSeen.remove(id);
        locks.remove(id);
        return removed;
    }

    public int size() { return sessions.size(); }

    private String newId() {
        byte[] bytes = new byte[12];
        secureRandom.nextBytes(bytes);
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private void evictIfNeeded() {
        long now = System.currentTimeMillis();
        // Always sweep stale sessions to keep the map small.
        for (Iterator<Map.Entry<String, Long>> it = lastSeen.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, Long> e = it.next();
            if (now - e.getValue() > IDLE_TIMEOUT_MS) {
                String id = e.getKey();
                it.remove();
                sessions.remove(id);
                locks.remove(id);
            }
        }
        if (sessions.size() < MAX_SESSIONS) return;
        // Evict the oldest until we're back under the cap.
        while (sessions.size() >= MAX_SESSIONS) {
            String oldestId = null;
            long oldest = Long.MAX_VALUE;
            for (Map.Entry<String, Long> e : lastSeen.entrySet()) {
                if (e.getValue() < oldest) { oldest = e.getValue(); oldestId = e.getKey(); }
            }
            if (oldestId == null) break;
            sessions.remove(oldestId);
            lastSeen.remove(oldestId);
            locks.remove(oldestId);
        }
    }
}
