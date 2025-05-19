package com.example.demo.service;

import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Documento;
import com.example.demo.model.ChunkDocumento;
import com.example.demo.model.Organizzazione;
import com.example.demo.repository.ChunkDocumentoRepository;
import com.example.demo.repository.ConversazioneRepository;
import com.example.demo.repository.DocumentoRepository;
import com.example.demo.repository.OrganizzazioneRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DocumentoService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    private final ConversazioneRepository conversazioneRepository;
    private final DocumentoRepository documentoRepository;
    private final ChunkDocumentoRepository chunkDocumentoRepository;
    private final OrganizzazioneRepository organizzazioneRepository;
    private final AIService aiService;
    private final WhatsAppService whatsAppService;

    @Autowired
    public DocumentoService(
            ConversazioneRepository conversazioneRepository, DocumentoRepository documentoRepository,
            ChunkDocumentoRepository chunkDocumentoRepository,
            OrganizzazioneRepository organizzazioneRepository,
            AIService aiService,
            WhatsAppService whatsAppService) {
        this.conversazioneRepository = conversazioneRepository;
        this.documentoRepository = documentoRepository;
        this.chunkDocumentoRepository = chunkDocumentoRepository;
        this.organizzazioneRepository = organizzazioneRepository;
        this.aiService = aiService;
        this.whatsAppService = whatsAppService;
    }

    /**
     * Salva un documento caricato tramite form/API
     */
    public Documento salvaDocumento(MultipartFile file, String titolo, Long organizzazioneId) throws IOException {
        Organizzazione organizzazione = organizzazioneRepository.findById(organizzazioneId)
                .orElseThrow(() -> new ResourceNotFoundException("Organizzazione", "id", organizzazioneId));

        // Crea directory se non esiste
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Genera nome file univoco
        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(fileName);

        // Salva il file
        Files.copy(file.getInputStream(), filePath);

        // Crea e salva documento
        Documento documento = new Documento();
        documento.setTitolo(titolo);
        documento.setOrganizzazione(organizzazione);
        documento.setPercorsoFile(filePath.toString());
        documento.setTipoContenuto(file.getContentType());
        documento.setStatoElaborazione("ricevuto");

        // Aggiungi metadati base
        Map<String, Object> metadati = new HashMap<>();
        metadati.put("originalFileName", file.getOriginalFilename());
        metadati.put("fileSize", file.getSize());
        metadati.put("uploadedBy", "webUI");
        documento.setMetadati(metadati);  // Utilizziamo il nuovo setter che accetta direttamente una Map

        Documento documentoSalvato = documentoRepository.save(documento);

        // Avvia elaborazione asincrona
        elaboraDocumento(documentoSalvato.getId());

        return documentoSalvato;
    }

    /**
     * Crea un documento a partire da un file già presente sul disco
     */
    public Documento creaDocumentoDaPath(String percorsoFile, String titolo, String tipoContenuto,
                                         Long organizzazioneId, Long conversazioneId) throws IOException {
        try {
            Organizzazione organizzazione = organizzazioneRepository.findById(organizzazioneId)
                    .orElseThrow(() -> new ResourceNotFoundException("Organizzazione", "id", organizzazioneId));

            // Crea e salva documento
            Documento documento = new Documento();
            documento.setTitolo(titolo);
            documento.setOrganizzazione(organizzazione);
            documento.setPercorsoFile(percorsoFile);
            documento.setTipoContenuto(tipoContenuto);
            documento.setStatoElaborazione("ricevuto");

            // Aggiungi metadati con conversazione
            Map<String, Object> metadati = new HashMap<>();
            if (conversazioneId != null) {
                metadati.put("conversazioneId", conversazioneId.toString());
                metadati.put("fonte", "whatsapp");
            }

            // Imposta i metadati direttamente come mappa
            documento.setMetadati(metadati);

            // Salva il documento
            Documento documentoSalvato = documentoRepository.save(documento);

            // Avvia elaborazione asincrona
            elaboraDocumento(documentoSalvato.getId());

            return documentoSalvato;
        } catch (Exception e) {
            System.err.println("Errore nell'elaborazione dell'allegato: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Ottiene i documenti di un'organizzazione con possibilità di filtrare per stato elaborazione
     */
    public List<Documento> getDocumentiByOrganizzazione(Long organizzazioneId, boolean soloElaborati) {
        if (soloElaborati) {
            return documentoRepository.findByOrganizzazioneIdAndElaborato(organizzazioneId, true);
        } else {
            return documentoRepository.findByOrganizzazioneId(organizzazioneId);
        }
    }

    @Async
    public void elaboraDocumento(Long documentoId) {
        try {
            Documento documento = documentoRepository.findById(documentoId)
                    .orElseThrow(() -> new ResourceNotFoundException("Documento", "id", documentoId));

            // Aggiorna stato
            documento.setStatoElaborazione("in-elaborazione");
            documentoRepository.save(documento);

            // Estrai testo in base al tipo di documento
            String testoCompleto = "";
            String tipoContenuto = documento.getTipoContenuto().toLowerCase();

            if (tipoContenuto.contains("pdf")) {
                testoCompleto = estraiTestoDaPdf(documento.getPercorsoFile());
            } else if (tipoContenuto.contains("plain")) {
                testoCompleto = new String(Files.readAllBytes(Paths.get(documento.getPercorsoFile())));
            } else if (tipoContenuto.contains("word") ||
                    documento.getPercorsoFile().endsWith(".doc") ||
                    documento.getPercorsoFile().endsWith(".docx")) {
                testoCompleto = estraiTestoDaWord(documento.getPercorsoFile());
            } else if (tipoContenuto.contains("excel") ||
                    documento.getPercorsoFile().endsWith(".xls") ||
                    documento.getPercorsoFile().endsWith(".xlsx")) {
                testoCompleto = estraiTestoDaExcel(documento.getPercorsoFile());
            } else {
                // Altri formati di documento
                documento.setStatoElaborazione("formato-non-supportato");
                documentoRepository.save(documento);
                return;
            }

            // Suddividi in chunk
            List<String> chunks = suddividiInChunkSemantici(testoCompleto, 1000);

            // Genera e salva embedding per ogni chunk
            for (int i = 0; i < chunks.size(); i++) {
                String chunk = chunks.get(i);
                byte[] embedding = aiService.generaEmbedding(chunk);

                ChunkDocumento chunkDocumento = new ChunkDocumento();
                chunkDocumento.setDocumento(documento);
                chunkDocumento.setIndiceChunk(i);
                chunkDocumento.setTestoChunk(chunk);
                chunkDocumento.setEmbedding(embedding);

                chunkDocumentoRepository.save(chunkDocumento);
            }

            // Aggiorna stato documento
            documento.setElaborato(true);
            documento.setStatoElaborazione("completato");
            documentoRepository.save(documento);

            // Controlla se c'è una conversazione associata per notificare l'utente
            try {
                Map<String, Object> metadati = documento.getMetadati();  // Ora getMetadati() restituisce direttamente una Map
                if (metadati != null && metadati.containsKey("conversazioneId") && metadati.containsKey("fonte")) {
                    if ("whatsapp".equals(metadati.get("fonte"))) {
                        String conversazioneId = (String) metadati.get("conversazioneId");
                        conversazioneRepository.findById(Long.parseLong(conversazioneId)).ifPresent(conversazione -> {
                            String numeroCliente = conversazione.getTelefonoCliente();
                            whatsAppService.inviaMessaggioTest(numeroCliente,
                                    "✅ Il tuo documento '" + documento.getTitolo() + "' è stato elaborato con successo! " +
                                            "Ora puoi farmi domande specifiche su questo documento.");
                        });
                    }
                }
            } catch (Exception e) {
                System.err.println("Errore nell'invio della notifica di completamento: " + e.getMessage());
            }

        } catch (Exception e) {
            Documento documento = documentoRepository.findById(documentoId).orElse(null);
            if (documento != null) {
                documento.setStatoElaborazione("errore");
                documentoRepository.save(documento);
            }
            e.printStackTrace();
        }
    }

    private String estraiTestoDaPdf(String percorsoFile) throws IOException {
        try (PDDocument document = PDDocument.load(new File(percorsoFile))) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String estraiTestoDaWord(String percorsoFile) throws IOException {
        // Questo è un esempio che dovrà essere implementato con Apache POI
        // Richiederebbe le dipendenze org.apache.poi:poi e poi-ooxml nel pom.xml
        return "Estrazione testo da Word non implementata";
    }

    private String estraiTestoDaExcel(String percorsoFile) throws IOException {
        // Questo è un esempio che dovrà essere implementato con Apache POI
        // Richiederebbe le dipendenze org.apache.poi:poi e poi-ooxml nel pom.xml
        return "Estrazione testo da Excel non implementata";
    }

    private List<String> suddividiInChunkSemantici(String testo, int dimensioneChunk) {
        List<String> chunks = new ArrayList<>();

        // Divisione per paragrafi
        String[] paragrafi = testo.split("\n\n");

        StringBuilder currentChunk = new StringBuilder();

        for (String paragrafo : paragrafi) {
            // Se il paragrafo da solo è già troppo grande
            if (paragrafo.length() > dimensioneChunk) {
                // Prima salva ciò che già abbiamo nel chunk corrente
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString());
                    currentChunk = new StringBuilder();
                }

                // Poi dividi il paragrafo grande in frasi
                String[] frasi = paragrafo.split("(?<=[.!?])\\s+");

                StringBuilder tempChunk = new StringBuilder();
                for (String frase : frasi) {
                    if (tempChunk.length() + frase.length() > dimensioneChunk) {
                        // Se aggiungere questa frase supera la dimensione massima
                        if (tempChunk.length() > 0) {
                            chunks.add(tempChunk.toString());
                            tempChunk = new StringBuilder();
                        }

                        // Se la frase stessa è troppo lunga, dividiamo per parole
                        if (frase.length() > dimensioneChunk) {
                            String[] parole = frase.split("\\s+");
                            tempChunk = new StringBuilder();
                            for (String parola : parole) {
                                if (tempChunk.length() + parola.length() + 1 > dimensioneChunk) {
                                    chunks.add(tempChunk.toString());
                                    tempChunk = new StringBuilder();
                                }
                                if (tempChunk.length() > 0) tempChunk.append(" ");
                                tempChunk.append(parola);
                            }
                            if (tempChunk.length() > 0) {
                                chunks.add(tempChunk.toString());
                                tempChunk = new StringBuilder();
                            }
                        } else {
                            // Altrimenti la frase è abbastanza corta da essere un chunk a sé
                            chunks.add(frase);
                        }
                    } else {
                        // Aggiungi la frase al chunk temporaneo
                        tempChunk.append(frase).append(" ");
                    }
                }

                if (tempChunk.length() > 0) {
                    chunks.add(tempChunk.toString());
                }
            } else {
                // Paragrafo abbastanza piccolo, aggiungiamolo al chunk corrente se c'è spazio
                if (currentChunk.length() + paragrafo.length() + 2 > dimensioneChunk) {
                    // Non c'è spazio, salviamo il chunk corrente e iniziamone uno nuovo
                    chunks.add(currentChunk.toString());
                    currentChunk = new StringBuilder(paragrafo);
                } else {
                    // C'è spazio, aggiungiamo questo paragrafo
                    if (currentChunk.length() > 0) {
                        currentChunk.append("\n\n");
                    }
                    currentChunk.append(paragrafo);
                }
            }
        }

        // Aggiungi l'ultimo chunk se non è vuoto
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString());
        }

        return chunks;
    }
}