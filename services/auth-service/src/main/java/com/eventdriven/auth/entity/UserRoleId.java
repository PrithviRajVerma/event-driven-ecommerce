package com.eventdriven.auth.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;


// this works as a composite primary key and is not a entity

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Setter
@Embeddable
@AllArgsConstructor
public class UserRoleId implements Serializable {

    private UUID userId;
    private UUID roleId;

}
