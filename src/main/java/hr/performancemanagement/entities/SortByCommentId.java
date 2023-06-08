package hr.performancemanagement.entities;

import java.util.Comparator;

public class SortByCommentId implements Comparator<Comment> {
    // Used for sorting in ascending order of
    public int compare(Comment a, Comment b) {
        return (int) (a.getId() - b.getId());
    }
}
