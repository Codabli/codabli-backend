package com.codabli.repository;

import com.codabli.entity.MessageAssistantIa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageAssistantIaRepository extends JpaRepository<MessageAssistantIa, UUID> {

    List<MessageAssistantIa> findByConteDanseIdOrderByDateCreationAscIdAsc(UUID conteDanseId);

    Optional<MessageAssistantIa> findFirstByConteDanseIdOrderByDateCreationDescIdDesc(UUID conteDanseId);

    void deleteByConteDanseId(UUID conteDanseId);
}
