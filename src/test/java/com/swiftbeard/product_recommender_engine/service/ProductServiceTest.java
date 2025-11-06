package com.swiftbeard.product_recommender_engine.service;

import com.swiftbeard.product_recommender_engine.TestDataFactory;
import com.swiftbeard.product_recommender_engine.config.ApplicationProperties;
import com.swiftbeard.product_recommender_engine.model.Category;
import com.swiftbeard.product_recommender_engine.model.Product;
import com.swiftbeard.product_recommender_engine.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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

        // Setup application properties mock
        when(applicationProperties.getProduct()).thenReturn(productConfig);
        when(productConfig.getMaxLimit()).thenReturn(100);
    }

    @Test
    void getAllProducts_ShouldReturnPagedProducts() {
        // Arrange
        Page<Product> expectedPage = new PageImpl<>(testProducts);
        when(productRepository.findByActiveTrue(any(Pageable.class))).thenReturn(expectedPage);

        // Act
        Page<Product> result = productService.getAllProducts(0, 10);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(5);
        assertThat(result.getContent()).isEqualTo(testProducts);
        verify(productRepository).findByActiveTrue(any(Pageable.class));
    }

    @Test
    void getAllProducts_ShouldRespectMaxLimit() {
        // Arrange
        Page<Product> expectedPage = new PageImpl<>(testProducts);
        when(productRepository.findByActiveTrue(any(Pageable.class))).thenReturn(expectedPage);

        // Act
        productService.getAllProducts(0, 200); // Request more than max

        // Assert
        verify(productRepository).findByActiveTrue(argThat(pageable ->
                pageable.getPageSize() == 100 // Should be limited to max
        ));
    }

    @Test
    void getProductById_ShouldReturnProduct_WhenExists() {
        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        // Act
        Product result = productService.getProductById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Wireless Headphones");
        verify(productRepository).findById(1L);
    }

    @Test
    void getProductById_ShouldThrowException_WhenNotFound() {
        // Arrange
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> productService.getProductById(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Product not found with id: 999");
        verify(productRepository).findById(999L);
    }

    @Test
    void getProductsByCategory_ShouldReturnFilteredProducts() {
        // Arrange
        List<Product> electronicsProducts = Arrays.asList(
                TestDataFactory.createElectronicsProduct(),
                TestDataFactory.createProduct(4L, "Gaming Mouse", Category.ELECTRONICS, new BigDecimal("79.99"))
        );
        Page<Product> expectedPage = new PageImpl<>(electronicsProducts);
        when(productRepository.findByCategoryAndActiveTrue(eq(Category.ELECTRONICS), any(Pageable.class)))
                .thenReturn(expectedPage);

        // Act
        Page<Product> result = productService.getProductsByCategory(Category.ELECTRONICS, 0, 10);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).allMatch(p -> p.getCategory() == Category.ELECTRONICS);
        verify(productRepository).findByCategoryAndActiveTrue(eq(Category.ELECTRONICS), any(Pageable.class));
    }

    @Test
    void searchProducts_ShouldReturnMatchingProducts() {
        // Arrange
        Page<Product> expectedPage = new PageImpl<>(Arrays.asList(testProduct));
        when(productRepository.searchByKeyword(eq("wireless"), any(Pageable.class)))
                .thenReturn(expectedPage);

        // Act
        Page<Product> result = productService.searchProducts("wireless", 0, 10);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(productRepository).searchByKeyword(eq("wireless"), any(Pageable.class));
    }

    @Test
    void getProductsByPriceRange_ShouldReturnFilteredProducts() {
        // Arrange
        BigDecimal minPrice = new BigDecimal("50.00");
        BigDecimal maxPrice = new BigDecimal("150.00");
        Page<Product> expectedPage = new PageImpl<>(Arrays.asList(testProduct));
        when(productRepository.findByPriceRange(eq(minPrice), eq(maxPrice), any(Pageable.class)))
                .thenReturn(expectedPage);

        // Act
        Page<Product> result = productService.getProductsByPriceRange(minPrice, maxPrice, 0, 10);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isNotEmpty();
        verify(productRepository).findByPriceRange(eq(minPrice), eq(maxPrice), any(Pageable.class));
    }

    @Test
    void getTopRatedProducts_ShouldReturnHighlyRatedProducts() {
        // Arrange
        List<Product> topRatedProducts = Arrays.asList(
                TestDataFactory.createProductWithRating(1L, new BigDecimal("4.8"), 100),
                TestDataFactory.createProductWithRating(2L, new BigDecimal("4.7"), 80)
        );
        Page<Product> expectedPage = new PageImpl<>(topRatedProducts);
        when(productRepository.findTopRated(any(Pageable.class))).thenReturn(expectedPage);

        // Act
        Page<Product> result = productService.getTopRatedProducts(0, 10);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        verify(productRepository).findTopRated(any(Pageable.class));
    }

    @Test
    void createProduct_ShouldSaveAndReturnProduct() {
        // Arrange
        Product newProduct = TestDataFactory.createProductWithoutId("New Product", Category.ELECTRONICS, new BigDecimal("199.99"));
        Product savedProduct = TestDataFactory.createProduct(10L, "New Product", Category.ELECTRONICS, new BigDecimal("199.99"));

        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);
        doNothing().when(vectorStoreService).addProduct(any(Product.class));

        // Act
        Product result = productService.createProduct(newProduct);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getName()).isEqualTo("New Product");
        verify(productRepository).save(any(Product.class));
        verify(vectorStoreService).addProduct(any(Product.class));
    }

    @Test
    void createProduct_ShouldNotFailIfVectorStoreThrowsException() {
        // Arrange
        Product newProduct = TestDataFactory.createProductWithoutId("New Product", Category.ELECTRONICS, new BigDecimal("199.99"));
        Product savedProduct = TestDataFactory.createProduct(10L, "New Product", Category.ELECTRONICS, new BigDecimal("199.99"));

        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);
        doThrow(new RuntimeException("Vector store error")).when(vectorStoreService).addProduct(any(Product.class));

        // Act
        Product result = productService.createProduct(newProduct);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(10L);
        verify(productRepository).save(any(Product.class));
        verify(vectorStoreService).addProduct(any(Product.class));
    }

    @Test
    void updateProduct_ShouldUpdateAndReturnProduct() {
        // Arrange
        Product existingProduct = TestDataFactory.createElectronicsProduct();
        Product updateData = TestDataFactory.createProduct(1L, "Updated Headphones", Category.ELECTRONICS, new BigDecimal("149.99"));

        when(productRepository.findById(1L)).thenReturn(Optional.of(existingProduct));
        when(productRepository.save(any(Product.class))).thenReturn(updateData);
        doNothing().when(vectorStoreService).updateProduct(any(Product.class));

        // Act
        Product result = productService.updateProduct(1L, updateData);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Updated Headphones");
        assertThat(result.getPrice()).isEqualTo(new BigDecimal("149.99"));
        verify(productRepository).findById(1L);
        verify(productRepository).save(any(Product.class));
        verify(vectorStoreService).updateProduct(any(Product.class));
    }

    @Test
    void updateProduct_ShouldThrowException_WhenProductNotFound() {
        // Arrange
        Product updateData = TestDataFactory.createElectronicsProduct();
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> productService.updateProduct(999L, updateData))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Product not found with id: 999");
        verify(productRepository).findById(999L);
        verify(productRepository, never()).save(any());
    }

    @Test
    void deleteProduct_ShouldSoftDeleteProduct() {
        // Arrange
        Product productToDelete = TestDataFactory.createElectronicsProduct();
        when(productRepository.findById(1L)).thenReturn(Optional.of(productToDelete));
        when(productRepository.save(any(Product.class))).thenReturn(productToDelete);
        doNothing().when(vectorStoreService).deleteProduct(1L);

        // Act
        productService.deleteProduct(1L);

        // Assert
        verify(productRepository).findById(1L);
        verify(productRepository).save(argThat(p -> !p.getActive()));
        verify(vectorStoreService).deleteProduct(1L);
    }

    @Test
    void generatePersonalizedDescription_ShouldReturnAIGeneratedDescription() {
        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        Generation generation = mock(Generation.class);
        AssistantMessage assistantMessage = mock(AssistantMessage.class);
        when(assistantMessage.getText()).thenReturn("This amazing wireless headphone is perfect for music lovers!");
        when(generation.getOutput()).thenReturn(assistantMessage);

        ChatResponse chatResponse = mock(ChatResponse.class);
        when(chatResponse.getResult()).thenReturn(generation);

        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

        // Act
        String result = productService.generatePersonalizedDescription(1L, "I love high-quality audio");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).contains("wireless headphone");
        verify(productRepository).findById(1L);
        verify(chatModel).call(any(Prompt.class));
    }

    @Test
    void ingestProductsToVectorStore_ShouldAddAllProducts() {
        // Arrange
        when(productRepository.findAll()).thenReturn(testProducts);
        doNothing().when(vectorStoreService).addProducts(anyList());

        // Act
        productService.ingestProductsToVectorStore();

        // Assert
        verify(productRepository).findAll();
        verify(vectorStoreService).addProducts(testProducts);
    }

    @Test
    void getAllBrands_ShouldReturnDistinctBrands() {
        // Arrange
        List<String> expectedBrands = Arrays.asList("Brand A", "Brand B", "Brand C");
        when(productRepository.findDistinctBrands()).thenReturn(expectedBrands);

        // Act
        List<String> result = productService.getAllBrands();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
        assertThat(result).containsExactlyElementsOf(expectedBrands);
        verify(productRepository).findDistinctBrands();
    }

    @Test
    void getAllTags_ShouldReturnDistinctTags() {
        // Arrange
        List<String> expectedTags = Arrays.asList("electronics", "wireless", "premium");
        when(productRepository.findDistinctTags()).thenReturn(expectedTags);

        // Act
        List<String> result = productService.getAllTags();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
        assertThat(result).containsExactlyElementsOf(expectedTags);
        verify(productRepository).findDistinctTags();
    }

    @Test
    void countByCategory_ShouldReturnCount() {
        // Arrange
        when(productRepository.countByCategoryAndActiveTrue(Category.ELECTRONICS)).thenReturn(15L);

        // Act
        long result = productService.countByCategory(Category.ELECTRONICS);

        // Assert
        assertThat(result).isEqualTo(15L);
        verify(productRepository).countByCategoryAndActiveTrue(Category.ELECTRONICS);
    }
}
