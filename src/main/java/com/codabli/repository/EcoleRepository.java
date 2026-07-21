package com.codabli.repository;

import com.codabli.entity.Ecole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface EcoleRepository extends JpaRepository<Ecole, UUID> {
}
