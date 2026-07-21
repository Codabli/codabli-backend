package com.codabli.service;

import com.codabli.dto.InscriptionClasseRequest;
import com.codabli.dto.InscriptionClasseResponse;
import com.codabli.entity.Classe;
import com.codabli.entity.InscriptionClasse;
import com.codabli.entity.Utilisateur;
import com.codabli.repository.InscriptionClasseRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InscriptionClasseService {

    private final InscriptionClasseRepository inscriptionClasseRepository;
    private final EntityManager entityManager;

    public InscriptionClasseService(InscriptionClasseRepository inscriptionClasseRepository,
            EntityManager entityManager) {
        this.inscriptionClasseRepository = inscriptionClasseRepository;
        this.entityManager = entityManager;
    }

    public InscriptionClasseResponse inscrire(InscriptionClasseRequest request) {
        Utilisateur eleve = entityManager.find(Utilisateur.class, request.getEleveId());
        if (eleve == null) {
            throw new ResourceNotFoundException("Eleve non trouve avec l'ID: " + request.getEleveId());
        }

        Classe classe = entityManager.find(Classe.class, request.getClasseId());
        if (classe == null) {
            throw new ResourceNotFoundException("Classe non trouvee avec l'ID: " + request.getClasseId());
        }

        InscriptionClasse inscription = InscriptionClasse.builder()
                .eleve(eleve)
                .classe(classe)
                .build();

        InscriptionClasse saved = inscriptionClasseRepository.save(inscription);

        return InscriptionClasseResponse.builder()
                .id(saved.getId())
                .eleveId(eleve.getId())
                .eleveNom(eleve.getNom())
                .elevePrenom(eleve.getPrenom())
                .classeId(classe.getId())
                .classeNom(classe.getNom())
                .dateInscription(saved.getDateInscription())
                .build();
    }
}
