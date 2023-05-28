package org.uci.spacifyPortal.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.uci.spacifyLib.entity.MonitoringEntity;
import org.uci.spacifyLib.repository.MonitoringRepository;
import org.uci.spacifyLib.repository.RoomRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class OccupancyService {
    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private MonitoringRepository monitoringRepository;

    public List<Long> getRoomsWithZeroOccupancy(List<Long> roomIds) {
        LocalDateTime current_time = LocalDateTime.parse("2023-02-22T12:00:00"); // HARDCODED

        List<Long> roomIdsWithZeroOccupancy = new ArrayList<>();
        Set<Long> monitoringIdsWithZeroOccupancy = new HashSet<>();

        for (Long roomId : roomIds) {
            Integer tippers_room_id = roomRepository.findByRoomId(roomId).getTippersSpaceId();
            List<MonitoringEntity> monitoringObjects = monitoringRepository.findAllByTippersSpaceId(tippers_room_id);
            List<LocalDateTime> timestampToValues = new ArrayList<>(); // timestamp_to values for the given room ID

            LocalDateTime fifteenMinutesBeforeCurrentTime = current_time.minusMinutes(15);

            for (MonitoringEntity monitoringObject : monitoringObjects) {
                LocalDateTime timestampTo = monitoringObject.getTimestampTo();
                if (timestampTo.isEqual(current_time) || (timestampTo.isAfter(fifteenMinutesBeforeCurrentTime) && timestampTo.isBefore(current_time))) {
                    timestampToValues.add(timestampTo);
                }
            }

            // Check if the occupancy is zero for timestampToValues
            boolean hasZeroOccupancy = false;
            for (LocalDateTime timestampToValue : timestampToValues) {
                List<MonitoringEntity> monitoringList = monitoringRepository.findByTippersSpaceIdAndTimestampTo(tippers_room_id, timestampToValue);
                for (MonitoringEntity monitoring : monitoringList) {
                    if (monitoring.getRoomOccupancy() == 0) {
                        hasZeroOccupancy = true;
                        monitoringIdsWithZeroOccupancy.add(monitoring.getMonitoring_id());

                        break; // No need to continue checking other entities with the same timestamp
                    }
                }
                if (hasZeroOccupancy) {
                    break; // No need to check further timestamps
                }
            }

            if (hasZeroOccupancy) {
                roomIdsWithZeroOccupancy.add(roomId);
            }
        }

//        return roomIdsWithZeroOccupancy;
        return new ArrayList<>(monitoringIdsWithZeroOccupancy);
    }







}
