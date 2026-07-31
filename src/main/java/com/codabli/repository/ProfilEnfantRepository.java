package com.codabli.repository;

import com.codabli.entity.ProfilEnfant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository pour ProfilEnfant (CDC FAM-02, ENF-01).
 */
@Repository
public interface ProfilEnfantRepository extends JpaRepository<ProfilEnfant, UUID> {

    List<ProfilEnfant> findByResponsableIdOrderByPseudonyme(UUID responsableId);
}
