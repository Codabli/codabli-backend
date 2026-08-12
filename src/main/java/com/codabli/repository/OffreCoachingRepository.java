package com.codabli.repository;

import com.codabli.entity.OffreCoaching;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OffreCoachingRepository extends JpaRepository<OffreCoaching, UUID> {

    List<OffreCoaching> findByActifTrue();
}
