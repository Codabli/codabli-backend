package com.codabli.repository;

import com.codabli.entity.SalleGalerie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SalleGalerieRepository extends JpaRepository<SalleGalerie, UUID> {

    List<SalleGalerie> findByActifTrueOrderByOrdreAffichageAsc();
}
