package com.codabli.service;

import com.codabli.dto.EcoleRequest;
import com.codabli.dto.EcoleResponse;
import com.codabli.entity.Ecole;
import com.codabli.repository.EcoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class EcoleService {

    private final EcoleRepository ecoleRepository;

    public EcoleService(EcoleRepository ecoleRepository) {
        this.ecoleRepository = ecoleRepository;
    }

    public EcoleResponse creer(EcoleRequest request) {
        Ecole ecole = Ecole.builder()
                .nom(request.getNom())
                .pays(request.getPays())
                .ville(request.getVille())
                .build();
        Ecole saved = ecoleRepository.save(ecole);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<EcoleResponse> listerTout() {
        return ecoleRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private EcoleResponse toResponse(Ecole ecole) {
        return EcoleResponse.builder()
                .id(ecole.getId())
                .nom(ecole.getNom())
                .pays(ecole.getPays())
                .ville(ecole.getVille())
                .niveauAbonnement(ecole.getNiveauAbonnement())
                .statut(ecole.getStatut())
                .dateCreation(ecole.getDateCreation())
                .build();
    }
}
