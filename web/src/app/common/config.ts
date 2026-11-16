/** localhost ou IP de rede local (192.168.x.x, 10.x.x.x, 172.16-31.x.x). */
const REDE_LOCAL = /^(localhost|127\.0\.0\.1|192\.168\.\d+\.\d+|10\.\d+\.\d+\.\d+|172\.(1[6-9]|2\d|3[01])\.\d+\.\d+)$/;

/**
 * Endereço da API do Banco Hidra (Quarkus, porta 8080).
 * - Na mesma rede: usa o computador de onde o site foi aberto. Quem entra por http://localhost:4300 fala com
 *   localhost:8080; um colega que entra por http://192.168.x.x:4300 fala com 192.168.x.x:8080 ("modo turma").
 * - Pelo túnel (https://xxxx.trycloudflare.com): o túnel só leva a porta do site, então a API vai por /api,
 *   que o servidor do site repassa para a porta 8080 (proxy.conf.json).
 */
export const API = REDE_LOCAL.test(location.hostname)
  ? `${location.protocol}//${location.hostname}:8080`
  : `${location.origin}/api`;

/** Porta do site (ng serve). */
export const PORTA_SITE = 4300;

/** Autor do projeto: aparece como marca d'água no rodapé de todas as telas. */
export const AUTOR = 'joaoklebermoraes123@gmail.com';
export const VERSAO = '0.5.0';
