package com.mehmetkatr.project_easy_translate.entity;

import com.mehmetkatr.project_easy_translate.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Bir kullanicinin bir sozluk kelimesi icin tekrar (SRS) durumu — SM-2.
 * Kelime havuzu UNIVERSAL (dictionary_entry); ilerleme/katsayi KULLANICIYA ozel.
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "user_study_progress",
        indexes = {
                @Index(name = "idx_usp_user_due", columnList = "user_id, due_date"),
                @Index(name = "idx_usp_entry", columnList = "entry_id")
        },
        uniqueConstraints = @UniqueConstraint(
                name = "uq_usp_user_entry", columnNames = {"user_id", "entry_id"})
)
public class UserStudyProgress extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "entry_id", nullable = false)
    private Long entryId;

    /** SM-2 katsayi (ease factor). Baslangic 2.5, min 1.3. */
    @Builder.Default
    @Column(nullable = false)
    private double ease = 2.5;

    /** Mevcut aralik (gun). */
    @Builder.Default
    @Column(name = "interval_days", nullable = false)
    private int intervalDays = 0;

    /** Bir sonraki tekrar gunu (null = hic calisilmadi / yeni). */
    @Column(name = "due_date")
    private LocalDate dueDate;

    /** Toplam dogru tekrar sayisi. */
    @Builder.Default
    @Column(nullable = false)
    private int reps = 0;

    /** Kac kez unutuldu (AGAIN). */
    @Builder.Default
    @Column(nullable = false)
    private int lapses = 0;

    /** Son sonuc: AGAIN | HARD | GOOD | EASY. */
    @Column(name = "last_result", length = 10)
    private String lastResult;

    @Column(name = "last_reviewed_at")
    private LocalDateTime lastReviewedAt;
}
