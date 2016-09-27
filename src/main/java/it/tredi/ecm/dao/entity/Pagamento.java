package it.tredi.ecm.dao.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Pagamento extends BaseEntity{
	/*
	 * Creazione record di pagamento per ogni provider all'inizio dell'anno (dataPagamento = null)
	 *
	 * Esiste l'apposito metodo PagamentoRepository.findAllProviderNotPagamentoEffettuato(annoPagamento)
	 * per individuare lo stato del pagamento per i provider per l'anno in corso
	 *
	 * */
	@Column(name = "data_pagamento")
	private LocalDate dataPagamento;
	@Column(name = "data_scadenza_pagamento")
	private LocalDate dataScadenzaPagamento;
	private Double importo;
	@Column(name = "anno_pagamento")
	private Integer annoPagamento;

	@ManyToOne
	private Provider provider;

	@JoinColumn(name = "fk_evento")
	@OneToOne(fetch = FetchType.LAZY)
	private Evento evento;
	private String anagrafica;
	private String indirizzo;
	private String civico;
	private String localita;
	private String provincia;
	private String nazione;
	private String cap;
	private String codiceFiscale;
	private String partitaIva;
	private String email;
	private String tipoVersamento;
	private String causale;
	private String datiSpecificiRiscossione;
	private String identificativoUnivocoDovuto;
	private Double commissioneCaricoPa;
	private String idSession;
	private Date dataInvio;
	private String codiceEsito;
	private Date dataEsito;
	private Double importoTotalePagato;
	private String esitoSingoloPagamento;
	private Date dataEsitoSingoloPagamento;
	private String identificativoUnivocoRiscosse;


}
