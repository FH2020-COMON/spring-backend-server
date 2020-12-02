package com.on.spring.entity.user;

import com.on.spring.entity.glass.Glass;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.List;

@Entity(name = "user")
@Getter @AllArgsConstructor @NoArgsConstructor @Builder
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "glass")
    private List<Glass> glasses;

    public User addGlass(Glass glass) {
        glasses.add(glass);
        return this;
    }
}
