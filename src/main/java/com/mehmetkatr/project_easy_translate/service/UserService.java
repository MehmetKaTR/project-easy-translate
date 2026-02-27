package com.mehmetkatr.project_easy_translate.service;

import com.mehmetkatr.project_easy_translate.dto.auth.AuthResponse;
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
    private final GoogleTokenVerifierService googleTokenVerifierService;

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
                .name("All")
                .user(savedUser)
                .hexColorCode("#EEEEEE")
                .build();
        wordListRepository.save(generalList);

        // JWT artık sadece username veya subscriptionLevel içerebilir
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
                .name("All")
                .user(savedUser)
                .hexColorCode("#EEEEEE")
                .build();
        wordListRepository.save(generalList);

        return savedUser;
    }

    @Transactional
    public AuthResponse register(String username, String email, String rawPassword) {
        User savedUser = registerUser(username, email, rawPassword);
        String token = tokenService.generateToken(savedUser.getUsername(), "ROLE_USER");
        return buildAuthResponse(savedUser, token);
    }

    public AuthResponse login(String username, String rawPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        String token = tokenService.generateToken(user.getUsername(), "ROLE_USER");
        return buildAuthResponse(user, token);
    }

    @Transactional
    public AuthResponse socialLogin(String email, String username) {
        User user = registerOrLoginSocial(email, username);
        String token = tokenService.generateToken(user.getUsername(), "ROLE_USER");
        return buildAuthResponse(user, token);
    }

    @Transactional
    public AuthResponse loginWithGoogleIdToken(String idToken) {
        GoogleTokenVerifierService.VerifiedGoogleUser googleUser = googleTokenVerifierService.verifyIdToken(idToken);
        return socialLogin(googleUser.email(), googleUser.username());
    }

    private AuthResponse buildAuthResponse(User user, String token) {
        return AuthResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .token(token)
                .tokenType("Bearer")
                .build();
    }
}
