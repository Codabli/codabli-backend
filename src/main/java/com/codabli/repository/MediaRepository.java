package com.codabli.repository;

import com.codabli.entity.Media;
import com.codabli.entity.enums.MediaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MediaRepository extends JpaRepository<Media, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Media m WHERE m.id = :id")
    Optional<Media> findByIdForUpdate(@Param("id") UUID id);

    List<Media> findTop100ByStatusInOrderByCreatedAtAsc(Collection<MediaStatus> statuses);

    List<Media> findTop100ByStatusInAndCreatedAtBeforeOrderByCreatedAtAsc(
            Collection<MediaStatus> statuses,
            OffsetDateTime createdAt);
}
