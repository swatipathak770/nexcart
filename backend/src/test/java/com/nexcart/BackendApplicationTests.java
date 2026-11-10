package com.nexcart;

import com.nexcart.entity.*;
import com.nexcart.repository.BrandRepository;
import com.nexcart.repository.CartRepository;
import com.nexcart.repository.CategoryRepository;
import com.nexcart.repository.OrderRepository;
import com.nexcart.repository.ProductRepository;
import com.nexcart.repository.UserRepository;
import com.nexcart.recovery.service.RecoveryService;
import com.nexcart.recovery.entity.RecoveryCase;
import com.nexcart.recovery.enums.RecoveryActionType;
import com.nexcart.recovery.enums.RecoveryStatus;
import com.nexcart.recovery.enums.RecoveryType;
import com.nexcart.recovery.repository.RecoveryCaseRepository;
import com.nexcart.service.OrderService;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class BackendApplicationTests {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private OrderService orderService;

    @Autowired
    private RecoveryService recoveryService;

    @Autowired
    private RecoveryCaseRepository recoveryCaseRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void contextLoads() {
    }

    @Test
    @Transactional
    void pagedOrderItemsAndProductsAreLoadedWithBoundedQueryCount() {
        String suffix = UUID.randomUUID().toString();
        User user = User.builder()
                .fullName("Query Count Test")
                .email("query-" + suffix + "@example.test")
                .password("not-a-real-password")
                .phoneNumber("9" + suffix.replaceAll("\\D", "").substring(0, 9))
                .role(Role.CUSTOMER)
                .enabled(true)
                .accountNonLocked(true)
                .accountNonExpired(true)
                .credentialsNonExpired(true)
                .build();
        entityManager.persist(user);

        Category category = Category.builder().name("Query Category " + suffix).build();
        Brand brand = Brand.builder().name("Query Brand " + suffix).build();
        entityManager.persist(category);
        entityManager.persist(brand);

        List<Product> products = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Product product = Product.builder()
                    .name("Query Product " + i + " " + suffix)
                    .sku("QUERY-" + suffix + "-" + i)
                    .price(new BigDecimal("10.00"))
                    .stock(10)
                    .active(true)
                    .category(category)
                    .brand(brand)
                    .build();
            entityManager.persist(product);
            products.add(product);
        }

        for (int orderIndex = 0; orderIndex < 2; orderIndex++) {
            Order order = Order.builder()
                    .user(user)
                    .totalAmount(new BigDecimal("20.00"))
                    .discountAmount(BigDecimal.ZERO)
                    .finalAmount(new BigDecimal("20.00"))
                    .status(OrderStatus.PENDING)
                    .orderItems(new ArrayList<>())
                    .build();
            for (int itemIndex = 0; itemIndex < 2; itemIndex++) {
                Product product = products.get(orderIndex * 2 + itemIndex);
                order.getOrderItems().add(OrderItem.builder()
                        .order(order)
                        .product(product)
                        .quantity(1)
                        .price(product.getPrice())
                        .build());
            }
            entityManager.persist(order);
        }

        entityManager.flush();
        Long userId = user.getId();
        entityManager.clear();
        User persistedUser = userRepository.findById(userId).orElseThrow();
        Statistics statistics = entityManager.getEntityManagerFactory()
                .unwrap(SessionFactory.class)
                .getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();

        Page<Order> page = orderRepository.findByUser(persistedUser, PageRequest.of(0, 2));
        page.getContent().forEach(order -> order.getOrderItems().forEach(item ->
                item.getProduct().getName().length()));

        long queryCount = statistics.getPrepareStatementCount();
        System.out.println("Measured paged-order serialization SQL statements: " + queryCount);
        assertEquals(2, page.getTotalElements());
        assertTrue(queryCount <= 6,
                "Paged order serialization should avoid a select per associated product");
    }

    @Test
    void simultaneousCheckoutsCannotOversellTheLastUnit() throws Exception {
        String suffix = UUID.randomUUID().toString();
        String firstEmail = "checkout-a-" + suffix + "@example.test";
        String secondEmail = "checkout-b-" + suffix + "@example.test";
        TransactionTemplate transactions = new TransactionTemplate(transactionManager);
        CheckoutFixture fixture = transactions.execute(status -> {
            User first = testUser(firstEmail, "8" + suffix.replaceAll("\\D", "").substring(0, 9));
            User second = testUser(secondEmail, "9" + suffix.replaceAll("\\D", "").substring(0, 9));
            userRepository.saveAll(List.of(first, second));
            Category category = categoryRepository.save(Category.builder().name("Checkout Category " + suffix).build());
            Brand brand = brandRepository.save(Brand.builder().name("Checkout Brand " + suffix).build());
            Product product = productRepository.save(Product.builder()
                    .name("Last unit " + suffix).sku("LAST-" + suffix)
                    .price(new BigDecimal("19.99")).stock(1).active(true)
                    .category(category).brand(brand).build());
            cartRepository.save(Cart.builder().user(first).product(product).quantity(1).build());
            cartRepository.save(Cart.builder().user(second).product(product).quantity(1).build());
            return new CheckoutFixture(first.getId(), second.getId(), product.getId(),
                    category.getId(), brand.getId(), firstEmail, secondEmail);
        });

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> first = executor.submit(() -> placeConcurrentOrder(fixture.firstEmail(), ready, start));
            Future<Boolean> second = executor.submit(() -> placeConcurrentOrder(fixture.secondEmail(), ready, start));
            assertTrue(ready.await(10, TimeUnit.SECONDS), "Both checkout workers should reach the start barrier");
            start.countDown();

            boolean firstPlaced = first.get(30, TimeUnit.SECONDS);
            boolean secondPlaced = second.get(30, TimeUnit.SECONDS);
            assertTrue(firstPlaced ^ secondPlaced, "Exactly one checkout should purchase the final unit");

            transactions.executeWithoutResult(status -> {
                assertEquals(0, productRepository.findById(fixture.productId()).orElseThrow().getStock());
                long firstOrders = orderRepository.findByUser(
                        userRepository.findById(fixture.firstUserId()).orElseThrow(), PageRequest.of(0, 10)).getTotalElements();
                long secondOrders = orderRepository.findByUser(
                        userRepository.findById(fixture.secondUserId()).orElseThrow(), PageRequest.of(0, 10)).getTotalElements();
                assertEquals(1, firstOrders + secondOrders);
            });
        } finally {
            start.countDown();
            executor.shutdownNow();
            transactions.executeWithoutResult(status -> {
                User first = userRepository.findById(fixture.firstUserId()).orElse(null);
                User second = userRepository.findById(fixture.secondUserId()).orElse(null);
                if (first != null) cartRepository.deleteByUser(first);
                if (second != null) cartRepository.deleteByUser(second);
                if (first != null) orderRepository.findByUser(first, PageRequest.of(0, 10))
                        .forEach(orderRepository::delete);
                if (second != null) orderRepository.findByUser(second, PageRequest.of(0, 10))
                        .forEach(orderRepository::delete);
                productRepository.deleteById(fixture.productId());
                categoryRepository.deleteById(fixture.categoryId());
                brandRepository.deleteById(fixture.brandId());
                if (first != null) userRepository.delete(first);
                if (second != null) userRepository.delete(second);
            });
        }
    }

    @Test
    void recoveryMetricsUseBoundedDatabaseAggregates() {
        Statistics statistics = entityManager.getEntityManagerFactory()
                .unwrap(SessionFactory.class)
                .getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();

        var metrics = recoveryService.metrics("ALL");

        long queryCount = statistics.getPrepareStatementCount();
        assertTrue(queryCount <= 5, "Recovery dashboard metrics should use a fixed number of aggregate queries");
        assertTrue(metrics.containsKey("revenueAtRisk"));
        assertTrue(metrics.containsKey("revenueRecovered"));
        assertTrue(metrics.containsKey("expectedRecoverable"));
        assertTrue(metrics.containsKey("casesDetected"));
        assertTrue(metrics.containsKey("recoveryRate"));
        assertTrue(metrics.containsKey("actionDistribution"));
        assertTrue(metrics.containsKey("simulatedRevenueRecovered"));
        System.out.println("Measured RecoverAI aggregate metric SQL statements: " + queryCount);
    }

    @Test
    @Transactional
    void recoveryMetricsPreserveRealSimulatedAndCombinedAccounting() {
        var realBaseline = recoveryService.metrics("REAL");
        var simulatedBaseline = recoveryService.metrics("SIMULATED");
        var allBaseline = recoveryService.metrics("ALL");

        recoveryCaseRepository.save(recoveryCase(false, RecoveryStatus.DETECTED,
                "100.00", "50.00", "0.00", RecoveryActionType.RETRY_PAYMENT));
        recoveryCaseRepository.save(recoveryCase(false, RecoveryStatus.RECOVERED,
                "30.00", "10.00", "30.00", RecoveryActionType.CREATE_PAYMENT_LINK));
        recoveryCaseRepository.save(recoveryCase(true, RecoveryStatus.RECOVERED,
                "20.00", "8.00", "20.00", RecoveryActionType.NO_ACTION));
        recoveryCaseRepository.save(recoveryCase(false, RecoveryStatus.CUSTOMER_CANCELLED,
                "99.00", "90.00", "0.00", RecoveryActionType.NO_ACTION));

        var real = recoveryService.metrics("REAL");
        var simulated = recoveryService.metrics("SIMULATED");
        var all = recoveryService.metrics("ALL");

        assertEquals(((BigDecimal) realBaseline.get("revenueAtRisk")).add(new BigDecimal("100.00")), real.get("revenueAtRisk"));
        assertEquals(((BigDecimal) realBaseline.get("revenueRecovered")).add(new BigDecimal("30.00")), real.get("revenueRecovered"));
        assertEquals(((BigDecimal) realBaseline.get("expectedRecoverable")).add(new BigDecimal("50.00")), real.get("expectedRecoverable"));
        assertEquals(((Number) realBaseline.get("casesDetected")).longValue() + 3, ((Number) real.get("casesDetected")).longValue());
        assertEquals(((Number) realBaseline.get("recoveredCases")).longValue() + 1, ((Number) real.get("recoveredCases")).longValue());

        assertEquals(simulatedBaseline.get("revenueAtRisk"), simulated.get("revenueAtRisk"));
        assertEquals(((BigDecimal) simulatedBaseline.get("revenueRecovered")).add(new BigDecimal("20.00")), simulated.get("revenueRecovered"));
        assertEquals(simulatedBaseline.get("expectedRecoverable"), simulated.get("expectedRecoverable"));
        assertEquals(((Number) simulatedBaseline.get("casesDetected")).longValue() + 1, ((Number) simulated.get("casesDetected")).longValue());
        assertEquals(((Number) simulatedBaseline.get("recoveredCases")).longValue() + 1, ((Number) simulated.get("recoveredCases")).longValue());

        assertEquals(((BigDecimal) allBaseline.get("revenueAtRisk")).add(new BigDecimal("100.00")), all.get("revenueAtRisk"));
        assertEquals(((BigDecimal) allBaseline.get("revenueRecovered")).add(new BigDecimal("30.00")), all.get("revenueRecovered"));
        assertEquals(((BigDecimal) allBaseline.get("expectedRecoverable")).add(new BigDecimal("50.00")), all.get("expectedRecoverable"));
        assertEquals(((Number) allBaseline.get("casesDetected")).longValue() + 4, ((Number) all.get("casesDetected")).longValue());
        assertEquals(((Number) allBaseline.get("recoveredCases")).longValue() + 1, ((Number) all.get("recoveredCases")).longValue());
        assertEquals(((BigDecimal) simulatedBaseline.get("revenueRecovered")).add(new BigDecimal("20.00")), all.get("simulatedRevenueRecovered"));
    }

    private RecoveryCase recoveryCase(boolean simulated, RecoveryStatus status, String amount,
                                      String expected, String recovered, RecoveryActionType action) {
        return RecoveryCase.builder()
                .amount(new BigDecimal(amount))
                .expectedRecoveryAmount(new BigDecimal(expected))
                .recoveredAmount(new BigDecimal(recovered))
                .currency("INR")
                .type(RecoveryType.PAYMENT_FAILED)
                .failureReason("Aggregate accounting test")
                .status(status)
                .recommendedAction(action)
                .actionAttempts(0)
                .simulated(simulated)
                .build();
    }

    private boolean placeConcurrentOrder(String email, CountDownLatch ready, CountDownLatch start) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null));
        ready.countDown();
        try {
            if (!start.await(10, TimeUnit.SECONDS)) return false;
            orderService.placeOrder(null);
            return true;
        } catch (RuntimeException exception) {
            return false;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private User testUser(String email, String phone) {
        return User.builder().fullName("Concurrency Test").email(email).password("not-a-real-password")
                .phoneNumber(phone).role(Role.CUSTOMER).enabled(true).accountNonLocked(true)
                .accountNonExpired(true).credentialsNonExpired(true).build();
    }

    private record CheckoutFixture(Long firstUserId, Long secondUserId, Long productId,
                                   Long categoryId, Long brandId, String firstEmail, String secondEmail) { }
}
