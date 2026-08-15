package com.superhumans.integration;

import com.superhumans.mis.dto.UserMisDTO;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;

class MisUserIntegrationTest extends AbstractIntegrationTest {

    @Test
    void getUser_returnsDoctorUser() {
        var entity = authGet(getDoctorToken());
        Long userId = 11L;

        var res = restTemplate.exchange(
                "/api/users/{id}", HttpMethod.GET, entity,
                UserMisDTO.class, userId);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().getFullName()).contains("Мельник");
    }

    @Test
    void getUser_returnsNurseUser() {
        var entity = authGet(getNurseToken());
        Long userId = 13L;

        var res = restTemplate.exchange(
                "/api/users/{id}", HttpMethod.GET, entity,
                UserMisDTO.class, userId);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().getFullName()).contains("Ткаченко");
    }

    @Test
    void getUser_whenNotFound_returns404() {
        var entity = authGet(getDoctorToken());
        Long unknownId = 9999L;

        var res = restTemplate.exchange(
                "/api/users/{id}", HttpMethod.GET, entity,
                UserMisDTO.class, unknownId);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getUser_returnsHodUser() {
        var entity = authGet(getHodToken());
        Long userId = 15L;

        var res = restTemplate.exchange(
                "/api/users/{id}", HttpMethod.GET, entity,
                UserMisDTO.class, userId);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().getFullName()).contains("Гончарук");
    }

    @Test
    void getUser_userMeEndpoint_stillWorks() {
        var entity = authGet(getDoctorToken());

        var res = restTemplate.exchange(
                "/api/users/me", HttpMethod.GET, entity,
                com.superhumans.entity.core.User.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().getLogin()).isEqualTo("doctor1");
    }

    @Test
    void getUser_doctorsEndpoint_stillWorks() {
        var entity = authGet(getDoctorToken());

        var res = restTemplate.exchange(
                "/api/users/doctors", HttpMethod.GET, entity,
                com.superhumans.entity.core.User[].class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotEmpty();
    }
}
