package com.codabli.service;

import com.codabli.entity.ConteDanse;
import com.codabli.entity.Utilisateur;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.UUID;

@Service
public class ConteDanseAccesService {

    private final ConteDanseService conteDanseService;
    private final UtilisateurService utilisateurService;

    public ConteDanseAccesService(ConteDanseService conteDanseService, UtilisateurService utilisateurService) {
        this.conteDanseService = conteDanseService;
        this.utilisateurService = utilisateurService;
    }

    public ConteDanse verifierAcces(UUID conteDanseId, Jwt jwt) {
        return verifierAcces(conteDanseService.findById(conteDanseId), jwt);
    }

    public ConteDanse verifierAcces(ConteDanse conte, Jwt jwt) {
        Utilisateur utilisateur = utilisateurService.getUtilisateurFromJwt(jwt);
        Collection<String> roles = utilisateurService.extractRoles(jwt);

        boolean estAuteur = conte.getCreateur().getId().equals(utilisateur.getId());
        boolean estAdmin = roles.contains("admin") || roles.contains("super_admin") || roles.contains("moderateur");

        if (!estAuteur && !estAdmin) {
            throw new AccessDeniedException("Vous ne pouvez acceder qu'aux contes danses autorises");
        }
        return conte;
    }
}
