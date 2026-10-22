import { ChangeDetectionStrategy, Component, signal, viewChild } from '@angular/core';
import { MatchListComponent } from '../../components/match-list/match-list';
import { CreateMatchDialogComponent } from '../../components/create-match-dialog/create-match-dialog';
import { ButtonComponent } from '../../../../shared/ui/button/button';

@Component({
  selector: 'app-lobby-page',
  imports: [MatchListComponent, CreateMatchDialogComponent, ButtonComponent],
  templateUrl: './lobby-page.html',
  styleUrl: './lobby-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class LobbyPage {
  readonly showCreateDialog = signal(false);
  readonly matchList = viewChild(MatchListComponent);

  openCreateDialog(): void {
    this.showCreateDialog.set(true);
  }

  closeCreateDialog(): void {
    this.showCreateDialog.set(false);
    this.matchList()?.refresh();
  }
}
