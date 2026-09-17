package com.codabli.service;

import com.codabli.dto.ProduitRequest;
import com.codabli.dto.ProduitResponse;
import com.codabli.entity.Produit;
import com.codabli.entity.enums.TypeProduit;
import com.codabli.repository.ProduitRepository;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProduitServiceTest {

    @Mock
    private ProduitRepository produitRepository;

    @InjectMocks
    private ProduitService produitService;

    private Produit produitPhysique;

    @BeforeEach
    void setUp() {
        produitPhysique = Produit.builder()
                .id(UUID.randomUUID())
                .nom("Livre Physique")
                .description("Un beau conte illustre")
                .prix(new BigDecimal("15.99"))
                .type(TypeProduit.produit_physique)
                .stock(50)
                .actif(true)
                .build();
    }

    @Test
    void lister_shouldReturnFilteredPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Produit> page = new PageImpl<>(List.of(produitPhysique));

        when(produitRepository.findByTypeAndActifTrue(TypeProduit.produit_physique, pageable))
                .thenReturn(page);

        Page<ProduitResponse> result = produitService.lister(TypeProduit.produit_physique, true, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Livre Physique", result.getContent().getFirst().nom());
        verify(produitRepository, times(1)).findByTypeAndActifTrue(TypeProduit.produit_physique, pageable);
    }

    @Test
    void getById_shouldReturnResponse() {
        UUID id = produitPhysique.getId();
        when(produitRepository.findById(id)).thenReturn(Optional.of(produitPhysique));

        ProduitResponse response = produitService.getById(id);

        assertNotNull(response);
        assertEquals("Livre Physique", response.nom());
        assertEquals(new BigDecimal("15.99"), response.prix());
    }

    @Test
    void getById_shouldThrowExceptionWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(produitRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> produitService.getById(id));
    }

    @Test
    void creer_shouldSaveAndReturnResponse() {
        ProduitRequest request = produitRequest("Nouveau Produit", new BigDecimal("29.90"), 100, true);

        when(produitRepository.save(any(Produit.class))).thenAnswer(invocation -> {
            Produit toSave = invocation.getArgument(0);
            toSave.setId(UUID.randomUUID());
            return toSave;
        });

        ProduitResponse response = produitService.creer(request);

        assertNotNull(response);
        assertNotNull(response.id());
        assertEquals("Nouveau Produit", response.nom());
        assertEquals(new BigDecimal("29.90"), response.prix());
        verify(produitRepository, times(1)).save(any(Produit.class));
    }

    @Test
    void modifier_shouldUpdateAndSave() {
        UUID id = produitPhysique.getId();
        ProduitRequest request = produitRequest("Nom Modifie", new BigDecimal("19.99"), 40, false);

        when(produitRepository.findById(id)).thenReturn(Optional.of(produitPhysique));
        when(produitRepository.save(any(Produit.class))).thenReturn(produitPhysique);

        ProduitResponse response = produitService.modifier(id, request);

        assertNotNull(response);
        assertEquals("Nom Modifie", response.nom());
        assertEquals(new BigDecimal("19.99"), response.prix());
        assertFalse(response.actif());
        verify(produitRepository, times(1)).save(produitPhysique);
    }

    @Test
    void supprimer_shouldDeleteWhenExists() {
        UUID id = produitPhysique.getId();
        when(produitRepository.findById(id)).thenReturn(Optional.of(produitPhysique));
        doNothing().when(produitRepository).delete(produitPhysique);

        assertDoesNotThrow(() -> produitService.supprimer(id));

        verify(produitRepository, times(1)).delete(produitPhysique);
    }

    private ProduitRequest produitRequest(String nom, BigDecimal prix, Integer stock, Boolean actif) {
        return new ProduitRequest(nom, null, prix, null, TypeProduit.produit_physique, stock, actif);
    }
}
