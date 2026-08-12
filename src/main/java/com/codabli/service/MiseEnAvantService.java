package com.codabli.service;

import com.codabli.dto.MiseEnAvantRequest;
import com.codabli.dto.MiseEnAvantResponse;
import com.codabli.entity.MiseEnAvant;
import com.codabli.repository.MiseEnAvantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service metier pour les mises en avant de la page d'accueil (CDC ACC-04).
 */
@Service
@Transactional
public class MiseEnAvantService {

    private final MiseEnAvantRepository miseEnAvantRepository;

    public MiseEnAvantService(MiseEnAvantRepository miseEnAvantRepository) {
        this.miseEnAvantRepository = miseEnAvantRepository;
    }

    @Transactional(readOnly = true)
    public List<MiseEnAvantResponse> listerActives() {
        return miseEnAvantRepository.findActivesEnCours(OffsetDateTime.now()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public MiseEnAvantResponse creer(MiseEnAvantRequest request) {
        MiseEnAvant miseEnAvant = MiseEnAvant.builder()
                .typeCible(request.getTypeCible())
                .cibleId(request.getCibleId())
                .titre(request.getTitre())
                .dateDebut(request.getDateDebut())
                .dateFin(request.getDateFin())
                .ordreAffichage(request.getOrdreAffichage() != null ? request.getOrdreAffichage() : 0)
                .actif(request.getActif() == null || request.getActif())
                .build();
        return toResponse(miseEnAvantRepository.save(miseEnAvant));
    }

    public MiseEnAvantResponse modifier(UUID id, MiseEnAvantRequest request) {
        MiseEnAvant miseEnAvant = getOrThrow(id);

        miseEnAvant.setTypeCible(request.getTypeCible());
        miseEnAvant.setCibleId(request.getCibleId());
        miseEnAvant.setTitre(request.getTitre());
        if (request.getDateDebut() != null) {
            miseEnAvant.setDateDebut(request.getDateDebut());
        }
        miseEnAvant.setDateFin(request.getDateFin());
        if (request.getOrdreAffichage() != null) {
            miseEnAvant.setOrdreAffichage(request.getOrdreAffichage());
        }
        if (request.getActif() != null) {
            miseEnAvant.setActif(request.getActif());
        }

        return toResponse(miseEnAvantRepository.save(miseEnAvant));
    }

    public void supprimer(UUID id) {
        miseEnAvantRepository.delete(getOrThrow(id));
    }

    private MiseEnAvant getOrThrow(UUID id) {
        return miseEnAvantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mise en avant non trouvee avec l'ID: " + id));
    }

    private MiseEnAvantResponse toResponse(MiseEnAvant miseEnAvant) {
        return MiseEnAvantResponse.builder()
                .id(miseEnAvant.getId())
                .typeCible(miseEnAvant.getTypeCible())
                .cibleId(miseEnAvant.getCibleId())
                .titre(miseEnAvant.getTitre())
                .dateDebut(miseEnAvant.getDateDebut())
                .dateFin(miseEnAvant.getDateFin())
                .ordreAffichage(miseEnAvant.getOrdreAffichage())
                .actif(miseEnAvant.isActif())
                .build();
    }
}
