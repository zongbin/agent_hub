package com.leon.agenthub.auth.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Where;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * 用户实体 — 对应 users 表。
 */
@Data
@Entity
@Table(name = "users")
@Where(clause = "deleted = 0")
public class User implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String username;

    @Column(nullable = false, length = 128)
    private String password;

    @Column(name = "real_name", length = 64)
    private String realName;

    @Column(length = 128)
    private String email;

    @Column(nullable = false, length = 16)
    private String role;

    @Column(length = 64)
    private String department;

    @Column(nullable = false)
    private Integer status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(nullable = false)
    private Integer deleted = 0;
}
