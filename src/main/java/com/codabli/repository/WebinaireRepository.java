package com.codabli.repository;

import com.codabli.entity.Webinaire;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface WebinaireRepository extends JpaRepository<Webinaire, UUID> {

    Page<Webinaire> findByActifTrueOrderByDateDebutAsc(Pageable pageable);
}
