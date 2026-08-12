package com.codabli.repository;

import com.codabli.entity.PageCarnetLecture;
import com.codabli.entity.enums.StatutPageCarnet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository pour PageCarnetLecture (CDC 8.17.1, CDL-17).
 */
@Repository
public interface PageCarnetLectureRepository extends JpaRepository<PageCarnetLecture, UUID> {

    List<PageCarnetLecture> findByProfilEnfantIdOrderByDateCreationDesc(UUID profilEnfantId);

    long countByStatut(StatutPageCarnet statut);
}
