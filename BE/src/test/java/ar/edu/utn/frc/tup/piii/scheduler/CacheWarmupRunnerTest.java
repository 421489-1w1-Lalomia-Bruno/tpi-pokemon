package ar.edu.utn.frc.tup.piii.scheduler;

import ar.edu.utn.frc.tup.piii.dtos.card.CardFilterDTO;
import ar.edu.utn.frc.tup.piii.services.CardService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.data.domain.PageRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CacheWarmupRunnerTest {

    @Mock
    private CardService cardService;

    @Captor
    private ArgumentCaptor<CardFilterDTO> filterCaptor;

    @Captor
    private ArgumentCaptor<PageRequest> pageRequestCaptor;

    @Test
    void run_shouldLoadFirst200Cards() {
        CacheWarmupRunner runner = new CacheWarmupRunner(cardService);

        runner.run(new DefaultApplicationArguments());

        verify(cardService).listCards(filterCaptor.capture(), pageRequestCaptor.capture());
        assertEquals(0, pageRequestCaptor.getValue().getPageNumber());
        assertEquals(200, pageRequestCaptor.getValue().getPageSize());
    }
}
