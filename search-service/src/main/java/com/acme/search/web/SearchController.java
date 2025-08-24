package com.acme.search.web;

import com.acme.search.model.Product;
import com.acme.search.repo.ProductRepository;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.*;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.AggregationsContainer;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;

import java.util.*;

import static org.springframework.data.elasticsearch.client.elc.QueryBuilders.*;

@RestController
@RequestMapping("/api/search")
public class SearchController {

  private final ElasticsearchOperations esOps;
  private final ProductRepository repo;

  public SearchController(ElasticsearchOperations esOps, ProductRepository repo) {
    this.esOps = esOps; this.repo = repo;
  }

  @PostMapping(value="/index-demo", consumes=MediaType.APPLICATION_JSON_VALUE)
  public Product index(@RequestBody Product p) { return repo.save(p); }

  @GetMapping("/fulltext")
  public Map<String,Object> fulltext(
    @RequestParam String q,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "10") int size,
    @RequestParam(required = false) String brand,
    @RequestParam(required = false) String category,
    @RequestParam(required = false) Double minPrice,
    @RequestParam(required = false) Double maxPrice
  ) {
    var must = new ArrayList<co.elastic.clients.elasticsearch._types.query_dsl.Query>();
    // match en name (search_as_you_type.*) y description
    must.add(multiMatch(mm -> mm
      .query(q)
      .fields("name","name._2gram","name._3gram","description")
      .type(co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType.BestFields)
    ));

    var filters = new ArrayList<co.elastic.clients.elasticsearch._types.query_dsl.Query>();
    if (brand != null)    filters.add(term(t -> t.field("brand").value(brand)));
    if (category != null) filters.add(term(t -> t.field("category").value(category)));
    if (minPrice != null || maxPrice != null) {
      filters.add(range(r -> {
        var rr = r.field("price");
        if (minPrice != null) rr = rr.gte(minPrice);
        if (maxPrice != null) rr = rr.lte(maxPrice);
        return rr;
      }));
    }

    var boolQuery = bool(b -> b.must(must).filter(filters));

    var query = NativeQuery.builder()
      .withQuery(qb -> boolQuery)
      .withPageable(org.springframework.data.domain.PageRequest.of(page, size))
      // FACETS (aggregations): brand/category (terms) y price (range)
      .withAggregation("by_brand",   agg -> agg.terms(t -> t.field("brand")))
      .withAggregation("by_category",agg -> agg.terms(t -> t.field("category")))
      .withAggregation("price_ranges", agg -> agg.range(r -> r.field("price")
        .ranges(rs -> rs.key("0-50").to(50.0))
        .ranges(rs -> rs.key("50-100").from(50.0).to(100.0))
        .ranges(rs -> rs.key("100-200").from(100.0).to(200.0))
        .ranges(rs -> rs.key("200+").from(200.0))
      ))
      .build();

    SearchHits<Product> hits = esOps.search(query, Product.class);
    Map<String,Object> out = new HashMap<>();
    out.put("total", hits.getTotalHits());
    out.put("items", hits.stream().map(SearchHit::getContent).toList());

    AggregationsContainer<?> aggs = hits.getAggregations();
    out.put("facets", aggs);
    return out;
  }

  @GetMapping("/suggest")
  public List<String> suggest(@RequestParam String q, @RequestParam(defaultValue="5") int size) {
    // sugiere términos usando prefix query sobre search_as_you_type (infix soportado con subcampos)
    var query = NativeQuery.builder()
      .withQuery(qb -> prefix(p -> p.field("name").value(q)))
      .withMaxResults(size)
      .build();

    return esOps.search(query, Product.class)
            .stream()
            .map(h -> h.getContent().name())
            .distinct()
            .limit(size)
            .toList();
  }
}
