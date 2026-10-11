package com.airwise.controller;

import com.airwise.entity.Role;
import com.airwise.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            @RequestParam(value = "registered", required = false) String registered,
                            Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", "Invalid username or password. Please try again.");
        }
        if (logout != null) {
            model.addAttribute("successMessage", "You have been logged out successfully.");
        }
        if (registered != null) {
            model.addAttribute("successMessage", "Registration successful! You can now log in.");
        }
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        return "register";
    }

    @PostMapping("/register")
    public String processRegister(@RequestParam("username") String username,
                                  @RequestParam("password") String password,
                                  @RequestParam("confirmPassword") String confirmPassword,
                                  @RequestParam("fullName") String fullName,
                                  @RequestParam(value = "phone", required = false) String phone,
                                  @RequestParam(value = "role", defaultValue = "ROLE_PASSENGER") String roleStr,
                                  Model model) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            model.addAttribute("errorMessage", "Username and password cannot be empty.");
            return "register";
        }

        if (!password.equals(confirmPassword)) {
            model.addAttribute("errorMessage", "Passwords do not match.");
            return "register";
        }

        if (userService.usernameExists(username)) {
            model.addAttribute("errorMessage", "Username or email is already registered.");
            return "register";
        }

        Role role = "ROLE_TAXI_OPERATOR".equals(roleStr) ? Role.ROLE_TAXI_OPERATOR : Role.ROLE_PASSENGER;

        userService.registerUser(username, password, fullName, phone, role);
        return "redirect:/login?registered=true";
    }
}
