package hr.performancemanagement.entities;


import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.sql.Date;

@Entity
public class RatingGuide {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private int rating;
    private String description;
    private Date date;

    public RatingGuide(long id, int rating, String description, Date date) {
        this.id = id;
        this.rating = rating;
        this.description = description;
        this.date = date;
    }

    public RatingGuide() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
