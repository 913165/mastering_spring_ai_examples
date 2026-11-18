package com.example.springai_graphrag.graph;

import java.util.ArrayList;
import java.util.List;

public record ProductCatalog(List<ProductLink> links) {

    public ProductCatalog {
        links = links == null ? new ArrayList<>() : new ArrayList<>(links);
    }

    public void include(List<ProductLink> more) {
        if (more != null) {
            links.addAll(more);
        }
    }
}
