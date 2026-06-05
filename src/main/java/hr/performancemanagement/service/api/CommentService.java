package hr.performancemanagement.service.api;

import hr.performancemanagement.entities.Comment;
import java.util.List;

public interface CommentService {
    List<Comment> getCommentsByGoalId(long goalId);
    int countCommentsByGoalId(long goalId);
    void saveComment(Comment comment);
}
