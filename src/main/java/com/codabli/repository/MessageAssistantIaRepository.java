package com.codabli.repository;

import java.util.List;
import java.util.UUID;

import com.codabli.entity.MessageAssistantIa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageAssistantIaRepository extends JpaRepository<MessageAssistantIa, UUID> {

    List<MessageAssistantIa> findByConteDanseIdOrderByDateCreationAscIdAsc(UUID conteDanseId);

    void deleteByConteDanseId(UUID conteDanseId);
}
