package com.mehmetkatr.project_easy_translate.entity;

import com.mehmetkatr.project_easy_translate.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.LinkedHashSet;
import java.util.Set;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "user_words",
        indexes = {
                @Index(name = "idx_user_words_user_id", columnList = "user_id")
        },
        uniqueConstraints=@UniqueConstraint(name="uq_user_word", columnNames={"user_id","word_id"})
)
public class UserWord  extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=LAZY)
    @JoinColumn(name="user_id", nullable=false)
    private User user;

    @ManyToOne(fetch=LAZY)
    @JoinColumn(name="word_id", nullable=false)
    private Word word;

    private String translated;

    private boolean starred;

    @ManyToMany
    @JoinTable(name="user_word_wordlist",
            joinColumns=@JoinColumn(name="user_word_id"),
            inverseJoinColumns=@JoinColumn(name="word_list_id"))
    @Builder.Default
    private Set<WordList> wordLists = new LinkedHashSet<>();
}
