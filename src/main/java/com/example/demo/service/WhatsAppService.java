package com.example.demo.service;

import com.example.demo.config.TwilioConfig;
import com.example.demo.model.Messaggio;
import com.example.demo.repository.ConversazioneRepository;
import com.example.demo.repository.MessaggioRepository;
import com.example.demo.repository.OrganizzazioneRepository;
import com.twilio.rest.api.v2010.account.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class WhatsAppService {

    private final ConversazioneRepository conversazioneRepository;
    private final MessaggioRepository messaggioRepository;
    private final OrganizzazioneRepository organizzazioneRepository;
    private final RAGService ragService;
    private final TwilioConfig twilioConfig;
    private final RestTemplate restTemplate;

    @Value("${twilio.whatsapp.number}")
    private String twilioPhoneNumber;

    @Value("${twilio.account.sid}")
    private String accountSid;

    @Value("${twilio.auth.token}")
    private String authToken;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Autowired
    public WhatsAppService(
            ConversazioneRepository conversazioneRepository,
            MessaggioRepository messaggioRepository,
            OrganizzazioneRepository organizzazioneRepository,
            RAGService ragService,
            TwilioConfig twilioConfig) {
        this.conversazioneRepository = conversazioneRepository;
        this.messaggioRepository = messaggioRepository;
        this.organizzazioneRepository = organizzazioneRepository;
        this.ragService = ragService;
        this.twilioConfig = twilioConfig;
        this.restTemplate = new RestTemplate();
    }

    // Metodo di test per inviare un messaggio direttamente
    public boolean inviaMessaggioTest(String numeroDestinatario, String contenuto) {
        try {
            System.out.println("Test invio messaggio WhatsApp...");
            System.out.println("Da: whatsapp:" + twilioPhoneNumber);
            System.out.println("A: whatsapp:" + numeroDestinatario);
            System.out.println("Contenuto: " + contenuto);

            // Formatta i numeri di telefono
            String whatsappFrom = "whatsapp:" + twilioPhoneNumber;
            String whatsappTo = "whatsapp:" + numeroDestinatario;

            // Stampa credenziali Twilio (parziali per sicurezza)
            try {
                String accountSid = com.twilio.Twilio.getRestClient().getAccountSid();
                System.out.println("Account SID configurato: " +
                        (accountSid != null ? accountSid.substring(0, 5) + "..." : "NULL"));
            } catch (Exception e) {
                System.err.println("Impossibile ottenere informazioni sul client Twilio: " + e.getMessage());
            }

            // Invia il messaggio tramite Twilio
            Message message = Message.creator(
                    new com.twilio.type.PhoneNumber(whatsappTo),
                    new com.twilio.type.PhoneNumber(whatsappFrom),
                    contenuto
            ).create();

            System.out.println("Messaggio di test inviato con successo! SID: " + message.getSid());
            return true;
        } catch (Exception e) {
            System.err.println("Errore nell'invio del messaggio di test: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public void inviaRisposta(Messaggio messaggio) {
        try {
            System.out.println("Preparazione invio risposta WhatsApp...");

            // Prepara il numero di telefono completo del cliente
            String numeroCliente = messaggio.getConversazione().getTelefonoCliente();

            // Per il sandbox WhatsApp, usiamo il formato "whatsapp:+numero"
            String whatsappFrom = "whatsapp:" + twilioPhoneNumber;
            String whatsappTo = "whatsapp:" + numeroCliente;

            System.out.println("Invio da: " + whatsappFrom + " a: " + whatsappTo);
            System.out.println("Contenuto: " + messaggio.getContenuto());

            // Invia il messaggio tramite Twilio
            Message message = Message.creator(
                    new com.twilio.type.PhoneNumber(whatsappTo),
                    new com.twilio.type.PhoneNumber(whatsappFrom),
                    messaggio.getContenuto()
            ).create();

            System.out.println("Messaggio inviato con successo, SID: " + message.getSid());

            // Aggiorna il messaggio con l'ID di Twilio
            Map<String, Object> metadati = new HashMap<>();
            metadati.put("twilioMessageSid", message.getSid());

            // Impostare il campo documentiRiferiti come Map, non come stringa
            messaggio.setDocumentiRiferiti(metadati);
            messaggioRepository.save(messaggio);

            System.out.println("Messaggio aggiornato con metadati Twilio");

        } catch (Exception e) {
            System.err.println("Errore nell'invio del messaggio WhatsApp: " + e.getMessage());
            e.printStackTrace();

            // Aggiungi dettagli sulla causa dell'errore
            if (e instanceof com.twilio.exception.ApiException) {
                com.twilio.exception.ApiException apiEx = (com.twilio.exception.ApiException) e;
                System.err.println("Twilio API Error Code: " + apiEx.getCode());
                System.err.println("Twilio API Error Message: " + apiEx.getMessage());
            }
        }
    }

    /**
     * Scarica un allegato multimediale da Twilio e lo salva sul disco
     *
     * @param mediaUrl URL dell'allegato da Twilio
     * @param contentType Tipo di contenuto dell'allegato
     * @param originalFilename Nome del file originale (se disponibile)
     * @return Percorso del file salvato
     * @throws IOException In caso di problemi con il download o il salvataggio
     */
    public String scaricaAllegato(String mediaUrl, String contentType, String originalFilename) throws IOException {
        System.out.println("Avvio download dell'allegato: " + mediaUrl);

        // Configura autenticazione Twilio
        HttpHeaders headers = new HttpHeaders();
        String auth = accountSid + ":" + authToken;
        String authHeader = "Basic " + Base64.getEncoder().encodeToString(auth.getBytes());
        headers.set("Authorization", authHeader);

        // Esegui la richiesta per scaricare il file
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<byte[]> response = restTemplate.exchange(
                mediaUrl, HttpMethod.GET, entity, byte[].class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            // Determina estensione file in base al content type
            String extension = getFileExtension(contentType, originalFilename);

            // Crea directory se non esiste
            String dirPath = uploadDir + "/whatsapp_media";
            Files.createDirectories(Paths.get(dirPath));

            // Genera un nome file univoco
            String fileName = UUID.randomUUID().toString();
            if (originalFilename != null && !originalFilename.isEmpty()) {
                // Mantieni il nome originale del file ma con UUID come prefisso
                fileName += "_" + originalFilename;
            } else {
                fileName += extension;
            }

            // Salva il file
            String filePath = dirPath + "/" + fileName;
            Path path = Paths.get(filePath);
            Files.write(path, response.getBody());

            System.out.println("Allegato salvato con successo: " + filePath);
            return filePath;
        } else {
            throw new IOException("Download del media fallito: " +
                    response.getStatusCode() + " " + response.getStatusCodeValue());
        }
    }

    /**
     * Determina l'estensione del file in base al tipo di contenuto
     */
    private String getFileExtension(String contentType, String originalFilename) {
        // Se il filename originale è disponibile, prendi l'estensione
        if (originalFilename != null && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        // Altrimenti, basa l'estensione sul tipo MIME
        switch (contentType.toLowerCase()) {
            case "application/pdf":
                return ".pdf";
            case "image/jpeg":
                return ".jpg";
            case "image/png":
                return ".png";
            case "text/plain":
                return ".txt";
            case "application/msword":
                return ".doc";
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document":
                return ".docx";
            case "application/vnd.ms-excel":
                return ".xls";
            case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet":
                return ".xlsx";
            default:
                return ".bin";
        }
    }
}