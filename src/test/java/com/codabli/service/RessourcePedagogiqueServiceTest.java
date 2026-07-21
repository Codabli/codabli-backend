package com.codabli.service;

import com.codabli.dto.RessourcePedagogiqueRequest;
import com.codabli.dto.RessourcePedagogiqueResponse;
import com.codabli.entity.RessourcePedagogique;
import com.codabli.entity.enums.TypeRessource;
import com.codabli.repository.RessourcePedagogiqueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RessourcePedagogiqueServiceTest {

    @Mock
    private RessourcePedagogiqueRepository repository;

    @InjectMocks
    private RessourcePedagogiqueService service;

    private Jwt teacherJwt;
    private Jwt comiteJwt;
    private Jwt adminJwt;

    @BeforeEach
    void setUp() {
        teacherJwt = mock(Jwt.class);
        when(teacherJwt.getClaimAsMap("realm_access")).thenReturn(Map.of("roles", List.of("enseignant")));

        comiteJwt = mock(Jwt.class);
        when(comiteJwt.getClaimAsMap("realm_access")).thenReturn(Map.of("roles", List.of("comite_lecture")));

        adminJwt = mock(Jwt.class);
        when(adminJwt.getClaimAsMap("realm_access")).thenReturn(Map.of("roles", List.of("admin")));
    }

    @Test
    void lister_asTeacher_shouldForceActifTrue() {
        Pageable pageable = PageRequest.of(0, 10);
        RessourcePedagogique mockRes = RessourcePedagogique.builder()
                .id(UUID.randomUUID())
                .titre("Fiche 1")
                .type(TypeRessource.fiche)
                .actif(true)
                .dateAjout(OffsetDateTime.now())
                .build();

        when(repository.findByFilters(eq(TypeRessource.fiche), eq("Maths"), eq("6eme"), eq(true), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(mockRes)));

        Page<RessourcePedagogiqueResponse> result = service.lister(
                TypeRessource.fiche, "Maths", "6eme", false, teacherJwt, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertTrue(result.getContent().get(0).isActif());
        verify(repository).findByFilters(eq(TypeRessource.fiche), eq("Maths"), eq("6eme"), eq(true), eq(pageable));
    }

    @Test
    void lister_asComite_shouldHonorActifFilter() {
        Pageable pageable = PageRequest.of(0, 10);
        RessourcePedagogique mockRes = RessourcePedagogique.builder()
                .id(UUID.randomUUID())
                .titre("Fiche 2")
                .type(TypeRessource.guide)
                .actif(false)
                .dateAjout(OffsetDateTime.now())
                .build();

        when(repository.findByFilters(eq(TypeRessource.guide), eq("Maths"), eq("6eme"), eq(false), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(mockRes)));

        Page<RessourcePedagogiqueResponse> result = service.lister(
                TypeRessource.guide, "Maths", "6eme", false, comiteJwt, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertFalse(result.getContent().get(0).isActif());
        verify(repository).findByFilters(eq(TypeRessource.guide), eq("Maths"), eq("6eme"), eq(false), eq(pageable));
    }

    @Test
    void getById_whenActive_shouldReturnToAll() {
        UUID id = UUID.randomUUID();
        RessourcePedagogique mockRes = RessourcePedagogique.builder()
                .id(id)
                .titre("Fiche math active")
                .type(TypeRessource.fiche)
                .actif(true)
                .dateAjout(OffsetDateTime.now())
                .build();

        when(repository.findById(id)).thenReturn(Optional.of(mockRes));

        RessourcePedagogiqueResponse result = service.getById(id, teacherJwt);
        assertNotNull(result);
        assertEquals("Fiche math active", result.getTitre());
    }

    @Test
    void getById_whenInactive_shouldThrowAccessDeniedForTeacher() {
        UUID id = UUID.randomUUID();
        RessourcePedagogique mockRes = RessourcePedagogique.builder()
                .id(id)
                .titre("Fiche inactive")
                .type(TypeRessource.fiche)
                .actif(false)
                .dateAjout(OffsetDateTime.now())
                .build();

        when(repository.findById(id)).thenReturn(Optional.of(mockRes));

        assertThrows(AccessDeniedException.class, () -> service.getById(id, teacherJwt));
    }

    @Test
    void getById_whenInactive_shouldAllowAdminOrComite() {
        UUID id = UUID.randomUUID();
        RessourcePedagogique mockRes = RessourcePedagogique.builder()
                .id(id)
                .titre("Fiche inactive")
                .type(TypeRessource.fiche)
                .actif(false)
                .dateAjout(OffsetDateTime.now())
                .build();

        when(repository.findById(id)).thenReturn(Optional.of(mockRes));

        RessourcePedagogiqueResponse resultComite = service.getById(id, comiteJwt);
        RessourcePedagogiqueResponse resultAdmin = service.getById(id, adminJwt);

        assertNotNull(resultComite);
        assertNotNull(resultAdmin);
        assertFalse(resultComite.isActif());
    }

    @Test
    void creer_shouldSaveRessource() {
        RessourcePedagogiqueRequest request = RessourcePedagogiqueRequest.builder()
                .titre("Nouveau guide")
                .type(TypeRessource.guide)
                .actif(true)
                .build();

        RessourcePedagogique savedRes = RessourcePedagogique.builder()
                .id(UUID.randomUUID())
                .titre("Nouveau guide")
                .type(TypeRessource.guide)
                .actif(true)
                .dateAjout(OffsetDateTime.now())
                .build();

        when(repository.save(any(RessourcePedagogique.class))).thenReturn(savedRes);

        RessourcePedagogiqueResponse result = service.creer(request);
        assertNotNull(result);
        assertEquals("Nouveau guide", result.getTitre());
        verify(repository).save(any(RessourcePedagogique.class));
    }

    @Test
    void modifier_shouldUpdateFields() {
        UUID id = UUID.randomUUID();
        RessourcePedagogique mockRes = RessourcePedagogique.builder()
                .id(id)
                .titre("Fiche V1")
                .type(TypeRessource.fiche)
                .actif(true)
                .dateAjout(OffsetDateTime.now())
                .build();

        RessourcePedagogiqueRequest request = RessourcePedagogiqueRequest.builder()
                .titre("Fiche V2")
                .type(TypeRessource.video)
                .actif(false)
                .build();

        when(repository.findById(id)).thenReturn(Optional.of(mockRes));
        when(repository.save(any(RessourcePedagogique.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RessourcePedagogiqueResponse result = service.modifier(id, request);
        assertNotNull(result);
        assertEquals("Fiche V2", result.getTitre());
        assertEquals(TypeRessource.video, result.getType());
        assertFalse(result.isActif());
    }

    @Test
    void supprimer_asAdmin_shouldDelete() {
        UUID id = UUID.randomUUID();
        RessourcePedagogique mockRes = RessourcePedagogique.builder()
                .id(id)
                .build();

        when(repository.findById(id)).thenReturn(Optional.of(mockRes));

        assertDoesNotThrow(() -> service.supprimer(id, adminJwt));
        verify(repository).delete(mockRes);
    }

    @Test
    void supprimer_asComiteOrTeacher_shouldThrowAccessDenied() {
        UUID id = UUID.randomUUID();

        assertThrows(AccessDeniedException.class, () -> service.supprimer(id, comiteJwt));
        assertThrows(AccessDeniedException.class, () -> service.supprimer(id, teacherJwt));
        verify(repository, never()).delete(any());
    }
}
