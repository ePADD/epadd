package edu.stanford.muse.ner.model;

import edu.stanford.muse.Config;
import edu.stanford.muse.ner.tokenize.Tokenizer;
import edu.stanford.muse.util.Span;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class LoadResultsModel implements NERModel {

    private static final Logger log =  LogManager.getLogger(LoadResultsModel.class);

    private static final Map<String, Short> typeMap = new HashMap<>() {{
        put("PER", NEType.Type.PERSON.getCode());
        put("LOC", NEType.Type.PLACE.getCode());
        put("ORG", NEType.Type.ORGANISATION.getCode());
        put("MISC", NEType.Type.OTHER.getCode());
    }};

    private static final Map<String, List<Span>> entitiesMap = new HashMap<>();

    public LoadResultsModel() {
        String filename = Config.ENTITIES_FILE;
        if (filename == null) {
            log.warn("No entity file defined, skipping");
            return;
        }
        log.info("Start loading entities from {}", filename);
        JSONParser parser = new JSONParser();
        try {
            Object obj = parser.parse(new FileReader(filename));
            JSONArray emailEntities = (JSONArray) obj;
            for (JSONObject emailDetails : (Iterable<JSONObject>) emailEntities) {
                String msgId = (String)emailDetails.get("message-id");
                List<Span> spans = new ArrayList<>();
                JSONArray entities = (JSONArray)emailDetails.get("entities");
                for (JSONObject entity : (Iterable<JSONObject>) entities) {
                    spans.add(makeSpanFromJson(entity));
                }
                entitiesMap.put(msgId, spans);
            }
        } catch (Exception e) {
            log.error("Got error {}", e.getMessage());
        }
    }

    private static Span makeSpanFromJson(JSONObject entity) {
        String word = (String)entity.get("word");
        Long start = (Long)entity.get("start");
        Long end = (Long)entity.get("end");
        String entityGroup = (String)entity.get("entity_group");
        Double score = (Double)entity.get("score");

        Span span = new Span(word, start.intValue(), end.intValue());
        span.setType(
                typeMap.getOrDefault(entityGroup, NEType.Type.OTHER.getCode()),
                score.floatValue()
        );
        return span;
    }

    @Override
    public Span[] find(String messageId) {
        if (entitiesMap.containsKey(messageId))
            return entitiesMap.get(messageId).toArray(new Span[0]);
        log.warn("Could not find {} in entities cache...", messageId);
        return new Span[] {};
    }

    @Override
    public void setTokenizer(Tokenizer tokenizer) { }

}
