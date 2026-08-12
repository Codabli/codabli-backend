package com.codabli.service;

import com.codabli.dto.StatistiquesResponse;
import com.codabli.entity.enums.RoleUtilisateur;
import com.codabli.entity.enums.StatutAbonnement;
import com.codabli.entity.enums.StatutCommande;
import com.codabli.entity.enums.StatutConte;
import com.codabli.entity.enums.StatutModeration;
import com.codabli.entity.enums.StatutPageCarnet;
import com.codabli.repository.AbonnementRepository;
import com.codabli.repository.ActualiteRepository;
import com.codabli.repository.CarteAConteRepository;
import com.codabli.repository.ClasseRepository;
import com.codabli.repository.CommandeRepository;
import com.codabli.repository.ConteDanseRepository;
import com.codabli.repository.DemandeContactRepository;
import com.codabli.repository.EcoleRepository;
import com.codabli.repository.FicheActiviteRepository;
import com.codabli.repository.OffreAbonnementRepository;
import com.codabli.repository.PageCarnetLectureRepository;
import com.codabli.repository.PageCarnetVoyageRepository;
import com.codabli.repository.PartenaireRepository;
import com.codabli.repository.ProduitRepository;
import com.codabli.repository.ProfilEnfantRepository;
import com.codabli.repository.RessourcePedagogiqueRepository;
import com.codabli.repository.UtilisateurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Statistiques agregees pour le tableau de bord administrateur (CDC
 * section 15). Uniquement des compteurs globaux, aucune donnee
 * individuelle sur un utilisateur ou un enfant.
 */
@Service
@Transactional(readOnly = true)
public class StatistiquesService {

    private final UtilisateurRepository utilisateurRepository;
    private final EcoleRepository ecoleRepository;
    private final ClasseRepository classeRepository;
    private final ConteDanseRepository conteDanseRepository;
    private final CarteAConteRepository carteAConteRepository;
    private final RessourcePedagogiqueRepository ressourcePedagogiqueRepository;
    private final FicheActiviteRepository ficheActiviteRepository;
    private final ProduitRepository produitRepository;
    private final CommandeRepository commandeRepository;
    private final PartenaireRepository partenaireRepository;
    private final OffreAbonnementRepository offreAbonnementRepository;
    private final AbonnementRepository abonnementRepository;
    private final ProfilEnfantRepository profilEnfantRepository;
    private final PageCarnetLectureRepository pageCarnetLectureRepository;
    private final PageCarnetVoyageRepository pageCarnetVoyageRepository;
    private final ActualiteRepository actualiteRepository;
    private final DemandeContactRepository demandeContactRepository;

    public StatistiquesService(UtilisateurRepository utilisateurRepository,
            EcoleRepository ecoleRepository,
            ClasseRepository classeRepository,
            ConteDanseRepository conteDanseRepository,
            CarteAConteRepository carteAConteRepository,
            RessourcePedagogiqueRepository ressourcePedagogiqueRepository,
            FicheActiviteRepository ficheActiviteRepository,
            ProduitRepository produitRepository,
            CommandeRepository commandeRepository,
            PartenaireRepository partenaireRepository,
            OffreAbonnementRepository offreAbonnementRepository,
            AbonnementRepository abonnementRepository,
            ProfilEnfantRepository profilEnfantRepository,
            PageCarnetLectureRepository pageCarnetLectureRepository,
            PageCarnetVoyageRepository pageCarnetVoyageRepository,
            ActualiteRepository actualiteRepository,
            DemandeContactRepository demandeContactRepository) {
        this.utilisateurRepository = utilisateurRepository;
        this.ecoleRepository = ecoleRepository;
        this.classeRepository = classeRepository;
        this.conteDanseRepository = conteDanseRepository;
        this.carteAConteRepository = carteAConteRepository;
        this.ressourcePedagogiqueRepository = ressourcePedagogiqueRepository;
        this.ficheActiviteRepository = ficheActiviteRepository;
        this.produitRepository = produitRepository;
        this.commandeRepository = commandeRepository;
        this.partenaireRepository = partenaireRepository;
        this.offreAbonnementRepository = offreAbonnementRepository;
        this.abonnementRepository = abonnementRepository;
        this.profilEnfantRepository = profilEnfantRepository;
        this.pageCarnetLectureRepository = pageCarnetLectureRepository;
        this.pageCarnetVoyageRepository = pageCarnetVoyageRepository;
        this.actualiteRepository = actualiteRepository;
        this.demandeContactRepository = demandeContactRepository;
    }

    public StatistiquesResponse consulter() {
        Map<String, Long> utilisateursParRole = new LinkedHashMap<>();
        for (RoleUtilisateur role : RoleUtilisateur.values()) {
            utilisateursParRole.put(role.name(), utilisateurRepository.countByRole(role));
        }

        Map<String, Long> contesParStatut = new LinkedHashMap<>();
        for (StatutConte statut : StatutConte.values()) {
            contesParStatut.put(statut.name(), conteDanseRepository.countByStatut(statut));
        }

        Map<String, Long> cartesParStatut = new LinkedHashMap<>();
        for (StatutModeration statut : StatutModeration.values()) {
            cartesParStatut.put(statut.name(), carteAConteRepository.countByStatutModeration(statut));
        }

        Map<String, Long> commandesParStatut = new LinkedHashMap<>();
        for (StatutCommande statut : StatutCommande.values()) {
            commandesParStatut.put(statut.name(), commandeRepository.countByStatut(statut));
        }

        Map<String, Long> abonnementsParStatut = new LinkedHashMap<>();
        for (StatutAbonnement statut : StatutAbonnement.values()) {
            abonnementsParStatut.put(statut.name(), abonnementRepository.countByStatut(statut));
        }

        return StatistiquesResponse.builder()
                .totalUtilisateurs(utilisateurRepository.count())
                .utilisateursParRole(utilisateursParRole)
                .totalEcoles(ecoleRepository.count())
                .totalClasses(classeRepository.count())
                .totalContesDanses(conteDanseRepository.count())
                .contesDansesParStatut(contesParStatut)
                .totalCartesAConte(carteAConteRepository.count())
                .cartesAConteParStatut(cartesParStatut)
                .totalRessourcesPedagogiques(ressourcePedagogiqueRepository.count())
                .totalFichesActivites(ficheActiviteRepository.count())
                .totalProduits(produitRepository.count())
                .totalCommandes(commandeRepository.count())
                .commandesParStatut(commandesParStatut)
                .totalPartenaires(partenaireRepository.count())
                .totalOffresAbonnement(offreAbonnementRepository.count())
                .abonnementsParStatut(abonnementsParStatut)
                .totalProfilsEnfants(profilEnfantRepository.count())
                .totalPagesCarnetLecture(pageCarnetLectureRepository.count())
                .pagesCarnetLectureTerminees(pageCarnetLectureRepository.countByStatut(StatutPageCarnet.terminee))
                .totalPagesCarnetVoyage(pageCarnetVoyageRepository.count())
                .pagesCarnetVoyageTerminees(pageCarnetVoyageRepository.countByStatut(StatutPageCarnet.terminee))
                .totalActualitesPubliees(actualiteRepository.countByPublieTrue())
                .totalDemandesContact(demandeContactRepository.count())
                .demandesContactNonTraitees(demandeContactRepository.countByTraite(false))
                .build();
    }
}
