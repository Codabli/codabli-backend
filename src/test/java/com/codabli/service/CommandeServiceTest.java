package com.codabli.service;

import com.codabli.dto.AdresseLivraisonResponse;
import com.codabli.dto.CommandeResponse;
import com.codabli.dto.CreerCommandeRequest;
import com.codabli.entity.AdresseLivraison;
import com.codabli.entity.Commande;
import com.codabli.entity.LignePanier;
import com.codabli.entity.Panier;
import com.codabli.entity.Produit;
import com.codabli.entity.Utilisateur;
import com.codabli.entity.enums.TypeProduit;
import com.codabli.repository.AdresseLivraisonRepository;
import com.codabli.repository.CommandeRepository;
import com.codabli.repository.ProduitRepository;
import com.codabli.repository.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CommandeServiceTest {

    @Mock
    private CommandeRepository commandeRepository;

    @Mock
    private ProduitRepository produitRepository;

    @Mock
    private AdresseLivraisonRepository adresseLivraisonRepository;

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @Mock
    private PanierService panierService;

    @InjectMocks
    private CommandeService commandeService;

    @Mock
    private Jwt jwt;

    private Utilisateur mockUtilisateur;
    private Produit mockProduitPhysique;
    private Panier mockPanier;
    private AdresseLivraison mockAdresse;

    @BeforeEach
    void setUp() {
        mockUtilisateur = new Utilisateur();
        mockUtilisateur.setId(UUID.randomUUID());
        mockUtilisateur.setKeycloakId("keycloak-user-123");

        mockProduitPhysique = Produit.builder()
                .id(UUID.randomUUID())
                .nom("Peluche")
                .prix(new BigDecimal("20.00"))
                .type(TypeProduit.produit_physique)
                .stock(10)
                .actif(true)
                .build();

        mockPanier = Panier.builder()
                .id(UUID.randomUUID())
                .utilisateur(mockUtilisateur)
                .lignes(new ArrayList<>())
                .build();

        mockAdresse = AdresseLivraison.builder()
                .id(UUID.randomUUID())
                .utilisateur(mockUtilisateur)
                .nom("Destinataire")
                .adresse("123 rue test")
                .codePostal("75000")
                .ville("Paris")
                .telephone("0102030405")
                .parDefaut(true)
                .build();
    }

    @Test
    void creerCommande_shouldSaveCommandeAndEmptyCart() {
        LignePanier ligne = LignePanier.builder()
                .id(UUID.randomUUID())
                .panier(mockPanier)
                .produit(mockProduitPhysique)
                .quantite(2)
                .build();
        mockPanier.getLignes().add(ligne);

        CreerCommandeRequest request = CreerCommandeRequest.builder()
                .adresseLivraisonId(mockAdresse.getId())
                .build();

        when(jwt.getSubject()).thenReturn("keycloak-user-123");
        when(utilisateurRepository.findByKeycloakId("keycloak-user-123"))
                .thenReturn(Optional.of(mockUtilisateur));
        when(panierService.getOrCreatePanier(mockUtilisateur)).thenReturn(mockPanier);
        when(adresseLivraisonRepository.findById(mockAdresse.getId())).thenReturn(Optional.of(mockAdresse));
        when(produitRepository.save(any(Produit.class))).thenReturn(mockProduitPhysique);
        when(commandeRepository.save(any(Commande.class))).thenAnswer(inv -> {
            Commande c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        CommandeResponse response = commandeService.creerCommande(request, jwt);

        assertNotNull(response);
        assertEquals(new BigDecimal("40.00"), response.getSousTotal());
        assertEquals(new BigDecimal("5.00"), response.getFraisLivraison());
        assertEquals(new BigDecimal("45.00"), response.getTotal());
        assertEquals(8, mockProduitPhysique.getStock());
        assertTrue(mockPanier.getLignes().isEmpty());
        verify(commandeRepository, times(1)).save(any(Commande.class));
    }

    @Test
    void creerCommande_shouldThrowExceptionIfStockInsufficient() {
        LignePanier ligne = LignePanier.builder()
                .id(UUID.randomUUID())
                .panier(mockPanier)
                .produit(mockProduitPhysique)
                .quantite(15) // stock is 10
                .build();
        mockPanier.getLignes().add(ligne);

        CreerCommandeRequest request = CreerCommandeRequest.builder()
                .adresseLivraisonId(null)
                .build();

        when(jwt.getSubject()).thenReturn("keycloak-user-123");
        when(utilisateurRepository.findByKeycloakId("keycloak-user-123"))
                .thenReturn(Optional.of(mockUtilisateur));
        when(panierService.getOrCreatePanier(mockUtilisateur)).thenReturn(mockPanier);

        assertThrows(StockInsuffisantException.class, () -> commandeService.creerCommande(request, jwt));
        assertEquals(10, mockProduitPhysique.getStock()); // unchanged
    }

    @Test
    void getCommandeById_shouldRestrictAccessToOwner() {
        UUID id = UUID.randomUUID();
        Commande commande = Commande.builder()
                .id(id)
                .utilisateur(new Utilisateur()) // Another user
                .lignes(Collections.emptyList())
                .build();
        commande.getUtilisateur().setId(UUID.randomUUID());

        when(jwt.getSubject()).thenReturn("keycloak-user-123");
        when(utilisateurRepository.findByKeycloakId("keycloak-user-123"))
                .thenReturn(Optional.of(mockUtilisateur));
        when(commandeRepository.findById(id)).thenReturn(Optional.of(commande));

        assertThrows(AccessDeniedException.class, () -> commandeService.getCommandeById(id, jwt));
    }
}
