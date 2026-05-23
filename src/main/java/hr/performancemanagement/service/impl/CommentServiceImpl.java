package hr.performancemanagement.service.impl;

import org.springframework.stereotype.Service;
import hr.performancemanagement.service.api.*;

import hr.performancemanagement.entities.Comment;
import hr.performancemanagement.repository.CommentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;


@Service
public class CommentServiceImpl implements hr.performancemanagement.service.api.CommentService {
    @Autowired
    CommentRepository commentRepository;

    @Override
    public List<Comment> getCommentsByGoalId(long goalId){
        List<Comment> commentList = new ArrayList<>();
        commentRepository.findCommentsByGoalId(goalId).forEach(commentList::add);
        return commentList;
    }

    @Override
    public int countCommentsByGoalId(long goalId){
        try {
            return commentRepository.countCommentsByGoalId(goalId);
        }catch (Exception e){
            return 0;
        }
    }


    @Override
    public void saveComment(Comment comment) {
            commentRepository.save(comment);
    }

    @Transactional
    @Override
    public void deleteComment(Comment comment){
        commentRepository.delete(comment);
    }
}
