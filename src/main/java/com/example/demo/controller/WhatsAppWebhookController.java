package com.example.demo.controller;

import com.example.demo.config.TwilioConfig;
import com.example.demo.model.Conversazione;
import com.example.demo.model.Messaggio;
import com.example.demo.model.Organizzazione;
import com.example.demo.service.AIService;
import com.example.demo.service.DocumentoService;
import com.example.demo.service.RAGService;
import com.example.demo.service.WhatsAppService;
import com.example.demo.repository.OrganizzazioneRepository;
import com.example.demo.repository.ConversazioneRepository;
import com.example.demo.repository.MessaggioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/webhook")
public class WhatsAppWebhookController {

    private final OrganizzazioneRepository organizzazioneRepository;
    private final ConversazioneRepository conversazioneRepository;
    private final MessaggioRepository messaggioRepository;
    private final WhatsAppService whatsAppService;
    private final TwilioConfig twilioConfig;
    private final AIService aiService;
    private final RAGService ragService;
    private final DocumentoService documentoService;

    @Value("${twilio.whatsapp.number}")
    private String whatsappNumber;

    @Autowired
    public WhatsAppWebhookController(
            OrganizzazioneRepository organizzazioneRepository,
            ConversazioneRepository conversazioneRepository,
            MessaggioRepository messaggioRepository,
            WhatsAppService whatsAppService,
            TwilioConfig twilioConfig,
            AIService aiService,
            RAGService ragService,
            DocumentoService documentoService) {
        this.organizzazioneRepository = organizzazioneRepository;
        this.conversazioneRepository = conversazioneRepository;
        this.messaggioRepository = messaggioRepository;
        this.whatsAppService = whatsAppService;
        this.twilioConfig = twilioConfig;
        this.aiService = aiService;
        this.ragService = ragService;
        this.documentoService = documentoService;
    }


    // Verifica che il webhook sia funzionante
    // GET --> http://localhost:8080/webhook/test
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Webhook test successful");
    }


    @PostMapping("/test-twilio-send")
    public ResponseEntity<String> testTwilioSend(@RequestBody Map<String, String> payload) {
        try {
            String to = payload.get("to");
            String body = payload.get("body");

            if (to == null || body == null) {
                return ResponseEntity.badRequest().body("I parametri 'to' e 'body' sono obbligatori");
            }

            System.out.println("Avvio test invio messaggio WhatsApp...");
            System.out.println("Numero destinatario: " + to);
            System.out.println("Messaggio: " + body);

            boolean success = whatsAppService.inviaMessaggioTest(to, body);

            if (success) {
                return ResponseEntity.ok("Messaggio WhatsApp inviato con successo a " + to);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Test fallito. Controlla i log del server per i dettagli dell'errore.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Errore nell'invio del messaggio di test: " + e.getMessage());
        }
    }


    // Invia un messaggio di test tramite WhatsApp
    // GET --> http://localhost:8080/webhook/test-twilio-send
    @GetMapping("/test-twilio-connection")
    public ResponseEntity<String> testTwilioConnection() {
        try {
            if (!twilioConfig.isInitialized()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Errore: Twilio non è stato inizializzato correttamente. Controlla le credenziali.");
            }

            com.twilio.rest.api.v2010.Account account =
                    com.twilio.rest.api.v2010.Account.fetcher().fetch();

            return ResponseEntity.ok("Connessione Twilio verificata con successo!\nNome account: " +
                    account.getFriendlyName() + "\nStatus account: " + account.getStatus() +
                    "\n=== TWILIO INIZIALIZZATO CORRETTAMENTE ===");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Errore nella connessione a Twilio: " + e.getMessage());
        }
    }


    // Riceve i messaggi WhatsApp da Twilio
    // POST --> http://localhost:8080/webhook/whatsapp
    @PostMapping(value = "/whatsapp", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> handleWhatsAppMessage(
            @RequestParam("From") String from,
            @RequestParam("To") String to,
            @RequestParam("Body") String body,
            @RequestParam(value = "MessageSid", required = false) String messageSid,
            @RequestParam(value = "NumMedia", defaultValue = "0") int numMedia,
            HttpServletRequest request) {

        // Rimuovi il prefisso "whatsapp:" dai numeri di telefono
        String cleanedFrom = from.replace("whatsapp:", "");
        String cleanedTo = to.replace("whatsapp:", "");

        // Log dei dati ricevuti per debug
        System.out.println("Messaggio WhatsApp ricevuto:");
        System.out.println("Da: " + cleanedFrom);
        System.out.println("A: " + cleanedTo);
        System.out.println("Corpo: " + body);
        System.out.println("Numero di allegati: " + numMedia);

        try {
            // 1. Identificare l'organizzazione dal numero WhatsApp
            Optional<Organizzazione> organizzazioneOpt;
            if (cleanedTo.equals(whatsappNumber)) {
                // Se c'è una sola organizzazione, usa quella
                organizzazioneOpt = organizzazioneRepository.findAll().stream().findFirst();
            } else {
                // Altrimenti cerca per numero WhatsApp
                organizzazioneOpt = organizzazioneRepository.findAll().stream()
                        .filter(org -> cleanedTo.equals(org.getNumeroWhatsapp()))
                        .findFirst();
            }

            Organizzazione organizzazione;
            if (organizzazioneOpt.isEmpty()) {
                System.out.println("Creazione di un'organizzazione predefinita per il numero: " + cleanedTo);
                Organizzazione nuovaOrganizzazione = new Organizzazione();
                nuovaOrganizzazione.setNome("Yovendo Test");
                nuovaOrganizzazione.setNumeroWhatsapp(cleanedTo);
                nuovaOrganizzazione.setDataCreazione(LocalDateTime.now());
                nuovaOrganizzazione.setTonoDiVoce("Professionale e amichevole");
                organizzazione = organizzazioneRepository.save(nuovaOrganizzazione);
                System.out.println("Organizzazione creata con ID: " + organizzazione.getId());
            } else {
                organizzazione = organizzazioneOpt.get();
                System.out.println("Organizzazione trovata con ID: " + organizzazione.getId());
            }

            // 2. Trovare o creare una conversazione attiva per questo cliente
            Conversazione conversazione = conversazioneRepository
                    .findByTelefonoClienteAndStatoOrderByOrarioInizioDesc(cleanedFrom, "attiva")
                    .orElseGet(() -> {
                        System.out.println("Creazione di una nuova conversazione per il cliente: " + cleanedFrom);
                        Conversazione nuovaConversazione = new Conversazione();
                        nuovaConversazione.setOrganizzazione(organizzazione);
                        nuovaConversazione.setTelefonoCliente(cleanedFrom);
                        nuovaConversazione.setStato("attiva");
                        nuovaConversazione.setOrarioInizio(LocalDateTime.now());

                        // Assicurati che il contesto sia inizializzato come una Map vuota
                        Map<String, Object> contestoVuoto = new HashMap<>();
                        nuovaConversazione.setContesto(contestoVuoto);

                        Conversazione conversazioneSalvata = conversazioneRepository.save(nuovaConversazione);
                        System.out.println("Conversazione creata con ID: " + conversazioneSalvata.getId());
                        return conversazioneSalvata;
                    });

            // 3. Controllo per comandi speciali
            if (body.startsWith("/")) {
                ResponseEntity<String> commandResponse = handleCommandMessage(cleanedFrom, body, organizzazione, conversazione);
                if (commandResponse != null) {
                    return commandResponse;
                }
            }

            // 4. Salvare il messaggio ricevuto
            Messaggio messaggio = new Messaggio();
            messaggio.setConversazione(conversazione);
            messaggio.setDaCliente(true);
            messaggio.setContenuto(body);
            messaggio.setOrarioInvio(LocalDateTime.now());

            // Inizializza documentiRiferiti
            Map<String, Object> documentiVuoti = new HashMap<>();
            messaggio.setDocumentiRiferiti(documentiVuoti);

            Messaggio messaggioSalvato = messaggioRepository.save(messaggio);
            System.out.println("Messaggio del cliente salvato con ID: " + messaggioSalvato.getId());

            // 5. Gestione degli allegati (media)
            if (numMedia > 0) {
                for (int i = 0; i < numMedia; i++) {
                    String mediaUrl = request.getParameter("MediaUrl" + i);
                    String contentType = request.getParameter("MediaContentType" + i);
                    String mediaFileName = request.getParameter("MediaFileName" + i);

                    // Nome del file predefinito se non fornito
                    if (mediaFileName == null || mediaFileName.isEmpty()) {
                        mediaFileName = "allegato_" + System.currentTimeMillis();
                    }

                    try {
                        // Scarica l'allegato
                        System.out.println("Download allegato: " + mediaUrl + " di tipo " + contentType);
                        String savedPath = whatsAppService.scaricaAllegato(mediaUrl, contentType, mediaFileName);

                        // Titolo automatico per il documento
                        String docTitle = "WhatsApp: " + (mediaFileName != null ? mediaFileName : "Documento " + LocalDateTime.now());

                        // Crea documento e avvia elaborazione
                        documentoService.creaDocumentoDaPath(savedPath, docTitle, contentType, organizzazione.getId(), conversazione.getId());

                        // Invia conferma al cliente
                        whatsAppService.inviaMessaggioTest(cleanedFrom,
                                "Ho ricevuto il tuo documento '" + docTitle + "' e lo sto elaborando. Ti avviserò quando sarà pronto per essere utilizzato nelle risposte.");

                    } catch (Exception e) {
                        System.err.println("Errore nell'elaborazione dell'allegato: " + e.getMessage());
                        whatsAppService.inviaMessaggioTest(cleanedFrom,
                                "Mi dispiace, ho riscontrato un problema nell'elaborazione del tuo documento. Riprova più tardi.");
                    }
                }

                // Se ci sono solo allegati senza testo, possiamo terminare qui
                if (body.trim().isEmpty()) {
                    return ResponseEntity.ok("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Response></Response>");
                }
            }

            // 6. Generare una risposta usando il RAGService o AIService
            String rispostaContenuto;
            try {
                // Costruiamo un contesto della conversazione
                StringBuilder contestoConversazione = new StringBuilder();
                List<Messaggio> messaggiRecenti = messaggioRepository.findByConversazioneIdOrderByOrarioInvioAsc(conversazione.getId());
                messaggiRecenti.stream()
                        .skip(Math.max(0, messaggiRecenti.size() - 5))  // Ultimi 5 messaggi
                        .forEach(msg -> {
                            String prefix = msg.getDaCliente() ? "Cliente: " : "Assistente: ";
                            contestoConversazione.append(prefix).append(msg.getContenuto()).append("\n");
                        });

                // Usa RAGService per generare una risposta con contesto dei documenti
                rispostaContenuto = ragService.processaQuery(body, organizzazione.getId(), contestoConversazione.toString());
                System.out.println("Risposta generata con RAGService: " + rispostaContenuto);
            } catch (Exception e) {
                System.out.println("Errore con RAGService, tentativo con AIService diretto: " + e.getMessage());

                // Fallback: usa AIService direttamente se RAG non funziona
                String prompt = "Sei un assistente WhatsApp per " + organizzazione.getNome() +
                        ". Rispondi in modo " + organizzazione.getTonoDiVoce() +
                        " alla seguente domanda: " + body;

                rispostaContenuto = aiService.generaRisposta(prompt);
                System.out.println("Risposta generata con AIService diretto: " + rispostaContenuto);
            }

            // 7. Salva la risposta generata
            Messaggio risposta = new Messaggio();
            risposta.setConversazione(conversazione);
            risposta.setDaCliente(false);
            risposta.setContenuto(rispostaContenuto);
            risposta.setOrarioInvio(LocalDateTime.now());
            risposta.setDocumentiRiferiti(new HashMap<>());

            Messaggio rispostaSalvata = messaggioRepository.save(risposta);
            System.out.println("Risposta AI salvata con ID: " + rispostaSalvata.getId());

            // 8. Invia la risposta tramite Twilio
            System.out.println("Tentativo di invio risposta tramite Twilio...");
            whatsAppService.inviaRisposta(rispostaSalvata);
            System.out.println("Richiesta di invio completata.");

        } catch (Exception e) {
            System.err.println("Errore nella gestione del messaggio WhatsApp: " + e.getMessage());
            e.printStackTrace();

            // In caso di errore, invia comunque una risposta di fallback
            try {
                // Crea un messaggio di fallback
                String fallbackMessage = "Mi dispiace, sto avendo qualche difficoltà a elaborare la tua richiesta. " +
                        "Potresti riprovare più tardi?";

                // Invia direttamente tramite Twilio senza salvare nel database
                whatsAppService.inviaMessaggioTest(cleanedFrom, fallbackMessage);
            } catch (Exception ex) {
                System.err.println("Errore anche nell'invio del messaggio di fallback: " + ex.getMessage());
            }
        }

        // Restituisci una risposta XML vuota che Twilio si aspetta
        return ResponseEntity.ok("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Response></Response>");
    }

    // Metodo per gestire i comandi speciali
    private ResponseEntity<String> handleCommandMessage(String from, String body, Organizzazione organizzazione, Conversazione conversazione) {
        String comando = body.toLowerCase();

        if (comando.equals("/aiuto") || comando.equals("/help")) {
            String messaggioAiuto = "👋 Ciao! Ecco i comandi disponibili per interagire con me:\n\n" +
                    "📄 /documenti - Mostra la lista dei documenti disponibili\n" +
                    "❓ /aiuto - Mostra questo messaggio di aiuto\n\n" +
                    "💡 *Cosa puoi fare:*\n" +
                    "• Inviarmi qualsiasi domanda e ti risponderò in base alle mie conoscenze\n" +
                    "• Caricare PDF o documenti di testo inviandoli come allegati\n" +
                    "• Farmi domande specifiche sui documenti caricati\n\n" +
                    "Sono qui per aiutarti! 😊";

            whatsAppService.inviaMessaggioTest(from, messaggioAiuto);
            return ResponseEntity.ok("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Response></Response>");
        }

        if (comando.equals("/documenti")) {
            // Ottieni i documenti elaborati per l'organizzazione
            var documenti = documentoService.getDocumentiByOrganizzazione(organizzazione.getId(), true);

            if (documenti.isEmpty()) {
                whatsAppService.inviaMessaggioTest(from,
                        "Non hai ancora caricato documenti. Invia un file PDF o un documento di testo come allegato per iniziare.");
            } else {
                StringBuilder sb = new StringBuilder("📚 *Documenti disponibili:*\n\n");
                for (int i = 0; i < documenti.size(); i++) {
                    var doc = documenti.get(i);
                    sb.append(i + 1).append(". ").append(doc.getTitolo());

                    // Aggiungi data se disponibile
                    if (doc.getDataCaricamento() != null) {
                        sb.append(" (").append(doc.getDataCaricamento().toLocalDate()).append(")");
                    }

                    // Aggiungi stato elaborazione
                    if (!doc.getElaborato()) {
                        sb.append(" - ⏳ In elaborazione");
                    }

                    sb.append("\n");
                }

                sb.append("\nPuoi farmi domande su questi documenti e ti risponderò in base al loro contenuto.");
                whatsAppService.inviaMessaggioTest(from, sb.toString());
            }

            return ResponseEntity.ok("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Response></Response>");
        }

        // Nessun comando riconosciuto
        return null;
    }
}