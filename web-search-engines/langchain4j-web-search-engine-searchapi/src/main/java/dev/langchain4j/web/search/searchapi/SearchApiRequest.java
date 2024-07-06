package dev.langchain4j.web.search.searchapi;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

import static dev.langchain4j.internal.ValidationUtils.ensureNotBlank;
import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;

@Getter
class SearchApiRequest {

    private final SearchApiEngine engine;
    private final String apiKey;
    private final String query;
    private final Map<String, Object> additionalParameters;

    @Builder
    SearchApiRequest(SearchApiEngine engine,
                            String apiKey,
                            String query,
                            Map<String, Object> additionalParameters) {
        this.engine = ensureNotNull(engine, "engine");
        this.apiKey = ensureNotBlank(apiKey, "apiKey");
        this.query = ensureNotBlank(query, "query");
        this.additionalParameters = additionalParameters;
    }
}