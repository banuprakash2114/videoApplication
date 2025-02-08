package com.VideoCall.application.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@CrossOrigin(origins = "*")
@Slf4j
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @PostMapping
    public void register(@RequestBody User user) {
        service.register(user);
    }

    @PostMapping("/login")
    public User login(@RequestBody User user) {
        return service.login(user);
    }

    @PostMapping("/logout")
    public void logout(@RequestParam String email) {
        service.logout(email);
    }

    @GetMapping
    public List<User> findAll() {
        return service.findAll();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handle(Exception ex) {
        ex.printStackTrace();
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)  // Corrected the issue here
                .body(ex.getMessage());
    }
}