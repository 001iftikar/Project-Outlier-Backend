package com.iftikar.outlier.service.impl;

import com.iftikar.outlier.service.api.OtpService;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
public class OtpServiceImpl implements OtpService {
    private final SecureRandom secureRandom = new SecureRandom();
    @Override
    public String generateOtp() {
        int otp = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(otp);
    }
}
