package hr.performancemanagement.sessions;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
@Service
public class SessionManager {

    private static final ConcurrentHashMap<String, UserSession> sessions = new ConcurrentHashMap<>();

    public static UserSession getSession(String userPhone) {
        return sessions.computeIfAbsent(userPhone, k -> new UserSession());
    }

    public static void removeSession(String userPhone) {
        sessions.remove(userPhone);
    }

    public static void cleanupExpiredSessions() {
        sessions.entrySet().removeIf(entry ->
                entry.getValue().getLastUpdated().plusMinutes(30).isBefore(LocalDateTime.now())
        );
    }
}

