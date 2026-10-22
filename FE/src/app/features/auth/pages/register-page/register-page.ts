import { HttpErrorResponse } from '@angular/common/http';
import { afterNextRender, ChangeDetectionStrategy, Component, ElementRef, inject, OnDestroy, signal } from '@angular/core';
import { Subject, takeUntil } from 'rxjs';
import { AbstractControl, FormBuilder, FormGroup, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { GENDER, Gender } from '../../../../shared/models/user.model';
import { ButtonComponent } from '../../../../shared/ui/button/button';

function passwordMatchValidator(group: AbstractControl): ValidationErrors | null {
  const password = group.get('password')?.value;
  const confirmPassword = group.get('confirmPassword')?.value;
  return password && confirmPassword && password !== confirmPassword
    ? { passwordMismatch: true }
    : null;
}

@Component({
  selector: 'app-register-page',
  imports: [ReactiveFormsModule, RouterLink, ButtonComponent],
  templateUrl: './register-page.html',
  styleUrl: './register-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RegisterPage implements OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly el = inject(ElementRef);

  constructor() {
    afterNextRender(() => {
      (this.el.nativeElement.querySelector('#register-username') as HTMLInputElement | null)?.focus();
    });
  }

  readonly genderOptions: { value: Gender | ''; label: string }[] = [
    { value: '', label: 'Prefiero no decir' },
    { value: GENDER.MALE, label: 'Masculino' },
    { value: GENDER.FEMALE, label: 'Femenino' },
    { value: GENDER.OTHER, label: 'Otro' },
  ];

  readonly registerForm: FormGroup = this.fb.group(
    {
      username: ['', [Validators.required, Validators.minLength(3)]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', Validators.required],
      gender: [''],
    },
    { validators: passwordMatchValidator },
  );

  private readonly destroy$ = new Subject<void>();

  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onSubmit(): void {
    this.registerForm.markAllAsTouched();
    if (this.registerForm.invalid) return;

    const { username, password, gender } = this.registerForm.value;

    this.loading.set(true);
    this.errorMessage.set(null);

    this.auth
      .register({ username, password, gender: gender || null })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.loading.set(false);
          this.router.navigateByUrl('/pokedex');
        },
        error: (err: HttpErrorResponse) => {
          this.loading.set(false);
          this.errorMessage.set(err.error?.message || 'Error al crear la cuenta. Intentá de nuevo.');
        },
      });
  }
}
