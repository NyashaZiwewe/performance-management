package hr.performancemanagement.sessions;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UserSession {
    private List<ConversationMessage> messages = new ArrayList<>();
    private LocalDateTime lastUpdated = LocalDateTime.now();

    public void addUserMessage(String text) {
        messages.add(new ConversationMessage("user", text));
        updateTime();
    }

    public void addBotMessage(String text) {
        messages.add(new ConversationMessage("assistant", text));
        updateTime();
    }

    public List<ConversationMessage> getMessages() {
        return messages;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    private void updateTime() {
        this.lastUpdated = LocalDateTime.now();
    }

    public void trimHistory(int maxMessages) {
        if (messages.size() > maxMessages) {
            messages = messages.subList(messages.size() - maxMessages, messages.size());
        }
    }
}
