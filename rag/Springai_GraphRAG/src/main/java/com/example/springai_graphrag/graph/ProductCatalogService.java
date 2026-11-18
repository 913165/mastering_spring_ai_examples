package com.example.springai_graphrag.graph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class ProductCatalogService {

    private static final Logger log = LoggerFactory.getLogger(ProductCatalogService.class);

    private final ChatClient extractor;
    private final ProductCatalog catalog = new ProductCatalog(new ArrayList<>());

    public ProductCatalogService(ChatModel chatModel) {
        this.extractor = ChatClient.builder(chatModel)
                .defaultSystem("""
                        Extract relationships from the product descriptions.
                        Allowed relations only:
                        HELPS_MANAGE — a product helps someone manage a record, appointment, prescription, or schedule.
                        USES — a product uses a component or capability, including charging and navigation.
                        CONNECTS — a product connects people, or links them to a course or mentorship program.
                        Put the product name in source. Put the thing managed, used, or connected in target.
                        Use entity names from the text: MediTrack, VoltDrive, SkillBridge, and the items each one mentions.
                        Map courses and mentorship programs to CONNECTS. Do not invent other relation types.
                        Return only relationships the text supports.
                        """)
                .build();
    }

    public void addDescriptions(String text) {
        ProductCatalog extracted = extractor.prompt()
                .user(user -> user.text("""
                        Product descriptions:
                        {text}
                        """).param("text", text))
                .call()
                .entity(ProductCatalog.class, spec -> spec
                        .useProviderStructuredOutput()
                        .validateSchema());
        catalog.include(extracted.links());
        log.info("Catalog now has {} relationships.", catalog.links().size());
        for (ProductLink link : catalog.links()) {
            log.info("{} --{}--> {}", link.source(), link.relation(), link.target());
        }
    }

    @Tool(description = """
            List the entity names currently stored in the product catalog.
            Call this when a name lookup returns nothing, then retry with a listed name.
            """)
    public List<String> listEntities() {
        log.info("Listing entities in the product catalog.");
        Set<String> names = new LinkedHashSet<>();
        for (ProductLink link : catalog.links()) {
            names.add(link.source());
            names.add(link.target());
        }
        return List.copyOf(names);
    }

    @Tool(description = """
            Search the product catalog.
            HELPS_MANAGE: a product helps manage an appointment, record, prescription, or schedule. Product is `source`.
            USES: a product uses a battery, charging method, or navigation capability. Product is `source`.
            CONNECTS: a product connects people, courses, or mentorship programs. Product is `source`.
            Provide `source`, `target`, or both. Always provide `relation`.
            """)
    public List<ProductLink> findLinks(
            @ToolParam(description = "Entity the relationship starts from. Omit to search by target.", required = false)
            String source,
            @ToolParam(description = "Relationship type: HELPS_MANAGE, USES, or CONNECTS.")
            Relation relation,
            @ToolParam(description = "Entity the relationship points to. Omit to search by source.", required = false)
            String target) {
        log.info("Finding links. {} --{}--> {}", source, relation, target);
        if (isBlank(source) && isBlank(target)) {
            return List.of();
        }
        return catalog.links().stream()
                .filter(link -> matches(source, link.source()))
                .filter(link -> link.relation() == relation)
                .filter(link -> matches(target, link.target()))
                .toList();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static boolean matches(String expected, String actual) {
        if (isBlank(expected)) {
            return true;
        }
        return actual != null && actual.trim().equalsIgnoreCase(expected.trim());
    }
}
