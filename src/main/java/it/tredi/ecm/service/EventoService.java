package it.tredi.ecm.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;

import it.tredi.ecm.dao.entity.Account;
import it.tredi.ecm.dao.entity.Accreditamento;
import it.tredi.ecm.dao.entity.Evento;
import it.tredi.ecm.dao.entity.EventoFSC;
import it.tredi.ecm.dao.entity.File;
import it.tredi.ecm.dao.entity.Pagamento;
import it.tredi.ecm.dao.entity.Sponsor;
import it.tredi.ecm.dao.enumlist.EventoStatoEnum;
import it.tredi.ecm.dao.enumlist.EventoVersioneEnum;
import it.tredi.ecm.dao.enumlist.RuoloFSCEnum;
import it.tredi.ecm.exception.AccreditamentoNotFoundException;
import it.tredi.ecm.web.bean.EventoWrapper;
import it.tredi.ecm.web.bean.ModificaOrarioAttivitaWrapper;
import it.tredi.ecm.web.bean.RicercaEventoWrapper;
import it.tredi.ecm.web.bean.ScadenzeEventoWrapper;

public interface EventoService {
	public Evento getEvento(Long id);
	public void save(Evento evento) throws Exception;
	public void delete(Long id) throws Exception;

	public void validaRendiconto(Long id, File rendiconto) throws Exception;
	public List<Evento> getAllEventi();
	public Page<Evento> getAllEventi(Integer pageNumber, Integer columnNumber, String order, Integer numOfPages);
	public Set<Evento> getAllEventiForProviderId(Long providerId);
	public Page<Evento> getAllEventiForProviderId(Long providerId, Integer pageNumber, Integer columnNumber, String order, Integer numOfPages);
	public boolean canCreateEvento(Account account);
	public boolean canRieditEvento(Account account);
	public void inviaRendicontoACogeaps(Long id) throws Exception;
	public void statoElaborazioneCogeaps(Long id) throws Exception;
	public Evento handleRipetibiliAndAllegati(EventoWrapper eventoWrapper) throws Exception;
	public EventoWrapper prepareRipetibiliAndAllegati(EventoWrapper eventoWrapper);


	public void calculateAutoCompilingData(EventoWrapper eventoWrapper) throws Exception;
//	public float calcoloDurataEvento(EventoWrapper eventoWrapper);
//	public float calcoloCreditiEvento(EventoWrapper eventoWrapper);

	//TODO Questo metodo puo' diventare private
	public void retrieveProgrammaAndAddJoin(EventoWrapper eventoWrapper);
	public void aggiornaDati(EventoWrapper eventoWrapper);
	public Set<Evento> getAllEventiRieditabiliForProviderId(Long providerId) throws AccreditamentoNotFoundException;

	//TODO chiedere 1 mese di ferie almeno (joe19 mode on)
	public Evento prepareRiedizioneEvento(Evento evento) throws Exception;
	public int getLastEdizioneEventoByPrefix(String prefix);
	public Evento getEventoForRiedizione(Long eventoId);
	public Set<Evento> getEventiByProviderIdAndAnnoRiferimento(Long providerId, Integer annoRiferimento);
	public Set<Evento> getEventiRendicontatiByProviderIdAndAnnoRiferimento(Long providerId, Integer annoRiferimento, boolean withRiedizioni);
	public Set<Evento> getEventiForRelazioneAnnualeByProviderIdAndAnnoRiferimento(Long providerId, Integer annoRiferimento);

	// detacha e clona l'Evento Padre da rieditare
//	public <T> void detachEvento(T obj) throws Exception;
	public Evento detachEvento(Evento evento) throws Exception;

	public Set<Evento> getEventiForProviderIdInScadenzaDiPagamento(Long providerId);
	public int countEventiForProviderIdInScadenzaDiPagamento(Long providerId);
	public Set<Evento> getEventiForProviderIdInScadenzaDiRendicontazione(Long providerId);
	public int countEventiForProviderIdInScadenzaDiRendicontazione(Long providerId);
	public Set<Evento> getEventiForProviderIdScadutiENonPagati(Long providerId);
	public int countEventiForProviderIdScadutiENonPagati(Long providerId);
	public Set<Evento> getEventiForProviderIdScadutiENonRendicontati(Long providerId);
	public int countEventiForProviderIdScadutiENonRendicontati(Long providerId);

	public List<Evento> cerca(RicercaEventoWrapper wrapper);

	public boolean isEditSemiBloccato(Evento evento);
	public boolean isEventoIniziato(Evento evento);
//	public boolean hasDataInizioRestrictions(Evento evento);
	public Sponsor getSponsorById(Long sponsorId);
	public void saveAndCheckContrattoSponsorEvento(File sponsorFile, Sponsor sponsor, Long eventoId, String mode) throws Exception;
	public Set<Evento> getEventiByProviderIdAndStato(Long id, EventoStatoEnum stato);
	public Integer countAllEventiByProviderIdAndStato(Long id, EventoStatoEnum stato);

	public Set<Evento> getEventiCreditiNonConfermati();
	public Integer countAllEventiCreditiNonConfermati();
	public void updateScadenze(Long eventoId, ScadenzeEventoWrapper wrapper) throws Exception;
	public Evento getEventoByPrefix(String idEventoLink);
	public Evento getEventoByPrefixAndEdizione(String prefix, int edizione);
	public Evento getEventoByCodiceIdentificativo(String codiceIdentificativo);
	public boolean existsByPrefixAndStatoNot(String idEventoLink, EventoStatoEnum stato);

	public Integer countAllEventiAlimentazionePrimaInfanzia();
	public Set<Evento> getEventiAlimentazionePrimaInfanzia();
	public Integer countAllEventiMedicineNonConvenzionali();
	public Set<Evento> getEventiMedicineNonConvenzionali();
//	public boolean checkIfRESAndWorkshopOrCorsoAggiornamentoAndInterettivoSelected(Evento evento);
	public boolean checkIfFSCAndTrainingAndTutorPartecipanteRatioAlert(Evento evento);

	//MEV riedizioni
	public boolean updateOrariAttivita(ModificaOrarioAttivitaWrapper jsonObj, EventoWrapper eventoWrapper);
	public boolean existRiedizioniOfEventoId(Long eventoId);
	public Set<Evento> getRiedizioniOfEventoId(Long eventoId);

	public void salvaQuietanzaPagamento(File quietanzaPagamento, Long eventoId) throws Exception;
	public Pagamento getPagamentoForQuietanza(Evento evento) throws Exception;
	public Long getFileQuietanzaId(Long eventoId);

	public List<RuoloFSCEnum> getListRuoloFSCEnumPerResponsabiliScientifici(EventoFSC evento);
	public List<RuoloFSCEnum> getListRuoloFSCEnumPerEsperti(EventoFSC evento);
	public List<RuoloFSCEnum> getListRuoloFSCEnumPerCoordinatori(EventoFSC evento);

	public void archiveEventoInPrimaInfanziaOrMedNonConv(List<Long> ids);

	// ERM014776
	public void eliminaEventiPerChiusuraAccreditamento(Accreditamento acc, LocalDate dataCut) throws Exception;

	public Integer countAllEventiCondivisioneEsitiValutazione();
	public Set<Evento> getEventiCondivisioneEsitiValutazione();

	public void marcaNoEcm(Long eventoId) throws Exception;
}