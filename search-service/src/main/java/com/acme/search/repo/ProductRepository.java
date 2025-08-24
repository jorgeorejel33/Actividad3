package com.acme.search.repo;

import com.acme.search.model.Product;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import java.util.List;

public interface ProductRepository extends ElasticsearchRepository<Product, String> {
  List<Product> findByName(String name);
}
