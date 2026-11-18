package com.example.springai_graphrag.chat;

import com.example.springai_graphrag.graph.ProductCatalogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Scanner;

@Component
@Order(0)
@ConditionalOnProperty(name = "app.graphrag.enabled", matchIfMissing = true)
public class ProductGraphChat implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ProductGraphChat.class);

    private static final String NOTES = """
            MediTrack is a healthcare application that helps patients manage medical appointments \
            and maintain their health records. It also tracks prescriptions and medication schedules.
            VoltDrive is an electric vehicle designed for urban transportation. It uses an electric battery, \
            supports fast charging, and provides intelligent navigation.
            SkillBridge is an online learning platform that connects students with industry professionals. \
            It offers interactive courses and facilitates mentorship programs.
            """;

    private final ProductCatalogService productCatalogService;
    private final ChatClient graphChatClient;
    private final String singleQuestion;

    public ProductGraphChat(
            ProductCatalogService productCatalogService,
            ChatClient graphChatClient,
            @Value("${app.graphrag.question:}") String singleQuestion) {
        this.productCatalogService = productCatalogService;
        this.graphChatClient = graphChatClient;
        this.singleQuestion = singleQuestion;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Reading product descriptions into the catalog...");
        productCatalogService.addDescriptions(NOTES);

        if (singleQuestion != null && !singleQuestion.isBlank()) {
            System.out.println(answer(singleQuestion));
            return;
        }

        System.out.println("""
                Product catalog is ready for MediTrack, VoltDrive, and SkillBridge.
                Ask what a product helps manage, what it uses, or whom it connects.
                Try: What does MediTrack help patients manage?
                Try: What does VoltDrive use, and whom does SkillBridge connect?
                Press Enter on an empty line to exit.
                """);
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("> ");
                if (!scanner.hasNextLine()) {
                    break;
                }
                String question = scanner.nextLine().trim();
                if (question.isEmpty() || question.equalsIgnoreCase("exit")) {
                    break;
                }
                System.out.println(answer(question));
                System.out.println();
            }
        }
    }

    private String answer(String question) {
        return graphChatClient.prompt()
                .user(question)
                .call()
                .content();
    }
}
