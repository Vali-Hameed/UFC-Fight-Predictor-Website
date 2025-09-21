package com.valihameed.ufcfightpredictor.registration.token;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ConformationTokenService {
    private final ConformationTokenRepository conformationTokenRepository;

    public void saveConformationToken(ConformationToken conformationToken) {
        conformationTokenRepository.save(conformationToken);
    }
}
