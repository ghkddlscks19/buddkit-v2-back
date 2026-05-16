package com.buddkitv2.domain.common;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "\"Address\"")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "address_id")
    private Long id;

    @Column(length = 50)
    private String city;

    @Column(length = 50)
    private String district;

    private Integer code;

    public static Address of(String city, String district, Integer code) {
        Address a = new Address();
        a.city = city;
        a.district = district;
        a.code = code;
        return a;
    }
}
