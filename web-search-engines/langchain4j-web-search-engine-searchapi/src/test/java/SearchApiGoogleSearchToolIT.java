import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModelName;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.web.search.WebSearchEngine;
import dev.langchain4j.web.search.WebSearchTool;
import dev.langchain4j.web.search.WebSearchToolIT;
import dev.langchain4j.web.search.searchapi.SearchApiEngine;
import dev.langchain4j.web.search.searchapi.SearchApiWebSearchEngine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "SEARCHAPI_API_KEY", matches = ".*")
class SearchApiGoogleSearchToolIT extends WebSearchToolIT {

    private static final String SYSTEM_MSG = "You are a web search support agent. If there is any event that has not happened yet, you MUST use a web search tool to look up the information on the web. Include the source link in your final response. Do not say that you have not the capability to browse the web in real time";

    WebSearchEngine searchApiEngine = SearchApiWebSearchEngine.withApiKey(System.getenv("SEARCHAPI_API_KEY"));

    ChatLanguageModel chatModel = OpenAiChatModel.builder()
            .apiKey(System.getenv("OPENAI_API_KEY"))
            .modelName(OpenAiChatModelName.GPT_3_5_TURBO)
            .logRequests(true)
            .build();

    interface Assistant {
        @dev.langchain4j.service.SystemMessage(SYSTEM_MSG)
        String answer(String userMessage);
    }

    @Test
    void should_execute_searchApi_tool_with_chatLanguageModel_to_give_a_final_response() {
        // given
        searchApiEngine = SearchApiWebSearchEngine.builder()
                .apiKey(System.getenv("SEARCHAPI_API_KEY"))
                .engine(SearchApiEngine.GOOGLE_SEARCH)
                .build();

        WebSearchTool webSearchTool = WebSearchTool.from(searchApiEngine);
        List<ToolSpecification> tools = ToolSpecifications.toolSpecificationsFrom(webSearchTool);
        String query = "What are the movies to be released in May 2024?";
        List<ChatMessage> messages = new ArrayList<>();
        SystemMessage systemMessage = SystemMessage.from(SYSTEM_MSG);
        messages.add(systemMessage);
        UserMessage userMessage = UserMessage.from(query);
        messages.add(userMessage);
        // when
        AiMessage aiMessage = chatLanguageModel().generate(messages, tools).content();

        // then
        assertThat(aiMessage.hasToolExecutionRequests()).isTrue();
        assertThat(aiMessage.toolExecutionRequests())
                .anySatisfy(toolSpec -> {
                            assertThat(toolSpec.name())
                                    .containsIgnoringCase("searchWeb");
                            assertThat(toolSpec.arguments())
                                    .isNotBlank();
                        }
                );
        messages.add(aiMessage);

        // when
        String strResult = webSearchTool.searchWeb(query);
        ToolExecutionResultMessage toolExecutionResultMessage = ToolExecutionResultMessage.from(aiMessage.toolExecutionRequests().get(0), strResult);
        messages.add(toolExecutionResultMessage);

        AiMessage finalResponse = chatLanguageModel().generate(messages).content();
        System.out.println(finalResponse.text());

        // then
        assertThat(finalResponse.text())
                .as("The result string should contain 'movies' and 'May 2024' ignoring case")
                .containsIgnoringCase("movies")
                .containsIgnoringCase("May 2024");
    }

    @Test
    void should_execute_searchApi_tool_with_chatLanguageModel_to_summary_response_in_images() {
        // given
        searchApiEngine = SearchApiWebSearchEngine.builder()
                .apiKey(System.getenv("SEARCHAPI_API_KEY"))
                .engine(SearchApiEngine.GOOGLE_SEARCH)
                .build();

        WebSearchTool webSearchTool = WebSearchTool.from(searchApiEngine);
        List<ToolSpecification> tools = ToolSpecifications.toolSpecificationsFrom(webSearchTool);
        String query = "My family is coming to visit me in Madrid next week, list the best tourist activities suitable for the whole family";
        List<ChatMessage> messages = new ArrayList<>();
        SystemMessage systemMessage = SystemMessage.from(SYSTEM_MSG);
        messages.add(systemMessage);
        UserMessage userMessage = UserMessage.from(query);
        messages.add(userMessage);
        // when
        AiMessage aiMessage = chatLanguageModel().generate(messages, tools).content();

        // then
        assertThat(aiMessage.hasToolExecutionRequests()).isTrue();
        assertThat(aiMessage.toolExecutionRequests())
                .anySatisfy(toolSpec -> {
                            assertThat(toolSpec.name())
                                    .containsIgnoringCase("searchWeb");
                            assertThat(toolSpec.arguments())
                                    .isNotBlank();
                        }
                );
        messages.add(aiMessage);

        // when
        String strResult = webSearchTool.searchWeb(query);
        ToolExecutionResultMessage toolExecutionResultMessage = ToolExecutionResultMessage.from(aiMessage.toolExecutionRequests().get(0), strResult);
        messages.add(toolExecutionResultMessage);

        AiMessage finalResponse = chatLanguageModel().generate(messages).content();
        System.out.println(finalResponse.text());

        // then
        assertThat(finalResponse.text())
                .as("The result string should contain 'madrid' and 'tourist' ignoring case")
                .containsIgnoringCase("Madrid")
                .containsIgnoringCase("tourist");
    }

    @Test
    void should_execute_searchApi_tool_with_AiServices() {
        // given
        WebSearchTool webTool = WebSearchTool.from(searchApiEngine);

        Assistant assistant = AiServices.builder(Assistant.class)
                .chatLanguageModel(chatModel)
                .tools(webTool)
                .build();
        // when
        String answer = assistant.answer("Who won the FIFA World Cup 2022?");

        // then
        assertThat(answer).containsIgnoringCase("Argentina");
    }

    @Override
    protected WebSearchEngine searchEngine() {
        return searchApiEngine;
    }

    @Override
    protected ChatLanguageModel chatLanguageModel() {
        return chatModel;
    }
}