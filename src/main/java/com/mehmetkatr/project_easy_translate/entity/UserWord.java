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

    // Language the user translated INTO (target). Drives word-detail example/definition translations.
    @Column(length = 10)
    private String targetLanguageCode;

    private boolean starred;

    // --- TaleMind Sozlugu baglantisi (Faz 5'te kullanilacak; nullable, mevcut akisi bozmaz) ---
    /** Eslesti ise dictionary_entry.id (zengin kart). */
    @Column(name = "dictionary_entry_id")
    private Long dictionaryEntryId;

    /** Secili anlam dictionary_sense.id (homonymde hangi anlam). */
    @Column(name = "sense_id")
    private Long senseId;

    /** Kullanicinin kendi yazdigi serbest kelime (sozlukte yok). */
    @Builder.Default
    @Column(nullable = false)
    private boolean custom = false;

    /** Premium AI ile uretilmis kart. */
    @Builder.Default
    @Column(name = "ai_generated", nullable = false)
    private boolean aiGenerated = false;

    @ManyToMany
    @JoinTable(name="user_word_wordlist",
            joinColumns=@JoinColumn(name="user_word_id"),
            inverseJoinColumns=@JoinColumn(name="word_list_id"))
    @Builder.Default
    private Set<WordList> wordLists = new LinkedHashSet<>();
}
