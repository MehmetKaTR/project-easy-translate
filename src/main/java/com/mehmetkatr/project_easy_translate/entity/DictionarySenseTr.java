package com.mehmetkatr.project_easy_translate.entity;

import com.mehmetkatr.project_easy_translate.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import static jakarta.persistence.FetchType.LAZY;

/**
 * Bir anlamin belirli bir hedef dildeki cevirisi (lazy doldurulur, bir kez cevrilir).
 * (sense_id, target_lang) tekildir.
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "dictionary_sense_tr",
        indexes = {
                @Index(name = "idx_dict_sense_tr_sense", columnList = "sense_id")
        },
        uniqueConstraints = @UniqueConstraint(
                name = "uq_dict_sense_tr", columnNames = {"sense_id", "target_lang"})
)
public class DictionarySenseTr extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "sense_id", nullable = false)
    private DictionarySense sense;

    @Column(name = "target_lang", nullable = false, length = 10)
    private String targetLang;

    /** Birincil ceviri (or. "yarasa"). */
    @Column(name = "primary_tr", length = 255)
    private String primaryTr;

    @Lob
    @Column(name = "definition_tr", columnDefinition = "TEXT")
    private String definitionTr;

    /** JSON dizisi (cevrilmis ornek cumleler). */
    @Lob
    @Column(name = "examples_tr_json", columnDefinition = "TEXT")
    private String examplesTrJson;

    /** JSON dizisi (alternatif ceviriler, or. ["sopa","beyzbol sopasi"]). */
    @Lob
    @Column(name = "variants_json", columnDefinition = "TEXT")
    private String variantsJson;

    /** Ceviri motoru: GEMINI | GOOGLE_TRANSLATE | SEED (kaikki). */
    @Column(length = 20)
    private String engine;

    /** Gemini'nin sectigi "en yaygin" siralamasindaki yeri (0=en yaygin). null=secilmedi/gosterilmez. */
    @Column(name = "display_rank")
    private Integer displayRank;
}
