package com.mehmetkatr.project_easy_translate.entity;

import com.mehmetkatr.project_easy_translate.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.FetchType.LAZY;

/**
 * TaleMind Sozlugu — kendi sozluk veritabanimiz (Wiktionary/kaikki'den ETL).
 * Kelime basina 1 kayit. Anlamlar (homonym dahil) {@link DictionarySense} icinde.
 * Formlar (go->goes/going/went/gone, cat->cats) JSON olarak formsJson icinde.
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "dictionary_entry",
        indexes = {
                @Index(name = "idx_dict_entry_word", columnList = "word")
        },
        uniqueConstraints = @UniqueConstraint(
                name = "uq_dict_entry_word_lang", columnNames = {"word", "language_code"})
)
public class DictionaryEntry extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String word;

    @Column(name = "language_code", nullable = false, length = 10)
    @Builder.Default
    private String languageCode = "en";

    /** CEFR seviyesi: A1..C2 (bos olabilir). */
    @Column(length = 2)
    private String cefr;

    @Builder.Default
    @Column(nullable = false)
    private boolean idiom = false;

    /** Gemini enrich (siralama + eksik ornek uretimi) uygulandi mi — resumable isaret. */
    @Builder.Default
    @Column(name = "ai_enriched", nullable = false)
    private boolean aiEnriched = false;

    /** JSON dizisi: [{"form":"went","tags":["past"]}, ...]. Bos ise "[]". */
    @Lob
    @Column(name = "forms_json", columnDefinition = "TEXT")
    private String formsJson;

    @OneToMany(mappedBy = "entry", fetch = LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("senseOrder ASC")
    @Builder.Default
    private List<DictionarySense> senses = new ArrayList<>();
}
