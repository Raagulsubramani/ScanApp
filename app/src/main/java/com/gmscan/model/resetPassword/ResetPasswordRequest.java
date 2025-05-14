package com.gmscan.model.resetPassword;

public class ResetPasswordRequest {
    public String email;
    public String otp;
    public String new_password;
    public String confirm_password;

    public ResetPasswordRequest(String otp,String email) {
        this.email = email;
        this.otp = otp;
    }

    public ResetPasswordRequest(String newPassword, String confirmPassword, String otp) {
        this.new_password = newPassword;
        this.confirm_password = confirmPassword;
        this.otp = otp;
    }
}