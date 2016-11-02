package it.tredi.ecm.service;

import java.util.Set;

import it.tredi.ecm.dao.entity.Evento;
import it.tredi.ecm.dao.entity.Pagamento;
import it.tredi.ecm.dao.entity.Provider;
import it.tredi.ecm.utils.Utils;

public interface PagamentoService {
	public boolean providerIsPagamentoEffettuato(Long providerId, Integer annoRiferimento);
	public Set<Provider> getAllProviderNotPagamentoEffettuato(Integer annoRiferimento);
	
	/* Pagamenti Quote Provider */
	public Pagamento getPagamentoByProviderIdAndAnnoRiferimento(Long providerId, Integer annoRiferimento);
	public Set<Pagamento> getPagamentiProviderDaVerificare();
	public Set<Pagamento> getAllPagamentiByProviderId(Long providerId);
	
	/* Pagamenti Eventi */
	public Pagamento getPagamentoByEvento(Evento evento);
	public Set<Pagamento> getPagamentiEventiDaVerificare();
	
	public void save(Pagamento p);
	public void deleteAll(Iterable<Pagamento> pagamenti);
	public Set<Pagamento> getAllPagamenti();
	
	public Pagamento preparePagamentoProviderPerQuotaAnnua(Long providerId, Integer annoRiferimento, boolean primoAnno);
	
}
