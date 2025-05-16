package com.example.demo.repository;

import com.example.demo.model.Utente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UtenteRepository extends JpaRepository<Utente, Long> {
    Optional<Utente> findByNomeUtente(String nomeUtente);
    Optional<Utente> findByEmail(String email);
    List<Utente> findByOrganizzazione_Id(Long organizzazioneId);
}