package com.example.demo.config;

import jakarta.annotation.PostConstruct;
import com.twilio.Twilio;
import com.twilio.exception.AuthenticationException;
import com.twilio.exception.ApiException;
import com.twilio.rest.api.v2010.account.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class TwilioConfig {
    private static final Logger logger = LoggerFactory.getLogger(TwilioConfig.class);

    @Value("${twilio.account.sid:}")
    private String accountSid;

    @Value("${twilio.auth.token:}")
    private String authToken;

    @Value("${twilio.whatsapp.number:}")
    private String whatsappNumber;

    private boolean isInitialized = false;

    @PostConstruct
    public void initTwilio() {
        try {
            logger.info("=== INIZIALIZZAZIONE TWILIO ===");

            // Verifica che le credenziali non siano vuote
            if (!StringUtils.hasText(accountSid) || !StringUtils.hasText(authToken) || !StringUtils.hasText(whatsappNumber)) {
                logger.warn("Credenziali Twilio mancanti o non valide:");
                logger.warn("Account SID: {}", StringUtils.hasText(accountSid) ? accountSid.substring(0, Math.min(8, accountSid.length())) + "..." : "MANCANTE");
                logger.warn("Auth Token: {}", StringUtils.hasText(authToken) ? authToken.substring(0, Math.min(4, authToken.length())) + "..." : "MANCANTE");
                logger.warn("WhatsApp Number: {}", StringUtils.hasText(whatsappNumber) ? whatsappNumber : "MANCANTE");
                logger.warn("Le funzionalità WhatsApp saranno disabilitate.");
                logger.warn("Per abilitare Twilio, imposta le variabili TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, TWILIO_WHATSAPP_NUMBER");

                // Usciamo con isInitialized = false, ma senza causare errori
                isInitialized = false;
                return;
            }

            logger.info("Account SID: {}", accountSid.substring(0, Math.min(8, accountSid.length())) + "...");
            logger.info("Auth Token: {}", authToken.substring(0, Math.min(4, authToken.length())) + "...");
            logger.info("WhatsApp Number: {}", whatsappNumber);

            // Inizializza Twilio
            Twilio.init(accountSid, authToken);
            isInitialized = true;

            // Verifica che la connessione funzioni
            try {
                com.twilio.rest.api.v2010.Account account =
                        com.twilio.rest.api.v2010.Account.fetcher().fetch();
                logger.info("Connessione Twilio verificata con successo!");
                logger.info("Nome account: {}", account.getFriendlyName());
                logger.info("Status account: {}", account.getStatus());
                logger.info("=== TWILIO INIZIALIZZATO CORRETTAMENTE ===");
            } catch (AuthenticationException ae) {
                logger.error("ERRORE DI AUTENTICAZIONE TWILIO: {}", ae.getMessage());
                logger.error("Le credenziali fornite non sono valide o l'account è stato sospeso.");
                logger.error("Controlla Account SID e Auth Token nella configurazione.");
                isInitialized = false;
            } catch (Exception e) {
                logger.error("ERRORE durante la verifica della connessione Twilio: {}", e.getMessage());
                logger.error("Twilio è stato inizializzato ma non è stato possibile verificare la connessione.");
                // Non impostiamo isInitialized = false qui perché potrebbe essere solo un problema temporaneo
            }
        } catch (Exception e) {
            logger.error("ERRORE CRITICO durante l'inizializzazione di Twilio: {}", e.getMessage());
            logger.error("Twilio non è stato inizializzato correttamente.");
            isInitialized = false;
        }
    }

    // Metodo per testare l'invio di un messaggio
    public boolean testSendMessage(String to, String body) {
        if (!isInitialized) {
            logger.error("Impossibile inviare il messaggio: Twilio non è stato inizializzato correttamente");
            return false;
        }

        if (!StringUtils.hasText(to) || !StringUtils.hasText(body)) {
            logger.error("Impossibile inviare il messaggio: destinatario o corpo del messaggio mancante");
            return false;
        }

        try {
            // Assicurati che il numero sia formattato correttamente
            String formattedTo = to.startsWith("+") ? to : "+" + to.replaceAll("[^0-9]", "");

            logger.info("Test invio messaggio WhatsApp...");
            logger.info("Da: whatsapp:{}", whatsappNumber);
            logger.info("A: whatsapp:{}", formattedTo);
            logger.info("Corpo: {}", body);

            Message message = Message.creator(
                    new com.twilio.type.PhoneNumber("whatsapp:" + formattedTo),
                    new com.twilio.type.PhoneNumber("whatsapp:" + whatsappNumber),
                    body
            ).create();

            logger.info("Messaggio inviato con successo! SID: {}", message.getSid());
            return true;
        } catch (ApiException apiEx) {
            logger.error("Errore API Twilio: {} - {}", apiEx.getCode(), apiEx.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("Errore nell'invio del messaggio di test: {}", e.getMessage());
            return false;
        }
    }

    public String getAccountSid() {
        return accountSid;
    }

    public String getAuthToken() {
        return authToken;
    }

    public String getWhatsappNumber() {
        return whatsappNumber;
    }

    public boolean isInitialized() {
        return isInitialized;
    }
}