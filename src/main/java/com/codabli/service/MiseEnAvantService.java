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
                .toList();
    }

    public MiseEnAvantResponse creer(MiseEnAvantRequest request) {
        MiseEnAvant miseEnAvant = MiseEnAvant.builder()
                .typeCible(request.typeCible())
                .cibleId(request.cibleId())
                .titre(request.titre())
                .dateDebut(request.dateDebut())
                .dateFin(request.dateFin())
                .ordreAffichage(request.ordreAffichage() != null ? request.ordreAffichage() : 0)
                .actif(request.actif() == null || request.actif())
                .build();
        return toResponse(miseEnAvantRepository.save(miseEnAvant));
    }

    public MiseEnAvantResponse modifier(UUID id, MiseEnAvantRequest request) {
        MiseEnAvant miseEnAvant = getOrThrow(id);

        miseEnAvant.setTypeCible(request.typeCible());
        miseEnAvant.setCibleId(request.cibleId());
        miseEnAvant.setTitre(request.titre());
        if (request.dateDebut() != null) {
            miseEnAvant.setDateDebut(request.dateDebut());
        }
        miseEnAvant.setDateFin(request.dateFin());
        if (request.ordreAffichage() != null) {
            miseEnAvant.setOrdreAffichage(request.ordreAffichage());
        }
        if (request.actif() != null) {
            miseEnAvant.setActif(request.actif());
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
        return new MiseEnAvantResponse(
                miseEnAvant.getId(),
                miseEnAvant.getTypeCible(),
                miseEnAvant.getCibleId(),
                miseEnAvant.getTitre(),
                miseEnAvant.getDateDebut(),
                miseEnAvant.getDateFin(),
                miseEnAvant.getOrdreAffichage(),
                miseEnAvant.isActif());
    }
}
