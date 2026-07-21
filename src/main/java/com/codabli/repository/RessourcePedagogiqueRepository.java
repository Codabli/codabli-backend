package com.codabli.repository;

import com.codabli.entity.RessourcePedagogique;
import com.codabli.entity.enums.TypeRessource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface RessourcePedagogiqueRepository extends JpaRepository<RessourcePedagogique, UUID> {

    @Query("SELECT r FROM RessourcePedagogique r WHERE "
            + "(:type IS NULL OR r.type = :type) AND "
            + "(:thematique IS NULL OR LOWER(r.thematique) = LOWER(:thematique)) AND "
            + "(:niveauScolaire IS NULL OR LOWER(r.niveauScolaire) = LOWER(:niveauScolaire)) AND "
            + "(:actif IS NULL OR r.actif = :actif)")
    Page<RessourcePedagogique> findByFilters(
            @Param("type") TypeRessource type,
            @Param("thematique") String thematique,
            @Param("niveauScolaire") String niveauScolaire,
            @Param("actif") Boolean actif,
            Pageable pageable);
}
