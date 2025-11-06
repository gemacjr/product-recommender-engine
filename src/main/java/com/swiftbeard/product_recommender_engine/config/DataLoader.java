package com.swiftbeard.product_recommender_engine.config;

import com.swiftbeard.product_recommender_engine.model.Category;
import com.swiftbeard.product_recommender_engine.model.Product;
import com.swiftbeard.product_recommender_engine.repository.ProductRepository;
import com.swiftbeard.product_recommender_engine.service.VectorStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * Loads sample product data on application startup
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataLoader {

    @Bean
    CommandLineRunner loadSampleData(ProductRepository productRepository, VectorStoreService vectorStoreService) {
        return args -> {
            // Only load data if database is empty
            if (productRepository.count() > 0) {
                log.info("Database already contains products. Skipping sample data load.");
                return;
            }

            log.info("Loading sample product data...");

            List<Product> sampleProducts = Arrays.asList(
                    // Electronics
                    Product.builder()
                            .name("Sony WH-1000XM5 Wireless Headphones")
                            .description("Industry-leading noise canceling with Auto NC Optimizer, exceptional sound quality with 30mm driver units, up to 30-hour battery life, crystal clear hands-free calling")
                            .category(Category.ELECTRONICS)
                            .price(new BigDecimal("399.99"))
                            .brand("Sony")
                            .sku("SNY-WH1000XM5-BLK")
                            .tags(Arrays.asList("wireless", "noise-canceling", "premium", "audio"))
                            .features(Arrays.asList("30-hour battery", "Industry-leading noise canceling", "Premium sound quality", "Comfortable design"))
                            .stockQuantity(50)
                            .rating(new BigDecimal("4.8"))
                            .reviewCount(2456)
                            .imageUrl("https://example.com/sony-headphones.jpg")
                            .build(),

                    Product.builder()
                            .name("Apple MacBook Pro 14-inch M3 Pro")
                            .description("Supercharged by M3 Pro chip for exceptional performance, stunning Liquid Retina XDR display, up to 18 hours of battery life, advanced camera and audio")
                            .category(Category.ELECTRONICS)
                            .price(new BigDecimal("1999.00"))
                            .brand("Apple")
                            .sku("APL-MBP14-M3PRO")
                            .tags(Arrays.asList("laptop", "premium", "professional", "m3-chip"))
                            .features(Arrays.asList("M3 Pro chip", "14-inch Liquid Retina XDR", "18-hour battery", "16GB unified memory"))
                            .stockQuantity(25)
                            .rating(new BigDecimal("4.9"))
                            .reviewCount(1823)
                            .imageUrl("https://example.com/macbook-pro.jpg")
                            .build(),

                    Product.builder()
                            .name("Samsung 65-inch QLED 4K Smart TV")
                            .description("Quantum Dot technology for vibrant colors, 4K UHD resolution, built-in voice assistants, gaming mode with low input lag")
                            .category(Category.ELECTRONICS)
                            .price(new BigDecimal("1299.99"))
                            .brand("Samsung")
                            .sku("SAM-Q65-QLED")
                            .tags(Arrays.asList("tv", "4k", "smart-tv", "gaming"))
                            .features(Arrays.asList("Quantum Dot", "4K UHD", "Smart TV", "Gaming mode"))
                            .stockQuantity(30)
                            .rating(new BigDecimal("4.6"))
                            .reviewCount(3421)
                            .imageUrl("https://example.com/samsung-tv.jpg")
                            .build(),

                    // Clothing
                    Product.builder()
                            .name("Levi's 501 Original Fit Jeans")
                            .description("The iconic straight fit jean that started it all. Made with premium denim, button fly, sits at waist. A blank canvas for self-expression.")
                            .category(Category.CLOTHING)
                            .price(new BigDecimal("69.50"))
                            .brand("Levi's")
                            .sku("LEV-501-BLUE-32")
                            .tags(Arrays.asList("jeans", "denim", "classic", "casual"))
                            .features(Arrays.asList("Original fit", "Premium denim", "Button fly", "Classic 5-pocket design"))
                            .stockQuantity(150)
                            .rating(new BigDecimal("4.5"))
                            .reviewCount(8945)
                            .imageUrl("https://example.com/levis-501.jpg")
                            .build(),

                    Product.builder()
                            .name("Nike Air Max 270 Running Shoes")
                            .description("Max Air unit provides unrivaled, all-day comfort. Breathable mesh upper with synthetic overlays for support. Durable rubber outsole with waffle pattern.")
                            .category(Category.CLOTHING)
                            .price(new BigDecimal("150.00"))
                            .brand("Nike")
                            .sku("NIKE-AM270-BLK-10")
                            .tags(Arrays.asList("shoes", "running", "athletic", "comfortable"))
                            .features(Arrays.asList("Max Air cushioning", "Breathable mesh", "Durable outsole", "Lightweight design"))
                            .stockQuantity(200)
                            .rating(new BigDecimal("4.7"))
                            .reviewCount(5632)
                            .imageUrl("https://example.com/nike-airmax.jpg")
                            .build(),

                    // Home & Garden
                    Product.builder()
                            .name("Dyson V15 Detect Cordless Vacuum")
                            .description("Laser reveals invisible dust, intelligently adapts power and run time, whole-machine HEPA filtration, converts to handheld")
                            .category(Category.HOME_GARDEN)
                            .price(new BigDecimal("649.99"))
                            .brand("Dyson")
                            .sku("DYS-V15-DETECT")
                            .tags(Arrays.asList("vacuum", "cordless", "cleaning", "premium"))
                            .features(Arrays.asList("Laser dust detection", "60-minute runtime", "HEPA filtration", "LCD screen"))
                            .stockQuantity(40)
                            .rating(new BigDecimal("4.8"))
                            .reviewCount(2341)
                            .imageUrl("https://example.com/dyson-v15.jpg")
                            .build(),

                    Product.builder()
                            .name("KitchenAid Artisan Stand Mixer")
                            .description("5-quart stainless steel bowl, 10 speeds, tilt-head design, includes flat beater, dough hook and wire whip. Perfect for all your mixing, kneading and whipping needs.")
                            .category(Category.HOME_GARDEN)
                            .price(new BigDecimal("449.99"))
                            .brand("KitchenAid")
                            .sku("KA-ARTISAN-RED")
                            .tags(Arrays.asList("kitchen", "mixer", "baking", "cooking"))
                            .features(Arrays.asList("5-quart capacity", "10 speeds", "Tilt-head design", "Multiple attachments"))
                            .stockQuantity(60)
                            .rating(new BigDecimal("4.9"))
                            .reviewCount(12456)
                            .imageUrl("https://example.com/kitchenaid-mixer.jpg")
                            .build(),

                    // Sports & Outdoors
                    Product.builder()
                            .name("Hydro Flask 32oz Wide Mouth Water Bottle")
                            .description("TempShield double-wall vacuum insulation keeps drinks cold for 24 hours, hot for 12. BPA-Free, dishwasher safe, fits most car cup holders.")
                            .category(Category.SPORTS_OUTDOORS)
                            .price(new BigDecimal("44.95"))
                            .brand("Hydro Flask")
                            .sku("HF-32WM-BLK")
                            .tags(Arrays.asList("water-bottle", "insulated", "outdoor", "sustainable"))
                            .features(Arrays.asList("TempShield insulation", "32oz capacity", "BPA-Free", "Dishwasher safe"))
                            .stockQuantity(300)
                            .rating(new BigDecimal("4.7"))
                            .reviewCount(9876)
                            .imageUrl("https://example.com/hydroflask.jpg")
                            .build(),

                    Product.builder()
                            .name("Peloton Bike+ with 24-inch HD Touchscreen")
                            .description("Auto-Follow resistance matches your instructor's cues, rotating HD touchscreen for floor workouts, premium sound system, access to live and on-demand classes.")
                            .category(Category.SPORTS_OUTDOORS)
                            .price(new BigDecimal("2495.00"))
                            .brand("Peloton")
                            .sku("PEL-BIKEPLUS")
                            .tags(Arrays.asList("exercise-bike", "fitness", "premium", "smart"))
                            .features(Arrays.asList("24-inch HD touchscreen", "Auto-Follow resistance", "Premium sound", "Live classes"))
                            .stockQuantity(15)
                            .rating(new BigDecimal("4.6"))
                            .reviewCount(4521)
                            .imageUrl("https://example.com/peloton-bike.jpg")
                            .build(),

                    // Books
                    Product.builder()
                            .name("Atomic Habits by James Clear")
                            .description("An Easy & Proven Way to Build Good Habits & Break Bad Ones. Transform your life with tiny changes that deliver remarkable results.")
                            .category(Category.BOOKS)
                            .price(new BigDecimal("16.99"))
                            .brand("Penguin Random House")
                            .sku("BOOK-ATOMIC-HB")
                            .tags(Arrays.asList("self-help", "productivity", "habits", "bestseller"))
                            .features(Arrays.asList("Hardcover", "336 pages", "Bestseller", "Practical strategies"))
                            .stockQuantity(500)
                            .rating(new BigDecimal("4.8"))
                            .reviewCount(45234)
                            .imageUrl("https://example.com/atomic-habits.jpg")
                            .build(),

                    // Beauty & Personal Care
                    Product.builder()
                            .name("Neutrogena Hydro Boost Water Gel")
                            .description("Hyaluronic acid formula provides intense hydration, absorbs quickly like a gel but has the long-lasting moisturizing power of a cream. Oil-free, dye-free, fragrance-free.")
                            .category(Category.BEAUTY_PERSONAL_CARE)
                            .price(new BigDecimal("19.99"))
                            .brand("Neutrogena")
                            .sku("NEU-HYDROBOOST-50ML")
                            .tags(Arrays.asList("skincare", "moisturizer", "hydration", "hyaluronic-acid"))
                            .features(Arrays.asList("Hyaluronic acid", "Oil-free", "Non-comedogenic", "Quick absorption"))
                            .stockQuantity(250)
                            .rating(new BigDecimal("4.6"))
                            .reviewCount(8734)
                            .imageUrl("https://example.com/neutrogena-gel.jpg")
                            .build(),

                    // Health & Wellness
                    Product.builder()
                            .name("Optimum Nutrition Gold Standard Whey Protein")
                            .description("24g of premium whey protein per serving, 5.5g of naturally occurring BCAAs, over 20 delicious flavors, gluten-free. The world's best-selling whey protein powder.")
                            .category(Category.HEALTH_WELLNESS)
                            .price(new BigDecimal("64.99"))
                            .brand("Optimum Nutrition")
                            .sku("ON-WHEY-CHOC-5LB")
                            .tags(Arrays.asList("protein", "fitness", "supplement", "nutrition"))
                            .features(Arrays.asList("24g protein", "5.5g BCAAs", "Gluten-free", "Premium quality"))
                            .stockQuantity(100)
                            .rating(new BigDecimal("4.7"))
                            .reviewCount(34521)
                            .imageUrl("https://example.com/whey-protein.jpg")
                            .build()
            );

            // Save all products
            List<Product> savedProducts = productRepository.saveAll(sampleProducts);
            log.info("Saved {} sample products to database", savedProducts.size());

            // Add products to vector store for semantic search
            try {
                log.info("Adding products to vector store...");
                vectorStoreService.addProducts(savedProducts);
                log.info("Successfully added {} products to vector store", savedProducts.size());
            } catch (Exception e) {
                log.error("Failed to add products to vector store: {}", e.getMessage());
                log.info("Products saved to database but vector store population failed. " +
                        "You can manually ingest using POST /api/products/ingest");
            }

            log.info("Sample data loading complete!");
        };
    }
}
