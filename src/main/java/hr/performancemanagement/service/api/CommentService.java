package hr.performancemanagement.service.api;

import hr.performancemanagement.entities.Comment;
import hr.performancemanagement.entities.Note;
import hr.performancemanagement.entities.SortByCommentId;
import hr.performancemanagement.repository.CommentRepository;
import hr.performancemanagement.repository.NoteRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public interface CommentService {
    List<Comment> getCommentsByGoalId(long goalId);
    int countCommentsByGoalId(long goalId);
    void saveComment(Comment comment);
    void deleteComment(Comment comment);
}
