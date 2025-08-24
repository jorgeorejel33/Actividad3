package unir.dwi.search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.RangeBucket;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.stereotype.Service;
import unir.dwi.search.domain.Product;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchService {

  private final ElasticsearchOperations ops;
  private final ElasticsearchClient esClient;

  public SearchService(ElasticsearchOperations ops, ElasticsearchClient esClient) {
    this.ops = ops;
    this.esClient = esClient;
  }

  private static String escJson(String s) {
    if (s == null) return "";
    return s.replace("\\", "\\\\").replace("\"", "\\\"");
  }

  public Page<Product> search(String q, int page, int size) {
    final String jsonQuery;
    if (q == null || q.isBlank()) {
      jsonQuery = """
        { "query": { "match_all": {} } }
      """;
    } else {
      final String qq = escJson(q);
      jsonQuery = """
        {
          "query": {
            "multi_match": {
              "query": "%s",
              "fields": ["name", "name._2gram", "name._3gram", "description"],
              "fuzziness": "AUTO"
            }
          }
        }
      """.formatted(qq);
    }

    Query query = new StringQuery(jsonQuery);
    query.setPageable(PageRequest.of(page, size));

    SearchHits<Product> hits = ops.search(query, Product.class);
    List<Product> content = hits.getSearchHits()
        .stream()
        .map(SearchHit::getContent)
        .collect(Collectors.toList());

    return new PageImpl<>(content, PageRequest.of(page, size), hits.getTotalHits());
  }

  public Map<String, Object> facets(String q) {
    try {
      SearchRequest.Builder builder = new SearchRequest.Builder()
          .index("products")
          .size(0)
          .aggregations("by_category", a -> a.terms(t -> t.field("category")))
          .aggregations("price_ranges", a -> a.range(r -> r.field("price")
              .ranges(r1 -> r1.to("200"))
              .ranges(r2 -> r2.from("200").to("500"))
              .ranges(r3 -> r3.from("500"))
          ));

      if (q != null && !q.isBlank()) {
        final String term = q;
        builder = builder.query(qb -> qb.multiMatch(mm -> mm
            .query(term)
            .fields("name", "name._2gram", "name._3gram", "description")
            .fuzziness("AUTO")));
      }

      SearchResponse<Map> resp = esClient.search(builder.build(), Map.class);

      Map<String, Object> result = new LinkedHashMap<>();

      Aggregate catAgg = resp.aggregations().get("by_category");
      List<Map<String, Object>> catBucketsOut = new ArrayList<>();
      if (catAgg != null && catAgg.isSterms()) {
        for (StringTermsBucket b : catAgg.sterms().buckets().array()) {
          Map<String, Object> bucket = new LinkedHashMap<>();
          bucket.put("key", b.key().stringValue());
          bucket.put("doc_count", b.docCount());
          catBucketsOut.add(bucket);
        }
      }
      result.put("by_category", Map.of("buckets", catBucketsOut));

      Aggregate rangeAgg = resp.aggregations().get("price_ranges");
      List<Map<String, Object>> rangeBucketsOut = new ArrayList<>();
      if (rangeAgg != null && rangeAgg.isRange()) {
        for (RangeBucket b : rangeAgg.range().buckets().array()) {
          Map<String, Object> bucket = new LinkedHashMap<>();
          bucket.put("key", b.key());
          if (b.from() != null) bucket.put("from", b.from());
          if (b.to() != null) bucket.put("to", b.to());
          bucket.put("doc_count", b.docCount());
          rangeBucketsOut.add(bucket);
        }
      }
      result.put("price_ranges", Map.of("buckets", rangeBucketsOut));

      return result;
    } catch (IOException e) {
      return Map.of(
          "by_category", Map.of("buckets", List.of()),
          "price_ranges", Map.of("buckets", List.of())
      );
    }
  }
}
