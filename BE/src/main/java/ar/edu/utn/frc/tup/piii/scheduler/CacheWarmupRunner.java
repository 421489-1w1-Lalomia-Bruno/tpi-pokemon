package ar.edu.utn.frc.tup.piii.scheduler;

import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.boot.ApplicationArguments;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.PageRequest;

import ar.edu.utn.frc.tup.piii.dtos.card.CardFilterDTO;
import ar.edu.utn.frc.tup.piii.services.CardService;

@Component
@Order(2)
public class CacheWarmupRunner implements ApplicationRunner {
    private final CardService cardService;

    public CacheWarmupRunner(CardService cardService) {
        this.cardService = cardService;
    }

    @Override
    public void run(ApplicationArguments args) {
        cardService.listCards(new CardFilterDTO(), PageRequest.of(0, 200));
    }
}
