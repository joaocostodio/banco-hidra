/**
 * Endereço da API do Banco Hidra. É sempre o mesmo endereço do site + /api: o servidor do site (ng serve)
 * repassa /api para a API na porta 8080 (proxy.conf.json). Assim o cookie de sessão (httpOnly, SameSite=Strict)
 * é do próprio site e funciona igual pelo localhost, pelo IP da rede ou pelo túnel.
 */
export const API = `${location.origin}/api`;

/** Porta do site (ng serve). */
export const PORTA_SITE = 4300;

/** Autor do projeto: aparece como marca d'água no rodapé de todas as telas. */
export const AUTOR = 'joaoklebermoraes123@gmail.com';
export const VERSAO = '0.5.0';
