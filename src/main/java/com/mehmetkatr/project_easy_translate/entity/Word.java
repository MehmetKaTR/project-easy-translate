package com.mehmetkatr.project_easy_translate.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "words",
        indexes = {
                @Index(name = "idx_words_wordlist_id", columnList = "wordlist_id"),
                @Index(name = "idx_words_starred", columnList = "starred")
        }
)
public class Word extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Size(min = 1, max = 50)
    private String word;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wordlist_id")
    @JsonIgnore
    private WordList wordList;

    @NotNull
    private String languageCode;

    @NotNull
    @Size(min = 1, max = 50)
    private String translated;

    @Builder.Default
    @Column(nullable = false)
    private boolean starred = false;
}
