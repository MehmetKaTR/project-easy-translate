package com.mehmetkatr.project_easy_translate.entity;

import com.mehmetkatr.project_easy_translate.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.FetchType.LAZY;

/**
 * Bir sozluk kelimesinin tek anlami (homonym/cok-anlamlilik burada).
 * definition/examples/synonyms kaynak dilde (EN); ceviriler {@link DictionarySenseTr}.
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "dictionary_sense",
        indexes = {
                @Index(name = "idx_dict_sense_entry", columnList = "entry_id")
        }
)
public class DictionarySense extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "entry_id", nullable = false)
    private DictionaryEntry entry;

    /** Kart icindeki siralama (0..n). */
    @Column(name = "sense_order", nullable = false)
    @Builder.Default
    private int senseOrder = 0;

    @Column(length = 20)
    private String pos;

    @Builder.Default
    @Column(nullable = false)
    private boolean idiom = false;

    /** Gemini enrich: yayginlik sirasi (0=en yaygin). null=secilmedi/kartta gosterilmez. */
    @Column(name = "common_rank")
    private Integer commonRank;

    /** Ornekler Gemini ile uretildi mi (kaynak etiketi icin). */
    @Builder.Default
    @Column(name = "example_ai", nullable = false)
    private boolean exampleAi = false;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String definition;

    /** JSON dizisi (EN ornek cumleler). Bos ise "[]". */
    @Lob
    @Column(name = "examples_json", columnDefinition = "TEXT")
    private String examplesJson;

    /** JSON dizisi (EN es anlamlilar). Bos ise "[]". */
    @Lob
    @Column(name = "synonyms_json", columnDefinition = "TEXT")
    private String synonymsJson;

    @OneToMany(mappedBy = "sense", fetch = LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DictionarySenseTr> translations = new ArrayList<>();
}
