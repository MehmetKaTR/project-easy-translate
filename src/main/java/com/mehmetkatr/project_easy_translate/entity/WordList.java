package com.mehmetkatr.project_easy_translate.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "wordlists",
        indexes = {
                @Index(name = "idx_wordlists_user_id", columnList = "user_id")
        }
)
public class WordList extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @Size(min = 1, max = 50)
    private String name;

    @NotNull
    @Size(min = 1, max = 7)
    @Column(name = "hex_color_code", length = 7, nullable = false)
    private String hexColorCode;

    @OneToMany(mappedBy = "wordList", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Word> words;

}
