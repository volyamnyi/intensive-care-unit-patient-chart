package com.superhumans.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.superhumans.entity.core.AuthProvider;
import com.superhumans.entity.core.User;
import com.superhumans.entity.core.UserRole;
import com.superhumans.repository.core.UserRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Unit tests for environment-driven user seeding (no hardcoded user values).
 */
@ExtendWith(MockitoExtension.class)
class UserSeedServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserSeedService userSeedService;

    @Test
    void fullEnvironment_seedsNineLocalUsersWithHashedPasswords() {
        Map<String, String> env = fullEnv();
        when(passwordEncoder.encode(anyString()))
                .thenAnswer(invocation -> "HASHED:" + invocation.getArgument(0));

        userSeedService.seedFromEnvironment(env::get);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(9)).save(captor.capture());
        User first = captor.getAllValues().get(0);
        assertThat(first.getLogin()).isEqualTo("doctor1");
        assertThat(first.getPasswordHash()).isEqualTo("HASHED:doctor123");
        assertThat(first.getRole()).isEqualTo(UserRole.DOCTOR);
        assertThat(first.getAuthProvider()).isEqualTo(AuthProvider.LOCAL);
        assertThat(first.getFullName()).isEqualTo("Олександр Мельник");
        assertThat(first.getEmail()).isEqualTo("melnyk@hospital.ua");
        assertThat(first.getPhone()).isEqualTo("380501111111");
        assertThat(first.getSpecialityName()).isEqualTo("Лікар-хірург");
        User last = captor.getAllValues().get(8);
        assertThat(last.getLogin()).isEqualTo("prosthetics_admin1");
        assertThat(last.getRole()).isEqualTo(UserRole.PROSTHETICS_ADMINISTRATOR);
    }

    @Test
    void missingPair_skipsOnlyThatUser() {
        Map<String, String> env = fullEnv();
        env.remove("APP_TEST_USERNAME3");
        env.remove("APP_TEST_PASSWORD3");
        when(passwordEncoder.encode(anyString()))
                .thenAnswer(invocation -> "HASHED:" + invocation.getArgument(0));

        userSeedService.seedFromEnvironment(env::get);

        verify(userRepository, times(8)).save(any(User.class));
    }

    @Test
    void existingLogin_isNeverOverwritten() {
        Map<String, String> env = new HashMap<>();
        env.put("APP_TEST_USERNAME1", "doctor1");
        env.put("APP_TEST_PASSWORD1", "doctor123");
        env.put("APP_TEST_USERROLE1", "DOCTOR");
        User existing = User.builder().login("doctor1").role(UserRole.DOCTOR).build();
        when(userRepository.findByLogin("doctor1")).thenReturn(Optional.of(existing));

        userSeedService.seedFromEnvironment(env::get);

        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void invalidRole_defaultsToGuest() {
        Map<String, String> env = new HashMap<>();
        env.put("APP_TEST_USERNAME1", "doctor1");
        env.put("APP_TEST_PASSWORD1", "doctor123");
        env.put("APP_TEST_USERROLE1", "BOGUS");

        userSeedService.seedFromEnvironment(env::get);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(UserRole.GUEST);
    }

    @Test
    void missingProfile_fallsBackToLoginAndNulls() {
        Map<String, String> env = new HashMap<>();
        env.put("APP_TEST_USERNAME1", "doctor1");
        env.put("APP_TEST_PASSWORD1", "doctor123");
        env.put("APP_TEST_USERROLE1", "DOCTOR");

        userSeedService.seedFromEnvironment(env::get);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getFullName()).isEqualTo("doctor1");
        assertThat(captor.getValue().getEmail()).isNull();
        assertThat(captor.getValue().getPhone()).isNull();
        assertThat(captor.getValue().getSpecialityName()).isNull();
    }

    private static Map<String, String> fullEnv() {
        Map<String, String> env = new HashMap<>();
        String[][] users = {
            {"doctor1", "doctor123", "Олександр Мельник", "melnyk@hospital.ua",
                    "380501111111", "Лікар-хірург", "DOCTOR"},
            {"doctor2", "doctor123", "Наталія Бойко", "boyko@hospital.ua",
                    "380502222222", "Лікар-хірург", "DOCTOR"},
            {"nurse1", "nurse123", "Олена Ткаченко", "tkachenko@hospital.ua",
                    "380503333333", "Медична сестра стаціонару", "NURSE"},
            {"nurse2", "nurse123", "Марія Кравчук", "kravchuk@hospital.ua",
                    "380504444444", "Медична сестра стаціонару", "NURSE"},
            {"head1", "head123", "Василь Гончарук", "goncharuk@hospital.ua",
                    "380505555555", "Завідувач", "HEAD_OF_DEPARTMENT"},
            {"admin", "admin123", "Адмін Системи", "admin@hospital.ua",
                    "380506666666", "Адміністратор", "ADMINISTRATOR"},
            {"prosthetist1", "doctor123", "Олег Романюк", "romanyuk@hospital.ua",
                    "380507777777", "Протезист", "PROSTHETIST"},
            {"prosthetist2", "doctor123", "Ірина Шевчук", "shevchuk@hospital.ua",
                    "380508888888", "Протезист", "PROSTHETIST"},
            {"prosthetics_admin1", "doctor123", "Тарас Мельник", "ptadmin@hospital.ua",
                    "380509999999", "Адміністратор протезування", "PROSTHETICS_ADMINISTRATOR"},
        };
        for (int i = 0; i < users.length; i++) {
            String suffix = String.valueOf(i + 1);
            env.put("APP_TEST_USERNAME" + suffix, users[i][0]);
            env.put("APP_TEST_PASSWORD" + suffix, users[i][1]);
            env.put("APP_TEST_USERFULLNAME" + (i == 0 ? "" : suffix), users[i][2]);
            env.put("APP_TEST_EMAIL" + suffix, users[i][3]);
            env.put("APP_TEST_PHONE" + suffix, users[i][4]);
            env.put("APP_TEST_PROFESSION" + suffix, users[i][5]);
            env.put("APP_TEST_USERROLE" + suffix, users[i][6]);
        }
        return env;
    }
}
