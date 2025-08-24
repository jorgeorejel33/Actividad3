package unir.dwi.search.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.SourceFilter;
import org.springframework.stereotype.Service;
import unir.dwi.search.domain.Product;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.data.elasticsearch.client.elc.QueryBuilders.*;

@Service
public class SearchService {
  private final ElasticsearchOperations ops;

  public SearchService(ElasticsearchOperations ops) {
    this.ops = ops;
  }

  public Page<Product> search(String q, int page, int size) {
    var fields = List.of("name", "name._2gram", "name._3gram", "description");
    var query = NativeQuery.builder()
      .withQuery(multiMatch(m -> m
        .query(q != null ? q : "")
        .fields(fields)
        .fuzziness("AUTO")))
      .withPageable(PageRequest.of(page, size))
      .build();

    SearchHits<Product> hits = ops.search(query, Product.class);
    List<Product> content = hits.getSearchHits().stream()
      .map(SearchHit::getContent)
      .collect(Collectors.toList());
    return new PageImpl<>(content, PageRequest.of(page, size), hits.getTotalHits());
  }

  public Map<String, Object> facets(String q) {
    var qb = bool(b -> b
      .must(multiMatch(m -> m
        .query(q != null ? q : "")
        .fields("name, name._2gram, name._3gram, description"))));

    var query = NativeQuery.builder()
      .withQuery(qb)
      .withAggregation("by_category", a -> a.terms(t -> t.field("category")))
      .withAggregation("price_ranges", a -> a.range(r -> r.field("price")
        .ranges(r1 -> r1.to("200"),
                r2 -> r2.from("200").to("500"),
                r3 -> r3.from("500"))))
      .withSourceFilter(new SourceFilter() {
        @Override public String[] getIncludes() { return new String[0]; }
        @Override public String[] getExcludes() { return new String[] {"*"}; }
      })
      .build();

    var sr = ops.search(query, Product.class);
    return sr.getAggregations().asMap();
  }
}
