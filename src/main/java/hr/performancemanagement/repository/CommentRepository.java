package hr.performancemanagement.repository;

import hr.performancemanagement.entities.Comment;
import hr.performancemanagement.entities.Target;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    @Query(value = "SELECT '*' FROM Comment WHERE target = :target ORDER BY id")
    List<Comment> findCommentsByTarget(Target target);

    @Query(value = "SELECT count(Comment) FROM Comment WHERE target = :target")
    int countCommentsByTarget(@Param("target") Target target);

}
