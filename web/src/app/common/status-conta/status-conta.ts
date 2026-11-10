import { Component, computed, input } from '@angular/core';

/** Chip com a situação da conta (ativa, bloqueada, encerrada). */
@Component({
  selector: 'app-status-conta',
  templateUrl: './status-conta.html',
})
export class StatusConta {
  readonly status = input.required<'ATIVA' | 'BLOQUEADA' | 'ENCERRADA'>();

  protected readonly classe = computed(() => 'chip ' + this.status().toLowerCase());
  protected readonly texto = computed(
    () => ({ ATIVA: 'Ativa', BLOQUEADA: 'Bloqueada', ENCERRADA: 'Encerrada' })[this.status()],
  );
}
