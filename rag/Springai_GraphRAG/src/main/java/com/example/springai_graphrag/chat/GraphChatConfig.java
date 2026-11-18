package com.example.springai_graphrag.chat;

import com.example.springai_graphrag.graph.ProductCatalogService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GraphChatConfig {

    @Bean
    ChatClient graphChatClient(ChatClient.Builder builder, ProductCatalogService productCatalogService) {
        return builder
                .defaultSystem("""
                        You answer questions about three products: MediTrack, VoltDrive, and SkillBridge.
                        MediTrack is healthcare, VoltDrive is an electric vehicle, and SkillBridge is online learning.
                        Use the catalog tools to retrieve facts. Do not answer from prior knowledge.
                        HELPS_MANAGE, USES, and CONNECTS are the only relationships.
                        If one lookup is not enough, call the tools again with names you just received.
                        If the tools do not contain the answer, say you don't know.
                        """)
                .defaultTools(productCatalogService)
                .build();
    }
}
