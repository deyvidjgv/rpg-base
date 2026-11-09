package com.rpg.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Una victoria de un jugador: contra que monstruo y cuando. */
public class Victoria {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String monstruo;
    private final LocalDateTime fecha;

    public Victoria(String monstruo, LocalDateTime fecha) {
        this.monstruo = monstruo;
        this.fecha = fecha;
    }

    public String getMonstruo() {
        return monstruo;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    @Override
    public String toString() {
        return String.format("%-15s %s", monstruo, fecha.format(FORMATO));
    }
}
