package com.example.demo.service;

import com.example.demo.model.ChunkDocumento;
import com.example.demo.model.Documento;
import com.example.demo.model.Organizzazione;
import com.example.demo.repository.ChunkDocumentoRepository;
import com.example.demo.repository.DocumentoRepository;
import com.example.demo.repository.OrganizzazioneRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RAGService {

    private final ChunkDocumentoRepository chunkDocumentoRepository;
    private final DocumentoRepository documentoRepository;
    private final OrganizzazioneRepository organizzazioneRepository;
    private final AIService aiService;

    @Autowired
    public RAGService(
            ChunkDocumentoRepository chunkDocumentoRepository,
            DocumentoRepository documentoRepository,
            OrganizzazioneRepository organizzazioneRepository,
            AIService aiService) {
        this.chunkDocumentoRepository = chunkDocumentoRepository;
        this.documentoRepository = documentoRepository;
        this.organizzazioneRepository = organizzazioneRepository;
        this.aiService = aiService;
    }

    public String processaQuery(String query, Long organizzazioneId, String contestoConversazione) {
        try {
            // Ottieni informazioni sull'organizzazione
            Organizzazione organizzazione = organizzazioneRepository.findById(organizzazioneId)
                    .orElse(null);

            // 1. Genera embedding per la query
            byte[] queryEmbedding = aiService.generaEmbedding(query);

            // 2. Trova documenti rilevanti
            List<ChunkDocumento> documentiRilevanti = trovaDocumentiRilevanti(queryEmbedding, organizzazioneId, 5);

            // 3. Costruisci il prompt per l'IA
            String prompt = costruisciPrompt(query, documentiRilevanti, contestoConversazione, organizzazione);

            // 4. Genera risposta
            return aiService.generaRisposta(prompt);

        } catch (Exception e) {
            e.printStackTrace();
            return "Mi dispiace, non sono in grado di rispondere a questa domanda al momento.";
        }
    }

    private List<ChunkDocumento> trovaDocumentiRilevanti(byte[] queryEmbedding, Long organizzazioneId, int limite) {
        // Ottieni tutti i chunk dei documenti dell'organizzazione
        List<ChunkDocumento> tuttiChunks = new ArrayList<>();

        // Ottieni tutti i documenti dell'organizzazione
        List<Documento> documenti = documentoRepository.findByOrganizzazioneIdAndElaborato(organizzazioneId, true);

        // Per ogni documento, ottieni i chunk
        for (Documento documento : documenti) {
            tuttiChunks.addAll(chunkDocumentoRepository.findByDocumentoId(documento.getId()));
        }

        // Calcola similarità coseno per ogni chunk
        List<ChunkConPunteggio> chunksConPunteggio = tuttiChunks.stream()
                .map(chunk -> new ChunkConPunteggio(chunk, calcolaSimilaritaCoseno(queryEmbedding, chunk.getEmbedding())))
                .sorted(Comparator.comparing(ChunkConPunteggio::getPunteggio).reversed())
                .limit(limite)
                .collect(Collectors.toList());

        // Estrai solo i chunk
        return chunksConPunteggio.stream()
                .map(ChunkConPunteggio::getChunk)
                .collect(Collectors.toList());
    }

    private float calcolaSimilaritaCoseno(byte[] embedding1, byte[] embedding2) {
        float[] floatArray1 = byteArrayToFloatArray(embedding1);
        float[] floatArray2 = byteArrayToFloatArray(embedding2);

        float prodottoPuntato = 0.0f;
        float norma1 = 0.0f;
        float norma2 = 0.0f;

        for (int i = 0; i < floatArray1.length; i++) {
            prodottoPuntato += floatArray1[i] * floatArray2[i];
            norma1 += floatArray1[i] * floatArray1[i];
            norma2 += floatArray2[i] * floatArray2[i];
        }

        norma1 = (float) Math.sqrt(norma1);
        norma2 = (float) Math.sqrt(norma2);

        if (norma1 == 0 || norma2 == 0) {
            return 0.0f;
        }

        return prodottoPuntato / (norma1 * norma2);
    }

    private float[] byteArrayToFloatArray(byte[] byteArray) {
        float[] floatArray = new float[byteArray.length / 4];

        for (int i = 0; i < floatArray.length; i++) {
            int intBits =
                    (byteArray[i * 4] & 0xFF) |
                            ((byteArray[i * 4 + 1] & 0xFF) << 8) |
                            ((byteArray[i * 4 + 2] & 0xFF) << 16) |
                            ((byteArray[i * 4 + 3] & 0xFF) << 24);

            floatArray[i] = Float.intBitsToFloat(intBits);
        }

        return floatArray;
    }

    private String costruisciPrompt(String query, List<ChunkDocumento> documentiRilevanti,
                                    String contestoConversazione, Organizzazione organizzazione) {
        StringBuilder promptBuilder = new StringBuilder();

        // Istruzioni di base
        promptBuilder.append("Sei un assistente virtuale WhatsApp per ");
        if (organizzazione != null) {
            promptBuilder.append("l'azienda " + organizzazione.getNome() + ". ");

            if (organizzazione.getTonoDiVoce() != null && !organizzazione.getTonoDiVoce().isEmpty()) {
                promptBuilder.append("Utilizza un tono " + organizzazione.getTonoDiVoce() + ". ");
            }
        } else {
            promptBuilder.append("un'azienda. Utilizza un tono professionale ma amichevole. ");
        }

        promptBuilder.append("Rispondi alla domanda dell'utente basandoti esclusivamente sulle informazioni fornite nei documenti sottostanti. ");
        promptBuilder.append("Se non sei in grado di rispondere utilizzando solo queste informazioni, indica che le informazioni non sono disponibili. ");
        promptBuilder.append("Le risposte devono essere concise e dirette, ideali per la lettura su WhatsApp (preferibilmente sotto i 200 caratteri).\n\n");

        // Aggiungi contesto della conversazione se disponibile
        if (contestoConversazione != null && !contestoConversazione.isEmpty()) {
            promptBuilder.append("CONTESTO DELLA CONVERSAZIONE PRECEDENTE:\n");
            promptBuilder.append(contestoConversazione).append("\n\n");
        }

        // Aggiungi documenti rilevanti con punteggio di similarità
        if (!documentiRilevanti.isEmpty()) {
            promptBuilder.append("DOCUMENTI PERTINENTI ALLA DOMANDA:\n");

            for (int i = 0; i < documentiRilevanti.size(); i++) {
                ChunkDocumento chunk = documentiRilevanti.get(i);
                promptBuilder.append("--- DOCUMENTO ").append(i + 1).append(": ");
                promptBuilder.append(chunk.getDocumento().getTitolo()).append(" ---\n");
                promptBuilder.append(chunk.getTestoChunk()).append("\n\n");
            }
        } else {
            promptBuilder.append("Non sono stati trovati documenti pertinenti alla tua domanda nel nostro database. ");
            promptBuilder.append("Risponderò in base alle mie conoscenze generali, ma potrei non avere informazioni specifiche sull'argomento.\n\n");
        }

        // Aggiungi la query dell'utente
        promptBuilder.append("DOMANDA DELL'UTENTE: ").append(query).append("\n\n");

        // Linee guida per la risposta
        promptBuilder.append("LINEE GUIDA PER LA RISPOSTA:\n");
        promptBuilder.append("1. Sii conciso e diretto\n");
        promptBuilder.append("2. Rispondi SOLO in base ai documenti forniti\n");
        promptBuilder.append("3. Se l'informazione non è nei documenti, dillo chiaramente\n");
        promptBuilder.append("4. Non inventare informazioni\n");
        promptBuilder.append("5. Formato adatto a WhatsApp: risposte brevi e facilmente leggibili su mobile\n");
        promptBuilder.append("6. Usa un linguaggio semplice e accessibile\n");

        return promptBuilder.toString();
    }

    // Classe interna per la gestione dei punteggi
    private static class ChunkConPunteggio {
        private final ChunkDocumento chunk;
        private final float punteggio;

        public ChunkConPunteggio(ChunkDocumento chunk, float punteggio) {
            this.chunk = chunk;
            this.punteggio = punteggio;
        }

        public ChunkDocumento getChunk() {
            return chunk;
        }

        public float getPunteggio() {
            return punteggio;
        }
    }
}