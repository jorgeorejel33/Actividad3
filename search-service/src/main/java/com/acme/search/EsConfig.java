package com.acme.search.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.annotation.PostConstruct;
import org.springframework.data.elasticsearch.core.index.*;
import org.springframework.data.elasticsearch.core.document.Document;

@Configuration
public class EsConfig {

  @Autowired
  ElasticsearchTemplate template;

  @PostConstruct
  public void init() {
    IndexOperations ops = template.indexOps("products");
    if (!ops.exists()) {
      ops.create();
      Document mapping = Document.parse("""
      {
        "properties": {
          "id":        { "type": "keyword" },
          "name":      { "type": "search_as_you_type" },
          "brand":     { "type": "keyword" },
          "category":  { "type": "keyword" },
          "price":     { "type": "double" },
          "description": { "type": "text" }
        }
      }
      """);
      ops.putMapping(mapping);
    }
  }
}
