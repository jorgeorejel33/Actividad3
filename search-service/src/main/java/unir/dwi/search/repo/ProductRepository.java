package unir.dwi.search.repo;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import unir.dwi.search.domain.Product;

@Repository
public interface ProductRepository extends ElasticsearchRepository<Product, String> {
}
