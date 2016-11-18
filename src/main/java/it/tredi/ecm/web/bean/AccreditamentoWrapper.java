package it.tredi.ecm.web.bean;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import it.tredi.ecm.dao.entity.Account;
import it.tredi.ecm.dao.entity.Accreditamento;
import it.tredi.ecm.dao.entity.DatiAccreditamento;
import it.tredi.ecm.dao.entity.EventoPianoFormativo;
import it.tredi.ecm.dao.entity.FieldEditabileAccreditamento;
import it.tredi.ecm.dao.entity.FieldValutazioneAccreditamento;
import it.tredi.ecm.dao.entity.File;
import it.tredi.ecm.dao.entity.Persona;
import it.tredi.ecm.dao.entity.Professione;
import it.tredi.ecm.dao.entity.Provider;
import it.tredi.ecm.dao.entity.Sede;
import it.tredi.ecm.dao.entity.Valutazione;
import it.tredi.ecm.dao.enumlist.AccreditamentoWrapperModeEnum;
import it.tredi.ecm.dao.enumlist.FileEnum;
import it.tredi.ecm.dao.enumlist.IdFieldEnum;
import it.tredi.ecm.dao.enumlist.SubSetFieldEnum;
import it.tredi.ecm.utils.Utils;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccreditamentoWrapper {

	//dati domanda di accreditamento
	private Accreditamento accreditamento;

	//dati provider
	private Provider provider;

	private Sede sedeLegale;
	private Set<Sede> sedi = new HashSet<Sede>();

	private Persona legaleRappresentante;
	private Persona delegatoLegaleRappresentante;

	//dati domanda accreditamento
	private DatiAccreditamento datiAccreditamento;

	//dati anagrafiche interne provider
	private Persona responsabileSegreteria;
	private Persona responsabileAmministrativo;
	private Persona coordinatoreComitatoScientifico;
	private List<Persona> componentiComitatoScientifico = new ArrayList<Persona>();
	private Persona responsabileSistemaInformatico;
	private Persona responsabileQualita;

	/*	flag per stato avanzamento domanda	*/
	private boolean providerStato;
	private boolean sediStato;
	private boolean tutteSediValutate = true;
	private boolean legaleRappresentanteStato;
	private boolean delegatoLegaleRappresentanteStato;

	private boolean datiAccreditamentoStato;

	private boolean responsabileSegreteriaStato;
	private boolean responsabileAmministrativoStato;
	private boolean responsabileSistemaInformaticoStato;
	private boolean responsabileQualitaStato;

	private boolean comitatoScientificoStato;
	private boolean tuttiComponentiValutati = true;
	private String comitatoScientificoErrorMessage;

	private boolean attoCostitutivoStato;
	private boolean esperienzaFormazioneStato;
	private boolean utilizzoStato;
	private boolean sistemaInformaticoStato;
	private boolean pianoQualitaStato;
	private boolean dichiarazioneLegaleStato;
	private boolean dichiarazioneEsclusioneStato;

	private boolean tuttiEventiValutati = true;

	private boolean sezione1Stato;
	private boolean sezione2Stato;
	private boolean sezione3Stato;
	private boolean sezione4Stato;

	private boolean canSendValutazione;
	private boolean canSendIntegrazione;

	//stati per i pulsanti segreteria
	private boolean canPrendiInCarica;
	private boolean canValutaDomanda;
	private boolean canShowValutazione;
	private boolean canShowValutazioneRiepilogo;
	private boolean canAssegnaNuovoGruppo;
	private boolean canConfermaValutazione;
	private boolean canPresaVisione;
	private boolean canEnableField;

	//flag per vedere se la segreteria può editare
	private boolean canSegreteriaEdit;

	//int referee da riassegnare
	private int refereeDaRiassegnare = 0;

	//boolean Stati dei multistanza
	private Map<Long, Boolean> componentiComitatoScientificoStati = new HashMap<Long, Boolean>();
	private Map<Long, Boolean> sediStati = new HashMap<Long, Boolean>();
	private boolean coordinatoreComitatoScientificoStato;
	private boolean sedeLegaleStato;
	//TODO togliere se confermato che non serve più
	//private Map<Long, Boolean> eventiStati = new HashMap<Long, Boolean>();

	//Mappe con i FieldValutazione
	private Map<IdFieldEnum, FieldValutazioneAccreditamento> mappa;
	private Map<Long, Map<IdFieldEnum, FieldValutazioneAccreditamento>> mappaComponenti;
	private Map<Long, Map<IdFieldEnum, FieldValutazioneAccreditamento>> mappaSedi;
	private Map<IdFieldEnum, FieldValutazioneAccreditamento> mappaCoordinatore;
	private Map<IdFieldEnum, FieldValutazioneAccreditamento> mappaSedeLegale;
	//TODO togliere se confermato che non serve più
	//private Map<Long, Map<IdFieldEnum, FieldValutazioneAccreditamento>> mappaEventi;

	//Gruppo dei referee
	private Set<Account> refereeGroup = new HashSet<Account>();

	//Valutazione Complessiva
	private String valutazioneComplessiva;

	//Valutazioni per l'accreditamento
	private Set<Valutazione> valutazioniList = new HashSet<Valutazione>();

	//Elenco MultiIstanza aggiunti e eliminati durante Integrazione
	private Set<Long> aggiunti = new HashSet<Long>();
	private Set<Long> eliminati = new HashSet<Long>();

	//File allegati integrazione
	private File noteOsservazioniIntegrazione;
	private File noteOsservazioniPreavvisoRigetto;
	
	//File import pianoFormativo da csv
	private File importEventiDaCsvFile;

	public AccreditamentoWrapper(){};
	public AccreditamentoWrapper(Accreditamento accreditamento){
		setAllAccreditamento(accreditamento);
	}

	public void setAllAccreditamento(Accreditamento accreditamento) {
		this.setAccreditamento(accreditamento);

		// PROVIDER
		this.setProvider(accreditamento.getProvider());

		//SEDI
		for(Sede s : accreditamento.getProvider().getSedi()) {
			if(s.isSedeLegale())
				this.setSedeLegale(s);
			else
				this.getSedi().add(s);
		}

		//DATI ACCREDITAMENTO
		DatiAccreditamento datiAccreditamento = accreditamento.getDatiAccreditamento();
		this.setDatiAccreditamento(datiAccreditamento != null ? datiAccreditamento : new DatiAccreditamento());

		// LEGALE RAPPRESENTANTE E RESPONSABILI
		for(Persona p : accreditamento.getProvider().getPersone()){
			if(p.isLegaleRappresentante())
				this.setLegaleRappresentante(p);
			else if(p.isDelegatoLegaleRappresentante())
				this.setDelegatoLegaleRappresentante(p);
			else if(p.isResponsabileSegreteria())
				this.setResponsabileSegreteria(p);
			else if(p.isResponsabileAmministrativo())
				this.setResponsabileAmministrativo(p);
			else if(p.isResponsabileSistemaInformatico())
				this.setResponsabileSistemaInformatico(p);
			else if(p.isResponsabileQualita())
				this.setResponsabileQualita(p);
			else if(p.isCoordinatoreComitatoScientifico())
				this.setCoordinatoreComitatoScientifico(p);
			else if(p.isComponenteComitatoScientifico())
				this.getComponentiComitatoScientifico().add(p);
		}
	}


	public void checkStati(int numeroComponentiComitatoScientifico, int numeroProfessionistiSanitarie, Set<Professione> elencoProfessioniDeiComponenti, int professioniDeiComponentiAnaloghe,Set<String> filesDelProvider, AccreditamentoWrapperModeEnum mode){
		//TODO migliorare la logica per evitare di fare troppi if
		// ad esempio inizializzare gli stati a true e poi ad ogni controllo se fallisce si mette il false sia allo stato che al valid
		// cosi facendo valid è settato in automatico senza rifare tutti i controlli
		//
		//NON lo faccio adesso perchè voglio capire in fase di validazione della domanda come gestiremo i vari stati

		//check sulla modalità di visualizzazione
		if(mode == AccreditamentoWrapperModeEnum.SHOW || mode == AccreditamentoWrapperModeEnum.EDIT) {
			providerStato = (provider.getRagioneSociale()!= null) ? true : false;

			//check inserimento sede Legale
			sedeLegaleStato = (sedeLegale != null && !sedeLegale.isNew()) ? true : false;

			//check sul cv unico campo non settabile in registrazione o in modifica delle anagrafiche, ma solo durante la domanda di accreditamento
			legaleRappresentanteStato = (legaleRappresentante != null && !legaleRappresentante.isNew() && legaleRappresentante.getAnagrafica().getCellulare() != null && !legaleRappresentante.getAnagrafica().getCellulare().isEmpty()) ? true : false;
			delegatoLegaleRappresentanteStato = (delegatoLegaleRappresentante != null && !delegatoLegaleRappresentante.isNew() && delegatoLegaleRappresentante.getAnagrafica().getCellulare() != null && !delegatoLegaleRappresentante.getAnagrafica().getCellulare().isEmpty()) ? true : false;

			datiAccreditamentoStato = (datiAccreditamento != null && !datiAccreditamento.isNew()) ? true : false;

			responsabileSegreteriaStato = (responsabileSegreteria != null && !responsabileSegreteria.isNew()) ? true : false;
			responsabileAmministrativoStato = (responsabileAmministrativo != null && !responsabileAmministrativo.isNew()) ? true : false;
			responsabileSistemaInformaticoStato = (responsabileSistemaInformatico != null && !responsabileSistemaInformatico.isNew()) ? true : false;
			responsabileQualitaStato = (responsabileQualita != null && !responsabileQualita.isNew()) ? true : false;

			checkComitatoScientifico_fromDB(numeroComponentiComitatoScientifico, numeroProfessionistiSanitarie, elencoProfessioniDeiComponenti, professioniDeiComponentiAnaloghe);
			setFilesStato(filesDelProvider);

			sezione1Stato = (providerStato && sedeLegaleStato && legaleRappresentanteStato && datiAccreditamentoStato) ? true : false;
			sezione2Stato = (responsabileSegreteriaStato && responsabileAmministrativoStato && responsabileSistemaInformaticoStato && responsabileQualitaStato && comitatoScientificoStato) ? true : false;
			sezione3Stato = (attoCostitutivoStato && (esperienzaFormazioneStato || !accreditamento.getDatiAccreditamento().getDatiEconomici().hasFatturatoFormazione()) && utilizzoStato && sistemaInformaticoStato && pianoQualitaStato && dichiarazioneLegaleStato) ? true : false;
		}

		if(mode == AccreditamentoWrapperModeEnum.VALIDATE) {

			providerStato = (mappa.containsKey(IdFieldEnum.PROVIDER__TIPO_ORGANIZZATORE) &&
				mappa.containsKey(IdFieldEnum.PROVIDER__DENOMINAZIONE_LEGALE) &&
				mappa.containsKey(IdFieldEnum.PROVIDER__PARTITA_IVA) &&
				mappa.containsKey(IdFieldEnum.PROVIDER__CODICE_FISCALE) &&
				mappa.containsKey(IdFieldEnum.PROVIDER__RAGIONE_SOCIALE) &&
				mappa.containsKey(IdFieldEnum.PROVIDER__EMAIL_STRUTTURA) &&
				mappa.containsKey(IdFieldEnum.PROVIDER__NATURA_ORGANIZZAZIONE) &&
				mappa.containsKey(IdFieldEnum.PROVIDER__NO_PROFIT));

			legaleRappresentanteStato =	(mappa.containsKey(IdFieldEnum.LEGALE_RAPPRESENTANTE__COGNOME) &&
				mappa.containsKey(IdFieldEnum.LEGALE_RAPPRESENTANTE__NOME) &&
				mappa.containsKey(IdFieldEnum.LEGALE_RAPPRESENTANTE__CODICEFISCALE) &&
				mappa.containsKey(IdFieldEnum.LEGALE_RAPPRESENTANTE__CELLULARE) &&
				mappa.containsKey(IdFieldEnum.LEGALE_RAPPRESENTANTE__EMAIL) &&
				mappa.containsKey(IdFieldEnum.LEGALE_RAPPRESENTANTE__PEC) &&
				mappa.containsKey(IdFieldEnum.LEGALE_RAPPRESENTANTE__ATTO_NOMINA) &&
				mappa.containsKey(IdFieldEnum.LEGALE_RAPPRESENTANTE__CV));

			delegatoLegaleRappresentanteStato = (mappa.containsKey(IdFieldEnum.DELEGATO_LEGALE_RAPPRESENTANTE__COGNOME) &&
				mappa.containsKey(IdFieldEnum.DELEGATO_LEGALE_RAPPRESENTANTE__NOME) &&
				mappa.containsKey(IdFieldEnum.DELEGATO_LEGALE_RAPPRESENTANTE__CODICEFISCALE) &&
				mappa.containsKey(IdFieldEnum.DELEGATO_LEGALE_RAPPRESENTANTE__TELEFONO) &&
				mappa.containsKey(IdFieldEnum.DELEGATO_LEGALE_RAPPRESENTANTE__CELLULARE) &&
				mappa.containsKey(IdFieldEnum.DELEGATO_LEGALE_RAPPRESENTANTE__EMAIL) &&
				mappa.containsKey(IdFieldEnum.DELEGATO_LEGALE_RAPPRESENTANTE__CV) &&
				mappa.containsKey(IdFieldEnum.DELEGATO_LEGALE_RAPPRESENTANTE__DELEGA));

			datiAccreditamentoStato = (mappa.containsKey(IdFieldEnum.DATI_ACCREDITAMENTO__TIPOLOGIA_ACCREDITAMENTO) &&
				mappa.containsKey(IdFieldEnum.DATI_ACCREDITAMENTO__PROCEDURE_FORMATIVE) &&
				mappa.containsKey(IdFieldEnum.DATI_ACCREDITAMENTO__PROFESSIONI_ACCREDITAMENTO) &&
				mappa.containsKey(IdFieldEnum.DATI_ACCREDITAMENTO__DISCIPLINE) &&
				mappa.containsKey(IdFieldEnum.DATI_ACCREDITAMENTO__FATTURATO_COMPLESSIVO) &&
				mappa.containsKey(IdFieldEnum.DATI_ACCREDITAMENTO__ESTRATTO_BILANCIO_COMPLESSIVO) &&
				mappa.containsKey(IdFieldEnum.DATI_ACCREDITAMENTO__FATTURATO_FORMAZIONE) &&
				mappa.containsKey(IdFieldEnum.DATI_ACCREDITAMENTO__ESTRATTO_BILANCIO_FORMAZIONE) &&
				mappa.containsKey(IdFieldEnum.DATI_ACCREDITAMENTO__NUMERO_DIPENDENTI) &&
				mappa.containsKey(IdFieldEnum.DATI_ACCREDITAMENTO__ORGANIGRAMMA) &&
				mappa.containsKey(IdFieldEnum.DATI_ACCREDITAMENTO__FUNZIONIGRAMMA));

			responsabileSegreteriaStato = (mappa.containsKey(IdFieldEnum.RESPONSABILE_SEGRETERIA__COGNOME) &&
				mappa.containsKey(IdFieldEnum.RESPONSABILE_SEGRETERIA__NOME) &&
				mappa.containsKey(IdFieldEnum.RESPONSABILE_SEGRETERIA__CODICEFISCALE) &&
				mappa.containsKey(IdFieldEnum.RESPONSABILE_SEGRETERIA__TELEFONO) &&
				mappa.containsKey(IdFieldEnum.RESPONSABILE_SEGRETERIA__EMAIL) &&
				mappa.containsKey(IdFieldEnum.RESPONSABILE_SEGRETERIA__ATTO_NOMINA) &&
				mappa.containsKey(IdFieldEnum.RESPONSABILE_SEGRETERIA__CV));

			responsabileAmministrativoStato = (mappa.containsKey(IdFieldEnum.RESPONSABILE_AMMINISTRATIVO__COGNOME) &&
				mappa.containsKey(IdFieldEnum.RESPONSABILE_AMMINISTRATIVO__NOME) &&
				mappa.containsKey(IdFieldEnum.RESPONSABILE_AMMINISTRATIVO__CODICEFISCALE) &&
				mappa.containsKey(IdFieldEnum.RESPONSABILE_AMMINISTRATIVO__TELEFONO) &&
				mappa.containsKey(IdFieldEnum.RESPONSABILE_AMMINISTRATIVO__EMAIL) &&
				mappa.containsKey(IdFieldEnum.RESPONSABILE_AMMINISTRATIVO__ATTO_NOMINA) &&
				mappa.containsKey(IdFieldEnum.RESPONSABILE_AMMINISTRATIVO__CV));

			responsabileSistemaInformaticoStato = (mappa.containsKey(IdFieldEnum.RESPONSABILE_SISTEMA_INFORMATICO__COGNOME) &&
				mappa.containsKey(IdFieldEnum.RESPONSABILE_SISTEMA_INFORMATICO__NOME) &&
				mappa.containsKey(IdFieldEnum.RESPONSABILE_SISTEMA_INFORMATICO__CODICEFISCALE) &&
				mappa.containsKey(IdFieldEnum.RESPONSABILE_SISTEMA_INFORMATICO__TELEFONO) &&
				mappa.containsKey(IdFieldEnum.RESPONSABILE_SISTEMA_INFORMATICO__EMAIL) &&
				mappa.containsKey(IdFieldEnum.RESPONSABILE_SISTEMA_INFORMATICO__ATTO_NOMINA) &&
				mappa.containsKey(IdFieldEnum.RESPONSABILE_SISTEMA_INFORMATICO__CV));

			responsabileQualitaStato = (mappa.containsKey(IdFieldEnum.RESPONSABILE_QUALITA__COGNOME) &&
				mappa.containsKey(IdFieldEnum.RESPONSABILE_QUALITA__NOME) &&
				mappa.containsKey(IdFieldEnum.RESPONSABILE_QUALITA__CODICEFISCALE) &&
				mappa.containsKey(IdFieldEnum.RESPONSABILE_QUALITA__TELEFONO) &&
				mappa.containsKey(IdFieldEnum.RESPONSABILE_QUALITA__EMAIL) &&
				mappa.containsKey(IdFieldEnum.RESPONSABILE_QUALITA__ATTO_NOMINA) &&
				mappa.containsKey(IdFieldEnum.RESPONSABILE_QUALITA__CV));

			attoCostitutivoStato = mappa.containsKey(IdFieldEnum.ACCREDITAMENTO_ALLEGATI__ATTO_COSTITUIVO);
			esperienzaFormazioneStato = mappa.containsKey(IdFieldEnum.ACCREDITAMENTO_ALLEGATI__ESPERIENZA_FORMAZIONE);
			utilizzoStato = mappa.containsKey(IdFieldEnum.ACCREDITAMENTO_ALLEGATI__UTILIZZO);
			sistemaInformaticoStato = mappa.containsKey(IdFieldEnum.ACCREDITAMENTO_ALLEGATI__SISTEMA_INFORMATICO);
			pianoQualitaStato = mappa.containsKey(IdFieldEnum.ACCREDITAMENTO_ALLEGATI__PIANO_QUALITA);
			dichiarazioneLegaleStato = mappa.containsKey(IdFieldEnum.ACCREDITAMENTO_ALLEGATI__DICHIARAZIONE_LEGALE);
			dichiarazioneEsclusioneStato = mappa.containsKey(IdFieldEnum.ACCREDITAMENTO_ALLEGATI__DICHIARAZIONE_ESCLUSIONE);

			//check valutazione dei multistanza

			//componenti comitato scientifico N.B. NON controlla bene tutti i FieldValutazione come gli altri per semplicità TODO decidere se implementare o se semplificare anche le altre
			for (Persona p : componentiComitatoScientifico) {
				if(mappaComponenti.get(p.getId()) != null && !mappaComponenti.get(p.getId()).isEmpty())
					componentiComitatoScientificoStati.replace(p.getId(), true);
			};

			//sedi N.B. NON controlla bene tutti i FieldValutazione come gli altri per semplicità TODO decidere se implementare o se semplificare anche le altre
			for (Sede s : sedi) {
				if(mappaSedi.get(s.getId()) != null && !mappaSedi.get(s.getId()).isEmpty())
					sediStati.replace(s.getId(), true);
			};

			//controllo anche il coordinatore N.B. stesso discorso dei componenti
			coordinatoreComitatoScientificoStato = (mappaCoordinatore != null && !mappaCoordinatore.isEmpty()) ? true : false;

			//controllo anche la sede legale N.B. stesso discorso delle sedi
			sedeLegaleStato = (mappaSedeLegale != null && !mappaSedeLegale.isEmpty()) ? true : false;

//			coordinatoreComitatoScientificoStato = (mappaCoordinatore.containsKey(IdFieldEnum.COMPONENTE_COMITATO_SCIENTIFICO__COGNOME) &&
//				mappaCoordinatore.containsKey(IdFieldEnum.COMPONENTE_COMITATO_SCIENTIFICO__NOME) &&
//				mappaCoordinatore.containsKey(IdFieldEnum.COMPONENTE_COMITATO_SCIENTIFICO__CODICEFISCALE) &&
//				mappaCoordinatore.containsKey(IdFieldEnum.COMPONENTE_COMITATO_SCIENTIFICO__TELEFONO) &&
//				mappaCoordinatore.containsKey(IdFieldEnum.COMPONENTE_COMITATO_SCIENTIFICO__CELLULARE) &&
//				mappaCoordinatore.containsKey(IdFieldEnum.COMPONENTE_COMITATO_SCIENTIFICO__EMAIL) &&
//				mappaCoordinatore.containsKey(IdFieldEnum.COMPONENTE_COMITATO_SCIENTIFICO__PROFESSIONE) &&
//				mappaCoordinatore.containsKey(IdFieldEnum.COMPONENTE_COMITATO_SCIENTIFICO__ATTO_NOMINA) &&
//				mappaCoordinatore.containsKey(IdFieldEnum.COMPONENTE_COMITATO_SCIENTIFICO__CV));

			//eventi pianoFormativo N.B. stesso discorso dei componenti
			//TODO rimuovere se confermato che non serve più
//			for (EventoPianoFormativo e : accreditamento.getPianoFormativo().getEventi()) {
//				if(mappaEventi.get(e.getId()) != null && !mappaEventi.get(e.getId()).isEmpty())
//					eventiStati.replace(e.getId(), true);
//			};

			//ciclo la mappa degli stati dei componenti (tuttiComponentiValutati default == true)
			componentiComitatoScientificoStati.forEach((k,v) -> {
				if(v == false)
					tuttiComponentiValutati = false;
			});

			//ciclo la mappa degli stati delle sedi (tutteSediValutate default == true)
			sediStati.forEach((k,v) -> {
				if(v == false)
					tutteSediValutate = false;
			});

			comitatoScientificoStato = (coordinatoreComitatoScientificoStato && tuttiComponentiValutati) ? true : false;
			sediStato = (sedeLegaleStato && tutteSediValutate) ? true : false;

			//ciclo la mappa degli stati degli eventi (tuttiEventiValutati default == true)
			//TODO rimuovere se deciso che non serve più
//			eventiStati.forEach((k,v) -> {
//				if(v == false)
//					tuttiEventiValutati = false;
//			});

			//sezioni validate o meno
			sezione1Stato = (providerStato && sediStato && legaleRappresentanteStato && datiAccreditamentoStato) ? true : false;
			sezione2Stato = (responsabileSegreteriaStato && responsabileAmministrativoStato && responsabileSistemaInformaticoStato && responsabileQualitaStato && comitatoScientificoStato) ? true : false;
			sezione3Stato = (attoCostitutivoStato && esperienzaFormazioneStato && utilizzoStato && sistemaInformaticoStato && pianoQualitaStato && dichiarazioneLegaleStato) ? true : false;
			//TODO rimuovere se deciso che non serve più
//			sezione4Stato = tuttiEventiValutati ? true : false;

			//stato di valutazione completa
			canSendValutazione = (sezione1Stato && sezione2Stato && sezione3Stato/* && sezione4Stato */);

			canConfermaValutazione = (canValutaDomanda && canSendValutazione) ? true : false;
		}
	}

	/*
	 * [A] Almeno 5 componenti (coordinatore incluso)
	 * [B] Almeno 5 professionisti Sanitari
	 * [C] Se settato "Generale" -> Almeno 2 professioni diverse tra i componenti TODO rimosse come richiesto, ma commentate solamente (non si sa mai)
	 * [D] Se settato "Settoriale" -> Almeno 2 professioni analoghe a quelle selezionate (almeno che non sia stata selezionate solo 1 professione)
	 * [D-bis] Se Selezionata solo 1 professione -> deve conicidere con quella e UNICA dei componenti del comitato scientifico
	 * */
	public void checkComitatoScientifico_fromDB(int numeroComponentiComitatoScientifico, int numeroProfessionistiSanitari, Set<Professione> elencoProfessioniDeiComponenti, int professioniDeiComponentiAnaloghe){
		comitatoScientificoStato = true;

//		int professioniDeiComponenti = elencoProfessioniDeiComponenti.size();

		//[A]
		if(numeroComponentiComitatoScientifico < 4 || (coordinatoreComitatoScientifico == null || coordinatoreComitatoScientifico.isNew())){
			comitatoScientificoStato = false;
			comitatoScientificoErrorMessage = "error.numero_minimo_comitato";
		}//[B]
		else if(numeroProfessionistiSanitari < 5){
			comitatoScientificoStato = false;
			comitatoScientificoErrorMessage = "error.numero_minimo_professionisti_sanitari";
		}
//		else{
//			// mi assicuro che sono stati gia' inseriti i dati relativi all'accreditamento
//			if(datiAccreditamentoStato){
//				//[C]
//				if(datiAccreditamento.getProfessioniAccreditamento().equalsIgnoreCase("generale")){
//					if(professioniDeiComponenti < 2){
//						comitatoScientificoStato = false;
//						comitatoScientificoErrorMessage = "error.numero_minimo_professioni";
//					}
//				}//[D]
//				else{
//					if(datiAccreditamento.getProfessioniSelezionate().size() > 1){
//						if(professioniDeiComponentiAnaloghe < 2){
//							comitatoScientificoStato = false;
//							comitatoScientificoErrorMessage = "error.numero_minimo_professioni_settoriale";
//						}
//					}//[D - bis]
//					else{
//						if(elencoProfessioniDeiComponenti.size() > 1 || elencoProfessioniDeiComponenti.size() == 0){
//							comitatoScientificoStato = false;
//							comitatoScientificoErrorMessage = "error.professioni_non_conformi_con_accreditamento";
//						}else{
//							if(elencoProfessioniDeiComponenti.iterator().next() != datiAccreditamento.getProfessioniSelezionate().iterator().next()){
//								comitatoScientificoStato = false;
//								comitatoScientificoErrorMessage = "error.professioni_non_conformi_con_accreditamento";
//							}
//						}
//					}
//				}
//			}else{
//				comitatoScientificoStato = false;
//				comitatoScientificoErrorMessage = "error.datiaccreditamento_mancanti";
//			}
//		}
	}

	private void setFilesStato(Set<String> filesDelProvider){
		if(filesDelProvider.contains(FileEnum.FILE_ATTO_COSTITUTIVO))
			attoCostitutivoStato = true;
		if(filesDelProvider.contains(FileEnum.FILE_ESPERIENZA_FORMAZIONE))
			esperienzaFormazioneStato = true;
		if(filesDelProvider.contains(FileEnum.FILE_UTILIZZO))
			utilizzoStato = true;
		if(filesDelProvider.contains(FileEnum.FILE_SISTEMA_INFORMATICO))
			sistemaInformaticoStato = true;
		if(filesDelProvider.contains(FileEnum.FILE_PIANO_QUALITA))
			pianoQualitaStato = true;
		if(filesDelProvider.contains(FileEnum.FILE_DICHIARAZIONE_LEGALE))
			dichiarazioneLegaleStato = true;
		if(filesDelProvider.contains(FileEnum.FILE_DICHIARAZIONE_ESCLUSIONE))
			dichiarazioneEsclusioneStato = true;
	}

	public boolean isComitatoScientificoEditabile(){
		Set<IdFieldEnum> ids = Utils.getSubsetOfIdFieldEnum(new HashSet<FieldEditabileAccreditamento>(getAccreditamento().getIdEditabili()), SubSetFieldEnum.COMITATO_SCIENTIFICO);
		if(ids.isEmpty())
			return false;
		else
			return true;
	}

	//la domanda è stata compilata in tutte le sue parti (tutti i flag sono TRUE)
	public boolean isCompleta(){
		if(sezione1Stato && sezione2Stato && sezione3Stato)
			return true;
		else
			return false;
	}

	//Inserisci piano formativo e blocca idEditabili
	public boolean isCanInsertPianoFormativo(){
		if(accreditamento.isBozza() && isCompleta() && !accreditamento.hasPianoFormativo())
			return true;
		else
			return false;
	}

	//Invia domanda alla segreteria cambiando stato all'accreditamento e rendendo la domanda non più modificabile
	public boolean isCanSend(){
		if(accreditamento.isBozza() && isCompleta() && isPianoFormativoCompleto())
			return true;
		else
			return false;
	}

	//ci sono eventi inseriti nel piano formativo
	private boolean isPianoFormativoCompleto(){
		return ((accreditamento.getPianoFormativo() != null) && !accreditamento.getPianoFormativo().getEventiPianoFormativo().isEmpty());
	}

	public boolean isCanInsertEventoInPianoFormativo(){
		return (accreditamento.hasPianoFormativo());
		//return (accreditamento.hasPianoFormativo() && accreditamento.isEditabile());
		//TODO controllo FieldEditabileAccreditamento
	}
}
