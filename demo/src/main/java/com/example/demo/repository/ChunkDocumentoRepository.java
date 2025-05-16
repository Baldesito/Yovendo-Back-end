package com.example.demo.repository;

import com.example.demo.model.ChunkDocumento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChunkDocumentoRepository extends JpaRepository<ChunkDocumento, Long> {
    List<ChunkDocumento> findByDocumentoId(Long documentoId);
}