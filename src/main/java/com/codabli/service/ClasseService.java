package com.codabli.service;

import com.codabli.dto.ClasseRequest;
import com.codabli.dto.ClasseResponse;
import com.codabli.entity.Classe;
import com.codabli.entity.Ecole;
import com.codabli.entity.Utilisateur;
import com.codabli.repository.ClasseRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ClasseService {

    private final ClasseRepository classeRepository;
    private final EntityManager entityManager;

    public ClasseService(ClasseRepository classeRepository, EntityManager entityManager) {
        this.classeRepository = classeRepository;
        this.entityManager = entityManager;
    }

    public ClasseResponse creer(ClasseRequest request) {
        Ecole ecole = entityManager.getReference(Ecole.class, request.getEcoleId());
        Utilisateur enseignant = entityManager.getReference(Utilisateur.class, request.getEnseignantId());

        Classe classe = Classe.builder()
                .ecole(ecole)
                .enseignant(enseignant)
                .nom(request.getNom())
                .niveau(request.getNiveau())
                .anneeScolaire(request.getAnneeScolaire())
                .build();

        Classe saved = classeRepository.save(classe);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ClasseResponse> listerTout() {
        return classeRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private ClasseResponse toResponse(Classe classe) {
        return ClasseResponse.builder()
                .id(classe.getId())
                .nom(classe.getNom())
                .niveau(classe.getNiveau())
                .anneeScolaire(classe.getAnneeScolaire())
                .ecoleId(classe.getEcole().getId())
                .enseignantId(classe.getEnseignant().getId())
                .dateCreation(classe.getDateCreation())
                .build();
    }
}
