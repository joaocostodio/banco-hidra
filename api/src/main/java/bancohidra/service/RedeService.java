package bancohidra.service;

import jakarta.enterprise.context.ApplicationScoped;

import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Descobre o IP deste PC na rede local e o link do túnel (se aberto), para o "modo turma".
 * Só lê as interfaces de rede e o cloudflared do próprio computador: nada é enviado para fora.
 */
@ApplicationScoped
public class RedeService {

    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(300)).build();
    private static final Pattern HOSTNAME_TUNEL =
            Pattern.compile("\"hostname\"\\s*:\\s*\"([a-z0-9-]+\\.trycloudflare\\.com)\"");

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
     * Link do túnel da Cloudflare, se houver um aberto neste PC ({@code cloudflared tunnel --url ...}).
     * O cloudflared responde o endereço em http://127.0.0.1:2024x/quicktunnel (servidor de métricas local).
     *
     * @return ex.: "https://dvds-trip-throwing-clear.trycloudflare.com"
     */
    public Optional<String> tunel() {
        for (int porta = 20241; porta <= 20245; porta++) {
            try {
                HttpResponse<String> resposta = HTTP.send(
                        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + porta + "/quicktunnel"))
                                .timeout(Duration.ofMillis(500)).build(),
                        HttpResponse.BodyHandlers.ofString());
                Matcher m = HOSTNAME_TUNEL.matcher(resposta.body());
                if (resposta.statusCode() == 200 && m.find()) {
                    return Optional.of("https://" + m.group(1));
                }
            } catch (Exception semTunelNestaPorta) {
                // nada escutando nesta porta: tenta a próxima
            }
        }
        return Optional.empty();
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
