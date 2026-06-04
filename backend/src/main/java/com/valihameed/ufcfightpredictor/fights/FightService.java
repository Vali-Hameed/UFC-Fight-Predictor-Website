package com.valihameed.ufcfightpredictor.fights;

import com.valihameed.ufcfightpredictor.models.Fight;
import com.valihameed.ufcfightpredictor.repository.FightRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class FightService {
    private final FightRepository fightRepository;

    public List<Fight> findByEventId(Long eventId) { return fightRepository.findByEventIdOrderByFightOrderAsc(eventId); }

    public Fight create(Fight fight) { return fightRepository.save(fight); }

    public Optional<Fight> findById(Long id) { return fightRepository.findById(id); }

    public Fight update(Long id, Fight updated) {
        return fightRepository.findById(id).map(f -> {
            f.setFighter1Name(updated.getFighter1Name());
            f.setFighter2Name(updated.getFighter2Name());
            f.setWeightClass(updated.getWeightClass());
            f.setIsMainEvent(updated.getIsMainEvent());
            f.setFightOrder(updated.getFightOrder());
            f.setStatus(updated.getStatus());
            return fightRepository.save(f);
        }).orElseThrow(() -> new RuntimeException("Fight not found"));
    }
}
