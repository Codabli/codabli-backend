package com.codabli.repository;

import com.codabli.entity.RefusModerationQwenGuard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RefusModerationQwenGuardRepository
        extends JpaRepository<RefusModerationQwenGuard, UUID> {

    List<RefusModerationQwenGuard> findByConteDanseIdOrderByDateCreationDesc(UUID conteDanseId);
}
