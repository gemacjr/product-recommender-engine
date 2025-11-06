package com.swiftbeard.product_recommender_engine.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftbeard.product_recommender_engine.TestDataFactory;
import com.swiftbeard.product_recommender_engine.dto.ProductRequest;
import com.swiftbeard.product_recommender_engine.model.Category;
import com.swiftbeard.product_recommender_engine.model.Product;
import com.swiftbeard.product_recommender_engine.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    @Test
    void getAllProducts_ShouldReturnPagedProducts() throws Exception {
        // Arrange
        List<Product> products = TestDataFactory.createProductList();
        when(productService.getAllProducts(0, 10)).thenReturn(new PageImpl<>(products));

        // Act & Assert
        mockMvc.perform(get("/api/products")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products", hasSize(5)))
                .andExpect(jsonPath("$.currentPage", is(0)))
                .andExpect(jsonPath("$.totalItems", is(5)))
                .andExpect(jsonPath("$.totalPages", is(1)));

        verify(productService).getAllProducts(0, 10);
    }

    @Test
    void getAllProducts_ShouldUseDefaultPagination() throws Exception {
        // Arrange
        when(productService.getAllProducts(0, 10)).thenReturn(new PageImpl<>(Arrays.asList()));

        // Act & Assert
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk());

        verify(productService).getAllProducts(0, 10);
    }

    @Test
    void getProductById_ShouldReturnProduct_WhenExists() throws Exception {
        // Arrange
        Product product = TestDataFactory.createElectronicsProduct();
        when(productService.getProductById(1L)).thenReturn(product);

        // Act & Assert
        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Wireless Headphones")))
                .andExpect(jsonPath("$.category", is("ELECTRONICS")))
                .andExpect(jsonPath("$.price", is(99.99)));

        verify(productService).getProductById(1L);
    }

    @Test
    void getProductById_ShouldReturn404_WhenNotFound() throws Exception {
        // Arrange
        when(productService.getProductById(999L))
                .thenThrow(new RuntimeException("Product not found with id: 999"));

        // Act & Assert
        mockMvc.perform(get("/api/products/999"))
                .andExpect(status().is5xxServerError());

        verify(productService).getProductById(999L);
    }

    @Test
    void searchProducts_ShouldReturnMatchingProducts() throws Exception {
        // Arrange
        Product product = TestDataFactory.createElectronicsProduct();
        when(productService.searchProducts(eq("wireless"), anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(Arrays.asList(product)));

        // Act & Assert
        mockMvc.perform(get("/api/products/search")
                        .param("keyword", "wireless")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products", hasSize(1)))
                .andExpect(jsonPath("$.products[0].name", is("Wireless Headphones")));

        verify(productService).searchProducts("wireless", 0, 10);
    }

    @Test
    void getProductsByCategory_ShouldReturnCategoryProducts() throws Exception {
        // Arrange
        List<Product> electronicsProducts = Arrays.asList(
                TestDataFactory.createElectronicsProduct(),
                TestDataFactory.createProduct(4L, "Gaming Mouse", Category.ELECTRONICS, new BigDecimal("79.99"))
        );
        when(productService.getProductsByCategory(eq(Category.ELECTRONICS), anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(electronicsProducts));

        // Act & Assert
        mockMvc.perform(get("/api/products/category/ELECTRONICS")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products", hasSize(2)))
                .andExpect(jsonPath("$.products[*].category", everyItem(is("ELECTRONICS"))));

        verify(productService).getProductsByCategory(Category.ELECTRONICS, 0, 10);
    }

    @Test
    void getProductsByPriceRange_ShouldReturnFilteredProducts() throws Exception {
        // Arrange
        Product product = TestDataFactory.createElectronicsProduct();
        when(productService.getProductsByPriceRange(
                eq(new BigDecimal("50.00")),
                eq(new BigDecimal("150.00")),
                anyInt(),
                anyInt()))
                .thenReturn(new PageImpl<>(Arrays.asList(product)));

        // Act & Assert
        mockMvc.perform(get("/api/products/price-range")
                        .param("minPrice", "50.00")
                        .param("maxPrice", "150.00")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products", hasSize(1)));

        verify(productService).getProductsByPriceRange(
                new BigDecimal("50.00"),
                new BigDecimal("150.00"),
                0, 10);
    }

    @Test
    void getTopRatedProducts_ShouldReturnHighlyRatedProducts() throws Exception {
        // Arrange
        List<Product> topRated = Arrays.asList(
                TestDataFactory.createProductWithRating(1L, new BigDecimal("4.8"), 100),
                TestDataFactory.createProductWithRating(2L, new BigDecimal("4.7"), 80)
        );
        when(productService.getTopRatedProducts(anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(topRated));

        // Act & Assert
        mockMvc.perform(get("/api/products/top-rated")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products", hasSize(2)));

        verify(productService).getTopRatedProducts(0, 10);
    }

    @Test
    void createProduct_ShouldCreateAndReturnProduct() throws Exception {
        // Arrange
        ProductRequest request = TestDataFactory.createProductRequest();
        Product createdProduct = TestDataFactory.createProduct(
                10L,
                request.getName(),
                request.getCategory(),
                request.getPrice()
        );

        when(productService.createProduct(any(Product.class))).thenReturn(createdProduct);

        // Act & Assert
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(10)))
                .andExpect(jsonPath("$.name", is(request.getName())))
                .andExpect(jsonPath("$.category", is(request.getCategory().name())));

        verify(productService).createProduct(any(Product.class));
    }

    @Test
    void createProduct_ShouldReturn400_WhenValidationFails() throws Exception {
        // Arrange
        ProductRequest invalidRequest = TestDataFactory.createInvalidProductRequest();

        // Act & Assert
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(productService, never()).createProduct(any());
    }

    @Test
    void updateProduct_ShouldUpdateAndReturnProduct() throws Exception {
        // Arrange
        ProductRequest request = TestDataFactory.createProductRequest();
        Product updatedProduct = TestDataFactory.createProduct(
                1L,
                request.getName(),
                request.getCategory(),
                request.getPrice()
        );

        when(productService.updateProduct(eq(1L), any(Product.class))).thenReturn(updatedProduct);

        // Act & Assert
        mockMvc.perform(put("/api/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is(request.getName())));

        verify(productService).updateProduct(eq(1L), any(Product.class));
    }

    @Test
    void deleteProduct_ShouldDeleteAndReturnSuccessMessage() throws Exception {
        // Arrange
        doNothing().when(productService).deleteProduct(1L);

        // Act & Assert
        mockMvc.perform(delete("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Product deleted successfully")))
                .andExpect(jsonPath("$.id", is("1")));

        verify(productService).deleteProduct(1L);
    }

    @Test
    void getPersonalizedDescription_ShouldReturnCustomDescription() throws Exception {
        // Arrange
        String personalizedDesc = "These premium wireless headphones are perfect for audiophiles who value quality sound.";
        when(productService.generatePersonalizedDescription(eq(1L), anyString()))
                .thenReturn(personalizedDesc);

        // Act & Assert
        mockMvc.perform(post("/api/products/1/personalized-description")
                        .param("userPreferences", "high quality audio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId", is("1")))
                .andExpect(jsonPath("$.personalizedDescription", is(personalizedDesc)));

        verify(productService).generatePersonalizedDescription(1L, "high quality audio");
    }

    @Test
    void getPersonalizedDescription_ShouldHandleNullPreferences() throws Exception {
        // Arrange
        when(productService.generatePersonalizedDescription(eq(1L), isNull()))
                .thenReturn("Standard description");

        // Act & Assert
        mockMvc.perform(post("/api/products/1/personalized-description"))
                .andExpect(status().isOk());

        verify(productService).generatePersonalizedDescription(eq(1L), isNull());
    }

    @Test
    void ingestProducts_ShouldIngestToVectorStore() throws Exception {
        // Arrange
        doNothing().when(productService).ingestProductsToVectorStore();

        // Act & Assert
        mockMvc.perform(post("/api/products/ingest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("ingested to vector store")));

        verify(productService).ingestProductsToVectorStore();
    }

    @Test
    void getAllBrands_ShouldReturnBrandsList() throws Exception {
        // Arrange
        List<String> brands = Arrays.asList("Brand A", "Brand B", "Brand C");
        when(productService.getAllBrands()).thenReturn(brands);

        // Act & Assert
        mockMvc.perform(get("/api/products/brands"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$", containsInAnyOrder("Brand A", "Brand B", "Brand C")));

        verify(productService).getAllBrands();
    }

    @Test
    void getAllTags_ShouldReturnTagsList() throws Exception {
        // Arrange
        List<String> tags = Arrays.asList("wireless", "premium", "gaming");
        when(productService.getAllTags()).thenReturn(tags);

        // Act & Assert
        mockMvc.perform(get("/api/products/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$", containsInAnyOrder("wireless", "premium", "gaming")));

        verify(productService).getAllTags();
    }
}
