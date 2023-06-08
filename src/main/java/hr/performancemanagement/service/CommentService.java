package hr.performancemanagement.service;

import hr.performancemanagement.entities.Comment;
import hr.performancemanagement.entities.Note;
import hr.performancemanagement.entities.SortByCommentId;
import hr.performancemanagement.repository.CommentRepository;
import hr.performancemanagement.repository.NoteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Service
public class CommentService {
    @Autowired
    CommentRepository commentRepository;

    public List<Comment> getCommentsByGoalId(long goalId){
        List<Comment> commentList = new ArrayList<>();
        commentRepository.findCommentsByGoal_Id(goalId).forEach(comment -> commentList.add(comment));
        return commentList;
    }

    public int countCommentsByGoalId(long goalId){
        try {
            int count = commentRepository.countCommentsByGoal_Id(goalId);
            return count;
        }catch (Exception e){
            return 0;
        }
    }


    public void saveComment(Comment comment) {
            commentRepository.save(comment);
    }

    @Transactional
    public void deleteComment(Comment comment){
        commentRepository.delete(comment);
    }
}
