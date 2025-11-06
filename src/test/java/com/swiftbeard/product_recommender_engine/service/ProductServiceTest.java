package com.swiftbeard.product_recommender_engine.service;

import com.swiftbeard.product_recommender_engine.TestDataFactory;
import com.swiftbeard.product_recommender_engine.config.ApplicationProperties;
import com.swiftbeard.product_recommender_engine.model.Category;
import com.swiftbeard.product_recommender_engine.model.Product;
import com.swiftbeard.product_recommender_engine.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ProductService Unit Tests")
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private VectorStoreService vectorStoreService;

    @Mock
    private ChatModel chatModel;

    @Mock
    private ApplicationProperties applicationProperties;

    @Mock
    private ApplicationProperties.ProductConfig productConfig;

    @InjectMocks
    private ProductService productService;

    private Product testProduct;
    private List<Product> testProducts;

    @BeforeEach
    void setUp() {
        testProduct = TestDataFactory.createElectronicsProduct();
        testProducts = TestDataFactory.createProductList();

        when(applicationProperties.getProduct()).thenReturn(productConfig);
        when(productConfig.getMaxLimit()).thenReturn(100);
    }

    @Test
    @DisplayName("Should return paginated products when getting all products")
    void getAllProducts_ShouldReturnPagedProducts() {
        Page<Product> expectedPage = new PageImpl<>(testProducts);
        when(productRepository.findByActiveTrue(any(Pageable.class))).thenReturn(expectedPage);

        Page<Product> result = productService.getAllProducts(0, 10);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(5);
        assertThat(result.getContent()).isEqualTo(testProducts);
        verify(productRepository).findByActiveTrue(any(Pageable.class));
    }

    @Test
    @DisplayName("Should respect max limit when getting all products")
    void getAllProducts_ShouldRespectMaxLimit() {
        when(productConfig.getMaxLimit()).thenReturn(20);
        Page<Product> expectedPage = new PageImpl<>(testProducts);
        when(productRepository.findByActiveTrue(any(Pageable.class))).thenReturn(expectedPage);

        productService.getAllProducts(0, 150);

        verify(productRepository).findByActiveTrue(argThat(pageable -> {
            PageRequest pr = (PageRequest) pageable;
            return pr.getPageSize() == 20;
        }));
    }

    @Test
    @DisplayName("Should return product when found by ID")
    void getProductById_ShouldReturnProduct_WhenExists() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        Product result = productService.getProductById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Wireless Headphones");
        verify(productRepository).findById(1L);
    }

    @Test
    @DisplayName("Should throw exception when product not found")
    void getProductById_ShouldThrowException_WhenNotFound() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Product not found with id: 999");
    }

    @Test
    @DisplayName("Should return products by category")
    void getProductsByCategory_ShouldReturnCategoryProducts() {
        Page<Product> expectedPage = new PageImpl<>(testProducts);
        when(productRepository.findByCategoryAndActiveTrue(eq(Category.ELECTRONICS), any(Pageable.class)))
                .thenReturn(expectedPage);

        Page<Product> result = productService.getProductsByCategory(Category.ELECTRONICS, 0, 10);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(5);
        verify(productRepository).findByCategoryAndActiveTrue(eq(Category.ELECTRONICS), any(Pageable.class));
    }

    @Test
    @DisplayName("Should search products by keyword")
    void searchProducts_ShouldReturnMatchingProducts() {
        Page<Product> expectedPage = new PageImpl<>(testProducts);
        when(productRepository.searchByKeyword(eq("wireless"), any(Pageable.class))).thenReturn(expectedPage);

        Page<Product> result = productService.searchProducts("wireless", 0, 10);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(5);
        verify(productRepository).searchByKeyword(eq("wireless"), any(Pageable.class));
    }

    @Test
    @DisplayName("Should return products by price range")
    void getProductsByPriceRange_ShouldReturnProductsInRange() {
        Page<Product> expectedPage = new PageImpl<>(testProducts);
        when(productRepository.findByPriceRange(any(BigDecimal.class), any(BigDecimal.class), any(Pageable.class)))
                .thenReturn(expectedPage);

        Page<Product> result = productService.getProductsByPriceRange(
                new BigDecimal("50"), new BigDecimal("200"), 0, 10);

        assertThat(result).isNotNull();
        verify(productRepository).findByPriceRange(
                eq(new BigDecimal("50")), eq(new BigDecimal("200")), any(Pageable.class));
    }

    @Test
    @DisplayName("Should return top-rated products")
    void getTopRatedProducts_ShouldReturnTopRatedProducts() {
        Page<Product> expectedPage = new PageImpl<>(testProducts);
        when(productRepository.findTopRated(any(Pageable.class))).thenReturn(expectedPage);

        Page<Product> result = productService.getTopRatedProducts(0, 10);

        assertThat(result).isNotNull();
        verify(productRepository).findTopRated(any(Pageable.class));
    }

    @Test
    @DisplayName("Should create product and add to vector store")
    void createProduct_ShouldSaveAndAddToVectorStore() {
        Product newProduct = TestDataFactory.createProductWithoutId("New Product", Category.ELECTRONICS, new BigDecimal("99.99"));
        Product savedProduct = TestDataFactory.createProduct(1L, "New Product", Category.ELECTRONICS, new BigDecimal("99.99"));
        when(productRepository.save(newProduct)).thenReturn(savedProduct);
        doNothing().when(vectorStoreService).addProduct(any(Product.class));

        Product result = productService.createProduct(newProduct);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(productRepository).save(newProduct);
        verify(vectorStoreService).addProduct(savedProduct);
    }

    @Test
    @DisplayName("Should handle vector store failure gracefully when creating product")
    void createProduct_ShouldHandleVectorStoreFailure() {
        Product newProduct = TestDataFactory.createProductWithoutId("New Product", Category.ELECTRONICS, new BigDecimal("99.99"));
        Product savedProduct = TestDataFactory.createProduct(1L, "New Product", Category.ELECTRONICS, new BigDecimal("99.99"));
        when(productRepository.save(newProduct)).thenReturn(savedProduct);
        doThrow(new RuntimeException("Vector store error")).when(vectorStoreService).addProduct(any(Product.class));

        Product result = productService.createProduct(newProduct);

        assertThat(result).isNotNull();
        verify(productRepository).save(newProduct);
        verify(vectorStoreService).addProduct(savedProduct);
    }

    @Test
    @DisplayName("Should update product and update vector store")
    void updateProduct_ShouldUpdateAndUpdateVectorStore() {
        Product existingProduct = TestDataFactory.createElectronicsProduct();
        Product updatedDetails = TestDataFactory.createProduct(1L, "Updated Product", Category.ELECTRONICS, new BigDecimal("149.99"));
        when(productRepository.findById(1L)).thenReturn(Optional.of(existingProduct));
        when(productRepository.save(any(Product.class))).thenReturn(updatedDetails);
        doNothing().when(vectorStoreService).updateProduct(any(Product.class));

        Product result = productService.updateProduct(1L, updatedDetails);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Updated Product");
        verify(productRepository).save(any(Product.class));
        verify(vectorStoreService).updateProduct(updatedDetails);
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent product")
    void updateProduct_ShouldThrowException_WhenProductNotFound() {
        Product updatedDetails = TestDataFactory.createProduct(999L, "Updated Product", Category.ELECTRONICS, new BigDecimal("149.99"));
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateProduct(999L, updatedDetails))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Product not found with id: 999");
    }

    @Test
    @DisplayName("Should soft delete product and remove from vector store")
    void deleteProduct_ShouldSoftDeleteAndRemoveFromVectorStore() {
        Product existingProduct = TestDataFactory.createElectronicsProduct();
        when(productRepository.findById(1L)).thenReturn(Optional.of(existingProduct));
        when(productRepository.save(any(Product.class))).thenReturn(existingProduct);
        doNothing().when(vectorStoreService).deleteProduct(anyLong());

        productService.deleteProduct(1L);

        verify(productRepository).findById(1L);
        verify(productRepository).save(argThat(p -> !p.getActive()));
        verify(vectorStoreService).deleteProduct(1L);
    }

    @Test
    @DisplayName("Should generate personalized description using AI")
    void generatePersonalizedDescription_ShouldReturnAIGeneratedDescription() {
        Product product = TestDataFactory.createElectronicsProduct();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        AssistantMessage assistantMessage = mock(AssistantMessage.class);
        when(assistantMessage.getText()).thenReturn("This is a personalized description for tech enthusiasts.");
        Generation generation = mock(Generation.class);
        when(generation.getOutput()).thenReturn(assistantMessage);
        ChatResponse chatResponse = mock(ChatResponse.class);
        when(chatResponse.getResult()).thenReturn(generation);
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

        String result = productService.generatePersonalizedDescription(1L, "tech enthusiast");

        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
        assertThat(result).contains("personalized description");
        verify(chatModel).call(any(Prompt.class));
    }

    @Test
    @DisplayName("Should ingest all products to vector store")
    void ingestProductsToVectorStore_ShouldAddAllProducts() {
        when(productRepository.findAll()).thenReturn(testProducts);
        doNothing().when(vectorStoreService).addProducts(anyList());

        productService.ingestProductsToVectorStore();

        verify(productRepository).findAll();
        verify(vectorStoreService).addProducts(testProducts);
    }

    @Test
    @DisplayName("Should return all distinct brands")
    void getAllBrands_ShouldReturnDistinctBrands() {
        List<String> brands = Arrays.asList("Brand A", "Brand B", "Brand C");
        when(productRepository.findDistinctBrands()).thenReturn(brands);

        List<String> result = productService.getAllBrands();

        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
        assertThat(result).containsAll(brands);
        verify(productRepository).findDistinctBrands();
    }

    @Test
    @DisplayName("Should return all distinct tags")
    void getAllTags_ShouldReturnDistinctTags() {
        List<String> tags = Arrays.asList("tag1", "tag2", "tag3");
        when(productRepository.findDistinctTags()).thenReturn(tags);

        List<String> result = productService.getAllTags();

        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
        verify(productRepository).findDistinctTags();
    }

    @Test
    @DisplayName("Should count products by category")
    void countByCategory_ShouldReturnCount() {
        when(productRepository.countByCategoryAndActiveTrue(Category.ELECTRONICS)).thenReturn(5L);

        long result = productService.countByCategory(Category.ELECTRONICS);

        assertThat(result).isEqualTo(5L);
        verify(productRepository).countByCategoryAndActiveTrue(Category.ELECTRONICS);
    }
}

