package com.example.demo.repository;

import com.example.demo.model.Documento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentoRepository extends JpaRepository<Documento, Long> {
    List<Documento> findByOrganizzazioneId(Long organizzazioneId);
    List<Documento> findByOrganizzazioneIdAndElaborato(Long organizzazioneId, Boolean elaborato);
}