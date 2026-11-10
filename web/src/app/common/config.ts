/**
 * Endereço da API do Banco Hidra (Quarkus, porta 8080).
 * Usa o mesmo computador de onde o site foi aberto: quem entra por http://localhost:4300 fala com
 * localhost:8080; um colega que entra por http://192.168.x.x:4300 fala com 192.168.x.x:8080 ("modo turma").
 */
export const API = `${location.protocol}//${location.hostname}:8080`;

/** Porta do site (ng serve). */
export const PORTA_SITE = 4300;

/** Autor do projeto: aparece como marca d'água no rodapé de todas as telas. */
export const AUTOR = 'joaoklebermoraes123@gmail.com';
export const VERSAO = '0.5.0';
