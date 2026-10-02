package com.mehmetkatr.project_easy_translate.entity;
import com.mehmetkatr.project_easy_translate.entity.base.BaseEntity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "words",
        indexes = {
                @Index(name="idx_words_text", columnList="text")
        },
        uniqueConstraints = @UniqueConstraint(name="uq_words_text_lang", columnNames={"text","language_code"})
)
public class Word extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Size(min = 1, max = 100)
    private String text;

    @NotNull
    private String languageCode;

    // Admin tarafindan bloklanmis (soft): tekrar eklenemez ve kullanici listelerinde gizlenir.
    @Builder.Default
    @Column(nullable = false)
    private boolean blocked = false;

}
