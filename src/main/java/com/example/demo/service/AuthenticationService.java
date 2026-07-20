package com.example.demo.service;

import com.example.demo.dto.AuthenticationResponse;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class AuthenticationService {


    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthenticationResponse register(RegisterRequest registerRequest){
        //Handles user registration
        //passing RegisterRequest as parameters
        //using authentication response dto to generate token a user

         User user = User.builder() // building a new user
                 .firstName(registerRequest.getFirstName())
                 .lastName(registerRequest.getLastName())
                 .email(registerRequest.getEmail())
                 .password(passwordEncoder.encode(registerRequest.getPassword()))
                 .role(Role.USER)
                 .build();

         userRepository.save(user); // saves user in the database

        var jwtToken = jwtService.generateToken(user);
        var refreshToken = jwtService.generatedRefresh(new HashMap<>(), user);

        // returns both tokens in wrapped AuthenticationResponse DTO to the client
        return AuthenticationResponse.builder()
                .authenticationToken(jwtToken)
                .refreshToken(refreshToken)
                .build();

    }
}
