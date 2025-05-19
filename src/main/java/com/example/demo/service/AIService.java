package com.example.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class AIService {
    private static final Logger logger = LoggerFactory.getLogger(AIService.class);

    @Value("${ai.api.key}")
    private String apiKey;

    @Value("${ai.api.url:https://api.openai.com/v1}")
    private String apiUrl;

    @Value("${ai.embedding.model:text-embedding-ada-002}")
    private String embeddingModel;

    @Value("${ai.chat.model:gpt-3.5-turbo}")
    private String chatModel;

    @Value("${ai.temperature:0.7}")
    private double temperature;

    @Value("${ai.api.timeout:30000}")
    private int timeout;

    @Value("${ai.api.max-retries:3}")
    private int maxRetries;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AIService() {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * Genera una risposta basata sul prompt fornito utilizzando il modello di chat
     */
    @Cacheable(value = "aiResponses", key = "#prompt.hashCode()", unless = "#result == null || #result.contains('errore')")
    public String generaRisposta(String prompt) {
        logger.info("Generazione risposta per prompt di {} caratteri", prompt.length());
        int retries = 0;
        Exception lastException = null;

        while (retries < maxRetries) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(apiKey);

                ObjectNode requestBody = objectMapper.createObjectNode();
                requestBody.put("model", chatModel);

                ObjectNode message = objectMapper.createObjectNode();
                message.put("role", "user");
                message.put("content", prompt);

                requestBody.putArray("messages").add(message);
                requestBody.put("temperature", temperature);

                HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);

                logger.debug("Invio richiesta a OpenAI (tentativo {}): {}", retries + 1, apiUrl + "/chat/completions");

                String responseBody = restTemplate.postForObject(apiUrl + "/chat/completions", request, String.class);
                JsonNode responseJson = objectMapper.readTree(responseBody);

                // Verifica se ci sono errori nella risposta API
                if (responseJson.has("error")) {
                    String errorMessage = responseJson.path("error").path("message").asText("Errore sconosciuto");
                    logger.error("Errore OpenAI: {}", errorMessage);
                    throw new RuntimeException("Errore API OpenAI: " + errorMessage);
                }

                String result = responseJson.path("choices").path(0).path("message").path("content").asText();
                logger.info("Risposta generata con successo ({} caratteri)", result.length());
                return result;

            } catch (Exception e) {
                lastException = e;
                logger.warn("Tentativo {} fallito: {}", retries + 1, e.getMessage());
                retries++;

                if (retries < maxRetries) {
                    try {
                        // Attesa esponenziale tra i tentativi
                        long backoffTime = (long) Math.pow(2, retries) * 1000;
                        logger.info("Attesa di {} ms prima del prossimo tentativo", backoffTime);
                        Thread.sleep(backoffTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        // Se arriviamo qui, tutti i tentativi sono falliti
        logger.error("Tutti i {} tentativi di chiamata API falliti", maxRetries, lastException);
        return "Si è verificato un errore durante la generazione della risposta dopo " + maxRetries + " tentativi. " +
                "Dettaglio: " + (lastException != null ? lastException.getMessage() : "Errore sconosciuto");
    }

    /**
     * Genera un embedding per il testo fornito
     */
    @Cacheable(value = "aiEmbeddings", key = "#testo.hashCode()")
    public byte[] generaEmbedding(String testo) {
        logger.info("Generazione embedding per testo di {} caratteri", testo.length());
        int retries = 0;
        Exception lastException = null;

        while (retries < maxRetries) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(apiKey);

                ObjectNode requestBody = objectMapper.createObjectNode();
                requestBody.put("model", embeddingModel);
                requestBody.put("input", testo);

                HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);

                logger.debug("Invio richiesta embedding a OpenAI (tentativo {})", retries + 1);

                String responseBody = restTemplate.postForObject(apiUrl + "/embeddings", request, String.class);
                JsonNode responseJson = objectMapper.readTree(responseBody);

                // Verifica se ci sono errori nella risposta API
                if (responseJson.has("error")) {
                    String errorMessage = responseJson.path("error").path("message").asText("Errore sconosciuto");
                    logger.error("Errore OpenAI embedding: {}", errorMessage);
                    throw new RuntimeException("Errore API OpenAI embedding: " + errorMessage);
                }

                // Estrazione dell'embedding
                JsonNode data = responseJson.path("data").path(0).path("embedding");

                if (data.isMissingNode() || !data.isArray()) {
                    throw new RuntimeException("Formato di risposta embedding non valido");
                }

                float[] embeddings = new float[data.size()];
                for (int i = 0; i < data.size(); i++) {
                    embeddings[i] = data.get(i).floatValue();
                }

                byte[] result = floatArrayToByteArray(embeddings);
                logger.info("Embedding generato con successo (dimensione: {} float / {} byte)", embeddings.length, result.length);
                return result;

            } catch (Exception e) {
                lastException = e;
                logger.warn("Tentativo embedding {} fallito: {}", retries + 1, e.getMessage());
                retries++;

                if (retries < maxRetries) {
                    try {
                        long backoffTime = (long) Math.pow(2, retries) * 1000;
                        logger.info("Attesa di {} ms prima del prossimo tentativo", backoffTime);
                        Thread.sleep(backoffTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        // Se arriviamo qui, tutti i tentativi sono falliti
        logger.error("Tutti i {} tentativi di generazione embedding falliti", maxRetries, lastException);
        return new byte[0]; // Restituisci array vuoto in caso di errore
    }

    /**
     * Converte un array di float in un array di byte per lo storage
     */
    private byte[] floatArrayToByteArray(float[] floatArray) {
        byte[] byteArray = new byte[floatArray.length * 4];

        for (int i = 0; i < floatArray.length; i++) {
            int intBits = Float.floatToIntBits(floatArray[i]);
            byteArray[i * 4] = (byte) (intBits & 0xFF);
            byteArray[i * 4 + 1] = (byte) ((intBits >> 8) & 0xFF);
            byteArray[i * 4 + 2] = (byte) ((intBits >> 16) & 0xFF);
            byteArray[i * 4 + 3] = (byte) ((intBits >> 24) & 0xFF);
        }

        return byteArray;
    }
}