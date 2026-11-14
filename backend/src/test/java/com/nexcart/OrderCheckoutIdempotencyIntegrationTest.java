package com.nexcart;

import com.nexcart.entity.*;
import com.nexcart.repository.*;
import com.nexcart.service.CouponService;
import com.nexcart.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class OrderCheckoutIdempotencyIntegrationTest {
    @DynamicPropertySource
    static void forceIsolatedInMemoryDatabase(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> "jdbc:h2:mem:checkout-idempotency-test;MODE=MySQL;DB_CLOSE_DELAY=-1");
        properties.add("spring.datasource.username", () -> "sa");
        properties.add("spring.datasource.password", () -> "");
        properties.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        properties.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        properties.add("spring.flyway.enabled", () -> "false");
    }

    @Autowired private OrderService orderService;
    @Autowired private UserRepository users;
    @Autowired private CategoryRepository categories;
    @Autowired private BrandRepository brands;
    @Autowired private ProductRepository products;
    @Autowired private CartRepository carts;
    @Autowired private OrderRepository orders;
    @Autowired private CheckoutIdempotencyKeyRepository idempotencyKeys;
    @Autowired private CouponRepository couponRepository;
    @Autowired private PlatformTransactionManager transactionManager;
    @MockBean private CouponService couponService;

    @Test
    void identicalCheckoutKeyReturnsOriginalOrderAndClearsCartOnlyOnce() {
        Fixture f = fixture(5, 1);

        var first = submit(f.email(), "same-checkout-key-1234567890");
        var replay = submit(f.email(), "same-checkout-key-1234567890");

        assertEquals(first.getOrderId(), replay.getOrderId());
        assertEquals(1, orderCount(f.userId()));
        assertEquals(4, productStock(f.productId()));
        assertTrue(cartEmpty(f.userId()));
    }

    @Test
    void simultaneousRequestsWithSameKeyCreateOneOrderAndReserveStockOnce() throws Exception {
        Fixture f = fixture(5, 1);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var first = pool.submit(() -> concurrentSubmit(f.email(), "parallel-checkout-key-123456", ready, start));
            var second = pool.submit(() -> concurrentSubmit(f.email(), "parallel-checkout-key-123456", ready, start));
            assertTrue(ready.await(10, java.util.concurrent.TimeUnit.SECONDS));
            start.countDown();
            var firstOrder = first.get(30, java.util.concurrent.TimeUnit.SECONDS);
            var secondOrder = second.get(30, java.util.concurrent.TimeUnit.SECONDS);
            assertEquals(firstOrder.getOrderId(), secondOrder.getOrderId());
            assertEquals(1, orderCount(f.userId()));
            assertEquals(4, productStock(f.productId()));
        } finally {
            start.countDown();
            pool.shutdownNow();
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void newIdempotencyKeyAllowsASeparatePurchaseAfterCartIsRepopulated() {
        Fixture f = fixture(5, 1);
        var first = submit(f.email(), "legitimate-purchase-one-123456");
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            User user = users.findById(f.userId()).orElseThrow();
            Product product = products.findById(f.productId()).orElseThrow();
            carts.save(Cart.builder().user(user).product(product).quantity(1).build());
        });

        var second = submit(f.email(), "legitimate-purchase-two-123456");

        assertNotEquals(first.getOrderId(), second.getOrderId());
        assertEquals(2, orderCount(f.userId()));
        assertEquals(3, productStock(f.productId()));
    }

    @Test
    void insufficientStockRollsBackIdempotencyReservationAndKeepsCart() {
        Fixture f = fixture(1, 2);

        assertThrows(RuntimeException.class, () -> submit(f.email(), "insufficient-stock-key-123456"));

        assertEquals(0, orderCount(f.userId()));
        assertEquals(1, productStock(f.productId()));
        assertFalse(cartEmpty(f.userId()));
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                assertTrue(idempotencyKeys.findByUserAndKeyForUpdate(f.userId(), "insufficient-stock-key-123456").isEmpty()));
    }

    @Test
    void downstreamFailureRollsBackStockOrderCartAndIdempotencyTogether() {
        Fixture f = fixture(4, 2);
        Coupon coupon = new TransactionTemplate(transactionManager).execute(status -> couponRepository.save(Coupon.builder()
                .code("FAIL-" + UUID.randomUUID().toString().substring(0, 8))
                .description("rollback test")
                .discountType(DiscountType.FLAT)
                .discountValue(BigDecimal.ZERO)
                .minimumOrderAmount(BigDecimal.ZERO)
                .expiryDate(LocalDateTime.now().plusDays(1))
                .active(true).build()));
        when(couponService.validateCoupon(eq(coupon.getCode()), any(BigDecimal.class), any(User.class)))
                .thenReturn(coupon);
        doThrow(new IllegalStateException("simulated downstream failure"))
                .when(couponService).recordUsage(eq(coupon), any(User.class), any(Order.class));

        assertThrows(IllegalStateException.class, () -> submit(f.email(), "rollback-checkout-key-123456", coupon.getCode()));

        assertEquals(0, orderCount(f.userId()));
        assertEquals(4, productStock(f.productId()));
        assertFalse(cartEmpty(f.userId()));
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                assertTrue(idempotencyKeys.findByUserAndKeyForUpdate(f.userId(), "rollback-checkout-key-123456").isEmpty()));
    }

    private Fixture fixture(int stock, int quantity) {
        String suffix = UUID.randomUUID().toString();
        return new TransactionTemplate(transactionManager).execute(status -> {
            User user = users.save(User.builder().fullName("Checkout Test").email("checkout-" + suffix + "@example.test")
                    .password("test-only").phoneNumber("8" + suffix.replaceAll("\\D", "").substring(0, 9))
                    .role(Role.CUSTOMER).enabled(true).accountNonLocked(true).accountNonExpired(true)
                    .credentialsNonExpired(true).build());
            Category category = categories.save(Category.builder().name("Category " + suffix).build());
            Brand brand = brands.save(Brand.builder().name("Brand " + suffix).build());
            Product product = products.save(Product.builder().name("Product " + suffix).sku("SKU-" + suffix)
                    .price(new BigDecimal("10.00")).stock(stock).active(true).category(category).brand(brand).build());
            carts.save(Cart.builder().user(user).product(product).quantity(quantity).build());
            return new Fixture(user.getId(), product.getId(), user.getEmail());
        });
    }

    private com.nexcart.dto.response.OrderResponse submit(String email, String key) {
        return submit(email, key, null);
    }

    private com.nexcart.dto.response.OrderResponse submit(String email, String key, String couponCode) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(email, null));
        try { return orderService.placeOrder(couponCode, key); }
        finally { SecurityContextHolder.clearContext(); }
    }

    private com.nexcart.dto.response.OrderResponse concurrentSubmit(String email, String key,
                                                                     CountDownLatch ready, CountDownLatch start) throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(email, null));
        ready.countDown();
        try {
            if (!start.await(10, java.util.concurrent.TimeUnit.SECONDS)) throw new IllegalStateException("start barrier timed out");
            return orderService.placeOrder(null, key);
        } finally { SecurityContextHolder.clearContext(); }
    }

    private long orderCount(Long userId) {
        return orders.findByUser(users.findById(userId).orElseThrow(), PageRequest.of(0, 20)).getTotalElements();
    }
    private int productStock(Long productId) { return products.findById(productId).orElseThrow().getStock(); }
    private boolean cartEmpty(Long userId) { return carts.findByUser(users.findById(userId).orElseThrow()).isEmpty(); }
    private record Fixture(Long userId, Long productId, String email) { }
}
