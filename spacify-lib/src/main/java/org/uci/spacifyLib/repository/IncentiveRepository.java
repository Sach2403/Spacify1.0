package org.uci.spacifyLib.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.uci.spacifyLib.entity.IncentiveEntity;

import java.util.List;

@Repository
public interface IncentiveRepository extends JpaRepository<IncentiveEntity, Long> {
    public List<IncentiveEntity> findAllByUserId(String userId);

    public List<IncentiveEntity> findAllByAdded(Boolean added);
}
