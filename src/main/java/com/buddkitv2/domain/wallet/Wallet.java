package com.buddkitv2.domain.wallet;

import com.buddkitv2.domain.common.BaseEntity;
import com.buddkitv2.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "\"WALLET\"")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Wallet extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wallet_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private Long balance;

    public static Wallet create(User user) {
        Wallet w = new Wallet();
        w.user = user;
        w.balance = 0L;
        return w;
    }

    public static Wallet createWithBonus(User user, Long bonus) {
        Wallet w = new Wallet();
        w.user = user;
        w.balance = bonus;
        return w;
    }
}
