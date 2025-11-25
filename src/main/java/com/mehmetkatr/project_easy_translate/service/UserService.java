package com.mehmetkatr.project_easy_translate.service;

import com.mehmetkatr.project_easy_translate.entity.User;
import com.mehmetkatr.project_easy_translate.entity.WordList;
import com.mehmetkatr.project_easy_translate.repository.UserRepository;
import com.mehmetkatr.project_easy_translate.repository.WordListRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final WordListRepository wordListRepository;
    private final PasswordEncoder passwordEncoder;

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public List<User> findBySubscriptionLevel(User.SubscriptionLevel subscriptionLevel) {
        return userRepository.findBySubscriptionLevel(subscriptionLevel);
    }

    @Transactional
    public User registerUser(String username, String email, String rawPassword) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Email already exists");
        }
        if (userRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .subscriptionLevel(User.SubscriptionLevel.FREE)
                .tokenBalance(0)
                .build();

        User savedUser = userRepository.save(user);

        WordList generalList = WordList.builder()
                .name("General")
                .user(savedUser)
                .hexColorCode("#EEEEEE")
                .build();
        wordListRepository.save(generalList);

        // JWT artık sadece username veya subscriptionLevel içerebilir
        String jwt = tokenService.generateToken(savedUser.getUsername(), savedUser.getSubscriptionLevel().name());
        System.out.println("Generated JWT for new user: " + jwt);

        return savedUser;
    }

    @Transactional
    public User registerOrLoginSocial(String email, String username) {
        Optional<User> existingUser = userRepository.findByEmail(email);
        if (existingUser.isPresent()) {
            return existingUser.get();
        }

        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash("") // social login: şifre gerekmez
                .subscriptionLevel(User.SubscriptionLevel.FREE)
                .tokenBalance(0)
                .build();

        User savedUser = userRepository.save(user);

        WordList generalList = WordList.builder()
                .name("General")
                .user(savedUser)
                .hexColorCode("#EEEEEE")
                .build();
        wordListRepository.save(generalList);

        String jwt = tokenService.generateToken(savedUser.getUsername(), savedUser.getSubscriptionLevel().name());
        System.out.println("Generated JWT for social user: " + jwt);

        return savedUser;
    }
}
