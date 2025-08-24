package unir.dwi.search.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "products")
public class Product {
  @Id
  private String id;

  @Field(type = FieldType.Search_As_You_Type)
  private String name;

  @Field(type = FieldType.Text)
  private String description;

  @Field(type = FieldType.Keyword)
  private String category;

  @Field(type = FieldType.Double)
  private Double price;

  @Field(type = FieldType.Keyword)
  private String image;

  public Product() {}

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }
  public String getCategory() { return category; }
  public void setCategory(String category) { this.category = category; }
  public Double getPrice() { return price; }
  public void setPrice(Double price) { this.price = price; }
  public String getImage() { return image; }
  public void setImage(String image) { this.image = image; }
}
