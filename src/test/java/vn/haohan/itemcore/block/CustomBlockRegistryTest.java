package vn.haohan.itemcore.block;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.joml.Vector3f;
import vn.haohan.itemcore.api.block.CustomBlockDefinition;
import vn.haohan.itemcore.api.block.CustomBlockModel;
import vn.haohan.itemcore.api.block.CustomBlockRegistry;
import vn.haohan.itemcore.internal.block.DefaultCustomBlockRegistry;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CustomBlockRegistry Tests")
class CustomBlockRegistryTest {

    private CustomBlockRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new DefaultCustomBlockRegistry();
    }

    @Test
    @DisplayName("Should register and retrieve custom block definition successfully")
    void testRegisterAndGet() {
        CustomBlockModel model = new CustomBlockModel(null);
        assertEquals(new Vector3f(1.0f, 1.0f, 1.0f), model.getScale());
        assertEquals(new Vector3f(0.0f, 0.0f, 0.0f), model.getTranslation());

        CustomBlockDefinition def = CustomBlockDefinition.builder("haohan:ruby_block")
                .model(model)
                .dropItemId("haohan:ruby_block")
                .property("hardness", 3.0f)
                .build();

        registry.register(def);

        assertTrue(registry.has("haohan:ruby_block"));
        assertEquals(1, registry.size());

        Optional<CustomBlockDefinition> retrieved = registry.get("haohan:ruby_block");
        assertTrue(retrieved.isPresent());
        assertEquals("haohan:ruby_block", retrieved.get().getId());
        assertEquals("haohan:ruby_block", retrieved.get().getDropItemId());
        assertEquals(3.0f, retrieved.get().getProperties().get("hardness"));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException on duplicate ID")
    void testDuplicateRegistration() {
        CustomBlockDefinition def1 = CustomBlockDefinition.builder("haohan:copper_casing").build();
        CustomBlockDefinition def2 = CustomBlockDefinition.builder("haohan:copper_casing").build();

        registry.register(def1);
        assertThrows(IllegalArgumentException.class, () -> registry.register(def2));
    }

    @Test
    @DisplayName("Should throw NullPointerException when registering null definition or missing ID")
    void testNullValidation() {
        assertThrows(NullPointerException.class, () -> registry.register(null));
        assertThrows(NullPointerException.class, () -> CustomBlockDefinition.builder(null).build());
    }

    @Test
    @DisplayName("Should return empty optional for non-existent or null ID")
    void testGetNonExistent() {
        assertFalse(registry.has("non_existent"));
        assertTrue(registry.get("non_existent").isEmpty());
        assertFalse(registry.has(null));
        assertTrue(registry.get(null).isEmpty());
    }

    @Test
    @DisplayName("Should clear all definitions")
    void testClear() {
        registry.register(CustomBlockDefinition.builder("test:block_1").build());
        registry.register(CustomBlockDefinition.builder("test:block_2").build());
        assertEquals(2, registry.size());

        registry.clear();
        assertEquals(0, registry.size());
        assertFalse(registry.has("test:block_1"));
    }

    @Test
    @DisplayName("Should handle concurrent read and write operations safely")
    void testConcurrency() throws InterruptedException {
        int threadCount = 16;
        int operationsPerThread = 250;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger registerSuccessCount = new AtomicInteger();

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < operationsPerThread; i++) {
                        String id = "concurrent:block_" + threadId + "_" + i;
                        CustomBlockDefinition def = CustomBlockDefinition.builder(id).build();
                        registry.register(def);
                        registerSuccessCount.incrementAndGet();

                        assertTrue(registry.has(id));
                        assertTrue(registry.get(id).isPresent());
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean finished = latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(finished, "Concurrent operations did not complete in time");
        assertEquals(threadCount * operationsPerThread, registerSuccessCount.get());
        assertEquals(threadCount * operationsPerThread, registry.size());
        assertEquals(threadCount * operationsPerThread, registry.all().size());
    }
}
