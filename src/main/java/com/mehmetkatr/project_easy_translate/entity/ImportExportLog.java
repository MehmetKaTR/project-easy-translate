package com.mehmetkatr.project_easy_translate.entity;
import com.mehmetkatr.project_easy_translate.entity.base.BaseEntity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "import_export_log")
public class ImportExportLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String source;

    @Column(name = "words_count")
    private int wordsCount;
}
