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

    private final AbonnementRepository abonnementRepository;
    private final ActualiteRepository actualiteRepository;
    private final CarteAConteRepository carteAConteRepository;
    private final ClasseRepository classeRepository;
    private final CommandeRepository commandeRepository;
    private final ConteDanseRepository conteDanseRepository;
    private final DemandeContactRepository demandeContactRepository;
    private final EcoleRepository ecoleRepository;
    private final FicheActiviteRepository ficheActiviteRepository;
    private final OffreAbonnementRepository offreAbonnementRepository;
    private final PageCarnetLectureRepository pageCarnetLectureRepository;
    private final PageCarnetVoyageRepository pageCarnetVoyageRepository;
    private final PartenaireRepository partenaireRepository;
    private final ProduitRepository produitRepository;
    private final ProfilEnfantRepository profilEnfantRepository;
    private final RessourcePedagogiqueRepository ressourcePedagogiqueRepository;
    private final UtilisateurRepository utilisateurRepository;

    public StatistiquesService(AbonnementRepository abonnementRepository,
                               ActualiteRepository actualiteRepository,
                               CarteAConteRepository carteAConteRepository,
                               ClasseRepository classeRepository,
                               CommandeRepository commandeRepository,
                               ConteDanseRepository conteDanseRepository,
                               DemandeContactRepository demandeContactRepository,
                               EcoleRepository ecoleRepository,
                               FicheActiviteRepository ficheActiviteRepository,
                               OffreAbonnementRepository offreAbonnementRepository,
                               PageCarnetLectureRepository pageCarnetLectureRepository,
                               PageCarnetVoyageRepository pageCarnetVoyageRepository,
                               PartenaireRepository partenaireRepository,
                               ProduitRepository produitRepository,
                               ProfilEnfantRepository profilEnfantRepository,
                               RessourcePedagogiqueRepository ressourcePedagogiqueRepository,
                               UtilisateurRepository utilisateurRepository) {
        this.abonnementRepository = abonnementRepository;
        this.actualiteRepository = actualiteRepository;
        this.carteAConteRepository = carteAConteRepository;
        this.classeRepository = classeRepository;
        this.commandeRepository = commandeRepository;
        this.conteDanseRepository = conteDanseRepository;
        this.demandeContactRepository = demandeContactRepository;
        this.ecoleRepository = ecoleRepository;
        this.ficheActiviteRepository = ficheActiviteRepository;
        this.offreAbonnementRepository = offreAbonnementRepository;
        this.pageCarnetLectureRepository = pageCarnetLectureRepository;
        this.pageCarnetVoyageRepository = pageCarnetVoyageRepository;
        this.partenaireRepository = partenaireRepository;
        this.produitRepository = produitRepository;
        this.profilEnfantRepository = profilEnfantRepository;
        this.ressourcePedagogiqueRepository = ressourcePedagogiqueRepository;
        this.utilisateurRepository = utilisateurRepository;
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

        return new StatistiquesResponse(
                utilisateurRepository.count(),
                utilisateursParRole,
                ecoleRepository.count(),
                classeRepository.count(),
                conteDanseRepository.count(),
                contesParStatut,
                carteAConteRepository.count(),
                cartesParStatut,
                ressourcePedagogiqueRepository.count(),
                ficheActiviteRepository.count(),
                produitRepository.count(),
                commandeRepository.count(),
                commandesParStatut,
                partenaireRepository.count(),
                offreAbonnementRepository.count(),
                abonnementsParStatut,
                profilEnfantRepository.count(),
                pageCarnetLectureRepository.count(),
                pageCarnetLectureRepository.countByStatut(StatutPageCarnet.terminee),
                pageCarnetVoyageRepository.count(),
                pageCarnetVoyageRepository.countByStatut(StatutPageCarnet.terminee),
                actualiteRepository.countByPublieTrue(),
                demandeContactRepository.count(),
                demandeContactRepository.countByTraite(false));
    }
}
