package com.codabli.service;

import com.codabli.dto.ConteContextData;
import com.codabli.exception.ErrorCode;
import com.codabli.exception.ResourceNotFoundException;
import com.codabli.repository.ConteDanseRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ConteContextService {

    private final ConteDanseRepository conteDanseRepository;
    private final ConteDanseAccesService conteDanseAccesService;

    public ConteContextService(ConteDanseRepository conteDanseRepository,
                               ConteDanseAccesService conteDanseAccesService) {
        this.conteDanseRepository = conteDanseRepository;
        this.conteDanseAccesService = conteDanseAccesService;
    }

    @Transactional(readOnly = true)
    public ConteContextData charger(UUID conteDanseId, Jwt jwt) {
        conteDanseAccesService.verifierAcces(conteDanseId, jwt);
        return conteDanseRepository.findConteContextDataById(conteDanseId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.UNKNOWN_CONTE_DANSE,
                        "Conte danse non trouve avec l'ID: " + conteDanseId));
    }
}
