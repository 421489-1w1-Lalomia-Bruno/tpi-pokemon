import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LobbyPage } from './lobby-page';

describe('LobbyPage', () => {
  let component: LobbyPage;
  let fixture: ComponentFixture<LobbyPage>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [LobbyPage]
    });

    fixture = TestBed.createComponent(LobbyPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have showCreateDialog signal initialized to false', () => {
    expect(component.showCreateDialog()).toBeFalse();
  });

  it('should open dialog when openCreateDialog is called', () => {
    component.openCreateDialog();
    expect(component.showCreateDialog()).toBeTrue();
  });

  it('should close dialog and refresh match list when closeCreateDialog is called', () => {
    const mockMatchList = jasmine.createSpyObj('MatchListComponent', ['refresh']);
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (component as any).matchList = () => mockMatchList;

    component.showCreateDialog.set(true);
    component.closeCreateDialog();

    expect(component.showCreateDialog()).toBeFalse();
    expect(mockMatchList.refresh).toHaveBeenCalled();
  });

  it('should render header with title and create button', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.lobby-h1')?.textContent).toContain('Sala de Espera');
    expect(compiled.querySelector('.create-button')).toBeTruthy();
  });

  it('should render MatchListComponent', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('app-match-list')).toBeTruthy();
  });
});
