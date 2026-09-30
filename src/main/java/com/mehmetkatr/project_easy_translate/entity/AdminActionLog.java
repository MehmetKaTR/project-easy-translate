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
@Table(
        name = "admin_action_log",
        indexes = {
                @Index(name = "idx_admin_action_admin_id", columnList = "admin_id"),
                @Index(name = "idx_admin_action_target_user_id", columnList = "target_user_id")
        }
)
public class AdminActionLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private Admin admin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id")
    private User targetUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 50)
    private AdminActionType actionType;

    public enum AdminActionType {
        CREATE_USER,
        DELETE_USER,
        UPDATE_SUBSCRIPTION,
        UPDATE_TOKEN_LIMIT,
        SUSPEND_USER,
        REACTIVATE_USER,
        ADD_LLM_MODEL,
        REMOVE_LLM_MODEL
    }
}
