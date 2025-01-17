package org.uci.spacifyLib.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.uci.spacifyLib.entity.AvailableSlotsEntity;
import org.uci.spacifyLib.entity.RoomType;

import java.util.List;

public interface AvailableSlotsRepository extends JpaRepository<AvailableSlotsEntity, Long> {
    List<AvailableSlotsEntity> findByroomType(RoomType room_type);
}
