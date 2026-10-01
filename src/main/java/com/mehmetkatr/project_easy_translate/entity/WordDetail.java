package com.mehmetkatr.project_easy_translate.entity;

import com.mehmetkatr.project_easy_translate.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "word_details",
        uniqueConstraints = @UniqueConstraint(name="uq_word_detail", columnNames={"word_id","target_language"})
)
public class WordDetail extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=LAZY)
    @JoinColumn(name="word_id", nullable=false)
    private Word word;

    private String targetLanguage;

    private String partOfSpeech;

    @Column(columnDefinition="TEXT")
    private String definition;

    // Definition translated into the target language (shown under the original).
    @Column(columnDefinition="TEXT")
    private String definitionTranslation;

    @Column(columnDefinition="LONGTEXT")
    private String synonyms;

    @Column(columnDefinition="LONGTEXT")
    private String examples;

    // Genel kaynak (geriye donuk uyumluluk) + bolum bazli kaynaklar.
    private String source;

    private String definitionSource;   // FREE_DICTIONARY | GEMINI
    private String examplesSource;     // FREE_DICTIONARY | TATOEBA | GEMINI
    private String translationSource;  // GOOGLE_TRANSLATE | GEMINI
}
