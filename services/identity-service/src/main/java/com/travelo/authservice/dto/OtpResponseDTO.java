package com.travelo.authservice.dto;

public class OtpResponseDTO {
    private String email;
    private Integer otpExpiresIn;
    private Integer resendAllowedAfter;

    public OtpResponseDTO() {
    }

    public OtpResponseDTO(String email, Integer otpExpiresIn, Integer resendAllowedAfter) {
        this.email = email;
        this.otpExpiresIn = otpExpiresIn;
        this.resendAllowedAfter = resendAllowedAfter;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Integer getOtpExpiresIn() {
        return otpExpiresIn;
    }

    public void setOtpExpiresIn(Integer otpExpiresIn) {
        this.otpExpiresIn = otpExpiresIn;
    }

    public Integer getResendAllowedAfter() {
        return resendAllowedAfter;
    }

    public void setResendAllowedAfter(Integer resendAllowedAfter) {
        this.resendAllowedAfter = resendAllowedAfter;
    }

    @Override
    public String toString() {
        return "OtpResponseDTO{" +
                "email='" + email + '\'' +
                ", otpExpiresIn=" + otpExpiresIn +
                ", resendAllowedAfter=" + resendAllowedAfter +
                '}';
    }
}

