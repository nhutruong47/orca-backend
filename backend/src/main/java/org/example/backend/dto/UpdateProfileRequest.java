package org.example.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UpdateProfileRequest {
    @Size(max = 100, message = "Full name must be at most 100 characters")
    private String fullName;

    @Size(max = 500, message = "Avatar URL must be at most 500 characters")
    @Pattern(regexp = "^(https?://.*|data:image/.*)?$",
            message = "Avatar must be a valid http(s) URL or data:image/*")
    private String avatar;

    @Size(max = 120)
    @Email(message = "Email must be a valid email address")
    private String email;

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
