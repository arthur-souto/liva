package sus.liva.application.port.in;

import java.time.LocalDate;

public record SemanaDeComparecimento(LocalDate inicio, long compareceram, long faltaram, long cancelaramAntes, long reaproveitadas) {}
