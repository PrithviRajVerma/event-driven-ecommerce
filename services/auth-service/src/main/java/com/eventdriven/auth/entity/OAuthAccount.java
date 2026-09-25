package com.eventdriven.auth.entity;

import com.eventdriven.auth.enums.OAuthProvider;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "oauth_accounts",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_oauth_provider_user",
                        columnNames = {"provider","provider_user_id"}
                )
        }
)
@NoArgsConstructor
@Getter
@Setter
public class OAuthAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY,optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider",nullable = false, length = 50)
    private OAuthProvider provider;

    @Column(name = "provider_user_id", nullable = false,unique = true,length = 255)
    private String providerUserId;

    @Column(name = "created_at",nullable = false)
    private OffsetDateTime createdAt;
}
