package com.clanmanager.clanmanager.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "members")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long memberId;

    @Column(nullable = false, unique = true, length = 50)
    private String characterName;

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    @Column(name = "must_change_password", nullable = false, columnDefinition = "boolean default false")
    private Boolean mustChangePassword;

    @Column(nullable = false)
    private Integer combatPower;

    @Column(length = 30)
    private String guildName;

    @Column(length = 50)
    private String characterClass;

    @Column(name = "myth_status", nullable = false, length = 10, columnDefinition = "varchar(10) default 'X'")
    private String mythStatus;

    @Column(name = "enoch_enabled", nullable = false, columnDefinition = "boolean default false")
    private Boolean enochEnabled;

    private Integer level;

    @Column(name = "member_rank", length = 30)
    private String rank;

    @Column(length = 30)
    private String status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberRole role;

    @Column(nullable = false)
    private Boolean active;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.combatPower = this.combatPower == null ? 0 : this.combatPower;
        this.level = this.level == null ? 0 : this.level;
        this.mythStatus = normalizeMythStatus(this.mythStatus);
        this.enochEnabled = this.enochEnabled == null ? false : this.enochEnabled;
        this.role = this.role == null ? MemberRole.MEMBER : this.role;
        this.active = this.active == null ? true : this.active;
        this.mustChangePassword = this.mustChangePassword == null ? false : this.mustChangePassword;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.mythStatus = normalizeMythStatus(this.mythStatus);
        this.enochEnabled = this.enochEnabled == null ? false : this.enochEnabled;
        this.updatedAt = LocalDateTime.now();
    }

    private String normalizeMythStatus(String value) {
        return "쌍신화".equals(value) || "외신화".equals(value) ? value : "X";
    }
}
