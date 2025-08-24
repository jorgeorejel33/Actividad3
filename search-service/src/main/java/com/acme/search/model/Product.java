package com.acme.search.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

@Document(indexName = "products")
public record Product(
  @Id String id,
  String name,
  String brand,
  String category,
  Double price,
  String description
) {}
