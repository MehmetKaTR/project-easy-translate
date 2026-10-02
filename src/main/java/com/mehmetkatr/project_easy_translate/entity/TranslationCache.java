package com.mehmetkatr.project_easy_translate.entity;

import com.mehmetkatr.project_easy_translate.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Paylasimli ceviri onbellegi (Katman 2): ayni (metin, kaynak, hedef) bir daha cevrilmez.
 * Tum kullanicilar ve platformlar (web + mobil) ayni cache'ten yararlanir.
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "translation_cache",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_translation_cache",
                columnNames = {"source_text", "source_lang", "target_lang"})
)
public class TranslationCache extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_text", nullable = false, length = 160)
    private String sourceText;

    @Column(name = "source_lang", nullable = false, length = 10)
    private String sourceLang;

    @Column(name = "target_lang", nullable = false, length = 10)
    private String targetLang;

    @Column(columnDefinition = "TEXT")
    private String translation;

    // GOOGLE_TRANSLATE | GEMINI
    private String engine;
}
