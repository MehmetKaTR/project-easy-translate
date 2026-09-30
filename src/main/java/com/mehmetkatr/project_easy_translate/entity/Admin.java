package com.mehmetkatr.project_easy_translate.entity;
import com.mehmetkatr.project_easy_translate.entity.base.BaseEntity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "admins",
        indexes = {
                @Index(name = "idx_admins_username", columnList = "username"),
                @Index(name = "idx_admins_email", columnList = "email")
        }
)
public class Admin extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Size(min = 3, max = 50)
    @Column(unique = true, nullable = false)
    private String username;

    @NotNull
    @Email
    @Column(unique = true, nullable = false)
    private String email;

    @NotNull
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @OneToMany(mappedBy = "admin", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AdminActionLog> adminActions;
}
