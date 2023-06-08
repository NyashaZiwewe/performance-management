package hr.performancemanagement.repository;

import hr.performancemanagement.entities.Comment;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    @Query(value = "SELECT '*' FROM Comment WHERE goal = :goalId ORDER BY id")
    List<Comment> findCommentsByGoal_Id(long goalId);

    @Query(value = "SELECT count(Comment) FROM Comment WHERE goal = :goalId")
    int countCommentsByGoal_Id(@Param("goalId") long goalId);

}
