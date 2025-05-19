package com.example.demo.repository;

import com.example.demo.model.Messaggio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MessaggioRepository extends JpaRepository<Messaggio, Long> {
    List<Messaggio> findByConversazioneIdOrderByOrarioInvioAsc(Long conversazioneId);
    Optional<Messaggio> findFirstByConversazioneIdOrderByOrarioInvioDesc(Long conversazioneId);
    Long countByConversazioneId(Long conversazioneId);
    Long countByOrarioInvioAfter(LocalDateTime dateTime);
    Long countByConversazione_OrganizzazioneId(Long organizzazioneId);
}