package com.superhumans.mapper;

import com.superhumans.dto.SignResponse;
import com.superhumans.entity.Signature;

public class SignatureMapper {

    public static SignResponse toResponse(Signature entity) {
        return SignResponse.builder()
                .signatureId(entity.getId())
                .clinicalDayId(entity.getClinicalDay().getId())
                .role(entity.getRole())
                .signedAt(entity.getSignedAt())
                .hash(entity.getHash())
                .version(entity.getVersion())
                .build();
    }
}
