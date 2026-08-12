package com.codabli.repository;

import com.codabli.entity.ReservationCoaching;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReservationCoachingRepository extends JpaRepository<ReservationCoaching, UUID> {

    List<ReservationCoaching> findByUtilisateurIdOrderByDateCreneauDesc(UUID utilisateurId);
}
