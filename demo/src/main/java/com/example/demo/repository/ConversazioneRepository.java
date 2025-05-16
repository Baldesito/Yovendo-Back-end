package com.example.demo.repository;

import com.example.demo.model.Conversazione;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversazioneRepository extends JpaRepository<Conversazione, Long> {
    List<Conversazione> findByOrganizzazioneId(Long organizzazioneId);
    List<Conversazione> findByTelefonoClienteAndStato(String telefonoCliente, String stato);
    Optional<Conversazione> findByTelefonoClienteAndStatoOrderByOrarioInizioDesc(String telefonoCliente, String stato);
}