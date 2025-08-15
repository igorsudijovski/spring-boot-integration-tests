package com.igorsudijovski.integrationtests.model;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    private String username;

    private String firstName;

    private String lastName;

    @Embedded
    private AddressEntity address;

    // getters and setters

}

