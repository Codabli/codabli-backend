package com.codabli.service;

import com.codabli.dto.EcoleRequest;
import com.codabli.dto.EcoleResponse;
import com.codabli.entity.Ecole;
import com.codabli.repository.EcoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class EcoleService {

    private final EcoleRepository ecoleRepository;

    public EcoleService(EcoleRepository ecoleRepository) {
        this.ecoleRepository = ecoleRepository;
    }

    public EcoleResponse creer(EcoleRequest request) {
        Ecole ecole = Ecole.builder()
                .nom(request.nom())
                .pays(request.pays())
                .ville(request.ville())
                .build();
        Ecole saved = ecoleRepository.save(ecole);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<EcoleResponse> listerTout() {
        return ecoleRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    private EcoleResponse toResponse(Ecole ecole) {
        return new EcoleResponse(
                ecole.getId(),
                ecole.getNom(),
                ecole.getPays(),
                ecole.getVille(),
                ecole.getNiveauAbonnement(),
                ecole.getStatut(),
                ecole.getDateCreation());
    }
}
