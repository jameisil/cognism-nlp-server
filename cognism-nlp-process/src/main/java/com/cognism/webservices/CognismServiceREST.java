package com.cognism.webservices;

import com.cognism.sentiment.SentimentExtractor;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component
@Path("/nlp")
@Service
public class CognismServiceREST implements Serializable {

    private static final Logger log = Logger.getLogger(CognismServiceREST.class);
    private static final long serialVersionUID = 1L;

    @Autowired
    private SentimentExtractor sentimentExtractor;

    public CognismServiceREST() {

    }

    @POST
    @Path("/post/content")
    @Consumes(MediaType.APPLICATION_JSON)
    public List<Cognitive> sendContent(String jsonObject) {
        log.info("CognismServiceREST: start sending content to NLP ");
        List<Cognitive> list = new ArrayList<Cognitive>();
        Integer status = 0;
        if (jsonObject == null) {
            return null;
        }

        boolean validJson = false;
        try {
            validJson = this.isValidJSON(jsonObject);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        if (!validJson) {
            return null;
        }
        ObjectMapper mapper = new ObjectMapper();
        Cognitive cognitive = new Cognitive();
        JsonFactory factory = mapper.getJsonFactory();
        JsonParser jp;
        try {
            jp = factory.createJsonParser(jsonObject.toString());
            JsonNode input = mapper.readTree(jp);

            final JsonNode content = input.get("content");

            list = sentimentExtractor.getOutputList(content.getTextValue());
            String json = new Gson().toJson(list);
            log.info("After NLP process output list size is  " + list.size());
            return list;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public SentimentExtractor getSentimentExtractor() {
        return sentimentExtractor;
    }

    public void setSentimentExtractor(SentimentExtractor sentimentExtractor) {
        this.sentimentExtractor = sentimentExtractor;
    }

    private boolean isValidJSON(final String json) throws IOException {
        boolean valid = false;
        try {
            final JsonParser parser = new ObjectMapper().getJsonFactory()
                    .createJsonParser(json);
            while (parser.nextToken() != null) {
            }
            valid = true;
        } catch (JsonParseException jpe) {
            //jpe.printStackTrace();
            valid = false;
        }
        return valid;
    }

}
