package dev.tutz.pauta.client;

import java.util.Collection;
import java.util.Map;

/**
 * O que o cadastro de clientes precisa saber da agenda. Fica aqui e é implementado em
 * {@code appointment}, para a dependência entre os dois pacotes andar num sentido só.
 */
public interface ClientSchedule {

    /** Compromissos em aberto de hoje em diante, por cliente. Cliente sem nenhum fica fora do mapa. */
    Map<Long, Long> upcomingCountByClient(Collection<Long> clientIds);

    boolean hasAppointments(Long clientId);
}
