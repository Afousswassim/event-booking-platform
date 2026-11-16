package com.eventbooking.service;

import com.eventbooking.BaseJpaTest;
import com.eventbooking.model.*;
import com.eventbooking.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class ConcurrencyTest extends BaseJpaTest {

    @Test
    @DisplayName("Test de concurrence : 2 clients réservent simultanément les dernières places (anti-surréservation)")
    public void testConcurrentReservationsNoOverselling() throws Exception {
        // Initialisation des données : événement avec exactement 3 places restantes
        Long eventId;
        Long user1Id;
        Long user2Id;

        CategoryRepository categoryRepo = new CategoryRepository();
        EventRepository eventRepo = new EventRepository();
        UserRepository userRepo = new UserRepository();

        injectEntityManager(categoryRepo, em);
        injectEntityManager(eventRepo, em);
        injectEntityManager(userRepo, em);

        EntityTransaction tx = em.getTransaction();
        tx.begin();

        User user1 = new User("Client 1", "client1_" + System.nanoTime() + "@test.com", "pass", Role.CLIENT);
        User user2 = new User("Client 2", "client2_" + System.nanoTime() + "@test.com", "pass", Role.CLIENT);
        userRepo.save(user1);
        userRepo.save(user2);
        user1Id = user1.getId();
        user2Id = user2.getId();

        Category cat = categoryRepo.save(new Category("Concert Limité", "Places rares"));
        Event limitedEvent = new Event(
                "Concert VIP Exclusif", "Dernières places",
                LocalDateTime.now().plusDays(10), "Club VIP",
                3, // Capacité exacte : 3 places
                new BigDecimal("100.00"), cat
        );
        limitedEvent.setStatut(EventStatus.PUBLIE);
        eventRepo.save(limitedEvent);
        eventId = limitedEvent.getId();

        tx.commit();

        // Préparation de 2 threads concurrents tentant chacun de réserver 2 places (2 + 2 = 4 > 3 places disponibles)
        int threadsCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadsCount);
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(threadsCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        Long[] users = new Long[]{user1Id, user2Id};

        for (int i = 0; i < threadsCount; i++) {
            final Long uid = users[i];
            executor.submit(() -> {
                // Chaque requête cliente possède son propre EntityManager et contexte de transaction
                EntityManager threadEm = emf.createEntityManager();
                try {
                    ReservationRepository resRepoThread = new ReservationRepository();
                    EventRepository evRepoThread = new EventRepository();
                    UserRepository uRepoThread = new UserRepository();

                    injectEntityManager(resRepoThread, threadEm);
                    injectEntityManager(evRepoThread, threadEm);
                    injectEntityManager(uRepoThread, threadEm);

                    ReservationService serviceThread = new ReservationService(resRepoThread, evRepoThread, uRepoThread);

                    startSignal.await(); // Synchronisation pour départ simultané

                    EntityTransaction threadTx = threadEm.getTransaction();
                    threadTx.begin();
                    try {
                        serviceThread.reserver(uid, eventId, 2);
                        threadTx.commit();
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        if (threadTx.isActive()) {
                            threadTx.rollback();
                        }
                        failureCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    if (threadEm.isOpen()) {
                        threadEm.close();
                    }
                    doneSignal.countDown();
                }
            });
        }

        // Lancement simultané des 2 transactions
        startSignal.countDown();
        boolean completed = doneSignal.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();

        // Exactement une réservation a dû réussir (2 places) et l'autre échouer (car 2 > 1 place restante)
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(1);

        // Vérification de la cohérence finale en base
        EntityManager verifyEm = emf.createEntityManager();
        try {
            ReservationRepository resRepo = new ReservationRepository();
            injectEntityManager(resRepo, verifyEm);
            int totalPlacesReservees = resRepo.countReservedPlaces(eventId);

            // Pas de surréservation : exactement 2 places réservées sur les 3 disponibles
            assertThat(totalPlacesReservees).isEqualTo(2);
        } finally {
            verifyEm.close();
        }
    }
}
