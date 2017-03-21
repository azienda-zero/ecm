package it.tredi.ecm.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import it.tredi.bonita.api.model.TaskInstanceDataModel;
import it.tredi.ecm.audit.entity.AccreditamentoAudit;
import it.tredi.ecm.dao.entity.Account;
import it.tredi.ecm.dao.entity.Accreditamento;
import it.tredi.ecm.dao.entity.AccreditamentoDiff;
import it.tredi.ecm.dao.entity.DatiAccreditamento;
import it.tredi.ecm.dao.entity.DatiAccreditamentoDiff;
import it.tredi.ecm.dao.entity.FieldEditabileAccreditamento;
import it.tredi.ecm.dao.entity.FieldIntegrazioneAccreditamento;
import it.tredi.ecm.dao.entity.FieldValutazioneAccreditamento;
import it.tredi.ecm.dao.entity.File;
import it.tredi.ecm.dao.entity.Persona;
import it.tredi.ecm.dao.entity.PersonaDiff;
import it.tredi.ecm.dao.entity.PianoFormativo;
import it.tredi.ecm.dao.entity.Provider;
import it.tredi.ecm.dao.entity.ProviderDiff;
import it.tredi.ecm.dao.entity.Sede;
import it.tredi.ecm.dao.entity.SedeDiff;
import it.tredi.ecm.dao.entity.Seduta;
import it.tredi.ecm.dao.entity.Valutazione;
import it.tredi.ecm.dao.entity.ValutazioneCommissione;
import it.tredi.ecm.dao.entity.VerbaleValutazioneSulCampo;
import it.tredi.ecm.dao.entity.WorkflowInfo;
import it.tredi.ecm.dao.enumlist.AccreditamentoStatoEnum;
import it.tredi.ecm.dao.enumlist.AccreditamentoTipoEnum;
import it.tredi.ecm.dao.enumlist.IdFieldEnum;
import it.tredi.ecm.dao.enumlist.ProviderStatoEnum;
import it.tredi.ecm.dao.enumlist.Ruolo;
import it.tredi.ecm.dao.enumlist.StatoWorkflowEnum;
import it.tredi.ecm.dao.enumlist.SubSetFieldEnum;
import it.tredi.ecm.dao.enumlist.TipoIntegrazioneEnum;
import it.tredi.ecm.dao.enumlist.TipoWorkflowEnum;
import it.tredi.ecm.dao.enumlist.ValutazioneTipoEnum;
import it.tredi.ecm.dao.repository.AccountRepository;
import it.tredi.ecm.dao.repository.AccreditamentoDiffRepository;
import it.tredi.ecm.dao.repository.AccreditamentoRepository;
import it.tredi.ecm.dao.repository.DatiAccreditamentoDiffRepository;
import it.tredi.ecm.dao.repository.PersonaDiffRepository;
import it.tredi.ecm.dao.repository.ProviderDiffRepository;
import it.tredi.ecm.dao.repository.SedeDiffRepository;
import it.tredi.ecm.dao.repository.ValutazioneCommissioneRepository;
import it.tredi.ecm.exception.AccreditamentoNotFoundException;
import it.tredi.ecm.pdf.PdfAccreditamentoProvvisorioAccreditatoInfo;
import it.tredi.ecm.pdf.PdfAccreditamentoProvvisorioIntegrazionePreavvisoRigettoInfo;
import it.tredi.ecm.pdf.PdfAccreditamentoProvvisorioRigettoInfo;
import it.tredi.ecm.service.bean.CurrentUser;
import it.tredi.ecm.service.bean.EcmProperties;
import it.tredi.ecm.utils.Utils;

@Service
public class AccreditamentoServiceImpl implements AccreditamentoService {
	private static Logger LOGGER = LoggerFactory.getLogger(AccreditamentoServiceImpl.class);
	private static long millisecondiInGiorno = 86400000;
	private static long millisecondiInMinuto = 60000;

	@Autowired private AccreditamentoRepository accreditamentoRepository;

	@Autowired private ProviderService providerService;
	@Autowired private PianoFormativoService pianoFormativoService;
	@Autowired private AccountRepository accountRepository;
	@Autowired private EmailService emailService;
	@Autowired private AccountService accountService;
//	@Autowired private PagamentoService pagamentoService;
	@Autowired private QuotaAnnualeService quotaAnnualeService;
	@Autowired private ProtocolloService protocolloService;

	@Autowired private ValutazioneService valutazioneService;
	@Autowired private FieldEditabileAccreditamentoService fieldEditabileService;
	@Autowired private FieldValutazioneAccreditamentoService fieldValutazioneAccreditamentoService;
	@Autowired private FieldIntegrazioneAccreditamentoService fieldIntegrazioneAccreditamentoService;

	@Autowired private IntegrazioneService integrazioneService;
	@Autowired private WorkflowService workflowService;

	@Autowired private FileService fileService;
	@Autowired private PdfService pdfService;

	@Autowired private MessageSource messageSource;

	@Autowired private EcmProperties ecmProperties;

	@Autowired private AlertEmailService alertEmailService;
	@Autowired private DatiAccreditamentoService datiAccreditamentoService;
	@Autowired private AccreditamentoStatoHistoryService accreditamentoStatoHistoryService;

	@Autowired private ValutazioneCommissioneRepository valutazioneCommissioneRepository;
	@Autowired private SedutaService sedutaService;

	@Autowired private PersonaService personaService;
	@Autowired private SedeService sedeService;
	@Autowired private AuditService auditService;

	@Autowired private DiffService diffService;

	@Override
	public Accreditamento getNewAccreditamentoForCurrentProvider(AccreditamentoTipoEnum tipoDomanda) throws Exception{
		LOGGER.debug(Utils.getLogMessage("Creazione domanda di accreditamento per il provider corrente"));
		Provider currentProvider = providerService.getProvider();
		return getNewAccreditamento(currentProvider,tipoDomanda);
	}

	@Override
	public Accreditamento getNewAccreditamentoForProvider(Long providerId, AccreditamentoTipoEnum tipoDomanda) throws Exception {
		LOGGER.debug(Utils.getLogMessage("Creazione domanda di accreditamento " + tipoDomanda + " per il provider: " + providerId));
		Provider provider = providerService.getProvider(providerId);
		return getNewAccreditamento(provider,tipoDomanda);
	}

	@Transactional
	private Accreditamento getNewAccreditamento(Provider provider, AccreditamentoTipoEnum tipoDomanda) throws Exception{
		if(provider == null){
			throw new Exception("Provider non può essere NULL");
		}

		if(provider.isNew()){
			throw new Exception("Provider non registrato");
		}else{

			Set<Accreditamento> accreditamentiAttivi = getAccreditamentiAvviatiForProvider(provider.getId(), tipoDomanda);

			if(canProviderCreateAccreditamento(provider.getId(), tipoDomanda)){
				Accreditamento accreditamento = new Accreditamento(tipoDomanda);
				accreditamento.setProvider(provider);
				accreditamento.enableAllIdField();
				save(accreditamento);

				//se è la seconda volta che inserisco un provvisorio risetto i dati a default
				if(tipoDomanda == AccreditamentoTipoEnum.PROVVISORIO) {
					provider.setCanInsertAccreditamentoProvvisorio(false);
					provider.setDataRinnovoInsertAccreditamentoProvvisorio(null);
				}

				if(tipoDomanda == AccreditamentoTipoEnum.STANDARD){
					try{
						Accreditamento ultimoAccreditamento = getAccreditamentoAttivoForProvider(provider.getId());

						diffService.creaAllDiffAccreditamento(ultimoAccreditamento);

						if(ultimoAccreditamento != null && ultimoAccreditamento.getDatiAccreditamento() != null){
							DatiAccreditamento ultimoDatiAccreditamento = ultimoAccreditamento.getDatiAccreditamento();
							if(ultimoDatiAccreditamento.isDatiStrutturaInseriti() || ultimoDatiAccreditamento.isTipologiaFormativaInserita()){
								DatiAccreditamento datiAccreditamento = new DatiAccreditamento();

								datiAccreditamento.setTipologiaAccreditamento(String.copyValueOf(ultimoDatiAccreditamento.getTipologiaAccreditamento().toCharArray()));
								datiAccreditamento.getProcedureFormative().addAll(ultimoDatiAccreditamento.getProcedureFormative());
								datiAccreditamento.setProfessioniAccreditamento(String.copyValueOf(ultimoDatiAccreditamento.getProfessioniAccreditamento().toCharArray()));
								datiAccreditamento.getDiscipline().addAll(ultimoDatiAccreditamento.getDiscipline());

								datiAccreditamento.setNumeroDipendentiFormazioneTempoIndeterminato(ultimoDatiAccreditamento.getNumeroDipendentiFormazioneTempoIndeterminato());
								datiAccreditamento.setNumeroDipendentiFormazioneAltro(ultimoDatiAccreditamento.getNumeroDipendentiFormazioneAltro());

								File organigramma = null;
								File funzionigramma = null;
								for(File f : ultimoDatiAccreditamento.getFiles()){
									if(f.isORGANIGRAMMA())
										organigramma = (File) f.clone();
									else if(f.isFUNZIONIGRAMMA()){
										funzionigramma = (File) f.clone();
									}
								}

								if(organigramma != null){
									fileService.save(organigramma);
									datiAccreditamento.addFile(organigramma);
								}

								if(funzionigramma != null){
									fileService.save(funzionigramma);
									datiAccreditamento.addFile(funzionigramma);
								}

								datiAccreditamentoService.save(datiAccreditamento, accreditamento.getId());
							}
						}
					}catch(Exception ex){
						ex.printStackTrace();
					}
				}

				return accreditamento;
			}else{
				throw new Exception("Il provider " + provider.getId() + " non può presentare una domanda di accreditamento " + tipoDomanda);
			}
		}
	}

	@Override
	public Accreditamento getAccreditamento(Long accreditamentoId) {
		LOGGER.debug(Utils.getLogMessage("Caricamento domanda di accreditamento: " + accreditamentoId));
		return accreditamentoRepository.findOne(accreditamentoId);
	};

	@Override
	public Set<Accreditamento> getAllAccreditamentiForProvider(Long providerId) {
		LOGGER.debug(Utils.getLogMessage("Recupero tutte le domande di accreditamento per il provider " + providerId));
		Set<Accreditamento> accreditamenti = accreditamentoRepository.findByProviderId(providerId);
		if(accreditamenti != null)
			LOGGER.debug("Trovati " + accreditamenti.size() + " accreditamenti");
		return accreditamenti;
	}

	@Override
	public Set<Accreditamento> getAllAccreditamentiForProvider(Long providerId,AccreditamentoTipoEnum tipoDomnda) {
		LOGGER.debug(Utils.getLogMessage("Recupero tutte le domande di accreditamento " + tipoDomnda + " per il provider " + providerId));
		Set<Accreditamento> accreditamenti = accreditamentoRepository.findAllByProviderIdAndTipoDomanda(providerId,tipoDomnda);
		if(accreditamenti != null)
			LOGGER.debug("Trovati " + accreditamenti.size() + " accreditamenti");
		return accreditamenti;
	}

	/**
	 * Restituisce tutte le domande di accreditamento che hanno una data di scadenza "attiva"
	 * */
	@Override
	public Set<Accreditamento> getAccreditamentiAvviatiForProvider(Long providerId, AccreditamentoTipoEnum tipoDomanda) {
		LOGGER.debug(Utils.getLogMessage("Recupero domande di accreditamento avviate per il provider " + providerId));
		LOGGER.debug(Utils.getLogMessage("Ricerca domande di accreditamento di tipo: " + tipoDomanda.name() + "con data di scadenza posteriore a: " + LocalDate.now()));
		return accreditamentoRepository.findAllByProviderIdAndTipoDomandaAndDataScadenzaAfter(providerId, tipoDomanda, LocalDate.now());
	}

	/**
	 * Restituisce l'unica domanda di accreditamento che ha una data di fine accreditamento "attiva" e che è in stato "ACCREDITATO"
	 * se ne trova più di una prendo quella che scadrà per primo
	 * */
	@Override
	public Accreditamento getAccreditamentoAttivoForProvider(Long providerId) throws AccreditamentoNotFoundException{
		LOGGER.debug(Utils.getLogMessage("Recupero eventuale accreditamento attivo per il provider: " + providerId));

		Accreditamento accreditamento = null;
		Set<Accreditamento> listaAccreditamentiAttivi = accreditamentoRepository.findAllByProviderIdAndStatoAndDataFineAccreditamentoAfterOrderByDataFineAccreditamentoAsc(providerId, AccreditamentoStatoEnum.ACCREDITATO, LocalDate.now());

		if(listaAccreditamentiAttivi != null && !listaAccreditamentiAttivi.isEmpty()){
			LOGGER.info("Trovati " + listaAccreditamentiAttivi.size() + " accreditamenti attivi per il provider: " + providerId);
			//prendo il primo (quello che sarà il primo a scadere)
			accreditamento = listaAccreditamentiAttivi.iterator().next();
			LOGGER.info("Selezionato come valido l'accreditamento: " + accreditamento.getId() + " valido fino al " + accreditamento.getDataFineAccreditamento());
		}else{
			throw new AccreditamentoNotFoundException("Nessun Accreditamento attivo trovato per il provider " + providerId);
		}

		return accreditamento;
	}

	@Override
	public AccreditamentoStatoEnum getStatoAccreditamento(Long accreditamentoId) {
		return accreditamentoRepository.getStatoByAccreditamentoId(accreditamentoId);
	}

	@Override
	@Transactional
	public void save(Accreditamento accreditamento) {
		LOGGER.debug("Salvataggio domanda di accreditamento " + accreditamento.getTipoDomanda() + " per il provider " + accreditamento.getProvider().getId());
		//accreditamentoRepository.save(accreditamento);
		saveAndAudit(accreditamento);
	}

	public void saveAndAudit(Accreditamento accreditamento) {
		accreditamentoRepository.saveAndFlush(accreditamento);
		auditService.commitForCurrrentUser(new AccreditamentoAudit(accreditamento));
	}

	public void audit(Accreditamento accreditamento) {
		auditService.commitForCurrrentUser(new AccreditamentoAudit(accreditamento));
	}

	@Override
	public boolean canProviderCreateAccreditamento(Long providerId, AccreditamentoTipoEnum tipoDomanda) {

		//per le domande standard è innanzitutto necessario che la segreteria abiliti il provider
		if(tipoDomanda == AccreditamentoTipoEnum.STANDARD){
			if(!providerService.canInsertAccreditamentoStandard(providerId)){
				LOGGER.debug(Utils.getLogMessage("Provider(" + providerId + ") - canProviderCreateAccreditamento: False -> " + "Non Abilitato"));
				return false;
			}
		}

		//si può reinserire una domanda provvisoria se la standard è stata diniegata e sono passati almeno 6 mesi
		if(tipoDomanda == AccreditamentoTipoEnum.PROVVISORIO) {
			Provider provider = providerService.getProvider(providerId);
			if(!providerService.canInsertAccreditamentoProvvisorio(providerId)) {
				LOGGER.debug(Utils.getLogMessage("Provider(" + providerId + ") - canProviderCreateAccreditamento: False -> " + "Non può ancora creare una nuova domanda provvisoria"));
				return false;
			}
		}

		Set<Accreditamento> accreditamentoList = getAllAccreditamentiForProvider(providerId, tipoDomanda);
		for(Accreditamento accreditamento : accreditamentoList){
			if(accreditamento.isBozza()){
				LOGGER.debug(Utils.getLogMessage("Provider(" + providerId + ") - canProviderCreateAccreditamento: False -> Presente domanda " + accreditamento.getId() + " in stato di " + accreditamento.getStato().name()));
				return false;
			}

			if(accreditamento.isProcedimentoAttivo()){
				LOGGER.debug(Utils.getLogMessage("Provider(" + providerId + ") - canProviderCreateAccreditamento: False -> Presente domanda " + accreditamento.getId() + " in stato di Procedimento Attivo"));
				return false;
			}

			//SOLO PER IL PROVVISORIO - se e' già attiva una domanda non facciamo crearne altre
			//PER LA STD invece si...caso del rinnovo
			if(tipoDomanda == AccreditamentoTipoEnum.PROVVISORIO){
				if (accreditamento.isDomandaAttiva()){
					LOGGER.debug(Utils.getLogMessage("Provider(" + providerId + ") - canProviderCreateAccreditamento: False -> Presente domanda " + accreditamento.getId() + " in stato di Domanda Attiva con scadenza " + accreditamento.getDataFineAccreditamento()));
					return false;
				}

			}
		}

		return true;
	}

	@Override
	//TODO capire perchè con @Transactional non ha effetto il salvataggio del wokflowService
	public void inviaDomandaAccreditamento(Long accreditamentoId) throws Exception{
		LOGGER.debug(Utils.getLogMessage("Invio domanda di Accreditamento " + accreditamentoId + " alla segreteria"));

		Accreditamento accreditamento = accreditamentoRepository.findOne(accreditamentoId);
		if(accreditamento.getDataInvio() == null)
			accreditamento.setDataInvio(LocalDate.now());
		accreditamento.setDataScadenza(accreditamento.getDataInvio().plusDays(180));

		accreditamento.getProvider().setStatus(ProviderStatoEnum.VALIDATO);

		if(accreditamento.isStandard()) {
			//accreditamento.setStato(AccreditamentoStatoEnum.VALUTAZIONE_SEGRETERIA_ASSEGNAMENTO);
			accreditamento.getProvider().setCanInsertAccreditamentoStandard(false);
			accreditamento.getProvider().setDataScadenzaInsertAccreditamentoStandard(null);
			accreditamento.getProvider().setInviatoAccreditamentoStandard(true);
		}

		fieldEditabileService.removeAllFieldEditabileForAccreditamento(accreditamentoId);

		protocolloService.protocollaDomandaInArrivo(accreditamentoId, accreditamento.getFileIdForProtocollo());

		try{
			if(accreditamento.getTipoDomanda() == AccreditamentoTipoEnum.PROVVISORIO)
				workflowService.createWorkflowAccreditamentoProvvisorio(Utils.getAuthenticatedUser(), accreditamento);
			if(accreditamento.getTipoDomanda() == AccreditamentoTipoEnum.STANDARD)
				workflowService.createWorkflowAccreditamentoStandard(Utils.getAuthenticatedUser(), accreditamento);
		}catch (Exception ex){
			LOGGER.debug(Utils.getLogMessage("Errore avvio Workflow Accreditamento per la domanda " + accreditamentoId));
			throw new Exception("Errore avvio Workflow Accreditamento per la domanda " + accreditamentoId);
		}

		//accreditamentoRepository.save(accreditamento);
		saveAndAudit(accreditamento);

	}

	@Override
	@Transactional
	public void inserisciPianoFormativo(Long accreditamentoId) {
		LOGGER.debug(Utils.getLogMessage("Inserimento piano formativo per la domanda di Accreditamento " + accreditamentoId));

		Accreditamento accreditamento = accreditamentoRepository.findOne(accreditamentoId);
		PianoFormativo pianoFormativo = new PianoFormativo();
		pianoFormativo.setAnnoPianoFormativo(LocalDate.now().getYear());
		pianoFormativo.setProvider(accreditamento.getProvider());
		pianoFormativoService.save(pianoFormativo);
		accreditamento.setPianoFormativo(pianoFormativo);
		//accreditamentoRepository.save(accreditamento);
		saveAndAudit(accreditamento);


		fieldEditabileService.removeAllFieldEditabileForAccreditamento(accreditamentoId);
		fieldEditabileService.insertFieldEditabileForAccreditamento(accreditamentoId, null, SubSetFieldEnum.FULL, new HashSet<IdFieldEnum>(Arrays.asList(IdFieldEnum.EVENTO_PIANO_FORMATIVO__FULL)));
	}

	@Override
	@Transactional
	public void inviaValutazioneDomanda(Long accreditamentoId, String valutazioneComplessiva, Set<Account> refereeGroup, VerbaleValutazioneSulCampo verbale) throws Exception {
		LOGGER.debug(Utils.getLogMessage("Assegnamento domanda di Accreditamento " + accreditamentoId + " ad un gruppo CRECM"));
		Valutazione valutazione = valutazioneService.getValutazioneByAccreditamentoIdAndAccountIdAndNotStoricizzato(accreditamentoId, Utils.getAuthenticatedUser().getAccount().getId());
		Accreditamento accreditamento = getAccreditamento(accreditamentoId);
		Account user = Utils.getAuthenticatedUser().getAccount();
		Long valutazioneId = valutazione.getId();

		//setta la data
		valutazione.setDataValutazione(LocalDateTime.now());
		//disabilito tutti i filedValutazioneAccreditamento
		for (FieldValutazioneAccreditamento fva : valutazione.getValutazioni()) {
			fva.setEnabled(false);
		}

		//inserisce il commento complessivo
		valutazione.setValutazioneComplessiva(valutazioneComplessiva);

		//setta lo stato dell'accreditamento al momento del salvataggio
		valutazione.setAccreditamentoStatoValutazione(accreditamento.getStato());

		valutazioneService.saveAndFlush(valutazione);

		//detacha e copia: da questo momento valutazione si riferisce alla copia storicizzata
		valutazioneService.copiaInStorico(valutazione);

		if(accreditamento.isProvvisorio()) {
			//il referee azzera il suo contatore di valutazioni non date consecutivamente e svuota la lista
			if (user.isReferee()) {
				user.setValutazioniNonDate(0);
				user.setDomandeNonValutate(new HashSet<Accreditamento>());
				accountRepository.save(user);
				workflowService.eseguiTaskValutazioneCrecmForCurrentUser(accreditamento);
			}
			//la segreteria crea le valutazioni per i referee
			if (user.isSegreteria()){
				List<String> usernameWorkflowValutatoriCrecm = new ArrayList<String>();
				for (Account a : refereeGroup) {
					Valutazione valutazioneReferee = new Valutazione();
					//setta i campi valutati positivamente di default
					valutazioneReferee.setValutazioni(fieldValutazioneAccreditamentoService.getValutazioniDefault(accreditamento));
					valutazioneReferee.setAccount(a);
					valutazioneReferee.setAccreditamento(accreditamento);
					valutazioneReferee.setTipoValutazione(ValutazioneTipoEnum.REFEREE);
					valutazioneService.save(valutazioneReferee);
					//valutazioneService.saveAndFlush(valutazioneReferee);
					emailService.inviaNotificaAReferee(a.getEmail(), accreditamento.getProvider().getDenominazioneLegale());
					usernameWorkflowValutatoriCrecm.add(a.getUsernameWorkflow());
				}
				accreditamento.setDataValutazioneCrecm(LocalDate.now());
				accreditamentoRepository.save(accreditamento);
				//saveAndAudit(accreditamento);

				//il numero minimo di valutazioni necessarie (se 3 Referee -> minimo 2)
				Integer numeroValutazioniCrecmRichieste = new Integer(usernameWorkflowValutatoriCrecm.size() - 1);
				workflowService.eseguiTaskValutazioneAssegnazioneCrecmForCurrentUser(accreditamento, usernameWorkflowValutatoriCrecm, numeroValutazioniCrecmRichieste);
			}
		} else if(accreditamento.isStandard()) {
			Valutazione valutazioneReload = valutazioneService.getValutazione(valutazioneId);
			//svuoto e riabilito la valutazione della segreteria (comunque salvata in storico)
			valutazioneReload.setDataValutazione(null);
			valutazioneReload.setDataOraScadenzaPossibilitaValutazione(null);
			valutazioneReload.setValutazioneComplessiva(null);
			valutazioneReload.getValutazioni().clear();
			valutazioneReload.setValutazioni(fieldValutazioneAccreditamentoService.getValutazioniDefault(accreditamento));
			valutazioneService.save(valutazioneReload);

			//segretario valutatore
			if(verbale != null) {
				verbale.setValutatore(Utils.getAuthenticatedUser().getAccount());
				accreditamento.setVerbaleValutazioneSulCampo(verbale);
			}
			//accreditamentoRepository.save(accreditamento);
			saveAndAudit(accreditamento);


			//è qui che è stata settala la data di valutazione del verbale e tutti i suoi componenti???
			// risposta: sì, ma rimane modificabile per tutta la durata di valutazione sul campo
			Set<String> dst = new HashSet<String>();
			if(verbale != null){
				if(verbale.getTeamLeader() != null)
					dst.add(verbale.getTeamLeader().getEmail());
				if(verbale.getReferenteInformatico() != null)
					dst.add(verbale.getReferenteInformatico().getEmail());

				if(verbale.getComponentiSegreteria() != null){
					for(Account a : verbale.getComponentiSegreteria())
						dst.add(a.getEmail());
				}
				emailService.inviaConvocazioneValutazioneSulCampo(dst, verbale.getGiorno(), accreditamento.getProvider().getDenominazioneLegale());
			}

			//non deve creare un task di valutazione per il team leader.. deve solo mandare il flusso in valutazione sul campo con lo stesso
			//attore (segretario) che deve inserire la valutazione sul campo.. solo se l'accreditamento va in integrazione il teamleader deve valutare
			workflowService.eseguiTaskValutazioneAssegnazioneTeamLeaderForCurrentUser(accreditamento, verbale.getTeamLeader().getUsernameWorkflow());
		}
	}

	@Override
	@Transactional
	public void inviaValutazioneTeamLeader(Long accreditamentoId, String valutazioneComplessiva) throws Exception {
		LOGGER.debug(Utils.getLogMessage("Invia Valutazione TeamLeader " + accreditamentoId));
		Account user = Utils.getAuthenticatedUser().getAccount();
		Valutazione valutazione = valutazioneService.getValutazioneByAccreditamentoIdAndAccountIdAndNotStoricizzato(accreditamentoId, user.getId());
		Accreditamento accreditamento = getAccreditamento(accreditamentoId);

		//setta la data
		valutazione.setDataValutazione(LocalDateTime.now());
		//disabilito tutti i filedValutazioneAccreditamento
		for (FieldValutazioneAccreditamento fva : valutazione.getValutazioni()) {
			fva.setEnabled(false);
		}

		//inserisce il commento complessivo
		valutazione.setValutazioneComplessiva(valutazioneComplessiva);

		//setta lo stato dell'accreditamento al momento del salvataggio
		valutazione.setAccreditamentoStatoValutazione(accreditamento.getStato());

		valutazioneService.saveAndFlush(valutazione);

		//detacha e copia: da questo momento valutazione si riferisce alla copia storicizzata
		valutazioneService.copiaInStorico(valutazione);
		workflowService.eseguiTaskValutazioneTeamLeaderForCurrentUser(accreditamento);

		//per il referee si azzera il suo contatore di valutazioni non date consecutivamente e svuota la lista
		user.setValutazioniNonDate(0);
		user.setDomandeNonValutate(new HashSet<Accreditamento>());
		accountRepository.save(user);
	}

	@Override
	public void approvaIntegrazione(Long accreditamentoId) throws Exception{

		Accreditamento accreditamento = getAccreditamento(accreditamentoId);
		Long workFlowProcessInstanceId = accreditamento.getWorkflowInCorso().getProcessInstanceId();
		//NB sono in valutazione
		AccreditamentoStatoEnum stato = accreditamento.getStatoUltimaIntegrazione();

		Set<FieldValutazioneAccreditamento> fieldValutazioniSegreteria = fieldValutazioneAccreditamentoService.getAllFieldValutazioneForAccreditamentoBySegreteriaNotStoricizzato(accreditamentoId);
		Set<FieldIntegrazioneAccreditamento> fieldIntegrazione = fieldIntegrazioneAccreditamentoService.getAllFieldIntegrazioneForAccreditamentoByContainer(accreditamentoId, stato, workFlowProcessInstanceId);

		Set<FieldIntegrazioneAccreditamento> approved = new HashSet<FieldIntegrazioneAccreditamento>();
		Set<FieldIntegrazioneAccreditamento> notApproved = new HashSet<FieldIntegrazioneAccreditamento>();

		fieldValutazioniSegreteria.forEach(v -> {
			FieldIntegrazioneAccreditamento field = null;
			if(v.getIdField().getGruppo().isEmpty()) {
				if(v.getEsito().booleanValue()){
					if(v.getObjectReference() == -1)
						field = Utils.getField(fieldIntegrazione, v.getIdField());
					else
						field = Utils.getField(fieldIntegrazione,v.getObjectReference(), v.getIdField());
					if(field != null)
						approved.add(field);
				}
				else {
					if(v.getObjectReference() == -1)
						field = Utils.getField(fieldIntegrazione, v.getIdField());
					else
						field = Utils.getField(fieldIntegrazione,v.getObjectReference(), v.getIdField());
					if(field != null)
						notApproved.add(field);
				}
			}
			//gestione del raggruppamento (non prevista per i multiinstanza)
			else {
				if(v.getEsito().booleanValue()){
					for(IdFieldEnum id : v.getIdField().getGruppo()) {
						field = Utils.getField(fieldIntegrazione, id);
						if(field != null) {
							approved.add(field);
						}
					}
				}
				else {
					for(IdFieldEnum id : v.getIdField().getGruppo()) {
						field = Utils.getField(fieldIntegrazione, id);
						if(field != null) {
							notApproved.add(field);
						}
					}
				}
			}
		});

		if(!accreditamento.isVariazioneDati()) {
			//calcolo se mi trovo in una situazione di integrazione o di preavviso di rigetto
			//guardo la data di inizio del preavviso di rigetto -> se è null devo per forza trovarmi ancora in integrazione
			if(accreditamento.getDataPreavvisoRigettoInizio() == null)
				accreditamento.setPresaVisioneIntegrazione(false);
			else accreditamento.setPresaVisionePreavvisoDiRigetto(false);
		}

		integrazioneService.applyIntegrazioneAccreditamentoAndSave(accreditamentoId, approved);
		integrazioneService.cancelObjectNotApproved(accreditamentoId, notApproved);

		fieldIntegrazioneAccreditamentoService.applyIntegrazioneInContainer(accreditamentoId, stato, workFlowProcessInstanceId);

	}


	@Override
	//ritorna il numero di referee che non hanno valutato
	public void riassegnaGruppoCrecm(Long accreditamentoId, Set<Account> refereeGroup) throws Exception {
		LOGGER.debug(Utils.getLogMessage("Riassegnamento domanda di Accreditamento " + accreditamentoId + " ad un ALTRO gruppo CRECM"));
		Accreditamento accreditamento = getAccreditamento(accreditamentoId);

		// crea le valutazioni per i nuovi referee
		List<String> usernameWorkflowValutatoriCrecm = new ArrayList<String>();
		for (Account a : refereeGroup) {
			Valutazione valutazioneReferee = new Valutazione();
			//setta i campi valutati positivamente di default
			valutazioneReferee.setValutazioni(fieldValutazioneAccreditamentoService.getValutazioniDefault(accreditamento));
			valutazioneReferee.setAccount(a);
			valutazioneReferee.setAccreditamento(accreditamento);
			valutazioneReferee.setTipoValutazione(ValutazioneTipoEnum.REFEREE);
			valutazioneService.save(valutazioneReferee);
			emailService.inviaNotificaAReferee(a.getEmail(), accreditamento.getProvider().getDenominazioneLegale());
			usernameWorkflowValutatoriCrecm.add(a.getUsernameWorkflow());
		}

		accreditamento.setDataValutazioneCrecm(LocalDate.now());
		//accreditamentoRepository.save(accreditamento);
		saveAndAudit(accreditamento);


		//il numero minimo di valutazioni necessarie (se 3 Referee -> minimo 2)
		Integer numeroValutazioniCrecmRichieste = new Integer(usernameWorkflowValutatoriCrecm.size() - 1);
		workflowService.eseguiTaskAssegnazioneCrecmForCurrentUser(accreditamento, usernameWorkflowValutatoriCrecm, numeroValutazioniCrecmRichieste);
	}

	//TODO al secondo giro e al terzo questo sarebbe il valuta domanda della segreteria.. sarebbe da fare un corrispettivo per lo standard
	//dove al primo giro crea la valutazione del team leader e al terzo la riassegna..
	@Override
	public void assegnaStessoGruppoCrecm(Long accreditamentoId, String valutazioneComplessiva) throws Exception {
		LOGGER.debug(Utils.getLogMessage("Riassegnamento domanda di Accreditamento " + accreditamentoId + " allo STESSO gruppo CRECM"));
		Valutazione valutazioneSegreteria = valutazioneService.getValutazioneByAccreditamentoIdAndAccountIdAndNotStoricizzato(accreditamentoId, Utils.getAuthenticatedUser().getAccount().getId());
		Accreditamento accreditamento = getAccreditamento(accreditamentoId);

		approvaIntegrazione(accreditamentoId);

		//disabilito tutti i filedValutazioneAccreditamento
		for (FieldValutazioneAccreditamento fva : valutazioneSegreteria.getValutazioni()) {
			fva.setEnabled(false);
		}

		//setta la data (per la presa visione)
		valutazioneSegreteria.setDataValutazione(LocalDateTime.now());
		//Non dovrebbe servire perche' passando in AssegnazioneCRECM la valutazione della segreteria è già bloccata
		/*
		//disabilito tutti i filedValutazioneAccreditamento
		for (FieldValutazioneAccreditamento fva : valutazioneSegreteria.getValutazioni()) {
			fva.setEnabled(false);
		}
		*/

		//inserisce il commento complessivo
		valutazioneSegreteria.setValutazioneComplessiva(valutazioneComplessiva);

		//setta lo stato dell'accreditamento al momento del salvataggio
		valutazioneSegreteria.setAccreditamentoStatoValutazione(accreditamento.getStato());

		valutazioneService.save(valutazioneSegreteria);

		valutazioneService.copiaInStorico(valutazioneSegreteria);

		//elimino le date delle vecchie valutazioni
		Set<Account> valutatori = valutazioneService.getAllValutatoriForAccreditamentoId(accreditamentoId);
		List<String> usernameWorkflowValutatoriCrecm = new ArrayList<String>();
		for(Account a : valutatori) {
			if(a.isReferee()) {
				usernameWorkflowValutatoriCrecm.add(a.getUsernameWorkflow());
				Valutazione valutazione = valutazioneService.getValutazioneByAccreditamentoIdAndAccountIdAndNotStoricizzato(accreditamentoId, a.getId());

				//TODO potrebbe spaccarsi perchè esistono gia - setta i campi valutati positivamente di default
				Set<FieldValutazioneAccreditamento> defaults = fieldValutazioneAccreditamentoService.getValutazioniDefault(accreditamento);
				for(FieldValutazioneAccreditamento f : valutazione.getValutazioni()){
					defaults.removeIf(field -> (
													field.getIdField() == f.getIdField() &&
													field.getObjectReference() == f.getObjectReference() &&
													field.getAccreditamento().getId() == f.getAccreditamento().getId()
													)
										);
				}

				if(!defaults.isEmpty())
					valutazione.getValutazioni().addAll(defaults);

				valutazione.setDataValutazione(null);
				valutazioneService.save(valutazione);
				emailService.inviaNotificaAReferee(a.getEmail(), accreditamento.getProvider().getDenominazioneLegale());
			}
		}

		accreditamento.setDataValutazioneCrecm(LocalDate.now());
		accreditamentoRepository.save(accreditamento);
		//saveAndAudit(accreditamento);

		workflowService.eseguiTaskValutazioneSegreteriaForCurrentUser(accreditamento, false, usernameWorkflowValutatoriCrecm);
	}

	@Override
	public void assegnaTeamLeader(Long accreditamentoId, String valutazioneComplessiva) throws Exception {
		LOGGER.debug(Utils.getLogMessage("Riassegnamento domanda di Accreditamento " + accreditamentoId + " al Team Leader"));
		Valutazione valutazioneSegreteria = valutazioneService.getValutazioneByAccreditamentoIdAndAccountIdAndNotStoricizzato(accreditamentoId, Utils.getAuthenticatedUser().getAccount().getId());
		Accreditamento accreditamento = getAccreditamento(accreditamentoId);

		approvaIntegrazione(accreditamentoId);

		//setta la data (per la presa visione)
		valutazioneSegreteria.setDataValutazione(LocalDateTime.now());

		//disabilito tutti i filedValutazioneAccreditamento
		for (FieldValutazioneAccreditamento fva : valutazioneSegreteria.getValutazioni()) {
			fva.setEnabled(false);
		}

		//inserisce il commento complessivo
		valutazioneSegreteria.setValutazioneComplessiva(valutazioneComplessiva);

		//setta lo stato dell'accreditamento al momento del salvataggio
		valutazioneSegreteria.setAccreditamentoStatoValutazione(accreditamento.getStato());

		valutazioneService.save(valutazioneSegreteria);

		valutazioneService.copiaInStorico(valutazioneSegreteria);

		Account accountTeamLeader = accreditamento.getVerbaleValutazioneSulCampo().getTeamLeader();
		String usernameWorkflowTeamLeader = accountTeamLeader.getUsernameWorkflow();

		//accreditamento.setDataValutazioneCrecm(LocalDate.now());
		accreditamentoRepository.save(accreditamento);
		//saveAndAudit(accreditamento);

		workflowService.eseguiTaskValutazioneSegreteriaTeamLeaderForCurrentUser(accreditamento, false, usernameWorkflowTeamLeader);
	}

	@Override
	public void presaVisione(Long accreditamentoId) throws Exception {
		LOGGER.debug(Utils.getLogMessage("Presa visione della conferma dei dati da parte del provider e cambiamento stato della domanda " + accreditamentoId + " in INS_ODG"));
		Accreditamento accreditamento = getAccreditamento(accreditamentoId);

		accreditamento.setDataInserimentoOdg(LocalDate.now());

		//calcolo se mi trovo in una situazione di presa visione dell'integrazione o del preavviso di rigetto
		//guardo la data di inizio del preavviso di rigetto -> se è null devo per forza trovarmi ancora in integrazione
		if(accreditamento.getDataPreavvisoRigettoInizio() == null)
			accreditamento.setPresaVisioneIntegrazione(true);
		else accreditamento.setPresaVisionePreavvisoDiRigetto(true);

		accreditamentoRepository.save(accreditamento);
		//saveAndAudit(accreditamento);


		Long workFlowProcessInstanceId = accreditamento.getWorkflowInCorso().getProcessInstanceId();
		AccreditamentoStatoEnum stato = accreditamento.getStatoUltimaIntegrazione();

		//setto il container dei field integrazione ad applicati
		fieldIntegrazioneAccreditamentoService.applyIntegrazioneInContainer(accreditamentoId, stato, workFlowProcessInstanceId);

		if(accreditamento.isProvvisorio())
			workflowService.eseguiTaskValutazioneSegreteriaForCurrentUser(accreditamento, true, null);
		else if (accreditamento.isStandard())
			workflowService.eseguiTaskValutazioneSegreteriaTeamLeaderForCurrentUser(accreditamento, true, null);

	}

	@Override
	@Transactional
	public void inviaRichiestaIntegrazione(Long accreditamentoId, Long giorniTimer) throws Exception {
		LOGGER.debug(Utils.getLogMessage("Invio Richiesta Integrazione della domanda " + accreditamentoId + " al Provider"));
		Accreditamento accreditamento = getAccreditamento(accreditamentoId);
		if(accreditamento.getWorkflowInCorso().getTipo() == TipoWorkflowEnum.ACCREDITAMENTO) {
			accreditamento.setGiorniIntegrazione(giorniTimer);
		} else {
			accreditamento.getWorkflowInCorso().setGiorniIntegrazione(giorniTimer);
		}
		accreditamentoRepository.save(accreditamento);
		//saveAndAudit(accreditamento);


		Long timerIntegrazioneRigetto = giorniTimer * millisecondiInGiorno;
		if(ecmProperties.isDebugTestMode() && giorniTimer < 0) {
			//Per efffettuare i test si da la possibilità di inserire il tempo in minuti
			timerIntegrazioneRigetto = (-giorniTimer) * millisecondiInMinuto;
		}
		workflowService.eseguiTaskRichiestaIntegrazioneForCurrentUser(accreditamento, timerIntegrazioneRigetto);
	}

	@Override
	@Transactional
	public void inviaRichiestaPreavvisoRigetto(Long accreditamentoId, Long giorniTimer) throws Exception {
		LOGGER.debug(Utils.getLogMessage("Invio Richiesta Preavviso Rigetto della domanda " + accreditamentoId + " al Provider"));
		Accreditamento accreditamento = getAccreditamento(accreditamentoId);
		accreditamento.setGiorniPreavvisoRigetto(giorniTimer);
		//accreditamentoRepository.save(accreditamento);
		saveAndAudit(accreditamento);


		Long timerIntegrazioneRigetto = giorniTimer * millisecondiInGiorno;
		if(ecmProperties.isDebugTestMode() && giorniTimer < 0) {
			//Per efffettuare i test si da la possibilità di inserire il tempo in minuti
			timerIntegrazioneRigetto = (-giorniTimer) * millisecondiInMinuto;
		}
		workflowService.eseguiTaskRichiestaPreavvisoRigettoForCurrentUser(accreditamento, timerIntegrazioneRigetto);
	}

	@Override
	public void inviaIntegrazione(Long accreditamentoId) throws Exception {
		LOGGER.debug(Utils.getLogMessage("Integrazione della domanda " + accreditamentoId + " inviata alla segreteria per essere valutata"));

		Accreditamento accreditamento = getAccreditamento(accreditamentoId);
		Long workFlowProcessInstanceId = accreditamento.getWorkflowInCorso().getProcessInstanceId();
		AccreditamentoStatoEnum stato = accreditamento.getStatoUltimaIntegrazione();

		//controllo quali campi sono stati modificati e quali confermati
		Set<FieldIntegrazioneAccreditamento> fieldIntegrazioneList = fieldIntegrazioneAccreditamentoService.getAllFieldIntegrazioneForAccreditamentoByContainer(accreditamentoId, stato, workFlowProcessInstanceId);
		integrazioneService.checkIfFieldIntegrazioniConfirmedForAccreditamento(accreditamentoId, fieldIntegrazioneList);
		fieldIntegrazioneAccreditamentoService.saveSet(fieldIntegrazioneList);

		//per i campi modificati...elimino i field integrazione su tutte le valutazioni presenti
		Set<FieldIntegrazioneAccreditamento> fieldModificati = fieldIntegrazioneAccreditamentoService.getModifiedFieldIntegrazioneForAccreditamento(accreditamentoId, stato, workFlowProcessInstanceId);

		//se ci sono state delle modifiche ri-abilito la valutazione cancellando la data
		if(fieldModificati != null && !fieldModificati.isEmpty()){
			//elimina data valutazione se flusso di accreditamento
			if(!accreditamento.isVariazioneDati()) {
				Set<Valutazione> valutazioni = valutazioneService.getAllValutazioniCompleteForAccreditamentoIdAndNotStoricizzato(accreditamentoId);
				for(Valutazione valutazione : valutazioni){
					if(valutazione.getTipoValutazione() == ValutazioneTipoEnum.SEGRETERIA_ECM){
						valutazione.setDataValutazione(null);
						valutazioneService.save(valutazione);
					}
				}
			}
		}

		//setto il flag per vedere se ci sono state modifiche di integrazione nei field valutazioni, elimino la vecchia valutazione e li riabilito
		Set<Valutazione> valutazioni = valutazioneService.getAllValutazioniForAccreditamentoIdAndNotStoricizzato(accreditamentoId);
		sbloccaValutazioniByFieldIntegrazioneList(valutazioni, fieldIntegrazioneList);


		//creo una lista di fieldIntegrazione fittizia, per applicare sui fieldValutazione l'info che un campo abilitato non è stato modificato dal provider
		//il fieldIntegrazione (che non verrà salvato su db, ma utilizzato solo per richiamare la 'sbloccaValutazioniByFieldIntegrazioneList' è realizzato valorizzando solo
		//i campi: objectReference, idField, isModificato
		Long id = -1L;
		Set<FieldIntegrazioneAccreditamento> fieldIntegrazioneListFITTIZIA = new HashSet<FieldIntegrazioneAccreditamento>();
		Set<FieldEditabileAccreditamento> fieldEditabileList = fieldEditabileService.getAllFieldEditabileForAccreditamento(accreditamentoId);
		if(fieldEditabileList != null){
			for(FieldEditabileAccreditamento fieldEditabile : fieldEditabileList){
				//per ogni fieldEditabile (abilitato attraverso l'enableField sulla domanda), controllo se NON esiste il fieldIntegrazione creato dal provider
				FieldIntegrazioneAccreditamento fieldIntegrazione = null;
				if(fieldEditabile.getObjectReference() != -1){
					fieldIntegrazione = Utils.getField(fieldIntegrazioneList, fieldEditabile.getObjectReference(), fieldEditabile.getIdField());
				}else{
					fieldIntegrazione = Utils.getField(fieldIntegrazioneList, fieldEditabile.getIdField());
				}

				if(fieldIntegrazione == null){
					//NON esiste il fieldIntegrazione creato dal provider...creo quello fittizio
					if(fieldEditabile.getObjectReference() != -1){
						fieldIntegrazione = new FieldIntegrazioneAccreditamento(fieldEditabile.getIdField(), fieldEditabile.getAccreditamento(), fieldEditabile.getObjectReference(),null,null);
					}else{
						fieldIntegrazione = new FieldIntegrazioneAccreditamento(fieldEditabile.getIdField(), fieldEditabile.getAccreditamento(), null,null);
					}
					fieldIntegrazione.setModificato(false);
					fieldIntegrazione.setId(id--);

					fieldIntegrazioneListFITTIZIA.add(fieldIntegrazione);
				}
			}

			if(fieldIntegrazioneListFITTIZIA != null && !fieldIntegrazioneListFITTIZIA.isEmpty()){
				sbloccaValutazioniByFieldIntegrazioneList(valutazioni, fieldIntegrazioneListFITTIZIA);
				fieldIntegrazioneListFITTIZIA.clear();
			}
		}

		//TODO non spacca niente???
		fieldEditabileService.removeAllFieldEditabileForAccreditamento(accreditamentoId);

		if(accreditamento.isIntegrazione()){
			emailService.inviaConfermaReInvioIntegrazioniAccreditamento(accreditamento.isStandard(), false, accreditamento.getProvider());
		}
		else if(accreditamento.isPreavvisoRigetto()){
			emailService.inviaConfermaReInvioIntegrazioniAccreditamento(accreditamento.isStandard(), true, accreditamento.getProvider());
		}
		else if(accreditamento.isModificaDati()){
			//workflowService.eseguiTaskIntegrazioneForCurrentUser(accreditamento);
		}
	}

	@Override
	public void eseguiTaskInviaIntegrazione(Long accreditamentoId) throws Exception{
		LOGGER.debug(Utils.getLogMessage("Esecuzione Task - Integrazione della domanda " + accreditamentoId));
		Accreditamento accreditamento = getAccreditamento(accreditamentoId);

		if(accreditamento.isIntegrazione()){
			workflowService.eseguiTaskIntegrazioneForCurrentUser(accreditamento);
		}
		else if(accreditamento.isPreavvisoRigetto()){
			workflowService.eseguiTaskPreavvisoRigettoForCurrentUser(accreditamento);
		}
		else if(accreditamento.isModificaDati()){
			workflowService.eseguiTaskIntegrazioneForCurrentUser(accreditamento);
		}
	}

	//metodo che gestisce l'abilitazione dei fieldValutazione a seconda dei fieldIntegrazione (applica per tutte le valutazioni passate)
	private void sbloccaValutazioniByFieldIntegrazioneList(Set<Valutazione> valutazioni, Set<FieldIntegrazioneAccreditamento> fieldIntegrazioneList) {
		for(Valutazione valutazione : valutazioni){
			sbloccaValutazioneByFieldIntegrazioneList(valutazione, fieldIntegrazioneList);
		}
	}

	//metodo che gestisce l'abilitazione dei fieldValutazione a seconda dei fieldIntegrazione
	private void sbloccaValutazioneByFieldIntegrazioneList(Valutazione valutazione, Set<FieldIntegrazioneAccreditamento> fieldIntegrazioneList) {
		Set<FieldValutazioneAccreditamento> fieldValutazioni = valutazione.getValutazioni();
		for(FieldIntegrazioneAccreditamento fieldIntegrazione : fieldIntegrazioneList){
			FieldValutazioneAccreditamento field = null;
			LOGGER.debug(Utils.getLogMessage("Sblocco valutazione per " + fieldIntegrazione.getIdField()));
			if(fieldIntegrazione.getObjectReference() != -1){
				//multi-istanza
				field = Utils.getField(fieldValutazioni, fieldIntegrazione.getObjectReference(), fieldIntegrazione.getIdField());
			}else{
				//non multi-istanza
				field = Utils.getField(fieldValutazioni, fieldIntegrazione.getIdField());
			}
			if(field != null && !field.isEnabled()){
				field.setEsito(null);
				field.setEnabled(true);
				field.setNote(null);
				if(fieldIntegrazione.isModificato())
					field.setModificatoInIntegrazione(true);
				else
					field.setModificatoInIntegrazione(false);
				fieldValutazioni.add(field);
			}
		}
		//ciclo per gli idField enum cercado quelli raggruppati (prendo il padre)
		for(IdFieldEnum id : IdFieldEnum.values()) {
			FieldIntegrazioneAccreditamento fieldInteg = null;
			FieldValutazioneAccreditamento fieldVal = null;
			boolean modificato = false;
			if(!id.getGruppo().isEmpty()) {
				//controllo se ci sono fieldIntegrazione attivi per i figli
				for(IdFieldEnum idGruppo : id.getGruppo()) {
					fieldInteg = Utils.getField(fieldIntegrazioneList, idGruppo);
					if(fieldInteg != null) {
						//se ci sono mi faccio dare il fieldValutazione del padre
						fieldVal = Utils.getField(fieldValutazioni, id);
						if(fieldVal != null && fieldInteg.isModificato()) {
							modificato = true;
						}
					}
				}
				//modifico di conseguenza il fieldValutazione del padre
				if(fieldVal != null && !fieldVal.isEnabled()) {
					fieldVal.setEsito(null);
					fieldVal.setEnabled(true);
					fieldVal.setNote(null);
					fieldVal.setModificatoInIntegrazione(modificato);
				}
			}
		}
		valutazione.setValutazioni(fieldValutazioni);
		valutazioneService.save(valutazione);
	}

	@Override
	public DatiAccreditamento getDatiAccreditamentoForAccreditamentoId(Long accreditamentoId) throws Exception{
		LOGGER.debug(Utils.getLogMessage("Recupero datiAccreditamento per la domanda " + accreditamentoId));
		DatiAccreditamento datiAccreditamento = accreditamentoRepository.getDatiAccreditamentoForAccreditamento(accreditamentoId);
		if(datiAccreditamento == null)
			throw new Exception("Dati non presenti");
		return datiAccreditamento;
	}

	@Override
	public Long getProviderIdForAccreditamento(Long accreditamentoId) {
		LOGGER.debug(Utils.getLogMessage("Recupero providerId per domanda " + accreditamentoId));
		return accreditamentoRepository.getProviderIdById(accreditamentoId);
	}

	@Override
	public Set<Accreditamento> getAllAccreditamentiInviati(){
		LOGGER.debug(Utils.getLogMessage("Recupero delle domande di accreditamento inviate alla segreteria"));
		return accreditamentoRepository.findAllByStato(AccreditamentoStatoEnum.VALUTAZIONE_SEGRETERIA_ASSEGNAMENTO);
	}

	@Override
	public int countAllAccreditamentiByStatoAndProviderId(AccreditamentoStatoEnum stato, Long providerId) {
		LOGGER.debug(Utils.getLogMessage("Numero delle domande di accreditamento " + stato + " per provider " + providerId));
		return accreditamentoRepository.countAllByStatoAndProviderId(stato, providerId);
	}

	@Override
	public Set<Accreditamento> getAllAccreditamentiByStatoAndProviderId(AccreditamentoStatoEnum stato, Long providerId) {
		LOGGER.debug(Utils.getLogMessage("Recupero delle domande di accreditamento " + stato + " per provider " + providerId));
		return accreditamentoRepository.findAllByStatoAndProviderId(stato, providerId);
	}

	//recupera tutti gli accreditamenti a seconda dello stato e del tipo, il flag filterTaken settato a true aggiunge la richiesta
	//di filtrare tutti gli accreditamenti già presi in carica (la funzione restituisce così solo gli accreditamenti che possono essere presi in carica)
	@Override
	public Set<Accreditamento> getAllAccreditamentiByStatoAndTipoDomanda(AccreditamentoStatoEnum stato,	AccreditamentoTipoEnum tipo, Boolean filterTaken) {
		if (tipo != null) {
			if (filterTaken != null && filterTaken == true) {
				LOGGER.debug(Utils.getLogMessage("Recupero delle domande di accreditamento " + stato + " di tipo " + tipo + " NON prese in carica"));
				return accreditamentoRepository.findAllByStatoAndTipoDomandaNotTaken(stato, tipo);
			}
			else {
				LOGGER.debug(Utils.getLogMessage("Recupero delle domande di accreditamento " + stato + " di tipo " + tipo));
				return accreditamentoRepository.findAllByStatoAndTipoDomanda(stato, tipo);
			}
		}
		else {
			if (filterTaken != null && filterTaken == true) {
				LOGGER.debug(Utils.getLogMessage("Recupero delle domande di accreditamento " + stato + " NON prese in carica"));
				return accreditamentoRepository.findAllByStatoNotTaken(stato);
			}
			else {
				LOGGER.debug(Utils.getLogMessage("Recupero delle domande di accreditamento " + stato));
				return accreditamentoRepository.findAllByStato(stato);
			}
		}
	}

	//conta tutti gli accreditamenti a seconda dello stato e del tipo, il flag filterTaken settato a true aggiunge la richiesta
	//di filtrare tutti gli accreditamenti già presi in carica (la funzione restituisce così solo il numero degli accreditamenti che possono essere presi in carica)
	@Override
	public int countAllAccreditamentiByStatoAndTipoDomanda(AccreditamentoStatoEnum stato, AccreditamentoTipoEnum tipo, Boolean filterTaken) {
		if (tipo != null) {
			if (filterTaken != null && filterTaken == true) {
				LOGGER.debug(Utils.getLogMessage("Conteggio delle domande di accreditamento " + stato + " di tipo " + tipo + " NON prese in carica"));
				return accreditamentoRepository.countAllByStatoAndTipoDomandaNotTaken(stato, tipo);
			}
			else {
				LOGGER.debug(Utils.getLogMessage("Conteggio delle domande di accreditamento " + stato + " di tipo " + tipo));
				return accreditamentoRepository.countAllByStatoAndTipoDomanda(stato, tipo);
			}
		}
		else {
			if (filterTaken != null && filterTaken == true) {
				LOGGER.debug(Utils.getLogMessage("Conteggio delle domande di accreditamento " + stato + " NON prese in carica"));
				return accreditamentoRepository.countAllByStatoNotTaken(stato);
			}
			else {
				LOGGER.debug(Utils.getLogMessage("Conteggio delle domande di accreditamento " + stato));
				return accreditamentoRepository.countAllByStato(stato);
			}
		}
	}

	@Override
	public Set<Accreditamento> getAllAccreditamentiByGruppoAndTipoDomanda(String gruppo, AccreditamentoTipoEnum tipo, Boolean filterTaken) {
		Set<AccreditamentoStatoEnum> stati = AccreditamentoStatoEnum.getAllStatoByGruppo(gruppo);
		if (tipo != null) {
			if (filterTaken != null && filterTaken == true) {
				LOGGER.debug(Utils.getLogMessage("Recupero delle domande di accreditamento del gruppo " + gruppo + " di tipo " + tipo + " NON prese in carica"));
				return accreditamentoRepository.findAllByStatoInAndTipoDomandaNotTaken(stati, tipo);
			}
			else {
				LOGGER.debug(Utils.getLogMessage("Recupero delle domande di accreditamento del gruppo " + gruppo + " di tipo " + tipo));
				return accreditamentoRepository.findAllByStatoInAndTipoDomanda(stati, tipo);
			}
		}
		else {
			if (filterTaken != null && filterTaken == true) {
				LOGGER.debug(Utils.getLogMessage("Recupero delle domande di accreditamento del gruppo " + gruppo + " NON prese in carica"));
				return accreditamentoRepository.findAllByStatoInNotTaken(stati);
			}
			else {
				LOGGER.debug(Utils.getLogMessage("Recupero delle domande di accreditamento del gruppo " + gruppo));
				return accreditamentoRepository.findAllByStatoIn(stati);
			}
		}
	}

	@Override
	public int countAllAccreditamentiByGruppoAndTipoDomanda(String gruppo, AccreditamentoTipoEnum tipo, Boolean filterTaken) {
		Set<AccreditamentoStatoEnum> stati = AccreditamentoStatoEnum.getAllStatoByGruppo(gruppo);
		if (tipo != null) {
			if (filterTaken != null && filterTaken == true) {
				LOGGER.debug(Utils.getLogMessage("Conteggio delle domande di accreditamento del gruppo " + gruppo + " di tipo " + tipo + " NON prese in carica"));
				return accreditamentoRepository.countAllByStatoInAndTipoDomandaNotTaken(stati, tipo);
			}
			else {
				LOGGER.debug(Utils.getLogMessage("Conteggio delle domande di accreditamento del gruppo " + gruppo + " di tipo " + tipo));
				return accreditamentoRepository.countAllByStatoInAndTipoDomanda(stati, tipo);
			}
		}
		else {
			if (filterTaken != null && filterTaken == true) {
				LOGGER.debug(Utils.getLogMessage("Conteggio delle domande di accreditamento del gruppo " + gruppo + " NON prese in carica"));
				return accreditamentoRepository.countAllByStatoInNotTaken(stati);
			}
			else {
				LOGGER.debug(Utils.getLogMessage("Conteggio delle domande di accreditamento del gruppo " + gruppo));
				return accreditamentoRepository.countAllByStatoIn(stati);
			}
		}
	}

	//recupera tutti gli accreditamenti in stato INS_ODG NON ancora inseriti in NESSUNA seduta NON bloccata
	@Override
	public Set<Accreditamento> getAllAccreditamentiInseribiliInODG() {
		LOGGER.debug(Utils.getLogMessage("Recupero delle domande di accreditamento in stato INS_ODG NON inseriti in NESSUNA Seduta non bloccata/valutata"));
		return accreditamentoRepository.findAllAccreditamentiInseribiliInODG();
	}

	//conta tutti gli accreditamenti in stato INS_ODG NON ancora inseriti in NESSUNA seduta NON bloccata
	@Override
	public int countAllAccreditamentiInseribiliInODG() {
		LOGGER.debug(Utils.getLogMessage("Conteggio delle domande di accreditamento in stato INS_ODG NON inseriti in NESSUNA Seduta non bloccata/valutata"));
		return accreditamentoRepository.countAllAccreditamentiInseribiliInODG();
	}

	//recupera tutti gli accreditamenti a seconda dello stato e del tipo che sono state assegnate in valutazione ad un certo id utente
	@Override
	public Set<Accreditamento> getAllAccreditamentiByStatoAndTipoDomandaForValutatoreId(AccreditamentoStatoEnum stato, AccreditamentoTipoEnum tipo, Long id, Boolean filterDone) {
		if (tipo != null) {
			if(filterDone != null && filterDone == true) {
				LOGGER.debug(Utils.getLogMessage("Recupero le domande di accreditamento " + stato + " di tipo " + tipo + " assegnate all'id: " + id + ", che NON ha ancora valutato"));
				return accreditamentoRepository.findAllByStatoAndTipoDomandaInValutazioneAssignedToAccountIdNotDone(stato, tipo, id);
			}
			else {
				LOGGER.debug(Utils.getLogMessage("Recupero le domande di accreditamento " + stato + " di tipo " + tipo + " assegnate all'id: " + id));
				return accreditamentoRepository.findAllByStatoAndTipoDomandaInValutazioneAssignedToAccountId(stato, tipo, id);
			}
		}
		else {
			if(filterDone != null && filterDone == true) {
				LOGGER.debug(Utils.getLogMessage("Recupero le domande di accreditamento " + stato + " assegnate all'id: " + id + ", che NON ha ancora valutato"));
				return accreditamentoRepository.findAllByStatoInValutazioneAssignedToAccountIdNotDone(stato, id);
			}
			else {
				LOGGER.debug(Utils.getLogMessage("Recupero le domande di accreditamento " + stato + " assegnate all'id: " + id));
				return accreditamentoRepository.findAllByStatoInValutazioneAssignedToAccountId(stato, id);
			}
		}
	}

	//conta tutti gli accreditamenti a seconda dello stato e del tipo che sono state assegnate in valutazione ad un certo id utente
	@Override
	public int countAllAccreditamentiByStatoAndTipoDomandaForValutatoreId(AccreditamentoStatoEnum stato, AccreditamentoTipoEnum tipo, Long id, Boolean filterDone) {
		if (tipo != null) {
			if(filterDone != null && filterDone == true) {
				LOGGER.debug(Utils.getLogMessage("Conteggio delle domande di accreditamento " + stato + " di tipo " + tipo + " assegnate all'id: " + id + ", che NON ha ancora valutato"));
				return accreditamentoRepository.countAllByStatoAndTipoDomandaInValutazioneAssignedToAccountIdNotDone(stato, tipo, id);
			}
			else {
				LOGGER.debug(Utils.getLogMessage("Conteggio delle domande di accreditamento " + stato + " di tipo " + tipo + " assegnate all'id: " + id));
				return accreditamentoRepository.countAllByStatoAndTipoDomandaInValutazioneAssignedToAccountId(stato, tipo, id);
			}
		}
		else {
			if(filterDone != null && filterDone == true) {
				LOGGER.debug(Utils.getLogMessage("Conteggio delle domande di accreditamento " + stato + " assegnate all'id: " + id + ", che NON ha ancora valutato"));
				return accreditamentoRepository.countAllByStatoInValutazioneAssignedToAccountIdNotDone(stato, id);
			}
			else {
				LOGGER.debug(Utils.getLogMessage("Conteggio delle domande di accreditamento " + stato + " assegnate all'id: " + id));
				return accreditamentoRepository.countAllByStatoInValutazioneAssignedToAccountId(stato, id);
			}
		}
	}

	//recupera tutti gli accreditamenti a seconda dello stato e del tipo che sono state assegnate ad un certo id provider
	@Override
	public Set<Accreditamento> getAllAccreditamentiByStatoAndTipoDomandaForProviderId(AccreditamentoStatoEnum stato, AccreditamentoTipoEnum tipo, Long providerId) {
		if (tipo != null) {
			LOGGER.debug(Utils.getLogMessage("Conteggio delle domande di accreditamento " + stato + " di tipo " + tipo + " assegnate al provider: " + providerId));
			return accreditamentoRepository.findAllByStatoAndTipoDomandaAndProviderId(stato, tipo, providerId);
		}
		else {
			LOGGER.debug(Utils.getLogMessage("Conteggio delle domande di accreditamento " + stato + " assegnate al provider: " + providerId));
			return accreditamentoRepository.findAllByStatoAndProviderId(stato, providerId);
		}
	}

	//conta tutti gli accreditamenti a seconda dello stato e del tipo che sono state assegnate ad un certo id provider
	@Override
	public int countAllAccreditamentiByStatoAndTipoDomandaForProviderId(AccreditamentoStatoEnum stato, AccreditamentoTipoEnum tipo, Long id) {
		if (tipo != null) {
			LOGGER.debug(Utils.getLogMessage("Conteggio delle domande di accreditamento " + stato + " di tipo " + tipo + " assegnate al provider: " + id));
			return accreditamentoRepository.countAllByStatoAndTipoDomandaAndProviderId(stato, tipo, id);
		}
		else {
			LOGGER.debug(Utils.getLogMessage("Conteggio delle domande di accreditamento " + stato + " assegnate al provider: " + id));
			return accreditamentoRepository.countAllByStatoAndProviderId(stato, id);
		}
	}

	//recupera tutte le domande di accreditamento in scadenza
	//controlla se la data è compresa tra la data di scadenza e 30 giorni alla data di scadenza
	@Override
	public Set<Accreditamento> getAllAccreditamentiInScadenza() {
		LocalDate oggi = LocalDate.now();
		LocalDate dateScadenza = LocalDate.now().plusDays(30);
		return accreditamentoRepository.findAllByDataScadenzaProssima(oggi, dateScadenza);
	}

	//conta tutte le domande di accreditamento in scadenza
	//controlla se oggi + 30 giorni supera la data di scadenza
	@Override
	public int countAllAccreditamentiInScadenza() {
		LocalDate oggi = LocalDate.now();
		LocalDate dateScadenza = LocalDate.now().plusDays(30);
		return accreditamentoRepository.countAllByDataScadenzaProssima(oggi, dateScadenza);
	}

	@Override
	/*
	 * L'utente segreteria può prendere in carica una domanda se:
	 * 	+ La domanda è in stato VALUTAZIONE_SEGRTERIA_ASSEGNAMENTO
	 *  + E NON esiste già una valutazione di tipo SEGRETERIA_ECM (significa che nessun altro l'ha già presa in carico)
	 *	+ Deve esistere il corrispondente Task da prendere in carica nel flusso
	 */
	public boolean canUserPrendiInCarica(Long accreditamentoId, CurrentUser currentUser) throws Exception {
		if(currentUser.isSegreteria() && getAccreditamento(accreditamentoId).isValutazioneSegreteriaAssegnamento()) {
			Set<Valutazione> valutazioniAccreditamento = valutazioneService.getAllValutazioniForAccreditamentoIdAndNotStoricizzato(accreditamentoId);
			for (Valutazione v : valutazioniAccreditamento) {
				if(v.getTipoValutazione() == ValutazioneTipoEnum.SEGRETERIA_ECM)
					return false;
			}

			//TODO rimuovere il seguente "if" quando si avrà il flusso STANDARD
			if(getAccreditamento(accreditamentoId).isProvvisorio()) {
			TaskInstanceDataModel task = workflowService.currentUserGetTaskForState(getAccreditamento(accreditamentoId));
			if(task == null){
				return false;
			}

			if(task.isAssigned())
				return false;

			}

			return true;
		}
		else
			return false;
	}

	@Override
	/*
	 * L'utente (segreteria | referee) può andare in validate e valutare se:
	 * 	+ ESISTE una valutazione agganciata al suo account e non è stata ancora inviata (dataValutazione == NULL)
	 *  + ESISTE il TASK relativo all'utente sul flusso
	 */
	public boolean canUserValutaDomanda(Long accreditamentoId, CurrentUser currentUser) throws Exception{
		Accreditamento accreditamento = getAccreditamento(accreditamentoId);

		if( ((accreditamento.isValutazioneSegreteriaAssegnamento() || accreditamento.isValutazioneSulCampo() || accreditamento.isValutazioneSegreteria()) && currentUser.isSegreteria()) ||
			(accreditamento.isValutazioneCrecm() && currentUser.isReferee()) ||
			(accreditamento.isValutazioneTeamLeader() && currentUser.isReferee()) ||
			(accreditamento.isValutazioneSegreteriaVariazioneDati() && currentUser.isSegreteria()) ||
			(accreditamento.isValutazioneCrecmVariazioneDati() && currentUser.isReferee())){
			Valutazione valutazione = valutazioneService.getValutazioneByAccreditamentoIdAndAccountIdAndNotStoricizzato(accreditamentoId, currentUser.getAccount().getId());
			if(valutazione != null && valutazione.getDataValutazione() == null){
				TaskInstanceDataModel task = workflowService.currentUserGetTaskForState(accreditamento);
				//TODO rimuovere il seguente "if" quando si avrà il flusso STANDARD
				if(accreditamento.isStandard())
					return true;
				//TODO rimuovere il seguente "if" quando si avrà il flusso VARIAZIONE DATI E DOCUMENTI
				if(accreditamento.isVariazioneDati())
					return true;
				if(task == null){
					return false;
				}
				if(accreditamento.isValutazioneSegreteria()){
					if(!task.isAssigned() && !canUserPresaVisione(accreditamentoId, currentUser))
						return true;
					else
						return false;
				}else{
					if(!task.isAssigned())
						return false;
				}

				return true;
			}
		}
		return false;
	}

	@Override
	/*
	 * L'utente (segreteria | referee) può visualizzare la valutazione:
	 * 	+ ESISTE una valutazione agganciata al suo account ed è stata inviata (dataValutazione != NULL)
	 */
	public boolean canUserValutaDomandaShow(Long accreditamentoId, CurrentUser currentUser) {
		Valutazione valutazione = valutazioneService.getValutazioneByAccreditamentoIdAndAccountIdAndNotStoricizzato(accreditamentoId, currentUser.getAccount().getId());
		if(valutazione != null && (currentUser.isSegreteria() || currentUser.isReferee()))
			return true;
		else return false;
	}

	@Override
	/*
	 * L'utente (segreteria | commissioneECM) può visualizzare tutte le valutazioni inviate
	 */
	public boolean canUserValutaDomandaShowRiepilogo(Long accreditamentoId, CurrentUser currentUser) {
		if(currentUser.isSegreteria() || currentUser.isCommissioneEcm()){
			Set<Valutazione> valutazioni = valutazioneService.getAllValutazioniCompleteForAccreditamentoIdAndNotStoricizzato(accreditamentoId);
			return !valutazioni.isEmpty();
		}
		return false;
	}

	@Override
	/*
	 * L'utente (segreteria | commissioneECM) può visualizzare tutte le valutazioni inviate
	 */
	public boolean canUserValutaDomandaShowStorico(Long accreditamentoId, CurrentUser currentUser) {
		if(currentUser.isSegreteria() || currentUser.isCommissioneEcm() || currentUser.isReferee()){
			Set<Valutazione> valutazioni = valutazioneService.getAllValutazioniStoricizzateForAccreditamentoId(accreditamentoId);
			return !valutazioni.isEmpty();
		}
		return false;
	}

	@Override
	/*
	 * L'utente (segreteria) può riassegnare l'accreditamento ad un altro gruppo di referee crecm
	 */
	public boolean canRiassegnaGruppo(Long accreditamentoId, CurrentUser currentUser) {
		if(currentUser.isSegreteria() && getAccreditamento(accreditamentoId).isAssegnamento())
			return true;
		return false;
	}

	@Override
	/*
	 * L'utente (segreteria) può riassegnare l'accreditamento allo stesso gruppo referee crecm
	 */
	public boolean canUserPresaVisione(Long accreditamentoId, CurrentUser currentUser) throws Exception {
		Accreditamento accreditamento = getAccreditamento(accreditamentoId);
		if(currentUser.isSegreteria() && accreditamento.isValutazioneSegreteria()){
			//disabilitato per consentire ad ogni utente segreteria di fare "presa visione"
//			Valutazione valutazione = valutazioneService.getValutazioneByAccreditamentoIdAndAccountId(accreditamentoId, currentUser.getAccount().getId());
//			if(valutazione != null && valutazione.getTipoValutazione() == ValutazioneTipoEnum.SEGRETERIA_ECM && valutazione.getDataValutazione() != null){
			Long workFlowProcessInstanceId = accreditamento.getWorkflowInCorso().getProcessInstanceId();
			AccreditamentoStatoEnum stato = accreditamento.getStatoUltimaIntegrazione();
			Set<FieldIntegrazioneAccreditamento> fields = fieldIntegrazioneAccreditamentoService.getModifiedFieldIntegrazioneForAccreditamento(accreditamentoId, stato, workFlowProcessInstanceId);
			TaskInstanceDataModel task = workflowService.currentUserGetTaskForState(accreditamento);
			if(task == null){
				return false;
			}

			if(fields == null || fields.isEmpty())
				return true;
		}
//		}
		return false;
	}

	@Override
	/*
	 * L'utente (segreteria) può abilitare i campi per eventuale modifica
	 */
	public boolean canUserEnableField(Long accreditamentoId, CurrentUser currentUser) throws Exception {
		Accreditamento accreditamento = getAccreditamento(accreditamentoId);
		if(currentUser.isSegreteria() && (accreditamento.isRichiestaIntegrazione() || accreditamento.isRichiestaPreavvisoRigetto())){
			TaskInstanceDataModel task = workflowService.currentUserGetTaskForState(accreditamento);
			if(task == null){
				return false;
			}
			if(!task.isAssigned())
				return true;
			return true;
		}
		return false;
	}

	@Override
	public boolean canUserInviaRichiestaIntegrazione(Long accreditamentoId, CurrentUser currentUser) throws Exception {
		return canUserEnableField(accreditamentoId, currentUser) || canUserInviaCampiVariazioneDati(accreditamentoId, currentUser);
	}

	@Override
	/*
	 * La domanda deve essere in INTEGRAZIONE
	 * 	+	L'utente provider titolare della domanda
	 * 	+ 	La segreteria
	 * */
	public boolean canUserInviaIntegrazione(Long accreditamentoId, CurrentUser currentUser) throws Exception{
		Accreditamento accreditamento = getAccreditamento(accreditamentoId);
		if(accreditamento.isIntegrazione() || accreditamento.isPreavvisoRigetto()){
			if(currentUser.isProvider()){
				Long providerId = getProviderIdForAccreditamento(accreditamentoId);
				if(currentUser.getAccount().getProvider() != null &&  currentUser.getAccount().getProvider().getId().equals(providerId)){
					TaskInstanceDataModel task = workflowService.currentUserGetTaskForState(accreditamento);
					if(task == null){
						return false;
					}
					return true;
				}
				return false;
			}
		}
		return false;
	}

	@Override
	public boolean canUserInviaVariazioneDati(Long accreditamentoId, CurrentUser currentUser) {
		Accreditamento accreditamento = getAccreditamento(accreditamentoId);
		if(accreditamento.isModificaDati() && currentUser.isProvider()){
			Long providerId = getProviderIdForAccreditamento(accreditamentoId);
			if(currentUser.getAccount().getProvider() != null &&  currentUser.getAccount().getProvider().getId().equals(providerId)){
				return true;
			}
		}
		return false;
	}

	@Override
	public void changeState(Long accreditamentoId, AccreditamentoStatoEnum stato) throws Exception  {
		changeState(accreditamentoId, stato, null);
	}

	@Override
	public void changeState(Long accreditamentoId, AccreditamentoStatoEnum stato, Boolean eseguitoDaUtente) throws Exception  {
		Accreditamento accreditamento = accreditamentoRepository.findOne(accreditamentoId);

		WorkflowInfo workflowInCorso = accreditamento.getWorkflowInCorso();
		if(workflowInCorso == null)
			throw new Exception("AccreditamentoService - changeState: Impossibile ricavare il workflow in corso per l'accreaditamento id: " + accreditamento.getId());
		Boolean presaVisione = false;
		//In alcuni stati devono essere effettuate altre operazioni
		//Creazione pdf
		if(workflowInCorso.getTipo() == TipoWorkflowEnum.ACCREDITAMENTO) {
			if(stato == AccreditamentoStatoEnum.VALUTAZIONE_SEGRETERIA_ASSEGNAMENTO) {
				accreditamento.startRestartConteggio();
			} else if(stato == AccreditamentoStatoEnum.INTEGRAZIONE) {
				accreditamento.standbyConteggio();
				accreditamento.setDataIntegrazioneInizio(LocalDate.now());
			} else if(stato == AccreditamentoStatoEnum.PREAVVISO_RIGETTO) {
				accreditamento.standbyConteggio();
				accreditamento.setDataPreavvisoRigettoInizio(LocalDate.now());
			} else if(stato == AccreditamentoStatoEnum.RICHIESTA_INTEGRAZIONE_IN_PROTOCOLLAZIONE) {
				//Ricavo la seduta
				Seduta seduta = null;
				for (ValutazioneCommissione valCom : accreditamento.getValutazioniCommissione()) {
					//TODO nel caso vengano agganciati piu' flussi alla domanda occorre prendere l'ultima ValutazioneCommissione
					if(valCom.getStato() == AccreditamentoStatoEnum.RICHIESTA_INTEGRAZIONE) {
						seduta = valCom.getSeduta();
					}
				}
				Set<FieldEditabileAccreditamento> fieldEditabiliAccreditamento = fieldEditabileService.getAllFieldEditabileForAccreditamento(accreditamento.getId());
				List<String> listaCriticita = new ArrayList<String>();
				fieldEditabiliAccreditamento.forEach(v -> {
		            //Richiesta
		            //Riepilogo_Consegne_ECM_20.10.2016.docx - Modulo 7 - 40 - a [inserire singole note sui campi] (pag 4)
					if(v.getNota() == null || v.getNota().isEmpty())
						listaCriticita.add(messageSource.getMessage("IdFieldEnum." + v.getIdField().name(), null, Locale.getDefault()));
					else
						listaCriticita.add(messageSource.getMessage("IdFieldEnum." + v.getIdField().name(), null, Locale.getDefault()) + "\n" + v.getNota());
				});
				PdfAccreditamentoProvvisorioIntegrazionePreavvisoRigettoInfo integrazioneInfo = new PdfAccreditamentoProvvisorioIntegrazionePreavvisoRigettoInfo(accreditamento, seduta, listaCriticita);
				integrazioneInfo.setGiorniIntegrazionePreavvisoRigetto(accreditamento.getGiorniIntegrazione());
				File file = null;
				if(accreditamento.getTipoDomanda() == AccreditamentoTipoEnum.PROVVISORIO)
					file = pdfService.creaPdfAccreditamentoProvvisiorioIntegrazione(integrazioneInfo);
				else if(accreditamento.getTipoDomanda() == AccreditamentoTipoEnum.STANDARD)
					file = pdfService.creaPdfAccreditamentoStandardIntegrazione(integrazioneInfo);
				protocolloService.protocollaAllegatoFlussoDomandaInUscita(accreditamentoId, file.getId());
				accreditamento.setRichiestaIntegrazione(file);
				accreditamento.setDataoraInvioProtocollazione(LocalDateTime.now());
				//protocollo il file
			} else if(stato == AccreditamentoStatoEnum.VALUTAZIONE_SEGRETERIA) {
				//mi sono spostato da INTEGRAZIONE o PREAVVISO_RIGETTO a VALUTAZIONE_SEGRETERIA quindi rimuovo i fieldEditabili
				accreditamento.startRestartConteggio();
				if(accreditamento.getStato() == AccreditamentoStatoEnum.INTEGRAZIONE) {
					accreditamento.setDataIntegrazioneFine(LocalDate.now());
					accreditamento.setIntegrazioneEseguitaDaProvider(eseguitoDaUtente);
				} else if(accreditamento.getStato() == AccreditamentoStatoEnum.PREAVVISO_RIGETTO) {
					accreditamento.setDataPreavvisoRigettoFine(LocalDate.now());
					accreditamento.setPreavvisoRigettoEseguitoDaProvider(eseguitoDaUtente);
				}
				inviaIntegrazione(accreditamentoId);
				fieldEditabileService.removeAllFieldEditabileForAccreditamento(accreditamentoId);
			} else if(stato == AccreditamentoStatoEnum.RICHIESTA_PREAVVISO_RIGETTO_IN_PROTOCOLLAZIONE) {
				//Ricavo la seduta
				Seduta seduta = null;
				for (ValutazioneCommissione valCom : accreditamento.getValutazioniCommissione()) {
					if(valCom.getStato() == AccreditamentoStatoEnum.RICHIESTA_PREAVVISO_RIGETTO) {
						seduta= valCom.getSeduta();
					}
				}
				Set<FieldEditabileAccreditamento> fieldEditabiliAccreditamento = fieldEditabileService.getAllFieldEditabileForAccreditamento(accreditamento.getId());
				List<String> listaCriticita = new ArrayList<String>();
				fieldEditabiliAccreditamento.forEach(v -> {
					listaCriticita.add(messageSource.getMessage("IdFieldEnum." + v.getIdField().name(), null, Locale.getDefault()) + " - " + v.getNota());
				});
				PdfAccreditamentoProvvisorioIntegrazionePreavvisoRigettoInfo preavvisoRigettoInfo = new PdfAccreditamentoProvvisorioIntegrazionePreavvisoRigettoInfo(accreditamento, seduta, listaCriticita);
				preavvisoRigettoInfo.setGiorniIntegrazionePreavvisoRigetto(accreditamento.getGiorniPreavvisoRigetto());
				File file = null;
				if(accreditamento.getTipoDomanda() == AccreditamentoTipoEnum.PROVVISORIO)
					file = pdfService.creaPdfAccreditamentoProvvisiorioPreavvisoRigetto(preavvisoRigettoInfo);
				else if(accreditamento.getTipoDomanda() == AccreditamentoTipoEnum.STANDARD)
					file = pdfService.creaPdfAccreditamentoStandardPreavvisoRigetto(preavvisoRigettoInfo);
				protocolloService.protocollaAllegatoFlussoDomandaInUscita(accreditamentoId, file.getId());
				accreditamento.setRichiestaPreavvisoRigetto(file);
				accreditamento.setDataoraInvioProtocollazione(LocalDateTime.now());
			} else if(stato == AccreditamentoStatoEnum.DINIEGO_IN_PROTOCOLLAZIONE) {
				//Ricavo la seduta
				Seduta sedutaRigetto = null;
				Seduta sedutaIntegrazione = null;
				Seduta sedutaPreavvisoRigetto = null;
				for (ValutazioneCommissione valCom : accreditamento.getValutazioniCommissione()) {
					if(valCom.getStato() == AccreditamentoStatoEnum.DINIEGO) {
						sedutaRigetto = valCom.getSeduta();
					} else if (valCom.getStato() == AccreditamentoStatoEnum.RICHIESTA_INTEGRAZIONE) {
						sedutaIntegrazione = valCom.getSeduta();
					} else if (valCom.getStato() == AccreditamentoStatoEnum.RICHIESTA_PREAVVISO_RIGETTO) {
						sedutaPreavvisoRigetto = valCom.getSeduta();
					}
				}
				/*
				Set<FieldIntegrazioneAccreditamento> fieldIntegrazioneAccreditamento = fieldIntegrazioneAccreditamentoService.getAllFieldIntegrazioneForAccreditamento(accreditamento.getId());
				List<String> listaCriticita = new ArrayList<String>();
				fieldIntegrazioneAccreditamento.forEach(v -> {
					listaCriticita.add(messageSource.getMessage("IdFieldEnum." + v.getIdField().name(), null, Locale.getDefault()));
				});*/
				PdfAccreditamentoProvvisorioRigettoInfo rigettoInfo = new PdfAccreditamentoProvvisorioRigettoInfo(accreditamento, sedutaRigetto, sedutaIntegrazione, sedutaPreavvisoRigetto);
				File file = null;
				if(accreditamento.getTipoDomanda() == AccreditamentoTipoEnum.PROVVISORIO)
					file = pdfService.creaPdfAccreditamentoProvvisiorioDiniego(rigettoInfo);
				else if(accreditamento.getTipoDomanda() == AccreditamentoTipoEnum.STANDARD)
					file = pdfService.creaPdfAccreditamentoStandardDiniego(rigettoInfo);
				protocolloService.protocollaAllegatoFlussoDomandaInUscita(accreditamentoId, file.getId());
				accreditamento.setDecretoDiniego(file);
				accreditamento.setDataoraInvioProtocollazione(LocalDateTime.now());
			} else if(stato == AccreditamentoStatoEnum.DINIEGO_IN_PROTOCOLLAZIONE) {
				//Setto il flusso come concluso
				workflowInCorso.setStato(StatoWorkflowEnum.CONCLUSO);
			} else if(stato == AccreditamentoStatoEnum.ACCREDITATO_IN_PROTOCOLLAZIONE) {
				//Ricavo la seduta
				Seduta sedutaAccreditamento = null;
				Seduta sedutaIntegrazione = null;
				Seduta sedutaPreavvisoRigetto = null;
				for (ValutazioneCommissione valCom : accreditamento.getValutazioniCommissione()) {
					if(valCom.getStato() == AccreditamentoStatoEnum.ACCREDITATO) {
						sedutaAccreditamento = valCom.getSeduta();
					} else if (valCom.getStato() == AccreditamentoStatoEnum.RICHIESTA_INTEGRAZIONE) {
						sedutaIntegrazione = valCom.getSeduta();
					} else if (valCom.getStato() == AccreditamentoStatoEnum.RICHIESTA_PREAVVISO_RIGETTO) {
						sedutaPreavvisoRigetto = valCom.getSeduta();
					}
				}
				//Set<FieldIntegrazioneAccreditamento> fieldIntegrazioneAccreditamento = fieldIntegrazioneAccreditamentoService.getAllFieldIntegrazioneForAccreditamento(accreditamento.getId());
				PdfAccreditamentoProvvisorioAccreditatoInfo accreditatoInfo = new PdfAccreditamentoProvvisorioAccreditatoInfo(accreditamento, sedutaAccreditamento, sedutaIntegrazione, sedutaPreavvisoRigetto);
				File file = null;
				if(accreditamento.getTipoDomanda() == AccreditamentoTipoEnum.PROVVISORIO)
					file = pdfService.creaPdfAccreditamentoProvvisiorioAccreditato(accreditatoInfo);
				else if(accreditamento.getTipoDomanda() == AccreditamentoTipoEnum.STANDARD)
					file = pdfService.creaPdfAccreditamentoStandardAccreditato(accreditatoInfo);
				protocolloService.protocollaAllegatoFlussoDomandaInUscita(accreditamentoId, file.getId());
				accreditamento.setDecretoAccreditamento(file);
				accreditamento.setDataoraInvioProtocollazione(LocalDateTime.now());
			} else if(stato == AccreditamentoStatoEnum.ACCREDITATO) {
				//Setto il flusso come concluso
				workflowInCorso.setStato(StatoWorkflowEnum.CONCLUSO);
			} else if(stato == AccreditamentoStatoEnum.INS_ODG) {
				//Cancelliamo le Valutazioni non completate dei referee e del team leader
				Set<Valutazione> valutazioni = valutazioneService.getAllValutazioniForAccreditamentoIdAndNotStoricizzato(accreditamentoId);
				for(Valutazione v : valutazioni){
					if((v.getTipoValutazione() == ValutazioneTipoEnum.REFEREE || v.getTipoValutazione() == ValutazioneTipoEnum.TEAM_LEADER) && v.getDataValutazione() == null){
						valutazioneService.delete(v);
					}
				}
			} else if(stato == AccreditamentoStatoEnum.VALUTAZIONE_TEAM_LEADER) {
				Account accountTeamLeader = accreditamento.getVerbaleValutazioneSulCampo().getTeamLeader();
				String usernameWorkflowTeamLeader = accountTeamLeader.getUsernameWorkflow();

				//controllo se ho gia la valutazione per lutente corrente
				Valutazione valutazioneTL = valutazioneService.getValutazioneByAccreditamentoIdAndAccountIdAndNotStoricizzato(accreditamentoId, accountTeamLeader.getId());

				//valutazione creata per la prima volta (valutazione integrazione)
				if(valutazioneTL == null) {
					//Ricavo la valutazione della segreteria perche' contiene i field editabili
					Valutazione valutazioneSegreteria = valutazioneService.getValutazioneSegreteriaForAccreditamentoIdNotStoricizzato(accreditamentoId);
					//ATTENZIONE la valutazioe restituita è detachata me è sempre lo stesso oggetto valutazioneSegreteria
					valutazioneTL = valutazioneService.detachValutazione(valutazioneSegreteria);
					valutazioneService.cloneDetachedValutazione(valutazioneTL);
					//valutazioneService.setEsitoForEnabledFields(valutazioneTL, null);

					valutazioneTL.setStoricizzato(false);
					valutazioneTL.setDataValutazione(null);
					valutazioneTL.setAccount(accountTeamLeader);
					valutazioneTL.setAccreditamento(accreditamento);
					valutazioneTL.setTipoValutazione(ValutazioneTipoEnum.TEAM_LEADER);
				}
				//mi trovo in valutazione del preavviso di rigetto
				else {
					valutazioneTL.setDataValutazione(null);
				}
				//recupero le integrazioni a partire dal container che la segreteria ha appena applicato
				Set<FieldIntegrazioneAccreditamento> fieldIntegrazioneList = fieldIntegrazioneAccreditamentoService.getAllFieldIntegrazioneForAccreditamentoByContainer(accreditamentoId, accreditamento.getStatoUltimaIntegrazione(), workflowInCorso.getProcessInstanceId());

				//sblocco field Valutazione a seconda dei field Integrazione
				sbloccaValutazioneByFieldIntegrazioneList(valutazioneTL, fieldIntegrazioneList);

				valutazioneService.save(valutazioneTL);
				emailService.inviaNotificaATeamLeader(accountTeamLeader.getEmail(), accreditamento.getProvider().getDenominazioneLegale());
			}

			//calcolo se la segreteria ha fatto presa visione
			//sa da valutazione segreteria vado diretto in INS_ODG c'è stata presa visione
			if(accreditamento.getStato() == AccreditamentoStatoEnum.VALUTAZIONE_SEGRETERIA && stato == AccreditamentoStatoEnum.INS_ODG) {
				presaVisione =  true;
			}
			accreditamento.setStato(stato);
		} else if(workflowInCorso.getTipo() == TipoWorkflowEnum.VARIAZIONE_DATI) {
			if(stato == AccreditamentoStatoEnum.RICHIESTA_INTEGRAZIONE_IN_PROTOCOLLAZIONE) {
				//Ricavo la seduta
				Seduta seduta = null;
				for (ValutazioneCommissione valCom : accreditamento.getValutazioniCommissione()) {
					//Prendo la seduta con data maggiore della data di avvio del flusso
					if(valCom.getStato() == AccreditamentoStatoEnum.RICHIESTA_INTEGRAZIONE) {
						if(valCom.getSeduta().getData().isAfter(workflowInCorso.getDataAvvio()))
							seduta = valCom.getSeduta();
					}
				}
				Set<FieldEditabileAccreditamento> fieldEditabiliAccreditamento = fieldEditabileService.getAllFieldEditabileForAccreditamento(accreditamento.getId());
				List<String> listaCriticita = new ArrayList<String>();
				fieldEditabiliAccreditamento.forEach(v -> {
		            //Richiesta
		            //Riepilogo_Consegne_ECM_20.10.2016.docx - Modulo 7 - 40 - a [inserire singole note sui campi] (pag 4)
					if(v.getNota() == null || v.getNota().isEmpty())
						listaCriticita.add(messageSource.getMessage("IdFieldEnum." + v.getIdField().name(), null, Locale.getDefault()));
					else
						listaCriticita.add(messageSource.getMessage("IdFieldEnum." + v.getIdField().name(), null, Locale.getDefault()) + "\n" + v.getNota());
				});
				//Da aggiungere se viene richiesta la creazione di file nel procedimento di "Variazione Dati"
//				PdfAccreditamentoProvvisorioIntegrazionePreavvisoRigettoInfo integrazioneInfo = new PdfAccreditamentoProvvisorioIntegrazionePreavvisoRigettoInfo(accreditamento, seduta, listaCriticita);
//				integrazioneInfo.setGiorniIntegrazionePreavvisoRigetto(workflowInCorso.getGiorniIntegrazione());
//				File file = null;
//				//creaPdfAccreditamentoVariazioneDatiIntegrazione va modificato e' una copi di creaPdfAccreditamentoIntegrazione
//				file = pdfService.creaPdfAccreditamentoVariazioneDatiIntegrazione(integrazioneInfo);
//				protocolloService.protocollaAllegatoFlussoDomandaInUscita(accreditamentoId, file.getId());
//				workflowInCorso.setRichiestaVariazioneDati(file);
				//protocollo il file
			} else if(stato == AccreditamentoStatoEnum.VALUTAZIONE_SEGRETERIA) {
				//mi sono spostato da INTEGRAZIONE a VALUTAZIONE_SEGRETERIA quindi rimuovo i fieldEditabili
				inviaIntegrazione(accreditamentoId);
				fieldEditabileService.removeAllFieldEditabileForAccreditamento(accreditamentoId);
				workflowInCorso.setIntegrazioneEseguitaDaProvider(eseguitoDaUtente);
			} else if(stato == AccreditamentoStatoEnum.INS_ODG) {
				//Cancelliamo le Valutazioni non completate dei referee e del team leader
				Set<Valutazione> valutazioni = valutazioneService.getAllValutazioniForAccreditamentoIdAndNotStoricizzato(accreditamentoId);
				for(Valutazione v : valutazioni){
					if((v.getTipoValutazione() == ValutazioneTipoEnum.REFEREE) && v.getDataValutazione() == null){
						valutazioneService.delete(v);
					}
				}
			} else if(stato == AccreditamentoStatoEnum.CONCLUSO) {
				//Setto il flusso come concluso
				workflowInCorso.setStato(StatoWorkflowEnum.CONCLUSO);
			}
			accreditamento.setStatoVariazioneDati(stato);
			//non e' prevista la presa visione della segreteria
		} else if(workflowInCorso.getTipo() == TipoWorkflowEnum.DECADENZA) {
			if(stato == AccreditamentoStatoEnum.CANCELLATO) {
				//Setto il flusso come concluso
				workflowInCorso.setStato(StatoWorkflowEnum.CONCLUSO);
			}
			if(stato == AccreditamentoStatoEnum.INS_ODG)
				gestioneChiusuraAccreditamento(accreditamento);
			accreditamento.setStato(stato);
		}

		//indipendentemente dal tipo di workflow se vado in uno stato di integrazione creo il relativo container
		if(stato == AccreditamentoStatoEnum.INTEGRAZIONE || stato == AccreditamentoStatoEnum.PREAVVISO_RIGETTO) {
			fieldIntegrazioneAccreditamentoService.createFieldIntegrazioneHistoryContainer(accreditamentoId, stato, workflowInCorso.getProcessInstanceId());
		}

		//salvo history
		if(workflowInCorso.getTipo() == TipoWorkflowEnum.VARIAZIONE_DATI)
			accreditamentoStatoHistoryService.createHistoryFine(accreditamento, workflowInCorso.getProcessInstanceId(), stato, accreditamento.getStatoVariazioneDati(), LocalDateTime.now(), presaVisione);
		else
			accreditamentoStatoHistoryService.createHistoryFine(accreditamento, workflowInCorso.getProcessInstanceId(), stato, accreditamento.getStato(), LocalDateTime.now(), presaVisione);


		//TODO se si chiama il servizio di protocollazione verrà settato uno stato intermedio di attesa protocollazione
		//TODO registrazione cronologia degli stati
		//accreditamentoRepository.save(accreditamento);
		saveAndAudit(accreditamento);

		//se lo stato è DINIEGO o ACCREDITATO storicizzo le valutazioni attive per la domanda
		if(stato == AccreditamentoStatoEnum.ACCREDITATO || stato == AccreditamentoStatoEnum.DINIEGO || stato == AccreditamentoStatoEnum.CONCLUSO) {
			Set<Valutazione> valutazioniAttive = valutazioneService.getAllValutazioniForAccreditamentoIdAndNotStoricizzato(accreditamentoId);
			for(Valutazione v : valutazioniAttive) {
				v.setStoricizzato(true);
				valutazioneService.save(v);
			}
		}

		alertEmailService.creaAlertForProvider(accreditamento, workflowInCorso);
	}

	//metodo che gestisce le procedure lasciate in sospeso da un accreditamento da concludere
	private void gestioneChiusuraAccreditamento(Accreditamento accreditamento) {
		//rimuove la valutazione commissione dell'accreditamento inserito in una seduta APERTA
		if(accreditamento.isInsOdg()) {
			valutazioneCommissioneRepository.deleteOneByAccreditamentoAndSedutaLockedFalse(accreditamento);
		}
		//cancellazione dei field integrazione ed editabili
		else if(accreditamento.isModificaDati() || accreditamento.isIntegrazione() || accreditamento.isPreavvisoRigetto()) {
			fieldEditabileService.removeAllFieldEditabileForAccreditamento(accreditamento.getId());
//			fieldIntegrazioneAccreditamentoService.removeAllFieldIntegrazioneForAccreditamento(accreditamento.getId());
		}
		//in ogni caso chiudo le valutazioni esistenti come storicizzate e stato cancellato
		Set<Valutazione> valutazioniAttive = valutazioneService.getAllValutazioniForAccreditamentoIdAndNotStoricizzato(accreditamento.getId());
		for(Valutazione v : valutazioniAttive) {
			v.setStoricizzato(true);
			v.setDataValutazione(LocalDateTime.now());
			v.setAccreditamentoStatoValutazione(AccreditamentoStatoEnum.CANCELLATO);
			valutazioneService.save(v);
		}
	}

	@Override
	public void prendiInCarica(Long accreditamentoId, CurrentUser currentUser) throws Exception{
		Accreditamento accreditamento = getAccreditamento(accreditamentoId);

		workflowService.prendiTaskInCarica(currentUser, accreditamento);

		Valutazione valutazione = new Valutazione();

		if(accreditamento.isStandard()) {
			//prendo l'ultimo diff dell'accreditamento
			AccreditamentoDiff diffOld = diffService.findLastDiffByProviderId(accreditamento.getProvider().getId());
			if(diffOld == null)
				throw new Exception("Ultimo diff per il provider " + accreditamento.getProvider().getId() + " non trovato!");
			//gestione diff
			AccreditamentoDiff diffNew = diffService.creaAllDiffAccreditamento(accreditamento);
			//va a prendere le valutazioni generate durante il diff del nuovo accreditamento col vecchio
			Set<FieldValutazioneAccreditamento> valutazioniDiff = diffService.confrontaDiffAccreditamento(diffOld, diffNew);
			//gestione valutazioni di default
			Set<FieldValutazioneAccreditamento> valutazioniDefault = fieldValutazioneAccreditamentoService.getValutazioniDefault(accreditamento);
			//gestione intersezione default e diff
			valutazione.setValutazioni(handleValutazioniDefaultDiff(valutazioniDiff, valutazioniDefault));
		}
		else {
			//setta i campi valutati positivamente di default
			valutazione.setValutazioni(fieldValutazioneAccreditamentoService.getValutazioniDefault(accreditamento));
		}

		//utente corrente che prende in carico
		Account segretarioEcm = currentUser.getAccount();
		valutazione.setAccount(segretarioEcm);

		//accreditamento
		valutazione.setAccreditamento(accreditamento);

		//tipo di valutatore
		valutazione.setTipoValutazione(ValutazioneTipoEnum.SEGRETERIA_ECM);

		valutazioneService.save(valutazione);
	}

	private Set<FieldValutazioneAccreditamento> handleValutazioniDefaultDiff(Set<FieldValutazioneAccreditamento> valutazioniDiff, Set<FieldValutazioneAccreditamento> valutazioniDefault) {
		LOGGER.debug(Utils.getLogMessage("Gestione delle valutazioni di default e diff"));

		//N.B. tutti i field valutazione sono già presenti su db

		//rimuove dalle default quelle gestite in diff che andranno a sostituire (rimuove anche da db)
		for(FieldValutazioneAccreditamento fva : valutazioniDiff) {
			Iterator<FieldValutazioneAccreditamento> iterDefault = valutazioniDefault.iterator();
			while(iterDefault.hasNext()) {
				FieldValutazioneAccreditamento fvaDefault = iterDefault.next();
				if(fva.getIdField() == fvaDefault.getIdField()
						&& fva.getObjectReference() == fvaDefault.getObjectReference()) {
					iterDefault.remove();
					fieldValutazioneAccreditamentoService.delete(fvaDefault.getId());
				}
			}
		}

		valutazioniDefault.addAll(valutazioniDiff);
		return valutazioniDefault;
	}

	public boolean canUserInviaAValutazioneCommissione(Long accreditamentoId, CurrentUser currentUser) throws Exception {
		Accreditamento accreditamento = getAccreditamento(accreditamentoId);
		if(currentUser.isSegreteria() && accreditamento.isInsOdg()){
			TaskInstanceDataModel task = workflowService.currentUserGetTaskForState(accreditamento);
			if(task == null){
				return false;
			}
			if(!task.isAssigned())
				return true;
		}
		return false;
	};

	@Override
	@Transactional
	public void inserisciInValutazioneCommissioneForSystemUser(Long accreditamentoId) throws Exception{
		workflowService.eseguiTaskInsOdgForSystemUser(getAccreditamento(accreditamentoId));
	}

	@Override
	public boolean canUserInserisciValutazioneCommissione(Long accreditamentoId, CurrentUser currentUser) throws Exception {
		Accreditamento accreditamento = getAccreditamento(accreditamentoId);
		if(currentUser.isSegreteria() && accreditamento.isValutazioneCommissione()){
			TaskInstanceDataModel task = workflowService.currentUserGetTaskForState(accreditamento);
			if(task == null){
				return false;
			}
			if(!task.isAssigned())
				return true;
		}
		return false;
	}

	@Override
	public void inviaValutazioneCommissione(Seduta seduta, Long accreditamentoId, AccreditamentoStatoEnum stato) throws Exception{
		workflowService.eseguiTaskInserimentoEsitoOdgForCurrentUser(getAccreditamento(accreditamentoId), stato);
		if(!getAccreditamento(accreditamentoId).isVariazioneDati())
			settaStatusProviderAndDateAccreditamentoAndQuotaAnnuale(seduta.getData(), accreditamentoId, stato);
	}

	@Override
	public void settaStatusProviderAndDateAccreditamentoAndQuotaAnnuale(LocalDate dataSeduta, Long accreditamentoId, AccreditamentoStatoEnum stato) throws Exception{
		Provider provider = providerService.getProvider(getProviderIdForAccreditamento(accreditamentoId));
		if(stato == AccreditamentoStatoEnum.ACCREDITATO){
			Accreditamento accreditamento = getAccreditamento(accreditamentoId);
			if(accreditamento.isProvvisorio()) {
				provider.setStatus(ProviderStatoEnum.ACCREDITATO_PROVVISORIAMENTE);
				accreditamento.setDataFineAccreditamento(dataSeduta.plusYears(2));
				accreditamento.setDataInizioAccreditamento(LocalDate.now());
			} else {
				provider.setStatus(ProviderStatoEnum.ACCREDITATO_STANDARD);
				if(dataSeduta != null)
					accreditamento.setDataFineAccreditamento(dataSeduta.plusYears(4));
				else
					accreditamento.setDataFineAccreditamento(LocalDate.now().plusYears(4));
				accreditamento.setDataInizioAccreditamento(LocalDate.now());
			}
			save(accreditamento);
		}
		if(stato == AccreditamentoStatoEnum.DINIEGO) {
			provider.setStatus(ProviderStatoEnum.DINIEGO);
			//se la domanda diniegata è standard setta il necessario per ripresentare una nuova domanda provvisoria
			Set<Accreditamento> accreditamentiProvvisori = getAllAccreditamentiForProvider(provider.getId(), AccreditamentoTipoEnum.PROVVISORIO);
			//controllo se c'è un accreditamento provvisorio attivo e se la sua data di scadenza è > a 6 mesi da oggi
			for(Accreditamento a : accreditamentiProvvisori) {
				if(a.isDomandaAttiva() && a.getDataFineAccreditamento().isAfter(LocalDate.now().plusMonths(6))) {
					//in questo caso la data di inserimento nuovo provvisorio  = data fine accreditamento della domanda attiva
					provider.setCanInsertAccreditamentoProvvisorio(true);
					provider.setDataRinnovoInsertAccreditamentoProvvisorio(a.getDataFineAccreditamento());
				}
				else {
					//altrimenti la data per il nuovo accreditamento provvisorio è fra 6 mesi da oggi
					provider.setCanInsertAccreditamentoProvvisorio(true);
					provider.setDataRinnovoInsertAccreditamentoProvvisorio(LocalDate.now().plusMonths(6));
				}
			}
		}
		providerService.save(provider);
		if(stato == AccreditamentoStatoEnum.ACCREDITATO)
			quotaAnnualeService.createPagamentoProviderPerQuotaAnnuale(provider.getId(), LocalDate.now().getYear(), true);
	}

	@Override
	public void rivaluta(Long accreditamentoId) {
		LOGGER.debug(Utils.getLogMessage("Rivaluta Domanda : " + accreditamentoId));
		Valutazione valutazione = valutazioneService.getValutazioneByAccreditamentoIdAndAccountIdAndNotStoricizzato(accreditamentoId, Utils.getAuthenticatedUser().getAccount().getId());
		valutazione.setDataValutazione(null);
		valutazioneService.save(valutazione);
	}

	@Override
	public void saveFileNoteOsservazioni(Long fileId, Long accreditamentoId) {
		Accreditamento accreditamento = getAccreditamento(accreditamentoId);
		File file = fileService.getFile(fileId);
		if (accreditamento.getStato().equals(AccreditamentoStatoEnum.INTEGRAZIONE))
			accreditamento.setNoteOsservazioniIntegrazione(file);
		else
			accreditamento.setNoteOsservazioniPreavvisoRigetto(file);
		accreditamentoRepository.save(accreditamento);
		//saveAndAudit(accreditamento);

	}

	@Override
	public Set<Accreditamento> getAllDomandeNonValutateByRefereeId(Long refereeId) {
		LOGGER.debug(Utils.getLogMessage("Ricerco tutte le utime domande non valutate consecutivamente dal referee id: " + refereeId));
		return accountRepository.getAllDomandeNonValutateByRefereeId(refereeId);
	}

	@Override
	@Transactional
	public void inviaValutazioneSulCampo(Long accreditamentoId, String valutazioneComplessiva, File verbalePdf, AccreditamentoStatoEnum destinazioneStatoDomandaStandard) throws Exception {
		LOGGER.debug(Utils.getLogMessage("Salvataggio verbale valutazione sul campo della domanda di Accreditamento " + accreditamentoId));
		Valutazione valutazione = valutazioneService.getValutazioneByAccreditamentoIdAndAccountIdAndNotStoricizzato(accreditamentoId, Utils.getAuthenticatedUser().getAccount().getId());
		Accreditamento accreditamento = getAccreditamento(accreditamentoId);
		Account user = Utils.getAuthenticatedUser().getAccount();

		//setta la data
		valutazione.setDataValutazione(LocalDateTime.now());
		//disabilito tutti i filedValutazioneAccreditamento
		for (FieldValutazioneAccreditamento fva : valutazione.getValutazioni()) {
			fva.setEnabled(false);
		}

		//inserisce il commento complessivo
		valutazione.setValutazioneComplessiva(valutazioneComplessiva);

		//setta lo stato dell'accreditamento al momento del salvataggio
		valutazione.setAccreditamentoStatoValutazione(accreditamento.getStato());

		valutazioneService.saveAndFlush(valutazione);

		valutazioneService.copiaInStorico(valutazione);

		//inutile viene modificato gia' quello
		//accreditamento.setVerbaleValutazioneSulCampo(verbale);
		//accreditamento.setVerbaleValutazioneSulCampoPdf(verbalePdf);

		accreditamento.setDataValutazioneCrecm(LocalDate.now());

		accreditamento.setVerbaleValutazioneSulCampoPdf(verbalePdf);

		//accreditamentoRepository.save(accreditamento);
		saveAndAudit(accreditamento);

		//TODO nemmeno qua sarebbe da assegnare la valutazione del team leader se l'accreditamento viene accreditato
		//mentre se va in integrazione se ne va in integrazione.. si potrebbe creare la valutazione del team leader al momento in cui va in integrazione
		//o meglio ancora quando si sblocca la valutazione dell'integrazione della segreteria.. nello stesso punto in cui nel provvisorio si andrebbe in riassegna stesso gruppo crecm
		workflowService.eseguiTaskValutazioneSulCampoForCurrentUser(accreditamento, accreditamento.getVerbaleValutazioneSulCampo().getTeamLeader().getUsernameWorkflow(), destinazioneStatoDomandaStandard);

		if(destinazioneStatoDomandaStandard == AccreditamentoStatoEnum.ACCREDITATO)
			settaStatusProviderAndDateAccreditamentoAndQuotaAnnuale(accreditamento.getVerbaleValutazioneSulCampo().getGiorno(), accreditamentoId, destinazioneStatoDomandaStandard);
	}

	//inserisce il sottoscrivente del verbale sul campo
	//TODO se si deve mandare un email di aggiornamento del verbale sul campo questo sarebbe il punto :')
	@Override
	public void editScheduleVerbaleValutazioneSulCampo(Accreditamento accreditamento, VerbaleValutazioneSulCampo verbaleNew) {
		VerbaleValutazioneSulCampo verbaleToUpdate = accreditamento.getVerbaleValutazioneSulCampo();
		verbaleToUpdate.setGiorno(verbaleNew.getGiorno());
		verbaleToUpdate.setTeamLeader(verbaleNew.getTeamLeader());
		verbaleToUpdate.setComponentiSegreteria(verbaleNew.getComponentiSegreteria());
		verbaleToUpdate.setOsservatoreRegionale(verbaleNew.getOsservatoreRegionale());
		verbaleToUpdate.setReferenteInformatico(verbaleNew.getReferenteInformatico());
		verbaleToUpdate.setSede(verbaleNew.getSede());
		verbaleToUpdate.setIsPresenteLegaleRappresentante(verbaleNew.getIsPresenteLegaleRappresentante());
		verbaleToUpdate.setDelegato(verbaleNew.getDelegato());
		verbaleToUpdate.setCartaIdentita(verbaleNew.getCartaIdentita());
		accreditamento.setVerbaleValutazioneSulCampo(verbaleToUpdate);
		save(accreditamento);
	}

	//modifica le info base del verbale sul campo
	@Override
	public void saveSottoscriventeVerbaleValutazioneSulCampo(Accreditamento accreditamento, VerbaleValutazioneSulCampo verbaleNew) {
		VerbaleValutazioneSulCampo verbaleToUpdate = accreditamento.getVerbaleValutazioneSulCampo();
		verbaleToUpdate.setCartaIdentita(verbaleNew.getCartaIdentita());
		verbaleToUpdate.setDelegato(verbaleNew.getDelegato());
		verbaleToUpdate.setIsPresenteLegaleRappresentante(verbaleNew.getIsPresenteLegaleRappresentante());
		accreditamento.setVerbaleValutazioneSulCampo(verbaleToUpdate);
		save(accreditamento);
	}

	@Override
	public void inviaEmailConvocazioneValutazioneSulCampo(Long accreditamentoId) throws Exception {
		LOGGER.info("Invio email per la Convocazione della Valutazione Sul Campo per accreditamento: " + accreditamentoId);
		Accreditamento accreditamento = getAccreditamento(accreditamentoId);
		VerbaleValutazioneSulCampo verbale = accreditamento.getVerbaleValutazioneSulCampo();
		Set<String> dst = new HashSet<String>();

		dst.add(verbale.getTeamLeader().getEmail());
		dst.add(verbale.getOsservatoreRegionale().getEmail());
		for(Account a : verbale.getComponentiSegreteria())
			dst.add(a.getEmail());
		if(verbale.getReferenteInformatico() != null)
			dst.add(verbale.getReferenteInformatico().getEmail());

		emailService.inviaConvocazioneValutazioneSulCampo(dst, verbale.getGiorno(), accreditamento.getProvider().getDenominazioneLegale());
	}

	@Override
	public boolean canUserAbilitaVariazioneDati(Long accreditamentoId, CurrentUser currentUser) {
		Accreditamento accreditamento = getAccreditamento(accreditamentoId);
		if(currentUser.isSegreteria()
				&& accreditamento.isAccreditato()
				&& (accreditamento.getStatoVariazioneDati() == null
				|| accreditamento.getStatoVariazioneDati() == AccreditamentoStatoEnum.CONCLUSO
				|| accreditamento.getStatoVariazioneDati() == AccreditamentoStatoEnum.RICHIESTA_INTEGRAZIONE)
				&& isNotVariazioneDatiPresaInCaricoDaAltri(accreditamento, currentUser.getAccount()))
			return true;
		else return false;
	}

	//funzione che controlla se la variazione dati può essere presa in carico
	private boolean isNotVariazioneDatiPresaInCaricoDaAltri(Accreditamento accreditamento, Account user) {
		Valutazione valutazioneSegreteriaVariazioneDati = valutazioneService.getValutazioneSegreteriaForAccreditamentoIdNotStoricizzato(accreditamento.getId());
		if(valutazioneSegreteriaVariazioneDati == null || valutazioneSegreteriaVariazioneDati.getAccount().equals(user))
			return true;
		else
			return false;
	}

	@Override
	public void avviaFlussoVariazioneDati(Accreditamento accreditamento) throws Exception {
		LOGGER.info("Avvio del flusso di Variazione dei Dati dell'Accreditamento: " + accreditamento.getId());
		Account segreteria = Utils.getAuthenticatedUser().getAccount();
		workflowService.createWorkflowAccreditamentoVariazioneDati(Utils.getAuthenticatedUser(), accreditamento);

		//si crea già anche la valutazione per la segreteria che sancisce la presa in carico
		Valutazione valutazioneSegreteria = new Valutazione();
		valutazioneSegreteria.setAccount(segreteria);
		valutazioneSegreteria.setAccreditamento(accreditamento);
		valutazioneSegreteria.setAccreditamentoStatoValutazione(null);
		valutazioneSegreteria.setTipoValutazione(ValutazioneTipoEnum.SEGRETERIA_ECM);
		valutazioneSegreteria.setStoricizzato(false);
		//setta tutti gli esiti a true e bloccati
		valutazioneSegreteria.setValutazioni(fieldValutazioneAccreditamentoService.createAllFieldValutazioneAndSetEsitoAndEnabled(true, false, valutazioneSegreteria.getAccreditamento()));

		valutazioneService.save(valutazioneSegreteria);
	}

	@Override
	public void inviaCampiSbloccatiVariazioneDati(Long accreditamentoId) throws Exception {
		LOGGER.info("Invio dei campi sbloccati in Variazione dei Dati dell'Accreditamento: " + accreditamentoId);
		Accreditamento accreditamento = getAccreditamento(accreditamentoId);
		//i giorni per modificare la domanda sono 10
		int giorniPerModifica = ecmProperties.getGiorniVariazioneDatiAccreditamento();
		Long timerIntegrazioneRigetto = giorniPerModifica * millisecondiInGiorno;

		workflowService.eseguiTaskRichiestaIntegrazioneForCurrentUser(accreditamento, timerIntegrazioneRigetto);
	}

	@Override
	public boolean canUserInviaCampiVariazioneDati(Long accreditamentoId, CurrentUser currentUser) {
		Accreditamento accreditamento = getAccreditamento(accreditamentoId);
		if(currentUser.isSegreteria() && accreditamento.getStatoVariazioneDati() == AccreditamentoStatoEnum.RICHIESTA_INTEGRAZIONE)
			return true;
		else return false;
	}

	@Override
	public void inviaValutazioneVariazioneDati(Long accreditamentoId, String valutazioneComplessiva, AccreditamentoStatoEnum destinazioneVariazioneDati, Account refereeVariazioneDati) throws Exception {
		Valutazione valutazione = valutazioneService.getValutazioneByAccreditamentoIdAndAccountIdAndNotStoricizzato(accreditamentoId, Utils.getAuthenticatedUser().getAccount().getId());
		Accreditamento accreditamento = getAccreditamento(accreditamentoId);
		Account user = Utils.getAuthenticatedUser().getAccount();
		List<FieldValutazioneAccreditamento> campiDaValutare = new ArrayList<FieldValutazioneAccreditamento>();

		//se l'utente è segreteria mi salvo tutti gli idField relativi ai fieldIntegrazione prima che questi
		//vengano cancellati e poi approvo l'integrazione
		if(user.isSegreteria()) {
			campiDaValutare = getFieldValutazioneDaValutare(accreditamentoId);
			//applica modifiche
			approvaIntegrazione(accreditamentoId);
		}

		//disabilito tutti i filedValutazioneAccreditamento
		for (FieldValutazioneAccreditamento fva : valutazione.getValutazioni()) {
			fva.setEnabled(false);
		}

		//setta la data
		valutazione.setDataValutazione(LocalDateTime.now());

		//inserisce il commento complessivo
		valutazione.setValutazioneComplessiva(valutazioneComplessiva);

		valutazione.setAccreditamentoStatoValutazione(null);

		valutazioneService.saveAndFlush(valutazione);

		//detacha e copia: da questo momento valutazione si riferisce alla copia storicizzata
		valutazioneService.copiaInStorico(valutazione);

		if(user.isSegreteria()) {

			if(destinazioneVariazioneDati == AccreditamentoStatoEnum.CONCLUSO) {
				workflowService.eseguiTaskValutazioneVariazioneDatiForCurrentUser(accreditamento, null, null, destinazioneVariazioneDati);
				//elimino le vecchie valutazioni
				Set<Valutazione> valutazioniVariazioneDati = valutazioneService.getAllValutazioniCompleteForAccreditamentoIdAndNotStoricizzato(accreditamento.getId());
				for(Valutazione v : valutazioniVariazioneDati) {
					valutazioneService.delete(v);
				}
			}
			else {
				List<String> valutatore = new ArrayList<String>();
				valutatore.add(refereeVariazioneDati.getUsername());
				workflowService.eseguiTaskValutazioneVariazioneDatiForCurrentUser(accreditamento, valutatore, 1, destinazioneVariazioneDati);

				//creo la valutazione per il referee
				Valutazione valutazioneReferee = new Valutazione();
				valutazioneReferee.setAccount(refereeVariazioneDati);
				valutazioneReferee.setAccreditamento(accreditamento);
				valutazioneReferee.setAccreditamentoStatoValutazione(null);
				valutazioneReferee.setTipoValutazione(ValutazioneTipoEnum.REFEREE);
				valutazioneReferee.setStoricizzato(false);
				//setta tutti gli esiti a true e bloccati
				valutazioneReferee.setValutazioni(fieldValutazioneAccreditamentoService.createAllFieldValutazioneAndSetEsitoAndEnabled(true, false, accreditamento));
				//sblocca e setta a null l'esito dei campi che dovrà valutare
				valutazioneService.resetEsitoAndEnabledForSubset(valutazioneReferee, campiDaValutare);

				valutazioneService.save(valutazioneReferee);
				emailService.inviaNotificaAReferee(refereeVariazioneDati.getEmail(), accreditamento.getProvider().getDenominazioneLegale());
			}
		}
		else {
			workflowService.eseguiTaskValutazioneCrecmForCurrentUser(accreditamento);
		}
	}

	private List<FieldValutazioneAccreditamento> getFieldValutazioneDaValutare(Long accreditamentoId) {
		List<FieldValutazioneAccreditamento> campiDaValutare = new ArrayList<FieldValutazioneAccreditamento>();
		Accreditamento accreditamento = getAccreditamento(accreditamentoId);
		Long workFlowProcessInstanceId = accreditamento.getWorkflowInCorso().getProcessInstanceId();
		AccreditamentoStatoEnum stato = accreditamento.getStatoUltimaIntegrazione();

		Set<FieldIntegrazioneAccreditamento> fieldIntegrazione = fieldIntegrazioneAccreditamentoService.getAllFieldIntegrazioneForAccreditamentoByContainer(accreditamentoId, stato, workFlowProcessInstanceId);
		Map<IdFieldEnum, Boolean> idGruppi = new HashMap<IdFieldEnum, Boolean>();
		for(FieldIntegrazioneAccreditamento fia : fieldIntegrazione) {
			FieldValutazioneAccreditamento fieldValutazione = new FieldValutazioneAccreditamento();
			//gestione adhoc per idField con gruppo
			//verranno poi aggiunti alla fine
			if(fia.getIdField().getSubSetField() == SubSetFieldEnum.DATI_ACCREDITAMENTO) {
				Boolean result = null;
				switch(fia.getIdField()) {
				case DATI_ACCREDITAMENTO__FATTURATO_COMPLESSIVO_UNO:
				case DATI_ACCREDITAMENTO__FATTURATO_COMPLESSIVO_DUE:
				case DATI_ACCREDITAMENTO__FATTURATO_COMPLESSIVO_TRE:
					result = idGruppi.get(IdFieldEnum.DATI_ACCREDITAMENTO__FATTURATO_COMPLESSIVO);
					//non ancora aggiunto, lo aggiungo oppure se result è false lo aggiorno con true
					if(result == null || result == false) {
						idGruppi.put(IdFieldEnum.DATI_ACCREDITAMENTO__FATTURATO_COMPLESSIVO, fia.isModificato());
					}
					break;
				case DATI_ACCREDITAMENTO__FATTURATO_FORMAZIONE_UNO:
				case DATI_ACCREDITAMENTO__FATTURATO_FORMAZIONE_DUE:
				case DATI_ACCREDITAMENTO__FATTURATO_FORMAZIONE_TRE:
					result = idGruppi.get(IdFieldEnum.DATI_ACCREDITAMENTO__FATTURATO_FORMAZIONE);
					//non ancora aggiunto, lo aggiungo oppure se result è false lo aggiorno con true
					if(result == null || result == false) {
						idGruppi.put(IdFieldEnum.DATI_ACCREDITAMENTO__FATTURATO_FORMAZIONE, fia.isModificato());
					}
					break;
				case DATI_ACCREDITAMENTO__NUMERO_DIPENDENTI_FORMAZIONE_TEMPO_INDETERMINATO:
				case DATI_ACCREDITAMENTO__NUMERO_DIPENDENTI_FORMAZIONE_ALTRO:
					result = idGruppi.get(IdFieldEnum.DATI_ACCREDITAMENTO__NUMERO_DIPENDENTI);
					//non ancora aggiunto, lo aggiungo oppure se result è false lo aggiorno con true
					if(result == null || result == false) {
						idGruppi.put(IdFieldEnum.DATI_ACCREDITAMENTO__NUMERO_DIPENDENTI, fia.isModificato());
					}
					break;
				default:
					fieldValutazione.setIdField(fia.getIdField());
					fieldValutazione.setObjectReference(fia.getObjectReference());
					fieldValutazione.setModificatoInIntegrazione(fia.isModificato());
					campiDaValutare.add(fieldValutazione);
					break;
				}
			}
			else {
				fieldValutazione.setIdField(fia.getIdField());
				fieldValutazione.setObjectReference(fia.getObjectReference());
				fieldValutazione.setModificatoInIntegrazione(fia.isModificato());
				campiDaValutare.add(fieldValutazione);
			}
		}
		//aggiungo tutti i field valutazione adHoc dei gruppi
		idGruppi.forEach((key, value) -> {
			FieldValutazioneAccreditamento fieldValutazione = new FieldValutazioneAccreditamento();
			fieldValutazione.setIdField(key);
			//non hanno mai obj reference
			fieldValutazione.setObjectReference(-1);
			fieldValutazione.setModificatoInIntegrazione(value);
			campiDaValutare.add(fieldValutazione);
		});

		return campiDaValutare;
	}

	//interrompe il flusso di accreditamento e avvia la procedura di conclusione
	@Override
	public void conclusioneProcedimento(Accreditamento accreditamento, CurrentUser currentUser) throws Exception {
		//chiamata a Bonita che annulla il vecchio flusso e apre il nuovo
		workflowService.createWorkflowAccreditamentoConclusioneProcedimento(currentUser, accreditamento);
	}

	@Override
	public Accreditamento getLastAccreditamentoForProviderId(Long providerId) {
		LOGGER.info("Cerco l'ultimo accreditamento del Provider: " + providerId);
		return accreditamentoRepository.findFirstByProviderIdOrderByDataScadenzaDesc(providerId);
	}

	/* Metodo che applica l'integrazione (ma non salva le modifiche) in modo in cui sia possibile fare un controllo sullo stato del DB
	 * a fine della valutazione della segreteria (ovvero subito prima di applicare le modifiche)
	 * Restituisce un array di String dove il primo elemento riguarda gli errori sul comitato e il secondo sulle sedi
	 */
	@Override
	public String[] controllaValidazioneIntegrazione(Long accreditamentoId) throws Exception {
		String erroreMsgComitato = null;
		String erroreMsgSedi = null;

		Provider provider = providerService.getProvider(getProviderIdForAccreditamento(accreditamentoId));
		Accreditamento accreditamento = getAccreditamento(accreditamentoId);
		Long workFlowProcessInstanceId = accreditamento.getWorkflowInCorso().getProcessInstanceId();
		//NB sono in valutazione
		AccreditamentoStatoEnum stato = accreditamento.getStatoUltimaIntegrazione();
		/* Verifichiamo che non vengano approvate modifiche al comitato scientifico che violino i vincoli della domanda */
		Set<Persona> componentiComitato = provider.getComponentiComitatoScientifico();
		/* e verifichiamo che non vengano approvate modifiche alle sedi che violino i vincoli della domanda */
		Set<Sede> sedi = provider.getSedi();
		Set<FieldIntegrazioneAccreditamento> fieldIntegrazione = fieldIntegrazioneAccreditamentoService.getAllFieldIntegrazioneApprovedBySegreteria(accreditamentoId, stato, workFlowProcessInstanceId);
		if(fieldIntegrazione == null || fieldIntegrazione.isEmpty()) {
			erroreMsgComitato = null;
			erroreMsgSedi = null;
		}
		else {
			Set<FieldIntegrazioneAccreditamento> fieldComponente = null;
			Iterator<Persona> personaIter = componentiComitato.iterator();
			while(personaIter.hasNext()) {
				Persona p = personaIter.next();
				fieldComponente = Utils.getSubset(fieldIntegrazione, p.getId(), SubSetFieldEnum.COMPONENTE_COMITATO_SCIENTIFICO);
				if(fieldComponente != null && !fieldComponente.isEmpty()) {
					integrazioneService.applyIntegrazioneObject(p, fieldComponente);
					fieldIntegrazione.removeAll(fieldComponente);
				}
				fieldComponente = Utils.getSubset(fieldIntegrazione, p.getId(), SubSetFieldEnum.FULL);
				if(fieldComponente != null && !fieldComponente.isEmpty()) {
					if(fieldComponente.iterator().next().getTipoIntegrazioneEnum() == TipoIntegrazioneEnum.ELIMINAZIONE) {
						personaIter.remove();
						fieldIntegrazione.removeAll(fieldComponente);
					}
				}
			}
			IdFieldEnum comitatoFull = Utils.getFullFromRuolo(Ruolo.COMPONENTE_COMITATO_SCIENTIFICO);
			for(FieldIntegrazioneAccreditamento fia : fieldIntegrazione) {
				if(fia.getIdField() == comitatoFull) {
					componentiComitato.add(personaService.getPersona(fia.getObjectReference()));
				}
			}
			erroreMsgComitato = providerService.controllaComitato(componentiComitato, true);

			Set<FieldIntegrazioneAccreditamento> fieldSede = null;
			Iterator<Sede> sedeIter = sedi.iterator();
			while(sedeIter.hasNext()) {
				Sede s = sedeIter.next();
				fieldSede = Utils.getSubset(fieldIntegrazione, s.getId(), SubSetFieldEnum.SEDE);
				if(fieldSede != null && !fieldSede.isEmpty()) {
					integrazioneService.applyIntegrazioneObject(s, fieldSede);
					fieldIntegrazione.removeAll(fieldSede);
				}
				fieldSede = Utils.getSubset(fieldIntegrazione, s.getId(), SubSetFieldEnum.FULL);
				if(fieldSede != null && !fieldSede.isEmpty()) {
					if(fieldSede.iterator().next().getTipoIntegrazioneEnum() == TipoIntegrazioneEnum.ELIMINAZIONE) {
						sedeIter.remove();
						fieldIntegrazione.removeAll(fieldSede);
					}
				}
			}
			IdFieldEnum sedeFull = IdFieldEnum.SEDE__FULL;
			for(FieldIntegrazioneAccreditamento fia : fieldIntegrazione) {
				if(fia.getIdField() == sedeFull) {
					sedi.add(sedeService.getSede(fia.getObjectReference()));
				}
			}
			erroreMsgSedi = providerService.controllaSedi(sedi);

		}
		return new String[] {erroreMsgComitato, erroreMsgSedi};
	}
}
