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
        Ecole ecole = entityManager.getReference(Ecole.class, request.ecoleId());
        Utilisateur enseignant = entityManager.getReference(Utilisateur.class, request.enseignantId());

        Classe classe = Classe.builder()
                .ecole(ecole)
                .enseignant(enseignant)
                .nom(request.nom())
                .niveau(request.niveau())
                .anneeScolaire(request.anneeScolaire())
                .build();

        Classe saved = classeRepository.save(classe);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ClasseResponse> listerTout() {
        return classeRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    private ClasseResponse toResponse(Classe classe) {
        return new ClasseResponse(
                classe.getId(),
                classe.getNom(),
                classe.getNiveau(),
                classe.getAnneeScolaire(),
                classe.getEcole().getId(),
                classe.getEcole().getNom(),
                classe.getEnseignant().getId(),
                classe.getEnseignant().getNom(),
                classe.getDateCreation());
    }
}
