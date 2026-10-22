package ar.edu.utn.frc.tup.piii.engine.combat.steps;

import ar.edu.utn.frc.tup.piii.engine.models.AttackContext;

public interface AttackStep {

    void execute(AttackContext context);

    void setNext(AttackStep next);
}
