package unir.dwi.search.web;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import unir.dwi.search.domain.Product;
import unir.dwi.search.repo.ProductRepository;
import unir.dwi.search.service.SearchService;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class SearchController {
  private final SearchService service;
  private final ProductRepository repo;

  public SearchController(SearchService service, ProductRepository repo) {
    this.service = service;
    this.repo = repo;
  }

  @GetMapping("/search")
  public Page<Product> search(
      @RequestParam(defaultValue = "") String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "12") int size) {
    return service.search(q, page, size);
  }

  @GetMapping("/search/facets")
  public Map<String, Object> facets(@RequestParam(defaultValue = "") String q) {
    return service.facets(q);
  }

  @GetMapping("/products/{id}")
  public Product byId(@PathVariable String id) {
    return repo.findById(id).orElse(null);
  }
}
