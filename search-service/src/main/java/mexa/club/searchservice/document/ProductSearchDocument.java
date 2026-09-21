package mexa.club.searchservice.document;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;
import java.util.List;

@Document(indexName = "products", createIndex = false)
@Getter
@Setter
public class ProductSearchDocument {
    @Id
    private String id;
    @Field(type = FieldType.Text, analyzer = "uzbek_analyzer")
    private String name;
    @Field(type = FieldType.Text, analyzer = "ngram_analyzer")
    private String nameNgram;
    @Field(type = FieldType.Keyword)
    private String barcode;
    @Field(type = FieldType.Text, analyzer = "uzbek_analyzer")
    private String description;
    @Field(type = FieldType.Keyword)
    private String categoryId;
    @Field(type = FieldType.Keyword)
    private String categoryName;
    @Field(type = FieldType.Keyword)
    private String brandId;
    @Field(type = FieldType.Keyword)
    private String brandName;
    @Field(type = FieldType.Keyword)
    private List<String> tags;
    @Field(type = FieldType.Double)
    private Double salePrice;
    @Field(type = FieldType.Boolean)
    private Boolean active;
    @Field(type = FieldType.Keyword)
    private String imageUrl;
    @Field(type = FieldType.Boolean)
    private Boolean inStock;
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime createdAt;
}
