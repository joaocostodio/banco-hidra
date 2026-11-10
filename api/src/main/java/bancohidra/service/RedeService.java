package bancohidra.service;

import jakarta.enterprise.context.ApplicationScoped;

import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Descobre o IP deste PC na rede local, para o "modo turma" (colegas acessando pelo IP).
 * Só lê as interfaces de rede do próprio computador: nada é enviado para fora.
 */
@ApplicationScoped
public class RedeService {

    /** IPs de rede local deste PC; o primeiro é o mais provável de ser o da rede de verdade. */
    public List<String> enderecosLocais() {
        Set<String> ips = new LinkedHashSet<>();
        ipDaRotaPadrao().ifPresent(ips::add);
        try {
            for (NetworkInterface rede : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!rede.isUp() || rede.isLoopback() || rede.isVirtual()) {
                    continue;
                }
                for (InetAddress ip : Collections.list(rede.getInetAddresses())) {
                    if (ip instanceof Inet4Address && ip.isSiteLocalAddress()) {
                        ips.add(ip.getHostAddress());
                    }
                }
            }
        } catch (SocketException e) {
            // sem acesso às interfaces: fica só com o que já achou
        }
        return new ArrayList<>(ips);
    }

    /**
     * O IP que o sistema usaria para sair para a internet (pula adaptadores virtuais do WSL, VirtualBox, VPN...).
     * "connect" em UDP só escolhe a rota: nenhum pacote é enviado.
     */
    private static Optional<String> ipDaRotaPadrao() {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 53);
            InetAddress local = socket.getLocalAddress();
            if (local instanceof Inet4Address && local.isSiteLocalAddress()) {
                return Optional.of(local.getHostAddress());
            }
        } catch (Exception e) {
            // sem rede: tudo bem
        }
        return Optional.empty();
    }
}
