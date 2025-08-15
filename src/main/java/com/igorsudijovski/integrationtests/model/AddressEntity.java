package com.igorsudijovski.integrationtests.model;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Embeddable
public class AddressEntity {

    private String address;

    private String number;

    private String city;

    private String postalCode;

    private String country;

}

