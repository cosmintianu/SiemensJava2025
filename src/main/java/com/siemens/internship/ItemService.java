package com.siemens.internship;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ItemService {
    @Autowired
    private ItemRepository itemRepository;

    // New Thread-safe data structures
    private final List<Item> processedItems = Collections.synchronizedList(new ArrayList<>());
    private final AtomicInteger processedCount = new AtomicInteger(0);


    /**
     * Initial problems:
     * 1. We were iterating over the list of ids and then creating separate threads
     * -> It is better to pass it to lambda to eliminate unexpected behaviour
     * <p>
     * 2. Use of non thread safe data structures for processedCount and processedItems
     * -> I used AtomicInteger and a synchronizedList respectively which support concurrency
     * <p>
     * 3. Returning the processedItems immediately, thus being a chance that the returned
     * list has not been processed completely
     * -> We need to wait so all the threads finish their processing so all the items get
     * processed using CompletableFutures, by creating one for each async task and waiting
     * for all of them to finish and just then return the processed items*
     */
    @Async
    public  CompletableFuture<List<Item>> processItemsAsync() {

            List<Long> itemIds = itemRepository.findAllIds();

            List<CompletableFuture<Void>> futures = itemIds.stream()
                    .map(id -> CompletableFuture.runAsync(() -> {
                        try{
                            Thread.sleep(100);
                            System.out.println(Thread.currentThread().getName());
                            itemRepository.findById(id).ifPresent(item -> {
                                item.setStatus("PROCESSED");
                                itemRepository.save(item);
                                processedItems.add(item);
                                processedCount.incrementAndGet();
                            });

                        } catch (Exception e) {
                            System.err.println("Error processing item " + id + ": " + e.getMessage());
                        }
                    }))
                    .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[Integer.parseInt(String.valueOf(processedCount))]))
                .thenApplyAsync(v -> new ArrayList<>(processedItems));
    }

    public List<Item> findAll() {
        return itemRepository.findAll();
    }

    public Optional<Item> findById(Long id) {
        return itemRepository.findById(id);
    }

    public Item save(Item item) {
        return itemRepository.save(item);
    }

    public void deleteById(Long id) {
        itemRepository.deleteById(id);
    }

}

