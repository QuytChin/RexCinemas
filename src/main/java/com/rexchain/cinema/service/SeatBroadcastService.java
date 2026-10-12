package com.rexchain.cinema.service;

import com.rexchain.cinema.dto.SeatUpdateMessage;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.List;

@Service
public class SeatBroadcastService {
    private final SimpMessagingTemplate t;

    public SeatBroadcastService(SimpMessagingTemplate t) {
        this.t = t;
    }

    public void send(Long showtimeId, List<Long> ids, String status) {
        List<Long> snapshot = List.copyOf(ids);
        Runnable publish = () -> t.convertAndSend(
                "/topic/showtimes/" + showtimeId + "/seats",
                new SeatUpdateMessage(showtimeId, snapshot, status));
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            publish.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publish.run();
            }
        });
    }
}
