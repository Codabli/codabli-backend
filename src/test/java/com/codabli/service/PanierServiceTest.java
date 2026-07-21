package com.codabli.service;

import com.codabli.dto.AjoutPanierRequest;
import com.codabli.dto.ModifQuantiteRequest;
import com.codabli.dto.PanierResponse;
import com.codabli.entity.LignePanier;
import com.codabli.entity.Panier;
import com.codabli.entity.Produit;
import com.codabli.entity.Utilisateur;
import com.codabli.entity.enums.TypeProduit;
import com.codabli.repository.PanierRepository;
import com.codabli.repository.ProduitRepository;
import com.codabli.repository.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PanierServiceTest {

    @Mock
    private PanierRepository panierRepository;

    @Mock
    private ProduitRepository produitRepository;

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @InjectMocks
    private PanierService panierService;

    @Mock
    private Jwt jwt;

    private Utilisateur mockUtilisateur;
    private Produit mockProduit;
    private Panier mockPanier;

    @BeforeEach
    void setUp() {
        mockUtilisateur = new Utilisateur();
        mockUtilisateur.setId(UUID.randomUUID());
        mockUtilisateur.setKeycloakId("keycloak-user-123");

        mockProduit = Produit.builder()
                .id(UUID.randomUUID())
                .nom("Livre Enfant")
                .prix(new BigDecimal("10.00"))
                .type(TypeProduit.produit_physique)
                .stock(100)
                .actif(true)
                .build();

        mockPanier = Panier.builder()
                .id(UUID.randomUUID())
                .utilisateur(mockUtilisateur)
                .lignes(new ArrayList<>())
                .build();
    }

    @Test
    void getPanier_shouldReturnExistingPanier() {
        when(jwt.getSubject()).thenReturn("keycloak-user-123");
        when(utilisateurRepository.findByKeycloakId("keycloak-user-123"))
                .thenReturn(Optional.of(mockUtilisateur));
        when(panierRepository.findByUtilisateurId(mockUtilisateur.getId()))
                .thenReturn(Optional.of(mockPanier));

        PanierResponse response = panierService.getPanier(jwt);

        assertNotNull(response);
        assertEquals(mockPanier.getId(), response.getId());
        assertEquals(0, response.getLignes().size());
        assertEquals(BigDecimal.ZERO, response.getTotal());
    }

    @Test
    void ajouterProduit_shouldAddNewLineItem() {
        AjoutPanierRequest request = AjoutPanierRequest.builder()
                .produitId(mockProduit.getId())
                .quantite(2)
                .build();

        when(jwt.getSubject()).thenReturn("keycloak-user-123");
        when(utilisateurRepository.findByKeycloakId("keycloak-user-123"))
                .thenReturn(Optional.of(mockUtilisateur));
        when(panierRepository.findByUtilisateurId(mockUtilisateur.getId()))
                .thenReturn(Optional.of(mockPanier));
        when(produitRepository.findById(mockProduit.getId())).thenReturn(Optional.of(mockProduit));
        when(panierRepository.save(any(Panier.class))).thenReturn(mockPanier);

        PanierResponse response = panierService.ajouterProduit(request, jwt);

        assertNotNull(response);
        assertEquals(1, mockPanier.getLignes().size());
        assertEquals(2, mockPanier.getLignes().get(0).getQuantite());
        assertEquals(new BigDecimal("20.00"), response.getTotal());
    }

    @Test
    void ajouterProduit_shouldIncrementQuantityIfAlreadyExists() {
        LignePanier existingLigne = LignePanier.builder()
                .id(UUID.randomUUID())
                .panier(mockPanier)
                .produit(mockProduit)
                .quantite(3)
                .build();
        mockPanier.getLignes().add(existingLigne);

        AjoutPanierRequest request = AjoutPanierRequest.builder()
                .produitId(mockProduit.getId())
                .quantite(2)
                .build();

        when(jwt.getSubject()).thenReturn("keycloak-user-123");
        when(utilisateurRepository.findByKeycloakId("keycloak-user-123"))
                .thenReturn(Optional.of(mockUtilisateur));
        when(panierRepository.findByUtilisateurId(mockUtilisateur.getId()))
                .thenReturn(Optional.of(mockPanier));
        when(produitRepository.findById(mockProduit.getId())).thenReturn(Optional.of(mockProduit));
        when(panierRepository.save(any(Panier.class))).thenReturn(mockPanier);

        PanierResponse response = panierService.ajouterProduit(request, jwt);

        assertNotNull(response);
        assertEquals(1, mockPanier.getLignes().size());
        assertEquals(5, mockPanier.getLignes().get(0).getQuantite());
        assertEquals(new BigDecimal("50.00"), response.getTotal());
    }

    @Test
    void modifierQuantite_shouldUpdateQuantity() {
        UUID ligneId = UUID.randomUUID();
        LignePanier existingLigne = LignePanier.builder()
                .id(ligneId)
                .panier(mockPanier)
                .produit(mockProduit)
                .quantite(1)
                .build();
        mockPanier.getLignes().add(existingLigne);

        ModifQuantiteRequest request = ModifQuantiteRequest.builder()
                .quantite(4)
                .build();

        when(jwt.getSubject()).thenReturn("keycloak-user-123");
        when(utilisateurRepository.findByKeycloakId("keycloak-user-123"))
                .thenReturn(Optional.of(mockUtilisateur));
        when(panierRepository.findByUtilisateurId(mockUtilisateur.getId()))
                .thenReturn(Optional.of(mockPanier));
        when(panierRepository.save(any(Panier.class))).thenReturn(mockPanier);

        PanierResponse response = panierService.modifierQuantite(ligneId, request, jwt);

        assertNotNull(response);
        assertEquals(4, mockPanier.getLignes().get(0).getQuantite());
        assertEquals(new BigDecimal("40.00"), response.getTotal());
    }

    @Test
    void supprimerLigne_shouldRemoveItem() {
        UUID ligneId = UUID.randomUUID();
        LignePanier existingLigne = LignePanier.builder()
                .id(ligneId)
                .panier(mockPanier)
                .produit(mockProduit)
                .quantite(1)
                .build();
        mockPanier.getLignes().add(existingLigne);

        when(jwt.getSubject()).thenReturn("keycloak-user-123");
        when(utilisateurRepository.findByKeycloakId("keycloak-user-123"))
                .thenReturn(Optional.of(mockUtilisateur));
        when(panierRepository.findByUtilisateurId(mockUtilisateur.getId()))
                .thenReturn(Optional.of(mockPanier));
        when(panierRepository.save(any(Panier.class))).thenReturn(mockPanier);

        PanierResponse response = panierService.supprimerLigne(ligneId, jwt);

        assertNotNull(response);
        assertTrue(mockPanier.getLignes().isEmpty());
    }

    @Test
    void viderPanier_shouldClearAllItems() {
        LignePanier existingLigne = LignePanier.builder()
                .id(UUID.randomUUID())
                .panier(mockPanier)
                .produit(mockProduit)
                .quantite(1)
                .build();
        mockPanier.getLignes().add(existingLigne);

        when(jwt.getSubject()).thenReturn("keycloak-user-123");
        when(utilisateurRepository.findByKeycloakId("keycloak-user-123"))
                .thenReturn(Optional.of(mockUtilisateur));
        when(panierRepository.findByUtilisateurId(mockUtilisateur.getId()))
                .thenReturn(Optional.of(mockPanier));
        when(panierRepository.save(any(Panier.class))).thenReturn(mockPanier);

        assertDoesNotThrow(() -> panierService.viderPanier(jwt));
        assertTrue(mockPanier.getLignes().isEmpty());
    }
}
