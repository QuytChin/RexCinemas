package com.rexchain.cinema.service;
import com.rexchain.cinema.dto.SeatUpdateMessage; import org.springframework.messaging.simp.SimpMessagingTemplate; import org.springframework.stereotype.Service; import java.util.List;
@Service public class SeatBroadcastService{private final SimpMessagingTemplate t; public SeatBroadcastService(SimpMessagingTemplate t){this.t=t;} public void send(Long showtimeId,List<Long> ids,String status){t.convertAndSend("/topic/showtimes/"+showtimeId+"/seats",new SeatUpdateMessage(showtimeId,ids,status));}}
