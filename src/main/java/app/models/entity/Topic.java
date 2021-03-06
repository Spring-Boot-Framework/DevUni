package app.models.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.List;


@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "topics")
public class Topic extends BaseEntity{

    @Column(nullable = false,unique = true,updatable = false)
    private String name;


    @OneToMany(mappedBy = "topic")
    private List<Course> courses;

    public Topic(String name) {
        this.name = name;
    }
}

