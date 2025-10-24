package hr.performancemanagement.sessions;

public class ConversationMessage {
    private String role; // "user" or "assistant"
    private String content;

    public ConversationMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public String getRole() { return role; }
    public String getContent() { return content; }
}

