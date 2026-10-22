import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { MatchListComponent } from './match-list';
import { LobbyService } from '../../services/lobby.service';
import { MatchModel } from '../../../../shared/models/match.model';

describe('MatchListComponent', () => {
  let component: MatchListComponent;
  let fixture: ComponentFixture<MatchListComponent>;
  let lobbyServiceSpy: jasmine.SpyObj<LobbyService>;
  let routerSpy: jasmine.SpyObj<Router>;

  const mockMatches: MatchModel[] = [
    {
      id: '12345678-1234-1234-1234-123456789012',
      player1Id: 'player-1',
      player2Id: null,
      status: 'WAITING',
      winnerId: null,
      createdAt: new Date().toISOString()
    },
    {
      id: '87654321-4321-4321-4321-210987654321',
      player1Id: 'player-2',
      player2Id: null,
      status: 'WAITING',
      winnerId: null,
      createdAt: new Date().toISOString()
    }
  ];

  beforeEach(() => {
    const lobbySpy = jasmine.createSpyObj('LobbyService', ['getMatches', 'joinMatch']);
    lobbySpy.availableMatches = jasmine.createSpy().and.returnValue(mockMatches);
    lobbySpy.loading = jasmine.createSpy().and.returnValue(false);

    const routerSpyObj = jasmine.createSpyObj('Router', ['navigate']);

    TestBed.configureTestingModule({
      imports: [MatchListComponent],
      providers: [
        { provide: LobbyService, useValue: lobbySpy },
        { provide: Router, useValue: routerSpyObj }
      ]
    });

    fixture = TestBed.createComponent(MatchListComponent);
    component = fixture.componentInstance;
    lobbyServiceSpy = TestBed.inject(LobbyService) as jasmine.SpyObj<LobbyService>;
    routerSpy = TestBed.inject(Router) as jasmine.SpyObj<Router>;
  });

  afterEach(() => {
    component.ngOnDestroy();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should call getMatches on init', () => {
    component.ngOnInit();
    expect(lobbyServiceSpy.getMatches).toHaveBeenCalled();
  });

  it('should render match list when matches are available', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    const matchCards = compiled.querySelectorAll('.match-card');
    expect(matchCards.length).toBe(2);
  });

  it('should show error message when deckId is empty on join', () => {
    spyOn(localStorage, 'getItem').and.callFake((key: string) => {
      if (key === 'playerId') return 'test-player';
      if (key === 'deck-for-test-player') return '';
      return null;
    });

    component.onJoin('match-123');

    expect(component.errorMessage()).toBe('No tenés un mazo seleccionado. Creá uno en el constructor de mazos.');
    expect(lobbyServiceSpy.joinMatch).not.toHaveBeenCalled();
  });

  it('should navigate to game page on successful join', fakeAsync(() => {
    spyOn(localStorage, 'getItem').and.callFake((key: string) => {
      if (key === 'playerId') return 'test-player';
      if (key === 'deck-for-test-player') return 'deck-123';
      return null;
    });

    const mockMatch = { id: 'match-456' } as MatchModel;
    lobbyServiceSpy.joinMatch.and.returnValue(of(mockMatch));

    component.onJoin('match-456');
    tick();

    expect(routerSpy.navigate).toHaveBeenCalledWith(['/game', 'match-456']);
  }));

  it('should show error message when join fails', fakeAsync(() => {
    spyOn(localStorage, 'getItem').and.callFake((key: string) => {
      if (key === 'playerId') return 'test-player';
      if (key === 'deck-for-test-player') return 'deck-123';
      return null;
    });

    lobbyServiceSpy.joinMatch.and.returnValue(throwError(() => new Error('Match full')));

    component.onJoin('match-789');
    tick();

    expect(component.errorMessage()).toBe('No se pudo unir a la partida. Puede que ya esté llena.');
    expect(component.joining()).toBeFalse();
  }));

  it('should clear error message when refresh is called', () => {
    component.errorMessage.set('Previous error');
    component.refresh();
    expect(lobbyServiceSpy.getMatches).toHaveBeenCalled();
  });

  it('should set up polling interval on init', fakeAsync(() => {
    component.ngOnInit();
    expect(lobbyServiceSpy.getMatches).toHaveBeenCalledTimes(1);

    tick(15000);
    expect(lobbyServiceSpy.getMatches).toHaveBeenCalledTimes(2);

    tick(15000);
    expect(lobbyServiceSpy.getMatches).toHaveBeenCalledTimes(3);
  }));

  it('should clear polling interval on destroy', fakeAsync(() => {
    component.ngOnInit();
    expect(lobbyServiceSpy.getMatches).toHaveBeenCalledTimes(1);

    component.ngOnDestroy();
    tick(15000);

    expect(lobbyServiceSpy.getMatches).toHaveBeenCalledTimes(1);
  }));
});
